import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * A player thread in the card game.
 *
 * <p>Player {@code i} prefers denomination {@code i}, draws from deck {@code i}
 * and discards to deck {@code i+1} (wrapping round to deck 1). A draw and the
 * matching discard form one atomic action: both decks are locked (in a
 * deadlock-free order) for the duration, so no other thread can ever observe a
 * player holding five cards, and cards are never lost or duplicated.
 *
 * <p>Discard strategy: after drawing, the player discards the
 * <em>longest-held</em> card that is not of its preferred denomination. This
 * is a valid "any non-preferred card" choice and also guarantees a player can
 * never hold on to a non-preferred card indefinitely.
 *
 * <p>The hand is only mutated by the player's own thread; accessors are
 * synchronised so other threads (e.g. tests) can read it safely.
 */
public class Player implements Runnable {

    private final int id;
    private final CardDeck drawDeck;
    private final CardDeck discardDeck;
    private final AtomicInteger winner;
    private final CountDownLatch initialChecks;
    private final PrintWriter out;
    private final List<Card> hand = new ArrayList<>();

    /**
     * @param id            player number (1..n); also the preferred denomination
     * @param drawDeck      deck this player draws from (the deck to their left)
     * @param discardDeck   deck this player discards to (the deck to their right)
     * @param winner        shared across all players: 0 until someone wins, then the
     *                      winner's number (set once, via compare-and-set, so there is
     *                      only ever one winner; polling it is how players are informed)
     * @param initialChecks shared start barrier: every player counts it down after
     *                      checking their dealt hand, and waits for it before playing
     * @param outputDir     directory in which {@code player<id>_output.txt} is created
     * @throws UncheckedIOException if the output file cannot be created
     */
    public Player(int id, CardDeck drawDeck, CardDeck discardDeck, AtomicInteger winner,
                  CountDownLatch initialChecks, Path outputDir) {
        if (id < 1) {
            throw new IllegalArgumentException("Player id must be at least 1: " + id);
        }
        this.id = id;
        this.drawDeck = drawDeck;
        this.discardDeck = discardDeck;
        this.winner = winner;
        this.initialChecks = initialChecks;
        try {
            this.out = new PrintWriter(Files.newBufferedWriter(outputDir.resolve("player" + id + "_output.txt")));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create output file for player " + id, e);
        }
    }

    /** @return the player number (also the preferred denomination) */
    public int getId() {
        return id;
    }

    /** Adds a card to the hand during the initial deal. */
    public synchronized void dealCard(Card card) {
        hand.add(card);
    }

    /** @return a snapshot of the current hand */
    public synchronized List<Card> getHand() {
        return new ArrayList<>(hand);
    }

    /** @return the hand as space-separated values, e.g. {@code "1 1 2 4"} */
    public synchronized String handString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hand.size(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(hand.get(i).getValue());
        }
        return sb.toString();
    }

    /** @return true if the hand is exactly four cards of one value */
    public synchronized boolean hasWinningHand() {
        if (hand.size() != 4) {
            return false;
        }
        int first = hand.get(0).getValue();
        for (Card c : hand) {
            if (c.getValue() != first) {
                return false;
            }
        }
        return true;
    }

    /** Tries to claim victory; only the first player to call this succeeds. */
    private boolean declareWin() {
        return winner.compareAndSet(0, id);
    }

    /**
     * Performs one atomic draw-and-discard.
     *
     * @return false if the draw deck was empty and nothing happened
     */
    private boolean takeTurn() {
        Card drawn;
        Card discarded;
        String handAfter;
        CardDeck.lockPair(drawDeck, discardDeck);
        try {
            drawn = drawDeck.drawFromTop();
            if (drawn == null) {
                return false;
            }
            synchronized (this) {
                hand.add(drawn);
                discarded = hand.remove(indexToDiscard());
                handAfter = handString();
            }
            discardDeck.addToBottom(discarded);
        } finally {
            CardDeck.unlockPair(drawDeck, discardDeck);
        }
        out.println("player " + id + " draws a " + drawn + " from deck " + drawDeck.getId());
        out.println("player " + id + " discards a " + discarded + " to deck " + discardDeck.getId());
        out.println("player " + id + " current hand is " + handAfter);
        return true;
    }

    /** Index of the longest-held card that is not of the preferred denomination. */
    private int indexToDiscard() {
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).getValue() != id) {
                return i;
            }
        }
        // Unreachable: a hand of five preferred cards would already have been a win.
        throw new IllegalStateException("No non-preferred card to discard for player " + id);
    }

    @Override
    public void run() {
        boolean iWon = false;
        try {
            out.println("player " + id + " initial hand " + handString());
            if (hasWinningHand()) {
                iWon = declareWin();
            }
        } finally {
            initialChecks.countDown();
        }
        try {
            initialChecks.await();
            while (!iWon && winner.get() == 0) {
                if (!takeTurn()) {
                    // Draw deck momentarily empty: wait for the player on our left to discard.
                    LockSupport.parkNanos(50_000L);
                    continue;
                }
                if (hasWinningHand()) {
                    iWon = declareWin();
                    if (!iWon) {
                        break; // another player got there first
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            finish(iWon);
        }
    }

    private void finish(boolean iWon) {
        try {
            if (iWon) {
                System.out.println("player " + id + " wins");
                out.println("player " + id + " wins");
                out.println("player " + id + " exits");
                out.println("player " + id + " final hand: " + handString());
            } else {
                int w = winner.get();
                if (w != 0) {
                    out.println("player " + w + " has informed player " + id + " that player " + w + " has won");
                }
                out.println("player " + id + " exits");
                out.println("player " + id + " hand: " + handString());
            }
        } finally {
            out.close();
        }
    }
}
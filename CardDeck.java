import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A thread-safe FIFO deck of cards.
 *
 * <p>Cards are drawn from the top (front) and discarded to the bottom (back).
 * Every public method takes the deck's lock, so single operations are always
 * atomic. Because a player's "draw then discard" must be one atomic action
 * spanning two decks, the lock is exposed through {@link #lockPair} and
 * {@link #unlockPair}, which always acquire locks in ascending deck-id order
 * so that players around the ring can never deadlock on each other.
 */
public class CardDeck {

    private final int id;
    private final Deque<Card> cards = new ArrayDeque<>();
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * @param id the deck number (1..n)
     * @throws IllegalArgumentException if {@code id} is less than 1
     */
    public CardDeck(int id) {
        if (id < 1) {
            throw new IllegalArgumentException("Deck id must be at least 1: " + id);
        }
        this.id = id;
    }

    /** @return the deck number */
    public int getId() {
        return id;
    }

    /**
     * Adds a card to the bottom (back) of the deck.
     *
     * @param card the card to add; must not be null
     */
    public void addToBottom(Card card) {
        if (card == null) {
            throw new IllegalArgumentException("card must not be null");
        }
        lock.lock();
        try {
            cards.addLast(card);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes and returns the card at the top (front) of the deck.
     *
     * @return the top card, or {@code null} if the deck is empty
     */
    public Card drawFromTop() {
        lock.lock();
        try {
            return cards.pollFirst();
        } finally {
            lock.unlock();
        }
    }

    /** @return the number of cards currently in the deck */
    public int size() {
        lock.lock();
        try {
            return cards.size();
        } finally {
            lock.unlock();
        }
    }

    /** @return true if the deck holds no cards */
    public boolean isEmpty() {
        return size() == 0;
    }

    /** @return a snapshot of the deck contents, top card first */
    public List<Card> snapshot() {
        lock.lock();
        try {
            return new ArrayList<>(cards);
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return the contents line used in the deck output file,
     *         e.g. {@code "deck2 contents: 1 3 3 7"}
     */
    public String contentsLine() {
        StringBuilder sb = new StringBuilder("deck").append(id).append(" contents:");
        for (Card c : snapshot()) {
            sb.append(' ').append(c.getValue());
        }
        return sb.toString();
    }

    /**
     * Locks two decks in a globally consistent order (ascending id). If both
     * arguments are the same deck (a one-player game) it is locked once.
     */
    public static void lockPair(CardDeck a, CardDeck b) {
        if (a == b) {
            a.lock.lock();
            return;
        }
        CardDeck first = a.id <= b.id ? a : b;
        CardDeck second = first == a ? b : a;
        first.lock.lock();
        second.lock.lock();
    }

    /** Releases locks taken with {@link #lockPair}. */
    public static void unlockPair(CardDeck a, CardDeck b) {
        if (a == b) {
            a.lock.unlock();
            return;
        }
        b.lock.unlock();
        a.lock.unlock();
    }
}
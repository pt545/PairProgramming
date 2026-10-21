import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Behavioural tests for {@link Player}: the discard strategy, winning, exiting
 * when told someone else has won, and the exact format of the output file.
 */
public class PlayerTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Path dir() {
        return tmp.getRoot().toPath();
    }

    private final AtomicInteger winner = new AtomicInteger(0);

    private static CardDeck deckOf(int id, int... values) {
        CardDeck d = new CardDeck(id);
        for (int v : values) {
            d.addToBottom(new Card(v));
        }
        return d;
    }

    private Player player(int id, CardDeck draw, CardDeck discard, CountDownLatch latch, int... hand) {
        Player p = new Player(id, draw, discard, winner, latch, dir());
        for (int v : hand) {
            p.dealCard(new Card(v));
        }
        return p;
    }

    private List<String> outputOf(int id) throws IOException {
        return Files.readAllLines(dir().resolve("player" + id + "_output.txt"));
    }

    private static List<Integer> values(CardDeck d) {
        List<Integer> out = new ArrayList<>();
        for (Card c : d.snapshot()) {
            out.add(c.getValue());
        }
        return out;
    }

    private static void waitUntil(BooleanSupplier condition, long millis) throws InterruptedException {
        long end = System.currentTimeMillis() + millis;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < end) {
            Thread.sleep(5);
        }
    }

    // ---- hand inspection ---------------------------------------------------

    @Test
    public void fourEqualCardsIsAWinningHand() {
        Player p = player(1, new CardDeck(1), new CardDeck(2), new CountDownLatch(1), 4, 4, 4, 4);
        assertTrue(p.hasWinningHand());
    }

    @Test
    public void threeEqualCardsIsNotAWinningHand() {
        Player p = player(1, new CardDeck(1), new CardDeck(2), new CountDownLatch(1), 4, 4, 4, 5);
        assertFalse(p.hasWinningHand());
    }

    @Test
    public void winningHandNeedNotBeThePreferredDenomination() {
        // Specification FAQ 2: four of a kind wins even if it is not the player's own number.
        Player p = player(1, new CardDeck(1), new CardDeck(2), new CountDownLatch(1), 9, 9, 9, 9);
        assertTrue(p.hasWinningHand());
    }

    @Test
    public void getHandReturnsACopy() {
        Player p = player(1, new CardDeck(1), new CardDeck(2), new CountDownLatch(1), 1, 2, 3, 4);
        p.getHand().clear();
        assertEquals(4, p.getHand().size());
    }

    // ---- immediate win -----------------------------------------------------

    @Test(timeout = 10000)
    public void immediateWinIsDeclaredWithExactlyFourLinesAndNoTurnsTaken() throws IOException {
        CardDeck draw = deckOf(1, 1, 2);
        CardDeck discard = deckOf(2, 3);
        Player p = player(1, draw, discard, new CountDownLatch(1), 4, 4, 4, 4);

        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured));
        try {
            p.run();
        } finally {
            System.setOut(original);
        }

        assertEquals(1, winner.get());
        assertEquals(Arrays.asList(
                "player 1 initial hand 4 4 4 4",
                "player 1 wins",
                "player 1 exits",
                "player 1 final hand: 4 4 4 4"), outputOf(1));
        assertTrue("terminal should show the win", captured.toString().contains("player 1 wins"));
        assertEquals(Arrays.asList(1, 2), values(draw));
        assertEquals(Arrays.asList(3), values(discard));
    }

    // ---- strategy and exact output format ---------------------------------

    @Test(timeout = 10000)
    public void discardsLongestHeldNonPreferredCardAndLogsEveryAction() throws IOException {
        CardDeck draw = deckOf(1, 1, 1);
        CardDeck discard = new CardDeck(2);
        Player p = player(1, draw, discard, new CountDownLatch(1), 1, 1, 2, 3);

        p.run();

        // 2 was held longest among the non-preferred cards, so it goes first, then 3.
        assertEquals(Arrays.asList(2, 3), values(discard));
        assertTrue(draw.isEmpty());
        assertEquals("1 1 1 1", p.handString());
        assertEquals(1, winner.get());
        assertEquals(Arrays.asList(
                "player 1 initial hand 1 1 2 3",
                "player 1 draws a 1 from deck 1",
                "player 1 discards a 2 to deck 2",
                "player 1 current hand is 1 1 3 1",
                "player 1 draws a 1 from deck 1",
                "player 1 discards a 3 to deck 2",
                "player 1 current hand is 1 1 1 1",
                "player 1 wins",
                "player 1 exits",
                "player 1 final hand: 1 1 1 1"), outputOf(1));
    }

    @Test(timeout = 10000)
    public void neverDiscardsAPreferredDenominationAndKeepsFourCards() throws Exception {
        // Player 2 prefers 2s. Feed it only 7s: it must keep its 2s and cycle the others.
        CardDeck draw = deckOf(2, 7, 7, 7);
        CardDeck discard = new CardDeck(3);
        Player p = player(2, draw, discard, new CountDownLatch(1), 2, 2, 2, 5);
        Thread t = new Thread(p);
        t.start();

        waitUntil(() -> discard.size() == 3, 5000);
        winner.set(99); // stop the game: nobody can win with only 7s arriving
        t.join(5000);

        assertFalse(t.isAlive());
        assertEquals(Arrays.asList(5, 7, 7), values(discard));
        assertFalse("a preferred card was discarded", values(discard).contains(2));
        assertEquals(4, p.getHand().size());
    }

    // ---- being told another player has won -------------------------------

    @Test(timeout = 10000)
    public void exitsWithoutPlayingWhenAnotherPlayerHasAlreadyWon() throws IOException {
        winner.set(2);
        CardDeck draw = deckOf(1, 1, 2, 3);
        Player p = player(1, draw, new CardDeck(2), new CountDownLatch(1), 1, 5, 8, 11);

        p.run();

        assertEquals(Arrays.asList(
                "player 1 initial hand 1 5 8 11",
                "player 2 has informed player 1 that player 2 has won",
                "player 1 exits",
                "player 1 hand: 1 5 8 11"), outputOf(1));
        assertEquals("no turn should be taken", 3, draw.size());
    }

    // ---- empty draw deck ---------------------------------------------------

    @Test(timeout = 15000)
    public void waitsOnEmptyDeckThenContinuesWhenACardArrives() throws Exception {
        CardDeck draw = new CardDeck(1);
        CardDeck discard = new CardDeck(2);
        Player p = player(1, draw, discard, new CountDownLatch(1), 1, 1, 2, 3);
        Thread t = new Thread(p);
        t.start();

        Thread.sleep(200);
        assertTrue("should still be waiting", t.isAlive());
        assertEquals("1 1 2 3", p.handString());
        assertTrue(discard.isEmpty());

        draw.addToBottom(new Card(1));
        waitUntil(() -> discard.size() == 1, 5000);
        winner.set(9);
        t.join(5000);

        assertFalse(t.isAlive());
        assertEquals(Arrays.asList(2), values(discard));
        assertEquals("1 1 3 1", p.handString());
    }

    // ---- concurrency -------------------------------------------------------

    @Test(timeout = 10000)
    public void onlyOnePlayerCanDeclareVictoryEvenIfTwoStartWithWinningHands() throws Exception {
        CountDownLatch latch = new CountDownLatch(2);
        Player p1 = player(1, new CardDeck(1), new CardDeck(2), latch, 3, 3, 3, 3);
        Player p2 = player(2, new CardDeck(2), new CardDeck(1), latch, 4, 4, 4, 4);

        PrintStream original = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));
        try {
            Thread t1 = new Thread(p1);
            Thread t2 = new Thread(p2);
            t1.start();
            t2.start();
            t1.join();
            t2.join();
        } finally {
            System.setOut(original);
        }

        int winners = 0;
        for (int id = 1; id <= 2; id++) {
            List<String> lines = outputOf(id);
            if (lines.contains("player " + id + " wins")) {
                winners++;
            }
        }
        assertEquals(1, winners);
        assertTrue(winner.get() == 1 || winner.get() == 2);
    }

    @Test(timeout = 30000)
    public void handNeverObservedWithAnyOtherSizeThanFourWhileGameRuns() throws Exception {
        // A three-player ring is built by hand so an observer thread can watch every hand.
        int n = 3;
        List<Integer> pack = new ArrayList<>();
        for (int v = 1; v <= n; v++) {
            for (int c = 0; c < 8; c++) {
                pack.add(v);
            }
        }
        Collections.shuffle(pack, new Random(11));

        List<CardDeck> decks = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            decks.add(new CardDeck(i));
        }
        CountDownLatch latch = new CountDownLatch(n);
        List<Player> players = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            players.add(new Player(i, decks.get(i - 1), decks.get(i % n), winner, latch, dir()));
        }
        int next = 0;
        for (int round = 0; round < 4; round++) {
            for (Player p : players) {
                p.dealCard(new Card(pack.get(next++)));
            }
        }
        for (int k = 0; next < pack.size(); k++) {
            decks.get(k % n).addToBottom(new Card(pack.get(next++)));
        }

        PrintStream original = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));
        List<Thread> threads = new ArrayList<>();
        int violations = 0;
        try {
            for (Player p : players) {
                Thread t = new Thread(p);
                threads.add(t);
                t.start();
            }
            boolean anyAlive = true;
            while (anyAlive) {
                for (Player p : players) {
                    if (p.getHand().size() != 4) {
                        violations++;
                    }
                }
                anyAlive = false;
                for (Thread t : threads) {
                    anyAlive |= t.isAlive();
                }
            }
        } finally {
            System.setOut(original);
        }

        assertEquals("a hand was observed with a size other than four", 0, violations);
        assertTrue(winner.get() >= 1 && winner.get() <= n);
    }
}
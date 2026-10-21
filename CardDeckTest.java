import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

/** Tests for {@link CardDeck}: FIFO behaviour, output format and thread safety. */
public class CardDeckTest {

    private static CardDeck deckOf(int id, int... values) {
        CardDeck d = new CardDeck(id);
        for (int v : values) {
            d.addToBottom(new Card(v));
        }
        return d;
    }

    // ---- FIFO behaviour ----------------------------------------------------

    @Test
    public void drawsFromTopInInsertionOrder() {
        CardDeck d = deckOf(1, 5, 6, 7);
        assertEquals(5, d.drawFromTop().getValue());
        assertEquals(6, d.drawFromTop().getValue());
        assertEquals(7, d.drawFromTop().getValue());
    }

    @Test
    public void discardedCardGoesToTheBottom() {
        CardDeck d = deckOf(1, 5, 6);
        d.addToBottom(new Card(9));
        assertEquals(5, d.drawFromTop().getValue());
        assertEquals(6, d.drawFromTop().getValue());
        assertEquals(9, d.drawFromTop().getValue());
    }

    @Test
    public void drawingFromEmptyDeckReturnsNullAndLeavesItEmpty() {
        CardDeck d = new CardDeck(1);
        assertNull(d.drawFromTop());
        assertTrue(d.isEmpty());
        assertEquals(0, d.size());
    }

    @Test
    public void sizeTracksAddsAndDraws() {
        CardDeck d = deckOf(1, 1, 2, 3, 4);
        assertEquals(4, d.size());
        d.drawFromTop();
        assertEquals(3, d.size());
        d.addToBottom(new Card(8));
        assertEquals(4, d.size());
    }

    @Test
    public void duplicateValuesAreKeptAsSeparateCards() {
        CardDeck d = deckOf(1, 3, 3, 3);
        assertEquals(3, d.size());
        d.drawFromTop();
        assertEquals(2, d.size());
    }

    // ---- snapshot and output format ---------------------------------------

    @Test
    public void snapshotIsTopFirstAndDoesNotExposeInternalState() {
        CardDeck d = deckOf(1, 1, 2, 3);
        List<Card> snap = d.snapshot();
        assertEquals(3, snap.size());
        assertEquals(1, snap.get(0).getValue());
        snap.clear(); // mutating the copy must not touch the deck
        assertEquals(3, d.size());
    }

    @Test
    public void snapshotReturnsNewListEachTime() {
        CardDeck d = deckOf(1, 1);
        assertNotSame(d.snapshot(), d.snapshot());
    }

    @Test
    public void contentsLineMatchesSpecificationFormat() {
        assertEquals("deck2 contents: 1 3 3 7", deckOf(2, 1, 3, 3, 7).contentsLine());
    }

    @Test
    public void contentsLineOfEmptyDeckHasNoCards() {
        assertEquals("deck3 contents:", new CardDeck(3).contentsLine());
    }

    // ---- invalid input -----------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void nullCardIsRejected() {
        new CardDeck(1).addToBottom(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deckNumberBelowOneIsRejected() {
        new CardDeck(0);
    }

    // ---- thread safety -----------------------------------------------------

    @Test(timeout = 20000)
    public void concurrentAddsLoseNoCards() throws InterruptedException {
        final CardDeck d = new CardDeck(1);
        int threads = 8;
        final int perThread = 2000;
        List<Thread> ts = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            Thread th = new Thread(() -> {
                for (int i = 0; i < perThread; i++) {
                    d.addToBottom(new Card(i));
                }
            });
            ts.add(th);
            th.start();
        }
        for (Thread th : ts) {
            th.join();
        }
        assertEquals(threads * perThread, d.size());
    }

    @Test(timeout = 20000)
    public void concurrentDrawsHandOutEveryCardExactlyOnce() throws InterruptedException {
        final CardDeck d = new CardDeck(1);
        int total = 5000;
        for (int i = 0; i < total; i++) {
            d.addToBottom(new Card(i)); // distinct values so duplicates are detectable
        }
        final Queue<Integer> drawn = new ConcurrentLinkedQueue<>();
        List<Thread> ts = new ArrayList<>();
        for (int t = 0; t < 8; t++) {
            Thread th = new Thread(() -> {
                Card c;
                while ((c = d.drawFromTop()) != null) {
                    drawn.add(c.getValue());
                }
            });
            ts.add(th);
            th.start();
        }
        for (Thread th : ts) {
            th.join();
        }
        Set<Integer> unique = new HashSet<>(drawn);
        assertEquals(total, drawn.size());
        assertEquals("a card was drawn twice", total, unique.size());
        assertTrue(d.isEmpty());
    }

    @Test(timeout = 20000)
    public void lockingTwoDecksInOppositeOrdersDoesNotDeadlock() throws InterruptedException {
        final CardDeck a = new CardDeck(1);
        final CardDeck b = new CardDeck(2);
        final CountDownLatch go = new CountDownLatch(1);
        Thread t1 = new Thread(() -> lockRepeatedly(go, a, b));
        Thread t2 = new Thread(() -> lockRepeatedly(go, b, a)); // reversed, as in a ring
        t1.start();
        t2.start();
        go.countDown();
        t1.join();
        t2.join();
        assertFalse(t1.isAlive() || t2.isAlive());
    }

    private static void lockRepeatedly(CountDownLatch go, CardDeck x, CardDeck y) {
        try {
            go.await();
        } catch (InterruptedException e) {
            return;
        }
        for (int i = 0; i < 20000; i++) {
            CardDeck.lockPair(x, y);
            CardDeck.unlockPair(x, y);
        }
    }

    @Test
    public void lockingTheSameDeckTwiceAsPairWorksForOnePlayerGames() {
        CardDeck d = deckOf(1, 1, 2);
        CardDeck.lockPair(d, d);
        try {
            d.addToBottom(d.drawFromTop()); // reentrant: still allowed while locked
        } finally {
            CardDeck.unlockPair(d, d);
        }
        assertEquals(2, d.size());
        // lock fully released: another thread can use the deck
        Thread other = new Thread(() -> d.drawFromTop());
        other.start();
        try {
            other.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertFalse(other.isAlive());
    }
}
package TestingSuite;

import Main.Card;
import Main.CardDeck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

public class TestCardDeck {

    CardDeck deck;

    public TestCardDeck(){
        deck = new CardDeck(2);
    }

    @Test
    public void TestFirstInFirstOut(){

        for(int value : new int[] {5, 6, 7}){
            deck.addToBottom(new Card(value));
        }
        assertEquals(3, deck.size());

        // cards come off the top in the order they went in
        assertEquals(5, deck.drawFromTop().getValue());
        assertEquals(6, deck.drawFromTop().getValue());
        assertEquals(7, deck.drawFromTop().getValue());

        // drawing from an empty deck gives null, not an error
        assertTrue(deck.isEmpty());
        assertNull(deck.drawFromTop());
    }

    @Test
    public void TestContentsLine(){

        // this is the line that is written to deck2_output.txt at the end of the game
        assertEquals("deck2 contents:", deck.contentsLine());

        for(int value : new int[] {1, 3, 3, 7}){
            deck.addToBottom(new Card(value));
        }
        assertEquals("deck2 contents: 1 3 3 7", deck.contentsLine());
    }

    @Test
    @Timeout(30)
    public void TestManyThreadsAtOnce() throws InterruptedException{

        int threadCount = 8;
        int perThread = 20000;
        int total = threadCount * perThread;

        // 8 threads each add 20000 different cards at the same time
        List<Thread> threads = new ArrayList<>();
        for(int t = 0; t < threadCount; t++){
            int start = t * perThread; 
            Thread thread = new Thread(() -> {
                for(int i = 0; i < perThread; i++){
                    deck.addToBottom(new Card(start + i));
                }
            });
            threads.add(thread);
            thread.start();
        }
        for(Thread thread : threads){
            thread.join();
        }
        assertEquals(total, deck.size()); // no card was lost

        // 8 threads draw cards until the deck is empty
        List<Integer> drawn = Collections.synchronizedList(new ArrayList<>());
        threads = new ArrayList<>();
        for(int t = 0; t < threadCount; t++){
            Thread thread = new Thread(() -> {
                Card card = deck.drawFromTop();
                while(card != null){
                    drawn.add(card.getValue());
                    card = deck.drawFromTop();
                }
            });
            threads.add(thread);
            thread.start();
        }
        for(Thread thread : threads){
            thread.join();
        }

        assertEquals(total, drawn.size());                // every card was drawn
        assertEquals(total, new HashSet<>(drawn).size()); // and none was drawn twice
    }
}

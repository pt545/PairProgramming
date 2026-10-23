package Main;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * CardDeck
 *
 * A deck of cards that sits between two players. Players take a card from the
 * TOP of the deck and put a card on the BOTTOM of the deck (like a queue: the
 * first card in is the first card out).
 *
 * Several player threads use the same deck at once, so every method is
 * "synchronized". That means only one thread at a time can be running any of
 * the deck's methods, which stops two players taking the same card or
 * corrupting the list.
 */
public class CardDeck {

    private final int id;                                  // deck number: 1, 2, 3...
    private final LinkedList<Card> cards = new LinkedList<>(); // front of the list = top of the deck

    /**
     * @param id the deck number, starting from 1
     */
    public CardDeck(int id) {
        if (id < 1) {
            throw new IllegalArgumentException("Deck number must be at least 1: " + id);
        }
        this.id = id;
    }

    /**
     * @return the deck number
     */
    public int getId() {
        return id;
    }

    /**
     * Puts a card on the bottom of the deck.
     *
     * @param card the card to add (must not be null)
     */
    public synchronized void addToBottom(Card card) {
        if (card == null) {
            throw new IllegalArgumentException("Card must not be null");
        }
        cards.addLast(card);
    }

    /**
     * Takes the card from the top of the deck.
     *
     * @return the top card, or null if the deck is empty
     */
    public synchronized Card drawFromTop() {
        if (cards.isEmpty()) {
            return null;
        }
        return cards.removeFirst();
    }

    /**
     * @return how many cards are in the deck right now
     */
    public synchronized int size() {
        return cards.size();
    }

    /**
     * @return true if there are no cards in the deck
     */
    public synchronized boolean isEmpty() {
        return cards.isEmpty();
    }

    /**
     * @return a COPY of the cards, top card first. It is a copy so that other
     *         classes cannot change the real deck by accident.
     */
    public synchronized List<Card> getCards() {
        return new ArrayList<>(cards);
    }

    /**
     * @return the line written to deckX_output.txt at the end of the game,
     *         for example "deck2 contents: 1 3 3 7"
     */
    public synchronized String contentsLine() {
        String line = "deck" + id + " contents:";
        for (Card card : cards) {
            line = line + " " + card.getValue();
        }
        return line;
    }
}

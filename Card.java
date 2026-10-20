/**
-Represent each card 
-Stores the card denomination
-Provides the other classes to read that denomination.
 */
public final class Card {

    private final int value;

    /**
     * Creates a card.
     *
     * @param value the face value (denomination); must be non-negative
     * @throws IllegalArgumentException if {@code value} is negative
     */
    public Card(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Card value must be non-negative: " + value);
        }
        this.value = value;
    }

    /** @return the face value of this card */
    public int getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Card)) {
            return false;
        }
        return value == ((Card) other).value;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(value);
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
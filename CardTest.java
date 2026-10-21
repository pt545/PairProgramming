import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** 
 * Tests for {@link Card}: construction rules, value semantics and printing. */
public class CardTest {

    @Test
    public void storesFaceValue() {
        assertEquals(7, new Card(7).getValue());
    }

    @Test
    public void zeroIsAValidDenomination() {
        assertEquals(0, new Card(0).getValue());
    }

    @Test
    public void valueAboveNumberOfPlayersIsLegal() {
        // The specification explicitly allows face values larger than n.
        assertEquals(1000, new Card(1000).getValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeValueIsRejected() {
        new Card(-1);
    }

    @Test
    public void cardsWithSameValueAreEqualAndShareHashCode() {
        Card a = new Card(3);
        Card b = new Card(3);
        assertEquals(a, b);
        assertEquals(b, a);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void cardsWithDifferentValuesAreNotEqual() {
        assertNotEquals(new Card(3), new Card(4));
    }

    @Test
    public void cardIsNotEqualToNullOrOtherTypes() {
        Card c = new Card(3);
        assertTrue(!c.equals(null));
        assertTrue(!c.equals("3"));
    }

    @Test
    public void toStringIsJustTheValue() {
        // The output files rely on this: "player 1 draws a 4 from deck 1".
        assertEquals("12", new Card(12).toString());
    }
}
package TestingSuite;

import  static org.junit.Assert.assertEquals;
import org.junit.Test;

import Main.Card;


public class TestCard {

    @Test 
    public void TestGetValue(){
        Card card = new Card(1);
        assertEquals(card.getValue() , 1);
    }
}

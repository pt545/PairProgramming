package TestingSuite;

import Main.Card;
import Main.Player;
import Main.CardDeck;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.File;

import org.junit.jupiter.api.Test;

public class TestPlayer {
    



    public TestPlayer(){
        
    }


    public List<String> readFile(String filename) throws IOException{
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line = reader.readLine();
            while (line != null) {
                lines.add(line);
                line = reader.readLine();
            }
            reader.close();
        }
        
        return lines;
    }
    

    @Test
    public void TestDealCard(){ 
        // checks that the cards given to the player will end up in their hand in proper order
        Player testplayer = new Player(1, null, null, null, null);
        Card[] testhand = {new Card(1),new Card(2),new Card(3),new Card(4)};
        for(int i = 0; i <4;i++){
            testplayer.dealCard(testhand[i]);
        }
        assertEquals("[ 1 2 3 4 ]", testplayer.getHand());
    }

    @Test
    public void TestOutputFileisCreated(){
        // check that after a game is finished there is an output file
        CardDeck testdeck = new CardDeck(1);
        for(int i = 0; i < 4; i++){
            testdeck.addToBottom(new Card(1));
        }

        Player testplayer = new Player(1,testdeck,testdeck,new AtomicInteger(0),"./TestingSuite/");

        Card[] testhand = {new Card(1),new Card(1),new Card(1),new Card(1)};
        for(int i = 0; i <4;i++){
            testplayer.dealCard(testhand[i]);
        }

        testplayer.run();
        File outputfile = new File("./TestingSuite/player1_output.txt");
        assertTrue(outputfile.exists());
    }

    @Test 
    public void TestRunWinCond(){
        // checks wether the win conditions will end the players game properly
        CardDeck testdeck = new CardDeck(1);
        for(int i = 0; i < 4; i++){
            testdeck.addToBottom(new Card(1));
        }

        Player testplayer = new Player(1,testdeck,testdeck,new AtomicInteger(0),"./TestingSuite/");

        Card[] testhand = {new Card(1),new Card(1),new Card(1),new Card(1)};
        for(int i = 0; i <4;i++){
            testplayer.dealCard(testhand[i]);
        }

        testplayer.run();

        List<String> playeroutput = new ArrayList<String>(){};

        List<String> expectedoutput = new ArrayList<String>(){};
        try{
            playeroutput = readFile( "./TestingSuite/player1_output.txt");
            expectedoutput = readFile("./TestingSuite/immidiatewinoutput.txt");
        }catch(IOException e){
            assertTrue(false);
        }
        //Check if the player will produce a valid output file

        assertEquals(expectedoutput, playeroutput);        
        //immidiatewinoutput.txt contains the expected result for a one player game; which for simplicity's sake is an immidiate win
    }  

    @Test
    public void TestCardDraw(){

        //Test that a card will be drawn if a player does not have a winning hand, and that the card is not the prioritised card
            CardDeck testdeck = new CardDeck(2);
        for(int i = 0; i < 4; i++){
            testdeck.addToBottom(new Card(2));
        }

        Player testplayer = new Player(2,testdeck,testdeck,new AtomicInteger(0),"./TestingSuite/");

        Card[] testhand = {new Card(2),new Card(2),new Card(2),new Card(1)};
        for(int i = 0; i <4;i++){
            testplayer.dealCard(testhand[i]);
        }

        testplayer.run();

        List<String> playeroutput = new ArrayList<String>(){};

        try{
            playeroutput = readFile( "./TestingSuite/player2_output.txt");
        }catch(IOException e){
            assertTrue(false);
        }
        assertTrue(playeroutput.contains("Player 2 has added 2 to its hand from deck 2") && playeroutput.contains("Player 2 has discarded 1 to deck 2"));

    }


}

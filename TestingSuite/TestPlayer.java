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

import org.junit.jupiter.api.Test;

public class TestPlayer {
    

    Player testplayer;
    Card[] testhand;
    CardDeck testdeck;

    public TestPlayer(){
        
        testdeck = new CardDeck(1);
        for(int i =0;i < 4;i++){
            testdeck.addToBottom(new Card(1));
        }
        testplayer = new Player(1, testdeck, testdeck, new AtomicInteger(0), "./TestingSuite/");

        testhand = new Card[4];
        for(int i = 0; i <4;i++){
            testhand[i] = new Card(1);
        }

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

        for(int i = 0; i <4;i++){
            testplayer.dealCard(testhand[i]);
        }
        assertEquals("[ 1 1 1 1 ]", testplayer.getHand());
    }

    @Test 
    public void TestRunWinCond(){

        //Player will immidiately win
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

        assertEquals(expectedoutput, playeroutput);        
    }
    //must check others change later   
}

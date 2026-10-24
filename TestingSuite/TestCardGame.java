package TestingSuite;

import Main.Card;
import Main.CardGame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

public class TestCardGame {

    String folder = "./TestingSuite/cardgame_output"; 
  
    public TestCardGame(){
        new File(folder).mkdirs();
        for(File old : new File(folder).listFiles()){
            old.delete();
        }
    }

    public List<String> lines(String filename) throws IOException{
        return Files.readAllLines(Paths.get(folder + "/" + filename));
    }

    public List<Card> load(String text, int n) throws IOException{
        Files.write(Paths.get(folder + "/pack.txt"), text.getBytes());
        return CardGame.loadPack(folder + "/pack.txt", n);
    }

    public int playGame(int n, List<Card> pack) throws IOException{
        PrintStream realOut = System.out;
        System.setOut(new PrintStream(new OutputStream(){
            public void write(int b){ }
        }));
        try{
            return CardGame.play(n, pack, folder);
        }finally{
            System.setOut(realOut);
        }
    }



    @Test
    public void TestLoadPack() throws IOException{

        // a valid pack for 2 players: 16 lines, 0 to 15, kept in file order
        String text = "";
        for(int i = 0; i < 16; i++){
            text += i + "\n";
        }
        List<Card> pack = load(text, 2);
        assertEquals(16, pack.size());
        for(int i = 0; i < 16; i++){
            assertEquals(i, pack.get(i).getValue());
        }

        // wrong number of lines (2 players need exactly 16)
        assertThrows(IllegalArgumentException.class, () -> load("1\n".repeat(15), 2));
        assertThrows(IllegalArgumentException.class, () -> load("1\n".repeat(17), 2));

        // a line that is not a non-negative whole number (line 5 of 8 is the bad one)
        for(String bad : new String[] {"abc", "-3", "2.5", "", "1 2"}){
            String badPack = "1\n".repeat(4) + bad + "\n" + "1\n".repeat(3);
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> load(badPack, 1));
            assertTrue(e.getMessage().contains("line 5"));
        }

        // a file that does not exist
        assertThrows(IOException.class, () -> CardGame.loadPack(folder + "/no_such_pack.txt", 2));
    }

    @Test
    @Timeout(30)
    public void TestImmediateWin() throws IOException{

        int[] values = {1, 2, 1, 2, 1, 2, 3, 2,  3, 3, 3, 3, 3, 3, 3, 3};
        List<Card> pack = new ArrayList<>();
        for(int value : values){
            pack.add(new Card(value));
        }

        assertEquals(2, playGame(2, pack));

        // the winner never takes a turn, so its file is always exactly the same
        List<String> expected = Files.readAllLines(Paths.get("./TestingSuite/cardgame_immediatewin_player2.txt"));
        assertEquals(expected, lines("player2_output.txt"));

        // the loser is told who won
        assertTrue(lines("player1_output.txt").toString().contains("player 2 has won"));
    }

    @Test
    @Timeout(60)
    public void TestWholeGame() throws IOException{

        // 4 players, 8 cards of each value 1 to 4, shuffled
        int n = 4;
        List<Card> pack = new ArrayList<>();
        for(int v = 1; v <= n; v++){
            for(int copy = 0; copy < 8; copy++){
                pack.add(new Card(v));
            }
        }
        Collections.shuffle(pack, new Random(77));

        int winner = playGame(n, pack);

        assertTrue(winner >= 1 && winner <= n);

        int cardsInDecks = 0;
        for(int i = 1; i <= n; i++){
            List<String> player = lines("player" + i + "_output.txt");

            // The first 4n cards are dealt round-robin, so player i's hand is cards
            // i-1, i-1+n, i-1+2n, i-1+3n. It is on the first line of the file.
            String hand = "";
            for(int k = 0; k < 4; k++){
                hand += pack.get(i - 1 + k * n).getValue() + " ";
            }
            assertTrue(player.get(0).contains(hand.trim()), "wrong hand for player " + i);

            int draws = 0;
            for(String line : player){
                if(line.contains("from deck") && draws < 4){
                    // a draw line has three numbers: the player, the card drawn, the deck
                    int cardDrawn = Integer.parseInt(line.replaceAll("[^0-9]+", " ").trim().split(" ")[1]);
                    assertEquals(pack.get(4 * n + (i - 1) + draws * n).getValue(), cardDrawn, "player " + i + " draw " + (draws + 1));
                    draws++;
                }
            }

            // only the winner's file says "wins", everybody else is told who won
            assertEquals(i == winner, player.toString().contains("wins"));
            assertEquals(i != winner, player.toString().contains("player " + winner + " has won"));

            // deckX_output.txt looks like "deck2 contents: 1 3 3 7", count the cards in it
            String deck = lines("deck" + i + "_output.txt").get(0);
            String cards = deck.substring(deck.indexOf(":") + 1).trim();
            if(cards.length() > 0){
                cardsInDecks += cards.split("\\s+").length;
            }
        }

        assertEquals(4 * n, cardsInDecks);
    }
}

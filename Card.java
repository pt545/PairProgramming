import java.io.File;                  
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Card{
    
    Deck[] decks;
    ArrayList<Integer> pack = new ArrayList<Integer>();
    int nPlayers;

    public Card(int nPlayers){
        this.nPlayers = nPlayers;
        decks = new Deck[nPlayers];
        for(int i = 0; i < nPlayers; i++){
            decks[i] = new Deck();
        }
    }


    public int[][] DealDecks(){
        int[][] hands = new int[nPlayers][4];
        // hands for the players
        int counter = 0;
        for(int x = 0; x < 4; x++){
            for(int y = 0; y < nPlayers;y++){
                hands[y][x] = pack.get(counter);
                counter++;
            }
        }
        

        //

        for (int[] row : hands) {
            System.out.println(Arrays.toString(row));
        }
        System.out.println("hands");
        //

        //dividing among the decks
        for(int x = 0; x < 4; x++){
            for(int y = 0; y < nPlayers;y++){
                //System.out.println(counter);
                decks[y].AddtoBottom(pack.get(counter));
                counter++;
            }
        }

        for (Deck d : decks) {
            System.out.println(d.toString());
        }
        System.out.println("decks");
        return hands;
        //In practice cards are divided evenly between the hands and the decks


    }

    public boolean readFile (String filename) throws IOException{
        
        
        try{
            List<String> fs = Files.readAllLines(Paths.get(filename));
            int lines = (int)fs.size();

            System.out.println(lines);
            fs.forEach(System.out::print);

            for(int i = 0; i < lines;i++){
                int p = Integer.parseInt(fs.get(i));
                if(p > nPlayers || p < 0){
                    System.out.println("Invalid value present");
                    return false;
                }
                // may lead to problems with leftover values    
                pack.add(p);
            }

            if(lines != 8 * nPlayers){
                System.out.println("File length not valid");
                return false;
            }
                
        }catch(FileNotFoundException e){
            System.out.println("File not found");
            return false;
        }


        


        return true;
    }


    public synchronized int AtomicDrawDiscard(int LeftIndex,int RightIndex,int discardCard){

        if (decks[LeftIndex].size() > 0) {
            decks[RightIndex].AddtoBottom(discardCard);
            return decks[LeftIndex].GivefromTop();
        }

        return -1;
        // if this cannot be done then there is an implicit wait until the next loop of 
        //since this is atomic

    }
}

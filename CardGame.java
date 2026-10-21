import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class CardGame{
    
    public static void main(){
        int nPlayers;
        Player[] players;
        Card cards;
        String winner;
        boolean win;
        String filename;


        Scanner reader = new Scanner(System.in);
        System.out.println("Input the number of players:");
        do{
            nPlayers = reader.nextInt();
        }while(nPlayers < 0);

        System.out.println("Input the pack filename:");
        cards = new Card(nPlayers);
        boolean readable = false;
        
        while(!readable){
            filename = reader.next();
            try{
                readable = cards.readFile(filename);
            }catch(IOException e){
                System.out.println("File not valid");
            }
        }


        int[][] hands = cards.DealDecks();
        AtomicInteger wincond = new AtomicInteger(0);
        // dimensions 4 x nPlayers
        //now initialise the players
        players = new Player[nPlayers];
        for(int i = 0; i < nPlayers;i++){
            int RightIndex = (i-1 > -1) ? i-1 : nPlayers - 1;
            players[i] = new Player(cards,hands[i],i,i,RightIndex, wincond);
            System.out.println(i + " " + i +" "+RightIndex);
        }

        Thread[] threads = new Thread[nPlayers];
        for(int i = 0; i < nPlayers; i++){
            threads[i] = new Thread(players[i]);
        }
        for(int i = 0; i < nPlayers;i++){
            threads[i].start();
        }
    
        //start the threads of each player   
    }
}
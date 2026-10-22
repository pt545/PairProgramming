import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;


public class Player implements  Runnable{

    private final AtomicInteger wincond;
    private int playerid; //doubles as prefered value
    private CardDeck LeftDeck;
    private CardDeck RightDeck;
    private CountDownLatch Gate;
    private String Folder;
    private Card[] hand;
    private int endIndex;
    private String log;


    public Player(int playerid,CardDeck LeftDeck, CardDeck RightDeck, AtomicInteger initialcond,CountDownLatch startGate,String folder){
        
        this.playerid = playerid;
        this.wincond = initialcond;
        this.LeftDeck = LeftDeck;
        this.RightDeck = RightDeck;
        Gate = startGate;
        Folder = folder;
        hand = new Card[4];
        endIndex = 0;
        log = "";
        //System.out.println("Initialised");
    }


    public synchronized void dealCard(Card card){
        hand[endIndex] = card;
        endIndex++;
    }

    private synchronized void addToLog(String logAdd){
        log += logAdd + "\n";
    }


    public synchronized void Win(){
        wincond.set(playerid);
    }

    public synchronized boolean checkWin(){
        
        for(int i = 1; i < 4; i++){
            if (hand[i-1].getValue() != hand[i].getValue()) {
                return false;
            }
        }
        return  true;
    }

    public synchronized String getHand(){
        String handstr = "[";
        for(int i = 0; i < 4;i++){
            handstr +=  hand[i].getValue()+ " ";
        }
        handstr += "]";

        return handstr;
    }

    public synchronized void writeLogtoFile() throws IOException{
        try (PrintWriter writer = new PrintWriter(Folder + "/player" + playerid + "_output.txt")) {
            writer.print(log);
        }
    }

    @Override 
    public synchronized void run(){
        System.out.println("Player " + playerid + " has joined the game with the hand " + getHand());
        Random r = new Random();
        int randInd = r.nextInt(4);
        while(wincond.intValue() == 0){
            
            String actionslog = "";
            //check win 
            //System.out.println(getHand());
            if(checkWin()){
                //System.out.println("checkwin");
                wincond.set(playerid);
                continue;
            }
            //draw and discard
            //get a random value that is not equal to the prefered value (playerid)
            // System.out.println("aaaaaa");
            do{
                randInd = r.nextInt(4);
                //System.out.println("Choose loop " +playerid + " " + randInd + " " + hand[randInd].getValue());
            }while (hand[randInd].getValue() == playerid);

            actionslog += "Player " + playerid + " has added " + hand[randInd].getValue() + " to its hand " + getHand();

            //System.out.println("add to bottom");
            RightDeck.addToBottom(hand[randInd]);

            //System.out.println("Draw from top");
            hand[randInd] = LeftDeck.drawFromTop();

            addToLog(actionslog);
            System.out.println(actionslog);

        }
        String winlog;
        if(wincond.intValue() == playerid){
            winlog = "Player " + playerid + " wins ";
            winlog += "with the hand " + getHand();
        }else{
            winlog = "Player " + wincond.intValue() + " has informed " + playerid + " that player " + playerid + " has won";
        }
        addToLog(winlog);
        System.out.println(winlog);

        try{
            writeLogtoFile();
        }catch(IOException e){
            System.out.println("Unable to write log for player " + playerid);
        }
        
    }
}


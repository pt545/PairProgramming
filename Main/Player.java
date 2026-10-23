package Main;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;


public class Player implements  Runnable{

    private final AtomicInteger wincond;
    private int playerid; //doubles as prefered value
    private CardDeck LeftDeck;
    private CardDeck RightDeck;
    private String Folder;
    private Card[] hand;
    private int endIndex;
    private String log;


    public Player(int playerid,CardDeck LeftDeck, CardDeck RightDeck, AtomicInteger initialcond,String folder){
        
        this.playerid = playerid; // player identifier
        this.wincond = initialcond; // doubles as a stop condition and identifies who wins
        this.LeftDeck = LeftDeck; // draw from this deck
        this.RightDeck = RightDeck; // discard to this deck
        Folder = folder; // where to write the output log files
        hand = new Card[4]; //hard set as hand size must always equal 4.
        endIndex = 0;
        log = "";
    }



    public synchronized void dealCard(Card card){
        hand[endIndex] = card;
        endIndex++;
    }

    private synchronized void addToLog(String logAdd){
        log += logAdd + "\n";
    }


    private synchronized void Win(){
        wincond.set(playerid);
    }

    private synchronized boolean checkWin(){
        
        for(int i = 1; i < 4; i++){
            if (hand[i-1].getValue() != hand[i].getValue()) {
                return false;
            }
        }
        return  true;
    }

    public synchronized String getHand(){
        String handstr = "[ ";
        for(int i = 0; i < 4;i++){
            handstr +=  hand[i].getValue()+ " ";
        }
        handstr += "]";

        return handstr;
    }

    private synchronized void writeLogtoFile() throws IOException{
        try (PrintWriter writer = new PrintWriter(Folder + "/player" + playerid + "_output.txt")) {
            writer.print(log);
        }
    }

    // This must be an atomic action
    private synchronized void DrawDiscard(int randInd){
        String actionslog = "";
        if (!LeftDeck.isEmpty()) {
                RightDeck.addToBottom(hand[randInd]);
                actionslog += "Player " + playerid + " has discarded " + hand[randInd].getValue() + " to deck " + RightDeck.getId() + "\n";
                hand[randInd] = LeftDeck.drawFromTop();
                actionslog += "Player " + playerid + " has added " + hand[randInd].getValue() + " to its hand " + "from deck " + LeftDeck.getId();

        }else{
                actionslog += "Player " + playerid+ " unable do draw a card due to deck " +LeftDeck.getId() +" not containing any cards";
        }

        addToLog(actionslog);
        System.out.println(actionslog);
    }

    @Override 
    public synchronized void run(){
        String entry = "Player " + playerid + " has joined the game with the hand " + getHand();
        System.out.println(entry);
        addToLog(entry);

        Random r = new Random();
        int randInd = r.nextInt(4);

        while(wincond.intValue() == 0){

            if(checkWin() && wincond.intValue() == 0){
                Win();
                continue;
            }
            //draw and discard
            //get a random value that is not equal to the prefered value (playerid)
            do{
                randInd = r.nextInt(4);
                
            }while (hand[randInd].getValue() == playerid);

            DrawDiscard(randInd);

        }
        String winlog;
        if(wincond.intValue() == playerid){
            winlog = "Player " + playerid + " wins ";
            winlog += "with the hand " + getHand();
        }else{
            winlog = "Player " + wincond.intValue() + " has informed " + playerid + " that player " + wincond.intValue() + " has won";
        }
        addToLog(winlog);
        System.out.println(winlog);

        try{
            writeLogtoFile();
        }catch(IOException e){
            System.out.println("Unable to write log for player " + playerid);
        }
        
        System.out.println("Player " + playerid + " exits");
    }
}


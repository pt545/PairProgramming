import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;


public class Player implements  Runnable{
    private final AtomicInteger wincond;
    private static boolean win = false;
    private int playerid; //doubles as prefered value
    private int LeftIndex;
    private int RightIndex;
    private int[] hand;
    private Card card;
    



    public Player(Card card,int[] hand, int playerid,int LeftIndex, int RightIndex, AtomicInteger initialcond){
        this.hand = hand;
        this.card = card;
        this.playerid = playerid;
        this.LeftIndex = LeftIndex;
        this.RightIndex = RightIndex;
        this.wincond = initialcond;
        
    }

    
    public void checkWin(){
        //formerly synchronised
        for(int i = 1; i < 4; i++){
            if (hand[i-1] != hand[i]) {
                return;
            }
        }
        System.out.println("Player" + (playerid + 1) + " wins" + Arrays.toString(hand));
        win = wincond.compareAndSet(0, playerid);
        // then tell other threads 

    }

    @Override 
    public synchronized void run(){
        //formerly synchronised
        //choose card with the lowest index thats not a prefered value
        // there is one since we haven't won.
        try{
        
        int counter = 0;
            while (wincond.intValue() == 0) {
                counter++;
                int index = 0;
                for(int i = 0; i < 4; i++){
                    if(hand[i] != playerid + 1){
                        index = i;
                    }
                }
                
                int replacement = card.AtomicDrawDiscard(LeftIndex, RightIndex, hand[index]);
                if(replacement != -1){
                    hand[index] = replacement;


                    System.out.println("|======" + (playerid+1) + "\'s hand " + Arrays.toString(hand) + " " + counter +" ======|");
                    
                }
                
                checkWin();
            }

            System.out.println("Player " + wincond.intValue() + " has informed player " + playerid + "that they have won");


            //System.out.println("out loop + notify " + (playerid +1));
            //notifyAll();
            
        }catch(IllegalMonitorStateException i){
            System.out.println(i.getMessage() + " notify " + (playerid +1));
        }
        //}
        
    }
}


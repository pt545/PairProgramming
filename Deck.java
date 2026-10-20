import java.util.ArrayList;

public class Deck{
    private ArrayList<Integer> deck;

    public Deck(){
        deck = new ArrayList<Integer>();
        // assuming two or more players
    }

    public void AddtoBottom(int card){
        deck.add(card);
    }

    public int GivefromTop(){
        int retval = deck.get(0);
        deck.remove(0);
        return retval;
        // check not empty first
    }

    public int size(){
        return deck.size();
    }

    public String toString(){
        return deck.toString();
    }
    

}
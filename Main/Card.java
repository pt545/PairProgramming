package Main;

public class Card{
    
    int cardvalue;

    public Card(int cardvalue){
        if (cardvalue < 0) {
            throw new IllegalArgumentException();
        }
        this.cardvalue = cardvalue;
    }

    public int getValue(){
        return cardvalue;
    }
    
}

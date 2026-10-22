import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CardGame:
 *   1. asks for the number of players,
 *   2. asks for the pack file and checks it is valid,
 *   3. deals the cards and creates the players and decks,
 *   4. starts one thread per player and waits for the game to finish,
 *   5. writes the final contents of every deck to deckX_output.txt.
 *
 * Players and decks are arranged in a circle: player i draws from deck i and
 * discards to deck i+1 (the last player discards to deck 1).
 */
public class CardGame {

    private static final int HAND_SIZE = 4; // every player holds four cards

    /**
     * Program start: asks the user for the two inputs, then plays the game.
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        try {
            int n = askForPlayers(scanner);
            List<Card> pack = askForPack(scanner, n);
            play(n, pack, "."); // "." means the current folder
        } catch (NoSuchElementException e) {
            System.out.println("No more input available; exiting.");
        } catch (IOException e) {
            System.out.println("Could not write the output files: " + e.getMessage());
        }
    }

    /**
     * Keeps asking until the user types a whole number that is 1 or more.
     */
    private static int askForPlayers(Scanner scanner) {
        while (true) {
            System.out.println("Please enter the number of players:");
            String text = scanner.nextLine().trim();
            try {
                int n = Integer.parseInt(text);
                if (n >= 1) {
                    return n;
                }
            } catch (NumberFormatException e) {
                // not a number: fall through to the message below
            }
            System.out.println("Invalid number of players: it must be a positive whole number.");
        }
    }

    /**
     * Keeps asking until the user gives the location of a valid pack file.
     */
    private static List<Card> askForPack(Scanner scanner, int n) {
        while (true) {
            System.out.println("Please enter location of pack to load:");
            String location = scanner.nextLine().trim();
            try {
                return loadPack(location, n);
            } catch (IOException | IllegalArgumentException e) {
                System.out.println("Invalid pack: " + e.getMessage());
            }
        }
    }

    /**
     * Reads a pack file and checks it. A valid pack has exactly 8n lines and
     * every line is one non-negative whole number.
     *
     * @param location the file name / path
     * @param n        number of players
     * @return the cards, in the same order as the file (first line = first card)
     * @throws IOException              if the file cannot be read (e.g. it does not exist)
     * @throws IllegalArgumentException if the contents are not a valid pack
     */
    public static List<Card> loadPack(String location, int n) throws IOException {
        // Read every line of the file into a list of Strings.
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(location))) {
            String line = reader.readLine();
            while (line != null) {
                lines.add(line);
                line = reader.readLine();
            }
        }

        // Check the number of lines.
        int expected = 8 * n;
        if (lines.size() != expected) {
            throw new IllegalArgumentException("expected " + expected + " lines for " + n
                    + " player(s) but found " + lines.size());
        }

        // Turn each line into a Card.
        List<Card> pack = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String text = lines.get(i).trim();
            try {
                // parseInt fails for text/decimals/blank; the Card constructor fails for
                // negatives. Both throw an IllegalArgumentException (NumberFormatException
                // is a kind of IllegalArgumentException), so one catch handles both.
                pack.add(new Card(Integer.parseInt(text)));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "line " + (i + 1) + " is not a non-negative whole number: \"" + text + "\"");
            }
        }
        return pack;
    }

    /**
     * Deals the cards, runs the whole game and writes the output files.
     *
     * @param n      number of players
     * @param pack   a valid pack of 8n cards
     * @param folder where to write the output files ("." = current folder)
     * @return the number of the winning player
     * @throws IOException if an output file cannot be written
     */
    public static int play(int n, List<Card> pack, String folder) throws IOException {
        if (n < 1 || pack.size() != 8 * n) {
            throw new IllegalArgumentException("pack must contain exactly 8n cards for n >= 1");
        }

        // Shared by every player (see Player for what they are used for).
        AtomicInteger winner = new AtomicInteger(0);

        // Create the decks (numbered 1 to n).
        List<CardDeck> decks = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            decks.add(new CardDeck(i));
        }

        // Create the players. Player i draws from deck i and discards to deck i+1.
        // decks.get(i - 1) is deck i, and decks.get(i % n) is deck i+1 (or deck 1 for the last player).
        List<Player> players = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            players.add(new Player(i, decks.get(i - 1), decks.get(i % n), winner, folder));
        }

        // Deal: give one card to each player in turn, four times round.
        int next = 0; // position of the next card in the pack
        for (int round = 0; round < HAND_SIZE; round++) {
            for (Player player : players) {
                player.dealCard(pack.get(next));
                next++;
            }
        }

        // The cards left over go to the decks, one at a time, round-robin.
        int deckIndex = 0;
        while (next < pack.size()) {
            decks.get(deckIndex % n).addToBottom(pack.get(next));
            next++;
            deckIndex++;
        }

        // Start one thread per player.
        List<Thread> threads = new ArrayList<>();
        for (Player player : players) {
            Thread thread = new Thread(player);
            threads.add(thread);
            thread.start();
        }

        // Wait for every thread to finish.
        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // The game is over: write what is left in each deck.
        for (CardDeck deck : decks) {
            try (PrintWriter writer = new PrintWriter(folder + "/deck" + deck.getId() + "_output.txt")) {
                writer.println(deck.contentsLine());
            }
        }

        return winner.get();
    }
}

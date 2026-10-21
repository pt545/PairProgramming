import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for {@link CardGame}: pack validation, dealing, whole-game invariants
 * (card conservation, four-card hands, single winner, clean termination) and
 * the required output files.
 */
public class CardGameTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    // ---- helpers -----------------------------------------------------------

    private Path dir() {
        return tmp.getRoot().toPath();
    }

    private Path writePack(String name, List<String> lines) throws IOException {
        Path p = dir().resolve(name);
        Files.write(p, lines);
        return p;
    }

    private static List<String> repeat(String s, int times) {
        return new ArrayList<>(Collections.nCopies(times, s));
    }

    private static List<Card> cards(int... values) {
        List<Card> out = new ArrayList<>();
        for (int v : values) {
            out.add(new Card(v));
        }
        return out;
    }

    /** A pack with eight copies of each of 1..n, shuffled reproducibly. */
    private static List<Card> randomPack(int n, long seed) {
        List<Integer> vals = new ArrayList<>();
        for (int v = 1; v <= n; v++) {
            for (int c = 0; c < 8; c++) {
                vals.add(v);
            }
        }
        Collections.shuffle(vals, new Random(seed));
        List<Card> pack = new ArrayList<>();
        for (int v : vals) {
            pack.add(new Card(v));
        }
        return pack;
    }

    /** Runs a game with terminal output suppressed. */
    private int playQuietly(int n, List<Card> pack) throws IOException {
        PrintStream original = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));
        try {
            return CardGame.play(n, pack, dir());
        } finally {
            System.setOut(original);
        }
    }

    private List<String> lines(String file) throws IOException {
        return Files.readAllLines(dir().resolve(file));
    }

    private static List<Integer> parseInts(String text) {
        List<Integer> out = new ArrayList<>();
        for (String tok : text.trim().split("\\s+")) {
            if (!tok.isEmpty()) {
                out.add(Integer.parseInt(tok));
            }
        }
        return out;
    }

    private static Map<Integer, Integer> count(List<Integer> values) {
        Map<Integer, Integer> m = new HashMap<>();
        for (int v : values) {
            m.merge(v, 1, Integer::sum);
        }
        return m;
    }

    // ---- pack validation ---------------------------------------------------

    @Test
    public void validPackIsLoadedInFileOrder() throws IOException {
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            rows.add(String.valueOf(i));
        }
        Path p = writePack("two.txt", rows);

        List<Card> pack = CardGame.loadPack(p.toString(), 2);

        assertEquals(16, pack.size());
        assertEquals(0, pack.get(0).getValue());
        assertEquals(15, pack.get(15).getValue());
    }

    @Test
    public void packValuesMayExceedNumberOfPlayers() throws IOException {
        List<String> rows = repeat("1", 15);
        rows.add("500");
        List<Card> pack = CardGame.loadPack(writePack("big.txt", rows).toString(), 2);
        assertEquals(500, pack.get(15).getValue());
    }

    @Test
    public void zeroIsAValidPackValue() throws IOException {
        List<String> rows = repeat("0", 8);
        assertEquals(8, CardGame.loadPack(writePack("zero.txt", rows).toString(), 1).size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void packWithTooFewCardsIsRejected() throws IOException {
        CardGame.loadPack(writePack("short.txt", repeat("1", 15)).toString(), 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void packWithTooManyCardsIsRejected() throws IOException {
        CardGame.loadPack(writePack("long.txt", repeat("1", 17)).toString(), 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void packLengthMustMatchNumberOfPlayers() throws IOException {
        // 16 lines is valid for two players, not for three.
        CardGame.loadPack(writePack("sixteen.txt", repeat("1", 16)).toString(), 3);
    }

    @Test
    public void nonNumericLineIsRejectedAndReportedByLineNumber() throws IOException {
        List<String> rows = repeat("1", 8);
        rows.set(4, "abc");
        try {
            CardGame.loadPack(writePack("nan.txt", rows).toString(), 1);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("line 5"));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeValueIsRejected() throws IOException {
        List<String> rows = repeat("1", 8);
        rows.set(0, "-3");
        CardGame.loadPack(writePack("neg.txt", rows).toString(), 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void decimalValueIsRejected() throws IOException {
        List<String> rows = repeat("1", 8);
        rows.set(7, "2.5");
        CardGame.loadPack(writePack("dec.txt", rows).toString(), 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void blankLineIsRejected() throws IOException {
        List<String> rows = repeat("1", 8);
        rows.set(3, "");
        CardGame.loadPack(writePack("blank.txt", rows).toString(), 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void valueTooLargeForAnIntIsRejected() throws IOException {
        List<String> rows = repeat("1", 8);
        rows.set(0, "99999999999");
        CardGame.loadPack(writePack("huge.txt", rows).toString(), 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void twoNumbersOnOneLineAreRejected() throws IOException {
        List<String> rows = repeat("1", 8);
        rows.set(2, "1 2");
        CardGame.loadPack(writePack("two_per_line.txt", rows).toString(), 1);
    }

    @Test(expected = IOException.class)
    public void missingFileIsReportedAsIoProblem() throws IOException {
        CardGame.loadPack(dir().resolve("does_not_exist.txt").toString(), 4);
    }

    @Test
    public void missingFileFailsWithNoSuchFile() {
        try {
            CardGame.loadPack(dir().resolve("nope.txt").toString(), 4);
            fail("expected an exception");
        } catch (IOException e) {
            assertTrue(e instanceof NoSuchFileException);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void playRejectsPackOfWrongSize() throws IOException {
        CardGame.play(4, cards(1, 2, 3), dir());
    }

    @Test(expected = IllegalArgumentException.class)
    public void playRejectsZeroPlayers() throws IOException {
        CardGame.play(0, new ArrayList<Card>(), dir());
    }

    // ---- dealing and the immediate win ------------------------------------

    /**
     * Rounds of the deal: p1 1 5 8 11, p2 2 2 2 2 (immediate winner),
     * p3 3 6 9 12, p4 4 7 10 13; the remaining cards 14..29 fill the decks round-robin.
     */
    private static List<Card> immediateWinPack() {
        List<Integer> v = new ArrayList<>(Arrays.asList(
                1, 2, 3, 4,
                5, 2, 6, 7,
                8, 2, 9, 10,
                11, 2, 12, 13));
        for (int c = 14; c <= 29; c++) {
            v.add(c);
        }
        List<Card> pack = new ArrayList<>();
        for (int x : v) {
            pack.add(new Card(x));
        }
        return pack;
    }

    @Test(timeout = 20000)
    public void handsAreDealtRoundRobinAndDecksFilledFromTheRemainder() throws IOException {
        int w = playQuietly(4, immediateWinPack());

        assertEquals(2, w);
        assertEquals("player 1 initial hand 1 5 8 11", lines("player1_output.txt").get(0));
        assertEquals("player 2 initial hand 2 2 2 2", lines("player2_output.txt").get(0));
        assertEquals("player 3 initial hand 3 6 9 12", lines("player3_output.txt").get(0));
        assertEquals("player 4 initial hand 4 7 10 13", lines("player4_output.txt").get(0));
        // No turns were taken, so the decks still show the dealt contents.
        assertEquals("deck1 contents: 14 18 22 26", lines("deck1_output.txt").get(0));
        assertEquals("deck2 contents: 15 19 23 27", lines("deck2_output.txt").get(0));
        assertEquals("deck3 contents: 16 20 24 28", lines("deck3_output.txt").get(0));
        assertEquals("deck4 contents: 17 21 25 29", lines("deck4_output.txt").get(0));
    }

    @Test(timeout = 20000)
    public void immediateWinWritesFourLinesForEveryPlayerAndNothingElseHappens() throws IOException {
        playQuietly(4, immediateWinPack());

        for (int i = 1; i <= 4; i++) {
            assertEquals("player " + i + " file", 4, lines("player" + i + "_output.txt").size());
        }
        assertEquals(Arrays.asList(
                "player 2 initial hand 2 2 2 2",
                "player 2 wins",
                "player 2 exits",
                "player 2 final hand: 2 2 2 2"), lines("player2_output.txt"));
        assertEquals(Arrays.asList(
                "player 1 initial hand 1 5 8 11",
                "player 2 has informed player 1 that player 2 has won",
                "player 1 exits",
                "player 1 hand: 1 5 8 11"), lines("player1_output.txt"));
    }

    @Test(timeout = 20000)
    public void winnerIsAnnouncedOnTheTerminal() throws IOException {
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured));
        try {
            CardGame.play(4, immediateWinPack(), dir());
        } finally {
            System.setOut(original);
        }
        assertEquals("player 2 wins", captured.toString().trim());
    }

    // ---- whole-game invariants --------------------------------------------

    private static final Pattern DRAW = Pattern.compile("player (\\d+) draws a (\\d+) from deck (\\d+)");
    private static final Pattern DISCARD = Pattern.compile("player (\\d+) discards a (\\d+) to deck (\\d+)");
    private static final Pattern CURRENT = Pattern.compile("player (\\d+) current hand is ([\\d ]+)");

    private void checkGame(int n, List<Card> pack, int returnedWinner) throws IOException {
        // exactly 2n output files
        for (int i = 1; i <= n; i++) {
            assertTrue(Files.exists(dir().resolve("player" + i + "_output.txt")));
            assertTrue(Files.exists(dir().resolve("deck" + i + "_output.txt")));
        }
        int outputFiles = 0;
        try (java.util.stream.Stream<Path> s = Files.list(dir())) {
            outputFiles = (int) s.filter(p -> p.getFileName().toString().endsWith("_output.txt")).count();
        }
        assertEquals("expected 2n output files", 2 * n, outputFiles);

        List<Integer> everyCardAtEnd = new ArrayList<>();
        int winners = 0;
        int winnerSeen = 0;

        for (int i = 1; i <= n; i++) {
            List<String> l = lines("player" + i + "_output.txt");
            assertTrue(l.get(0).startsWith("player " + i + " initial hand "));
            assertEquals("player " + i + " exits", l.get(l.size() - 2));

            String last = l.get(l.size() - 1);
            List<Integer> finalHand = parseInts(last.substring(last.indexOf(':') + 1));
            assertEquals("each player ends with exactly four cards", 4, finalHand.size());
            everyCardAtEnd.addAll(finalHand);

            if (l.get(l.size() - 3).equals("player " + i + " wins")) {
                winners++;
                winnerSeen = i;
                assertTrue(last.startsWith("player " + i + " final hand:"));
                assertEquals("winner holds four of a kind", 1, new java.util.HashSet<>(finalHand).size());
            } else {
                assertTrue(l.get(l.size() - 3).matches(
                        "player \\d+ has informed player " + i + " that player \\d+ has won"));
                assertTrue(last.startsWith("player " + i + " hand:"));
            }

            int draws = 0;
            int discards = 0;
            for (String line : l) {
                Matcher m = DRAW.matcher(line);
                if (m.matches()) {
                    draws++;
                    assertEquals("draws only from own deck", i, Integer.parseInt(m.group(3)));
                }
                m = DISCARD.matcher(line);
                if (m.matches()) {
                    discards++;
                    assertEquals("discards only to the next deck", i % n + 1, Integer.parseInt(m.group(3)));
                    assertTrue("preferred denomination must never be discarded",
                            Integer.parseInt(m.group(2)) != i);
                }
                m = CURRENT.matcher(line);
                if (m.matches()) {
                    assertEquals("hand must hold four cards after every action", 4, parseInts(m.group(2)).size());
                }
            }
            assertEquals("every draw is paired with a discard", draws, discards);
        }

        for (int i = 1; i <= n; i++) {
            String line = lines("deck" + i + "_output.txt").get(0);
            assertTrue(line.startsWith("deck" + i + " contents:"));
            everyCardAtEnd.addAll(parseInts(line.substring(line.indexOf(':') + 1)));
        }

        List<Integer> original = new ArrayList<>();
        for (Card c : pack) {
            original.add(c.getValue());
        }
        assertEquals("cards must be conserved: none lost, none duplicated",
                count(original), count(everyCardAtEnd));
        assertEquals("exactly one player declares victory", 1, winners);
        assertEquals(winnerSeen, returnedWinner);
    }

    @Test(timeout = 60000)
    public void fourPlayerGamesRespectAllInvariants() throws IOException {
        for (long seed = 0; seed < 25; seed++) {
            List<Card> pack = randomPack(4, seed);
            for (java.io.File f : dir().toFile().listFiles()) {
                Files.delete(f.toPath());
            }
            int w = playQuietly(4, pack);
            checkGame(4, pack, w);
        }
    }

    @Test(timeout = 60000)
    public void variousPlayerCountsRespectAllInvariants() throws IOException {
        for (int n : new int[] {1, 2, 3, 5, 8}) {
            for (long seed = 100; seed < 108; seed++) {
                List<Card> pack = randomPack(n, seed);
                for (java.io.File f : dir().toFile().listFiles()) {
                    Files.delete(f.toPath());
                }
                int w = playQuietly(n, pack);
                checkGame(n, pack, w);
            }
        }
    }

    @Test(timeout = 20000)
    public void gameTerminatesCleanlyAfterAWinnerEvenWithManyThreads() throws IOException {
        // If a thread failed to notice the winner, play() would never return and the timeout fires.
        List<Card> pack = randomPack(20, 5);
        int w = playQuietly(20, pack);
        checkGame(20, pack, w);
    }

    @Test(timeout = 20000)
    public void gameStillWorksWhenFaceValuesExceedPlayerCount() throws IOException {
        // Values 1..4 for two players: 3s and 4s are nobody's preference but can still win.
        List<Integer> vals = new ArrayList<>();
        for (int v = 1; v <= 4; v++) {
            for (int c = 0; c < 4; c++) {
                vals.add(v);
            }
        }
        Collections.shuffle(vals, new Random(3));
        List<Card> pack = new ArrayList<>();
        for (int v : vals) {
            pack.add(new Card(v));
        }
        int w = playQuietly(2, pack);
        checkGame(2, pack, w);
    }

    @Test(timeout = 20000)
    public void packFromFileCanDriveAFullGame() throws IOException {
        List<Card> expected = randomPack(4, 42);
        List<String> rows = new ArrayList<>();
        for (Card c : expected) {
            rows.add(String.valueOf(c.getValue()));
        }
        Path p = writePack("pack.txt", rows);

        List<Card> loaded = CardGame.loadPack(p.toString(), 4);
        assertEquals(expected, loaded);

        int w = playQuietly(4, loaded);
        assertFalse(w == 0);
        checkGame(4, loaded, w);
    }
}
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/** Runs every test class in the project (JUnit 4). */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        CardTest.class,
        CardDeckTest.class,
        PlayerTest.class,
        CardGameTest.class
})
public class CardGameTestSuite {
}
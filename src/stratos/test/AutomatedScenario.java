/**  
  *  Written by Jaroslaw Jedynak
  *  
  *  Feel free to poke around for non-commercial purposes.
  */
package stratos.test;
import stratos.game.common.*;
import stratos.start.Scenario;
import stratos.user.*;
import stratos.util.*;

import java.util.Date;


//  TODO:  You might consider moving this, and most of the DebugX classes, to
//         a separate src.test package?

public abstract class AutomatedScenario extends Scenario {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  String outputFile;
  Stack <TestCase> cases;
  Date startTime;
  
  
  
  public AutomatedScenario(String saveFile, String outputFile) {
    super(saveFile, true);
    this.outputFile = outputFile;
  }
  
  
  public AutomatedScenario(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Abstract interface to allow testing-
    */
  protected abstract TestResult getCurrentResult();

  protected abstract long getMaxTestDurationMs();
  
  
  public static enum TestResult {
    UNKNOWN, PASSED, FAILED
  }
  
  
  //  TODO:  If you look at, e.g, DebugMissions, you can see there are multiple
  //         scenarios for testing within a single file (e.g, strikeScenario,
  //         reconScenario, etc.)  We could either split those across multiple
  //         classes, but it might be simpler if we could iterate across a
  //         bunch of TestCases-
  
  public static abstract class TestCase {
    abstract AutomatedScenario initScenario();
    abstract void setupScenario(Stage world, Base base, BaseUI UI);
    abstract TestResult currentResult();
  };
  
  
  
  
  /**  Life cycle and updates-
    */
  public void updateGameState() {
    super.updateGameState();
    if (startTime == null) {
      startTime = new Date();
    }
    
    //  TODO:  As suggested, it would be better to use game-time here, rather
    //  than real time.
    
    Date currentTime = new Date();
    if (currentTime.getTime() - startTime.getTime() > getMaxTestDurationMs()) {
      processTestFailure();
    }
    else {
      TestResult result = getCurrentResult();
      if (result == TestResult.PASSED) {
        processTestSuccess();
      }
      if (result == TestResult.FAILED) {
        processTestFailure();
      }
    }
  }
  
  
  private void processTestFailure() {
    //  TODO:  I'd prefer logging results to an external file (and I.complain()
    //         will actually cause the game to exit.)
    I.complain(getClass().getName() + " test failed");
    finishTest();
  }
  
  
  private void processTestSuccess() {
    I.say(getClass().getName() + " test succeeded");
    finishTest();
  }
  
  
  private void finishTest() {
    //  TODO:  This should move on to the next test in the sequence, rather
    //         than quitting automatically (and I don't think there's any need
    //         to save yet.)
    super.scheduleSaveAndExit();
  }
}










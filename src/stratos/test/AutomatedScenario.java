/**  
  *  Written by Jaroslaw Jedynak
  *  
  *  Feel free to poke around for non-commercial purposes.
  */
package stratos.test;
import stratos.game.common.*;
import stratos.start.Scenario;
import stratos.user.BaseUI;
import stratos.util.*;

import java.util.Date;


//  TODO:  If you look at, e.g, DebugMissions, you can see there are multiple
//         scenarios for testing within a single file (e.g, strikeScenario,
//         reconScenario, etc.)  We could either split those across multiple
//         classes, but it might be simpler if we could iterate across a
//         bunch of TestCases-


public abstract class AutomatedScenario extends Scenario {
  /**  Data fields, constructors and save/load methods-
   */
  float startTime = -1;
  
  TestCase testCase;

  public AutomatedScenario(String saveFile, TestCase testCase) {
    super(saveFile, true);
    this.testCase = testCase;
  }
  
  
  public AutomatedScenario(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Abstract interface to allow testing-
    */
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    testCase.setupScenario(world, base, UI);
  }

  
  /**  Life cycle and updates-
    */
  public void updateGameState() {
    super.updateGameState();
    
    final float time = world().currentTime();
    if (startTime == -1) startTime = time;
    
    if (time - startTime > testCase.getMaxTestDuration()) {
      AutomatedTestRunner.testFailed(testCase, "timeout");
    }
    else {
      TestResult result = testCase.currentResult();
      if (result == TestResult.PASSED) {
        AutomatedTestRunner.testSucceeded(testCase);
      }
      if (result == TestResult.FAILED) {
        AutomatedTestRunner.testFailed(testCase, "check failed");
      }
    }
  }
}










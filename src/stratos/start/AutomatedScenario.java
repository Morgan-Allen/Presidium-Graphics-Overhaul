package stratos.start;

import stratos.game.common.Session;
import stratos.util.I;

import java.util.Date;

public abstract class AutomatedScenario extends Scenario {
  Date startTime;

  public AutomatedScenario(Session s) throws Exception {
    super(s);
  }

  public AutomatedScenario(String debug_combat, boolean b) {
    super(debug_combat, b);
  }

  protected abstract AutomatedTestResult getCurrentResult();

  protected abstract long getMaxTestDurationMs();

  public void updateGameState() {
    super.updateGameState();

    if (startTime == null) {
      startTime = new Date();
    }

    Date currentTime = new Date();
    if (currentTime.getTime() - startTime.getTime() > getMaxTestDurationMs()) {
      processTestFailure();
    } else {
      AutomatedTestResult result = getCurrentResult();
      if (result == AutomatedTestResult.PASSED) {
        processTestSuccess();
      }
      if (result == AutomatedTestResult.FAILED) {
        processTestFailure();
      }
    }
  }

  private void processTestFailure() {
    I.complain(getClass().getName() + " test failed");

    finishTest();
  }

  private void processTestSuccess() {
    I.say(getClass().getName() + " test succeeded");

    finishTest();
  }

  private void finishTest() {
    super.scheduleSaveAndExit();
  }
}


package stratos.start;

import stratos.game.common.Session;

public abstract class AutomatedScenario extends Scenario {

  public AutomatedScenario(Session s) throws Exception {
    super(s);
  }

  public AutomatedScenario(String debug_combat, boolean b) {
    super(debug_combat, b);
  }
}

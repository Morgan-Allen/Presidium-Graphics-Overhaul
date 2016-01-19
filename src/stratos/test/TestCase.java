package stratos.test;

import stratos.game.common.Base;
import stratos.game.common.Stage;
import stratos.user.BaseUI;

public abstract class TestCase {
  abstract AutomatedScenario initScenario();
  abstract void setupScenario(Stage world, Base base, BaseUI UI);
  abstract TestResult currentResult();
}

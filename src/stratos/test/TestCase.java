/**  
  *  Written by Jaroslaw Jedynak
  *  
  *  Feel free to poke around for non-commercial purposes.
  */
package stratos.test;
import stratos.game.common.*;
import stratos.user.*;



public abstract class TestCase {
  
  final String caseName;
  
  TestCase(String name) {
    this.caseName = name;
  }
  
  
  abstract AutomatedScenario initScenario();
  abstract void setupScenario(Stage world, Base base, BaseUI UI);
  abstract TestResult currentResult();
  abstract long getMaxTestDuration();
}

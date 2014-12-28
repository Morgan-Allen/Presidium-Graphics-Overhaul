

package stratos.start;
import stratos.game.common.*;
import stratos.game.politic.*;
import stratos.game.actors.*;
import stratos.graphics.common.Rendering;
import stratos.graphics.widgets.KeyInput;
import stratos.user.*;
import stratos.util.*;



//  TODO:  Test the stock exchange, and re-introduce the arcology & edifice.

//  Assign drills directly at the trooper lodge.  That'll keep 'em busy!

//  Arrange for contact missions to come back with a desired price from the
//  subject/s.

//  Ideally, hiring arrangements should have some kind of progress meter.

//  TODO:  Introduce hunting by the natives, and allow trading with them as a
//  source of carbs/greens/protein.  (Also, add singer performance?)  ...Just
//  make them less static/idle, I guess.

//  TODO:  Retreat should have the possibility to break pursuit as well.
//         Indoor combats take way too long.  (However, the SFX being shown
//         might be a clue to sprite-flickering in odd locations.  Consider
//         studying it for that reason?)

//  TODO:  Also, animals should react more strongly to strangers (either fight
//         or flee.)

//  Introduce a finance-report UI!  And try to get all the details.
//  Try to arrange that actors will seek out new Holdings if anything existing
//  is at or above half crowding.
//  Rework art for roads, the shield wall, the physician station, the engineer
//  station, the solar bank, and the archives.

public class DebugTutorial extends TutorialScenario {
  
  
  public static void main(String s[]) {
    PlayLoop.setupAndLoop(new DebugTutorial());
  }
  
  
  private DebugTutorial() {
    super();
  }
  
  
  public DebugTutorial(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  public void beginGameSetup() {
    super.initScenario("debug_tutorial");
  }
  
  
  protected String saveFilePrefix(Stage world, Base base) {
    return "debug_tutorial";
  }
  

  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    super.configureScenario(world, base, UI);
    UI.selection.pushSelection(base.ruler(), true);
  }
  
  
  public void updateGameState() {
    super.updateGameState();
  }
  
  
  public void renderVisuals(Rendering rendering) {
    super.renderVisuals(rendering);
  }
}





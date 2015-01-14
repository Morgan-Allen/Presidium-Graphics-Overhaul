/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.start;
import stratos.game.common.*;
import stratos.game.politic.*;
import stratos.game.actors.*;
import stratos.graphics.common.Rendering;
import stratos.graphics.widgets.KeyInput;
import stratos.user.*;
import stratos.util.*;



//  TODO:  Test the stock exchange plus upgrades for the supply depot.
//         Also, figure out how to get carbons and use spyces.

//  TODO:  Merge the summons-dialogue system with the Contact mission.  It's
//  much more natural that way.

//  TODO:  Assign drills directly at the trooper lodge, plus call to arms.
//         Also, something for the Air Corps to do- like bomb runs.

//  Ideally, hiring arrangements should have some kind of progress meter?

//  TODO:  Retreat should have the possibility to break pursuit as well.
//  TODO:  Also, animals should react more strongly to strangers (either fight,
//         talk or flee.)  Maybe natives too?

//  TODO:  Introduce hunting by the natives, and allow trading with them as a
//  source of carbs/greens/protein.  (Also, add singer performance.)

//  Have arcologies actually grow over time, if you have enough water.
//  TODO:  HAVE DISPLAYS FOR TOTAL SUPPLY/DEMAND OF WATER/POWER/ETC.

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
    GameSettings.fogFree = true;
    super.configureScenario(world, base, UI);
    UI.selection.pushSelection(base.ruler(), true);
    
    I.say("\nLISTING BASE RELATIONS:");
    for (Base b : world().bases()) {
      I.say("  Relations for "+b+" with...");
      for (Base o : world().bases()) if (o != b) {
        I.say("    "+o+": "+b.relations.relationWith(o));
      }
    }
  }
  
  
  public void updateGameState() {
    super.updateGameState();
  }
  
  
  public void renderVisuals(Rendering rendering) {
    super.renderVisuals(rendering);
  }
}





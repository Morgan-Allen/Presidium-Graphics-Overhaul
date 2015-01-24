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




//  Roads and walls, I think, are your main structural tool.  They have to
//  be placed explicitly.  And it would help if mines/farms would preview their
//  AoE.

//  ...It's more about giving the player a sense of channels of direct or
//  indirect interaction.  Ways to game the system.

//  TODO:  Placement of spontaneous structures needs to be a little more
//  prompt... base off immediate demand fulfillment, rather than time period.

//  Also, service hatches need to be placed!
//  TODO:  Get the set of all objects that conflict with a venue's placement,
//         and pass that to both the canPlace() & enterWorld() methods.



//  Also, just use a BlurMap for ambience.  It's a lot simpler and less freaky.

//  TODO:  Merge the summons-dialogue system with the Contact mission.  It's
//  much more natural that way.  And have citizens talk about their complaints-
//  link that to the bad/good thoughts and mood system!


//  TODO:  Assign drills directly at the trooper lodge, plus call to arms.
//         Also, something for the Air Corps to do- like bomb runs.

//  TODO:  Complete upgrades for all structures, including the supply depot,
//         stock exchange, runner market, kommando lodge, trooper barracks,
//         culture vats, et cetera.

//  Ideally, hiring arrangements should have some kind of progress meter?
//  (Also, actors still seem a little too eager to apply for jobs they have no
//   real qualification for.)


//  TODO:  Retreat should have the possibility to break pursuit as well.
//  TODO:  Also, animals should react more strongly to strangers (either fight,
//         talk or flee.)  Maybe natives too?

//  TODO:  Introduce hunting by the natives, and allow trading with them as a
//  source of carbs/greens/protein.  (Also, add singer performance.)


//  Try to arrange that actors will seek out new Holdings if anything existing
//  is at or above half crowding.

//  Introduce a finance-report UI!  And try to get all the details.
//  Rework art for roads, the shield wall, the physician station, the engineer
//  station, the solar bank, and the archives... nearly anything, really.



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





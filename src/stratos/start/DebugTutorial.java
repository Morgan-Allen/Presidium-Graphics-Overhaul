/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.start;
import stratos.game.common.*;
import stratos.graphics.common.Rendering;
import stratos.user.*;
import stratos.util.*;



//  BIG BUGFIX TODO LIST:

//  *  Make clear that upgrades aren't available for unfinished structures-
//     and there's a time-delay, et cetera.

//  *  Restore the fear associated with wandering outside your base-zone.


/*  BIG TODO LIST:

*  Need to ensure that unemployed job-seekers are more sensible.

*  Need to ensure that artilect raiding increases gradually over time.
     Base this off a 'waking up' mechanism as threats are found.

*  Need to restore animal nests and surface behaviours.
     Need to ensure auto-distribution of predator and prey nests.

*  Need service hatches to allow access to vermin.
     Need to write vermin behaviour.

*  Upgrades need to filled in and tested for:
     Stock Exchange
     Archives
     Runner Market
     Kommando Lodge
     Airfield
     Trooper Lodge (drilling)

*  Need to unify summons-dialogue with contact mechanics.

*  Psy powers need some checkup- cooldown system, replace TK, UI idiom.

*  Hide-and-seek needs to be more effective (both retreat and combat.)

*  Explicit or improved placement for shield walls and arcologies.
     Fresh art for shield walls.

*  Buildings need to have multiple levels.

*  General internal clean-up of plan routines.

*  Fresh art and level-depictions for all structures.


//  Minor items:
//  *  Ideally, using Contact on your own citizens should still leave a marker.
//  *  Charge for voluntary medical treatment!

//*/


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
    GameSettings.fogFree = false;
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





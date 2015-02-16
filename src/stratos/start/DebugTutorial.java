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



//  TODO:  LOG OUTPUT SHOULD BE TAGGED BY TIME/DATE, SO THAT IT ISN'T
//         OVERWRITTEN.  Also, old save files should probably be kept.

//  Either get rid of the extra save options or add back the psy meter.


/*  TUTORIAL TODO LIST:

*  Use a linear sequence with much more detail on specific, simple steps.  You
   want to cover-
     Building
     Recruitment
     Mission Types
     Basic Research
     Housing + Manufacture + Sales and Imports (keep brief)

*  Limit the structure types available during the tutorial, and limit based on
   structure-prereqs.  (Also, you might try merging into a single category.)

*  Limit the powers available too- have them rely on the ruler learning them,
   one at a time.

*  Focus on the objectives that have an intuitive, emotional component- such as
   survival (strike/defend) and comfort (the economy.)  Making those mandatory
   isn't too bad.
//*/



/*  BIG TODO LIST:

*  Need to ensure that artilect raiding increases gradually over time.
     Base this off a 'waking up' mechanism as threats are found.

*  Extra concept art for new creatures/vermin:
     Avrodil, Sea Bean, Rem Leech, Mistaken, Desert Maw, Hirex Body.

*  Need service hatches to allow access to vermin.
     Need to write vermin behaviour.

*  Need to restore animal nests and surface behaviours.
     Need to ensure auto-distribution of predator and prey nests.

*  Psy powers need some checkup.  Make them feel... more indirect.  And give
   them some custom UI rigging (hotkeys 1-9).

*  Explicit or improved placement for shield walls and arcologies.
     Fresh art for shield walls.

*  Make Pledges more persistent and nuanced (see Proposal class.)

*  Introduce Call-to-Arms at the Trooper Lodge (with respect to a particular
   mission at a time.)

*  Hide-and-seek needs to be more effective (both retreat and combat.)

*  Buildings need to have multiple levels.

*  Upgrades need to filled in and tested for:
     Stock Exchange
     Archives
     Runner Market
     Kommando Lodge
     Airfield
     Trooper Lodge (drilling)
     Cut out recruitment-extras for others.

*  General internal clean-up of plan routines.

*  Fresh art and level-depictions for all structures.

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
    UI.selection.pushSelection(base.ruler());
    
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





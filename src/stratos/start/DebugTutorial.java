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



/*  TUTORIAL TODO LIST:

*  Limit the powers available too- using upgrades or the household-panel?

*  Have tutorial-messages appear as message-items along the right-hand side, in
   a fashion similar to ongoing missions.

*  Focus on the objectives that have an intuitive, emotional component- such as
   survival (strike/defend) and comfort (the economy.)  Making those mandatory
   isn't too bad.

*  Use a linear sequence with much more detail on specific, simple steps.  You
   want to cover-
     Building
     Recruitment
     Mission Types
     Basic Research
     Housing + Manufacture + Sales and Imports (keep brief)

*  Limit the structure types available during the tutorial?
//*/



/*  BIG TODO LIST:

ENEMIES AND ENVIRONMENT-

*  Extra concept art for new creatures/vermin:
     Avrodil, Rem Leech, Mistaken, Desert Maw, Hirex Body.

*  Need to write vermin behaviour (see Vermin class.)

*  Need to restore animal nests and check their breeding behaviours.
   Need to ensure auto-distribution of predator and prey nests.

BUILDING AND UPGRADES-

*  Settle on a different division-system for structure-spacing.  The current
   version is confusing and a tad opaque.

*  Explicit or improved placement for shield walls and arcologies.
     Fresh art for shield walls.

*  Upgrades need to filled in and tested for:
     Stock Exchange
     Archives
     Runner Market
     Kommando Lodge
     Airfield
     Trooper Lodge (drilling)
     Cut out recruitment-extras for others.

*  Use service hatches to initiate heavier paving and utility-transmission?

*  Buildings need to have multiple levels.  (At least for the main/root-guild
   structures.)

*  Fresh art and level-depictions for all structures.

*  Ensure that spontaneous-buildings can 'migrate' to new sites if conditions
   change.  (Allow that for buildings in general?)

CONTROL AND DIRECTION-

*  Introduce Call-to-Arms at the Trooper Lodge (with respect to a particular
   mission at a time.)

*  Make Pledges more persistent and nuanced (see Proposal class.)

*  Psy abilities need to be persistent and list their associated costs.  And
   show the psy meter next to your portrait.


INTERFACE AND DEBUGGING

*  Include an Advisors(?) and Finances/Economy pane.

*  Read definitions, tutorial messages, etc. from .xml and allow for later
   translations.

*  Try to implement some global debugging-levels.


CITIZEN BEHAVIOUR-

*  Rework the migration system to that it's more gradual- colonists arrive from
   the homeworld on a scale of 1-2 months (10-20 days.)  Landing parties 'come
   ashore' every day or two, but those can't respond to job-demand as such.

*  Hide-and-seek needs to be more effective (both retreat and combat.)

*  Figure out entry-permissions for structures and guests, and/or sieges.

*  General internal clean-up of plan routines.  (Look in PlanUtils.)

*  Proper evaluation of mood and memory-events.
//*/



public class DebugTutorial extends TutorialScenario {
  
  
  public static void main(String s[]) {
    PlayLoop.setupAndLoop(new DebugTutorial());
  }
  
  
  private DebugTutorial() {
    super("tutorial_quick");
  }
  
  
  public DebugTutorial(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  public void beginGameSetup() {
    super.initScenario("tutorial_quick");
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





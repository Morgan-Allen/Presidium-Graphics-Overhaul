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


/*
TESTER FEEDBACK AND TUTORIAL TODO LIST:

*  Use a linear sequence with much more detail on specific, simple steps.  You
   want to cover-
   * Basic Navigation and UI Features (access to game inputs and readouts.)
   * Building & Upgrades (space management.)
   * Recruitment & Missions (indirect control, motivation.)
   * Inital objectives (security, basic housing, turning a profit.)
   
   Just don't railroad the player if they want to wander off and explore the
   game's mechanics themselves.

*  More feedback on needs and settlement demands.  (Notifications or an RCI
   indicator of some sort are needed.)  You need feedback for-
     *  Citizen needs   / no food source (imports or grown.)
     *  Low funding     / no income source (exports or taxes.)
     *  Citizen housing / no holdings built.
     *  Specific goods  / no factory source built.
     *  Citizen health  / no medical team (sickbay and archives.)
     *  Danger          / no security force (walls and military.)
   Emphasise the emotive component here?

*  A minimap-indicator is needed for your current viewpoint.

*  Ensure a small number of powers can be chosen for your ruler before the
   scenario begins!

*  Re-introduce secondary revenue from upgraded housing!

*  Ensure that money-making is reliable and intuitive.  Clarify and simplify
   the imports system and taxation.

*  Allow click-and-drag for things like the arcology or plantations (or maybe
   building-facings and housing blocs?)
   You could also make use of right-click.
   Drag buildings from the install-pane directly onto the ground!  That's what
   he was talking about.

*  Flora is a little too dense, especially when standalone.  It blocks the
   view.  Also, try to polish it up- look more exotic.

*  Icons could be clearer, at least in the early game.

*  This map is arguably too small to allow for multiple factions.  Stick to the
   ruins and economic buildup.

*  Sort out a decent carbons-source and a use for water-resources.  That, and
   spice flats!
//*/





/*  BIG TODO LIST:

BUILDING AND UPGRADES-
   
  >>>>>>> DO THIS <<<<<<<
*  There needs to be a more precise 'reservation' system to allow for zoning,
   entrances and not-in-the-world-yet situations.

*  Arcologies should have auto-placement.  In fact, I need an aesthetic option
   for the early game that's easy to include spatially.

*  Fresh art for shield walls.  And give them out for free!

*  Upgrades need to be filled in and tested for:
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


CITIZEN BEHAVIOUR-

  >>>>>>> DO THIS <<<<<<<
*  Rework the migration system to that it's more gradual- colonists arrive from
   the homeworld on a scale of 1-2 months (10-20 days.)  Landing parties 'come
   ashore' every day or two, but those can't respond to job-demand as such.

  >>>>>>> DO THIS <<<<<<<
*  Proper evaluation of mood and memory-events.

*  General internal clean-up of plan routines.  (Look in PlanUtils.)
    Consider having the actor's agenda contain *only* plans:   Actions and
    Missions can be handled separately.

*  Hide-and-seek needs to be more effective (for retreat, looting and combat.)

*  Figure out entry-permissions for structures and guests, and/or sieges.


CONTROL AND DIRECTION-

  >>>>>>> DO THIS <<<<<<<
*  Make Pledges more persistent and nuanced (see Proposal class.)
   Also, try to allow negotiation with buildings, in some sense.

  >>>>>>> DO THIS <<<<<<<
*  Introduce Call-to-Arms at the Trooper Lodge (with respect to a particular
   mission at a time.)

*  Psy abilities need to be persistent.

*  Allow psy abilities to be learnt from the Pseer School and other psy
   institutions.  Then, as you use them, you acquire practice that allows
   independant casting independantly (with reduced skill rank.)


ENEMIES AND ENVIRONMENT-

*  Extra concept art for new creatures/vermin:
     Avrodil, Rem Leech, Mistaken, Desert Maw, Hirex Body.

*  Try to adapt standard behaviours (such as FindHome, Repairs, Foraging, etc.
   for use by animals.)  Then get rid of the custom-coding.  And polish up
   vermin behaviours.

*  There needs to be a super-fast system for testing pathability to different
   areas of the map.  (And maybe caching dangers en-route?)

*  Offworld raids by enemy houses!

*  The Yamagur is sufficiently big and powerful that it can't really count as a
   'prey' species (in fact, it tends to wind up pulverising any predators in
   it's territory.)  It needs a tweak to the placement algorithm.


INTERFACE AND DEBUGGING

*  Try to implement some global debugging-levels.

*  Include Advisors(?) and more detail Finances/Economy pane.

*  Include healthbars for psy-meter and target-descriptions?  Personal icon?

*  Try tracking actors only when they wander outside your field of view (or
   start that way.)  And include tracking options in the UI.

*  Read definitions from .xml and allow for later string translations.
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





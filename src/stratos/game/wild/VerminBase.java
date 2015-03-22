/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.game.politic.*;
import stratos.util.*;






//  Let's try simplicity for now.  It's basically a kind of raid-behaviour-
//  (do I need to write a mission for that, or can I build it into the AI?)

//  ...Actually, a Recovery mission might not be such a bad idea.  Try to
//  add that.  (Plus a Disguise/Spying mission!)


//  *  If offworld, how do you model that specific to each base?
//  (This is desirable for the sake of simplicity and balance-maintanance.)

//  Okay.  So, a given base has 'neighbours' above, below, and on each
//  side, along with the general surrounding territory.

//  *  They have a certain chance to lodge at a suitable entry-point and
//     start to reproduce, and to migrate 'offworld' or to another entry-
//     point if things get crowded.  (So extermination can temporarily
//     cull their numbers, and failing to exterminate won't mean you're
//     overrun.)  Not too hard.

public class VerminBase extends Base {
  
  
  private static boolean verbose = false;
  
  final static float
    MAX_MISSION_POWER = CombatUtils.MAX_POWER * Mission.MAX_PARTY_LIMIT,
    CHECK_INTERVAL    = Stage.STANDARD_HOUR_LENGTH * 2;
  
  
  
  public VerminBase(Stage world) {
    super(world, true);
  }
  
  
  public VerminBase(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    
    //
    //  If the time has arrived, assemble a 'raid' where creatures arrive
    //  through another base's service hatches.
    
    final PresenceMap hatches = world.presences.mapFor(ServiceHatch.class);
    
    //int totalHatches = hatches.population();
    //  TODO:  Update more frequently as the hatch population increases.
    //  TODO:  Vary infestation chance based on the area's squalor.
    
    //  TODO:  Reduce spawn-chance based on local crowding.
    boolean spawnMore = false;
    
    if (numUpdates % CHECK_INTERVAL == 0 && spawnMore) {
      Target entryPoint = hatches.pickRandomAround(null, -1, null);
      if (entryPoint == null) return;
      final int numEntered = Rand.index(3) + 1;
      final ServiceHatch hatch = (ServiceHatch) entryPoint;
      
      //  Okay.  Assemble a group of vermin and add them to the hatch as
      //  immigrants.
      
      //  TODO:  VARY COMPOSITION MORE?
      
      I.say("Introducing "+numEntered+" vermin at "+hatch);
      
      for (int n = numEntered; n-- > 0;) {
        Actor enters = Roach.SPECIES.sampleFor(this);
        enters.enterWorldAt(hatch, world);
        enters.goAboard(hatch, world);
        enters.mind.setHome(hatch);
      }
    }
  }
  
  
  protected BaseTactics initTactics() {
    return new BaseTactics(this) {
      /*
      protected float rateMission(Mission mission) {
        final float importance = mission.rateImportance(base);
        if (importance <= 0) return -1;
        return importance + onlineLevel;
      }
      
      protected boolean shouldAllow(
        Actor actor, Mission mission,
        float actorChance, float actorPower,
        float partyChance, float partyPower
      ) {
        float powerLimit = MAX_MISSION_POWER * onlineLevel;
        return actorPower + partyPower <= powerLimit;
      }
      
      protected boolean shouldLaunch(
        Mission mission, float partyChance, float partyPower, boolean timeUp
      ) {
        float powerLimit = MAX_MISSION_POWER * onlineLevel;
        return (partyPower > (powerLimit / 2)) || timeUp;
      }
      //*/
    };
  }
}







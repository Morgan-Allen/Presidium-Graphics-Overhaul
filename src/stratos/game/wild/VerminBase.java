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
  
  final static int
    SPAWN_PER_ENTRY_INTERVAL = Stage.STANDARD_DAY_LENGTH,
    MAX_BIOMASS_PER_ENTRY    = 10;
  
  
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
    //  We perform updates to check for vermin-entry more quickly as the number
    //  of entry-points increases...
    final PresenceMap hatches = world.presences.mapFor(ServiceHatch.class);
    final int totalHatches = hatches.population();
    if (totalHatches == 0) return;
    final int interval = SPAWN_PER_ENTRY_INTERVAL / totalHatches;
    //
    //  If the time has arrived, assemble a 'raid' where creatures arrive
    //  through another base's service hatches.
    if (numUpdates % interval == 0) {
      Target entryPoint = hatches.pickRandomAround(null, -1, null);
      if (entryPoint == null) return;
      final ServiceHatch hatch = (ServiceHatch) entryPoint;
      //
      //  Get the maximum and current vermin population in the area.
      final float squalor = 5 - world.ecology().ambience.valueAt(hatch);
      int maxPop = (int) (squalor * MAX_BIOMASS_PER_ENTRY / 10f);
      maxPop = Nums.clamp(maxPop, MAX_BIOMASS_PER_ENTRY + 1);
      float realPop = 0;
      for (Actor a : hatch.staff.lodgers()) if (a.species().vermin()) {
        realPop += a.species().metabolism();
      }
      final float crowding = realPop / maxPop;
      final int numEntered = (int) (Rand.index(3) + 1 * (1 - crowding));
      if (numEntered <= 0) return;
      //
      //  Then, if crowding allows, assemble a group of vermin and add them to
      //  the hatch as immigrants.
      for (int n = numEntered; n-- > 0;) {
        Actor enters = Rand.num() < crowding ?
          Roach   .SPECIES.sampleFor(this) :
          Roachman.SPECIES.sampleFor(this) ;
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







/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.base.*;
import stratos.game.civic.*;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.util.*;



//  TODO:  I'm going to have to disable this for the moment.  I need to clean
//  up the threat-evaluation routines and allow stealth to work reliably (plus
//  general hiding routines.)


//  TODO:  Vermin should only pop up when there's a good opportunity for theft
//  (i.e, looting priority is > 0) and the area isn't too dangerous for them (
//  due to poor security) and corrosion lets them in (due to poor maintenance
//  and squalor.)  They should then disappear into an 'off-map reservoir' as
//  soon as looting is done and they retreat to their entry-point.
//
//  Simple.  Killing them is like playing whack-a-mole.


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
    final boolean report = verbose;
    super.updateAsScheduled(numUpdates, instant);
    
    //  TODO:  RESTORE LATER (see above)...
    if (true) return;
    //
    //  We perform updates to check for vermin-entry more quickly as the number
    //  of entry-points increases...
    final PresenceMap hatches = world.presences.mapFor(ServiceHatch.class);
    final int totalHatches = hatches.population();
    if (totalHatches == 0 || instant) return;
    final int interval = SPAWN_PER_ENTRY_INTERVAL / totalHatches;
    
    if (report) {
      I.say("Updating vermin base, total updates: "+numUpdates);
      I.say("  Total hatches:  "+totalHatches);
      I.say("  Spawn interval: "+interval    );
    }
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
      final int numEntered = (int) ((Rand.index(3) + 1) * (1 - crowding));
      //
      //  Then, if crowding allows, assemble a group of vermin and add them to
      //  the hatch as immigrants.
      if (report) {
        I.say("\nChecking for vermin entry at: "+hatch);
        I.say("  Position:    "+hatch.origin());
        I.say("  Squalor:     "+squalor   );
        I.say("  Crowding:    "+crowding  );
        I.say("  Num entered: "+numEntered);
      }
      for (int n = numEntered; n-- > 0;) {
        Actor enters = Rand.index(10) < crowding ?
          Roach   .SPECIES.sampleFor(this) :
          Roachman.SPECIES.sampleFor(this) ;
        enters.enterWorldAt(hatch, world);
        enters.mind.setHome(hatch);
        if (report) I.say("  Entering world: "+enters);
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







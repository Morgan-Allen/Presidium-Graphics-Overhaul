/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.content.civic.*;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.game.verse.*;
import stratos.util.*;



//  TODO:  I'm going to have to disable this for the moment.  I need to clean
//  up the threat-evaluation routines and allow stealth to work reliably (plus
//  general hiding routines.)


public class VerminBase extends Base {
  
  
  private static boolean verbose = false;
  
  final static float
    AVG_RAID_INTERVAL = Stage.STANDARD_DAY_LENGTH * 2,
    MIN_RAID_INTERVAL = Stage.STANDARD_DAY_LENGTH / 2,
    MAX_RAID_POWER    = CombatUtils.AVG_POWER * Mission.MAX_PARTY_LIMIT,
    POWER_PER_HATCH   = MAX_RAID_POWER / 20;
  
  final static Species ROACH_SPECIES[] = {
    Roach.SPECIES, Roachman.SPECIES
  };
  
  
  
  public VerminBase(Stage world) {
    super(world, Faction.FACTION_VERMIN);
  }
  
  
  public VerminBase(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  

  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    
    final PresenceMap hatches = world.presences.mapFor(ServiceHatch.class);
    final int totalHatches = hatches.population();
    if (totalHatches == 0 || instant) return;
    
    float maxTeamPower = totalHatches * POWER_PER_HATCH;
    float factionPower = maxTeamPower / MAX_RAID_POWER;
    float visitDelay = AVG_RAID_INTERVAL / (1 + factionPower);
    visitDelay = Nums.max(visitDelay, MIN_RAID_INTERVAL);
    
    if (world.currentTime() - visits.lastVisitTime() > visitDelay) {
      Target entryPoint = hatches.pickRandomAround(null, -1, null);
      final ServiceHatch hatch = (ServiceHatch) entryPoint;
      visits.attemptRaidingVisit(
        maxTeamPower, -1, Verse.SECTOR_UNDERGROUND, hatch, ROACH_SPECIES
      );
    }
  }
  
}







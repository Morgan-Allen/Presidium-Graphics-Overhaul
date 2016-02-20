/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.game.verse.*;
import stratos.game.actors.*;
import stratos.content.civic.*;
import stratos.util.*;
import static stratos.game.actors.Backgrounds.*;



public class CivicBase extends Base {
  
  
  
  public CivicBase(Stage world, Faction faction) {
    super(world, faction);
  }
  
  
  public CivicBase(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  final public static int
    MIN_RAID_INTERVAL = 2 * Stage.STANDARD_DAY_LENGTH,
    AVG_RAID_INTERVAL = MIN_RAID_INTERVAL * 5,
    MAX_RAID_INTERVAL = MIN_RAID_INTERVAL * 10,
    AVG_SECTOR_POWER  = (int) (
      Verse.MEDIUM_POPULATION *
      CombatUtils.AVG_POWER   *
      Mission.AVG_PARTY_LIMIT
    ) / 2;
  
  final public static Background RAID_CLASSES[] = {
    VOLUNTEER, TROOPER, ENFORCER, RUNNER
  };
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    
    //  TODO:...
    //  I think you either need multiple bases per sector, or you need a
    //  different name for what they represent?
    
    float factionPower = 0;
    for (SectorBase sB : world.offworld.sectorBases()) {
      factionPower += sB.powerLevel(faction()) * distanceFactor(sB);
    }
    
    float visitDelay = AVG_RAID_INTERVAL * AVG_SECTOR_POWER / factionPower;
    //  TODO:  NO RANDOM FACTOR!
    visitDelay *= 0.5f + Rand.num();
    visitDelay = Nums.clamp(visitDelay, MIN_RAID_INTERVAL, MAX_RAID_INTERVAL);
    
    float maxTeamPower = factionPower;
    if (world.currentTime() - visits.lastVisitTime() > visitDelay) {
      final SectorBase source = pickClaimedBase();
      if (source != null) visits.attemptRaidingVisit(
        maxTeamPower, -1, source.location, new Airship(this), RAID_CLASSES
      );
    }
  }
  
  
  private SectorBase pickClaimedBase() {
    Batch <Float     > chances = new Batch();
    Batch <SectorBase> claimed = new Batch();
    
    for (SectorBase b : world.offworld.sectorBases()) {
      if (b.faction() == faction()) {
        final float distFactor = distanceFactor(b);
        if (distFactor < 0) continue;
        chances.add(distFactor);
        claimed.add(b);
      }
    }
    return (SectorBase) Rand.pickFrom(claimed, chances);
  }
  
  
  private float distanceFactor(SectorBase claimed) {
    final Sector locale = world.localSector(), s = claimed.location;
    final float timeUnits = s.tripTimeUnits(locale);
    if (timeUnits < 0) return -1;
    return 1f / (1 + timeUnits);
  }
}





















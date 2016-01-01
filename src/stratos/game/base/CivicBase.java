/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.game.verse.*;
import stratos.game.base.*;
import stratos.game.actors.*;
import stratos.content.civic.*;
import stratos.util.*;


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
  
  
  
  public void updateVisits() {
    
    //  I think you either need multiple bases per sector, or you need a
    //  different name for what they represent.
    float factionPower = 0;
    for (SectorBase sB : world.offworld.sectorBases()) {
      factionPower += sB.powerLevel(faction()) * distanceFactor(sB);
    }
    
    float visitDelay = AVG_RAID_INTERVAL * AVG_SECTOR_POWER / factionPower;
    visitDelay *= 0.5f + Rand.num();
    visitDelay = Nums.clamp(visitDelay, MIN_RAID_INTERVAL, MAX_RAID_INTERVAL);
    
    float maxTeamPower = factionPower;
    if (world.currentTime() - lastVisitTime() > visitDelay) {
      beginRaidingVisit(maxTeamPower, -1);
    }
  }
  
  
  public boolean beginRaidingVisit(float maxTeamPower, float arriveDelay) {
    final SectorBase source = pickClaimedBase();
    if (source == null) return false;
    
    final Airship vessel = new Airship(this);
    final Journey journey = Journey.configForVisit(
      source.location, world,
      vessel, this, Journey.RAID_STAY_DURATION
    );
    EntryPoints.findLandingSite(vessel, journey, false);
    if (vessel.dropPoint() == null) return false;
    
    Mission strike = tactics.bestStrikeMissionFromPoint(vessel.dropPoint());
    if (strike == null || strike.targetValue(this) < 0) return false;
    
    final Batch <Background> soldierTypes   = new Batch();
    final Batch <Float     > recruitChances = new Batch();
    Sector hires = faction().startSite();
    if (hires == null) return false;
    
    for (Background b : Backgrounds.MILITARY_CIRCLES) {
      final float w = (hires.weightFor(b) + 0.5f) / 2;
      if (w <= 0) continue;
      soldierTypes  .add(b);
      recruitChances.add(w);
    }
    if (soldierTypes.empty()) return false;
    
    final Batch <Actor> team = new Batch();
    float teamPower = 0;
    while (teamPower <= maxTeamPower) {
      Background b = (Background) Rand.pickFrom(soldierTypes, recruitChances);
      if (b == null) continue;
      Actor recruit = b.sampleFor(this);
      float power = CombatUtils.powerLevel(recruit);
      teamPower += power;
      team.add(recruit);
    }
    MissionUtils.quickSetup(
      strike, Mission.PRIORITY_PARAMOUNT, Mission.TYPE_BASE_AI,
      team.toArray(Actor.class)
    );
    
    beginVisit(strike, journey);
    if (arriveDelay > 0) {
      journey.setArrivalTime(world.currentTime() + arriveDelay);
    }
    return true;
  }
  
  
  private SectorBase pickClaimedBase() {
    Batch <Float     > chances = new Batch();
    Batch <SectorBase> claimed = new Batch();
    
    for (SectorBase b : world.offworld.sectorBases()) {
      if (b.faction() == this.faction()) {
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























package stratos.game.campaign;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.tactical.*;
import stratos.util.*;
import java.util.Map;



//  TODO:  Try adapting this to multiple mission-declarations.
public class BaseAI {
  
  
  final public static int
    RAID_TIME_SHORT  = World.STANDARD_DAY_LENGTH / 2,
    RAID_TIME_NORMAL = World.STANDARD_DAY_LENGTH * 2,
    RAID_TIME_LONG   = World.STANDARD_DAY_LENGTH * 8;
  
  final World world;
  final Base base;
  
  private int raidInterval;
  private int lastRaidTime = -1;
  private Table <Target, Float> threatRatings = new Table <Target, Float> ();
  private Mission currentRaid = null;
  
  //  TODO:  Explore points, defence points, and contact points.
  
  
  public BaseAI(
    Base base,
    int raidInterval
  ) {
    this.base = base;
    this.world = base.world;
    this.raidInterval = raidInterval;
  }
  
  
  public Target raided() {
    if (currentRaid == null) return null;
    return currentRaid.subject();
  }
  
  
  public Venue raidedVenue() {
    final Target raided = raided();
    return (raided instanceof Venue) ? (Venue) raided : null;
  }
  
  
  public Actor raidedActor() {
    final Target raided = raided();
    return (raided instanceof Actor) ? (Actor) raided : null;
  }
  
  
  
  public void update() {
    final int time = (int) world.currentTime();
    updateThreats();
    
    if (currentRaid != null) {
      if (
        currentRaid.finished() ||
        (currentRaid.hasBegun() && currentRaid.totalApplied() == 0)
      ) {
        base.removeMission(currentRaid);
        currentRaid = null;
      }
    }
    
    if (lastRaidTime == -1) lastRaidTime = time;
    if (time - lastRaidTime > raidInterval) {
      
      Target worstThreat = pickWorstThreat();
      threatRatings.clear();
      
      if (worstThreat != null) {
        final Mission raid = new StrikeMission(base, worstThreat);
        
        //  TODO:  Check to make sure this works.
        raid.setMissionType(Mission.TYPE_PARTY);
        currentRaid = raid;
        base.addMission(raid);
      }
      lastRaidTime = time;
    }
  }
  
  
  private Target pickWorstThreat() {
    Target newPick = null, oldPick = null;
    Float newThreat = 0f, oldThreat = null;
    
    for (Map.Entry <Target, Float> e : threatRatings.entrySet()) {
      if (e.getValue() > newThreat) {
        newPick = e.getKey();
        newThreat = e.getValue();
      }
    }
    
    if (currentRaid != null) {
      oldPick = currentRaid.subject();
      oldThreat = threatRatings.get(oldPick);
    }
    
    if (oldThreat == null) oldThreat = 0f;
    if (newThreat > oldThreat * 1.5f) return newPick;
    return null;
  }
  
  
  private void updateThreats() {
    final Target point = world.presences.randomMatchNear(base, null, -1);
    
    final Batch <Venue> sample = new Batch <Venue> ();
    Venue assaults = null, lastAssault = raidedVenue();
    float bestThreat = 0;
    
    for (Base other : world.bases()) if (other != base) {
      sample.clear();
      if (lastAssault != null && lastAssault.base() == other) {
        sample.add(lastAssault);
      }
      world.presences.sampleFromMap(point, world, 10, sample, other);
      
      for (Venue v : sample) {
        final float threat = threatFrom(point, v);
        if (threat > bestThreat) { assaults = v; bestThreat = threat; }
      }
    }
    
    bestThreat *= 1f / raidInterval;
    final Float oldThreat = threatRatings.get(assaults);
    if (oldThreat == null) threatRatings.put(assaults, bestThreat);
    else threatRatings.put(assaults, oldThreat + bestThreat);
  }
  
  
  private float threatFrom(Target point, Venue venue) {
    final Base other = venue.base();
    float threat = 0;
    
    threat = 1f - other.relations.communitySpirit();
    
    float avgDist = Spacing.distance(point, venue);
    threat *= 1f / (1 + (avgDist / World.SECTOR_SIZE));
    
    threat *= 0 - base.relations.relationWith(other);
    
    //TODO:  Include an assessment of how much you dislike the inhabitants of
    //the venue in question, and how much the other base values the venue.
    return threat;
  }
}






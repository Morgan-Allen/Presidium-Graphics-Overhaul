/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.game.economic.*;
import stratos.util.*;


//  GENERAL NOTE:

//  It... might be desirable for multiple bases belonging to the same faction
//  to share the same fog-of-war, even if they have different AI.  (That way
//  they can launch missions independently as needed.)  Okay.




public class ArtilectBase extends Base {
  
  
  
  /**  Data fields, constants, constructors and save/load methods-
    */
  private static boolean verbose = BaseTactics.updatesVerbose;
  
  final static float
    MAX_MISSION_POWER = CombatUtils.MAX_POWER * Mission.MAX_PARTY_LIMIT,
    ONLINE_WAKE_TIME  = Stage.STANDARD_YEAR_LENGTH / 2,
    SPAWN_INTERVAL    = Stage.STANDARD_DAY_LENGTH;
  
  
  private float onlineLevel = 0;
  private Venue chosenHQ = null;
  
  
  
  public ArtilectBase(Stage world) {
    super(world, true);
  }
  
  
  public ArtilectBase(Session s) throws Exception {
    super(s);
    this.onlineLevel = s.loadFloat();
    this.chosenHQ = (Venue) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveFloat(onlineLevel);
    s.saveObject(chosenHQ);
  }
  
  
  
  /**  Regular update methods-
    */
  public void setOnlineLevel(float toLevel) {
    this.onlineLevel = toLevel;
  }
  
  
  public float onlineLevel() {
    return onlineLevel;
  }
  
  
  private float rateHQ(Venue chosen) {
    if (chosen == null) return 0;
    return 0 - dangerMap.sampleAround(chosen, -1);
  }
  
  
  public Property HQ() {
    return chosenHQ;
  }
  
  
  protected void updateSpawning(Ruins ruins, int period) {
    final float spawnChance = period * 1f / SPAWN_INTERVAL;

    for (Species s : Ruins.SPECIES) {
      final Actor adds = s.sampleFor(this);
      if (ruins.crowdRating(adds, s) < 1 && Rand.num() < spawnChance) {
        adds.enterWorldAt(ruins, world);
        adds.mind.setHome(ruins);
        break;
      }
    }
  }
  
  
  
  protected BaseTactics initTactics() { return new BaseTactics(this) {
    
    protected void updateMissionAssessments() {
      //
      //  As long as there's a technologically-advanced non-artilect base on
      //  the map, increment the 'wakeup' level.
      int numFoes = 0;
      for (Base other : world.bases()) {
        if (other.primal || base == other) continue;
        base.relations.setRelation(other, -1, false);
        if (world.presences.nearestMatch(other, null, -1) != null) {
          numFoes++;
        }
      }
      final int interval = updateInterval();
      final float inc = interval * 1f / ONLINE_WAKE_TIME;
      onlineLevel += inc * (numFoes > 0 ? numFoes : -1);
      onlineLevel = Nums.clamp(onlineLevel, 0, 100);
      
      //
      //  You also need to select a suitable headquarters as a launching-point
      //  for missions.
      final Pick <Venue> pickHQ = new Pick();
      pickHQ.compare(chosenHQ, rateHQ(chosenHQ) * 1.5f);
      for (Object o : world.presences.allMatches(base)) {
        final Venue v = (Venue) o;
        pickHQ.compare(v, rateHQ(v));
      }
      chosenHQ = pickHQ.result();
      
      if (verbose) {
        I.say("\nUpdating artilect tactics.");
        I.say("  Potential foes:      "+numFoes    );
        I.say("  Headquarters chosen: "+chosenHQ   );
        I.say("  Online level:        "+onlineLevel);
      }
      //
      //  Then perform other tasks as standard.
      super.updateMissionAssessments();
    }
    
    
    protected float rateMission(
      Mission mission,
      float relations, float targetValue, float harmLevel, float riskLevel
    ) {
      if (verbose) {
        I.say("\nAdjusting mission-rating for artilects: "+mission);
        I.say("  Old risk level: "+riskLevel);
        I.say("  Old target val: "+targetValue);
      }
      riskLevel = Nums.clamp(riskLevel - onlineLevel, 0, 1);
      if (harmLevel > 0 && relations <= 0) targetValue *= 1 + onlineLevel;
      
      final float rating = super.rateMission(
        mission, relations, targetValue, harmLevel, riskLevel
      );
      if (verbose) {
        I.say("  New risk level: "+riskLevel);
        I.say("  New target val: "+targetValue);
        I.say("  Rating is:      "+rating);
      }
      return rating;
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
  }; }
}











/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.CombatUtils;
import stratos.util.*;



//  Strike mission:  Structures the enemy values.  Vulnerability/lack of risk.
//                   Dislike of base.
//  Security mission:  The converse- structures valued highly for bases you
//                     like, in dangerous areas.
//  Contact mission:  Basically, where they have a good chance of success, but
//                    low motivation, for tasks where you have a low chance of
//                    success, but high motivation.
//  Recon mission:  Fog rating.  Demand for resources you haven't found.

//  TODO:  You'll have to give some thought to chaining missions together,
//  through (e.g, using a contact mission to gain applicants for a strike team.)
//  Also, ruler-personality.




public class BaseTactics {
  
  protected static boolean
    updatesVerbose = false,
    shortWaiting   = false,
    extraVerbose   = false;
  protected static String
    verboseBase    = Base.KEY_ARTILECTS;
  
  protected boolean shouldReport() {
    return updatesVerbose && I.matchOrNull(
      base.title(), verboseBase
    );
  }
  
  
  final static int
    MIN_MISSIONS = 1,
    MAX_MISSIONS = 5,
    
    //  TODO:  Make these variable?  Like raid-frequency/force-strength?
    APPLY_WAIT_DURATION   = Stage.STANDARD_DAY_LENGTH  * 2,
    DEFAULT_EVAL_INTERVAL = Stage.STANDARD_DAY_LENGTH  / 3,
    SHORT_EVAL_INTERVAL   = Stage.STANDARD_HOUR_LENGTH * 2,
    SHORT_WAIT_DURATION   = SHORT_EVAL_INTERVAL + 2       ;
  
  final static float
    DEFAULT_MIN_PARTY_STRENGTH = 1 / 1.5f,
    DEFAULT_MAX_PARTY_STRENGTH = 1 * 1.5f,
    MISSION_BEGUN_RATING_MULT  = 1.5f;
  
  static enum Launch { BEGIN, WAIT, CANCEL };
  
  
  final protected Base base;
  final List <Mission> missions = new List <Mission> ();
  private float forceStrength = -1;
  
  
  public BaseTactics(Base base) {
    this.base = base;
  }
  
  
  public void loadState(Session s) throws Exception {
    s.loadObjects(missions);
    forceStrength = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObjects(missions);
    s.saveFloat(forceStrength);
  }
  
  
  
  /**  Public queries-
    */
  public float forceStrength() {
    return forceStrength;
  }
  
  
  public List <Mission> allMissions() {
    return missions;
  }
  
  
  public void addMission(Mission t) {
    if (I.logEvents()) I.say("\nADDING MISSION: ("+base+" "+t+")");
    missions.include(t);
  }
  
  
  public void removeMission(Mission t) {
    if (I.logEvents()) I.say("\nREMOVING MISSION: ("+base+" "+t+")");
    missions.remove(t);
  }
  
  
  
  /**  Performing regular assessment updates-
    */
  public void updateTactics(int numUpdates) {
    final boolean report = shouldReport();
    
    for (Mission mission : missions) {
      mission.updateMission();
    }
    
    final int
      interval = shortWaiting ? SHORT_EVAL_INTERVAL : DEFAULT_EVAL_INTERVAL,
      period   = numUpdates % interval;
    
    if (period != 0) {
      if (report) I.say(
        "\n"+ (interval - period)+"/"+interval+" seconds until "+
        "tactics update for "+base
      );
      return;
    }
    
    //  TODO:  Consider doing direct assignments of actors to various missions
    //  here?
    this.forceStrength = estimateForceStrength();
    if (base.isBaseAI()) updateMissionAssessments();
  }
  
  
  protected void updateMissionAssessments() {
    final boolean report = shouldReport();
    if (report) I.say("\nUPDATING MISSION ASSESSMENTS FOR "+base);
    //
    //  Compile a list of all current and potential missions first.
    Batch <Mission> toAssess = new Batch <Mission> ();
    Visit.appendTo(toAssess, missions);
    addNewMissions(toAssess);
    //
    //  Then go to each mission, obtain a rating of it's importance (with a
    //  bonus if already underway/confirmed.)
    class Rating { Mission mission; float rating; }
    final Sorting <Rating> sorting = new Sorting <Rating> () {
      public int compare(Rating a, Rating b) {
        if (a.rating > b.rating) return -1;
        if (a.rating < b.rating) return  1;
        return 0;
      }
    };
    for (Mission mission : toAssess) {
      final Rating r = new Rating();
      r.rating  = rateMission(mission);
      r.mission = mission;
      if (report) I.say("  Rating "+r.rating+" for "+mission);
      
      final boolean
        official = missions.includes(mission),
        begun    = mission.hasBegun();
      if (official) r.rating *= MISSION_BEGUN_RATING_MULT;
      if (begun   ) r.rating *= MISSION_BEGUN_RATING_MULT;
      //
      //  If this is a new mission, then assign it a root priority based on its
      //  perceived importance, and see if it's time to begin (see below.)
      if (official & ! begun) checkToLaunchMission(mission, r.rating, report);
      sorting.add(r);
    }
    //
    //  Then, take the N best missions (where N is determined by overall
    //  manpower available,) and either begin them or allow them to continue.
    //  Then you simply terminate (or discard) the remainder.
    final Batch <Mission> active = new Batch <Mission> ();
    final int maxMissions = (int) Nums.clamp(
      forceStrength * 2f / CombatUtils.MAX_POWER,
      MIN_MISSIONS, MAX_MISSIONS
    );
    
    for (Rating r : sorting) {
      final Mission m = r.mission;
      final boolean begun = missions.includes(m);
      if (active.size() < maxMissions && r.rating > 0) {
        //
        //  If a distinct mission of this type hasn't been registered yet, then
        //  do so now:
        if (! begun) {
          if (base.matchingMission(m) != null) continue;
          if (report) I.say("  RECORDING NEW MISSION: "+m);
          addMission(m);
        }
        //
        //  All active missions have their launch-status checked-
        active.add(m);
        checkToLaunchMission(m, r.rating, report);
      }
      else if (begun) {
        if (report) I.say("  TERMINATING OLD MISSION: "+m);
        m.endMission(true);
      }
    }
  }
  
  
  protected void addNewMissions(Batch <Mission> toAssess) {
    final Batch <Venue> venues = this.getSampleVenues();
    for (Venue v : venues) {
      toAssess.add(new MissionStrike  (base, v));
      toAssess.add(new MissionSecurity(base, v));
    }
  }
  
  
  //  TODO:  Have a strength-limit (min/max) for each mission.  Each base can
  //  then override that.
  protected boolean shouldAllow(Actor actor, Mission mission) {
    final boolean report = shouldReport();
    final float
      actorChance = successChance(actor, mission),
      actorPower  = actorPower   (actor, mission),
      partyChance = successChance(mission),
      partyPower  = partyPower   (mission);
    final boolean approves = shouldAllow(
      actor, mission, actorChance, actorPower, partyChance, partyPower
    );
    if (report) {
      I.say("\nChecking to allow mission-application...");
      I.say("  Mission is:   "+mission    +" ("+mission.base+")");
      I.say("  Applicant:    "+actor      +" ("+actor.base()+")");
      I.say("  Actor chance: "+actorChance+" (power "+actorPower+")");
      I.say("  Party chance: "+partyChance+" (power "+partyPower+")");
      I.say("  Will approve? "+approves   );
    }
    return approves;
  }
  
  
  protected Launch launchDecision(Mission mission) {
    final float
      partyChance = successChance(mission),
      partyPower  = partyPower   (mission),
      timeOpen    = mission.timeOpen(),
      waitTime    = shortWaiting ? SHORT_WAIT_DURATION : APPLY_WAIT_DURATION,
      applied     = mission.applicants().size();
    boolean
      timeUp      = timeOpen >= waitTime,
      shouldBegin = false;
    if (timeOpen >  waitTime) shouldBegin = true ;
    if (applied  == 0       ) shouldBegin = false;
    shouldBegin &= this.shouldLaunch(mission, partyChance, partyPower, timeUp);
    
    if (! timeUp) return Launch.WAIT;
    return shouldBegin ? Launch.BEGIN : Launch.CANCEL;
  }
  
  
  private void checkToLaunchMission(
    Mission mission, float rating, boolean report
  ) {
    //  TODO:  LIMIT BASED ON FINANCES FOR NON-PRIMAL BASES (these also need
    //  to convert over to a non-Base-AI type once begun!)
    
    int priority  = Mission.PRIORITY_NOMINAL;
    priority     += Mission.PRIORITY_PARAMOUNT * rating;
    mission.assignPriority(priority);
    mission.setMissionType(Mission.TYPE_BASE_AI);
    final Launch launch = launchDecision(mission);
    
    if (report) {
      I.say("\nUpdating official mission: "+mission);
      I.say("  Assigned priority: "+mission.assignedPriority());
      I.say("  Assigned type:     "+mission.missionType     ());
      I.say("  Launch state:      "+launch);
    }
    
    //  TODO:  You might want to hold back a bit here, depending on the
    //  priority of the mission- expend fewer resources on lower-priority
    //  tasks.
    if (launch == Launch.BEGIN) {
      for (Actor applies : mission.applicants()) {
        mission.setApprovalFor(applies, true);
      }
      if (! mission.hasBegun()) mission.beginMission();
    }
    
    if (launch == Launch.CANCEL) {
      mission.endMission(false);
    }
  }
  
  
  
  /**  Utility methods for rating the importance and strength of missions,
    *  parties and candidates-
    */
  protected boolean shouldAllow(
    Actor actor, Mission mission,
    float actorChance, float actorPower,
    float partyChance, float partyPower
  ) {
    float strength = (actorChance + partyChance) * 2;
    //  TODO:  BASE OFF COURAGE, etc.
    return strength <= DEFAULT_MAX_PARTY_STRENGTH;
  }
  
  
  protected boolean shouldLaunch(
    Mission mission, float partyChance, float partyPower, boolean timeUp
  ) {
    float strength = partyChance * 2;
    //  TODO:  BASE OFF COURAGE, etc.
    return strength > DEFAULT_MIN_PARTY_STRENGTH;
  }
  
  
  protected float rateMission(Mission mission) {
    return mission.rateImportance(base);
  }
  
  
  protected float rateApplicant(Actor actor, Mission mission) {
    return successChance(actor, mission);
  }
  
  
  private float successChance(Actor a, Mission m) {
    final Behaviour step = m.nextStepFor(a, true);
    return (step instanceof Plan) ? ((Plan) step).successChanceFor(a) : 1;
  }
  
  
  private float successChance(Mission mission) {
    float sumChances = 0;
    for (Actor a : mission.applicants()) {
      sumChances += successChance(a, mission);
    }
    return sumChances;
  }
  
  
  private float actorPower(Actor actor, Mission mission) {
    return actor.senses.powerLevel();
  }
  
  
  private float partyPower(Mission mission) {
    float power = 0;
    for (Actor a : mission.applicants()) {
      power += actorPower(a, mission);
    }
    return power;
  }
  
  
  
  
  /**  Utility methods for estimating overall strength/base-power and for
    *  getting target-batches to sample.
    */
  protected float estimateForceStrength() {
    //  TODO:  Try to ensure this stays as efficient as possible- in fact, you
    //  might as well update for all bases at once!
    
    float est = 0;
    //  TODO:  Use base-profiles instead.
    
    for (Mobile m : base.world.allMobiles()) {
      if (m.base() != base || ! (m instanceof Actor)) continue;
      final Actor a = (Actor) m;
      if (a.health.alive()) est += a.senses.powerLevel();
    }
    //  TODO:  Get ratings for various skill-types relevant to each mission- or
    //  perform some kind of monte-carlo sampling to determine success-odds.
    
    est += 0 - base.dangerMap.globalValue();
    return est / 2;
  }
  
  
  protected Batch <Venue> getSampleVenues() {
    return addSamples(new Batch(), Venue.class);
  }
  
  
  protected Batch <Mobile> getSampleMobiles() {
    return addSamples(new Batch(), Mobile.class);
  }
  
  
  protected Batch addSamples(Batch sampled, Object typeKey) {
    final Property baseHQ = base.HQ();
    if (baseHQ == null) {
      if (I.logEvents()) {
        I.say("\nWARNING: "+base+" has no HQ, cannot get mission targets.");
      }
      return sampled;
    }
    
    final PresenceMap sampFrom = base.world.presences.mapFor(typeKey);
    final int limit = Nums.max(10, sampFrom.population() / 100);
    
    for (Target t : sampFrom.visitNear(null, -1, null)) {
      if (! checkReachability(t, baseHQ)) continue;
      sampled.add((Mobile) t);
      if (sampled.size() >= limit) break;
    }
    return sampled;
  }
  
  
  protected boolean checkReachability(Target t, Target baseHQ) {
    final Tile reachPoint = Spacing.nearestOpenTile(t, baseHQ);
    return base.world.pathingCache.hasPathBetween(
      baseHQ, reachPoint, base, false
    );
  }
  
  
  protected Batch <StageRegion> getSampleSections() {
    final Batch <StageRegion> sampled = new Batch <StageRegion> ();
    return sampled;
  }
}








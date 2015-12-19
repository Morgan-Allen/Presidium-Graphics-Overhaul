/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.game.verse.*;
import stratos.util.*;



//  Strike mission:  Structures the enemy values.  Vulnerability/lack of risk.
//                   Dislike of base.
//  Security mission:  The converse- structures valued highly for bases you
//                     like, in dangerous areas.
//  Contact mission:  Basically, where they have a good chance of success, but
//                    low motivation, for tasks where you have a low chance of
//                    success, but high motivation.
//  Recon mission:  Fog rating.  Demand for resources you haven't found.



//  TODO:  This will have to be associated passively with Factions, probably in
//         the Verse class.

//  TODO:  Base any decisions (almost) purely off information from a
//         Demographic object derived from a Base.

//  You can evaluate total force-strength for a given base (either local or
//  offworld.)  Then evaluate the importance of said mission.  (The missions
//  themselves can give a basic rating.)  Then you can allow staff to apply
//  locally (or randomly generate applicants) up to a certain total strength.

//  ...And then you launch, creating a journey if necessary to transport the
//  troops.  (The JoinMission class can handle that, I think.)



//  TODO:  Consider making this class abstract, so that you have to create a
//         CivicBase/CivicBaseAI, et cetera?


public class FactionAI {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final static int
    MIN_MISSIONS = 1,
    MAX_MISSIONS = 5,
    
    //  TODO:  Make these variable?  Like raid-frequency/force-strength?
    MISSION_WAIT_DURATION = Stage.STANDARD_DAY_LENGTH  * 2,
    DEFAULT_EVAL_INTERVAL = Stage.STANDARD_DAY_LENGTH  / 3,
    SHORT_EVAL_INTERVAL   = Stage.STANDARD_HOUR_LENGTH * 2,
    SHORT_WAIT_DURATION   = SHORT_EVAL_INTERVAL + 2       ;
  
  
  final protected Base base;
  final protected SectorBase sector;
  
  final List <Mission> missions = new List <Mission> ();
  private float forceStrength;
  
  
  public FactionAI(Base base) {
    this.base = base;
    this.sector = null;
  }
  
  
  public FactionAI(SectorBase demo) {
    this.base = null;
    this.sector = demo;
  }
  
  
  public void loadState(Session s) throws Exception {
    s.loadObjects(missions);
    forceStrength = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObjects(missions);
    s.saveFloat(forceStrength);
  }
  
  
  
  /**  Basic access methods-
    */
  public void addMission(Mission m) {
    missions.include(m);
  }
  
  
  public void removeMission(Mission m) {
    missions.remove(m);
  }
  
  
  public Series <Mission> allMissions() {
    return missions;
  }
  
  
  public float forceStrength() {
    return forceStrength;
  }
  
  
  
  /**  Calling regular updates-
    */
  public void updateForBase(int numUpdates) {
    if (base == null) return;
  }
  
  
  public void updateForSector() {
    if (sector == null) return;
  }
  
  
  protected int updateInterval() {
    return shortWaiting ? SHORT_EVAL_INTERVAL : DEFAULT_EVAL_INTERVAL;
  }
  
  
  protected int missionWaitInterval() {
    return shortWaiting ? SHORT_WAIT_DURATION : MISSION_WAIT_DURATION;
  }
  
  
  
  /**  Obtaining samples of 
    */
  protected Batch <Object> assembleSampleTargets() {
    final Batch <Object> sampled = new Batch();
    final Verse verse = base == null ? sector.universe : base.world.offworld;
    final Faction faction = base == null ? sector.faction() : base.faction();
    
    if (base != null) {
      addSamples(sampled, Venue .class, verse.world);
      addSamples(sampled, Mobile.class, verse.world);
    }
    for (SectorBase b : verse.sectorBases()) {
      if (b.faction() == faction) continue;
      sampled.add(b);
    }
    return sampled;
  }
  
  
  protected Batch addSamples(Batch sampled, Object typeKey, Stage world) {
    
    final Boarding origin = base == null ? null : base.HQ();
    if (origin == null && base != null) {
      if (I.logEvents()) {
        I.say("\nWARNING: "+this+" has no origin, cannot get mission targets.");
      }
      return sampled;
    }
    
    final PresenceMap sampFrom = base.world.presences.mapFor(typeKey);
    final int limit = Nums.max(10, sampFrom.population() / 100);
    
    for (Target t : sampFrom.visitNear(null, -1, null)) {
      if (origin != null && ! checkReachability(t, origin)) continue;
      sampled.add(t);
      if (sampled.size() >= limit) break;
    }
    return sampled;
  }
  
  
  protected boolean checkReachability(Target t, Boarding origin) {
    final Tile reachPoint = Spacing.nearestOpenTile(t, origin);
    return base.world.pathingMap.hasPathBetween(
      origin, reachPoint, base, false
    );
  }
  
  
  
  /**  Generating missions for the various targets assembled:
    */
  protected void addMissionsForTarget(Object target, Batch <Mission> added) {
    
    //  TODO:  The Mission-classes themselves need to specify if targets are
    //  valid or not.
    if (target instanceof Mobile) {
    }
    if (target instanceof Venue) {
    }
    if (target instanceof Tile) {
    }
    if (target instanceof SectorBase) {
    }
  }
  
  
  protected void compareMissionsVsCurrent(Batch <Mission> sampled) {
    //
    //  Declare no more than 3-5 missions at once (depending on intelligence or
    //  admin-skills of ruler and/or forces available.)
    
    //
    //  Only declare a new mission if an old one has failed or expired, or the
    //  rating drops below half the next top contender.  Simple enough.
    
  }
  
  
  
  /**  Evaluation of missions and applicants-
    */
  public boolean allowsApplicant(Actor actor, Mission m) {
    return true;
  }
  
  
  protected boolean checkWorthLaunching(Mission m) {
    return true;
  }
  
  
  protected float rateMission(
    Mission mission,
    float relations, float targetValue,
    float harmLevel, float riskLevel
  ) {
    return 1;
  }
  
  
  protected void generateOffworldApplicants(Mission mission) {
    //
    //  Finally, once missions have been declared, then if you happen to be
    //  off-world, you generate applicants, generate a journey, generate an
    //  entry-point, check for pathability to the target, assign the applicants
    //  as passengers, and launch...
  }
  
  
  
  
  
  /**  Rendering, interface and debug functions-
    */
  public static boolean
    updatesVerbose = false,
    shortWaiting   = true ,
    extraVerbose   = false;
  protected static Faction
    verboseBase    = Faction.FACTION_ARTILECTS;

  
  protected boolean shouldReport() {
    if (! updatesVerbose) return false;
    if (base != null) return I.matchOrNull(base  .faction(), verboseBase);
    else              return I.matchOrNull(sector.faction(), verboseBase);
  }
}
  
  
  /*
  public static boolean
    updatesVerbose = false,
    shortWaiting   = true ,
    extraVerbose   = false;
  protected static Faction
    verboseBase    = Faction.FACTION_ARTILECTS;
  
  protected boolean shouldReport() {
    return updatesVerbose && I.matchOrNull(base.faction(), verboseBase);
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
  
  
  public FactionAI(Base base) {
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
  /*
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
  /*
  public void updateTactics(int numUpdates) {
    final boolean report = shouldReport();
    
    for (Mission mission : missions) {
      mission.updateMission();
    }
    
    final int
      interval = updateInterval(),
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
  
  
  protected int updateInterval() {
    return shortWaiting ? SHORT_EVAL_INTERVAL : DEFAULT_EVAL_INTERVAL;
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
      final Target target = mission.subjectAsTarget();
      final Base other = target == null ? null : target.base();
      final float
        relations  = base.relations.relationWith(other),
        value      = mission.targetValue(base),
        harmLevel  = mission.harmLevel(),
        baseForce  = 1 + base .tactics.forceStrength(),
        enemyForce = 1 + other.tactics.forceStrength(),
        risk       = enemyForce / (baseForce + enemyForce);
      
      r.rating  = rateMission(mission, relations, value, harmLevel, risk);
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
      if (r.rating > 0) sorting.add(r);
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
    shouldBegin &= shouldLaunch(mission, partyChance, partyPower, timeUp);
    
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
  /*
  protected void addNewMissions(Batch <Mission> toAssess) {
    final Batch <Venue> venues = getSampleVenues();
    for (Venue v : venues) addMissionsForVenue(v, toAssess);
    //
    //  TODO:  Assess actors as well.
  }
  
  
  protected void addMissionsForVenue(Venue v, Batch <Mission> toAssess) {
    toAssess.add(new MissionStrike  (base, v));
    toAssess.add(new MissionSecurity(base, v));
  }
  
  
  protected void addMissionsForActor(Mobile a, Batch <Mission> toAssess) {
    toAssess.add(new MissionStrike  (base, a));
    toAssess.add(new MissionSecurity(base, a));
  }
  
  
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
  
  
  protected float rateMission(
    Mission mission,
    float relations,
    float targetValue,
    float harmLevel,
    float riskLevel
  ) {
    //
    //  In the case of 'helpful' missions, rate importance based on current
    //  relations.
    if (harmLevel < 0) {
      return targetValue <= 0 ? 0 : (targetValue * relations * 2);
    }
    //
    //  In the case of 'harmful' missions, modify based on dislike as well as
    //  the risks of provocation.
    else if (harmLevel > 0) {
      return (targetValue * (0 - relations) * 2) - riskLevel;
    }
    //
    //  Otherwise, just return the naked value:
    else return targetValue;
  }
  
  
  protected float rateApplicant(Actor actor, Mission mission) {
    return successChance(actor, mission);
  }
  
  
  protected float successChance(Actor a, Mission m) {
    final Behaviour step = m.nextStepFor(a, true);
    return (step instanceof Plan) ? ((Plan) step).successChanceFor(a) : 1;
  }
  
  
  protected float actorPower(Actor actor, Mission mission) {
    return actor.senses.powerLevel();
  }
  
  
  protected float partyPower(Mission mission) {
    float power = 0;
    for (Actor a : mission.applicants()) {
      power += actorPower(a, mission);
    }
    return power;
  }
  
  
  protected float successChance(Mission mission) {
    float sumChances = 0;
    for (Actor a : mission.applicants()) {
      sumChances += successChance(a, mission);
    }
    return sumChances;
  }
  
  
  
  
  /**  Utility methods for estimating overall strength/base-power and for
    *  getting target-batches to sample.
    */
  /*
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
    
    //
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
      sampled.add(t);
      if (sampled.size() >= limit) break;
    }
    return sampled;
  }
  
  
  protected boolean checkReachability(Target t, Property baseHQ) {
    final Tile reachPoint = Spacing.nearestOpenTile(t, baseHQ);
    return base.world.pathingMap.hasPathBetween(
      baseHQ, reachPoint, base, false
    );
  }
  
  
  protected Batch <StageRegion> getSampleSections() {
    final Batch <StageRegion> sampled = new Batch <StageRegion> ();
    return sampled;
  }
  //*/
//}








/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.politic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
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
//  though (e.g, using a contact mission to gain applicants for a strike team.)
//  Also, ruler-personality.



//  So, if you have a base with strength 5, on a map with other bases of
//  strength 3 and 6, and it's contemplating an assault on a single target of
//  threat 2, then maximum proportion of manpower invested would be:
//    ~= 2 / 9 = 0.22, or strength 1.
//    (Since 2-3 strength is needed, this stretches out the wait duration to
//     5 or 6 days- probabilistically.  If assaulting *any* target is simply
//     beyond your capacity, then you should sue for peace or evacuate.)
//
//  So basically, there would be a 0.33 chance of declaring that mission at
//  all, and the total strength allocated would be no more than 3 (and no less
//  than 1.33, allowing 50% either way.)


//  NOTE:  The strength of other bases (and even your own), is proportionate to
//  relation *from -1 to 1, scaled in-between*- i.e, a neutral base counts as
//  half a threat, and half an ally.
//  Security is evaluated similarly, but for points you value and feel are
//  threatened.

//  But... how does this work for the Base-AI?  You'll need to limit some
//  applicants and actively recruit others- just as a leader would do.



public class BaseTactics {
  
  protected static boolean
    updatesVerbose = true ,
    shortWaiting   = true ,
    extraVerbose   = false;
  protected static String
    verboseBase = Base.KEY_ARTILECTS;
  
  final static int
    MIN_MISSIONS   = 1,
    MAX_MISSIONS   = 5,
    
    APPLY_WAIT_DURATION   = Stage.STANDARD_DAY_LENGTH  * 2,
    DEFAULT_EVAL_INTERVAL = Stage.STANDARD_DAY_LENGTH  / 3,
    SHORT_EVAL_INTERVAL   = Stage.STANDARD_HOUR_LENGTH * 2,
    SHORT_WAIT_DURATION   = SHORT_EVAL_INTERVAL + 2;
  final static float
    MIN_PARTY_STRENGTH = 1 / 1.5f,
    MAX_PARTY_STRENGTH = 1 * 1.5f;
  
  final Stage world;
  final Base  base ;
  final List <Mission> missions = new List <Mission> ();
  private float forceStrength = -1;
  //private float valueStrength = -1;  //  Sum of all bases!
  //  TODO:  MOVE THE MISSIONS OVER HERE
  
  
  public BaseTactics(Base base) {
    this.world = base.world;
    this.base  = base;
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
    missions.include(t);
  }
  
  
  public void removeMission(Mission t) {
    missions.remove(t);
  }
  
  
  
  /**  Performing regular assessment updates-
    */
  public void updateTactics(int numUpdates) {
    final boolean report = updatesVerbose && I.matchOrNull(
      base.title(), verboseBase
    );
    for (Mission mission : missions) {
      mission.updateMission();
    }
    
    updateMissionAssessment(numUpdates, report);
  }
  
  
  private void updateMissionAssessment(int numUpdates, boolean report) {
    //  TODO:  Bases should have an 'isRealPlayer' property...
    if (! base.primal) return;
    
    final int
      interval = shortWaiting ? SHORT_EVAL_INTERVAL : DEFAULT_EVAL_INTERVAL,
      period   = numUpdates % interval;
    
    if (period != 0) {
      if (report) {
        I.say("\nNot yet time for base-tactics update! "+base);
        I.say("  Period: "+period+"/"+interval);
      }
      return;
    }
    else if (report) {
      I.say("\nUPDATING MISSION ASSESSMENTS FOR "+base);
    }
    
    final float BEGUN_MULT = 1.5f;
    //  TODO:  You might want to vary this a bit, based on the admin qualities
    //  of your base.
    forceStrength = this.estimateForceStrength();
    int maxMissions = (int) Nums.ceil(forceStrength / Mission.MAX_PARTY_LIMIT);
    maxMissions     = (int) Nums.clamp(maxMissions, MIN_MISSIONS, MAX_MISSIONS);
    if (report) I.say("  Maximum missions allowed: "+maxMissions);
    
    Batch <Mission> toAssess = new Batch <Mission> ();
    Visit.appendTo(toAssess, missions);
    //
    //  Then get a bunch of sampled actors and venues, etc., and see if they
    //  are worth pursuing.
    final Batch <Venue > venues  = this.getSampleVenues();
    for (Venue v : venues) {
      toAssess.add(new StrikeMission  (base, v));
      toAssess.add(new SecurityMission(base, v));
    }
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
      r.rating  = mission.rateImportance(base);
      r.mission = mission;
      final boolean
        official = missions.includes(mission),
        begun    = mission.hasBegun();
      if (official) r.rating *= BEGUN_MULT;
      if (begun   ) r.rating *= BEGUN_MULT;
      //
      //  If this is a new mission, then assign it a root priority based on its
      //  perceived importance, and see if it's time to begin (see below.)
      if (official & ! begun) updateOfficialMission(mission, r.rating, report);
      sorting.add(r);
    }
    //
    //  Then, take the N best missions (where N is determined by overall
    //  manpower available,) and either begin them or allow them to continue.
    //  Then you simply terminate (or discard) the remainder.
    final Batch <Mission> active = new Batch <Mission> ();
    
    for (Rating r : sorting) {
      final Mission m = r.mission;
      final boolean begun = missions.includes(m);
      if (report && extraVerbose) {
        I.say("  Evaluated mission: "+r.mission);
        I.say("    Importance:      "+r.rating );
        I.say("    Already begun?   "+begun    );
      }
      //
      //  If a distinct mission of this type hasn't been registered yet, then
      //  do so now:
      if (active.size() < maxMissions && r.rating > 0) {
        if (! begun) {
          if (base.matchingMission(m) != null) continue;
          if (report) I.say("  BEGINNING NEW MISSION: "+m);
          addMission(m);
          updateOfficialMission(m, r.rating, report);
        }
        active.add(m);
      }
      else {
        if (begun) {
          if (report) I.say("  TERMINATING OLD MISSION: "+m);
          m.endMission(true);
        }
      }
    }
  }
  
  
  protected void updateOfficialMission(
    Mission mission, float rating, boolean report
  ) {
    
    //  TODO:  LIMIT BASED ON FINANCES FOR NON-PRIMAL BASES (these also need
    //  to convert over to a non-Base-AI type once begun!)
    int priority =  Mission.PRIORITY_NOMINAL;
    priority     += Mission.PRIORITY_PARAMOUNT * rating;
    mission.assignPriority(priority);
    mission.setMissionType(Mission.TYPE_BASE_AI);
    
    if (report) {
      I.say("\nUpdating official mission: "+mission);
      I.say("  Assigned priority: "+mission.assignedPriority());
      I.say("  Assigned type:     "+mission.missionType     ());
    }
    
    //  TODO:  You could also use different criteria here (e.g, the party is
    //  full.)
    final float
      strength = successChance(mission) * 2,
      timeOpen = mission.timeOpen(),
      waitTime = shortWaiting ? SHORT_WAIT_DURATION : APPLY_WAIT_DURATION,
      applied  = mission.applicants().size();
    boolean
      timeUp      = timeOpen >= waitTime,
      shouldBegin = false;
    if (timeOpen > waitTime) shouldBegin = true;
    if (applied == 0) shouldBegin = false;
    if (report) {
      I.say("  Total applicants: "+applied);
      I.say("  Time open:        "+timeOpen+"/"+waitTime);
      I.say("  Party strength:   "+strength);
      I.say("  Applied are:");
      for (Actor a : mission.applicants()) {
        final float actorChance = chanceFrom(mission, a);
        I.say("    "+a+" chance: "+actorChance);
      }
    }
    
    //  TODO:  You might want to hold back a bit here, depending on the
    //  priority of the mission- expend fewer resources on lower-priority
    //  tasks.
    if (shouldBegin) {
      if (strength > MIN_PARTY_STRENGTH && Rand.num() < strength) {
        if (report) I.say("  STARTING MISSION NOW!");
        for (Actor applies : mission.applicants()) {
          mission.setApprovalFor(applies, true);
        }
        mission.beginMission();
      }
      else {
        if (report) I.say("  INSUFFICIENT PARTY STRENGTH- CANCELLING");
        mission.endMission(false);
      }
    }
  }
  
  
  private float chanceFrom(Mission m, Actor a) {
    final Behaviour step = m.cachedStepFor(a, true);
    return (step instanceof Plan) ? ((Plan) step).successChanceFor(a) : 1;
  }
  
  
  protected float successChance(Mission mission) {
    float sumChances = 0;
    for (Actor a : mission.applicants()) sumChances += chanceFrom(mission, a);
    return sumChances;
  }
  
  
  //  TODO:  Merge with method in Mission class.  Or save this for when the
  //  mission is due evaluation?
  
  protected boolean shouldApprove(Actor actor, Mission mission) {
    final boolean report = updatesVerbose && (
      I.talkAbout == actor || I.talkAbout == mission
    );
    if (! isCompetent(actor, mission)) {
      if (report) I.say("\n"+actor+" not competent to pursue "+mission);
      return false;
    }
    
    //  A 50% chance of victory implies equal strength.
    final float
      actorChance = chanceFrom(mission, actor),
      partyChance = successChance(mission),
      strength    = (actorChance + partyChance) * 2;
    final boolean
      approves    = strength <= MAX_PARTY_STRENGTH;
    
    if (report) {
      I.say("\nShould approve "+actor+" for "+mission+"?");
      I.say("  Actor success chance: "+actorChance);
      I.say("  Party success chance: "+partyChance);
      I.say("  Total party strength: "+strength);
      for (Actor a : mission.applicants()) {
        final float otherChance = chanceFrom(mission, a);
        I.say("    "+a+" chance: "+otherChance);
      }
      I.say("  Maximum strength:     "+MAX_PARTY_STRENGTH);
      I.say("  Will approve?         "+approves);
    }
    return approves;
  }
  
  
  protected boolean isCompetent(Actor actor, Mission mission) {
    if (! (actor instanceof Human)) return true;
    
    final Behaviour step = mission.cachedStepFor(actor, true);
    step.priorityFor(actor);
    if (step instanceof Plan && ((Plan) step).competence() < 1) return false;
    return true;
  }
  
  
  
  /**  Utility methods for estimating overall strength/base-power and for
    *  getting target-batches to sample.
    */
  protected float estimateForceStrength() {
    //  TODO:  Try to ensure this stays as efficient as possible- in fact, you
    //  might as well update for all bases at once!
    //*
    float est = 0;
    for (Mobile m : world.allMobiles()) {
      if (m.base() != base || ! (m instanceof Actor)) continue;
      final Actor a = (Actor) m;
      est += a.senses.powerLevel();
    }
    //  TODO:  Get ratings for various skill-types relevant to each mission- or
    //  perform some kind of monte-carlo sampling to determine success-odds.
    
    est += 0 - base.dangerMap.globalValue();
    return est / 2;
  }
  
  
  protected Batch <Venue> getSampleVenues() {
    final Batch <Venue> sampled = new Batch <Venue> ();
    final PresenceMap venues = world.presences.mapFor(Venue.class);
    final int limit = Nums.max(10, venues.population() / 100);
    for (Target t : venues.visitNear(null, -1, null)) {
      sampled.add((Venue) t);
      if (sampled.size() >= limit) break;
    }
    return sampled;
  }
  
  
  protected Batch <Mobile> getSampleActors() {
    final Batch <Mobile> sampled = new Batch <Mobile> ();
    final PresenceMap mobiles = world.presences.mapFor(Mobile.class);
    final int limit = Nums.max(10, mobiles.population() / 100);
    for (Target t : mobiles.visitNear(null, -1, null)) {
      sampled.add((Mobile) t);
      if (sampled.size() >= limit) break;
    }
    return sampled;
  }
  
  
  protected Batch <StageSection> getSampleSections() {
    final Batch <StageSection> sampled = new Batch <StageSection> ();
    return sampled;
  }
}


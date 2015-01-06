/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.politic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.user.BaseUI;
import stratos.util.*;

import java.util.Map;


//  This should actually be practicable now.

//  Strike mission:  Structures the enemy values.  Vulnerability/lack of risk.
//                   Dislike of base.
//  Security mission:  The converse- structures valued highly for bases you
//                     like, in dangerous areas.
//  Contact mission:  Basically, where they have a good chance of success, but
//                    low motivation, for tasks where you have a low chance of
//                    success, but high motivation.
//  Recon mission:  Fog rating.  Demand for resources you haven't found.

//  Give the mission two days to attract applicants (or be assigned some,) then
//  launch.  (Base the max. number of missions on your cash reserves, available
//  manpower, and desperation.)

//  TODO:  You'll have to give some thought to chaining missions together,
//  though (e.g, using a contact mission to gain applicants for a strike team.)

//  TODO:  For the moment, just focus on strike and security missions.  Later,
//         you'll also want to factor in ruler-personality.


public class BaseTactics {
  
  public static boolean
    updatesVerbose = true ;
  
  final static int
    MIN_MISSIONS   = 1,
    MAX_MISSIONS   = 5,
    
    APPLY_WAIT_DURATION = Stage.STANDARD_DAY_LENGTH * 2;
  
  final Stage world;
  final Base  base ;
  private float forceStrength = -1;
  //  TODO:  MOVE THE MISSIONS OVER HERE
  
  
  public BaseTactics(Base base) {
    this.world = base.world;
    this.base  = base;
  }
  
  
  public void loadState(Session s) throws Exception {
    forceStrength = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveFloat(forceStrength);
  }
  
  
  public float forceStrength() {
    return forceStrength;
  }
  
  
  
  /**  Performing regular assessment updates-
    */
  public void updateMissionAssessment() {
    final boolean report = updatesVerbose;// && BaseUI.currentPlayed() == base;
    
    //  TODO:  Bases should have an 'isHuman' property.
    if (base == BaseUI.currentPlayed()) return;
    if (report) {
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
    Visit.appendTo(toAssess, base.allMissions());
    //
    //  Then get a bunch of sampled actors and venues, etc., and see if they
    //  are worth pursuing.
    final Batch <Venue > venues  = this.getSampleVenues();
    final Batch <Mobile> mobiles = this.getSampleActors();
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
      if (base.allMissions().includes(mission)) r.rating *= BEGUN_MULT;
      if (mission.hasBegun()) r.rating *= BEGUN_MULT;
      //
      //  If this is a new mission, then assign it a root priority based on its
      //  perceived importance, and see if it's time to begin (see below.)
      else checkForMissionStart(mission, r.rating);
      sorting.add(r);
    }
    //
    //  Then, take the N best missions (where N is determined by overall
    //  manpower available,) and either begin them or allow them to continue.
    //  Then you simply terminate (or discard) the remainder.
    int numActive = 0;
    for (Rating r : sorting) {
      final boolean begun = r.mission.hasBegun();
      if (report) {
        I.say("  Evaluated mission: "+r.mission);
        I.say("    Importance:      "+r.rating );
        I.say("    Already begun?   "+begun    );
      }
      if (numActive < maxMissions && r.rating > 0) {
        if (! begun) {
          if (report) I.say("  BEGINNING NEW MISSION: "+r.mission);
          base.addMission(r.mission);
        }
        numActive++;
      }
      else {
        if (begun) {
          if (report) I.say("  TERMINATING OLD MISSION: "+r.mission);
          r.mission.endMission();
        }
      }
    }
  }
  
  
  protected void checkForMissionStart(Mission mission, float rating) {
    //  TODO:  You could also use different criteria here (e.g, the party is
    //  full.)
    if (mission.timeOpen() > APPLY_WAIT_DURATION && ! mission.hasBegun()) {
      //  TODO:  You might want to hold back a bit here, depending on the
      //  priority of the mission- expend fewer resources on lower-priority
      //  tasks.
      for (Actor applies : mission.applicants()) {
        mission.setApprovalFor(applies, true);
      }
      mission.beginMission();
    }
    else {
      //  TODO:  LIMIT BASED ON FINANCES FOR NON-PRIMAL BASES- these also need
      //  to convert over to a non-Base-AI type once begun.
      int priority =  Mission.PRIORITY_NOMINAL;
      priority     += Mission.PRIORITY_PARAMOUNT * rating;
      mission.assignPriority(priority);
      mission.setMissionType(Mission.TYPE_BASE_AI);
    }
  }
  
  
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
    
    est += 0 - base.dangerMap.overallValue();
    return est / 2;
  }
  
  
  
  /**  Utility sampling methods:  TODO:  MOVE TO PRESENCES CLASS?
    */
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


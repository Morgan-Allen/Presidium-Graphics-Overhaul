


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



//  TODO:  You can most likely remove the admin requirement, at least for
//  public bounties.

//  TODO:  Make this into a preliminary step for *all* missions- where you
//         apply and wait for other applicants when possible.


public class JoinMission extends Plan {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  private static boolean
    evalVerbose  = false,
    stepsVerbose = false;
  final Mission mission;
  
  
  private JoinMission(Actor actor, Mission mission) {
    super(actor, actor, MOTIVE_PERSONAL, NO_HARM);
    this.mission = mission;
  }


  public JoinMission(Session s) throws Exception {
    super(s);
    mission = (Mission) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(mission);
  }
  
  
  public Plan copyFor(Actor other) {
    return new JoinMission(other, mission);
  }
  
  
  
  /**  Assorted utility methods used externally-
    */
  public static JoinMission attemptFor(Actor actor) {
    if (actor.mind.mission() != null || ! actor.health.conscious()) {
      return null;
    }
    final boolean report = I.talkAbout == actor && evalVerbose;
    
    //  Find a mission that seems appealing at the moment (we disable culling
    //  of invalid plans, since missions might not have steps available until
    //  approved-)
    if (report) {
      I.say("\nEvaluating missions: "+actor+" ("+actor.mind.vocation()+")");
      I.say("  Total missions: "+actor.base().tactics.allMissions().size());
    }
    final Choice choice = new Choice(actor);
    //
    //  TODO:  Allow application for missions set by other bases!
    final Batch <Behaviour> steps    = new Batch <Behaviour> ();
    final Batch <Mission  > missions = new Batch <Mission  > ();
    
    for (Mission mission : actor.base().tactics.allMissions()) {
      if (! mission.canApply(actor)) {
        if (report) I.say("\n  Cannot apply for "+mission);
        continue;
      }
      
      final float
        competence = competence(actor, mission),
        urgency    = mission.assignedPriority();
      //
      //  TODO:  Compare against the competence of the best current applicant
      //  instead, and withdraw if you're low in the rankings.
      
      if (competence + (urgency / Mission.PRIORITY_PARAMOUNT) < 1) {
        if (report) {
          I.say("");
          I.say("  Cannot apply for    "+mission);
          I.say("  Mission urgency:    "+urgency);
          I.say("  Competence too low: "+competence);
        }
        continue;
      }
      
      final Behaviour step = mission.nextStepFor(actor, true);
      final float priority = step == null ? -1 : step.priorityFor(actor);
      
      choice  .add(step   );
      steps   .add(step   );
      missions.add(mission);
      
      if (report) {
        I.say("\n  Mission is: "+mission);
        I.say("  apply point:  "+applyPointFor(actor, mission));
        I.say("  next step:    "+mission.nextStepFor(actor, true));
        I.say("  priority:     "+priority);
      }
    }
    final Behaviour pickStep = choice.weightedPick();
    final int       index    = steps.indexOf(pickStep);
    final Mission   picked   = missions.atIndex(index);
    //
    //  And try to apply for it-
    if (report) I.say("Mission picked: "+picked);
    if (picked == null) return null;
    if (picked.missionType() == Mission.TYPE_PUBLIC) {
      actor.mind.assignMission(picked);
      return null;
    }
    else return new JoinMission(actor, picked);
  }
  
  
  private static Target applyPointFor(Actor actor, Mission mission) {
    final int type = mission.missionType();
    if (type == Mission.TYPE_BASE_AI) return actor;
    if (type == Mission.TYPE_PUBLIC ) return actor;
    
    //  TODO:  BE STRICTER ABOUT THIS
    if (mission.base().HQ() == null) return actor;
    return mission.base().HQ();
  }
  
  
  public static float competence(Actor actor, Mission mission) {
    if (! (actor instanceof Human)) return 1;
    final Behaviour step = mission.nextStepFor(actor, true);
    if (step == null) return 0;
    
    step.priorityFor(actor);
    if (step instanceof Plan) return ((Plan) step).competence();
    else return 1;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected float getPriority() {
    if (actor.mind.mission() == mission) return 0;
    return mission.priorityFor(actor);
  }
  
  
  //  TODO:  Use this instead of the mission-specific method?
  /*
  private static Venue applyPointFor(Actor actor, Mission mission) {
    if (mission.base().HQ() instanceof Venue) {
      return (Venue) mission.base().HQ();
    }
    else return null;
  }
  //*/
  
  
  protected Behaviour getNextStep() {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) I.say("\nGetting next step for joining "+mission);
    
    if (! mission.canApply(actor)) {
      if (report) {
        I.say("  Cannot apply!");
        I.say("  Finished/Begun: "+mission.finished()+"/"+mission.hasBegun());
        I.say("  Has approval:   "+mission.isApproved(actor));
      }
      interrupt(INTERRUPT_NO_PREREQ);
      return null;
    }
    
    if (mission.isApproved(actor)) {
      if (report) I.say("  Joining now.");
      final Action joins = new Action(
        actor, actor,
        this, "actionJoins",
        Action.LOOK, "Joining mission"
      );
      return joins;
    }
    
    final Target applyPoint = applyPointFor(actor, mission);
    if (applyPoint == null) {
      I.complain("NO APPLY POINT FOR "+mission);
      return null;
    }
    
    if (actor.mind.mission() != mission) {
      if (report) I.say("  Applying at "+applyPoint);
      final Action applies = new Action(
        actor, applyPoint,
        this, "actionApplies",
        Action.LOOK, "Applying for mission"
      );
      return applies;
    }
    else {
      if (report) I.say("  Waiting for OK at "+applyPoint);
      final Action waitForOK = new Action(
        actor, applyPoint,
        this, "actionWait",
        Action.STAND, "Waiting for party"
      );
      return waitForOK;
    }
  }
  
  
  public boolean actionJoins(Actor client, Actor self) {
    ///if (I.logEvents()) I.say("\n"+actor+" joining mission: "+mission);
    
    if (! mission.canApply(client)) return false;
    final boolean report = stepsVerbose && I.talkAbout == client;
    
    if (report) {
      I.say("");
      I.say("Joining mission:  "+mission);
      I.say("  Has begun?      "+mission.hasBegun());
      I.say("  Applicants:     "+mission.totalApplied());
    }
    client.mind.assignMission  (mission);
    client.mind.assignBehaviour(mission.nextStepFor(actor, true));
    return true;
  }
  
  
  public boolean actionApplies(Actor client, Target applyPoint) {
    if (! mission.canApply(client)) return false;
    client.mind.assignMission(mission);
    return true;
  }
  
  
  public boolean actionWait(Actor client, Target applyPoint) {
    return true;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("Joining mission: ");
    mission.describeMission(d);
  }
}




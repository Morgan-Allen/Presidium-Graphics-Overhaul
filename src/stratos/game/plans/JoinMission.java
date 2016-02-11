/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.verse.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;

import stratos.game.actors.Behaviour;
import stratos.game.actors.Choice;
import stratos.game.actors.Plan;



//  TODO:  You can most likely remove the admin requirement, at least for
//  public bounties.

//  TODO:  Make this into a preliminary step for *all* missions- where you
//         apply and wait for other applicants when possible.

public class JoinMission extends Plan implements Journey.Purpose {
  
  
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
    //
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

    for (Mission mission : actor.base().tactics.allMissions()) {
      if (! mission.canApply(actor)) {
        if (report) I.say("\n  Cannot apply for "+mission);
        continue;
      }
      final JoinMission j = JoinMission.resume(actor, mission);
      choice.add(j);
    }
    
    final JoinMission picked = (JoinMission) choice.weightedPick();
    if (report) I.say("Mission picked: "+picked);
    return picked;
  }
  
  
  public static JoinMission resume(Actor actor, Mission mission) {
    return new JoinMission(actor, mission);
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected float getPriority() {
    final boolean report = I.talkAbout == actor && evalVerbose;
    if (report) I.say("\n"+actor+" is assessing priority for "+mission);
    
    if (! mission.hasBegun()) {
      float competence  = mission.rateCompetence(actor);
      float competition = MissionUtils.competition(actor, mission);
      competition /= (0.5f + competence);
      if (mission.isApproved(actor)) competition /= 1.5f;
      
      if (report) I.say("  Competition is:  "+competition);
      if (competition >= 1) return -1;
    }
    
    if (mission.isOffworld()) {
      return mission.basePriority(actor);
    }
    else {
      final Behaviour step = mission.nextStepFor(actor, true);
      return step == null ? -1 : step.priorityFor(actor);
    }
  }
  
  
  public boolean isEmergency() {
    if (mission.isOffworld()) {
      return false;
    }
    else {
      final Behaviour step = mission.nextStepFor(actor, true);
      return step == null ? false : step.isEmergency();
    }
  }
  
  
  protected Behaviour getNextStep() {
    final boolean report = I.talkAbout == actor;// && stepsVerbose;
    if (report) {
      I.say("\nGetting next step for joining "+mission);
    }
    
    if (! mission.canApply(actor)) {
      if (report) {
        I.say("  Cannot apply!");
        I.say("  Finished/Begun: "+mission.finished()+"/"+mission.hasBegun());
        I.say("  Has approval:   "+mission.isApproved(actor));
      }
      interrupt(INTERRUPT_NO_PREREQ);
      return null;
    }
    
    Target applyPoint = mission.base().HQ();
    if (mission.missionType() == Mission.TYPE_PUBLIC) applyPoint = actor;
    
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
    
    if (! mission.isApproved(actor)) return null;
    if (! mission.hasBegun()       ) return null;
    if (  mission.finished()       ) return null;
    
    final Behaviour step;
    if (mission.isOffworld() && ! mission.resolved()) step = new Action(
      actor, mission.journey().migrantTransitPoint(),
      this, "actionBoards",
      Action.STAND, "Boarding "
    );
    else step = mission.nextStepFor(actor, true);
    final Action waiting = nextWaitAction(actor, step);
    
    if (report) I.say("  Next step is: "+step);
    
    if (waiting != null) return waiting;
    return step;
  }
  
  
  public boolean finished() {
    return mission.finished();
  }
  
  
  public void interrupt(String cause) {
    actor.mind.assignMission(null);
    super.interrupt(cause);
  }
  
  
  public boolean actionApplies(Actor client, Target applyPoint) {
    if (! mission.canApply(client)) return false;
    if (getPriority() <= 0) return false;
    
    client.mind.assignMission(mission);
    return true;
  }
  
  
  private Action nextWaitAction(Actor actor, Behaviour step) {
    if (step == null) return null;
    if (actor.senses.isEmergency()) return null;
    
    //  TODO:  Figure out the waiting-protocol here...
    
    /*
    for (Actor a : mission.approved()) {
      if (a == actor || a.planFocus(null, true) != subject) continue;
      
      final float dist = Spacing.distance(a, actor);
      if (dist < Stage.ZONE_SIZE * 2.5f && dist > Stage.ZONE_SIZE / 2) {
        final Action waits = new Action(
          actor, a,
          this, "actionWait",
          Action.TALK, "Waiting for "+a
        );
        waits.setPriority  (Action.URGENT);
        waits.setProperties(Action.RANGED | Action.QUICK);
        return waits;
      }
    }
    //*/
    return null;
  }
  
  
  public boolean actionWait(Actor actor, Target point) {
    return true;
  }
  
  
  public boolean actionBoards(Actor actor, Boarding transitPoint) {
    final Journey j = mission.journey();
    if (j == null) I.complain("\nNO JOURNEY FOR MISSION: "+mission);
    
    if (j.transport() == null) actor.exitToOffworld();
    else actor.goAboard(j.transport(), actor.world());
    
    j.addMigrant(actor);
    return true;
  }
  
  
  
  /**  Handling offworld behaviours-
    */
  public void onWorldExit() {
    return;
  }
  
  
  public void onWorldEntry() {
    return;
  }
  
  
  public void whileOffworld() {
    return;
  }
  
  
  public boolean doneOffworld() {
    return mission.resolved();
  }
  
  
  public Sector origin() {
    return mission.base().world.localSector();
  }
  
  
  public boolean acceptsTransport(Vehicle t, Journey j) {
    if (mission.journey() == j) return true;
    return j.forMission() && j.destination() == origin();
  }
  
  
  
  /**  Rendering, debug and interface methods-
    */
  public void describeBehaviour(Description d) {
    final Behaviour step = this.nextStep;
    if (step != null) {
      step.describeBehaviour(d);
    }
    else {
      d.append("On ");
      mission.describeMission(d);
    }
  }
}


















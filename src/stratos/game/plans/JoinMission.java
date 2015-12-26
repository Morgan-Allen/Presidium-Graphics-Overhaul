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
    final Batch <Behaviour> steps    = new Batch <Behaviour> ();
    final Batch <Mission  > missions = new Batch <Mission  > ();
    
    for (Mission mission : actor.base().tactics.allMissions()) {
      if (! mission.canApply(actor)) {
        if (report) I.say("\n  Cannot apply for "+mission);
        continue;
      }
      
      final Behaviour step = mission.nextStepFor(actor, true);
      final float
        priority    = step == null ? -1 : step.priorityFor(actor),
        competence  = MissionUtils.competence (actor, mission),
        competition = MissionUtils.competition(actor, mission),
        urgency     = mission.assignedPriority();

      if (competence + (urgency / Mission.PRIORITY_PARAMOUNT) < 1) {
        if (report) I.say("  Not competent for: "+mission+": "+competence);
        continue;
      }
      if (competition >= 1) {
        if (report) I.say("  Party at limit: "+mission);
        continue;
      }
      
      choice  .add(step   );
      steps   .add(step   );
      missions.add(mission);
      
      if (report) {
        I.say("\n  Mission is: "+mission);
        I.say("  next step:    "+mission.nextStepFor(actor, true));
        I.say("  competence:   "+competence);
        I.say("  competition:  "+competition);
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
  
  
  public static JoinMission resume(Actor actor, Mission mission) {
    return new JoinMission(actor, mission);
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
    
    final Target applyPoint = MissionUtils.applyPointFor(actor, mission);
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
    
    //  TODO:  You need an extra step for boarding a ship to the destination if
    //  the mission is offworld...
    
    //  TODO:  Okay.  In principle... I just have to add a few utility methods
    //         to the Journey and Mission class- and add a check to the AI and
    //         mission-factory methods to ensure that transport is available at
    //         all...  Okay then.
    
    if (mission.isOffworld()) {
      
      
      /*
      final Verse verse = actor.world().offworld;
      final Journey j = verse.journeys.nextJourneyBetween(
        verse.localSector(), mission.subjectSector(),
        mission.base(), false
      );
      if (j == null) return null;
      final Vehicle trans = j.transport;
      if (trans != null && ! trans.inWorld()) return null;
      return new Smuggling(actor, trans, verse.world, true);
      //*/
    }
    
    final Behaviour step = mission.nextStepFor(actor, true);
    final Action waiting = nextWaitAction(actor, step);
    
    if (waiting != null) return waiting;
    return step;
    
    /*
    else {
      if (report) I.say("  Waiting for OK at "+applyPoint);
      final Action waitForOK = new Action(
        actor, applyPoint,
        this, "actionWait",
        Action.STAND, "Waiting for party"
      );
      return waitForOK;
    }
    //*/
  }
  
  
  private Action nextWaitAction(Actor actor, Behaviour step) {
    if (step == null) return null;
    if (actor.senses.isEmergency()) return null;
    
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
    return null;
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
  

  
  
  //  TODO:  These need to be moved out to the JoinMission class!
  //*
  
  
  public boolean actionWait(Actor actor, Actor other) {
    return true;
  }
  //*/
  
  
  public boolean actionWait(Actor client, Target applyPoint) {
    return true;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("Joining mission: ");
    mission.describeMission(d);
  }
}












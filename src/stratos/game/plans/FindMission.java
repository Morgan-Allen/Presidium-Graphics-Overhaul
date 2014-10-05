


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.tactical.*;
import stratos.user.*;
import stratos.util.*;

import static stratos.game.actors.Qualities.*;
import static stratos.game.building.Economy.*;


//  TODO:  You can most likely remove the admin requirement, at least for
//  public bounties.


public class FindMission extends Plan {
  
  
  private static boolean verbose = false;
  final Mission mission;
  //final Venue admin;
  
  
  
  public static FindMission attemptFor(Actor actor) {
    if (actor.mind.mission() != null) return null;
    //final Venue admin = nearestAdminFor(actor);
    //if (admin == null) return null;
    
    final boolean report = verbose && I.talkAbout == actor;
    
    //  Find a mission that seems appealing at the moment (we disable culling
    //  of invalid plans, since missions might not have steps available until
    //  approved-)
    if (report) I.say("\nEvaluating missions...");
    final Choice choice = new Choice(actor) {
      protected boolean checkPlanValid(Behaviour b) {
        return true;
      }
    };
    for (Mission mission : actor.base().allMissions()) {
      final Venue HQ = missionHQ(actor, mission);
      final boolean open = mission.openToPublic();
      if (report) {
        I.say("\n  mission is: "+mission);
        I.say("  headquarters: "+HQ+", open to public? "+open);
        I.say("  priority: "+mission.priorityFor(actor));
        I.say("  next step: "+mission.nextStepFor(actor));
      }
      if (! open) continue;
      choice.add(mission);
    }
    final Mission picked = (Mission) choice.weightedPick();
    
    
    //  And try to apply for it-
    if (report) I.say("Mission picked: "+picked);
    if (picked == null) return null;
    return new FindMission(actor, picked);
  }
  

  private FindMission(Actor actor, Mission mission) {
    super(actor, mission.subject(), true);
    this.mission = mission;
    //this.admin = admin;
  }


  public FindMission(Session s) throws Exception {
    super(s);
    mission = (Mission) s.loadObject();
    //admin = (Venue) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(mission);
    //s.saveObject(admin);
  }
  
  
  public Plan copyFor(Actor other) {
    return new FindMission(other, mission);
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected float getPriority() {
    if (actor.mind.mission() == mission) return 0;
    
    //  TODO:  Vary this based on loyalty to the governor that declared the
    //  mission in question.
    
    return mission.priorityFor(actor);
    //float penalty = Plan.rangePenalty(admin, actor);
    //penalty += Plan.dangerPenalty(admin, actor);
    //return applies.priorityFor(actor) - (penalty / 2);
  }
  
  
  private static Venue missionHQ(Actor actor, Mission mission) {
    final Actor client = mission.base().ruler();
    if (client == null) return null;
    
    final Object HQ = client.mind.home();
    if (HQ instanceof Venue) return (Venue) HQ;
    else return null;
  }
  
  
  //  Couple of potential cases here.
  //  Public bounties.  (Attack flag only, open to anyone.)
  //  Screened mission- appointed by selection from household & guild heads.
  //                    actors may also petition for inclusion.
  //  Covert mission- must approach individually.
  
  
  protected Behaviour getNextStep() {
    
    if (mission.finished()) {
      abortBehaviour();
      return null;
    }
    
    boolean canJoin = false;

    //  TODO:  This needs to be a generalised rally point.
    final Venue HQ = missionHQ(actor, mission);
    
    if (mission.missionType() == Mission.TYPE_PUBLIC) canJoin = true;
    else if (mission.isApproved(actor)) canJoin = true;
    else if (HQ == null) canJoin = true;
    
    if (canJoin) {
      final Action joins = new Action(
        actor, actor,
        this, "actionJoins",
        Action.LOOK, "Joining mission"
      );
      return joins;
    }
    
    if (actor.mind.mission() != mission) {

      final Action applies = new Action(
        actor, HQ,
        this, "actionApplies",
        Action.LOOK, "Applying for mission"
      );
      return applies;
    }
    else {
      final Action waitForOK = new Action(
        actor, HQ,
        this, "action",
        Action.STAND, "Waiting for party"
      );
      return waitForOK;
    }
  }
  
  
  public boolean actionJoins(Actor client, Actor self) {
    client.mind.assignMission(mission);
    client.mind.assignBehaviour(mission);
    return true;
  }
  
  
  public boolean actionApplies(Actor client, Venue admin) {
    client.mind.assignMission(mission);
    return true;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("Joining mission: ");
    mission.describeBehaviour(d);
    //d.append(mission);
  }
}







package stratos.game.civilian ;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.tactical.*;
import stratos.user.*;
import stratos.util.*;



public class FindMission extends Plan implements Economy {
  
  
  private static boolean verbose = false;
  
  
  final Mission applies ;
  final Venue admin ;
  
  
  public static FindMission attemptFor(Actor actor) {
    if (actor.mind.mission() != null) return null;
    final Venue admin = Audit.nearestAdminFor(actor, false);
    if (admin == null) return null;
    
    //  Find a mission that seems appealing at the moment (we disable culling
    //  of invalid plans, since missions might not have steps available until
    //  approved-)
    if (verbose) I.sayAbout(actor, "\nEvaluating missions...");
    final Choice choice = new Choice(actor) {
      protected boolean checkPlanValid(Behaviour b) {
        return true;
      }
    };
    for (Mission mission : actor.base().allMissions()) {
      if (verbose && I.talkAbout == actor) {
        I.say("\n  mission is: "+mission);
        I.say("  priority: "+mission.priorityFor(actor));
        I.say("  next step: "+mission.nextStepFor(actor));
      }
      if (! mission.openToPublic()) continue ;
      choice.add(mission) ;
    }
    final Mission picked = (Mission) choice.weightedPick() ;
    
    
    //  And try to apply for it-
    if (verbose) I.sayAbout(actor, "Mission picked: "+picked);
    if (picked == null) return null ;
    return new FindMission(actor, picked, admin) ;
  }
  

  private FindMission(Actor actor, Mission mission, Venue admin) {
    super(actor, admin) ;
    this.applies = mission ;
    this.admin = admin ;
  }


  public FindMission(Session s) throws Exception {
    super(s) ;
    applies = (Mission) s.loadObject() ;
    admin = (Venue) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(applies) ;
    s.saveObject(admin) ;
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected float getPriority() {
    if (actor.mind.mission() == applies) return 0 ;
    
    float penalty = Plan.rangePenalty(admin, actor) ;
    penalty += Plan.dangerPenalty(admin, actor) ;
    return applies.priorityFor(actor) - (penalty / 2) ;
  }
  
  
  protected Behaviour getNextStep() {
    if (actor.mind.mission() == applies) return null ;
    if (applies.missionType() == Mission.TYPE_PUBLIC) {
      final Action joins = new Action(
        actor, actor,
        this, "actionJoins",
        Action.LOOK, "Joining mission"
      );
      return joins;
    }
    final Action applies = new Action(
      actor, admin,
      this, "actionApplies",
      Action.LOOK, "Applying for mission"
    );
    return applies;
  }
  
  
  public boolean actionJoins(Actor client, Actor self) {
    client.mind.assignMission(applies);
    client.mind.assignBehaviour(applies);
    return true;
  }
  
  
  public boolean actionApplies(Actor client, Venue admin) {
    client.mind.assignMission(applies) ;
    return true ;
  }
  
  
  public void describeBehaviour(Description d) {
    if (applies.missionType() == Mission.TYPE_PUBLIC) {
      d.append("Joining mission");
      return;
    }
    d.append("Applying for mission at ") ;
    d.append(admin) ;
  }
}




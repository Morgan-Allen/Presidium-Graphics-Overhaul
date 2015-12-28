/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.common.*;



public class MissionUtils {
  
  
  public static float competition(Actor actor, Mission mission) {
    if (mission.isApproved(actor)) return 0;
    final int
      priority   = mission.assignedPriority(),
      partyLimit = Mission.PARTY_LIMITS[priority];
    return mission.applicants().size() * 1f / partyLimit;
  }
  
  
  public static float competence(Actor actor, Mission mission) {
    final Behaviour step = mission.nextStepFor(actor, true);
    if (step == null) return 0;
    step.priorityFor(actor);
    if (step instanceof Plan) return ((Plan) step).competence();
    else return 1;
  }
  
  

  
  
  protected static float successChance(Mission mission) {
    float sumChances = 0;
    for (Actor a : mission.applicants()) {
      sumChances += competence(a, mission);
    }
    return sumChances;
  }
  
  
  protected static float partyPower(Mission mission) {
    float power = 0;
    for (Actor a : mission.applicants()) {
      power += a.senses.powerLevel();
    }
    return power;
  }
  
  

  
  
  public static void quickSetup(
    Mission mission, int priority, int type, Actor... toAssign
  ) {
    mission.assignPriority(priority > 0 ? priority : Mission.PRIORITY_ROUTINE);
    mission.setMissionType(type     > 0 ? type     : Mission.TYPE_PUBLIC     );
    for (Actor meets : toAssign) {
      meets.mind.assignMission(mission);
      mission.setApprovalFor(meets, true);
    }
    mission.base.tactics.addMission(mission);
    mission.beginMission();
  }
}








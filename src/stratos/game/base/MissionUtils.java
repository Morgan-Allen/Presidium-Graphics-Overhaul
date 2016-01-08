/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.util.*;



public class MissionUtils {
  
  
  public static float competition(Actor actor, Mission mission) {
    if (mission.isApproved(actor)) return 0;
    final int partyLimit = mission.partyLimit();
    return mission.roles.size() * 1f / partyLimit;
  }
  
  
  protected static float successChance(Mission mission) {
    float sumChances = 0;
    for (Actor a : mission.applicants()) {
      sumChances += mission.rateCompetence(a);
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








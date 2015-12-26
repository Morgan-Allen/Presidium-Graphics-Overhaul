/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.Vehicle;
import stratos.game.verse.*;



public class MissionUtils {
  
  
  public static Target applyPointFor(Actor actor, Mission mission) {
    final int type = mission.missionType();
    if (type == Mission.TYPE_BASE_AI) return actor;
    if (type == Mission.TYPE_PUBLIC ) return actor;
    
    //  TODO:  BE STRICTER ABOUT THIS!
    if (mission.base().HQ() == null) return actor;
    return mission.base().HQ();
  }
  

  /*
  public static boolean transportAvailable(Mission mission, boolean landed) {
    final Verse verse = mission.base().world.offworld;
    final Object subject = mission.subject;
    
    Sector orig = verse.localSector(), dest = null;
    if (subject instanceof Sector) dest = (Sector) subject;
    if (subject instanceof Mobile) dest = verse.currentSector((Mobile) subject);
    if (dest == null || dest == orig) return true;
    
    
    //  Rondezvous first, then head for either the border-tile or the vehicle.
    //  Which will have to be reserved at the time the mission is declared...
    //  and I think the Journey is the best place for that.  Done.
    
    //  Have the Airfields create the ships needed for this.
    
    final Journey j = verse.journeys.nextJourneyBetween(
      orig, dest, mission.base, false
    );
    
    if (j == null || j.transport() == null) return false;
    final Vehicle trans = j.transport();
    if (landed) return trans.inWorld();
    else return true;
  }
  //*/
  
  
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








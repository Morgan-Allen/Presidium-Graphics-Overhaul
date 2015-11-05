/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.util.*;



public class MissionSecurity extends Mission {
  
  
  /**  Field definitions, constants and save/load methods-
    */
  final static int DURATION_LENGTHS[] = {
    Stage.STANDARD_DAY_LENGTH / 2,
    Stage.STANDARD_DAY_LENGTH * 1,
    Stage.STANDARD_DAY_LENGTH * 2,
  };
  final static String DURATION_NAMES[] = {
    "12 hours security for ",
    "24 hours security for ",
    "48 hours security for ",
  };
  private static boolean verbose = false;
  
  
  float inceptTime = -1;
  
  
  public MissionSecurity(Base base, Element subject) {
    super(
      base, subject, SECURITY_MODEL,
      "Securing "+subject
    );
  }
  
  
  public MissionSecurity(Session s) throws Exception {
    super(s);
    inceptTime = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveFloat(inceptTime);
  }
  
  
  public float duration() {
    return DURATION_LENGTHS[objective()];
  }
  
  
  
  /**  Importance assessment-
    */
  public float targetValue(Base base) {
    //  TODO:  FILL THIS IN LATER
    return -1;
  }
  
  
  public float harmLevel() {
    return Plan.REAL_HELP * duration() / DURATION_LENGTHS[1];
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected boolean shouldEnd() {
    if (subjectAsTarget().destroyed()) return true;
    if (inceptTime == -1) return false;
    return (base.world.currentTime() - inceptTime) > duration();
  }
  
  
  public void beginMission() {
    if (hasBegun()) return;
    super.beginMission();
    inceptTime = base.world.currentTime();
  }
  
  
  
  protected Behaviour createStepFor(Actor actor) {
    if (finished()) return null;
    final Behaviour cached = nextStepFor(actor, false);
    if (cached != null) return cached;
    //  TODO:  Implement item salvage?
    
    final Patrolling patrol = Patrolling.aroundPerimeter(
      actor, (Element) subject, 0
    );
    final float basePriority = basePriority(actor);
    patrol.addMotives(Plan.MOTIVE_MISSION, basePriority);
    return cacheStepFor(actor, patrol);
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String progressDescriptor() {
    if (inceptTime == -1 || ! hasBegun()) return super.progressDescriptor();
    
    final int hours = (DURATION_LENGTHS[objective()] - (int) (
      base.world.currentTime() - inceptTime
    )) / Stage.STANDARD_HOUR_LENGTH;
    
    return super.progressDescriptor()+" ("+hours+" hours remain)";
  }
  
  
  public void describeMission(Description d) {
    d.append("Security Mission", this);
    d.append(" for ");
    d.append(subject);
  }
}






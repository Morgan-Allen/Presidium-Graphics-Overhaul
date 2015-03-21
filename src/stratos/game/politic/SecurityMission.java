/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.politic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.Patrolling;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



public class SecurityMission extends Mission implements Qualities {
  
  
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
  
  
  public SecurityMission(Base base, Target subject) {
    super(
      base, subject, SECURITY_MODEL,
      "Securing "+subject
    );
  }
  
  
  public SecurityMission(Session s) throws Exception {
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
  public float rateImportance(Base base) {
    //  TODO:  FILL THIS IN LATER
    return -1;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected boolean shouldEnd() {
    if (subject.destroyed()) return true;
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
      actor, (Element) subject, base.world
    );
    final float basePriority = basePriority(actor);
    patrol.addMotives(Plan.MOTIVE_MISSION, basePriority);
    return cacheStepFor(actor, patrol);
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String describeObjective(int objectIndex) {
    String desc = super.describeObjective(objectIndex);
    if (inceptTime != -1) {
      final int hours = (DURATION_LENGTHS[objectIndex] - (int) (
        base.world.currentTime() - inceptTime
      )) / Stage.STANDARD_HOUR_LENGTH;
      desc = hours+" more hours security for ";
    }
    return desc;
  }
  
  
  public String[] objectiveDescriptions() {
    return DURATION_NAMES;
  }
  
  
  public void describeMission(Description d) {
    d.append("On ");
    d.append("Security Mission", this);
    d.append(" for ");
    d.append(subject);
  }
}






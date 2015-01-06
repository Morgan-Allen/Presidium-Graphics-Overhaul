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
      base, subject,
      MissionsTab.SECURITY_MODEL,
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
    return DURATION_LENGTHS[objectIndex()];
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
  
  
  
  public Behaviour nextStepFor(Actor actor) {
    if (finished()) return null;
    final Behaviour cached = cachedStepFor(actor, false);
    if (cached != null) return cached;
    //  TODO:  Implement item salvage?
    
    final Patrolling patrol = Patrolling.aroundPerimeter(
      actor, (Element) subject, base.world
    );
    final float basePriority = basePriority(actor);
    patrol.setMotive(Plan.MOTIVE_MISSION, basePriority);
    return cacheStepFor(actor, patrol);
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected void describeObjective(Description d) {
    super.describeObjective(d);
    if (inceptTime != -1) {
      final int hours = (int) (
        (base.world.currentTime() - inceptTime) *
        24f / Stage.STANDARD_DAY_LENGTH
      );
      d.append(" ("+hours+" hours elapsed)");
    }
  }
  
  protected String[] objectiveDescriptions() {
    return DURATION_NAMES;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("On ");
    d.append("Security Mission", this);
    d.append(" around ");
    d.append(subject);
  }
}



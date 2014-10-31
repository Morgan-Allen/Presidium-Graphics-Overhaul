/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.tactical;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.civilian.*;
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
  
  
  
  /**  Behaviour implementation-
    */
  public float priorityFor(Actor actor) {
    if (actor == subject) return 0;
    final boolean report = verbose && I.talkAbout == actor;
    final float motive = basePriority(actor);
    if (motive <= 0) return 0;
    
    final Patrolling patrol = Patrolling.aroundPerimeter(
      actor, (Element) subject, subject.world()
    );
    patrol.setMotive(Plan.MOTIVE_MISSION, motive);
    
    final float priority = patrol.priorityFor(actor);
    if (report) {
      I.say("\nSecurity mission priority "+priority+" for "+actor);
      I.say("  Reward priority: "+motive);
      patrol.setMotive(Plan.MOTIVE_INIT, 0);
      I.say("  Intrinsic priority: "+patrol.priorityFor(actor));
    }
    return priority;
  }
  
  
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
  
  
  
  /**  Behaviour implementation-
    */
  //  TODO:  Consider implementing shifts or supply logistics, in the case of
  //  longer security details?
  
  public Behaviour nextStepFor(Actor actor) {
    ///final boolean report = verbose && I.talkAbout == actor;
    if (! isActive()) return null;
    
    //  TODO:  Implement item salvage?
    /*
    if (subject instanceof ItemDrop) {
      final ItemDrop SI = (ItemDrop) subject;
      final Recovery RS = new Recovery(actor, SI, admin);
      RS.setMotive(Plan.MOTIVE_DUTY, priority);
      choice.add(RS);
    }
    //*/
    
    return Patrolling.aroundPerimeter(
      actor, (Element) subject, base.world
    );
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



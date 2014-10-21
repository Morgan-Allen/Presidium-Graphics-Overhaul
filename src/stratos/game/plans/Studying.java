


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.user.*;
import stratos.util.*;

import static stratos.game.actors.Qualities.*;
import static stratos.game.building.Economy.*;



//  TODO:  Use this to perform domestic chores, cleanup, and child-minding.


public class Studying extends Plan {
  
  
  /**  Data fields, setup and save/load functions-
    */
  final static float STAY_TIME = World.STANDARD_HOUR_LENGTH;
  final static Skill NO_STUDY[] = new Skill[0];
  
  private static boolean verbose = false;
  
  final Venue venue;
  private float beginTime = -1;
  private Skill[] studied;
  
  
  public Studying(Actor actor, Venue studyAt) {
    super(actor, studyAt, false);
    this.venue = studyAt;
  }
  
  
  public Studying(Session s) throws Exception {
    super(s);
    this.venue = (Venue) s.loadObject();
    this.beginTime = s.loadFloat();
    this.studied = (Skill[]) s.loadObjectArray(Skill.class);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(venue);
    s.saveFloat(beginTime);
    s.saveObjectArray(studied);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Studying(other, venue);
  }
  
  
  
  /**  Evaluating targets and priority-
    */
  final static Skill BASE_SKILLS[] = { ACCOUNTING, INSCRIPTION };
  final static Trait BASE_TRAITS[] = { CURIOUS, AMBITIOUS, SOLITARY };

  protected float getPriority() {
    final boolean report = verbose && I.talkAbout == actor;
    if (! (actor instanceof Human)) return 0;
    
    if (studied == null) studied = toStudy();
    if (studied == NO_STUDY) return 0;
    
    return super.priorityForActorWith(
      actor, venue, CASUAL,
      NO_MODIFIER, MILD_HELP,
      NO_COMPETITION, BASE_SKILLS,
      BASE_TRAITS, PARTIAL_DISTANCE_CHECK, NO_FAIL_RISK,
      report
    );
  }
  
  
  private Skill[] toStudy() {
    
    final Background ambition = ((HumanMind) actor.mind).ambition();
    if (ambition == null) return NO_STUDY;
    
    final Batch <Skill> toStudy = new Batch <Skill> ();
    for (Skill s : ambition.skills()) {
      if (s.form != FORM_COGNITIVE) continue;
      final int knownLevel = (int) actor.traits.traitLevel(s);
      if (knownLevel >= ambition.skillLevel(s)) continue;
      toStudy.add(s);
    }
    if (toStudy.size() == 0) return NO_STUDY;
    
    return toStudy.toArray(Skill.class);
  }
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    
    final float time = actor.world().currentTime();
    if (beginTime == -1) beginTime = time;
    final float elapsed = time - beginTime;
    
    if (elapsed > STAY_TIME) {
      abortBehaviour();
      return null;
    }
    
    final Action study = new Action(
      actor, venue,
      this, "actionStudy",
      Action.LOOK, "Studying"
    );
    return study;
  }
  
  
  public boolean actionStudy(Actor actor, Venue venue) {
    if ((studied = toStudy()) == NO_STUDY) return false;
    
    //  TODO:  Include effects of accounting and inscription skills.
    
    final float XP = 1f / studied.length;
    for (Skill s : studied) {
      final int knownLevel = (int) actor.traits.traitLevel(s);
      actor.skills.practiceAgainst(knownLevel, XP, s);
    }
    return true;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("Studying at ");
    d.append(venue);
  }
}














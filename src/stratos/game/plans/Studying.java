


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.user.*;
import stratos.util.*;

import static stratos.game.actors.Qualities.*;
import static stratos.game.building.Economy.*;



//  TODO:  Allow for study of specific techniques.
//  TODO:  Allow upgrades at the archives to improve efficiency.
//  TODO:  Charge for the service?


public class Studying extends Plan {
  
  
  /**  Data fields, setup and save/load functions-
    */
  private static boolean
    evalVerbose  = false,
    studyVerbose = false,
    eventVerbose = false;
  
  final static float
    STAY_TIME = Stage.STANDARD_HOUR_LENGTH;
  
  final Venue venue;
  private float beginTime = -1;
  private Skill studied = null;
  
  
  public Studying(Actor actor, Venue studyAt) {
    super(actor, studyAt, false);
    this.venue = studyAt;
  }
  
  
  public Studying(Session s) throws Exception {
    super(s);
    this.venue     = (Venue) s.loadObject();
    this.beginTime = s.loadFloat();
    this.studied   = (Skill) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(venue    );
    s.saveFloat (beginTime);
    s.saveObject(studied  );
  }
  
  
  public Plan copyFor(Actor other) {
    return new Studying(other, venue);
  }
  
  
  
  /**  Evaluating targets and priority-
    */
  final static Skill BASE_SKILLS[] = { ACCOUNTING, INSCRIPTION };
  final static Trait BASE_TRAITS[] = { CURIOUS, AMBITIOUS, SOLITARY };

  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (! (actor instanceof Human)) return 0;
    if (! venue.isManned()) return 0;
    
    if (studied == null) studied = toStudy();
    if (studied == null) return 0;
    
    return priorityForActorWith(
      actor, venue, CASUAL,
      NO_MODIFIER, MILD_HELP,
      NO_COMPETITION, BASE_SKILLS,
      BASE_TRAITS, PARTIAL_DISTANCE_CHECK, NO_FAIL_RISK,
      report
    );
  }
  
  
  private Skill toStudy() {
    final boolean report = studyVerbose && I.talkAbout == actor;
    
    //  TODO:  Always include accounting and inscription as study options.
    final Pick <Skill> pick = new Pick <Skill> (null, 0);
    final Background ambition = FindWork.ambitionOf(actor);
    
    if (ambition != null) for (Skill s : ambition.skills()) {
      if (s.form != FORM_COGNITIVE) continue;
      final int knownLevel = (int) actor.traits.traitLevel(s);
      final float gap = (ambition.skillLevel(s) * 1.5f) - knownLevel;
      pick.compare(s, gap * Rand.num());
    }
    
    if (report) {
      I.say("\nGetting study regime for "+actor);
      I.say("  Ambition is: "+ambition+"  Key skills:");
      for (Skill s : ambition.skills()) {
        final int
          knownLevel = (int) actor.traits.traitLevel(s),
          needLevel  = ambition.skillLevel(s);
        I.say("    "+s+" "+knownLevel+"/"+needLevel);
      }
      I.say("  Skill to study: "+pick.result());
    }
    return pick.result();
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    
    final float time = actor.world().currentTime();
    if (beginTime == -1) beginTime = time;
    final float elapsed = time - beginTime;
    
    if (elapsed > STAY_TIME) {
      if (Rand.yes()) return null;
      else beginTime = time - (STAY_TIME * Rand.num());
    }
    final Action study = new Action(
      actor, venue,
      this, "actionStudy",
      Action.LOOK, "Studying"
    );
    return study;
  }
  
  
  public boolean actionStudy(Actor actor, Venue venue) {
    if (studied == null) return false;

    //  TODO:  Include effects of accounting and inscription skills.
    
    final int knownLevel = (int) actor.traits.traitLevel(studied);
    actor.skills.practiceAgainst(knownLevel, 1f, studied);
    return true;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("Studying ");
    d.append(studied);
    d.append(" at ");
    d.append(venue);
  }
}














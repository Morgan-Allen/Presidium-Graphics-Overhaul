


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.user.*;
import stratos.util.*;

import static stratos.game.actors.Qualities.*;
import static stratos.game.building.Economy.*;



//  TODO:  Use this to perform domestic chores, cleanup, and child-minding.


public class Supervision extends Plan {
  
  
  /**  Data fields, setup and save/load functions-
    */
  private static boolean
    verbose = false;
  
  final static float WAIT_TIME = Stage.STANDARD_HOUR_LENGTH;
  
  final Venue venue;
  private float beginTime = -1;
  
  
  public Supervision(Actor actor, Venue supervised) {
    super(actor, supervised, false);
    this.venue = supervised;
  }
  
  
  public Supervision(Session s) throws Exception {
    super(s);
    this.venue = (Venue) s.loadObject();
    this.beginTime = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(venue);
    s.saveFloat(beginTime);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Supervision(other, venue);
  }
  
  
  
  /**  Evaluating targets and priority-
    */
  final static Trait BASE_TRAITS[] = { RELAXED, IGNORANT, DUTIFUL };

  protected float getPriority() {
    final boolean report = verbose && I.talkAbout == actor;
    if (report) {
      I.say("\nAssessing priority for supervision of "+venue);
      I.say("  Value of subject: "+actor.relations.valueFor(subject));
    }
    
    return priorityForActorWith(
      actor, venue, ROUTINE,
      NO_MODIFIER, NO_HARM,
      FULL_COMPETITION, NO_FAIL_RISK,
      NO_SKILLS, BASE_TRAITS, PARTIAL_DISTANCE_CHECK,
      report
    );
  }
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    final float time = actor.world().currentTime();
    if (beginTime == -1) beginTime = time;
    final float elapsed = time - beginTime;
    
    if (elapsed > WAIT_TIME) {
      abortBehaviour();
      return null;
    }
    
    if (elapsed > WAIT_TIME / 2) {
      final Behaviour nextJob = venue.jobFor(actor);
      if (! (nextJob instanceof Supervision)) {
        abortBehaviour();
        return null;
      }
    }
    
    final Action supervise = new Action(
      actor, venue,
      this, "actionSupervise",
      Action.LOOK, "Supervising"
    );
    return supervise;
  }
  
  
  public boolean actionSupervise(Actor actor, Venue venue) {
    
    //  If you have any items demanded by the venue, put them away-
    for (Item i : actor.gear.allItems()) {
      if (i.refers != null || i.type.form != FORM_MATERIAL) continue;
      if (venue.stocks.demandFor(i.type) > 0) actor.gear.transfer(i, venue);
    }
    return true;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("Supervising ");
    d.append(venue);
  }
}






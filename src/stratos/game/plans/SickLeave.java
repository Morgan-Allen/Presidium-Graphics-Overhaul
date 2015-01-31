/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;



public class SickLeave extends Plan {
  
  
  private static boolean
    evalVerbose   = false,
    eventsVerbose = false;
  
  final Venue sickbay;
  private Condition sickness;
  private float visitCost;
  
  
  protected SickLeave(
    Actor actor, Venue sickbay, Condition sickness, float visitCost
  ) {
    super(actor, sickbay, MOTIVE_JOB, NO_HARM);
    this.sickbay   = sickbay  ;
    this.sickness  = sickness ;
    this.visitCost = visitCost;
  }
  
  
  public SickLeave(Session s) throws Exception {
    super(s);
    sickbay   = (Venue    ) s.loadObject();
    sickness  = (Condition) s.loadObject();
    visitCost = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(sickbay  );
    s.saveObject(sickness );
    s.saveFloat (visitCost);
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  
  /**  Priority and target evaluation-
    */
  final static Trait BASE_TRAITS[] = { HUMBLE, NERVOUS };
  
  
  public static SickLeave nextLeaveFor(
    Actor actor, Venue sickbay, float baseCost
  ) {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (report) I.say("\nGetting next sick leave for "+actor);
    
    final Treatment t = Treatment.nextTreatment(null, actor, sickbay);
    if (t == null) {
      if (report) I.say("  No treatment required!");
      return null;
    }
    final float visitCost = baseCost * (1 + t.severity());
    return new SickLeave(actor, sickbay, t.sickness, visitCost);
  }
  
  
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    
    if (sickbay == null || sickness == null) return -1;
    if (Treatment.hasTreatment(sickness, actor, hasBegun())) return -1;
    if (sickbay.crowdRating(actor, Backgrounds.AS_VISITOR) > 1) return -1;
    
    final float urgency = Treatment.dangerRating(sickness, actor);
    float modifier = NO_MODIFIER;
    
    if (visitCost > 0) {
      if (visitCost > actor.gear.credits()) return -1;
      modifier -= ActorMotives.greedPriority(actor, visitCost);
    }
    
    final float priority = priorityForActorWith(
      actor, sickbay,
      urgency * PARAMOUNT, modifier,
      NO_HARM, NO_COMPETITION,
      NO_FAIL_RISK, NO_SKILLS,
      BASE_TRAITS, NORMAL_DISTANCE_CHECK,
      report
    );
    if (report) {
      I.say("\nIntrinsic urgency: "+urgency);
      I.say("  Disease level    "+actor.traits.traitLevel(sickness));
    }
    return priority;
  }
  
  
  protected Behaviour getNextStep() {
    final Action leave = new Action(
      actor, sickbay,
      this, "actionLeave",
      Action.FALL, "Taking Sick Leave"
    );
    return leave;
  }
  
  
  public boolean actionLeave(Actor actor, PhysicianStation sickbay) {
    final boolean report = eventsVerbose && I.talkAbout == actor;
    if (report) I.say("...waiting for treatment.");
    
    return true;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    d.append("Seeking Treatment for ");
    d.append(sickness);
    d.append(" at ");
    d.append(sickbay);
  }
}









package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;




//  TODO:  It's possible the actor should be paying for this?


public class SickLeave extends Plan {
  
  
  private static boolean
    evalVerbose   = false,
    eventsVerbose = false;
  
  final Venue sickbay;
  private Condition sickness;
  
  
  protected SickLeave(Actor actor, Venue sickbay, Condition sickness) {
    super(actor, sickbay, MOTIVE_JOB, NO_HARM);
    this.sickbay  = sickbay ;
    this.sickness = sickness;
  }
  
  
  public SickLeave(Session s) throws Exception {
    super(s);
    sickbay  = (Venue    ) s.loadObject();
    sickness = (Condition) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(sickbay );
    s.saveObject(sickness);
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  
  /**  Priority and target evaluation-
    */
  final static Trait BASE_TRAITS[] = { HUMBLE, NERVOUS };
  
  
  public static SickLeave nextLeaveFor(Actor actor, Venue sickbay) {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (report) I.say("\nGetting next sick leave for "+actor);
    
    final Treatment t = Treatment.nextTreatment(null, actor, sickbay);
    return t == null ? null : new SickLeave(actor, sickbay, t.sickness);
  }
  
  
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    
    if (sickbay == null || sickness == null) return -1;
    if (Treatment.hasTreatment(sickness, actor, hasBegun())) return -1;
    
    if (sickbay.crowdRating(actor, Backgrounds.AS_VISITOR) > 1) return -1;
    
    final float urgency = Treatment.dangerRating(sickness, actor);
    final float priority = priorityForActorWith(
      actor, sickbay,
      urgency * PARAMOUNT, NO_MODIFIER,
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


/*
if (needed.baseUrgency() == 0) return 0;

final Item treatResult = needed.treatResult();
if (treatResult != null) {
  if (hasBegun()) { if (treatResult.amount >= 1) return 0; }
  else { if (treatResult.amount > 0) return 0; }
}

final float crowding = hasBegun() ? 0 :
  Plan.competition(SickLeave.class, sickbay, actor);
//
//  Modify for Psych Eval, since it's only needed in cases of severe bad
//  morale or for key personnel.
float impetus = needed.baseUrgency() - crowding;
if (needed.type == Treatment.TYPE_PSYCH_EVAL) {
  final Background v = actor.vocation();
  if (v.guild == Backgrounds.GUILD_MILITANT) return impetus;
  if (v.standing >= Backgrounds.UPPER_CLASS) return impetus;
  impetus *= (0.5f - actor.health.moraleLevel());
}

if (verbose) I.sayAbout(actor, "Sick leave impetus: "+impetus);
return Visit.clamp(impetus, 0, CRITICAL);
//*/








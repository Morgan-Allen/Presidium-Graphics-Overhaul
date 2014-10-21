

package stratos.game.plans;
import stratos.game.common.*;
import stratos.game.common.Session.Saveable;
import stratos.game.actors.*;
import stratos.game.base.Suspensor;
import stratos.game.building.*;
import stratos.util.*;




//  TODO:  Extend this more generally to capture and arrest.


public class StretcherDelivery extends Plan implements Qualities {
  
  
  private static boolean verbose = false;
  
  final Actor patient;
  final Boarding origin, destination;
  private Suspensor suspensor;
  
  
  public StretcherDelivery(
    Actor actor, Actor patient,
    Boarding destination
  ) {
    super(actor, patient, true);
    this.patient = patient;
    this.origin = patient.aboard();
    this.destination = destination;
  }
  
  
  public StretcherDelivery(Session s) throws Exception {
    super(s);
    this.patient = (Actor) s.loadObject();
    this.origin = (Boarding) s.loadTarget();
    this.destination = (Boarding) s.loadTarget();
    this.suspensor = (Suspensor) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(patient);
    s.saveTarget(origin);
    s.saveTarget(destination);
    s.saveObject(suspensor);
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected float getPriority() {
    final boolean report = verbose && I.talkAbout == actor;
    
    final float priority = priorityForActorWith(
      actor, patient, ROUTINE,
      NO_MODIFIER, NO_HARM,
      FULL_COMPETITION, NO_SKILLS,
      NO_TRAITS, PARTIAL_DISTANCE_CHECK, NO_FAIL_RISK,
      report
    );
    return priority;
  }
  
  
  protected Behaviour getNextStep() {
    final boolean report = verbose && I.talkAbout == actor;
    if ((! hasBegun()) && Plan.competition(this, patient, actor) > 0) {
      return null;
    }
    if (patient.aboard() == destination) return null;
    final Actor carries = Suspensor.carrying(patient);
    
    if (patient.aboard() == origin && carries == null) {
      if (report) I.say("Returning new pickup");
      final Action pickup = new Action(
        actor, patient,
        this, "actionPickup",
        Action.REACH_DOWN, "Picking up "
      );
      pickup.setMoveTarget(origin);
      return pickup;
    }
    
    if (carries == actor) {
      if (report) I.say("Returning new dropoff");
      final Action dropoff = new Action(
        actor, patient,
        this, "actionDropoff",
        Action.REACH_DOWN, "Dropping off "
      );
      dropoff.setMoveTarget(destination);
      return dropoff;
    }
    
    return null;
  }
  
  
  public boolean actionPickup(Actor actor, Actor patient) {
    if (Suspensor.carrying(patient) != null) return false;
    this.suspensor = new Suspensor(actor, this);
    suspensor.passenger = patient;
    suspensor.enterWorldAt(patient, actor.world());
    return true;
  }
  
  
  public boolean actionDropoff(Actor actor, Actor patient) {
    if (Suspensor.carrying(patient) != actor) {
      I.say("NOT CARRYING PATIENT!");
      return false;
    }
    suspensor.passenger = null;
    suspensor.exitWorld();
    this.suspensor = null;
    patient.goAboard(destination, actor.world());
    return true;
  }
  
  
  
  public void describeBehaviour(Description d) {
    d.append("Taking ");
    d.append(patient);
    d.append(" to ");
    d.append(destination);
  }
}



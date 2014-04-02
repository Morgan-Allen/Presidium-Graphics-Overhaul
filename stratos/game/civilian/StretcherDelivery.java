

package stratos.game.civilian;
import stratos.game.common.*;
import stratos.game.common.Session.Saveable;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.util.Description;
import stratos.util.I;



public class StretcherDelivery extends Plan implements Qualities {
  
  
  private static boolean verbose = false;
  
  final Actor patient;
  final Boardable origin, destination;
  private Suspensor suspensor;
  
  
  public StretcherDelivery(
    Actor actor, Actor patient,
    Boardable destination
  ) {
    super(actor, patient);
    this.patient = patient;
    this.origin = patient.aboard();
    this.destination = destination;
  }
  
  public StretcherDelivery(Session s) throws Exception {
    super(s);
    this.patient = (Actor) s.loadObject();
    this.origin = (Boardable) s.loadTarget();
    this.destination = (Boardable) s.loadTarget();
    this.suspensor = (Suspensor) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(patient);
    s.saveTarget(origin);
    s.saveTarget(destination);
    s.saveObject(suspensor);
  }
  
  
  
  public float priorityFor(Actor actor) {
    return priorityForActorWith(
      actor, patient, ROUTINE,
      REAL_HELP, FULL_COMPETITION,
      BASE_SKILLS, BASE_TRAITS,
      NO_MODIFIER, PARTIAL_DISTANCE_CHECK, NO_DANGER,
      verbose
    );
  }
  
  final Skill BASE_SKILLS[] = {};
  final Trait BASE_TRAITS[] = {};
  
  
  
  
  protected Behaviour getNextStep() {
    if ((! hasBegun()) && Plan.competition(this, patient, actor) > 0) {
      return null;
    }
    final Actor carries = Suspensor.carrying(patient);
    
    if (patient.aboard() == origin && carries == null) {
      final Action pickup = new Action(
        actor, patient,
        this, "actionPickup",
        Action.REACH_DOWN, "Picking up "
      );
      pickup.setMoveTarget(origin);
      return pickup;
    }
    
    if (carries == actor) {
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
    if (Suspensor.carrying(patient) != actor) return false;
    suspensor.passenger = null;
    suspensor.exitWorld();
    this.suspensor = null;
    patient.goAboard(destination, actor.world());
    return true;
  }
  
  
  
  public void describeBehaviour(Description d) {
    d.append("Delivering ");
    d.append(patient);
    d.append(" to ");
    d.append(destination);
  }
}



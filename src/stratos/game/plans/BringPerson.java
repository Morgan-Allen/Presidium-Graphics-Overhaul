/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.util.*;
import stratos.content.civic.Suspensor;
import static stratos.game.actors.Qualities.*;



//  TODO:  Allow the actor to carry the subject directly, if no other options
//  is available (e.g, for kidnappings.)


public class BringPerson extends Plan {
  
  
  private static boolean verbose = false;
  
  final Actor patient;
  final Boarding destination;
  private Mount platform = null;
  
  
  public BringPerson(
    Actor actor, Actor patient, Boarding destination
  ) {
    super(actor, patient, MOTIVE_JOB, NO_HARM);
    this.patient = patient;
    this.destination = destination;
  }
  
  
  public BringPerson(Session s) throws Exception {
    super(s);
    this.patient = (Actor) s.loadObject();
    this.destination = (Boarding) s.loadTarget();
    this.platform = (Suspensor) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(patient);
    s.saveTarget(destination);
    s.saveObject(platform);
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected float getPriority() {
    return PlanUtils.supportPriority(actor, patient, motiveBonus(), 1, 0.5f);
  }
  
  
  public static boolean canCarry(Actor a, Actor carried) {
    //
    //  TODO:  You might want to sophisticate this a bit...
    return true;
  }
  
  
  //  TODO:  Pick up the suspensor from either the origin or destination points
  //  (whichever is closer at the time?)
  
  
  protected Behaviour getNextStep() {
    final boolean report = I.talkAbout == actor && verbose;
    
    if ((! hasBegun()) && PlanUtils.competition(this, patient, actor) > 0) {
      return null;
    }
    if (patient.aboard() == destination) return null;
    final Actor carries = Suspensor.carrying(patient);
    
    if (carries == null) {
      if (report) I.say("Returning new pickup");
      final Action pickup = new Action(
        actor, patient,
        this, "actionPickup",
        Action.REACH_DOWN, "Picking up "
      );
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
    if (Suspensor.carrying(patient) != null) {
      return false;
    }
    else if (actor instanceof Mount) {
      this.platform = (Mount) actor;
      patient.bindToMount(platform);
    }
    else {
      this.platform = new Suspensor(actor, this);
      patient.bindToMount(platform);
      ((Suspensor) platform).enterWorldAt(patient, actor.world());
    }
    return true;
  }
  
  
  public boolean actionDropoff(Actor actor, Actor patient) {
    if (Suspensor.carrying(patient) != actor) {
      I.say("NOT CARRYING PATIENT!");
      return false;
    }
    else if (platform == actor) {
      patient.releaseFromMount();
    }
    else if (platform != null) {
      patient.releaseFromMount();
      ((Suspensor) platform).exitWorld();
      this.platform = null;
    }
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







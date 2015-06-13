/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.civic.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Conditions.*;
import static stratos.game.economic.Economy.*;



public class FirstAid extends Treatment {
  
  
  private static boolean
    evalVerbose  = true ,
    stepsVerbose = false;
  
  
  public FirstAid(Actor actor, Actor patient) {
    this(actor, patient, null);
  }
  
  
  public FirstAid(Actor actor, Actor patient, Boarding refuge) {
    super(actor, patient, INJURY, null, refuge);
  }
  
  
  public FirstAid(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  public Plan copyFor(Actor other) {
    if (sickbay == null) sickbay = findRefuge(actor);
    return new FirstAid(other, patient, sickbay);
  }
  
  
  
  /**  Targeting and priority evaluation.
    */
  final static Skill BASE_SKILLS[] = { ANATOMY, PHARMACY };
  final static Trait BASE_TRAITS[] = { EMPATHIC, DUTIFUL };
  
  
  protected float severity() {
    if (! patient.health.alive()) return 0.5f;
    float severity = patient.health.injuryLevel() * ActorHealth.MAX_INJURY;
    if (patient.health.bleeding()) severity += 0.5f;
    return severity;
  }
  
  
  private static Boarding findRefuge(Actor actor) {
    final Target t = Retreat.nearestHaven(actor, PhysicianStation.class, false);
    if (t instanceof Venue) return (Venue) t;
    return null;
  }
  
  
  protected float getPriority() {
    final boolean report = I.talkAbout == actor && evalVerbose && hasBegun();
    if (report) {
      I.say("\nGetting first aid priority for: "+patient);
      I.say("  Conscious? "+patient.health.conscious());
      I.say("  Organic?   "+patient.health.organic  ());
      I.say("  Bleeding?  "+patient.health.bleeding ());
      I.say("  Severity?  "+severity());
    }
    //
    //  First of all, we screen out things you can't do medicine to-
    setCompetence(0);
    if (! patient.health.organic()) return 0;
    //
    //  Then, we ensure the patient is physically accessible/won't wander off-
    final Actor carries = Suspensor.carrying(patient);
    if (
      (carries != null && carries != actor) ||
      (patient.health.conscious() && patient.aboard() != actor.aboard())
    ) {
      return -1;
    }
    //
    //  Then we determine if this is an actual emergency, and how severe the
    //  overall injury is.  (This is also used to limit the overall degree of
    //  team attention required.)
    final boolean urgent = patient.health.bleeding() || ! patient.indoors();
    if (urgent && patient.health.alive()) addMotives(MOTIVE_EMERGENCY);
    float urgency = severity();
    if (urgency <= 0  ) return 0;
    if (! urgent      ) urgency /= 2;
    if (urgency > 0.5f) addMotives(MOTIVE_EMERGENCY);
    if (PlanUtils.competition(this, patient, actor) > urgency) {
      return -1;
    }
    setCompetence(successChanceFor(actor));
    //
    //  And finally, overall priority is determined and returned...
    float priority = PlanUtils.supportPriority(
      actor, patient, motiveBonus(), competence(), urgency
    );
    if (report) {
      I.say("  Emergency?      "+urgent      );
      I.say("  Urgency rated:  "+urgency     );
      I.say("  Competence:     "+competence());
      I.say("  Final priority: "+priority    );
    }
    return priority;
  }
  
  
  public float successChanceFor(Actor actor) {
    if (! patient.health.alive()) return 1;
    return tryTreatment(
      actor, patient,
      INJURY, PhysicianStation.EMERGENCY_ROOM,
      ANATOMY, PHARMACY, false
    );
  }
  
  
  public int motionType(Actor actor) {
    return isEmergency() ? MOTION_FAST : MOTION_ANY;
  }
  
  
  protected Behaviour getNextStep() {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) I.say("\nGetting next first aid step for "+actor);
    
    //
    //  You can't perform actual treatment while under fire, but you can get
    //  the patient out of harm's way (see below.)
    final boolean underFire = actor.senses.underAttack();
    
    if (patient.health.bleeding() && ! underFire) {
      final Action aids = new Action(
        actor, patient,
        this, "actionFirstAid",
        Action.BUILD, "Giving first aid to "
      );
      return aids;
    }
    
    if (sickbay == null) {
      sickbay = findRefuge(actor);
    }
    if (sickbay != null && ! patient.indoors()) {
      final BringStretcher d = new BringStretcher(
        actor, patient, sickbay
      );
      if (d.nextStepFor(actor) != null) {
        if (report) I.say("Returning new stretcher delivery...");
        return d;
      }
    }
    
    if (underFire || Treatment.hasTreatment(INJURY, patient, ! hasBegun())) {
      return null;
    }
    final Action aids = new Action(
      actor, patient,
      this, "actionFirstAid",
      Action.BUILD, "Applying bandages to "
    );
    return aids;
  }
  
  
  public boolean actionFirstAid(Actor actor, Actor patient) {
    return tryTreatment(
      actor, patient,
      INJURY, PhysicianStation.EMERGENCY_ROOM,
      ANATOMY, PHARMACY, true
    ) > 0;
  }
  
  
  public void applyPassiveItem(Actor carries, Item from) {
    if (! patient.health.alive()) {
      patient.health.setState(ActorHealth.STATE_SUSPEND);
    }
    
    float bonus = (5 + from.quality) / 10f;
    float effect = 1.0f / STANDARD_EFFECT_TIME;
    float regen = ActorHealth.INJURY_REGEN_PER_DAY;
    regen *= 3 * effect * patient.health.maxHealth() * bonus;
    patient.health.liftInjury(regen);
    
    carries.gear.removeItem(Item.withAmount(from, effect));
  }
  
  
  
  /**  Rendering and interface methods
    */
  public void describeBehaviour(Description d) {
    if (super.needsSuffix(d, "Giving First Aid to ")) {
      d.append(patient);
    }
  }
}










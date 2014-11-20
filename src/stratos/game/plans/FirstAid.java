


package stratos.game.plans;
import stratos.game.common.*;
import stratos.game.tactical.*;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.base.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Conditions.*;
import static stratos.game.building.Economy.*;



public class FirstAid extends Treatment {
  
  
  
  private static boolean
    verbose     = false,
    evalVerbose = false;
  
  
  public FirstAid(Actor actor, Actor patient) {
    this(actor, patient, null);
  }
  
  
  public FirstAid(Actor actor, Actor patient, Boarding refuge) {
    super(actor, patient, INJURY, refuge);
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
    if (! patient.health.alive()) return 0.25f;
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
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (patient.health.conscious() || ! patient.health.organic()) return 0;
    
    final float severity = severity();
    if (severity <= 0) return 0;
    
    //  Try to avoid giving first aid in the middle of a firefight...
    float modifier = actor.senses.isEmergency() ? -1 : 0;
    final boolean ally = CombatUtils.isAllyOf(actor, patient);
    if (ally) modifier += severity;
    
    if (! CombatUtils.isArmed(patient)) {
      modifier += severity * (1f + actor.traits.relativeLevel(ETHICAL)) / 2;
    }
    
    final float priority = priorityForActorWith(
      actor, patient,
      severity * ROUTINE, modifier * ROUTINE,
      REAL_HELP, FULL_COMPETITION, NO_FAIL_RISK,
      BASE_SKILLS, BASE_TRAITS, NORMAL_DISTANCE_CHECK,
      report
    );
    if (report) {
      I.say("Considering first aid of "+patient);
      I.say("  Is ally?:           "+ally);
      I.say("  Severity of injury: "+severity());
      I.say("  Priority is: "+priority);
    }
    return priority;
  }
  

  protected float successChance() { return 0.5f; }
  
  
  protected Behaviour getNextStep() {
    final boolean report = verbose && I.talkAbout == actor;
    
    if (patient.health.bleeding()) {
      final Action aids = new Action(
        actor, patient,
        this, "actionFirstAid",
        Action.BUILD, "Giving first aid to "
      );
      return aids;
    }
    
    if (sickbay == null) sickbay = findRefuge(actor);
    if (sickbay != null && ! patient.indoors()) {
      final StretcherDelivery d = new StretcherDelivery(
        actor, patient, sickbay
      );
      if (d.nextStepFor(actor) != null) {
        if (report) I.say("Returning new stretcher delivery...");
        return d;
      }
    }
    
    if (Treatment.hasTreatment(INJURY, patient, hasBegun())) return null;
    final Action aids = new Action(
      actor, patient,
      this, "actionFirstAid",
      Action.BUILD, "Applying bandages to "
    );
    return aids;
  }
  
  
  public int motionType(Actor actor) {
    return patient.health.alive() ? MOTION_FAST : MOTION_ANY;
  }
  
  
  public boolean actionFirstAid(Actor actor, Actor patient) {
    
    Item current = existingTreatment(INJURY, patient);
    if (current == null) current = Item.with(TREATMENT, this, 0, 0);
    
    final float
      inc   = 1f / STANDARD_TREAT_TIME,
      DC    = severity() * 5,
      bonus = getVenueBonus(true, PhysicianStation.INTENSIVE_CARE);
    
    float check = Rand.yes() ? -1 : 1;
    if (actor.skills.test(ANATOMY , DC - bonus, 10)) check++;
    if (actor.skills.test(PHARMACY, 5  - bonus, 10)) check++;
    
    if (check > 0) {
      final float quality = current.amount == 0 ? 1 :
        (Item.MAX_QUALITY * (check - 1) / 2);
      current = Item.with(current.type, current.refers, inc, quality);
      patient.gear.addItem(current);
      
      if (check > 1) patient.health.setBleeding(false);
      else patient.health.liftInjury(0);
    }
    return true;
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
  
  
  
  public void describeBehaviour(Description d) {
    if (super.needsSuffix(d, "Giving First Aid to ")) {
      d.append(patient);
    }
  }
}










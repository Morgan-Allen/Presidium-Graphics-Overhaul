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
    evalVerbose  = false,
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
    final boolean report = evalVerbose && I.talkAbout == actor;
    
    setCompetence(0);  //  Will trump below...
    if (patient.health.conscious() || ! patient.health.organic()) return 0;
    
    if (report) {
      I.say("\nGetting first aid priority for: "+patient);
      I.reportStackTrace();
    }
    
    final Actor carries = Suspensor.carrying(patient);
    if (carries != null && carries != actor) return -1;
    if (
      PlanUtils.competition(this, patient, actor) > 0 &&
      ! patient.health.bleeding()
    ) {
      return -1;
    }
    
    final float severity = severity();
    if (severity <= 0) return 0;
    if (severity > 0.5f || ! patient.indoors()) addMotives(MOTIVE_EMERGENCY);
    
    setCompetence(successChanceFor(actor));
    
    float priority = PlanUtils.supportPriority(
      actor, patient, motiveBonus(), competence(), severity
    );
    if (report) I.say("  Final priority: "+priority);
    return priority;
  }
  
  
  public float successChanceFor(Actor actor) {
    if (! patient.health.alive()) return 1;
    return PlanUtils.successForActorWith(actor, BASE_SKILLS, ROUTINE_DC, false);
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
    
    if (underFire || Treatment.hasTreatment(INJURY, patient, hasBegun())) {
      return null;
    }
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
      bonus = getVenueBonus(true, PhysicianStation.EMERGENCY_ROOM);
    
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













package stratos.game.civilian;
import stratos.game.common.*;
import stratos.game.tactical.*;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.base.*;
import stratos.util.*;



public class FirstAid extends Plan implements Qualities, Economy {
  
  
  private static boolean verbose = false, evalVerbose = false;
  
  final Actor patient;
  final Boardable refuge;
  private Item result = null;
  
  
  public FirstAid(Actor actor, Actor patient) {
    this(actor, patient, findRefuge(actor));
  }
  
  
  public FirstAid(Actor actor, Actor patient, Boardable refuge) {
    super(actor, patient);
    this.patient = patient;
    this.refuge = refuge;
  }
  
  
  public FirstAid(Session s) throws Exception {
    super(s);
    patient = (Actor) s.loadObject();
    refuge = (Boardable) s.loadTarget();
    result = Item.loadFrom(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(patient);
    s.saveTarget(refuge);
    Item.saveTo(s, result);
  }
  
  
  
  /**  Targeting and priority evaluation.
    */
  final static Skill BASE_SKILLS[] = { ANATOMY, PHARMACY };
  final static Trait BASE_TRAITS[] = { EMPATHIC, DUTIFUL };
  
  
  private float severity() {
    if (! patient.health.alive()) return 0.25f;
    float severity = patient.health.injuryLevel();
    if (patient.health.bleeding()) severity += 0.5f;
    return severity;
  }
  
  
  private Item treatmentFor(Actor patient) {
    for (Item match : patient.gear.matches(SERVICE_TREAT)) {
      final Action action = (Action) match.refers;
      if (action.basis instanceof FirstAid) return match;
    }
    final Action asEffect = new Action(
      patient, patient,
      this, "actionAsItem",
      Action.STAND, "Bandaging"
    );
    return Item.withReference(SERVICE_TREAT, asEffect);
  }
  
  
  private static Boardable findRefuge(Actor actor) {
    final Target t = Retreat.nearestHaven(actor, Sickbay.class);
    if (t instanceof Venue) return (Venue) t;
    return null;
  }
  
  
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (patient.health.conscious()) return 0;
    float modifier = 0;
    
    if (patient.base() != actor.base()) {
      modifier -= (1 - actor.memories.relationValue(patient.base())) * ROUTINE;
    }
    if (patient.species() != actor.species()) {
      modifier -= ROUTINE;
    }
    modifier /= 2;
    
    final float priority = priorityForActorWith(
      actor, patient,
      severity() * PARAMOUNT,
      REAL_HELP,
      FULL_COMPETITION,
      BASE_SKILLS,
      BASE_TRAITS,
      modifier,
      NORMAL_DISTANCE_CHECK,
      MILD_FAIL_RISK,
      report
    );
    
    if (report) {
      I.say("Considering first aid of "+patient);
      I.say("  Severity of injury: "+severity());
      I.say("  Priority is: "+priority);
    }
    return priority;
  }
  
  
  protected Behaviour getNextStep() {
    final boolean report = verbose && I.talkAbout == actor && hasBegun();
    
    if (patient.health.bleeding()) {
      final Action aids = new Action(
        actor, patient,
        this, "actionFirstAid",
        Action.BUILD, "Giving first aid"
      );
      return aids;
    }
    
    if (refuge != null && ! patient.indoors()) {
      final StretcherDelivery d = new StretcherDelivery(actor, patient, refuge);
      if (d.nextStepFor(actor) != null) {
        if (report) I.say("Returning new stretcher delivery...");
        return d;
      }
    }
    
    result = treatmentFor(patient);
    final float AR = patient.gear.amountOf(result);
    if (report) I.say("Amount of treatment is: "+AR);
    
    if (AR < (hasBegun() ? 1 : 0.5f)) {
      final Action aids = new Action(
        actor, patient,
        this, "actionFirstAid",
        Action.BUILD, "Applying bandages"
      );
      return aids;
    }
    
    return null;
  }
  
  
  public int motionType(Actor actor) {
    return patient.health.alive() ? MOTION_FAST : MOTION_ANY;
  }
  
  /*
  protected float successChance() {
    final float DC = severity() * 5;
    float chance = actor.traits.chance(ANATOMY, DC);
    return chance;
  }
  //*/
  
  
  public boolean actionFirstAid(Actor actor, Actor patient) {
    float DC = severity() * 5;
    boolean success = true;
    success &= actor.traits.test(ANATOMY, DC, 10);
    
    if (success) {
      patient.health.liftInjury(0);
      result = treatmentFor(patient);
      patient.gear.addItem(Item.withAmount(result, 0.1f));
    }
    return true;
  }
  
  
  public boolean actionAsItem(Actor patient, Actor same) {
    if (! patient.health.alive()) {
      patient.health.setState(ActorHealth.STATE_SUSPEND) ;
    }
    
    float effect = 1.0f / World.STANDARD_DAY_LENGTH;
    float regen = ActorHealth.INJURY_REGEN_PER_DAY ;
    regen *= 3 * effect * patient.health.maxHealth() ;
    patient.health.liftInjury(regen) ;
    patient.gear.removeItem(Item.withAmount(result, effect));
    return true;
  }
  
  
  
  public void describeBehaviour(Description d) {
    if (lastStep instanceof Plan) {
      super.describedByStep(d);
      return;
    }
    if (super.describedByStep(d)) d.append(" to ");
    else d.append("Treating ");
    d.append(patient) ;
  }
}













package stratos.game.civilian;
import stratos.game.common.*;
import stratos.game.tactical.*;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.base.*;
import stratos.util.*;



public class FirstAid extends Plan implements Abilities, Economy {
  
  
  private static boolean verbose = true;
  
  final Actor patient;
  final Boardable refuge;
  final Item result;
  
  
  public FirstAid(Actor actor, Actor patient) {
    this(actor, patient, findRefuge(actor));
  }
  
  
  public FirstAid(Actor actor, Actor patient, Boardable refuge) {
    super(actor, patient);
    this.patient = patient;
    this.refuge = refuge;
    
    final Action asEffect = new Action(
      patient, patient,
      this, "actionAsItem",
      Action.STAND, "Bandaging"
    ) ;
    result = Item.asMatch(SERVICE_TREAT, asEffect) ;
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
  final static Skill BASE_SKILLS[] = { ANATOMY };
  final static Trait BASE_TRAITS[] = { EMPATHIC };
  
  
  private float severity() {
    float severity = actor.health.injuryLevel();
    if (actor.health.bleeding()) severity += 0.5f;
    return severity;
  }
  
  
  private static Boardable findRefuge(Actor actor) {
    final Target t = Retreat.nearestHaven(actor, Sickbay.class);
    if (t instanceof Inventory.Owner) return (Boardable) t;
    return null;
  }
  
  
  public float priorityFor(Actor actor) {
    if (patient.health.conscious()) return 0;
    if (patient.indoors() && ! patient.health.bleeding()) return 0;
    return priorityForActorWith(
      actor, patient,
      severity() * CRITICAL,
      REAL_HELP,
      FULL_COMPETITION,
      BASE_SKILLS,
      BASE_TRAITS,
      NO_MODIFIER,
      NORMAL_DISTANCE_CHECK,
      MILD_DANGER
    );
  }
  
  
  protected Behaviour getNextStep() {
    
    if (patient.health.bleeding()) {
      final Action aids = new Action(
        actor, patient,
        this, "actionFirstAid",
        Action.BUILD, "Giving first aid"
      );
      return aids;
    }
    
    if (refuge != null && ! patient.indoors()) {
      return new Delivery(patient, (Inventory.Owner) refuge);
    }
    
    if (patient.gear.amountOf(result) < 1) {
      final Action aids = new Action(
        actor, patient,
        this, "actionFirstAid",
        Action.BUILD, "Applying bandages"
      );
      return aids;
    }
    
    return null;
  }
  
  
  public boolean actionFirstAid(Actor actor, Actor patient) {
    float DC = severity() * 5;
    boolean success = true;
    success &= actor.traits.test(ANATOMY, DC, 10);
    
    if (success) {
      if (patient.health.bleeding()) patient.health.liftInjury(1);
      else patient.gear.addItem(Item.withAmount(result, 0.1f));
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
    patient.gear.removeItem(Item.withAmount(result, 0 - effect));
    return true;
  }
  
  
  
  public void describeBehaviour(Description d) {
    if (super.describedByStep(d)) d.append(" to ");
    else d.append("Treating ");
    d.append(patient) ;
  }
}










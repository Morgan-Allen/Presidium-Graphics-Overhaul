



package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.game.building.Inventory.Owner;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.building.Economy.*;



public class Resting extends Plan {
  
  
  
  /**  Static constants, field definitions, constructors and save/load methods-
    */
  private static boolean
    verbose = false;
  
  final static int
    MODE_NONE     = -1,
    MODE_DINE     =  0,
    MODE_LODGE    =  1,
    MODE_SLEEP    =  2,
    RELAX_TIME = Stage.STANDARD_HOUR_LENGTH / 2;
  
  final Owner restPoint;
  public int cost;
  
  private int currentMode = MODE_NONE;
  private float relaxTime = 0;
  
  
  public Resting(Actor actor, Target point) {
    super(actor, point, false);
    this.restPoint = (point instanceof Owner) ? (Owner) point : actor;
  }
  
  
  public Resting(Session s) throws Exception {
    super(s);
    this.restPoint = (Owner) s.loadTarget();
    this.cost = s.loadInt();
    this.currentMode = s.loadInt();
    this.relaxTime = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveTarget(restPoint);
    s.saveInt(cost);
    s.saveInt(currentMode);
    s.saveFloat(relaxTime);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Resting(other, restPoint);
  }
  
  
  
  /**  Behaviour implementation-
    */
  final static Trait BASE_TRAITS[] = { RELAXED, INDULGENT };
  
  
  protected float getPriority() {
    final boolean report = verbose && I.talkAbout == actor;
    float modifier = NO_MODIFIER, urgency = CASUAL;
    
    //  Include location effects-
    final Ambience ambience = actor.world().ecology().ambience;
    modifier += ambience.valueAt(restPoint) * ROUTINE;
    
    if (restPoint instanceof Venue) {
      final Venue venue = (Venue) restPoint;
      if (! venue.structure.intact()) return 0;
    }
    if (restPoint != actor) {
      modifier += actor.relations.valueFor(restPoint) * CASUAL;
    }
    
    //  Include effects of fatigue-
    final float stress = Visit.clamp(
      actor.health.fatigueLevel () +
      actor.health.stressPenalty() +
      actor.health.injuryLevel  (),
      0, 2
    ) / 2f;
    if (stress < 0.5f) {
      urgency *= stress * 2;
    }
    else {
      final float f = (stress - 0.5f) * 2;
      urgency = (urgency * f) + (PARAMOUNT * (1 - f));
    }
    
    //  Include effects of hunger-
    float sumFood = 0, hunger = Visit.clamp(actor.health.hungerLevel(), 0, 1);
    for (Traded s : menuFor(restPoint)) {
      sumFood += restPoint.inventory().amountOf(s);
    }
    if (sumFood > 1) sumFood = 1;
    urgency += hunger * sumFood * PARAMOUNT;
    
    //  Include pricing effects-
    if (cost > 0) {
      if (cost > actor.gear.credits() / 2) urgency -= ROUTINE;
      urgency -= Pledge.greedLevel(actor, cost) * ROUTINE;
    }
    
    //  Include day/night effects-
    urgency += (IDLE + 1 - Planet.dayValue(actor.world())) / 2f;
    
    final float priority = priorityForActorWith(
      actor, restPoint, Visit.clamp(urgency, 0, URGENT),
      modifier, NO_HARM,
      NO_COMPETITION, NO_SKILLS,
      BASE_TRAITS, NORMAL_DISTANCE_CHECK, NO_FAIL_RISK,
      report
    );
    return priority;
  }
  
  
  private static Batch <Traded> menuFor(Owner place) {
    Batch <Traded> menu = new Batch <Traded> ();
    for (Traded type : ALL_FOOD_TYPES) {
      if (place.inventory().amountOf(type) >= 0.1f) menu.add(type);
    }
    return menu;
  }
  
  
  protected Behaviour getNextStep() {
    if (restPoint == null) return null;
    /*
    if (restPoint instanceof Tile) {
      if (((Tile) restPoint).blocked()) return null;
    }
    //*/
    
    //  TODO:  Split dining off into a separate behaviour.
    if (menuFor(restPoint).size() > 0) {
      if (actor.health.hungerLevel() > 0.1f) {
        final Action eats = new Action(
          actor, restPoint,
          this, "actionEats",
          Action.BUILD, "Eating at "+restPoint
        );
        currentMode = MODE_DINE;
        return eats;
      }
    }
    //
    //  If you're tired, put your feet up.
    if (actor.health.fatigueLevel() > 0.1f) currentMode = MODE_SLEEP;
    else if (relaxTime > (Rand.num() + 1) * RELAX_TIME) return null;
    else currentMode = MODE_LODGE;
    final Action relax = new Action(
      actor, restPoint,
      this, "actionRest",
      Action.FALL, "Resting at "+restPoint
    );
    relax.setProperties(Action.NO_LOOP);
    return relax;
  }
  
  
  public boolean actionRest(Actor actor, Owner place) {
    
    //    TODO:  Reconsider these.
    //
    //  Transfer any incidental groceries-
    if (place == actor.mind.home()) for (Traded food : ALL_FOOD_TYPES) {
      actor.gear.transfer(food, (Employer) place);
    }
    //
    //  If you're resting at home, deposit any taxes due-
    if (place == actor.mind.home() && place instanceof Venue) {
      final float taxes = Audit.taxesDue(actor);
      final Venue v = (Venue) place;
      v.stocks.incCredits(taxes);
      actor.gear.incCredits(0 - taxes);
      actor.gear.taxDone();
    }
    //
    //  Otherwise, pay any initial fees required-
    else if (cost > 0) {
      ((Venue) place).stocks.incCredits(cost);
      actor.gear.incCredits(0 - cost);
      cost = 0;
    }
    if (currentMode == MODE_SLEEP) {
      actor.health.setState(ActorHealth.STATE_RESTING);
    }
    else {
      //  TODO:  Improve morale?
      relaxTime += 1.0f;
    }
    return true;
  }

  
  public boolean actionEats(Actor actor, Owner place) {
    return dineFrom(actor, place);
  }
  
  
  public static boolean dineFrom(Actor actor, Owner stores) {
    final Batch <Traded> menu = menuFor(stores);
    final int numFoods = menu.size();
    
    if (numFoods > 0 && actor.health.hungerLevel() > 0.1f) {
      final int FTC = ActorHealth.FOOD_TO_CALORIES;
      float sumFood = 0;
      
      for (Traded type : menu) {
        final Item portion = Item.withAmount(type, 0.2f / (numFoods * FTC));
        stores.inventory().removeItem(portion);
        sumFood += portion.amount;
      }
      
      actor.health.takeCalories(1, sumFood * FTC);
      return true;
    }
    return false;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    if (restPoint == actor || restPoint == null) {
      if (currentMode == MODE_DINE) d.append("Dining");
      else d.append("Resting");
      return;
    }
    if (currentMode == MODE_DINE) {
      d.append("Eating at ");
      d.append(restPoint);
    }
    else {
      d.append("Resting at ");
      d.append(restPoint);
    }
  }
}







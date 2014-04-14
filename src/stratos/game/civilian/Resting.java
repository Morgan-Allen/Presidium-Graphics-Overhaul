



package stratos.game.civilian ;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.planet.*;
import stratos.game.tactical.*;
import stratos.user.*;
import stratos.util.*;



public class Resting extends Plan implements Economy {
  
  
  
  /**  Static constants, field definitions, constructors and save/load methods-
    */
  private static boolean verbose = false ;
  
  final static int
    MODE_NONE     = -1,
    MODE_DINE     =  0,
    MODE_LODGE    =  1,
    MODE_SLEEP    =  2,
    RELAX_TIME = World.STANDARD_HOUR_LENGTH / 2;
  
  final Boardable restPoint ;
  public int cost ;
  
  private int currentMode = MODE_NONE ;
  //private float minPriority = -1 ;
  private float relaxTime = 0;
  
  
  public Resting(Actor actor, Target relaxesAt) {
    super(actor, relaxesAt) ;
    this.restPoint = (Boardable) relaxesAt ;
  }
  
  
  public Resting(Session s) throws Exception {
    super(s) ;
    this.restPoint = (Boardable) s.loadTarget() ;
    this.cost = s.loadInt() ;
    this.currentMode = s.loadInt() ;
    this.relaxTime = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveTarget(restPoint) ;
    s.saveInt(cost) ;
    s.saveInt(currentMode) ;
    s.saveFloat(relaxTime) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  final static Trait BASE_TRAITS[] = { RELAXED, INDULGENT };
  
  
  protected float getPriority() {
    final boolean report = verbose && I.talkAbout == actor;
    float modifier = NO_MODIFIER, urgency = CASUAL;
    
    modifier += actor.world().ecology().ambience.valueAt(restPoint) * ROUTINE;
    
    if (restPoint instanceof Tile) modifier -= 2 ;
    if (restPoint == actor.mind.home()) modifier += 2 ;
    if (restPoint instanceof Venue) {
      final Venue venue = (Venue) restPoint ;
      final float relation = actor.memories.relationValue(venue) ;
      if (relation > 0) modifier *= relation ;
      else modifier -= relation * 5 ;
    }

    final float fatigue = actor.health.fatigueLevel() ;
    if (fatigue < 0.5f) {
      urgency *= fatigue * 2 ;
    }
    else {
      final float f = (fatigue - 0.5f) * 2 ;
      urgency = (urgency * f) + (PARAMOUNT * (1 - f)) ;
    }
    
    if (restPoint instanceof Inventory.Owner) {
      final Inventory.Owner owner = (Inventory.Owner) restPoint ;
      float sumFood = 0 ;
      for (Service s : menuFor(owner)) {
        sumFood += owner.inventory().amountOf(s) ;
      }
      if (sumFood > 1) sumFood = 1 ;
      urgency += actor.health.hungerLevel() * sumFood * PARAMOUNT ;
    }
    if (cost > 0) {
      if (cost > actor.gear.credits() / 2) urgency -= ROUTINE ;
      urgency -= actor.mind.greedFor(cost) * ROUTINE ;
    }
    
    final float priority = priorityForActorWith(
      actor, restPoint, Visit.clamp(urgency, IDLE, URGENT),
      NO_HARM, NO_COMPETITION,
      NO_SKILLS, BASE_TRAITS,
      modifier, NORMAL_DISTANCE_CHECK, NO_DANGER,
      report
    );
    return priority;
  }
  
  
  private static Batch <Service> menuFor(Inventory.Owner place) {
    Batch <Service> menu = new Batch <Service> () ;
    for (Service type : ALL_FOOD_TYPES) {
      if (place.inventory().amountOf(type) >= 0.1f) menu.add(type) ;
    }
    return menu ;
  }
  
  
  protected Behaviour getNextStep() {
    if (restPoint == null) return null ;
    if (restPoint instanceof Tile) {
      if (((Tile) restPoint).blocked()) return null ;
    }
    
    //  TODO:  Split dining off into a separate behaviour.
    if (restPoint instanceof Venue && menuFor((Venue) restPoint).size() > 0) {
      if (actor.health.hungerLevel() > 0.1f) {
        final Action eats = new Action(
          actor, restPoint,
          this, "actionEats",
          Action.BUILD, "Eating at "+restPoint
        ) ;
        currentMode = MODE_DINE ;
        if (verbose) I.sayAbout(actor, "Returning eat action...") ;
        return eats ;
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
  
  
  public boolean actionRest(Actor actor, Boardable place) {
    //
    //  Transfer any incidental groceries-
    if (place == actor.mind.home()) for (Service food : ALL_FOOD_TYPES) {
      actor.gear.transfer(food, (Employer) place) ;
    }
    //
    //  If you're resting at home, deposit any taxes due-
    if (place == actor.mind.home() && place instanceof Venue) {
      final float taxes = Audit.taxesDue(actor) ;
      final Venue v = (Venue) place ;
      v.stocks.incCredits(taxes) ;
      actor.gear.incCredits(0 - taxes) ;
      actor.gear.taxDone() ;
    }
    //
    //  Otherwise, pay any initial fees required-
    else if (cost > 0) {
      ((Venue) place).stocks.incCredits(cost) ;
      actor.gear.incCredits(0 - cost) ;
      cost = 0 ;
    }
    if (currentMode == MODE_SLEEP) {
      actor.health.setState(ActorHealth.STATE_RESTING) ;
    }
    else {
      //  TODO:  Improve morale?
      relaxTime += 1.0f;
    }
    return true ;
  }

  
  public boolean actionEats(Actor actor, Venue place) {
    return dineFrom(actor, place) ;
  }
  
  
  public static boolean dineFrom(Actor actor, Inventory.Owner stores) {
    final Batch <Service> menu = menuFor(stores);
    final int numFoods = menu.size();
    
    if (numFoods > 0 && actor.health.hungerLevel() > 0.1f) {
      final int FTC = ActorHealth.FOOD_TO_CALORIES;
      float sumFood = 0;
      
      for (Service type : menu) {
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
    if (currentMode == MODE_DINE) {
      d.append("Eating at ") ;
      d.append(restPoint) ;
    }
    else {
      d.append("Resting at ") ;
      d.append(restPoint) ;
    }
  }
}











/*
public static float ratePoint(Actor actor, Boardable restPoint, int cost) {
  if (restPoint == null) return -1 ;
  //
  //  Basic attraction is founded on the ambience of the area, and relations
  //  with the venue in question (if applicable.)
  float priority = actor.world().ecology().ambience.valueAt(restPoint) ;
  priority *= 5 ;
  if (restPoint instanceof Tile) priority -= 2 ;
  if (restPoint == actor.mind.home()) priority += 2 ;
  if (restPoint instanceof Venue) {
    final Venue venue = (Venue) restPoint ;
    final float relation = actor.memories.relationValue(venue) ;
    if (relation > 0) priority *= relation ;
    else priority -= relation * 5 ;
  }
  //
  //  Also incorporate the effects of fatigue-urgency:
  final float fatigue = actor.health.fatigueLevel() ;
  if (fatigue < 0.5f) {
    priority *= fatigue * 2 ;
  }
  else {
    final float f = (fatigue - 0.5f) * 2 ;
    priority = (priority * f) + (PARAMOUNT * (1 - f)) ;
  }
  //
  //  Finally, include the effects of hunger, cost & distance-
  if (restPoint instanceof Inventory.Owner) {
    final Inventory.Owner owner = (Inventory.Owner) restPoint ;
    float sumFood = 0 ;
    for (Service s : menuFor(owner)) {
      sumFood += owner.inventory().amountOf(s) ;
    }
    if (sumFood > 1) sumFood = 1 ;
    priority += actor.health.hungerLevel() * sumFood * PARAMOUNT ;
  }
  if (cost > 0) {
    if (cost > actor.gear.credits() / 2) priority -= 5 ;
    priority -= actor.mind.greedFor(cost) / ROUTINE ;
  }
  priority -= Plan.rangePenalty(actor, restPoint) ;
  priority -= Plan.dangerPenalty(restPoint, actor) ;
  //
  //  And return the bounded result-
  return Visit.clamp(priority, 0, PARAMOUNT) ;
}
//*/


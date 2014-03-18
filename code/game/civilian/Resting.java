



package code.game.civilian ;
import code.game.actors.*;
import code.game.building.*;
import code.game.common.*;
import code.game.planet.*;
import code.game.tactical.*;
import code.user.*;
import code.util.*;



public class Resting extends Plan implements Economy {
  
  
  
  /**  Static constants, field definitions, constructors and save/load methods-
    */
  private static boolean verbose = false ;
  
  final static int
    MODE_NONE     = -1,
    MODE_DINE     =  0,
    MODE_LODGE    =  1,
    MODE_SLEEP    =  2 ;
  
  final Boardable restPoint ;
  public int cost ;
  
  private int currentMode = MODE_NONE ;
  private float minPriority = -1 ;
  
  
  public static Resting nextRestingFor(Actor actor) {
    final Boardable home = actor.mind.home(), work = actor.mind.work() ;
    if (home != null && ratePoint(actor, home, 0) > 0) {
      return new Resting(actor, home) ;
    }
    if (work != null && ratePoint(actor, work, 0) > 0) {
      return new Resting(actor, work) ;
    }
    Target free = Retreat.pickWithdrawPoint(actor, 16, actor, 0.1f) ;
    if (free == null) free = actor.aboard() ;
    return new Resting(actor, free) ;
  }
  
  
  public Resting(Actor actor, Target relaxesAt) {
    super(actor) ;
    this.restPoint = (Boardable) relaxesAt ;
  }
  
  
  public Resting(Session s) throws Exception {
    super(s) ;
    this.restPoint = (Boardable) s.loadTarget() ;
    this.cost = s.loadInt() ;
    this.currentMode = s.loadInt() ;
    this.minPriority = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveTarget(restPoint) ;
    s.saveInt(cost) ;
    s.saveInt(currentMode) ;
    s.saveFloat(minPriority) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
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
      final float relation = actor.mind.relationValue(venue) ;
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
      priority -= actor.mind.greedFor(cost) ;
    }
    priority -= Plan.rangePenalty(actor, restPoint) ;
    priority -= Plan.dangerPenalty(restPoint, actor) ;
    //
    //  And return the bounded result-
    return Visit.clamp(priority, 0, PARAMOUNT) ;
  }
  
  
  public float priorityFor(Actor actor) {
    return ratePoint(actor, restPoint, cost) ;
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
    if (actor.health.fatigueLevel() > 0.1f) {
      final Action relax = new Action(
        actor, restPoint,
        this, "actionRest",
        Action.FALL, "Resting at "+restPoint
      ) ;
      currentMode = MODE_SLEEP ;
      return relax ;
    }
    return null ;
  }
  
  
  public boolean actionRest(Actor actor, Boardable place) {
    //
    //  Transfer any incidental groceries-
    if (place == actor.mind.home()) for (Service food : ALL_FOOD_TYPES) {
      actor.gear.transfer(food, (Employment) place) ;
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
    actor.health.setState(ActorHealth.STATE_RESTING) ;
    return true ;
  }

  
  public boolean actionEats(Actor actor, Venue place) {
    return dineFrom(actor, place) ;
  }
  
  
  public static boolean dineFrom(Actor actor, Inventory.Owner stores) {
    final Batch <Service> menu = menuFor(stores) ;
    final int numFoods = menu.size() ;
    if (numFoods > 0 && actor.health.hungerLevel() > 0.1f) {
      //
      //  FOOD TO BODY-MASS RATIO IS 1 TO 10.  So, 1 unit of food will last a
      //  typical person 5 days.
      for (Service type : menu) {
        final Item portion = Item.withAmount(type, 0.1f * 1f / numFoods) ;
        stores.inventory().removeItem(portion) ;
      }
      actor.health.takeSustenance(1, numFoods / 2) ;
      return true ;
    }
    return false ;
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











/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.building ;
import stratos.game.actors.*;
import stratos.game.building.Inventory.Owner;
import stratos.game.civilian.Audit;
import stratos.game.common.*;
import stratos.user.*;
import stratos.util.*;


//
//  TODO:  Barges need to be made more persistent.


public class Delivery extends Plan implements Economy {
  
  
  final public static int
    TYPE_SHOPS  = 0,
    TYPE_TRADE  = 1,
    TYPE_UNPAID = 2 ;
  
  final static int
    STAGE_INIT    = -1,
    STAGE_PICKUP  =  0,
    STAGE_DROPOFF =  1,
    STAGE_RETURN  =  2,
    STAGE_DONE    =  3 ;
  final static int
    MIN_BULK = 5 ;
  
  private static boolean verbose = false ;
  
  
  final public Owner origin, destination ;
  final public Item items[] ;
  public Owner shouldPay ;
  
  private byte stage = STAGE_INIT ;
  private float pricePaid = 0 ;
  private Suspensor suspensor ;
  public Vehicle driven ;
  
  
  public Delivery(Service s, Owner origin, Owner destination) {
    this(origin.inventory().matches(s), origin, destination) ;
  }
  
  
  public Delivery(Item item, Owner origin, Owner destination) {
    this(new Item[] { item }, origin, destination) ;
  }
  
  
  public Delivery(Batch <Item> items, Owner orig, Owner dest) {
    this(items.toArray(Item.class), orig, dest) ;
  }
  
  
  public Delivery(Item items[], Owner origin, Owner destination) {
    super(null, origin) ;
    this.origin = origin ;
    this.destination = destination ;
    this.items = items ;
    for (Item n : items) {
      if (n.quality == -1) I.complain(n+" HAD NO QUALITY") ;
    }
  }
  
  
  public Delivery(Session s) throws Exception {
    super(s) ;
    
    items = new Item[s.loadInt()] ;
    for (int n = 0 ; n < items.length ; n++) {
      items[n] = Item.loadFrom(s) ;
      if (items[n].quality == -1) items[n] = Item.withQuality(items[n], 0) ;
    }
    
    origin = (Owner) s.loadObject() ;
    destination = (Owner) s.loadObject() ;
    shouldPay = (Owner) s.loadObject() ;
    
    stage = (byte) s.loadInt() ;
    pricePaid = s.loadFloat() ;
    suspensor = (Suspensor) s.loadObject() ;
    driven = (Vehicle) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    
    s.saveInt(items.length) ;
    for (Item i : items) Item.saveTo(s, i) ;
    
    s.saveObject((Session.Saveable) origin) ;
    s.saveObject((Session.Saveable) destination) ;
    s.saveObject(shouldPay) ;
    
    s.saveInt(stage) ;
    s.saveFloat(pricePaid) ;
    s.saveObject(suspensor) ;
    s.saveObject(driven) ;
  }
  
  
  int stage() { return stage ; }
  
  
  public boolean matchesPlan(Plan plan) {
    if (! super.matchesPlan(plan)) return false ;
    final Delivery d = (Delivery) plan ;
    if (d.origin != origin || d.destination != destination) return false ;
    boolean overlap = false;
    for (Item i : items) {
      for (Item dI : d.items) if (i.type == dI.type) overlap = true ;
    }
    return overlap ;
  }
  
  
  
  /**  Assessing targets and priorities-
    */
  public static float purchasePrice(Item item, Actor actor, Owner origin) {
    float TP = origin.priceFor(item.type) ;
    if (actor != null && actor.vocation().guild == Background.GUILD_MILITANT) {
      TP -= Audit.MILITANT_RATION ;
      if (TP <= 0) return 0 ;
    }
    return item.amount * TP ;
  }
  
  
  private Item[] available(Actor actor) {
    ///I.sayAbout(actor, "Getting available... "+items.length+" "+stage) ;
    final Batch <Item> available = new Batch <Item> () ;
    if (actor == null) {
      return items ;
    }
    
    //final boolean shopping = isShopping(actor) ;
    final boolean shopping = shouldPay == actor ;
    final Owner carrier = driven == null ? actor : driven ;
    
    if (stage <= STAGE_PICKUP) {
      float sumPrice = 0 ;
      for (Item i : items) {
        if (i.quality == -1) I.say(i+" has no quality!") ;
        final float amount = origin.inventory().amountOf(i) ;
        ///I.sayAbout(actor, "Amount of "+i+" is "+amount) ;
        if (amount <= 0) continue ;
        if (shopping) {
          sumPrice += purchasePrice(i, actor, origin) ;
          if (sumPrice > actor.gear.credits() / 2f) break ;
        }
        available.add(i) ;
      }
    }
    else {
      for (Item i : items) {
        if (i.quality == -1) I.say(i+" has no quality!") ;
        if (! carrier.inventory().hasItem(i)) {
          final float amount = carrier.inventory().amountOf(i) ;
          ///I.sayAbout(actor, "Amount of "+i+" is "+amount) ;
          if (amount > 0) available.add(Item.withAmount(i, amount)) ;
          continue ;
        }
        else available.add(i) ;
      }
    }
    return available.toArray(Item.class) ;
  }
  

  protected float getPriority() {
    final Item[] available = available(actor) ;
    if (available.length == 0) return 0 ;
    final boolean report = verbose && I.talkAbout == actor;
    
    float modifier = NO_MODIFIER;
    if (shouldPay == actor && stage <= STAGE_PICKUP) {
      int price = 0 ;
      float foodVal = 0 ;
      for (Item i : available) {
        price += purchasePrice(i, actor, origin) ;
        if (Visit.arrayIncludes(ALL_FOOD_TYPES, i.type)) foodVal += i.amount ;
      }
      if (price > actor.gear.credits()) return 0 ;
      modifier -= Plan.greedLevel(actor, price) - ROUTINE ;
      modifier += actor.health.hungerLevel() * CASUAL * foodVal ;
    }
    else for (Item i : available) {
      modifier += i.amount / 5f ;
    }
    
    final float rangeDiv = driven == null ? 2f : 10f;
    final float extraRangePenalty = (
      Plan.rangePenalty(actor, origin) +
      Plan.rangePenalty(origin, destination)
    ) / rangeDiv;
    
    final float priority = priorityForActorWith(
      actor, destination, ROUTINE,
      NO_HARM, NO_COMPETITION,
      NO_SKILLS, NO_TRAITS,
      modifier, NORMAL_DISTANCE_CHECK / rangeDiv, NO_DANGER,
      report
    ) - extraRangePenalty;
    return priority;
  }
  
  
  public boolean valid() {
    if (! super.valid()) return false ;
    if (driven != null) {
      if (driven.destroyed()) return false ;
      if (! driven.canPilot(actor)) return false ;
    }
    
    if (stage < STAGE_RETURN && available(actor).length == 0) {
      if (driven != null) { stage = STAGE_RETURN ; return true ; }
      return false ;
    }
    return true ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public Behaviour getNextStep() {
    
    if (stage == STAGE_INIT) {
      if (driven != null) {
        final Action boarding = new Action(
          actor, driven,
          this, "actionBoardVehicle",
          Action.STAND, "Boarding vehicle"
        ) ;
        return boarding ;
      }
      else stage = STAGE_PICKUP ;
    }
    if (stage == STAGE_PICKUP) {
      final Action pickup = new Action(
        actor, origin,
        this, "actionPickup",
        Action.REACH_DOWN, "Picking up goods"
      ) ;
      if (driven != null) pickup.setMoveTarget(driven) ;
      return pickup ;
    }
    if (stage == STAGE_DROPOFF) {
      final Action dropoff = new Action(
        actor, destination,
        this, "actionDropoff",
        Action.REACH_DOWN, "Dropping off goods"
      ) ;
      if (driven != null) dropoff.setMoveTarget(driven) ;
      //else dropoff.setProperties(Action.CARRIES) ;
      return dropoff ;
    }
    if (stage == STAGE_RETURN && driven != null && driven.hangar() != null) {
      final Action returns = new Action(
        actor, driven.hangar(),
        this, "actionReturn",
        Action.REACH_DOWN, "Returning in vehicle"
      ) ;
      returns.setMoveTarget(driven) ;
      return returns ;
    }
    
    //I.sayAbout(actor, "No next step!") ;
    return null ;
  }
  
  
  public boolean actionBoardVehicle(Actor actor, Vehicle driven) {
    actor.goAboard(driven, actor.world()) ;
    if (! driven.setPilot(actor)) abortBehaviour() ;
    stage = STAGE_PICKUP ;
    return true ;
  }
  
  
  private float transferGoods(Owner a, Owner b) {
    if (a == null || b == null) return 0 ;
    float sumItems = 0 ;
    float totalPrice = 0 ;
    for (Item i : available(actor)) {
      final float TA = a.inventory().transfer(i, b) ;
      totalPrice += TA * purchasePrice(i, actor, origin) / i.amount ;
      sumItems += TA ;
    }
    
    I.sayAbout(actor, "Should pay: "+shouldPay) ;
    
    if (shouldPay != null) {
      origin.inventory().incCredits(totalPrice) ;
      if (shouldPay == actor) actor.gear.incCredits(0 - totalPrice) ;
      else pricePaid = totalPrice ;
    }
    return sumItems ;
  }
  

  public boolean actionPickup(Actor actor, Target target) {
    if (stage != STAGE_PICKUP) return false ;
    //
    //  Vehicles get special treatment-
    if (driven != null) {
      if (! driven.setPilot(actor)) abortBehaviour() ;
      driven.pathing.updateTarget(target) ;
      if (driven.aboard() == target) {
        transferGoods(origin, driven) ;
        stage = STAGE_DROPOFF ;
        return true ;
      }
      return false ;
    }
    //
    //  Perform the actual transfer of goods, make the payment required, and
    //  see if a suspensor is needed-
    final float sum = transferGoods(origin, actor) ;
    final boolean bulky = sum >= 5;// || passenger != null ;
    if (verbose) I.sayAbout(actor, "Performing pickup!") ;
    if (bulky) {
      final Suspensor suspensor = new Suspensor(actor, this) ;
      final Tile o = actor.origin() ;
      suspensor.enterWorldAt(o.x, o.y, o.world) ;
      this.suspensor = suspensor ;
    }
    stage = STAGE_DROPOFF ;
    return true ;
  }
  
  
  public boolean actionDropoff(Actor actor, Owner target) {
    if (stage != STAGE_DROPOFF) return false ;
    
    if (shouldPay != null && pricePaid > 0) {
      shouldPay.inventory().incCredits(0 - pricePaid) ;
    }
    
    if (driven != null) {
      if (! driven.setPilot(actor)) abortBehaviour() ;
      driven.pathing.updateTarget(target) ;
      if (driven.aboard() == target) {
        for (Service t : ALL_COMMODITIES) {
          driven.cargo.transfer(t, target) ;
        }
        stage = STAGE_RETURN ;
        return true ;
      }
      return false ;
    }
    
    if (suspensor != null && suspensor.inWorld()) suspensor.exitWorld() ;
    
    for (Item i : items) actor.gear.transfer(i.type, target) ;
    stage = STAGE_DONE ;
    return true ;
  }
  
  
  public boolean actionReturn(Actor actor, Venue target) {
    if (! driven.setPilot(actor)) abortBehaviour() ;
    driven.pathing.updateTarget(target) ;
    if (driven.aboard() == target) {
      driven.setPilot(null) ;
      actor.goAboard(target, actor.world()) ;
      stage = STAGE_DONE ;
      return true ;
    }
    return false ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    
    if (stage == STAGE_RETURN) {
      d.append("Returning to ") ;
      d.append(origin) ;
      return ;
    }
    
    d.append("Delivering ") ;
    final Item available[] = available(actor) ;
    final Batch <Service> types = new Batch <Service> () ;
    for (Item i : available) types.add(i.type) ;
    d.appendList("", types) ;
    d.append(" from ") ;
    d.append(origin) ;
    d.append(" to ") ;
    d.append(destination) ;
  }
}












/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.base.Suspensor;
import stratos.game.common.*;
import stratos.game.economic.Inventory;
import stratos.game.economic.Item;
import stratos.game.economic.Traded;
import stratos.game.economic.Vehicle;
import stratos.game.economic.Venue;
import stratos.game.economic.Inventory.Owner;
import stratos.game.politic.Pledge;
import stratos.util.*;
import static stratos.game.economic.Economy.*;



public class Delivery extends Plan {
  
  
  final public static int
    TYPE_SHOPS  = 0,
    TYPE_TRADE  = 1,
    TYPE_UNPAID = 2;
  
  final static int
    STAGE_INIT    = -1,
    STAGE_PICKUP  =  0,
    STAGE_DROPOFF =  1,
    STAGE_RETURN  =  2,
    STAGE_DONE    =  3;
  final static int
    MIN_BULK = 5;
  
  private static boolean
    verbose      = false,
    stepsVerbose = false;
  
  
  final public Owner origin, destination;
  
  private byte stage = STAGE_INIT;
  private Item items[];
  private Owner shouldPay;
  private float goodsPrice, goodsBulk;
  
  private Suspensor suspensor; //  TODO:  Unify with vehicle...
  public Vehicle driven;
  public boolean replace = false;  //  TODO:  Shouldn't be public!  Gah!
  
  
  public Delivery(Traded s, Owner origin, Owner destination) {
    this(origin.inventory().matches(s), origin, destination);
  }
  
  
  public Delivery(Item item, Owner origin, Owner destination) {
    this(new Item[] { item }, origin, destination);
  }
  
  
  public Delivery(Batch <Item> items, Owner orig, Owner dest) {
    this(items.toArray(Item.class), orig, dest);
  }
  
  
  public Delivery(Item items[], Owner origin, Owner destination) {
    super(null, origin, true, NO_HARM);
    this.origin = origin;
    this.destination = destination;
    this.items = items;
  }
  
  
  public Delivery(Session s) throws Exception {
    super(s);
    
    items = new Item[s.loadInt()];
    for (int n = 0; n < items.length; n++) {
      items[n] = Item.loadFrom(s);
    }
    origin      = (Owner) s.loadObject();
    destination = (Owner) s.loadObject();
    
    shouldPay  = (Owner) s.loadObject();
    stage      = (byte) s.loadInt();
    goodsPrice = s.loadFloat();
    goodsBulk  = s.loadFloat();
    
    suspensor = (Suspensor) s.loadObject();
    driven    = (Vehicle  ) s.loadObject();
    replace   = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    
    s.saveInt(items.length);
    for (Item i : items) Item.saveTo(s, i);
    s.saveObject((Session.Saveable) origin);
    s.saveObject((Session.Saveable) destination);
    
    s.saveObject(shouldPay );
    s.saveInt   (stage     );
    s.saveFloat (goodsPrice);
    s.saveFloat (goodsBulk );
    
    s.saveObject(suspensor);
    s.saveObject(driven   );
    s.saveBool  (replace  );
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  int stage() { return stage; }
  
  
  public boolean matchesPlan(Behaviour plan) {
    if (! super.matchesPlan(plan)) return false;
    final Delivery d = (Delivery) plan;
    if (d.origin != origin || d.destination != destination) return false;
    boolean overlap = false;
    for (Item i : items) {
      for (Item dI : d.items) if (i.type == dI.type) overlap = true;
    }
    return overlap;
  }
  
  
  
  /**  Initial culling methods to allow for budget and inventory limits-
    */
  //  TODO:  Consider moving this to the rateTrading method?
  public Delivery withPayment(Owner pays, boolean priceLimit) {
    
    if (priceLimit && pays != null) {
      final float maxPrice = pays.inventory().credits() / 2;
      float price = -1;
      final Batch <Item> canAfford = new Batch <Item> ();
      
      itemsLoop: for (Item i : items) {
        while (true) {
          price = i.priceAt(origin);
          if (goodsPrice + price > maxPrice) {
            if (i.amount <= 1) break itemsLoop;
            i = Item.withAmount(i, i.amount - 1);
          }
          else break;
        }
        goodsPrice += price;
        goodsBulk += i.amount;
        canAfford.add(i);
      }
      if (canAfford.size() == 0) return null;
      
      this.shouldPay = pays;
      items = canAfford.toArray(Item.class);
      return this;
    }
    
    else {
      for (Item i : items) {
        goodsPrice += i.priceAt(origin);
        goodsBulk  += i.amount;
      }
      this.shouldPay = pays;
      return this;
    }
  }
  
  
  public float pricePaid() {
    return goodsPrice;
  }
  
  
  public Item delivered(Traded type) {
    for (Item i : items) if (i.type == type) return i;
    return null;
  }
  
  
  
  
  /**  Assessing targets and priorities-
    */
  protected float getPriority() {
    final boolean report = verbose && I.talkAbout == actor;
    
    float modifier = NO_MODIFIER;
    if (shouldPay == actor && stage <= STAGE_PICKUP) {
      int price = 0;
      for (Item i : items) {
        price += i.priceAt(origin);
        modifier += ActorMotives.rateDesire(i, null, actor);
      }
      if (price > actor.gear.credits()) return 0;
      modifier -= Pledge.greedPriority(actor, price);
    }
    else for (Item i : items) {
      modifier += i.amount / 10f;
    }
    
    final float rangeDiv = driven == null ? 2f : 10f;
    final float extraRangePenalty = (
      Plan.rangePenalty(actor, origin) +
      Plan.rangePenalty(origin, destination)
    ) / rangeDiv;
    
    final float priority = priorityForActorWith(
      actor, destination,
      ROUTINE, modifier - extraRangePenalty,
      NO_HARM, NO_COMPETITION, NO_FAIL_RISK,
      NO_SKILLS, NO_TRAITS, NORMAL_DISTANCE_CHECK / rangeDiv,
      report
    );
    return priority;
  }
  
  
  public boolean valid() {
    if (! super.valid()) return false;
    if (driven != null) {
      if (driven.destroyed()) return false;
      if (! driven.canPilot(actor)) return false;
    }
    return true;
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
        );
        return boarding;
      }
      else stage = STAGE_PICKUP;
    }
    if (stage == STAGE_PICKUP) {
      final Action pickup = new Action(
        actor, origin,
        this, "actionPickup",
        Action.REACH_DOWN, "Picking up goods"
      );
      if (driven != null) pickup.setMoveTarget(driven);
      return pickup;
    }
    if (stage == STAGE_DROPOFF) {
      final Action dropoff = new Action(
        actor, destination,
        this, "actionDropoff",
        Action.REACH_DOWN, "Dropping off goods"
      );
      if (driven != null) dropoff.setMoveTarget(driven);
      //else dropoff.setProperties(Action.CARRIES);
      return dropoff;
    }
    if (stage == STAGE_RETURN && driven != null) {
      final Action returns = new Action(
        actor, driven.hangar(),
        this, "actionReturnVehicle",
        Action.REACH_DOWN, "Returning in vehicle"
      );
      returns.setMoveTarget(driven);
      return returns;
    }
    if (stage == STAGE_RETURN && driven == null) {
      final Action returns = new Action(
        actor, origin,
        this, "actionReturnProceeds",
        Action.TALK_LONG, "Returning profits"
      );
      return returns;
    }
    
    //I.sayAbout(actor, "No next step!");
    return null;
  }
  
  
  public boolean actionBoardVehicle(Actor actor, Vehicle driven) {
    actor.goAboard(driven, actor.world());
    if (! driven.setPilot(actor)) abortBehaviour();
    stage = STAGE_PICKUP;
    return true;
  }
  

  public boolean actionPickup(Actor actor, Target target) {
    if (stage != STAGE_PICKUP) return false;
    //
    //  Vehicles get special treatment-
    if (driven != null) {
      if (! driven.setPilot(actor)) abortBehaviour();
      driven.pathing.updateTarget(target);
      if (driven.aboard() == target) {
        transferGoods(origin, driven);
        stage = STAGE_DROPOFF;
        return true;
      }
      return false;
    }
    //
    //  Perform the actual transfer of goods, make the payment required, and
    //  see if a suspensor is needed-
    transferGoods(origin, actor);
    final boolean bulky = goodsBulk >= 5;
    if (bulky) {
      final Suspensor suspensor = new Suspensor(actor, this);
      final Tile o = actor.origin();
      suspensor.enterWorldAt(o.x, o.y, o.world);
      this.suspensor = suspensor;
    }
    stage = STAGE_DROPOFF;
    return true;
  }
  
  
  private void transferGoods(Owner a, Owner b) {
    if (a == null || b == null) return;
    for (Item i : items) {
      if (replace) {
        final Item match = b.inventory().matchFor(i);
        b.inventory().removeItem(match);
      }
      a.inventory().transfer(i, b);
    }
  }
  
  
  public boolean actionDropoff(Actor actor, Owner target) {
    if (stage != STAGE_DROPOFF) return false;
    if (shouldPay != null) actor.gear.incCredits(goodsPrice);
    
    if (driven != null) {
      if (! driven.setPilot(actor)) abortBehaviour();
      driven.pathing.updateTarget(target);
      if (driven.aboard() == target) {
        transferGoods(driven, target);
        stage = STAGE_RETURN;
        return true;
      }
      return false;
    }
    else {
      if (suspensor != null && suspensor.inWorld()) suspensor.exitWorld();
      
      transferGoods(actor, target);
      if (actor != origin && shouldPay != null) stage = STAGE_RETURN;
      else stage = STAGE_DONE;
      return true;
    }
  }
  
  
  public boolean actionReturnVehicle(Actor actor, Venue hangar) {
    if (! driven.setPilot(actor)) {
      driven = null;
      return false;
    }
    else driven.pathing.updateTarget(hangar);
    
    if (driven.aboard() == hangar) {
      driven.setPilot(null);
      actor.goAboard(hangar, actor.world());
      driven = null;
      return true;
    }
    return false;
  }
  
  
  public boolean actionReturnProceeds(Actor actor, Owner origin) {
    actor.gear.incCredits(0 - goodsPrice);
    origin.inventory().incCredits(goodsPrice);
    stage = STAGE_DONE;
    return true;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    
    if (stage == STAGE_RETURN) {
      d.append("Returning to ");
      d.append(origin);
      return;
    }
    
    d.append("Delivering");
    for (Item i : items) {
      d.append(" "+i.type);
    }
    
    if (origin != actor) {
      d.append(" from ");
      d.append(origin);
    }
    d.append(" to ");
    d.append(destination);
  }
}












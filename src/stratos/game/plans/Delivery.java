/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.util.*;
import stratos.game.base.Suspensor;
import stratos.game.economic.Inventory.Owner;



//  TODO:  Unify this with Smuggling?  Same basic ideas involved.

//  TODO:  You may need to trim down the list of items based on what's actually
//  available once an actor reaches their destination.


public class Delivery extends Plan {
  

  
  private static boolean
    evalVerbose  = false,
    stepsVerbose = false;
  
  //  TODO:  Use these.
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
  
  
  final public Owner origin, destination;
  
  private byte stage = STAGE_INIT;
  private Item items[];
  private Owner shouldPay;
  private float goodsPrice, goodsBulk;
  
  private Suspensor suspensor; //  TODO:  Unify with vehicle.
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
  
  
  public int stage() { return stage; }
  
  
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
  //  TODO:  Consider moving this to the DeliveryUtils' rateTrading method?
  
  //  TODO:  Or see if it can be merged with the hasItemsFrom method below?...
  
  public Delivery setWithPayment(Owner pays, boolean priceLimit) {
    
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
        canAfford.add(i);
      }
      if (canAfford.size() == 0) return null;
      
      this.shouldPay = pays;
      items = canAfford.toArray(Item.class);
      hasItemsFrom(origin);
      return this;
    }
    
    else {
      hasItemsFrom(origin);
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
  
  
  private boolean manned(Owner o) {
    if (o instanceof Property) return ((Property) o).isManned();
    return false;
  }
  
  
  private boolean hasItemsFrom(Owner carries) {
    final Batch <Item> has = new Batch <Item> ();
    goodsPrice = goodsBulk = 0;
    for (Item i : items) {
      final float amount = Nums.min(i.amount, carries.inventory().amountOf(i));
      has.add(i = Item.withAmount(i, amount));
      goodsPrice += i.priceAt(origin);
      goodsBulk  += i.amount;
    }
    if (has.size() == 0) return false;
    this.items = has.toArray(Item.class);
    return true;
  }
  
  
  
  /**  Assessing targets and priorities-
    */
  protected float getPriority() {
    if (items == null || items.length == 0) return -1;
    
    final boolean report = evalVerbose && I.talkAbout == actor;
    final boolean shops = shouldPay == actor;
    float base = ROUTINE, modifier = NO_MODIFIER;
    if (shouldPay != destination) base = CASUAL;
    
    if (shops && stage <= STAGE_PICKUP) {
      if (! manned(origin)) return -1;
      
      int price = 0;
      for (Item i : items) {
        price += i.priceAt(origin);
        modifier += ActorMotives.rateDesire(i, null, actor);
      }
      if (price > actor.gear.credits()) return 0;
      modifier -= ActorMotives.greedPriority(actor, price);
    }
    
    if (! shops) for (Item i : items) {
      modifier += i.amount / 10f;
    }
    
    final float rangeDiv = driven == null ? 2f : 10f;
    final float extraRangePenalty = (
      Plan.rangePenalty(actor, origin) +
      Plan.rangePenalty(origin, destination)
    ) / rangeDiv;
    
    final float priority = priorityForActorWith(
      actor, destination,
      base, modifier - extraRangePenalty,
      NO_HARM, NO_COMPETITION, NO_FAIL_RISK,
      NO_SKILLS, NO_TRAITS, NORMAL_DISTANCE_CHECK / rangeDiv,
      report
    );
    if (report) {
      I.say("  Shopping? "+shops);
      I.say("  Base/modifier: "+base+"/"+modifier);
    }
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
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) I.say("\nGetting next delivery step: "+actor);
    
    if (shouldPay != null && shouldPay != actor && shouldPay != destination) {
      I.complain("PAYING AGENT MUST BE EITHER ACTOR OR DESTINATION!");
    }
    
    if (driven != null && actor.aboard() != driven) {
      if (report) I.say("  Boarding vehicle: "+driven);
      
      final Action boarding = new Action(
        actor, driven,
        this, "actionBoardVehicle",
        Action.STAND, "Boarding vehicle"
      );
      return boarding;
    }
    
    if (stage == STAGE_PICKUP || stage == STAGE_INIT) {
      if (report) I.say("  Performing pickup from "+origin);
      if (! hasItemsFrom(origin)) return null;
      
      stage = STAGE_PICKUP;
      final Action pickup = new Action(
        actor, origin,
        this, "actionPickup",
        Action.REACH_DOWN, "Picking up goods"
      );
      if (driven != null) pickup.setMoveTarget(driven);
      return pickup;
    }
    
    if (stage == STAGE_DROPOFF) {
      if (report) I.say("  Performing dropoff at "+destination);
      
      final Owner carries = driven == null ? actor : driven;
      if (! hasItemsFrom(carries)) return null;
      
      final Action dropoff = new Action(
        actor, destination,
        this, "actionDropoff",
        Action.REACH_DOWN, "Dropping off goods"
      );
      if (driven != null) dropoff.setMoveTarget(driven);
      return dropoff;
    }
    
    if (stage == STAGE_RETURN && driven != null) {
      if (report) I.say("  Returning vehicle to "+driven.hangar());
      
      final Action returns = new Action(
        actor, driven.hangar(),
        this, "actionReturnVehicle",
        Action.REACH_DOWN, "Returning in vehicle"
      );
      returns.setMoveTarget(driven);
      return returns;
    }
    
    if (stage == STAGE_RETURN && driven == null) {
      if (report) I.say("  Reporting profits to "+origin);
      
      final Action returns = new Action(
        actor, origin,
        this, "actionReturnProceeds",
        Action.TALK_LONG, "Returning profits"
      );
      return returns;
    }
    
    if (report) I.say("  No next step, will quit.");
    return null;
  }
  
  
  public boolean actionBoardVehicle(Actor actor, Vehicle driven) {
    actor.goAboard(driven, actor.world());
    if (! driven.setPilot(actor)) interrupt(INTERRUPT_CANCEL);
    return true;
  }
  
  
  private boolean drivingDone(Target toward) {
    if (actor.aboard() != driven) return false;
    if (! driven.setPilot(actor)) interrupt(INTERRUPT_CANCEL);
    else driven.pathing.updateTarget(toward);
    if (driven.aboard() != toward) return false;
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
  

  public boolean actionPickup(Actor actor, Owner origin) {
    if (stage != STAGE_PICKUP) return false;
    if (driven != null && ! drivingDone(origin)) return false;
    
    //  TODO:  Return the suspensor to it's point of origin.  (Not a lot of
    //         extra effort now.)
    if (goodsBulk > 5 && driven == null) {
      final Suspensor suspensor = new Suspensor(actor, this);
      final Tile o = actor.origin();
      suspensor.enterWorldAt(o.x, o.y, o.world);
      this.suspensor = suspensor;
    }
    //
    //  Perform the actual transfer of goods and, if making a personal trade,
    //  make the payment required:
    if (shouldPay == actor) {
      origin.inventory().incCredits(goodsPrice);
      actor.gear.incCredits(0 - goodsPrice);
      shouldPay = null;
    }
    transferGoods(origin, driven == null ? actor : driven);
    stage = STAGE_DROPOFF;
    return true;
  }
  
  
  public boolean actionDropoff(Actor actor, Owner destination) {
    if (stage != STAGE_DROPOFF) return false;
    if (driven != null && ! drivingDone(destination)) return false;
    
    if (suspensor != null && suspensor.inWorld()) suspensor.exitWorld();
    
    if (shouldPay == destination) {
      destination.inventory().incCredits(0 - goodsPrice);
      actor.gear.incCredits(goodsPrice);
    }
    transferGoods(driven == null ? actor : driven, destination);
    
    if (shouldPay != null || driven != null) stage = STAGE_RETURN;
    else stage = STAGE_DONE;
    return true;
  }
  
  
  public boolean actionReturnVehicle(Actor actor, Venue hangar) {
    if (stage != STAGE_RETURN) return false;
    if (! drivingDone(hangar)) return false;
    
    driven.setPilot(null);
    actor.goAboard(hangar, actor.world());
    driven = null;
    return true;
  }
  
  
  public boolean actionReturnProceeds(Actor actor, Owner origin) {
    if (stage != STAGE_RETURN) return false;
    actor.gear.incCredits(0 - goodsPrice);
    origin.inventory().incCredits(goodsPrice);
    stage = STAGE_DONE;
    return true;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    
    if (stage == STAGE_RETURN) {
      if (driven == null) d.append("Returning profits to ");
      else d.append("Returning to ");
      d.append(origin);
      return;
    }
    
    d.append("Delivering ");
    for (Item i : items) {
      //d.append((int) i.amount+" "+i.type);
      i.describeTo(d);
      if (i != Visit.last(items)) d.append(", ");
    }
    
    if (origin != actor) {
      d.append(" from ");
      d.append(origin);
    }
    d.append(" to ");
    d.append(destination);
  }
}












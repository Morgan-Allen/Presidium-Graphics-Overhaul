/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.civic.Suspensor;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.util.*;


//  TODO:  There must be some way to automatically inject these standardised
//  save/load methods...

//  TODO:  Unify this with Smuggling?  Same basic ideas involved.
public class Bringing extends Plan {
  

  
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
    STAGE_PICKUP  =  1,
    STAGE_DROPOFF =  2,
    STAGE_RETURN  =  3,
    STAGE_DONE    =  4;
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
  
  
  public Bringing(Traded s, Owner origin, Owner destination) {
    this(origin.inventory().matches(s), origin, destination);
  }
  
  
  public Bringing(Item item, Owner origin, Owner destination) {
    this(new Item[] { item }, origin, destination);
  }
  
  
  public Bringing(Batch <Item> items, Owner orig, Owner dest) {
    this(items.toArray(Item.class), orig, dest);
  }
  
  
  public Bringing(Item items[], Owner origin, Owner destination) {
    super(null, destination, MOTIVE_JOB, NO_HARM);
    this.origin = origin;
    this.destination = destination;
    this.items = items;
  }
  
  
  public Bringing(Vehicle vehicle, Owner destination) {
    this(new Item[0], vehicle, destination);
    this.driven = vehicle;
    this.stage = STAGE_RETURN;
  }
  
  
  public Bringing(Session s) throws Exception {
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
  public float totalPrice() { return goodsPrice; }
  public float totalBulk() { return goodsBulk; }
  
  
  public boolean matchesPlan(Behaviour plan) {
    if (! super.matchesPlan(plan)) return false;
    final Bringing d = (Bringing) plan;
    if (d.origin != origin || d.destination != destination) return false;
    boolean overlap = false;
    for (Item i : items) for (Item dI : d.items) {
      if (i.type == dI.type) overlap = true;
    }
    return overlap;
  }
  
  
  
  /**  Initial culling methods to allow for budget and inventory limits-
    */
  public Bringing setWithPayment(Owner pays) {
    //
    //  Basic variable bindings and sanity checks first-
    if (pays instanceof Actor) {
      attemptToBind((Actor) pays);
    }
    final boolean report = evalVerbose && (
      I.talkAbout == actor || I.talkAbout == pays
    );
    if (report) I.say("\nChecking for items at "+origin);
    
    if (stage >= STAGE_RETURN) {
      if (report) I.say("  Delivery complete.  Will return.");
      return this;
    }
    if (items == null || items.length == 0) {
      if (report) I.say("  No items present!");
      return null;
    }
    if (pays != null && pays != actor && pays != destination) {
      I.complain("  PAYING AGENT MUST BE EITHER ACTOR OR DESTINATION!");
      return null;
    }
    //
    //  Then, we determine who sets the price, who carries the goods, limits
    //  on total price paid, and reset our price/bulk counters.
    final Owner setsPrice = (origin.owningTier() > destination.owningTier()) ?
      origin : destination
    ;
    Owner carries = null;
    if (stage == STAGE_DROPOFF) carries = driven == null ? actor : driven;
    if (stage <= STAGE_PICKUP ) carries = origin;
    final boolean
      mustPay    = pays != null,
      priceLimit = pays == actor && stage <= STAGE_PICKUP,
      sellerSets = setsPrice == origin;
    if (report) {
      I.say("  Must pay?       "+mustPay);
      I.say("  Limit by price? "+priceLimit);
      I.say("  Prices set by:  "+setsPrice+" (seller: "+sellerSets+")");
      I.say("  Should carry:   "+carries);
    }
    
    final float maxPrice = mustPay ? (pays.inventory().allCredits() / 2) : -1;
    goodsPrice  =  0;
    goodsBulk   =  0;
    float itemPrice = -1;
    final Batch <Item> canBring = new Batch <Item> ();
    //
    //  Finally, we then iterate across every desired item, and ensure they are
    //  both affordable and physically present.
    itemsLoop: for (Item i : items) {
      final float amount = Nums.min(i.amount, carries.inventory().amountOf(i));
      if (amount <= 0) continue;
      if (amount < i.amount) i = Item.withAmount(i, amount);
      
      while (true) {
        itemPrice = i.priceAt(setsPrice, sellerSets);
        if (priceLimit && (goodsPrice + itemPrice > maxPrice)) {
          if (i.amount <= 1) break itemsLoop;
          i = Item.withAmount(i, i.amount - 1);
        }
        else break;
      }
      goodsPrice += itemPrice;
      goodsBulk  += i.amount ;
      canBring.add(i);
      if (report) I.say("  Price after "+i+" is "+goodsPrice);
    }
    //
    //  If nothing could be obtained, we return a null result- otherwise,
    //  compile and continue...
    if (canBring.size() == 0) {
      if (report) I.say("  No goods obtainable.");
      return null;
    }
    this.shouldPay = pays;
    items = canBring.toArray(Item.class);
    return this;
  }
  
  
  public float pricePaid() {
    return goodsPrice;
  }
  
  
  public Item delivered(Traded type) {
    for (Item i : items) if (i.type == type) return i;
    return null;
  }
  
  
  public Item[] allDelivered() {
    return items;
  }
  
  
  private boolean manned(Owner o) {
    if (o instanceof Property) return ((Property) o).openFor(actor);
    return false;
  }
  
  
  
  /**  Register and unregistering reservations-
    */
  public void toggleActive(boolean is) {
    super.toggleActive(is);
    origin     .inventory().setReservation(this, is);
    destination.inventory().setReservation(this, is);
  }
  
  
  
  /**  Assessing targets and priorities-
    */
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (report) I.say("\nEvaluating special factors for delivery priority:");
    //
    //  Determine basic priorities and delivery type:
    final boolean shops = shouldPay == actor;
    float base = ROUTINE, modifier = NO_MODIFIER, greedMinus = 0;
    if (setWithPayment(shouldPay) == null) {
      if (report) I.say("  Delivery not longer possible!");
      return -1;
    }
    //
    //  Personal purchases get a few special modifiers-
    if (shops) {
      base = CASUAL;
      for (Item i : items) {
        modifier += ActorMotives.rateDesire(i, null, actor);
      }
      modifier = Nums.clamp(modifier, 0, PARAMOUNT);
      
      final boolean needsBuy = stage <= STAGE_PICKUP;
      if (needsBuy && ! manned(origin)) {
        if (report) I.say("  Origin is not manned!");
        modifier -= ROUTINE;
      }
      if (needsBuy) {
        float pricePerDay = goodsPrice / GameSettings.ITEM_WEAR_DAYS;
        greedMinus = actor.motives.greedPriority(pricePerDay);
      }
    }
    //
    //  Otherwise, add a bonus for quantity and value-
    if (! shops) {
      base = ROUTINE;
      modifier += goodsPrice / 100f;
    }

    //
    //  Finally, since this plan involves a good deal of travel, we modify the
    //  usual distance evaluation.  Otherwise, proceed as normal.
    final float rangeDiv = driven == null ? 2f : 10f;
    final float extraRangePenalty = (
      Plan.rangePenalty(actor.base(), actor , origin     ) +
      Plan.rangePenalty(actor.base(), origin, destination)
    ) / rangeDiv;
    
    //  TODO:  Move this out to PlanUtils, I think?
    
    float priority = base + modifier - (extraRangePenalty + greedMinus);
    if (report) I.reportVars("", "  ",
      "Shopping?"     , shops,
      "Base/modifier:", base+"/"+modifier,
      "Range penalty:", extraRangePenalty,
      "Greed penalty:", greedMinus,
      "Priority"      , priority
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
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) {
      I.say("\nGetting next delivery step: "+actor);
      I.say("  Origin:      "+origin     );
      I.say("  Destination: "+destination);
      I.say("  Plan ID:     "+hashCode() );
    }
    //
    //  Initially, we may need to pick up a delivery vehicle or necessary funds
    //  for payment.  (Vehicles also need to be returned afterward.)
    if (stage == STAGE_INIT) stage = STAGE_PICKUP;
    
    if (driven != null && actor.aboard() != driven) {
      if (report) I.say("\nWill board "+driven);
      
      final Action boarding = new Action(
        actor, driven,
        this, "actionBoardVehicle",
        Action.STAND, "Boarding vehicle"
      );
      boarding.setMoveTarget(driven);
      return boarding;
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
    //
    //  Otherwise, simply pickup at the origin and dropoff at the destination-
    if (stage == STAGE_PICKUP) {
      if (report) I.say("  Performing pickup from "+origin);
      
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
      
      final Action meets = new Action(
        actor, destination,
        this, "actionMeets",
        Action.TALK, "Meeting with "
      );
      if (! isMeeting()) meets.setProperties(Action.RANGED);
      if (destination instanceof Actor) return meets;
      
      final Action dropoff = new Action(
        actor, destination,
        this, "actionDropoff",
        Action.REACH_DOWN, "Dropping off goods"
      );
      if (driven != null) dropoff.setMoveTarget(driven);
      return dropoff;
    }
    
    if (report) I.say("  No next step, will quit.");
    return null;
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
        if (match != null) b.inventory().removeItem(match);
      }
      a.inventory().transfer(i, b);
    }
  }
  
  
  public boolean actionBoardVehicle(Actor actor, Vehicle driven) {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) {
      final Boarding reaches[] = actor.aboard().canBoard();
      I.say("\nGoing aboard "+driven);
      I.say("  Currently aboard: "+actor.aboard());
      I.say("  Can reach?        "+Visit.arrayIncludes(reaches, driven));
    }
    
    actor.goAboard(driven, actor.world());
    if (! driven.setPilot(actor)) interrupt(INTERRUPT_CANCEL);
    return true;
  }
  

  public boolean actionPickup(Actor actor, Owner origin) {
    if (stage != STAGE_PICKUP) return false;
    if (driven != null && ! drivingDone(origin)) return false;
    
    if (I.talkAbout == actor) {
      I.say("Picking up!");
    }
    
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
    transferGoods(origin, driven == null ? actor : driven);
    if (shouldPay != null ) origin.inventory().incCredits(goodsPrice);
    if (shouldPay == actor) actor.gear.incCredits(0 - goodsPrice);
    stage = STAGE_DROPOFF;
    return true;
  }
  
  
  private boolean isMeeting() {
    if (! (destination instanceof Actor)) return false;
    return ((Actor) destination).isDoingAction("actionMeets", actor);
  }
  
  
  public boolean actionMeets(Actor actor, Actor other) {
    //
    //  This method can be called by either the deliverer or the recipient, so
    //  we have to put a fork in the evaluation...
    if (actor == destination) {
      actionDropoff(this.actor, destination);
      return true;
    }
    //
    //  If a meeting is already in progress, we can just wait for the recipient
    //  to arrive.  Otherwise, assign them that behaviour (or quit if they're
    //  too busy.)
    if (isMeeting()) return true;
    final Action meets = new Action(
      other, actor,
      this, "actionMeets",
      Action.TALK, "Meeting "+actor
    );
    meets.setPriority(Action.ROUTINE);
    if (other.mind.mustIgnore(meets)) {
      interrupt(INTERRUPT_CANCEL);
      return false;
    }
    other.mind.assignBehaviour(meets);
    other.assignAction(meets);
    return true;
  }
  
  
  public boolean actionDropoff(Actor actor, Owner destination) {
    if (stage != STAGE_DROPOFF) return false;
    if (driven != null && ! drivingDone(destination)) return false;
    
    if (suspensor != null && suspensor.inWorld()) suspensor.exitWorld();
    transferGoods(driven == null ? actor : driven, destination);
    
    final float cost = 0 - goodsPrice;
    if (shouldPay == destination) shouldPay.inventory().incCredits(cost);
    
    if (driven != null) stage = STAGE_RETURN;
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
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    
    if (stage == STAGE_RETURN && driven != null) {
      d.append("Returning to ");
      d.append(origin);
      return;
    }
    
    d.append("Bringing ");
    for (Item i : items) {
      i.describeFor(actor, d);
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












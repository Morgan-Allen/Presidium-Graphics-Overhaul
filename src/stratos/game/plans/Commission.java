


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.base.Pledge;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;


//  Purchase commodities for home.
//  Purchase a device/weapon, or outfit/armour.
//  Purchase rations, fuel cells, ammo or medkits.


//  TODO:  Stocks should be storing a list of Commissions, and checking if
//  those have expired...


public class Commission extends Plan {
  
  
  
  /**  Data fields, construction and save/load methods-
    */
  private static boolean
    evalVerbose  = false,
    stepsVerbose = false;
  
  final static int
    EXPIRE_TIME = Stage.STANDARD_DAY_LENGTH * 2,
    MAX_ORDERS  = 4;
  
  final Item item;
  final Venue shop;
  
  private float   price     = -1;
  private float   orderDate = -1;
  private boolean delivered = false;
  
  
  private Commission(Actor actor, Item baseItem, Venue shop) {
    super(actor, shop, MOTIVE_PERSONAL, NO_HARM);
    this.item = Item.withReference(baseItem, actor);
    this.shop = shop;
    
    if (item.type.materials() == null) {
      I.complain("COMMISSIONED ITEMS MUST HAVE A CONVERSION SPECIFIED!");
    }
  }
  
  
  public Commission(Session s) throws Exception {
    super(s);
    item = Item.loadFrom(s);
    shop = (Venue) s.loadObject();
    orderDate = s.loadFloat();
    delivered = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    Item.saveTo(s, item);
    s.saveObject(shop);
    s.saveFloat(orderDate);
    s.saveBool(delivered);
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  public boolean matchesPlan(Behaviour p) {
    if (! super.matchesPlan(p)) return false;
    return ((Commission) p).item.type == this.item.type;
  }
  
  
  
  /**  Assessing and locating targets-
    */
  //  TODO:  Specify the actor who buys and the actor it's intended for
  //  separately!
  
  public static void addCommissions(
    Actor actor, Venue makes, Choice choice, Traded itemType, Upgrade limits
  ) {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (report) I.say("\nChecking commissions for "+actor);
    
    final boolean hasCommission = actor.mind.hasToDo(Commission.class);
    if (hasCommission) return;
    final Item device = actor.gear.deviceEquipped();
    final Item outfit = actor.gear.outfitEquipped();
    
    if (device != null && device.type == itemType) {
      choice.add(nextCommission(actor, makes, device, limits));
    }
    if (outfit != null && outfit.type == itemType) {
      choice.add(nextCommission(actor, makes, outfit, limits));
    }
  }
  
  
  private static Commission nextCommission(
    Actor actor, Venue makes, Item baseItem, Upgrade limits
  ) {
    if (baseItem == null || ! makes.openFor(actor)) return null;
    if (makes.stocks.specialOrders().size() >= MAX_ORDERS) return null;
    
    final boolean report = evalVerbose && I.talkAbout == actor;
    final int baseQuality = (int) baseItem.quality;
    
    //
    //  We constrain maximum item-quality by the upgrade-level available at the
    //  venue in question...
    final float upgradeLevel = limits == null ? 1 : (
      makes.structure().upgradeLevel(limits) / limits.maxLevel
    );
    int maxQuality = Item.MAX_QUALITY + 1;
    maxQuality -= (Item.MAX_QUALITY - 1) * (1 - upgradeLevel);
    final boolean needsReplace = baseItem.amount < 0.5f;
    
    //
    //  Then we see if this exceeds the quality of the item of this type the
    //  actor currently possesses-
    int quality = maxQuality;
    Commission added = null;
    Item upgrade = null;
    while (--quality > 0) {
      //
      //  TODO:  Unify this with the priority-eval methods below!
      upgrade = Item.withQuality(baseItem.type, quality);
      final float price = calcPrice(upgrade, makes);
      final boolean done = makes.stocks.hasItem(upgrade);
      
      if (price >= actor.gear.allCredits() && ! done) continue;
      if (quality <= baseQuality && ! needsReplace) continue;
      else if (quality < baseQuality) continue;
      
      added = new Commission(actor, upgrade, makes);
      if (added.priorityFor(actor) <= 0) continue;
      break;
    }
    
    if (report) {
      I.say("\nConsidering commission for "+baseItem);
      I.say("  Max quality:  "+maxQuality);
      I.say("  Owner cash:   "+actor.gear.allCredits());
      I.say("  New item:     "+upgrade);
      I.say("  Base price:   "+upgrade.defaultPrice());
      I.say("  Vended price: "+calcPrice(upgrade, makes));
      if (added == null) I.say("  Can't afford replacement!");
    }
    return added;
  }
  
  
  private static float calcPrice(Item item, Venue shop) {
    float price = item.priceAt(shop, true);
    final Conversion m = item.type.materials();
    if (m != null) for (Item i : m.raw) price += i.priceAt(shop, true);
    return price;
  }
  
  
  final static Trait BASE_TRAITS[] = { ACQUISITIVE };
  
  
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (report) I.say("Getting priority for commission of "+item);
    //
    //  See if we're still waiting on completion-
    final boolean done = shop.stocks.hasItem(item);
    if (orderDate != -1 && ! done) {
      if (report) I.say("  Manufacture not complete.");
      return 0;
    }
    //
    //  Include effects of pricing and quality-
    if (price == -1) price = calcPrice(item, shop);
    float modifier = item.quality * ROUTINE * 1f / Item.MAX_QUALITY;
    if (price > actor.gear.allCredits() && ! done) {
      if (report) I.say("  Can't afford item.");
      return 0;
    }
    //
    //  If the item isn't finished yet, reduce eagerness to buy based on cost.
    //  And if the order isn't placed yet, try to avoid venues that have a lot
    //  of orders placed...
    if (! done) {
      final float dailyPrice = price / GameSettings.ITEM_WEAR_DAYS;
      modifier -= actor.motives.greedPriority(dailyPrice);
    }
    if (orderDate == -1 && ! done) {
      modifier -= shop.stocks.specialOrders().size() * ROUTINE / MAX_ORDERS;
    }
    //
    //  Summarise and return.
    final float priority = PlanUtils.jobPlanPriority(
      actor, this, modifier / ROUTINE, 1, -1, 0, BASE_TRAITS
    );
    if (report) {
      I.say("  Price value:      "+price   );
      I.say("  Manufacture done: "+done    );
      I.say("  Final priority:   "+priority);
    }
    return Nums.clamp(priority, 0, ROUTINE);
  }
  
  
  private boolean expired() {
    if (orderDate == -1 || shop.stocks.hasItem(item)) return false;
    if (actor.world().currentTime() - orderDate > EXPIRE_TIME) {
      return true;
    }
    return ! shop.stocks.hasOrderFor(item);
  }
  
  
  public boolean finished() {
    return delivered;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    if (finished() || item.type.materials() == null) return null;
    
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) I.say("\nGetting next commission step for "+actor);

    if (! shop.openFor(actor)) return null;
    if (! shop.structure.intact()) return null;
    
    if (expired()) {
      if (report) I.say("  Commission has expired.");
      delivered = true;
      return null;
    }
    
    if (shop.stocks.hasItem(item)) {
      if (report) I.say("  Picking up "+item);
      final Action pickup = new Action(
        actor, shop,
        this, "actionPickupItem",
        Action.REACH_DOWN, "Collecting "
      );
      return pickup;
    }
    
    if (! shop.stocks.hasOrderFor(item)) {
      if (report) I.say("  Placing order for "+item);
      final Action placeOrder = new Action(
        actor, shop,
        this, "actionPlaceOrder",
        Action.TALK_LONG, "Placing order for "
      );
      return placeOrder;
    }
    return null;
  }
  
  
  public boolean actionPlaceOrder(Actor actor, Venue shop) {
    if (shop.stocks.hasOrderFor(item)) return false;
    
    shop.stocks.addSpecialOrder(item);
    orderDate = shop.world().currentTime();
    return true;
  }
  
  
  public boolean actionPickupItem(Actor actor, Venue shop) {
    shop.inventory().removeMatch(item);
    actor.inventory().addItem(item);
    shop.stocks.deleteSpecialOrder(item);
    
    shop .inventory().incCredits(    price);
    actor.inventory().incCredits(0 - price);
    delivered = true;
    return true;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (shop.stocks.hasOrderFor(item) && ! shop.stocks.hasItem(item)) {
      d.append("Collecting ");
      item.describeFor(actor, d);
      d.append(" at ");
      d.append(shop);
      return;
    }
    if (super.needsSuffix(d, "Ordering ")) {
      item.describeFor(actor, d);
      d.append(" at ");
      d.append(shop);
    }
  }
}







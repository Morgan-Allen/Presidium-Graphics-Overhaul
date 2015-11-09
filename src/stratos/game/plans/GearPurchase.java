/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
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


public class GearPurchase extends Plan {
  
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
  
  
  private GearPurchase(Actor actor, Item baseItem, Venue shop) {
    super(actor, shop, MOTIVE_PERSONAL, NO_HARM);
    this.item = Item.withReference(baseItem, actor);
    this.shop = shop;
    
    if (item.type.materials() == null) {
      I.complain("COMMISSIONED ITEMS MUST HAVE A CONVERSION SPECIFIED!");
    }
  }
  
  
  public GearPurchase(Session s) throws Exception {
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
    return ((GearPurchase) p).item.type == this.item.type;
  }
  
  
  
  /**  Assessing and locating targets-
    */
  public static Item nextGearToPurchase(
    Actor actor, Venue makes, Traded... services
  ) {
    final Pick <Traded> pick = new Pick(0);
    
    if (! Visit.empty(services)) for (Traded t : services) {
      pick.compare(t, t.useRating(actor));
    }
    else for (Traded t : actor.skills.getProficiencies()) {
      if (! t.hasSourceAt(makes)) continue;
      pick.compare(t, t.useRating(actor));
    }
    
    final Traded itemType = pick.result();
    if (itemType == null) return null;
    
    final Item match = actor.gear.bestSample(itemType, null, -1);
    if (match != null) return match;
    else return Item.with(itemType, null, 0.1f, Item.BAD_QUALITY);
  }
  
  
  public static GearPurchase nextCommission(
    Actor actor, Venue makes, Item baseItem, Upgrade... limits
  ) {
    if (baseItem == null || ! makes.openFor(actor)) return null;
    if (makes.stocks.specialOrders().size() >= MAX_ORDERS) return null;
    final boolean report = I.talkAbout == actor && evalVerbose;
    
    //
    //  In the case of consumable items, we simply return a top-up relative to
    //  the carry-limit for it's type.
    if (baseItem.type.common() || baseItem.type instanceof UsedItemType) {
      final int   limit    = baseItem.type.normalCarry(actor);
      final float amount   = actor.gear.amountOf(baseItem);
      final Item  replaces = Item.with(
        baseItem.type, null, limit - amount, Item.AVG_QUALITY
      );
      return new GearPurchase(actor, replaces, makes);
    }
    
    //
    //  For devices and outfits, we constrain maximum item-quality by the
    //  upgrade-level available at the venue in question...
    final int baseQuality = (int) baseItem.quality;
    final int maxQuality = Manufacture.topQuality(
      makes, baseItem.type.materials(), limits
    );
    final boolean needsReplace = baseItem.amount < 0.5f;
    
    //
    //  Then we see if this exceeds the quality of the item of this type the
    //  actor currently possesses-
    int quality = maxQuality + 1;
    GearPurchase added = null;
    Item upgrade = null;
    while (--quality > 0) {
      //
      //  TODO:  Unify this with the priority-eval methods below!
      upgrade = Item.withQuality(baseItem.type, quality);
      final float price = upgrade.priceAt(makes, true);
      final boolean done = makes.stocks.hasItem(upgrade);
      
      if (price >= actor.gear.allCredits() && ! done) continue;
      if (quality <= baseQuality && ! needsReplace) continue;
      else if (quality < baseQuality) continue;
      
      added = new GearPurchase(actor, upgrade, makes);
      if (added.priorityFor(actor) > 0) break;
    }
    
    if (report) {
      I.say("\nConsidering commission for "+baseItem);
      I.say("  Max quality:  "+maxQuality);
      I.say("  Owner cash:   "+actor.gear.allCredits());
      I.say("  New item:     "+upgrade);
      I.say("  Base price:   "+upgrade.defaultPrice());
      I.say("  Vended price: "+upgrade.priceAt(makes, true));
      if (added == null) I.say("  Can't afford replacement!");
    }
    return added;
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
    if (shop == actor.mind.work() || shop == actor.mind.home()) price = 0;
    if (price == -1) price = item.priceAt(shop, true);
    float modifier = item.quality * ROUTINE * 1f / Item.AVG_QUALITY;
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
    return Nums.clamp(priority, 0, URGENT);
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
    shop .inventory().removeMatch(item);
    actor.inventory().addItem    (item);
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







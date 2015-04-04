


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


public class Commission extends Plan {
  
  
  
  /**  Data fields, construction and save/load methods-
    */
  private static boolean
    verbose       = false,
    actionVerbose = false;
  
  final static int EXPIRE_TIME = Stage.STANDARD_DAY_LENGTH * 2;
  
  final Item item;
  final Venue shop;
  
  private Manufacture order = null;
  private float price       = -1;
  private float orderDate   = -1;
  private boolean delivered = false;
  
  
  //  TODO:  Pass in the Conversion used for manufacture as well!
  
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
    order = (Manufacture) s.loadObject();
    orderDate = s.loadFloat();
    delivered = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    Item.saveTo(s, item);
    s.saveObject(shop);
    s.saveObject(order);
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
    Actor actor, Venue makes, Choice choice, Traded... itemTypes
  ) {
    final boolean report = verbose && I.talkAbout == actor;
    if (report) I.say("\nChecking commissions for "+actor);
    
    final boolean hasCommission = actor.mind.hasToDo(Commission.class);
    if (hasCommission) return;
    
    if (Visit.arrayIncludes(itemTypes, actor.gear.deviceType())) {
      choice.add(nextCommission(actor, makes, actor.gear.deviceEquipped()));
    }
    if (Visit.arrayIncludes(itemTypes, actor.gear.outfitType())) {
      choice.add(nextCommission(actor, makes, actor.gear.outfitEquipped()));
    }
  }
  
  
  private static Commission nextCommission(
    Actor actor, Venue makes, Item baseItem
  ) {
    if (baseItem == null || ! makes.isManned()) return null;

    final boolean report = verbose && I.talkAbout == actor;
    final int baseQuality = (int) baseItem.quality;
    final float baseAmount = baseItem.amount;
    
    int quality = Item.MAX_QUALITY + 1;
    Commission added = null;
    Item upgrade = null;
    
    while (--quality > 0) {
      upgrade = Item.withQuality(baseItem.type, quality);
      final float price = upgrade.priceAt(makes);
      if (price >= actor.gear.credits()) continue;
      if (quality <= baseQuality) continue;
      
      added = new Commission(actor, upgrade, makes);
      if (added.priorityFor(actor) <= 0) continue;
      break;
    }
    
    if (report) {
      I.say("\nRejected commission for "+baseItem);
      I.say("  Owner cash:   "+actor.gear.credits());
      I.say("  New item:     "+upgrade);
      I.say("  Base price:   "+upgrade.defaultPrice());
      I.say("  Vended price: "+upgrade.priceAt(makes));
      if (added == null) I.say("  Can't afford replacement!");
    }
    return added;
  }
  
  
  final static Trait BASE_TRAITS[] = { ACQUISITIVE };
  
  
  protected float getPriority() {
    final boolean report = verbose && I.talkAbout == actor;
    if (report) I.say("Getting priority for commission of "+item);
    //
    //  See if we're still waiting on completion-
    final boolean done = shop.stocks.hasItem(item);
    if (order != null && ! order.finished() && ! done) {
      if (report) I.say("  Manufacture not complete.");
      return 0;
    }
    //
    //  Include effects of pricing and quality-
    final float price = calcPrice();
    float modifier = NO_MODIFIER + item.quality;
    if (order == null) {
      if (price > actor.gear.credits()) {
        if (report) I.say("  Can't afford item.");
        return 0;
      }
      modifier -= actor.motives.greedPriority(price / ITEM_WEAR_DURATION);
    }
    
    final float priority = priorityForActorWith(
      actor, shop, CASUAL,
      modifier, MILD_HELP,
      MILD_COMPETITION, NO_FAIL_RISK,
      NO_SKILLS, BASE_TRAITS, NORMAL_DISTANCE_CHECK,
      report
    );
    if (report) {
      I.say("  Price value:      "+price   );
      I.say("  Manufacture done: "+done    );
      I.say("  Final priority:   "+priority);
    }
    return Nums.clamp(priority, 0, ROUTINE);
  }
  
  
  private float calcPrice() {
    if (price != -1) return price;
    
    price = item.priceAt(shop);
    final Conversion m = item.type.materials();
    if (m != null) for (Item i : m.raw) price += i.priceAt(shop);
    
    return price;
  }
  
  
  private boolean expired() {
    if (orderDate == -1 || shop.stocks.hasItem(item)) return false;
    if (actor.world().currentTime() - orderDate > EXPIRE_TIME) {
      return true;
    }
    return ! shop.stocks.specialOrders().includes(order);
  }
  
  
  public boolean finished() {
    return delivered;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    if (finished() || item.type.materials() == null) return null;
    if (shop.isManned()) return null;
    
    final boolean report = actionVerbose && I.talkAbout == actor;
    if (report) I.say("\nGetting next commission step for "+actor);
    
    if (order == null && shop.structure().intact()) {
      if (report) I.say("  Placing order for "+item);
      final Action placeOrder = new Action(
        actor, shop,
        this, "actionPlaceOrder",
        Action.TALK_LONG, "Placing order for "
      );
      return placeOrder;
    }
    
    if (expired()) {
      if (report) I.say("  Getting refund: "+(int) calcPrice()+" credits");
      final Action refund = new Action(
        actor, shop,
        this, "actionCollectRefund",
        Action.TALK_LONG, "Getting refund for "
      );
      return refund;
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
    return null;
  }
  
  
  public boolean actionPlaceOrder(Actor actor, Venue shop) {
    order = new Manufacture(null, shop, item.type.materials(), item);
    order.commission = this;
    shop.stocks.addSpecialOrder(order);
    orderDate = shop.world().currentTime();
    
    final int price = (int) calcPrice();
    shop .inventory().incCredits(    price);
    actor.inventory().incCredits(0 - price);
    return true;
  }
  
  
  public boolean actionPickupItem(Actor actor, Venue shop) {
    shop.inventory().removeMatch(item);
    actor.inventory().addItem(item);
    delivered = true;
    return true;
  }
  
  
  public boolean actionCollectRefund(Actor actor, Venue shop) {
    final int price = (int) calcPrice();
    shop .inventory().incCredits(0 - price);
    actor.inventory().incCredits(    price);
    order.addMotives(Plan.MOTIVE_CANCELLED, 0);
    delivered = true;
    return true;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (super.needsSuffix(d, "Placing order for ")) {
      item.describeTo(d);
      d.append(" at ");
      d.append(shop);
    }
  }
}









package stratos.game.civilian ;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.user.*;
import stratos.util.*;



//
//  Purchase commodities for home.
//  Purchase a device/weapon, or outfit/armour.
//  Purchase rations, fuel cells or a medkit.



public class Commission extends Plan implements Economy {
  
  
  
  /**  Data fields, construction and save/load methods-
    */
  private static boolean verbose = false;
  
  
  final Item item ;
  final Venue shop ;
  
  private Manufacture order = null ;
  private float orderDate = -1 ;
  private boolean delivered = false ;
  
  
  public Commission(Actor actor, Item baseItem, Venue shop) {
    super(actor, shop) ;
    this.item = Item.withReference(baseItem, actor) ;
    this.shop = shop ;
  }
  
  
  public Commission(Session s) throws Exception {
    super(s) ;
    item = Item.loadFrom(s) ;
    shop = (Venue) s.loadObject() ;
    order = (Manufacture) s.loadObject() ;
    orderDate = s.loadFloat() ;
    delivered = s.loadBool() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    Item.saveTo(s, item) ;
    s.saveObject(shop) ;
    s.saveObject(order) ;
    s.saveFloat(orderDate) ;
    s.saveBool(delivered) ;
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  public boolean matchesPlan(Plan p) {
    if (! super.matchesPlan(p)) return false;
    return ((Commission) p).item.type == this.item.type;
  }
  
  
  
  /**  Assessing and locating targets-
    */
  public static Commission forItem(Actor actor, Item baseItem) {
    if (baseItem == null || actor == null) return null;
    final Venue match = (Venue) actor.world().presences.nearestMatch(
      baseItem.type, actor, World.SECTOR_SIZE
    );
    if (match == null) return null;
    return nextCommission(actor, match, baseItem);
  }
  
  
  public static void addCommissions(
    Actor actor, Venue makes, Choice choice
  ) {
    final boolean hasCommission = actor.mind.hasToDo(Commission.class);
    if (hasCommission) return;
    choice.add(nextCommission(actor, makes, actor.gear.deviceEquipped()));
    choice.add(nextCommission(actor, makes, actor.gear.outfitEquipped()));
  }
  
  
  public static Commission nextCommission(
    Actor actor, Venue makes, Item baseItem
  ) {
    if (baseItem == null) return null;
    //if (! Visit.arrayIncludes(makes.services(), baseItem.type)) return null;
    
    final int baseQuality = (int) baseItem.quality;
    final float baseAmount = baseItem.amount;
    
    int quality = Item.MAX_QUALITY + 1;
    Commission added = null;
    
    while (--quality > 0) {
      final Item upgrade = Item.withQuality(baseItem.type, quality);
      final float price = upgrade.priceAt(makes);
      if (price >= actor.gear.credits()) continue;
      added = new Commission(actor, upgrade, makes);
      if (added.priorityFor(actor) <= 0) continue;
      break;
    }
    
    if (quality >= 0 && (baseAmount <= 0.5f || quality > baseQuality)) {
      return added;
    }
    return null;
  }
  
  
  final static Trait BASE_TRAITS[] = { ACQUISITIVE };
  
  
  protected float getPriority() {
    final boolean report = verbose && I.talkAbout == actor;
    if (report) I.say("Getting priority for commision of "+item);
    
    final boolean done = shop.stocks.hasItem(item) ;
    if (order != null && ! order.finished() && ! done) {
      return 0 ;
    }
    final float price = item.priceAt(shop);
    if (price > actor.gear.credits()) return 0;
    
    final float greed = Plan.greedLevel(actor, price / NUM_WEAR_DAYS) * ROUTINE;
    float modifier = NO_MODIFIER + item.quality - greed;
    
    final float priority = priorityForActorWith(
      actor, shop, CASUAL,
      MILD_HELP, MILD_COMPETITION,
      NO_SKILLS, BASE_TRAITS,
      modifier, NORMAL_DISTANCE_CHECK, NO_FAIL_RISK,
      report
    );
    
    if (report) {
      I.say("\nGetting priority for commission of "+item);
      I.say("  Price/greed value: "+price+"/"+greed);
      I.say("  Final priority is: "+priority);
    }
    return Visit.clamp(priority, 0, ROUTINE) ;
  }
  
  
  private boolean expired() {
    if (orderDate == -1) return false ;
    final int maxTime = World.STANDARD_DAY_LENGTH * 10 ;
    if (actor.world().currentTime() - orderDate > maxTime) return true ;
    final boolean
      ongoing = shop.stocks.specialOrders().includes(order),
      hasItem = shop.stocks.hasItem(item) ;
    if (ongoing || hasItem) return false ;
    return true ;
  }
  
  
  public boolean finished() {
    return delivered || expired();
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    if (finished()) return null ;
    final boolean report = I.talkAbout == actor;
    
    if (order == null && shop.structure().intact()) {
      final Action placeOrder = new Action(
        actor, shop,
        this, "actionPlaceOrder",
        Action.TALK_LONG, "Placing Order"
      ) ;
      return placeOrder ;
    }
    
    if (shop.isManned() && shop.stocks.hasItem(item)) {
      final Action pickup = new Action(
        actor, shop,
        this, "actionPickupItem",
        Action.REACH_DOWN, "Collecting"
      ) ;
      return pickup ;
    }
    return null ;
  }
  
  
  public boolean actionPlaceOrder(Actor actor, Venue shop) {
    order = new Manufacture(null, shop, item.type.materials(), item) ;
    shop.stocks.addSpecialOrder(order) ;
    orderDate = shop.world().currentTime() ;
    return true ;
  }
  
  
  public boolean actionPickupItem(Actor actor, Venue shop) {
    final int price = (int) (shop.priceFor(item.type) * item.amount) ;
    shop.inventory().incCredits(price) ;
    actor.inventory().incCredits(0 - price) ;
    
    shop.inventory().removeMatch(item);
    actor.inventory().addItem(Item.withReference(item, null));
    delivered = true ;
    ///I.say(actor+" picking up: "+item) ;
    return true ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (shop.stocks.hasItem(item)) {
      d.append("Collecting "+item+" at ") ;
      d.append(shop) ;
    }
    else {
      d.append("Placing order for "+item+" at ") ;
      d.append(shop) ;
    }
  }
}






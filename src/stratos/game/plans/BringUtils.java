/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;
import static stratos.game.economic.Owner.*;



public class BringUtils {
  
  
  //  NOTE:  See the rateTrading method below for how these are used...
  private static boolean
    sampleVerbose = false,
    rateVerbose   = false,
    dispVerbose   = false,
    rejectVerbose = false;
  private static Traded verboseGoodType = null;
  private static Class  verboseDestType = null;
  private static Class  verboseOrigType = null;
  
  
  /**  Helper methods for getting suitable distribution targets-
    */
  public static Batch <Venue> nearbyDepots(
    Target t, Stage world, Object... serviceKeys
  ) {
    final Batch <Venue> depots = new Batch <Venue> ();
    world.presences.sampleFromMaps(
      t, world, 5, depots, (Object[]) serviceKeys
    );
    return depots;
  }
  
  
  public static Batch <Venue> nearbyVendors(
    Traded type, Target target, Stage world
  ) {
    final Batch <Venue> vendors = new Batch <Venue> ();
    world.presences.sampleFromMap(target, world, 5, vendors, type.supplyKey);
    return vendors;
  }
  
  
  
  /**  Utility methods for dealing with domestic orders-
    */
  /*
  public static Bringing nextDisposalFor(Actor actor) {
    final boolean report = dispVerbose && I.talkAbout == actor;
    //
    //  We allow the actor to dispose of any 'excess' items in their own
    //  inventory, their home, or their workplace.
    final Owner origins[] = {actor, actor.mind.home(), actor.mind.work()};
    //
    //  Excess items can, in principle, be disposed of anywhere that accepts
    //  them- so we just fall back on whatever the actor can see.
    final Batch <Property> depots = new Batch <Property> ();
    for (Target t : actor.senses.awareOf()) if (t instanceof Property) {
      final Property depot = (Property) t;
      if (openForTrade(depot, actor)) depots.add(depot);
    }
    //
    //  Then iterate over each...
    for (Owner origin : origins) if (openForTrade(origin, actor)) {
      if (report) {
        I.say("\nGetting next item disposal: "+actor);
        I.say("Origin is: "+origin);
      }
      //
      //  We include anything 'unnecesary' from the inventory in question- any
      //  kind of bulk commodity which isn't in demand- as up for disposal.
      final Pick <Bringing> pick = new Pick <Bringing> ();
      final Batch <Item> excess = new Batch <Item> ();
      for (Item i : origin.inventory().allItems()) {
        if (i.type.form != Economy.FORM_MATERIAL) continue;
        if (origin.inventory().canDemand(i.type)) continue;
        if (report) I.say("  Excess item: "+i);
        excess.add(i);
      }
      if (excess.size() == 0) continue;
      //
      //  Then, we iterate over any candidate depots and see if they'll accept
      //  the good in question.  (If the depot is one of the origin points,
      //  only take goods in demand- otherwise, make sure the depot deals in
      //  goods of that type.)
      for (Property depot : depots) for (Item i : excess) {
        final Traded t = i.type;
        final boolean sells = ! Visit.arrayIncludes(origins, depot);
        
        if ((! sells) && depot.inventory().demandFor(t) <= 0       ) continue;
        if (   sells  && ! Visit.arrayIncludes(depot.services(), t)) continue;
        
        final Item moved = Item.withAmount(t, Nums.min(5, i.amount));
        final Bringing d = new Bringing(moved, origin, depot);
        d.setWithPayment(sells ? depot : null);
        
        if (report) {
          I.say("  Candidate depot: "+depot+" ("+i.type+")");
          I.say("  Rating (sells):  "+d.pricePaid()+" ("+sells+")");
        }
        pick.compare(d, d.pricePaid());
      }
      //
      //  Return whatever option seems most profitable (if any)-
      final Bringing disposal = pick.result();
      if (report) I.say("Final choice: "+disposal);
      if (disposal != null) return disposal;
    }
    return null;
  }
  //*/
  
  
  public static Bringing nextPersonalPurchase(
    Actor actor, Venue from, Traded... goods
  ) {
    final boolean report = I.talkAbout == actor && dispVerbose;
    if (report) {
      I.say("\nGetting next purchase for "+actor);
    }
    final boolean freeTrade = goods == null || goods.length == 0;
    if (freeTrade) goods = actor.motives.valuedForTrade();
    Bringing d = fillBulkOrder(from, actor, goods, 1, 5);
    //
    //  In the event that the actor themselves has no immediate purchases, we
    //  consider some for home instead.
    if (d == null && actor.mind.home() instanceof Venue) {
      final Venue home = (Venue) actor.mind.home();
      if (freeTrade) goods = home.stocks.demanded();
      d = fillBulkOrder(from, home, goods, 1, 5);
    }
    //
    //  We don't require that the actor pay for deliveries to self or their own
    //  home.
    if (d == null || Staff.doesBelong(actor, from)) return d;
    else return d.setWithPayment(actor);
  }
  
  
  public static Bringing nextDisposalFor(Actor actor, Owner... allFrom) {
    final Pick <Bringing> pick = new Pick();
    final Batch <Owner> dests = new Batch();
    for (Owner d : allFrom) if (d != null) dests.add(d);
    
    for (Owner from : allFrom) if (from != null) {
      //
      //  
      final Inventory stock = from.inventory();
      final Batch <Traded> excess = new Batch();
      for (Item i : stock.allItems()) {
        if (! stock.canDemand(i.type)) excess.add(i.type);
      }
      if (excess.empty()) return null;
      final Traded goods[] = excess.toArray(Traded.class);
      //
      //  
      Bringing b = BringUtils.bestBulkDeliveryFrom(from, goods, 1, 5, dests);
      if (b == null) b = BringUtils.bestBulkDeliveryFrom(from, goods, 1, 5, 2);
      if (b == null) continue;
      //
      //  
      final Owner goes = b.destination;
      if (! Staff.doesBelong(actor, goes)) b.setWithPayment(goes);
      pick.compare(b, b.pricePaid());
    }
    
    return pick.result();
  }
  
  
  
  /**  Returns the best bulk delivery from the given origin point.  (Note: the
    *  numSamples argument is only used if destinations is null.)  Helper
    *  methods with shorter contracts follow below.
    */
  public static Bringing bestBulkDeliveryFrom(
    Owner orig, Traded goods[], int unit, int amountLimit,
    Batch <? extends Owner> dests, int numSamples
  ) {
    final boolean report = sampleVerbose && I.talkAbout == orig;
    final Stage world = orig.world();
    if (world == null) return null;
    Tally <Owner> ratings = new Tally <Owner> ();
    if (report) {
      I.say("\nGetting bulk delivery for "+orig);
    }
    
    for (Traded good : goods) {
      final Batch <? extends Owner> sampled;
      if (dests != null) sampled = dests;
      else {
        sampled = new Batch <Owner> ();
        world.presences.sampleFromMap(
          orig, world, numSamples, sampled, good.demandKey
        );
      }
      if (report) I.say("  Sample size for "+good+" is "+sampled.size());
      
      final Owner bestDest = bestDestination(orig, sampled, good, unit);
      if (bestDest == null) continue;
      final float rating = rateTrading(orig, bestDest, good, unit, unit);
      ratings.add(rating, bestDest);
    }
    if (ratings.size() == 0) return null;
    
    final Owner destination = ratings.highestValued();
    return fillBulkOrder(orig, destination, goods, unit, amountLimit);
  }
  
  
  public static Bringing bestBulkDeliveryFrom(
    Owner origin, Traded goods[], int baseUnit, int amountLimit,
    Batch <? extends Owner> destinations
  ) {
    return bestBulkDeliveryFrom(
      origin, goods, baseUnit, amountLimit, destinations, -1
    );
  }
  
  
  public static Bringing bestBulkDeliveryFrom(
    Owner origin, Traded goods[], int baseUnit, int amountLimit,
    int numSamples
  ) {
    return bestBulkDeliveryFrom(
      origin, goods, baseUnit, amountLimit, null, numSamples
    );
  }
  
  

  /**  Returns the best bulk delivery to the given destination.  (Note: the
    *  numSamples argument is only used if origins is null.)  Helper methods
    *  with shorter contracts follow below.
    */
  public static Bringing bestBulkCollectionFor(
    Owner dest, Traded goods[], int unit, int amountLimit,
    Batch <? extends Owner> origs, int numSamples
  ) {
    final boolean report = sampleVerbose && I.talkAbout == dest;
    final Stage world = dest.world();
    if (world == null) return null;
    if (report) {
      I.say("\nGetting bulk collection for "+dest);
    }
    
    Tally <Owner> ratings = new Tally <Owner> ();
    for (Traded good : goods) {
      final Batch <? extends Owner> sampled;
      if (origs != null) sampled = origs;
      else {
        sampled = new Batch <Owner> ();
        world.presences.sampleFromMap(
          dest, world, numSamples, sampled, good.supplyKey
        );
      }
      if (report) I.say("  Sample size for "+good+" is "+sampled.size());
      
      final Owner bestOrig = bestOrigin(sampled, dest, good, unit);
      if (bestOrig == null) continue;
      final float rating = rateTrading(bestOrig, dest, good, unit, unit);
      ratings.add(rating, bestOrig);
    }
    if (ratings.size() == 0) return null;
    
    final Owner origin = ratings.highestValued();
    return fillBulkOrder(origin, dest, goods, unit, amountLimit);
  }
  
  
  public static Bringing bestBulkCollectionFor(
    Owner destination, Traded goods[], int baseUnit, int amountLimit,
    Batch <? extends Owner> origins
  ) {
    return bestBulkCollectionFor(
      destination, goods, baseUnit, amountLimit, origins, -1
    );
  }
  
  
  public static Bringing bestBulkCollectionFor(
    Owner destination, Traded goods[], int baseUnit, int amountLimit,
    int numSamples
  ) {
    return bestBulkCollectionFor(
      destination, goods, baseUnit, amountLimit, null, numSamples
    );
  }
  
  
  
  /**  Fills a bulk order of specified good types between the given origin and
    *  destination points.
    */
  public static Bringing fillBulkOrder(
    Owner origin, Owner destination, Traded goods[],
    int baseUnit, int amountLimit
  ) {
    final boolean report = reportRating(origin, destination, null);
    if (report) I.say("\nFilling bulk order");
    //
    //  In essence, we take the single most demanded good at each step, and
    //  increment the size of the order for that by the base unit, until we
    //  run out of either (A) space or (B) demand for any goods.
    int goodAmounts[] = new int[goods.length];
    boolean skip[] = new boolean[goods.length];
    int sumAmounts = 0;
    while (true) {
      int   bestIndex  = -1;
      float bestRating =  0;
      //
      //  We can skip over any provisioned goods, as these are distributed over
      //  the road network, along with any goods that got a negative rating
      //  before.
      for (int i = goods.length; i-- > 0;) {
        if (goods[i].form != FORM_MATERIAL || skip[i]) continue;
        
        final int nextAmount = goodAmounts[i] + baseUnit;
        final float rating = rateTrading(
          origin, destination, goods[i], nextAmount, baseUnit
        );
        if (rating > bestRating) { bestRating = rating; bestIndex = i; }
        else if (rating <= 0) skip[i] = true;
      }
      //
      //  Pick the most promising at each point and increment the order-
      if (bestIndex == -1) break;
      goodAmounts[bestIndex] += baseUnit;
      sumAmounts             += baseUnit;
      if (report) I.say("  "+goods[bestIndex]+" +"+baseUnit);
      if (sumAmounts >= amountLimit) break;
    }
    if (sumAmounts == 0) return null;
    //
    //  Then just convert to a set of items and setup correct payment options:
    final Batch <Item> toTake = new Batch <Item> ();
    for (int i = goods.length; i-- > 0;) {
      final int amount = goodAmounts[i];
      if (amount > 0) toTake.add(Item.withAmount(goods[i], amount));
    }
    if (report) {
      I.say("\nFinal order batch is: "+toTake);
    }
    final Bringing order = new Bringing(toTake, origin, destination);
    return order.setWithPayment(destination);
  }
  
  
  public static Owner bestOrigin(
    Batch <? extends Owner> origs, Owner dest,
    Traded good, int unit
  ) {
    if (origs.size() == 0) return null;
    if (dest.inventory().shortageOf(good) <= 0) return null;
    
    final boolean report = reportRating(null, dest, good);
    if (report) {
      I.say("\nFinding best origin of "+good+" for "+dest);
      I.say("  ("+origs.size()+" available)");
    }
    
    Owner pick = null;
    float bestRating = 0;
    for (Owner orig : origs) if (openForTrade(orig, null)) {
      final float rating = rateTrading(orig, dest, good, unit, unit);
      if (rating > bestRating) { bestRating = rating; pick = orig; }
      if (report) I.say("  Rating for "+orig+" is "+rating);
    }
    
    if (report) I.say("  Final selection: "+pick);
    return pick;
  }
  
  
  public static Owner bestDestination(
    Owner orig, Batch <? extends Owner> dests, Traded good, int unit
  ) {
    if (dests.size() == 0) return null;
    if (orig.inventory().amountOf(good) < unit) return null;
    
    final boolean report = reportRating(orig, null, good);
    if (report) {
      I.say("\nFinding best destination for "+good+" from "+orig);
      I.say("  ("+dests.size()+" available)");
    }
    
    Owner pick = null;
    float bestRating = 0;
    for (Owner dest : dests) if (openForTrade(dest, null)) {
      final float rating = rateTrading(orig, dest, good, unit, unit);
      if (rating > bestRating) { bestRating = rating; pick = dest; }
      if (report) I.say("  Rating for "+dest+" is "+rating);
    }
    
    if (report) I.say("  Final selection: "+pick);
    return pick;
  }
  
  
  static boolean openForTrade(Owner owner, Actor with) {
    if (owner == null) {
      return false;
    }
    if (owner instanceof Property) {
      final Property depot = (Property) owner;
      if (with != null) return depot.openFor(with);
      if (! depot.structure().intact()) return false;
    }
    if (owner.owningTier() <= Owner.TIER_PRIVATE) {
      return false;
    }
    return true;
  }
  
  
  
  /**  Some utility printout methods-
    */
  private static String nameForTier(int tier) {
    return Owner.TIER_NAMES[tier - Owner.TIER_TERRAIN]+" ("+tier+")";
  }
  
  
  private static boolean reportRating(Owner orig, Owner dest, Traded good) {
    if (! rateVerbose) return false;
    //
    //  Supply and demand can quickly get very hairy, so to help in tracking it
    //  we have some moderately elaborate reporting criteria.
    boolean report = (
      (orig != null && I.talkAbout == orig) ||
      (dest != null && I.talkAbout == dest)
    );
    if (verboseGoodType != null && good != null) {
      report &= verboseGoodType == good;
    }
    if (verboseOrigType != null && orig != null) {
      report &= verboseOrigType == orig.getClass();
    }
    if (verboseDestType != null && dest != null) {
      report &= verboseDestType == dest.getClass();
    }
    return report;
  }
  
  
  
  /**  Rates the attractiveness of trading a particular good types between
    *  the given origin and destination venues-
    */
  static float rateTrading(
    Owner orig, Owner dest, Traded good, int amount, int unit
  ) {
    if (orig == dest) return -1;
    final boolean report = reportRating(orig, dest, good);
    if (report) {
      I.say("\nGetting trade rating for "+good+" ("+orig+" -> "+dest+")");
    }
    final float baseFactor = orig.base().relations.relationWith(dest.base());
    if (baseFactor <= 0) {
      if (report) I.say("  Base relations negative.");
      return -1;
    }
    //
    //  First of all secure some preliminary variables.
    //  OS/DS == origin/destination stocks
    //  OA/DA == origin/destination amount
    //  OT/DT == origin/destination tier
    //  OD/DD == origin/destination demand
    final Inventory
      OS = orig.inventory(),
      DS = dest.inventory();
    final float
      OA = OS.amountOf(good),
      DA = DS.amountOf(good);
    if (OA < amount) {
      if (report) I.say("  Origin lacks supply.");
      return -1;
    }
    
    final int
      OT = OS.owner.owningTier(),
      DT = DS.owner.owningTier();
    final boolean
      OP = OS.producer(good),
      DP = DS.producer(good);
    
    if (report) {
      I.say("  Checking tiers...");
      I.say("  Origin tier:      "+nameForTier(OT)+", Exporter: "+OP);
      I.say("  Destination tier: "+nameForTier(DT)+", Exporter: "+DP);
    }
    if (! canTradeBetween(OT, OP, DT, DP)) return -1;
    final float
      OD = OS.demandFor(good),
      DD = DS.demandFor(good);
    if (DA >= DD) {
      if (report) I.say("  No shortage at destination.");
      return -1;
    }
    //
    //  Secondly, obtain an estimate of stocks before and after the exchange.
    final boolean
      isTrade    = OT == DT && DP == OP,
      isConsumer = DP == false && DT < Owner.TIER_SHIPPING;
    final float
      OFB = futureBalance(orig, good, report),
      DFB = futureBalance(dest, good, report);
    float origAfter = 0, destAfter = 0;
    origAfter = OA + (OFB - amount);
    destAfter = DA + (DFB + amount);
    if (report) {
      I.say("  Trade unit is "+amount);
      I.say("  Origin      reserved: "+OFB);
      I.say("  Destination reserved: "+DFB);
      I.say("  Origin      demand  : "+OD+" ("+nameForTier(OT)+")");
      I.say("  Destination demand  : "+DD+" ("+nameForTier(DT)+")");
      I.say("  Origin      after   : "+origAfter);
      I.say("  Destination after   : "+destAfter);
    }
    if (origAfter < 0 || destAfter >= (DD + unit)) return -1;
    //
    //  Then, assign ratings for relative shortages at the start/end points (in
    //  the case of symmetric trades, based on projected stocks afterwards.)
    float origShort = 0, destShort = 0, rating = 0;
    if (! isTrade) { origAfter += amount; destAfter -= amount; }
    if (OD > 0) origShort = 1 - (origAfter / OD);
    if (DD > 0) destShort = 1 - (destAfter / DD);
    //
    //  In the case of an equal trade, rate based on those relative shortages.
    //  Otherwise favour deliveries to local consumers or, finally, deliver for
    //  export.
    if (isTrade) {
      rating = destShort - origShort;
    }
    else if (isConsumer) {
      rating = 1 + destShort;
    }
    else {
      rating = destShort / 2;
    }
    if (report) {
      I.say("  Origin      shortage: "+origShort);
      I.say("  Destination shortage: "+destShort);
      I.say("  Rating so far       : "+rating   );
    }
    if (rating <= 0) return -1;
    //
    //  Then return an estimate of how much an exchange would equalise things,
    //  along with penalties for distance, tier-gap and base relations:
    final int SS = Stage.ZONE_SIZE;
    final float
      distFactor  = SS / (SS + Spacing.distance(orig, dest)),
      tierFactor  = Nums.max(1, Nums.abs(OT - DT)),
      priceDiff   = dest.priceFor(good, false) - orig.priceFor(good, true),
      priceFactor = Nums.clamp(1 + (priceDiff / good.defaultPrice()), 0.5f, 2);
    if (report) {
      I.say("  Final rating "+rating);
      I.say("  base/distance factors: "+baseFactor +"/"+distFactor);
      I.say("  price/tier factors:    "+priceFactor+"/"+tierFactor);
    }
    return rating * distFactor * baseFactor * priceFactor / tierFactor;
  }
  
  
  public static boolean canTradeBetween(
    int origTier, boolean origProduce,
    int destTier, boolean destProduce
  ) {
    //
    //  Trade exchanges are always okay with lower-tier owners as long as they
    //  produce/consume appropriately.
    if (destTier == TIER_TRADER && origTier < TIER_TRADER) {
      return origProduce;
    }
    if (origTier == TIER_TRADER && destTier < TIER_TRADER) {
      return ! destProduce;
    }
    //  Private trades (okay as long as the recipient is a consumer)
    if (destTier <= TIER_PRIVATE) {
      if (destProduce == true) return false;
    }
    //  Same-tier trades (illegal between trade vessels or same demands)
    else if (origTier == destTier) {
      if (origTier == TIER_SHIPPING) return false;
      if (origProduce == destProduce) return false;
    }
    //  Downstream deliveries (from ships to depots to facilities)
    else if (origTier > destTier) {
      if (origProduce == true  || destProduce == true ) return false;
    }
    //  Upstream delivieries (from facilities to ships to depots)
    else if (origTier < destTier) {
      if (origProduce == false || destProduce == false) return false;
    }
    return true;
  }
  
  
  static float futureBalance(
    Owner e, Traded good, boolean report
  ) {
    final Series <Bringing> reserved = e.inventory().reservations();
    if (reserved == null || reserved.size() == 0) return 0;
    if (report) I.say("  "+reserved.size()+" reservations at "+e);
    
    float balance = 0;
    for (Bringing d : reserved) {
      if (! d.isActive()) continue;
      final Item itemMatch = d.delivered(good);
      if (itemMatch == null) continue;
      if (report) {
        I.say("    Delivery is: "+d);
        I.say("    Actor is:    "+d.actor());
        I.say("    Match is:    "+itemMatch);
        I.say("    Stage is:    "+d.stage());
      }
      
      if (d.origin == e && d.stage() <= Bringing.STAGE_PICKUP) {
        balance += itemMatch.amount;
      }
      
      if (d.destination == e && d.stage() < Bringing.STAGE_RETURN) {
        balance += itemMatch.amount;
      }
    }
    
    return balance;
  }
}




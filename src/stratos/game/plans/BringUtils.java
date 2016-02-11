/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.verse.Faction;
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
  public static Bringing nextPersonalPurchase(
    Actor actor, Venue from, Traded... goods
  ) {
    final boolean report = I.talkAbout == actor && dispVerbose;
    if (report) {
      I.say("\nGetting next purchase for "+actor);
    }
    final boolean freeTrade = goods == null || goods.length == 0;
    if (freeTrade) goods = actor.motives.valuedForTrade();
    Bringing d = fillBulkOrder(from, actor, goods, 1, 5, false);
    //
    //  In the event that the actor themselves has no immediate purchases, we
    //  consider some for home instead.
    if (d == null && actor.mind.home() instanceof Venue) {
      final Venue home = (Venue) actor.mind.home();
      if (freeTrade) goods = home.stocks.demanded();
      d = fillBulkOrder(from, home, goods, 1, 5, false);
    }
    //
    //  We require that the actor pay for deliveries to self or their own home,
    //  but not from their workplace.
    if (d == null || actor.mind.work() == from) return d;
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
      if (excess.empty()) continue;
      final Traded goods[] = excess.toArray(Traded.class);
      //
      //  
      Bringing b       = bestBulkDeliveryFrom(from, goods, 1, 5, dests, false);
      if (b == null) b = bestBulkDeliveryFrom(from, goods, 1, 5, 2, false);
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
    Batch <? extends Owner> dests, int numSamples, boolean destPays
  ) {
    final boolean report = I.talkAbout == orig && sampleVerbose;
    final Stage world = orig.world();
    if (world == null) return null;
    Tally <Owner> ratings = new Tally <Owner> ();
    if (report) {
      I.say("\nGetting bulk delivery for "+orig);
    }
    
    for (Traded good : goods) {
      final Batch <? extends Owner> sampled;
      final boolean filter = dests == null;
      if (dests != null) sampled = dests;
      else {
        sampled = new Batch <Owner> ();
        world.presences.sampleFromMap(
          orig, world, numSamples, sampled, good.demandKey
        );
      }
      if (report) I.say("  Sample size for "+good+" is "+sampled.size());
      
      final Owner bestDest = bestDestination(orig, sampled, good, unit, filter);
      if (bestDest == null) continue;
      final float rating = rateTrading(orig, bestDest, good, unit, unit);
      ratings.add(rating, bestDest);
    }
    if (ratings.size() == 0) return null;
    
    final Owner destination = ratings.highestValued();
    return fillBulkOrder(orig, destination, goods, unit, amountLimit, destPays);
  }
  
  
  public static Bringing bestBulkDeliveryFrom(
    Owner origin, Traded goods[], int baseUnit, int amountLimit,
    Batch <? extends Owner> destinations, boolean destPays
  ) {
    return bestBulkDeliveryFrom(
      origin, goods, baseUnit, amountLimit, destinations, -1, destPays
    );
  }
  
  
  public static Bringing bestBulkDeliveryFrom(
    Owner origin, Traded goods[], int baseUnit, int amountLimit,
    int numSamples, boolean destPays
  ) {
    return bestBulkDeliveryFrom(
      origin, goods, baseUnit, amountLimit, null, numSamples, destPays
    );
  }
  
  

  /**  Returns the best bulk delivery to the given destination.  (Note: the
    *  numSamples argument is only used if origins is null.)  Helper methods
    *  with shorter contracts follow below.
    */
  public static Bringing bestBulkCollectionFor(
    Owner dest, Traded goods[], int unit, int amountLimit,
    Batch <? extends Owner> origs, int numSamples, boolean destPays
  ) {
    final boolean report = I.talkAbout == dest && sampleVerbose;
    final Stage world = dest.world();
    if (world == null) return null;
    if (report) {
      I.say("\nGetting bulk collection for "+dest);
    }
    
    Tally <Owner> ratings = new Tally <Owner> ();
    for (Traded good : goods) {
      final Batch <? extends Owner> sampled;
      final boolean filter = origs == null;
      
      if (origs != null) sampled = origs;
      else {
        sampled = new Batch <Owner> ();
        world.presences.sampleFromMap(
          dest, world, numSamples, sampled, good.supplyKey
        );
      }
      if (report) I.say("  Sample size for "+good+" is "+sampled.size());
      
      final Owner bestOrig = bestOrigin(sampled, dest, good, unit, filter);
      if (bestOrig == null) continue;
      final float rating = rateTrading(bestOrig, dest, good, unit, unit);
      ratings.add(rating, bestOrig);
    }
    if (ratings.size() == 0) return null;
    
    final Owner origin = ratings.highestValued();
    return fillBulkOrder(origin, dest, goods, unit, amountLimit, destPays);
  }
  
  
  public static Bringing bestBulkCollectionFor(
    Owner destination, Traded goods[], int baseUnit, int amountLimit,
    Batch <? extends Owner> origins, boolean destPays
  ) {
    return bestBulkCollectionFor(
      destination, goods, baseUnit, amountLimit, origins, -1, destPays
    );
  }
  
  
  public static Bringing bestBulkCollectionFor(
    Owner destination, Traded goods[], int baseUnit, int amountLimit,
    int numSamples, boolean destPays
  ) {
    return bestBulkCollectionFor(
      destination, goods, baseUnit, amountLimit, null, numSamples, destPays
    );
  }
  
  
  
  /**  Fills a bulk order of specified good types between the given origin and
    *  destination points.
    */
  public static Bringing fillBulkOrder(
    Owner origin, Owner destination, Traded goods[],
    int baseUnit, int amountLimit, boolean destPays
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
    return destPays ? order.setWithPayment(destination) : order;
  }
  
  
  public static Owner bestOrigin(
    Batch <? extends Owner> origs, Owner dest,
    Traded good, int unit, boolean filterOrigs
  ) {
    if (origs.size() == 0) return null;
    if (dest.inventory().relativeShortage(good, true) < 0) return null;
    
    final boolean report = reportRating(null, dest, good);
    if (report) {
      I.say("\nFinding best origin of "+good+" for "+dest);
      I.say("  ("+origs.size()+" available)");
    }
    
    Owner pick = null;
    float bestRating = 0;
    for (Owner orig : origs) if ((! filterOrigs) || openForTrade(orig, null)) {
      final float rating = rateTrading(orig, dest, good, unit, unit);
      if (rating > bestRating) { bestRating = rating; pick = orig; }
      if (report) I.say("  Rating for "+orig+" is "+rating);
    }
    
    if (report) I.say("  Final selection: "+pick);
    return pick;
  }
  
  
  public static Owner bestDestination(
    Owner orig, Batch <? extends Owner> dests,
    Traded good, int unit, boolean filterDests
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
    for (Owner dest : dests) if ((! filterDests) || openForTrade(dest, null)) {
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
    final float baseFactor = Faction.factionRelation(orig, dest);
    if (baseFactor <= 0) {
      if (report) I.say("  Base relations negative.");
      return -1;
    }
    //
    //  First of all secure some preliminary variables.
    //  OS/DS == origin/destination stocks
    //  OA/DA == origin/destination amount
    //  OT/DT == origin/destination tier
    //  OF/DF == origin/destination shortage
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
    final float
      OF = OS.relativeShortage(good, false),
      DF = DS.relativeShortage(good, false);
    
    if (report) {
      I.say("  Checking tiers...");
      I.say("  Origin tier:      "+nameForTier(OT)+", Shortage: "+OF);
      I.say("  Destination tier: "+nameForTier(DT)+", Shortage: "+DF);
    }
    if (! canTradeBetween(OT, OF, DT, DF)) return -1;
    //
    //  Secondly, obtain an estimate of stocks before and after the exchange.
    final float
      OFB = futureBalance(orig, good, report),
      DFB = futureBalance(dest, good, report)
    ;
    float origAfter = 0, destAfter = 0;
    origAfter = OA + (OFB - amount);
    destAfter = DA + (DFB + amount);
    if (report) {
      I.say("  Trade unit is "+amount);
      I.say("  Origin      reserved: "+OFB);
      I.say("  Destination reserved: "+DFB);
      I.say("  Origin      shortage: "+OF+" ("+nameForTier(OT)+")");
      I.say("  Destination shortage: "+DF+" ("+nameForTier(DT)+")");
      I.say("  Origin      after   : "+origAfter);
      I.say("  Destination after   : "+destAfter);
    }
    //
    //  We set the min/max amount thresholds to either 0, consumption, or total
    //  demand, depending on circumstances...
    final boolean downTier = DT < OT;
    float minAtOrig = downTier ? 0 : OS.consumption(good);
    float maxAtDest = DS.consumption(good) + DS.production(good);
    if (origAfter < minAtOrig || destAfter >= (maxAtDest + unit)) return -1;
    //
    //  In the case of an equal trade, rate based on those relative shortages.
    //  Otherwise favour deliveries to local consumers or, finally, deliver for
    //  export.
    final boolean
      isTrade    = OT == DT,
      isConsumer = DF > OF && DT < Owner.TIER_SHIPPING;
    
    float rating;
    if (isTrade) {
      rating = (DF - OF) * 2;
    }
    else if (isConsumer) {
      rating = 1 + DF;
    }
    else {
      rating = DF / 2;
    }
    if (report) {
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
    int origTier, float origShortage,
    int destTier, float destShortage
  ) {
    //
    //  As a rule, both shortages and surpluses at lower-tier venues will trump
    //  those at higher-tier venues.
    final boolean
      origNeed = origShortage > 0,
      destNeed = destShortage > 0,
      sameTier = origTier == destTier;
    
    if ((sameTier && origTier == TIER_SHIPPING) || (destShortage <= -1)) {
      return false;
    }
    if (origNeed) {
      if (destTier < origTier && destNeed) return true;
      else return false;
    }
    if (destNeed) {
      if (destTier > origTier && origNeed) return false;
      else return true;
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




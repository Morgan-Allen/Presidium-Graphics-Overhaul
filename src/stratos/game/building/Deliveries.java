/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


//
//  TODO:  This still suffers from some imbalances in the case of reciprocal
//         trade.


package stratos.game.building ;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.building.Inventory.Owner;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.util.*;



/**  This class implements a big bunch of helper methods to search for
  *  optimal delivery venues and amounts-
  */
public class Deliveries implements Economy {
  
  
  final static int
    IS_TRADE    = 0,
    IS_SHOPPING = 1,
    IS_IMPORT   = 2,
    IS_EXPORT   = 3 ;
  
  private static boolean
    collectVerbose = false,
    deliverVerbose = false,
    shopsVerbose   = false;
  
  
  public static Delivery nextDeliveryFor(
    Actor actor, Owner origin, Service goods[],
    int sizeLimit, World world
  ) {
    return Deliveries.bestClient(
      actor, goods, origin,
      (Batch) nearbyCustomers(goods, origin, world),
      sizeLimit, IS_TRADE
    ) ;
  }
  

  public static Delivery nextDeliveryFor(
    Actor actor, Owner origin, Service goods[],
    Batch <Venue> clients, int sizeLimit,
    World world
  ) {
    return Deliveries.bestClient(
      actor, goods, origin,
      (Batch) clients,
      sizeLimit, IS_TRADE
    ) ;
  }
  
  
  public static Delivery nextCollectionFor(
    Actor actor, Owner client, Service goods[],
    int sizeLimit, Actor pays, World world
  ) {
    return Deliveries.bestOrigin(
      actor, goods, client,
      (Batch) nearbyVendors(goods, client, world),
      sizeLimit, pays == null ? IS_TRADE : IS_SHOPPING
    ) ;
  }
  
  
  public static Delivery nextCollectionFor(
    Actor actor, Owner client, Service goods[], Batch <Venue> vendors,
    int sizeLimit, Actor pays, World world
  ) {
    return Deliveries.bestOrigin(
      actor, goods, client,
      (Batch) vendors,
      sizeLimit, pays == null ? IS_TRADE : IS_SHOPPING
    ) ;
  }
  
  
  public static Delivery nextImportDelivery(
    Actor actor, Owner origin, Service goods[], Batch <Venue> clients,
    int sizeLimit, World world
  ) {
    return Deliveries.bestClient(
      actor, goods, origin,
      (Batch) clients,
      sizeLimit, IS_IMPORT
    ) ;
  }
  
  
  public static Delivery nextExportCollection(
    Actor actor, Owner client, Service goods[], Batch <Venue> vendors,
    int sizeLimit, World world
  ) {
    final Delivery d = Deliveries.bestOrigin(
      actor, goods, client,
      (Batch) vendors,
      sizeLimit, IS_EXPORT
    ) ;
    I.sayAbout(actor, "Getting next export collection...") ;
    if (d != null) I.say("Should pay: "+d.shouldPay) ;
    return d ;
  }
  
  
  private static Delivery bestOrigin(
    Actor actor, Service goods[], Owner client,
    Batch <Owner> origins, int sizeLimit, int tradeType
  ) {
    final boolean report = collectVerbose && I.talkAbout == client;
    
    Delivery picked = null ;
    float bestRating = 0 ;
    for (Owner origin : origins) {
      Item order[] = Deliveries.configDelivery(
        goods, origin, client, actor,
        sizeLimit, actor.world(), tradeType
      ) ;
      if (report) I.say("Order length: "+order.length) ;
      if (order.length == 0) continue ;
      final Delivery d = new Delivery(order, origin, client) ;
      final float rating = d.priorityFor(actor) ;
      if (report) I.say("Rating: "+rating) ;
      if (rating > bestRating) { bestRating = rating ; picked = d ; }
    }
    if (picked != null) picked.shouldPay = picked.destination ;
    return picked ;
  }
  
  
  private static Delivery bestClient(
    Actor actor, Service goods[], Owner origin,
    Batch <Owner> clients, int sizeLimit, int tradeType
  ) {
    Delivery picked = null ;
    float bestRating = 0 ;
    for (Owner client : clients) {
      Item order[] = Deliveries.configDelivery(
        goods, origin, client, actor,
        sizeLimit, actor.world(), tradeType
      ) ;
      final Delivery d = new Delivery(order, origin, client) ;
      final float rating = d.priorityFor(actor) ;
      if (rating > bestRating) { bestRating = rating ; picked = d ; }
    }
    if (picked != null) picked.shouldPay = picked.destination ;
    return picked ;
  }
  
  

  /**  Helper methods for squeezing orders down into manageable chunks-
    */
  private static float rateTrading(
    Service good, Owner origin, Owner client, int tradeType, boolean report
  ) {
    //
    //  The basic purpose of this comparison is to ensure that deliveries are
    //  non-symmetric.
    if (tradeType == IS_SHOPPING) {
      final float rating = origin.inventory().amountOf(good) / 10f ;
      return rating ;
    }
    else if (tradeType == IS_IMPORT) {
      return Math.min(
        ((Service.Trade) client).importShortage(good),
        origin.inventory().amountOf(good)
      ) / 10f ;//capacity ;
    }
    if (tradeType == IS_EXPORT) {
      final int capacity = client.spaceFor(good) ;
      return Math.min(
        ((Service.Trade) origin).exportSurplus(good),
        capacity
      ) / 10f ;//capacity ;
    }
    
    if (! (origin instanceof Venue)) return 2 ;
    if (! (client instanceof Venue)) return 2 ;
    final Venue
      vO = (Venue) origin,
      vC = (Venue) client ;
    final int
      tO = vO.stocks.demandTier(good),
      tC = vC.stocks.demandTier(good) ;

    if (report) {
      I.say("Origin/client tiers for "+good+" "+tO+"/"+tC) ;
    }
    
    if (tO == VenueStocks.TIER_CONSUMER) return 0 ;
    if (tC == VenueStocks.TIER_PRODUCER) return 0 ;
    if (tC > tO) return 2 ;
    
    final float
      originUrgency = vO.stocks.shortageUrgency(good),
      clientUrgency = vC.stocks.shortageUrgency(good) ;
    
    if (clientUrgency <= originUrgency) return 0 ;
    float rating = 1 ;
    
    //
    //  TODO:  This is where you'll need to do some hard math to ensure that
    //  deliveries don't unbalance supply.
    
    rating *= origin.inventory().amountOf(good) ;
    rating /= 5 + ((Venue) client).stocks.shortageOf(good) ;
    
    if (report && rating > 0.1f) {
    }
    //
    //  TODO:  Make sure the client inventory has space!
    return rating ;
  }
  
  
  //  TODO:  This all seems mightily computationally expensive.  Any way to cut
  //  down on all this?
  
  private static Item[] configDelivery(
    Service goods[], Owner origin, Owner client,
    Actor pays, int sizeLimit, World world,
    int tradeType
  ) {
    //
    //  First, get the amount of each item available for trade at the point of
    //  origin, and desired by the destination/client, which constrains the
    //  quantities involved-
    final boolean report =
      (collectVerbose && I.talkAbout == client) ||
      (deliverVerbose && I.talkAbout == origin) ||
      (shopsVerbose   && I.talkAbout == pays  );
    
    final int
      roundUnit = sizeLimit < 10 ? (sizeLimit <= 5 ? 1 : 2) : 5,
      pickUnit  = sizeLimit < 10 ? (sizeLimit <= 5 ? 0 : 2) : 5 ;
    if (report) I.say(
      "Evaluating delivery between "+origin+" and "+client+
      "\nTotal goods: "+goods.length+
      ", pick/round unit: "+pickUnit+"/"+roundUnit
    );
    
    final List <Item> viable = new List <Item> () {
      protected float queuePriority(Item i) {
        return (1f + i.type.basePrice) / (i.amount + 1) ;
      }
    } ;
    final Batch <Behaviour>
      OD = world.activities.targeting(origin),
      CD = world.activities.targeting(client) ;
    float maxSold, maxBuys ;
    //
    //  In the process, we deduct the sum of goods already due to be delivered/
    //  taken away.
    for (Service good : goods) if (good.form == FORM_COMMODITY) {
      if (tradeType == IS_IMPORT) {
        maxBuys = ((Service.Trade) client).importShortage(good) ;
        maxSold = maxBuys ;
      }
      else if (tradeType == IS_EXPORT) {
        maxSold = ((Service.Trade) origin).exportSurplus(good) ;
        maxBuys = maxSold ;
      }
      else {
        maxSold = origin.inventory().amountOf(good) ;
        maxBuys = ((Venue) client).stocks.shortageOf(good) ;
      }
      if (maxSold == 0 || maxBuys == 0) continue;
      maxSold -= reservedForCollection(OD, good) ;
      maxBuys -= reservedForCollection(CD, good) ;
      final float amount = Math.min(maxSold, maxBuys) ;
      
      final float rateTrade = tradeType == IS_TRADE ? Deliveries.rateTrading(
        good, origin, client, tradeType, report
      ) : 2 ;
      
      if ((amount * rateTrade) < pickUnit) {
        if (report) {
          I.say("  Max bought/sold: "+maxBuys+"/"+maxSold);
          I.say("  Trade limit/rating: "+amount+"/"+rateTrade);
        }
        continue ;
      }
      if (report) {
        I.say("  Service: "+good) ;
        if (client instanceof Venue) {
          final Venue VC = (Venue) client ;
          I.say("  Buys urgency: "+VC.stocks.shortageUrgency(good)) ;
        }
        if (origin instanceof Venue) {
          final Venue VO = (Venue) origin ;
          I.say("  Sells urgency: "+VO.stocks.shortageUrgency(good)) ;
        }
        I.say("    Available: "+origin.inventory().amountOf(good)) ;
        I.say("    Reserved: "+reservedForCollection(OD, good)) ;
        I.say("    Max buys/sold: "+maxBuys+"/"+maxSold) ;
        I.say("    Trade rating is: "+rateTrade) ;
        I.say("    Trade amount is: "+(amount * rateTrade)) ;
      }
      
      if (rateTrade >= 1) {
        viable.queueAdd(Item.withAmount(good, amount)) ;
        continue ;
      }
      viable.queueAdd(Item.withAmount(good, amount * rateTrade)) ;
    }
    //
    //  We then compress the quantities of items to fit within prescribed size
    //  and price limits-
    final int amounts[] = new int[viable.size()] ;
    float sumAmounts = 0, sumPrice = 0, scale = 1 ;
    for (Item i : viable) {
      sumAmounts += i.amount ;
      final float price = Delivery.purchasePrice(i, pays, origin) ;
      if (price <= 0) continue ;
      sumPrice += price ;
    }
    if (sumAmounts == 0) {
      if (report) I.say("  Nothing to deliver!") ;
      return new Item[0] ;
    }
    
    if (sumAmounts > sizeLimit) {
      scale = sizeLimit / sumAmounts ;
      sumPrice *= scale ;
    }
    final float priceLimit = tradeType != IS_SHOPPING ?
      Float.POSITIVE_INFINITY :
      pays.gear.credits() / 2f ;
    if (sumPrice > priceLimit) {
      scale *= priceLimit / sumPrice ;
    }
    
    if (report) {
      I.say("Size/price limits: "+sizeLimit+" "+priceLimit+", goods:") ;
      for (Item v : viable) I.say("  "+v) ;
    }
    //
    //  In so doing, however, we must round up to the nearest order-unit...
    int i = 0 ;
    sumAmounts = 0 ;
    sumPrice = 0 ;
    for (Item v : viable) {
      amounts[i] = roundUnit * (int) Math.ceil(v.amount * scale / roundUnit) ;
      sumAmounts += amounts[i] ;
      sumPrice += amounts[i] * origin.priceFor(v.type) ;
      i++ ;
    }
    //
    //  ...which then necessitates trimming off possible excess-
    if (viable.size() != 0) while (true) {
      boolean noneTrimmed = true ;
      float price ;
      i = 0 ; for (Item v : viable) {
        price = Math.max(0, Delivery.purchasePrice(v, pays, origin)) ;
        final boolean mustTrim =
          sumAmounts > sizeLimit ||
          (sumPrice > priceLimit && price > 0) ||
          origin.inventory().amountOf(v) < amounts[i] ;
        if (amounts[i] > 0 && mustTrim) {
          amounts[i] -= roundUnit ;
          sumAmounts -= roundUnit ;
          sumPrice -= roundUnit * price / v.amount ;
          noneTrimmed = false ;
        }
        i++ ;
      }
      if (noneTrimmed) break ;
    }
    
    if (report) {
      I.say("AFTER TRIM") ;
      i = 0 ;
      for (Item v : viable) {
        I.say("  "+v.type+" "+amounts[i++]) ;
        //if (v.quality == -1) I.say(v+"  HAD NO QUALITY") ;
      }
    }
    
    //
    //  Finally, we compile and return the quantities as items:
    final Batch <Item> trimmed = new Batch <Item> () ;
    i = viable.size() ;
    for (ListEntry <Item> LE = viable ; (LE = LE.lastEntry()) != viable ;) {
      final int amount = amounts[--i] ;
      if (amount > 0) trimmed.add(Item.withAmount(LE.refers, amount)) ;
    }
    return trimmed.toArray(Item.class) ;
  }
  
  
  private static float reservedForCollection(
    Batch <Behaviour> doing, Service goodType
  ) {
    float sum = 0 ;
    for (Behaviour b : doing) {
      if (b instanceof Delivery) for (Item i : ((Delivery) b).items) {
        if (i.type == goodType) sum += i.amount ;
      }
    }
    return sum ;
  }
  
  
  
  /**  Helper methods for getting viable targets-
    */
  public static Batch <Venue> nearbyDepots(Target t, World world) {
    final Batch <Venue> depots = new Batch <Venue> () ;
    world.presences.sampleFromMaps(
      t, world, 5, depots,
      SupplyDepot.class, StockExchange.class
    ) ;
    return depots ;
  }
  
  
  public static Batch <Venue> nearbyCustomers(
    Service types[], Target target, World world
  ) {
    final boolean report = deliverVerbose && I.talkAbout == target;
    
    final Batch <Venue> sampled = new Batch <Venue> () ;
    final String keys[] = new String[types.length];
    for (int i = types.length; i-- > 0;) keys[i] = types[i].demandKey;
    
    world.presences.sampleFromMaps(
      target, world, 5, sampled, (Object[]) keys
    ) ;
    
    if (report) {
      I.say("\nGoods types are: ");
      for (Service t : types) {
        final int pop = world.presences.mapFor(t.demandKey).population();
        if (pop == 0) continue;
        I.say("  "+t+" (population "+pop+")");
      }
      if (sampled.size() > 0) {
        I.say("Customers sampled are: ");
        for (Venue v : sampled) I.say("  "+v);
      }
    }
    
    final Batch <Venue> returned = new Batch <Venue> () ;
    for (Venue v : sampled) {
      if (v.privateProperty()) continue ;
      if ((v instanceof SupplyDepot) || (v instanceof StockExchange)) continue ;
      returned.add(v) ;
    }
    return returned ;
  }
  
  
  public static Batch <Vehicle> nearbyTraders(Target target, World world) {
    final Batch <Vehicle> nearby = new Batch <Vehicle> () ;
    world.presences.sampleFromMap(target, world, 10, nearby, Dropship.class) ;
    return nearby ;
  }
  
  
  public static Batch <Venue> nearbyVendors(
    Service type, Target target, World world
  ) {
    final Batch <Venue> vendors = new Batch <Venue> () ;
    world.presences.sampleFromMap(target, world, 5, vendors, type) ;
    return vendors ;
  }
  
  
  public static Batch <Venue> nearbyVendors(
    Service types[], Target target, World world
  ) {
    final Batch <Venue> vendors = new Batch <Venue> () ;
    world.presences.sampleFromMaps(target, world, 5, vendors, (Object[]) types) ;
    return vendors ;
  }
}










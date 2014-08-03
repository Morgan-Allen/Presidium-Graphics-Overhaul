


package stratos.game.campaign;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.building.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.util.*;

import static stratos.game.building.Economy.*;


//  TODO:  This will have to be merged with (or rendered obsolete by) the more
//  generalised, and powerful, supply-and-demand algorithms I need to work on.


public class Commerce {
  
  
  /**  Fields definitions, constructor, save/load methods-
    */
  final public static float
    SUPPLY_INTERVAL = World.STANDARD_DAY_LENGTH / 2f,
    SUPPLY_DURATION = SUPPLY_INTERVAL / 2f,
    
    APPLY_INTERVAL  = World.STANDARD_DAY_LENGTH / 2f,
    UPDATE_INTERVAL = 10,
    DEMAND_INC      = 0.15f,
    MAX_APPLICANTS  = 3;
  
  private static boolean verbose = false;
  
  
  final Base base;
  Sector homeworld;
  final List <Sector> partners = new List <Sector> ();
  
  //  TODO:  Use a table here instead.  Just as a temporary kluge.
  //final static int NUM_J = Backgrounds.ALL_BACKGROUNDS.length;
  /*
  final float
    jobSupply[] = new float[NUM_J],
    jobDemand[] = new float[NUM_J];
  //*/
  //final Table <Background, Float>
    //jobSupply = new Table(),
    //jobDemand = new Table();
  final Tally <Background>
    jobSupply = new Tally(),
    jobDemand = new Tally();
  
  final List <Actor> candidates = new List <Actor> ();
  final List <Actor> migrantsIn = new List <Actor> ();
  
  final Inventory
    shortages = new Inventory(null),
    surpluses = new Inventory(null);
  final Table <TradeType, Float>
    importPrices = new Table <TradeType, Float> (),
    exportPrices = new Table <TradeType, Float> ();
  
  private Dropship ship;
  private float nextVisitTime;
  
  
  
  public Commerce(Base base) {
    this.base = base;
    for (TradeType type : ALL_MATERIALS) {
      importPrices.put(type, (float) type.basePrice);
      exportPrices.put(type, (float) type.basePrice);
    }
  }
  
  
  public void loadState(Session s) throws Exception {
    
    homeworld = (Sector) s.loadObject();
    for (int n = s.loadInt(); n-- > 0;) {
      partners.add((Sector) s.loadObject());
    }
    
    for (int n = s.loadInt(); n-- > 0;) {
      jobSupply.set((Background) s.loadObject(), s.loadFloat());
    }
    for (int n = s.loadInt(); n-- > 0;) {
      jobDemand.set((Background) s.loadObject(), s.loadFloat());
    }
    
    shortages.loadState(s);
    surpluses.loadState(s);
    for (TradeType type : ALL_MATERIALS) {
      importPrices.put(type, s.loadFloat());
      exportPrices.put(type, s.loadFloat());
    }
    
    s.loadObjects(candidates);
    s.loadObjects(migrantsIn);
    ship = (Dropship) s.loadObject();
    nextVisitTime = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObject(homeworld);
    s.saveInt(partners.size());
    for (Sector p : partners) s.saveObject(p);
    
    s.saveInt(jobSupply.size());
    for (Background b : jobSupply.keys()) {
      s.saveObject(b);
      s.saveFloat(jobSupply.valueFor(b));
    }
    s.saveInt(jobDemand.size());
    for (Background b : jobDemand.keys()) {
      s.saveObject(b);
      s.saveFloat(jobDemand.valueFor(b));
    }
    
    shortages.saveState(s);
    surpluses.saveState(s);
    for (TradeType type : ALL_MATERIALS) {
      s.saveFloat(importPrices.get(type));
      s.saveFloat(exportPrices.get(type));
    }
    
    s.saveObjects(candidates);
    s.saveObjects(migrantsIn);
    s.saveObject(ship);
    s.saveFloat(nextVisitTime);
  }
  
  
  public void assignHomeworld(Sector s) {
    homeworld = s;
    togglePartner(s, true);
  }
  
  
  public Sector homeworld() {
    return homeworld;
  }
  
  
  public void togglePartner(Sector s, boolean is) {
    if (is) {
      partners.include(s);
    }
    else {
      partners.remove(s);
      if (s == homeworld) homeworld = null;
    }
  }
  
  
  public List <Sector> partners() {
    return partners;
  }
  
  
  
  /**  Dealing with migrants and cargo-
    */
  public void addImmigrant(Actor actor) {
    migrantsIn.add(actor);
  }
  
  
  protected void updateCandidates(int numUpdates) {
    if ((numUpdates % UPDATE_INTERVAL) != 0) return;
    
    final float inc = DEMAND_INC, timeGone = UPDATE_INTERVAL / APPLY_INTERVAL;
    final Background demanded[] = jobDemand.keysToArray(Background.class);
    
    for (Background b : demanded) {
      float demand = jobDemand.valueFor(b);
      demand = Math.max((demand * (1 - inc)) - (inc / 100), 0);
      jobDemand.set(b, demand);
    }
    
    jobSupply.clear();
    for (Actor c : candidates) {
      final Application a = c.mind.application();
      if (a == null) continue;
      jobSupply.add(1, a.position);
    }
    
    if (verbose) I.say("\nChecking for new candidates...");
    
    for (Background b : demanded) {
      final float
        demand = jobDemand.valueFor(b),
        supply = jobSupply.valueFor(b);
      if (demand == 0) {
        jobDemand.set(b, 0);
        continue;
      }
      
      float applyChance = demand * demand / (supply + demand);
      applyChance *= timeGone;
      
      if (verbose) {
        I.say("  Hire chance for "+b+" is "+applyChance);
        I.say("  Supply/demand "+supply+" / "+demand);
      }
      
      while (Rand.num() < applyChance) {
        final Human applies = new Human(b, base);
        if (verbose) I.say("  New candidate: "+applies);
        candidates.addFirst(applies);
        Application a = FindWork.lookForWork((Human) applies, base, verbose);
        if (a != null) {
          if (verbose) I.say("  Applying at: "+a.employer);
          applies.mind.switchApplication(a);
        }
        applyChance--;
      }
    }
    
    //  TODO:  Consider time-slicing this again, at least for larger
    //  settlements.
    if (verbose) I.say("\nTotal candidates "+candidates.size());
    
    for (ListEntry e = candidates; (e = e.nextEntry()) != candidates;) {
      final Human c = (Human) e.refers;
      final Application a = c.mind.application();
      float quitChance = timeGone;
      if (verbose) I.say("  Updating "+c);
      
      if (a != null) {
        final Background b = a.position;
        final float
          supply = jobSupply.valueFor(b),
          demand = jobDemand.valueFor(b);
        quitChance *= supply / (supply + demand);
        
        if (verbose) {
          I.say("  Quit chance for "+a.position+" "+c+" is: "+quitChance);
        }
      }
      
      if (Rand.num() > quitChance) {
        Application newApp = FindWork.lookForWork((Human) c, base, verbose);
        if (newApp != null) {
          if (verbose) I.say("  Applying at: "+newApp.employer);
          c.mind.switchApplication(newApp);
        }
      }
      else {
        if (verbose) I.say(c+"("+c.vocation()+") is quitting...");
        candidates.removeEntry(e);
        if (a != null) a.employer.personnel().setApplicant(a, false);
      }
    }
  }
  
  
  public void incDemand(Background b, float amount, int period) {
    final float inc = amount *(period / UPDATE_INTERVAL);
    jobDemand.add(inc, b);
  }
  
  
  
  /**  Assessing supply and demand associated with goods-
    */
  private void summariseDemand(Base base) {
    final boolean report = verbose;
    if (report) I.say("\nSummarising demand for base: "+base);
    
    shortages.removeAllItems();
    surpluses.removeAllItems();
    
    final World world = base.world;
    final Tile t = world.tileAt(0, 0);
    
    for (Object o : world.presences.matchesNear(base, t, -1)) {
      final Venue venue = (Venue) o;
      if (venue.privateProperty()) continue;
      
      for (TradeType type : venue.stocks.demanded()) {
        if (type.form != FORM_MATERIAL) continue;
        final int tier = venue.stocks.demandTier(type);
        final float
          demand = venue.stocks.shortageOf(type),
          amount = venue.stocks.amountOf(type),
          shortage,
          surplus;
        
        if (tier == Stocks.TIER_PRODUCER) shortage = 0;
        else shortage = Visit.round(demand - amount, 5, true);
        
        if (tier == Stocks.TIER_CONSUMER) surplus  = 0;
        else surplus  = Visit.round(amount - demand, 5, true);
        
        shortages.bumpItem(type, shortage);
        surpluses.bumpItem(type, surplus );
      }
    }
    
    if (report) {
      I.say("Shortages for "+shortages.size()+" items");
      for (Item i : shortages.allItems()) I.say("  "+i);
      I.say("Surpluses for "+surpluses.size()+" items");
      for (Item i : surpluses.allItems()) I.say("  "+i);
      I.say("");
    }
  }
  
  
  private void calculatePrices() {
    //
    //  Typically speaking, exports have their value halved and imports have
    //  their price doubled if it's coming from offworld.  Anything coming from
    //  another sector of your own planet has much milder cost differences, and
    //  your homeworld will also cut you some slack, at least initially.
    //
    //  In addition, prices improve for exports particularly valued by your
    //  partners (and worsen if already abundant,) and vice versa for imports.
    //  Finally, the value of exports decreases, and of imports increases, with
    //  volume, but this is only likely to be significant for larger
    //  settlements.
    //  TODO:  Charge more for smuggler vessels, and less for Spacers.
    //  TODO:  Implement trade with settlements on the same planet(?)
    
    //
    //  TODO:  Have price levels be global for the settlement as a whole, rather
    //  than calculated at specific structures.  Vendors make money by charging
    //  more in general.
    
    for (TradeType type : ALL_MATERIALS) {
      ///final boolean offworld = true; //For now.
      float
        basePrice = 1 * type.basePrice,
        importMul = 2 + (shortages.amountOf(type) / 1000f),
        exportDiv = 2 + (surpluses.amountOf(type) / 1000f);
      
      for (Sector system : partners) {
        if (Visit.arrayIncludes(system.goodsMade, type)) {
          basePrice *= 0.75f;
          if (system == homeworld) importMul /= 1.50f;
        }
        if (Visit.arrayIncludes(system.goodsNeeded, type)) {
          basePrice *= 1.5f;
          if (system == homeworld) exportDiv *= 0.75f;
        }
      }
      
      if (homeworld != null) {
        final float sizeBonus = base.communitySpirit();
        importMul *= (1 - sizeBonus);
        exportDiv = (1 * sizeBonus) + (exportDiv * (1 - sizeBonus));
      }
      
      importPrices.put(type, basePrice * importMul);
      exportPrices.put(type, basePrice / exportDiv);
    }
  }
  
  
  public float localSurplus(TradeType type) {
    return surpluses.amountOf(type);
  }
  
  
  public float localShortage(TradeType type) {
    return shortages.amountOf(type);
  }
  
  
  public float importPrice(TradeType type) {
    final Float price = importPrices.get(type);
    if (price == null) return type.basePrice * 10f;
    return price;
  }
  
  
  public float exportPrice(TradeType type) {
    final Float price = exportPrices.get(type);
    if (price == null) return type.basePrice / 10f;
    return price;
  }
  
  
  
  
  /**  Dealing with shipping and crew complements-
    */
  private void refreshCrew(Dropship ship) {
    //
    //  TODO:  This crew will need to be updated every now and then- in the
    //         sense of changing the roster.
    //
    //  Get rid of fatigue and hunger, modulate mood, et cetera- account for
    //  the effects of time spent offworld.
    for (Actor works : ship.crew()) {
      final float MH = works.health.maxHealth();
      works.health.liftFatigue(MH * Rand.num());
      works.health.takeCalories(MH, 0.25f + Rand.num());
      works.health.adjustMorale(Rand.num() / 2f);
      works.mind.clearAgenda();
    }
  }
  
  
  private void addCrew(Dropship ship, Background... positions) {
    for (Background v : positions) {
      final Human staff = new Human(new Career(v), base);
      staff.mind.setWork(ship);
      //staff.mind.setHomeVenue(ship);
    }
  }
  
  
  private void loadCargo(
    Dropship ship, Inventory available, final boolean imports
  ) {
    ship.cargo.removeAllItems();
    I.say("Loading cargo...");
    //
    //  We prioritise items based on the amount of demand and the price of the
    //  goods in question-
    final Sorting <Item> sorting = new Sorting <Item> () {
      public int compare(Item a, Item b) {
        if (a == b) return 0;
        final float
          pA = a.amount / a.type.basePrice,
          pB = b.amount / b.type.basePrice;
        return (imports ? 1 : -1) * (pA > pB ? 1 : -1);
      }
    };
    for (Item item : available.allItems()) sorting.add(item);
    float totalAmount = 0;
    for (Item item : sorting) {
      if (totalAmount + item.amount > ship.MAX_CAPACITY) break;
      available.removeItem(item);
      ship.cargo.addItem(item);
      totalAmount += item.amount;
      I.say("Loading cargo: "+item);
    }
  }
  
  
  public Batch <Dropship> allVessels() {
    final Batch <Dropship> vessels = new Batch <Dropship> ();
    vessels.add(ship);
    return vessels;
  }
  
  
  
  /**  Perform updates to trigger new events or assess local needs-
    */
  public void updateCommerce(int numUpdates) {
    if (ship == null) {
      ship = new Dropship();
      ship.assignBase(base);
      addCrew(ship,
        Backgrounds.SHIP_CAPTAIN,
        Backgrounds.SHIP_MECHANIC
      );
      nextVisitTime = base.world.currentTime() + (Rand.num() * SUPPLY_INTERVAL);
    }
    
    updateCandidates(numUpdates);
    if (numUpdates % 10 == 0) {
      summariseDemand(base);
      calculatePrices();
    }
    updateShipping();
  }
  
  
  protected void updateShipping() {
    
    final int shipStage = ship.flightStage();
    if (ship.landed()) {
      final float sinceDescent = ship.timeLanded();
      if (sinceDescent > SUPPLY_DURATION) {
        if (shipStage == Dropship.STAGE_LANDED) ship.beginBoarding();
        if (ship.allAboard() && shipStage == Dropship.STAGE_BOARDING) {
          ship.beginAscent();
          nextVisitTime = base.world.currentTime();
          nextVisitTime += SUPPLY_INTERVAL * (0.5f + Rand.num());
        }
      }
    }
    if (! ship.inWorld()) {
      boolean shouldVisit = migrantsIn.size() > 0;
      if ((! shortages.empty()) || (! surpluses.empty())) shouldVisit = true;
      shouldVisit &= base.world.currentTime() > nextVisitTime;
      shouldVisit &= ship.timeAway(base.world) > SUPPLY_INTERVAL;
      
      if (shouldVisit) {
        I.say("SENDING SHIP...");
        final boolean canLand = ship.findLandingSite(base);
        if (! canLand) return;
        /*
        final Box2D zone = Dropship.findLandingSite(ship, base);
        if (zone == null) return;
        //*/
        I.say("Sending ship to land at: "+ship.dropPoint());
        
        while (ship.inside().size() < Dropship.MAX_PASSENGERS) {
          if (migrantsIn.size() == 0) break;
          final Actor migrant = migrantsIn.removeFirst();
          ship.setInside(migrant, true);
        }
        
        loadCargo(ship, shortages, true);
        refreshCrew(ship);
        for (Actor c : ship.crew()) ship.setInside(c, true);
        
        ship.beginDescent(base.world);
      }
    }
  }
}






/*
public boolean genCandidate(Background vocation, Venue venue, int numOpen) {
  //
  //  You might want to introduce limits on the probability of finding
  //  candidates based on the relative sizes of the source and destination
  //  settlements, and the number of existing applicants for a position.
  final int numA = venue.personnel.numApplicants(vocation);
  if (numA >= numOpen * MAX_APPLICANTS) return false;
  final Human candidate = new Human(vocation, venue.base());
  //
  //  This requires more work on the subject of pricing.  Some will join for
  //  free, but others need enticement, depending on distance and willingness
  //  to relocate, and the friendliness of the home system.
  final int signingCost = Backgrounds.HIRE_COSTS[vocation.standing];
  venue.personnel.applyFor(vocation, candidate, signingCost);
  //
  //  Insert the candidate in local records, and return.
  List <Actor> list = candidates.get(venue);
  if (list == null) candidates.put(venue, list = new List <Actor> ());
  list.add(candidate);
  return true;
}


public void cullCandidates(Background vocation, Venue venue) {
  final List <Actor> list = candidates.get(venue);
  if (list == null) return;
  final int numOpenings = venue.numOpenings(vocation);
  if (numOpenings > 0) return;
  for (Actor actor : list) if (actor.vocation() == vocation) {
    list.remove(actor);
    venue.personnel.removeApplicant(actor);
  }
  if (list.size() == 0) candidates.remove(venue);
}
//*/

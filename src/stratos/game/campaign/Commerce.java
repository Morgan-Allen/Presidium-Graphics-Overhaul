


package stratos.game.campaign;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.building.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.user.BaseUI;
import stratos.util.*;
import static stratos.game.building.Economy.*;


//  TODO:  This will have to be merged with (or rendered obsolete by) the more
//  generalised, and powerful, supply-and-demand algorithms I need to work on.


public class Commerce {
  
  
  /**  Field definitions, constructor, save/load methods-
    */
  private static boolean
    verbose        = false,
    extraVerbose   = false,
    migrateVerbose = verbose && false,
    tradeVerbose   = verbose && false;
  
  final public static float
    SUPPLY_INTERVAL = Stage.STANDARD_DAY_LENGTH / 2f,
    SUPPLY_DURATION = SUPPLY_INTERVAL / 2f,
    
    APPLY_INTERVAL  = Stage.STANDARD_DAY_LENGTH / 2f,
    UPDATE_INTERVAL = 10,
    DEMAND_INC      = 0.15f,
    MAX_APPLICANTS  = 3;
  
  
  
  final Base base;
  Sector homeworld;
  final List <Sector> partners = new List <Sector> ();
  
  final Tally <Background>
    jobSupply = new Tally <Background> (),
    jobDemand = new Tally <Background> ();
  
  final List <Actor> candidates = new List <Actor> ();
  final List <Actor> migrantsIn = new List <Actor> ();
  
  final Inventory
    localShortages  = new Inventory(null),
    localSurpluses  = new Inventory(null),
    globalShortages = new Inventory(null),
    globalSurpluses = new Inventory(null);
  final Table <Traded, Float>
    importPrices = new Table <Traded, Float> (),
    exportPrices = new Table <Traded, Float> ();
  
  private Dropship ship;
  private float nextVisitTime;
  
  
  
  public Commerce(Base base) {
    this.base = base;
    for (Traded type : ALL_MATERIALS) {
      importPrices.put(type, (float) type.basePrice());
      exportPrices.put(type, (float) type.basePrice());
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
    
    localShortages.loadState(s);
    localSurpluses.loadState(s);
    globalShortages.loadState(s);
    globalSurpluses.loadState(s);
    for (Traded type : ALL_MATERIALS) {
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
    
    localShortages.saveState(s);
    localSurpluses.saveState(s);
    globalShortages.saveState(s);
    globalSurpluses.saveState(s);
    for (Traded type : ALL_MATERIALS) {
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

    final boolean report = migrateVerbose;
    final float inc = DEMAND_INC, timeGone = UPDATE_INTERVAL / APPLY_INTERVAL;
    final Background demanded[] = jobDemand.keysToArray(Background.class);
    
    for (Background b : demanded) {
      float demand = jobDemand.valueFor(b);
      demand = Math.max((demand * (1 - inc)) - (inc / 100), 0);
      jobDemand.set(b, demand);
    }
    
    jobSupply.clear();
    for (Actor c : candidates) {
      final Background a = FindWork.ambitionOf(c);
      if (a == null) continue;
      jobSupply.add(1, a);
    }
    
    if (report) I.say("\nChecking for new candidates...");
    
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
      
      if (report) {
        I.say("  Hire chance for "+b+" is "+applyChance);
        I.say("  Supply/demand "+supply+" / "+demand);
      }
      
      while (Rand.num() < applyChance) {
        final Human applies = new Human(b, base);
        if (report) I.say("  New candidate: "+applies);
        
        candidates.addFirst(applies);
        final FindWork a = FindWork.attemptFor(applies, b, base);
        if (a.position() != null) {
          if (report) I.say("  Applying at: "+a.position());
          a.confirmApplication();
        }
        applyChance--;
      }
    }
    
    //  TODO:  Consider time-slicing this again, at least for larger
    //  settlements.
    if (report) I.say("\nTotal candidates "+candidates.size());
    
    for (ListEntry e = candidates; (e = e.nextEntry()) != candidates;) {
      final Human c = (Human) e.refers;
      
      final Background a = FindWork.ambitionOf(c);
      float quitChance = timeGone;
      if (report) I.say("  Updating "+c);
      
      if (a != null) {
        final float
          supply = jobSupply.valueFor(a),
          demand = jobDemand.valueFor(a);
        quitChance *= supply / (supply + demand);
        
        if (report) {
          I.say("  Quit chance for "+a+" "+c+" is: "+quitChance);
        }
      }
      
      final FindWork b = FindWork.attemptFor(c, a, base);
      if (Rand.num() > quitChance) {
        if (b.position() != null) {
          if (report) I.say("  Applying at: "+b.employer());
          b.confirmApplication();
        }
      }
      else {
        if (report) I.say(c+"("+c.vocation()+") is quitting...");
        candidates.removeEntry(e);
        b.cancelApplication();
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
    final boolean report = tradeVerbose && base == BaseUI.current().played();
    if (report) I.say("\nSummarising demand for base: "+base);
    
    localShortages.removeAllItems();
    localSurpluses.removeAllItems();
    
    final Stage world = base.world;
    final Tile t = world.tileAt(0, 0);
    
    for (Object o : world.presences.matchesNear(base, t, -1)) {
      final Venue venue = (Venue) o;
      if (venue.privateProperty()) continue;
      
      for (Traded type : venue.stocks.demanded()) {
        if (type.form != FORM_MATERIAL) continue;
        final int tier = venue.stocks.demandTier(type);
        final float
          amount   = venue.stocks.amountOf  (type),
          demand   = venue.stocks.demandFor (type),
          shortage = venue.stocks.shortageOf(type),
          surplus  = venue.stocks.surplusOf (type);
        
        if (report && extraVerbose) {
          I.say("  "+venue+" "+type+" (tier: "+tier+")");
          I.say("    Amount:   "+amount+"/"+demand);
          I.say("    Surplus:  "+surplus );
          I.say("    Shortage: "+shortage);
        }
        
        if (tier == Stocks.TIER_EXPORTER) {
          localSurpluses.bumpItem(type, Visit.round(amount  , 5, false));
        }
        else if (tier != Stocks.TIER_PRODUCER) {
          localShortages.bumpItem(type, Visit.round(shortage, 5, true ));
        }
        
        if (tier == Stocks.TIER_IMPORTER) {
          localShortages.bumpItem(type, Visit.round(shortage, 5, true ));
        }
        else if (tier != Stocks.TIER_CONSUMER) {
          localSurpluses.bumpItem(type, Visit.round(surplus , 5, false));
        }
      }
    }
    
    if (report) {
      I.say("Shortages for "+localShortages.size()+" items");
      for (Item i : localShortages.allItems()) I.say("  "+i);
      I.say("Surpluses for "+localSurpluses.size()+" items");
      for (Item i : localSurpluses.allItems()) I.say("  "+i);
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
    globalShortages.removeAllItems();
    globalSurpluses.removeAllItems();
    
    for (Traded type : ALL_MATERIALS) {
      ///final boolean offworld = true; //For now.
      float
        basePrice = 1 * type.basePrice(),
        importMul = 2 + (localShortages.amountOf(type) / 1000f),
        exportDiv = 2 + (localSurpluses.amountOf(type) / 1000f);
      
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
      
      if (basePrice > type.basePrice()) {
        globalShortages.addItem(Item.withAmount(type, 1));
      }
      else if (basePrice < type.basePrice()) {
        globalSurpluses.addItem(Item.withAmount(type, 1));
      }
      
      if (homeworld != null) {
        final float sizeBonus = base.relations.communitySpirit();
        importMul *= (1 - sizeBonus);
        exportDiv = (1 * sizeBonus) + (exportDiv * (1 - sizeBonus));
      }
      
      importPrices.put(type, basePrice * importMul);
      exportPrices.put(type, basePrice / exportDiv);
    }
  }
  
  
  public float localSurplus(Traded type) {
    return localSurpluses.amountOf(type);
  }
  
  
  public float localShortage(Traded type) {
    return localShortages.amountOf(type);
  }
  
  
  public Traded[] globalShortages() {
    return globalShortages.allItemTypes();
  }
  
  
  public Traded[] globalSurpluses() {
    return globalSurpluses.allItemTypes();
  }
  
  
  public float importPrice(Traded type) {
    final Float price = importPrices.get(type);
    if (price == null) return type.basePrice() * 10f;
    return price;
  }
  
  
  public float exportPrice(Traded type) {
    final Float price = exportPrices.get(type);
    if (price == null) return type.basePrice() / 10f;
    return price;
  }
  
  
  
  
  /**  Dealing with shipping and crew complements-
    */
  private void refreshCrew(Dropship ship, Background... positions) {
    //
    //  This crew will need to be updated every now and then- in the sense of
    //  changing the roster due to losses or career changes.
    for (Background b : positions) {
      if (ship.personnel().numHired(b) < 1) {
        final Human staff = new Human(new Career(b), base);
        staff.mind.setWork(ship);
        staff.mind.setHome(ship);
      }
    }
    //
    //  Get rid of fatigue and hunger, modulate mood, et cetera- account for
    //  the effects of time spent offworld.
    for (Actor works : ship.crew()) {
      final float MH = works.health.maxHealth();
      works.health.liftFatigue (MH * Rand.num());
      works.health.takeCalories(MH, 0.25f + Rand.num());
      works.health.adjustMorale(Rand.num() / 2f);
      works.mind.clearAgenda();
    }
  }
  
  
  private void refreshShip() {
    if (ship == null || ship.destroyed()) {
      ship = new Dropship();
      ship.assignBase(base);
      nextVisitTime = base.world.currentTime() + (Rand.num() * SUPPLY_INTERVAL);
    }
    else {
      final float repair = Visit.clamp(1.25f - (Rand.num() / 2), 0, 1);
      ship.structure.setState(Structure.STATE_INTACT, repair);
    }
    refreshCrew(ship,
      Backgrounds.SHIP_CAPTAIN,
      Backgrounds.SHIP_MECHANIC
    );
  }
  
  
  private void loadCargo(
    Dropship ship, Inventory available, final boolean imports
  ) {
    final boolean report = tradeVerbose && base == BaseUI.current().played();
    ship.cargo.removeAllItems();
    if (report) I.say("\nLoading dropship cargo...");
    //
    //  We prioritise items based on the amount of demand and the price of the
    //  goods in question-
    final Sorting <Item> sorting = new Sorting <Item> () {
      public int compare(Item a, Item b) {
        if (a == b) return 0;
        final float
          pA = a.amount / a.type.basePrice(),
          pB = b.amount / b.type.basePrice();
        return (imports ? 1 : -1) * (pA > pB ? 1 : -1);
      }
    };
    for (Item item : available.allItems()) sorting.add(item);
    float totalAmount = 0;
    for (Item item : sorting) {
      if (totalAmount + item.amount > ship.MAX_CAPACITY) break;
      ship.cargo.addItem(item);
      totalAmount += item.amount;
      if (report) I.say("  "+item);
    }
  }
  
  
  public Batch <Dropship> allVessels() {
    final Batch <Dropship> vessels = new Batch <Dropship> ();
    vessels.add(ship);
    return vessels;
  }
  
  
  public void scheduleDrop(float delay) {
    if (ship == null) refreshShip();
    nextVisitTime = base.world.currentTime() + delay;
    ship.resetAwayTime();
  }
  
  
  
  /**  Perform updates to trigger new events or assess local needs-
    */
  public void updateCommerce(int numUpdates) {
    //final boolean report = verbose && BaseUI.current().played() == base;
    
    if (ship == null) refreshShip();
    updateCandidates(numUpdates);
    if (numUpdates % 10 == 0) {
      summariseDemand(base);
      calculatePrices();
      updateShipping();
    }
  }
  
  
  protected void updateShipping() {
    if (base.primal) return;
    final boolean report = verbose && BaseUI.current().played() == base;
    
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
      final boolean
        needMigrate = migrantsIn.size() > 0, //  TODO:  Include emmigration.
        needTrade   = (! localShortages.empty()) || (! localSurpluses.empty()),
        visitDue    = base.world.currentTime()  > nextVisitTime,
        travelDone  = ship.timeAway(base.world) > SUPPLY_DURATION,
        shouldVisit = (needMigrate || needTrade) && (visitDue && travelDone),
        canLand     = ship.findLandingSite(base),
        willLand    = shouldVisit && canLand;
      
      if (willLand) {
        if (report) I.say("\nSENDING DROPSHIP TO "+ship.landArea());
        
        while (migrantsIn.size() > 0) {
          final Actor migrant = migrantsIn.removeFirst();
          ship.setInside(migrant, true);
          if (ship.inside().size() >= Dropship.MAX_PASSENGERS) break;
        }
        loadCargo(ship, localShortages, true);
        refreshCrew(ship);
        
        for (Actor c : ship.crew()) ship.setInside(c, true);
        ship.beginDescent(base.world);
      }
      else if (visitDue && report) {
        I.say("\nNo time for commerce?  Inconceivable!");
        I.say("  Travel done:   "+travelDone );
        I.say("  Need migrants: "+needMigrate);
        I.say("  Need trade:    "+needTrade  );
        I.say("  Can land:      "+canLand    );
      }
      else if (report) {
        final float interval = nextVisitTime - base.world.currentTime();
        I.say("\nNext ship drop due in "+interval);
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

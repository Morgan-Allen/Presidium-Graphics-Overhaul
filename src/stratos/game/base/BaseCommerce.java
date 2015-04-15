/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;




//  TODO:  Use a smuggling behaviour here instead.  And move some of the ship
//  functions to the ShipUtils class?

//  TODO:  Allow multiple ships- based on the size of the settlement and
//  limited by the number of airfields!  (You never get more than one
//  otherwise.)



public class BaseCommerce {
  
  
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
    TIME_SLICE      = UPDATE_INTERVAL / APPLY_INTERVAL,
    MAX_APPLICANTS  = 3,
    
    SMUGGLE_MARGIN  = 1.25f,
    BASE_EXPORT_DIV = 1.50f,
    BASE_IMPORT_MUL = 1.75f;
  
  
  final Base base;
  Sector homeworld;
  
  final Tally <Traded>
    primaryDemand = new Tally <Traded> (),
    primarySupply = new Tally <Traded> (),
    importDemand  = new Tally <Traded> (),
    exportSupply  = new Tally <Traded> ();
  final Table <Traded, Float>
    importPrices = new Table <Traded, Float> (),
    exportPrices = new Table <Traded, Float> ();
  
  final List <Actor> candidates = new List <Actor> ();
  
  final List <Sector> partners = new List <Sector> ();
  private Dropship ship;
  private float visitTime;
  
  
  
  public BaseCommerce(Base base) {
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
    
    for (Traded type : ALL_MATERIALS) {
      primaryDemand.set(type, s.loadFloat());
      primarySupply.set(type, s.loadFloat());
      importDemand.set (type, s.loadFloat());
      exportSupply.set (type, s.loadFloat());
      importPrices.put (type, s.loadFloat());
      exportPrices.put (type, s.loadFloat());
    }
    s.loadObjects(candidates);
    
    ship = (Dropship) s.loadObject();
    visitTime = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObject(homeworld);
    s.saveInt(partners.size());
    for (Sector p : partners) s.saveObject(p);
    
    for (Traded type : ALL_MATERIALS) {
      s.saveFloat(primaryDemand.valueFor(type));
      s.saveFloat(primarySupply.valueFor(type));
      s.saveFloat(importDemand .valueFor(type));
      s.saveFloat(exportSupply .valueFor(type));
      s.saveFloat(importPrices.get(type)      );
      s.saveFloat(exportPrices.get(type)      );
    }
    s.saveObjects(candidates);
    
    s.saveObject(ship);
    s.saveFloat(visitTime);
  }
  
  
  public void assignHomeworld(Sector s) {
    homeworld = s;
    togglePartner(s, true);
  }
  
  
  public Sector homeworld() {
    return homeworld;
  }
  
  
  public void togglePartner(Sector s, boolean is) {
    if (is) partners.include(s);
    else    partners.remove (s);
  }
  
  
  public List <Sector> partners() {
    return partners;
  }
  
  
  public List <Actor> allCandidates() {
    return candidates;
  }
  
  
  
  /**  Perform updates to trigger new events or assess local needs-
    */
  public void updateCommerce(int numUpdates) {
    final boolean report = verbose && BaseUI.current().played() == base;
    if (report && extraVerbose) I.say("\nUpdating commerce for base: "+base);
    
    updateCandidates(numUpdates);
    summariseDemandAndPrices(base, numUpdates);
    refreshShip(false);
    updateShipping(numUpdates);
  }
  
  
  
  /**  Dealing with migrants and cargo-
    */
  protected void updateCandidates(int numUpdates) {
    if ((numUpdates % UPDATE_INTERVAL) != 0) return;
    
    final boolean report = migrateVerbose && base == BaseUI.currentPlayed();
    final Background demanded[] = Background.INDEX.allEntries(Background.class);
    
    final Tally <Background> jobSupply = new Tally <Background> ();
    for (Actor c : candidates) jobSupply.add(1, c.mind.vocation());
    
    if (report) I.say("\nChecking for new recruits (slice: "+TIME_SLICE+")");
    
    for (Background b : demanded) {
      final float
        demand = (base.demands.globalDemand(b) - 0.5f) * MAX_APPLICANTS,
        supply = jobSupply.valueFor(b);
      if (demand <= 0) continue;
      float applyChance = (demand - supply) * TIME_SLICE;
      
      if (report) {
        I.say("  Hire chance for "+b+" is "+applyChance);
        I.say("  Supply/demand "+supply+" / "+demand);
      }
      
      while (Rand.num() < applyChance) {
        final Human applies = new Human(b, base);
        if (report) I.say("  New candidate: "+applies);
        candidates.addFirst(applies);
        final FindWork a = FindWork.attemptFor(applies, b, base);
        
        if (a == null || a.position() == null) {
          if (report) I.say("  No application made!");
        }
        else {
          if (report) I.say("  Applying at: "+a.position());
          a.confirmApplication();
        }
        applyChance--;
      }
    }
    
    //  TODO:  Consider time-slicing this again, at least for larger
    //  settlements.
    if (report) I.say("\nTotal candidates "+candidates.size());
    
    for (ListEntry <Actor> e = candidates; (e = e.nextEntry()) != candidates;) {
      final Human c = (Human) e.refers;
      
      final Background a = c.mind.vocation();
      final FindWork b = FindWork.attemptFor(c, a, base);
      float quitChance = TIME_SLICE;
      if (report) I.say("  Updating "+c+" ("+a+")");
      
      if      (c.inWorld ()) quitChance = 1;
      else if (b.wasHired()) quitChance = 0;
      else if (a != null) {
        final float
          supply = jobSupply.valueFor(a),
          demand = base.demands.globalDemand(a),
          total  = supply + demand;
        if (total > 0) quitChance *= supply / total;
        if (report) {
          I.say("    Quit chance: "+quitChance);
          I.say("    Supply/demand "+supply+" / "+demand);
        }
      }
      
      if (a == null || b == null || Rand.num() < quitChance) {
        if (report) I.say("    Quitting...");
        candidates.removeEntry(e);
        if (b != null) b.cancelApplication();
      }
      else if (b != null && b.position() != null) {
        if (report) I.say("    Applying at: "+b.employer());
        b.confirmApplication();
      }
    }
  }
  
  
  public void addCandidate(Actor applies, Venue at, Background position) {
    candidates.add(applies);
    FindWork.assignAmbition(applies, position, at, 2.0f);
  }
  
  
  
  /**  Assessing supply and demand associated with goods-
    */
  private void summariseDemandAndPrices(Base base, int numUpdates) {
    if ((numUpdates % UPDATE_INTERVAL) != 0) return;
    final boolean report = tradeVerbose && base == BaseUI.current().played();
    if (report) I.say("\nSUMMARISING DEMAND FOR "+base);
    //
    //  Firstly, we summarise domestic supply and demand for all the major
    //  commodities-
    primarySupply.clear();
    primaryDemand.clear();
    importDemand .clear();
    exportSupply .clear();
    
    for (Object o : base.world.presences.matchesNear(base, null, -1)) {
      final Venue venue = (Venue) o;

      for (Traded type : venue.stocks.demanded()) {
        final int tier = venue.owningTier();
        final boolean producer = venue.stocks.producer(type);
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
        
        if (tier <= Owner.TIER_FACILITY) {
          if (producer) primarySupply.add(amount, type);
          else primaryDemand.add(demand, type);
        }
        if (tier >= Owner.TIER_DEPOT) {
          if (producer) exportSupply.add(amount, type);
          else importDemand.add(demand, type);
        }
      }
    }
    //
    //  Then, we tally up average supply and demand for goods offworld.
    for (Traded type : ALL_MATERIALS) {
      float
        basePrice = type.basePrice(),
        importMul = 2, exportDiv = 2,
        avgDemand = 0, homeBonus = 0;
      for (Sector partner : partners) {
        if (Visit.arrayIncludes(partner.goodsMade  , type)) {
          avgDemand--;
        }
        if (Visit.arrayIncludes(partner.goodsNeeded, type)) {
          avgDemand++;
        }
        if (partner == homeworld) homeBonus++;
      }
      //
      //  Goods that are in demand offworld are more expensive to import but
      //  more profitable to export, and vice versa for goods in abundance.
      //  Small settlements dependant on their homeworld also get price
      //  subsidies.
      if (partners.empty()) {
        importMul += 1;
        exportDiv += 1;
      }
      else {
        avgDemand /= partners.size();
        homeBonus *= base.relations.communitySpirit() / partners.size();
        importMul += (avgDemand - homeBonus) / 2;
        exportDiv -= (avgDemand + homeBonus) / 2;
      }
      importMul *= BASE_IMPORT_MUL / 2;
      exportDiv *= BASE_EXPORT_DIV / 2;
      importPrices.put(type, basePrice * importMul);
      exportPrices.put(type, basePrice / exportDiv);
    }
  }
  
  
  public float primarySupply(Traded type) {
    return primarySupply.valueFor(type);
  }
  
  
  public float primaryDemand(Traded type) {
    return primaryDemand.valueFor(type);
  }
  
  
  public float primaryShortage(Traded type) {
    float demand = primaryDemand(type), supply = primarySupply(type);
    if (demand == 0) return 0;
    return (demand - supply) / demand;
  }
  
  
  public float importDemand(Traded type) {
    return importDemand.valueFor(type);
  }
  
  
  public float exportSupply(Traded type) {
    return exportSupply.valueFor(type);
  }
  
  
  public float tradingShortage(Traded type) {
    float demand = importDemand(type), supply = exportSupply(type);
    if (demand == 0) return 0;
    return (demand - supply) / demand;
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
  
  
  public void configCargo(Stocks forShipping, int fillLimit, boolean fillImp) {
    
    if (fillImp) forShipping.removeAllItems();
    
    final Batch <Item>
      imports = new Batch <Item> (),
      exports = new Batch <Item> ();
    float sumImp = 0, sumExp = 0, scaleImp = 1, scaleExp = 1;
    
    for (Traded type : ALL_MATERIALS) {
      final float shortage = importDemand(type) - exportSupply(type);
      if (shortage > 0) {
        imports.add(Item.withAmount(type, shortage));
        sumImp += shortage;
      }
      if (shortage < 0) {
        exports.add(Item.withAmount(type, 0 - shortage));
        sumExp += 0 - shortage;
      }
    }
    
    if (sumImp > fillLimit) scaleImp = fillLimit / sumImp;
    for (Item i : imports) {
      final int amount = Nums.round(i.amount * scaleImp, 2, false);
      if (amount <= 0) continue;
      forShipping.forceDemand(i.type, amount, false);
      if (fillImp) forShipping.bumpItem(i.type, amount);
    }
    
    if (sumExp > fillLimit) scaleExp = fillLimit / sumExp;
    for (Item e : exports) {
      final int amount = Nums.round(e.amount * scaleExp, 2, false);
      if (amount <= 0) continue;
      forShipping.forceDemand(e.type, amount, true);
    }
  }
  
  
  
  
  
  /**  Dealing with shipping and crew complements-
    */
  private void updateShipping(int numUpdates) {
    if ((numUpdates % UPDATE_INTERVAL) != 0) return;
    if (base.primal) return;
    
    final boolean report = verbose && BaseUI.current().played() == base;
    final float time = base.world.currentTime();
    final int shipStage = ship.flightStage();
    final boolean
      visitDue = (! ship.inWorld()) && time > visitTime,
      canLand  = ShipUtils.findLandingSite(ship, base);
    //
    //  If the ship has already landed, see if it's time to depart-
    if (ship.landed()) {
      final float sinceDescent = time - visitTime;
      final boolean allAboard = ShipUtils.allAboard(ship);
      
      if (sinceDescent > SUPPLY_DURATION) {
        if (shipStage == Dropship.STAGE_LANDED) ship.beginBoarding();
        if (allAboard && shipStage == Dropship.STAGE_BOARDING) {
          ship.beginAscent();
          visitTime = base.world.currentTime();
          visitTime += SUPPLY_INTERVAL * (0.5f + Rand.num());
        }
      }
    }
    //
    //  If the ship is offworld, see if it's time to return-
    if (visitDue && canLand) {
      if (report) I.say("\nSENDING DROPSHIP TO "+ship.landArea());
      
      base.world.offworld.addPassengersTo(ship);
      configCargo(ship.cargo, Dropship.MAX_CAPACITY, true);
      refreshCrew(ship, Backgrounds.DEFAULT_SHIP_CREW);
      refreshShip(true);
      
      for (Actor c : ship.crew()) ship.setInside(c, true);
      ship.beginDescent(base.world);
    }
  }
  

  private void refreshCrew(Dropship ship, Background... positions) {
    //
    //  This crew will need to be updated every now and then- in the sense of
    //  changing the roster due to losses or career changes.
    for (Background b : positions) {
      if (ship.staff().numHired(b) < Visit.countInside(b, positions)) {
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
  
  
  private void refreshShip(boolean forLanding) {
    final boolean report = verbose && base == BaseUI.current().played();
    if (report) I.say("\nREFRESHING SHIP: "+ship);
    
    if (ship == null || ship.destroyed()) {
      if (report) I.say("  New ship required!");
      ship = new Dropship();
      ship.assignBase(base);
      visitTime = base.world.currentTime() + (Rand.num() * SUPPLY_INTERVAL);
    }
    if (forLanding) {
      final float repair = Nums.clamp(1.25f - (Rand.num() / 2), 0, 1);
      ship.structure.setState(Structure.STATE_INTACT, repair);
    }
  }
  
  
  public Batch <Dropship> allVessels() {
    final Batch <Dropship> vessels = new Batch <Dropship> ();
    if (ship != null) vessels.add(ship);
    return vessels;
  }
  
  
  public void scheduleDrop(float delay) {
    if (ship == null) refreshShip(true);
    visitTime = base.world.currentTime() + delay;
  }
}




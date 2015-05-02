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
import stratos.game.civic.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;



public class BaseCommerce {
  
  
  /**  Field definitions, constructor, save/load methods-
    */
  private static boolean
    verbose        = true ,
    extraVerbose   = false,
    migrateVerbose = verbose && false,
    tradeVerbose   = verbose && true ;
  
  final public static float
    SHIP_JOURNEY_TIME   = Stage.STANDARD_DAY_LENGTH / 2f,
    SHIP_VISIT_DURATION = Stage.STANDARD_HOUR_LENGTH * 6,
    SHIP_UPDATE_CHANCE  = 1f / 4,
    
    APPLY_INTERVAL  = Stage.STANDARD_DAY_LENGTH / 2f,
    UPDATE_INTERVAL = 10,
    TIME_SLICE      = UPDATE_INTERVAL / APPLY_INTERVAL,
    MAX_APPLICANTS  = 3,
    
    SMUGGLE_MARGIN  = 1.25f,
    BASE_EXPORT_DIV = 1.50f,
    BASE_IMPORT_MUL = 1.75f;
  
  
  final Base base;
  
  protected VerseLocation homeworld = Verse.DEFAULT_HOMEWORLD;
  final List <VerseLocation> partners = new List <VerseLocation> ();
  protected int maxShipsPerDay = 0;
  final List <Actor> candidates = new List <Actor> ();
  
  final Tally <Traded>
    primaryDemand = new Tally <Traded> (),
    primarySupply = new Tally <Traded> (),
    importDemand  = new Tally <Traded> (),
    exportSupply  = new Tally <Traded> ();
  final Table <Traded, Float>
    importPrices = new Table <Traded, Float> (),
    exportPrices = new Table <Traded, Float> ();
  
  
  
  
  public BaseCommerce(Base base) {
    this.base = base;
    for (Traded type : ALL_MATERIALS) {
      importPrices.put(type, (float) type.basePrice());
      exportPrices.put(type, (float) type.basePrice());
    }
  }
  
  
  public void loadState(Session s) throws Exception {
    
    homeworld = (VerseLocation) s.loadObject();
    s.loadObjects(partners);
    maxShipsPerDay = s.loadInt();
    s.loadObjects(candidates);
    
    for (Traded type : ALL_MATERIALS) {
      primaryDemand.set(type, s.loadFloat());
      primarySupply.set(type, s.loadFloat());
      importDemand.set (type, s.loadFloat());
      exportSupply.set (type, s.loadFloat());
      importPrices.put (type, s.loadFloat());
      exportPrices.put (type, s.loadFloat());
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObject(homeworld);
    s.saveObjects(partners);
    s.saveInt(maxShipsPerDay);
    s.saveObjects(candidates);
    
    for (Traded type : ALL_MATERIALS) {
      s.saveFloat(primaryDemand.valueFor(type));
      s.saveFloat(primarySupply.valueFor(type));
      s.saveFloat(importDemand .valueFor(type));
      s.saveFloat(exportSupply .valueFor(type));
      s.saveFloat(importPrices.get(type)      );
      s.saveFloat(exportPrices.get(type)      );
    }
  }
  
  
  public void assignHomeworld(VerseLocation s) {
    homeworld = s;
    togglePartner(s, true);
  }
  
  
  public VerseLocation homeworld() {
    return homeworld;
  }
  
  
  public void togglePartner(VerseLocation s, boolean is) {
    if (is) partners.include(s);
    else    partners.remove (s);
  }
  
  
  public Series <VerseLocation> partners() {
    return partners;
  }
  
  
  public Series <Actor> allCandidates() {
    return candidates;
  }
  
  
  
  /**  Perform updates to trigger new events or assess local needs-
    */
  public void updateCommerce(int numUpdates) {
    final boolean report = verbose && BaseUI.current().played() == base;
    if (report && extraVerbose) I.say("\nUpdating commerce for base: "+base);
    if (base.primal) return;
    
    updateCandidates(numUpdates);
    summariseDemandAndPrices(numUpdates);
    updateActiveShipping(numUpdates);
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
          a.enterApplication();
        }
        applyChance--;
      }
    }
    
    //  TODO:  Consider time-slicing this again, at least for larger
    //  settlements.
    if (report) I.say("\nTotal candidates "+candidates.size());
    
    for (ListEntry <Actor> e = candidates; (e = e.nextEntry()) != candidates;) {
      //
      //  If there's a successful application, enter it.
      final Actor      actor = e.refers;
      final Background job   = actor.mind.vocation();
      final FindWork   finds = FindWork.attemptFor(actor, job, base);
      float quitChance = TIME_SLICE;
      if (finds == null || finds.wasHired()) {
        quitChance = 1;
      }
      else {
        final float
          supply = jobSupply.valueFor(job),
          demand = base.demands.globalDemand(job),
          total  = supply + demand;
        if (total > 0) quitChance *= supply / total;
        finds.enterApplication();
        if (report) I.say("  "+actor+" ("+job+") applying: "+finds.employer());
      }
      //
      //  Otherwise, quit chance is based on relative abundance.
      if (Rand.num() <= quitChance) {
        if (finds != null) finds.cancelApplication();
        candidates.removeEntry(e);
        if (report) I.say("  "+actor+" ("+job+") quitting...");
      }
    }
  }
  
  
  public void addCandidate(Actor applies, Venue at, Background position) {
    candidates.add(applies);
    FindWork finding = FindWork.assignAmbition(applies, position, at, 2.0f);
    finding.enterApplication();
  }
  
  
  public void addCandidate(Background position, Venue at) {
    final Actor applies = new Human(position, base);
    addCandidate(applies, at, position);
  }
  
  
  public void removeCandidate(Actor applies) {
    candidates.remove(applies);
  }
  
  
  public int numCandidates(Background position) {
    int count = 0;
    for (Actor a : candidates) if (a.mind.vocation() == position) count++;
    return count;
  }
  
  
  
  /**  Assessing supply and demand associated with goods-
    */
  private void summariseDemandAndPrices(int numUpdates) {
    if ((numUpdates % UPDATE_INTERVAL) != 0) return;
    final boolean report = tradeVerbose && base == BaseUI.currentPlayed();
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
          primarySupply.add(Nums.min(amount, demand), type);
          primaryDemand.add(demand, type);
        }
        if (tier >= Owner.TIER_DEPOT) {
          if (producer) exportSupply.add(amount, type);
          else importDemand.add(demand - amount, type);
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
      for (VerseLocation partner : partners) {
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
  
  
  
  /**  And finally, utility methods for calibrating the volume of shipping to
    *  or from this particular base:
    */
  private void updateActiveShipping(int numUpdates) {
    if ((numUpdates % UPDATE_INTERVAL) != 0) return;
    final boolean report = tradeVerbose && base == BaseUI.currentPlayed();
    if (report) I.say("\nUPDATING ACTIVE SHIPPING FOR "+base);
    
    //  TODO:  At the moment, we're aggregating all supply and demand into a
    //  single channel from the homeworld.  Once the planet-map is sorted out,
    //  you should evaluate pricing for each world independantly... and how
    //  many ships will come.
    
    int spaceLimit = 1;
    for (Object t : base.world.presences.allMatches(Airfield.class)) {
      final Airfield field = (Airfield) t;
      if (field.base() != base) continue;
      spaceLimit++;
    }
    spaceLimit = Nums.min(spaceLimit, homeworld.population + 1);
    
    //
    //  At any rate, we simply adjust the number of current ships based on
    //  the space allowance-
    VerseJourneys travel = base.world.offworld.journeys;
    VerseLocation locale = base.world.offworld.stageLocation();
    
    Dropship running[] = travel.shipsBetween(locale, homeworld, base, true);
    Dropship last = (Dropship) Visit.last(running);
    if (running.length < spaceLimit) {
      travel.setupShipping(homeworld, locale, base, true);
    }
    if (running.length > spaceLimit && ! last.inWorld()) {
      travel.retireShip(last);
    }
    
    if (report) {
      I.say("  Ships available: "+running.length);
      I.say("  Ideal limit:     "+spaceLimit);
    }
    this.maxShipsPerDay = spaceLimit;
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
}




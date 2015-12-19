/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.start;
import stratos.content.abilities.*;
import stratos.content.civic.*;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.game.plans.*;
import stratos.game.verse.*;
import stratos.graphics.common.Colour;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;
import static stratos.game.actors.Backgrounds.*;



public class DebugCommerce extends Scenario {
  
  
  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new DebugCommerce());
  }
  
  
  private DebugCommerce() {
    super("debug_commerce", true);
  }
  
  
  public DebugCommerce(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }

  
  public void beginGameSetup() {
    super.initScenario("debug_commerce");
  }
  
  
  protected Stage createWorld() {
    final TerrainGen TG = new TerrainGen(
      64, 0.2f,
      Habitat.FOREST      , 2f,
      Habitat.MEADOW      , 3f,
      Habitat.BARRENS     , 2f,
      Habitat.DUNE        , 1f
    );
    final Stage world = new Stage(TG.generateTerrain());
    TG.setupMinerals(world, 0.6f, 0, 0.2f);
    world.terrain().readyAllMeshes();
    Flora.populateFlora(world);
    return world;
  }
  
  
  protected Base createBase(Stage world) {
    return Base.settlement(world, "Player Base", Faction.FACTION_TAYGETA);
  }
  
  
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.setDefaults();
    GameSettings.buildFree = true;
    GameSettings.fogFree   = true;
    GameSettings.paveFree  = true;
    GameSettings.cashFree  = true;
    base.research.initKnowledgeFrom(base.commerce.homeworld());
    
    if (true ) shippingScenario(world, base, UI);
    if (false) shoppingScenario(world, base, UI);
    if (false) runnersScenario (world, base, UI);
    if (false) purchaseScenario(world, base, UI);
    if (false) deliveryScenario(world, base, UI);
    if (false) haulingScenario (world, base, UI);
  }
  
  
  private void shippingScenario(Stage world, Base base, BaseUI UI) {
    
    base .commerce.assignHomeworld    (Verse.PLANET_ASRA_NOVI);
    world.offworld.assignStageLocation(Verse.SECTOR_PAVONIS  );
    
    final Venue depot = new SupplyDepot(base);
    SiteUtils.establishVenue(depot, 5, 5, true, world);
    depot.stocks.forceDemand(CARBS , 0, 5);
    depot.stocks.forceDemand(METALS, 5, 0);
    depot.stocks.bumpItem(CARBS, 10);
    depot.updateAsScheduled(0, false);
    base.commerce.addCandidate(SUPPLY_CORPS, depot);
    Selection.pushSelection(depot, null);
    
    final Actor brought = new Human(SURVEYOR, base);
    world.offworld.journeys.addLocalImmigrant(brought, base);
    
    base.commerce.updateCommerce(0);
    world.offworld.journeys.setupDefaultShipping(base);
    world.offworld.journeys.scheduleLocalDrop(base, 5);
  }
  
  
  private void shoppingScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.hireFree = true;
    
    Venue runnerMarket = new RunnerMarket(base);
    SiteUtils.establishVenue(runnerMarket, 5, 5, true, world);
    base.setup.fillVacancies(runnerMarket, true);
    
    Venue trooperLodge = new TrooperLodge(base);
    SiteUtils.establishVenue(trooperLodge, 10, 5, true, world);
    base.setup.fillVacancies(trooperLodge, true);
    
    Venue medicalBay = new PhysicianStation(base);
    SiteUtils.establishVenue(medicalBay, 5, 10, true, world);
    base.setup.fillVacancies(medicalBay, true);

    runnerMarket.stocks.bumpItem(REAGENTS, 5);
    medicalBay  .stocks.bumpItem(MEDICINE, 5);
    
    for (Mobile m : base.allUnits()) if (m instanceof Actor) {
      final Actor a = (Actor) m;
      a.gear.incCredits(2000);
      a.gear.taxDone();
      if (a.mind.vocation() == TROOPER) Selection.pushSelection(a, null);
    }
    
    /*
    //  Create one settlement over here, with a supply depot, engineer station
    //  and fabricator.
    final Venue depot = new SupplyDepot(base);
    SiteUtils.establishVenue(depot, 5, 5, true, world);
    
    final Venue engineer = new EngineerStation(base);
    SiteUtils.establishVenue(engineer, 5, 5, true, world);
    final Venue fabricator = new Fabricator(base);
    SiteUtils.establishVenue(fabricator, 5, 5, true, world);
    final Venue reactor = new Generator(base);
    SiteUtils.establishVenue(reactor, 5, 5, true, world);
    
    //  Create another settlement over here, with a stock exchange, archives
    //  and physician station.
    final Venue exchange = new StockExchange(base);
    SiteUtils.establishVenue(exchange, 25, 25, true, world);
    
    final Venue archives = new Archives(base);
    SiteUtils.establishVenue(archives, 25, 25, true, world);
    final Venue physician = new PhysicianStation(base);
    SiteUtils.establishVenue(physician, 25, 25, true, world);
    final Venue condensor = new Condensor(base);
    SiteUtils.establishVenue(condensor, 25, 25, true, world);
    
    
    for (Mobile m : world.allMobiles()) if (m instanceof Actor) {
      final Actor a = (Actor) m;
      if (a.base() == base) {
        a.gear.incCredits(2000);
        a.gear.taxDone();
      }
    }
    for (Object o : world.presences.matchesNear(base(), null, -1)) {
      final Venue v = (Venue) o;
      if (v instanceof Holding) continue;
      
      for (Traded t : v.stocks.demanded()) {
        v.stocks.bumpItem(t, 100, 100);
      }
    }

    PlayLoop.setGameSpeed(1);
    //*/
  }
  
  
  private void runnersScenario(Stage world, Base base, BaseUI UI) {
    //world.advanceCurrentTime(Stage.STANDARD_DAY_LENGTH / 2);
    world.offworld.journeys.scheduleLocalDrop(base, 5);
    
    final Actor runner = new Human(Backgrounds.RUNNER, base);
    final Venue runnerMarket = new RunnerMarket(base);
    SiteUtils.establishVenue(runnerMarket, 10,  5, true, world, runner);
    
    final Actor vendor = new Human(Backgrounds.STOCK_VENDOR, base);
    final Venue looted = new StockExchange(base);
    for (Traded t : Economy.ALL_FOOD_TYPES) {
      looted.stocks.bumpItem(t, 10);
    }
    SiteUtils.establishVenue(looted, 5, 10, true, world, vendor);
    
    final SupplyCache cache = new SupplyCache();
    cache.enterWorldAt(15, 15, world, true);
    cache.inventory().bumpItem(Economy.DECOR, 10);
    
    runnerMarket.stocks.bumpItem(Economy.DECOR, 20);
    runnerMarket.stocks.bumpItem(Economy.ANTIMASS, 20);
    
    //  TODO:  RESTORE THIS!
    /*
    final Item moved[] = base.commerce.getBestCargo(
      runnerMarket.stocks, 5, false
    );
    final Dropship ship = base.commerce.allVessels().atIndex(0);
    final Smuggling smuggle = new Smuggling(runner, runnerMarket, ship, moved);
    smuggle.addMotives(Plan.MOTIVE_JOB, Plan.ROUTINE);
    runner.mind.assignBehaviour(smuggle);
    runner.goAboard(world.tileAt(13, 13), world);
    //*/
    
    //  TODO:  Now, all you have to work out is the selection of services and
    //  manufacture of contraband.
    //  TODO:  Consider having a sub-class who provide those services?
    
    Selection.pushSelection(runner, null);
    //  TODO:  Set up initial relationships...
  }
  
  
  private void purchaseScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.needsFree = true;
    GameSettings.techsFree = true;
    
    /*
    I.say("\nSETTING UP SCENARIO!");
    I.say("  Homeworld:     "+base.commerce.homeworld());
    VerseLocation homeW = base.commerce.homeworld();
    Upgrade houses  = Holding.BLUEPRINT.baseUpgrade();
    Upgrade known[] = homeW.knowledge().toArray(Upgrade.class);
    for (Upgrade u : known) {
      I.say("    Knows of: "+u);
    }
    I.say("  Housing is:    "+houses);
    
    boolean hasTech = base.research.hasTheory(houses);
    I.say("  Has tech theory? "+hasTech);
    boolean canSite = base.setup.hasSitePermission(Holding.BLUEPRINT);
    I.say("  Has site permission? "+canSite);
    //*/
    
    
    final Venue foundry = new EngineerStation(base);
    SiteUtils.establishVenue(foundry, 6, 6, true, world);
    base.setup.fillVacancies(foundry, true);
    
    foundry.stocks.bumpItem(Economy.METALS  , 10);
    foundry.stocks.bumpItem(Economy.PARTS   , 10);
    foundry.stocks.bumpItem(Economy.POLYMER , 10);
    foundry.stocks.bumpItem(Economy.PLASTICS, 10);
    foundry.structure.setUpgradeLevel(EngineerStation.WEAPONS_WORKSHOP, 2);
    foundry.structure.setUpgradeLevel(EngineerStation.ARMOUR_FOUNDRY  , 2);
    
    final Venue reactor = new Generator(base);
    SiteUtils.establishVenue(reactor, 3, 6, true, world);
    base.setup.fillVacancies(reactor, true);
    reactor.stocks.bumpItem(FUEL_RODS, 10);
    
    Actor buys = new Human(Backgrounds.RUNNER, base);
    buys.enterWorldAt(12, 12, world);
    buys.gear.incCredits(1000);
    buys.gear.taxDone();
    buys.gear.equipDevice(Item.withQuality(Devices.CARBINE, 1));
    
    buys = new Human(Backgrounds.TROOPER, base);
    buys.enterWorldAt(10, 12, world);
    buys.gear.incCredits(2000);
    buys.gear.taxDone();
    buys.gear.equipDevice(Item.withQuality(Devices.HALBERD_GUN, 4));
    buys.skills.addTechnique(TrooperTechniques.POWER_ARMOUR_USE);
    
    Selection.pushSelection(buys, null);
  }
  
  
  private void haulingScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.hireFree = true;
    
    final Venue depot    = new SupplyDepot  (base);
    SiteUtils.establishVenue(depot   , 50, 50, true, world);
    base.setup.fillVacancies(depot, true);
    depot.stocks.bumpItem(PARTS   , 80);
    depot.stocks.bumpItem(PLASTICS, 35);
    depot.stocks.bumpItem(CARBS   , 45);
    
    final Venue exchange = new StockExchange(base);
    SiteUtils.establishVenue(exchange, 5 , 5 , true, world);
    base.setup.fillVacancies(exchange, true);
    exchange.stocks.forceDemand(PARTS   , 40, 0);
    exchange.stocks.forceDemand(PLASTICS, 25, 0);
    exchange.stocks.forceDemand(CARBS   , 35, 0);
    Selection.pushSelection(depot, null);
  }
  
  
  private void deliveryScenario(Stage world, Base base, BaseUI UI) {
    final Venue depot   = new SupplyDepot(base);
    final Venue foundry = new EngineerStation(base);
    final Actor
      guyA = new Human(Backgrounds.TECHNICIAN, base),
      guyB = new Human(Backgrounds.TECHNICIAN, base);
    
    SiteUtils.establishVenue(depot, 11, 1, true, world);
    SiteUtils.establishVenue(foundry, 6, 6, true, world, guyA, guyB);
    
    depot.stocks.bumpItem(METALS, 10);
    foundry.stocks.forceDemand(METALS, 3, 0);
    
    Selection.pushSelection(foundry, null);
    
    for (Actor guy : foundry.staff.workers()) {
      final Bringing d = BringUtils.fillBulkOrder(
        depot, foundry, new Traded[] {METALS}, 1, 10, true
      );
      if (d == null) continue;
      guy.mind.assignBehaviour(d);
      guy.setPosition(2, 2, world);
    }
    
    world.offworld.journeys.scheduleLocalDrop(base, 20);
  }
  
  
  protected void afterCreation() {
  }
}








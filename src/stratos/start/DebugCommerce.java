/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.start;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.civic.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.game.plans.*;
import stratos.graphics.common.Colour;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;



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
      Habitat.ESTUARY     , 2f,
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
    return Base.settlement(world, "Player Base", Colour.BLUE);
  }
  
  
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.setDefaults();
    GameSettings.buildFree = true;
    GameSettings.fogFree   = true;
    GameSettings.paveFree  = true;
    
    //  TODO:  Try giving the residents pots of money instead...
    //GameSettings.freeHousingLevel = 0;
    
    if (false) shippingScenario(world, base, UI);
    if (false) shoppingScenario(world, base, UI);
    if (false) runnersScenario (world, base, UI);
    if (true ) purchaseScenario(world, base, UI);
    if (false) deliveryScenario(world, base, UI);
    if (false) haulingScenario (world, base, UI);
    if (false) shoppingScenario(world, base, UI);
  }
  
  
  private void shippingScenario(Stage world, Base base, BaseUI UI) {
    
    final Venue depot = new SupplyDepot(base);
    Placement.establishVenue(depot, 5, 5, true, world);
    depot.stocks.forceDemand(CARBS, 5, true );
    depot.stocks.forceDemand(ORES , 5, false);
    
    depot.stocks.bumpItem(CARBS, 10);
    depot.updateAsScheduled(0, false);
    
    final Actor brought = new Human(Backgrounds.KOMMANDO, base);
    world.offworld.addImmigrant(brought, world);  //  TODO:  SPECIFY BASE
    
    base.commerce.updateCommerce(0);
    base.commerce.scheduleDrop(5);
    
    UI.selection.pushSelection(depot);
    
    //  TODO:  Check to ensure that distribution from depots to facilities, and
    //  shopping at the stock exchange, are working correctly!
    
    //UI.selection.pushSelection(base.commerce.allVessels().first());
  }
  
  
  private void runnersScenario(Stage world, Base base, BaseUI UI) {
    //world.advanceCurrentTime(Stage.STANDARD_DAY_LENGTH / 2);
    base.commerce.scheduleDrop(5);
    
    final Actor runner = new Human(Backgrounds.RUNNER_SILVERFISH, base);
    final Venue runnerMarket = new RunnerLodge(base);
    Placement.establishVenue(runnerMarket, 10,  5, true, world, runner);
    
    final Actor vendor = new Human(Backgrounds.STOCK_VENDOR, base);
    final Venue looted = new StockExchange(base);
    for (Traded t : Economy.ALL_FOOD_TYPES) {
      looted.stocks.bumpItem(t, 10);
    }
    Placement.establishVenue(looted, 5, 10, true, world, vendor);
    
    final SupplyCache cache = new SupplyCache();
    cache.enterWorldAt(15, 15, world);
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
    
    UI.selection.pushSelection(runner);
    //  TODO:  Set up initial relationships...
  }
  
  
  private void purchaseScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.needsFree = true;
    
    Actor citizen = null;
    for (int n = 2; n-- > 0;) {
      citizen = new Human(Backgrounds.RUNNER_HUDZENA, base);
      citizen.enterWorldAt(world.tileAt(10 + n, 10 + n), world);
      citizen.gear.incCredits(1000);
    }
    UI.selection.pushSelection(citizen);
    
    final Venue foundry = new EngineerStation(base);
    Placement.establishVenue(foundry, 6, 6, true, world);
    base.setup.fillVacancies(foundry, true);
    foundry.stocks.bumpItem(Economy.ORES , 10);
    foundry.stocks.bumpItem(Economy.PARTS, 2 );
    
    final Venue reactor = new Reactor(base);
    Placement.establishVenue(reactor, 3, 6, true, world);
    base.setup.fillVacancies(reactor, true);
    reactor.stocks.bumpItem(ISOTOPES, 10);
  }
  
  
  private void haulingScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.hireFree = true;
    
    final Venue depot    = new SupplyDepot  (base);
    Placement.establishVenue(depot   , 50, 50, true, world);
    base.setup.fillVacancies(depot, true);
    depot.stocks.bumpItem(PARTS   , 80);
    depot.stocks.bumpItem(PLASTICS, 35);
    depot.stocks.bumpItem(CARBS   , 45);
    
    final Venue exchange = new StockExchange(base);
    Placement.establishVenue(exchange, 5 , 5 , true, world);
    base.setup.fillVacancies(exchange, true);
    exchange.stocks.forceDemand(PARTS   , 40, false);
    exchange.stocks.forceDemand(PLASTICS, 25, false);
    exchange.stocks.forceDemand(CARBS   , 35, false);
    
    UI.selection.pushSelection(depot);
  }
  
  
  private void deliveryScenario(Stage world, Base base, BaseUI UI) {
    final Venue depot   = new SupplyDepot(base);
    final Venue foundry = new EngineerStation(base);
    final Actor
      guyA = new Human(Backgrounds.TECHNICIAN, base),
      guyB = new Human(Backgrounds.TECHNICIAN, base);
    
    Placement.establishVenue(depot, 11, 1, true, world);
    Placement.establishVenue(foundry, 6, 6, true, world, guyA, guyB);
    
    depot.stocks.bumpItem(ORES, 10);
    foundry.stocks.forceDemand(ORES, 3, false);
    
    UI.selection.pushSelection(foundry);
    
    for (Actor guy : foundry.staff.workers()) {
      final Delivery d = DeliveryUtils.fillBulkOrder(
        depot, foundry, new Traded[] {ORES}, 1, 10
      );
      if (d == null) continue;
      d.setWithPayment(foundry, false);
      guy.mind.assignBehaviour(d);
      guy.setPosition(2, 2, world);
    }
    
    base.commerce.scheduleDrop(20);
  }
  
  
  private void shoppingScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.hireFree = true;
    
    //  Create one settlement over here, with a supply depot, engineer station
    //  and fabricator.
    final Venue depot = new SupplyDepot(base);
    Placement.establishVenue(depot, 5, 5, true, world);
    
    final Venue engineer = new EngineerStation(base);
    Placement.establishVenue(engineer, 5, 5, true, world);
    final Venue fabricator = new Fabricator(base);
    Placement.establishVenue(fabricator, 5, 5, true, world);
    final Venue reactor = new Reactor(base);
    Placement.establishVenue(reactor, 5, 5, true, world);
    
    //  Create another settlement over here, with a stock exchange, archives
    //  and physician station.
    final Venue exchange = new StockExchange(base);
    Placement.establishVenue(exchange, 25, 25, true, world);
    
    final Venue archives = new Archives(base);
    Placement.establishVenue(archives, 25, 25, true, world);
    final Venue physician = new PhysicianStation(base);
    Placement.establishVenue(physician, 25, 25, true, world);
    final Venue condensor = new Condensor(base);
    Placement.establishVenue(condensor, 25, 25, true, world);
    
    
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
  }
  
  
  public void updateGameState() {
    super.updateGameState();
    
    if (base().finance.credits() < 1000) {
      base().finance.incCredits(500, BaseFinance.SOURCE_CHARITY);
    }
  }
  
  
  protected void afterCreation() {
  }
}








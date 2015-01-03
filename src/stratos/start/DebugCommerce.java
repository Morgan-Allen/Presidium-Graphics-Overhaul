/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.start;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.base.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.game.plans.*;
import stratos.game.politic.*;
import stratos.graphics.common.Colour;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;



public class DebugCommerce extends Scenario {
  
  
  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new DebugCommerce());
  }
  
  
  private DebugCommerce() {
    super();
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
  
  
  protected String saveFilePrefix(Stage world, Base base) {
    return "debug_commerce";
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
    return Base.withName(world, "Player Base", Colour.BLUE);
  }
  
  
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.setDefaults();
    GameSettings.buildFree = true;
    GameSettings.fogFree   = true;
    GameSettings.paveFree  = true;
    //GameSettings.noChat    = true;
    
    //  TODO:  Try giving the residents pots of money instead...
    //GameSettings.freeHousingLevel = 0;
    
    if (false) shoppingScenario(world, base, UI);
    if (false) runnersScenario (world, base, UI);
    if (false) shippingScenario(world, base, UI);
    if (true ) deliveryScenario(world, base, UI);
  }
  
  
  private void shippingScenario(Stage world, Base base, BaseUI UI) {
    
    /*
    final Airfield airfield = new Airfield(base);
    Placement.establishVenue(airfield, 5, 5, true, world);
    airfield.setTrading(CARBS  , Airfield.TRADE_IMPORT, 10);
    airfield.setTrading(PROTEIN, Airfield.TRADE_IMPORT, 5 );
    airfield.updateAsScheduled(0, false);
    //*/
    final Actor brought = new Human(Backgrounds.KOMMANDO, base);
    world.offworld.addImmigrant(brought, world);  //  TODO:  SPECIFY BASE
    
    base.commerce.updateCommerce(0);
    base.commerce.scheduleDrop(5);
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
    final Item moved[] = base.commerce.getBestCargo(
      runnerMarket.stocks, 5, false
    );
    final Dropship ship = base.commerce.allVessels().atIndex(0);
    final Smuggling smuggle = new Smuggling(runner, runnerMarket, ship, moved);
    smuggle.setMotive(Plan.MOTIVE_DUTY, Plan.ROUTINE);
    runner.mind.assignBehaviour(smuggle);
    runner.goAboard(world.tileAt(13, 13), world);
    
    //  TODO:  Now, all you have to work out is the selection of services and
    //  manufacture of contraband.
    //  TODO:  Consider having a sub-class who provide those services?
    
    UI.selection.pushSelection(runner, true);
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
    UI.selection.pushSelection(citizen, true);
    
    final Venue foundry = new EngineerStation(base);
    Placement.establishVenue(
      foundry, 6, 6, true, world,
      new Human(Backgrounds.TECHNICIAN, base),
      new Human(Backgrounds.TECHNICIAN, base),
      new Human(Backgrounds.ARTIFICER, base)
    );
    foundry.stocks.bumpItem(Economy.ORES , 40);
    foundry.stocks.bumpItem(Economy.PARTS, 20);
    
  }
  
  
  private void deliveryScenario(Stage world, Base base, BaseUI UI) {
    final Venue depot = new SupplyDepot(base);
    final Venue foundry = new EngineerStation(base);
    final Actor citizen = new Human(Backgrounds.TECHNICIAN, base);
    Placement.establishVenue(depot, 11, 1, true, world);
    Placement.establishVenue(foundry, 6, 6, true, world, citizen);
    depot.stocks.bumpItem(ORES, 10);
    
    final Delivery d = new Delivery(ORES, depot, foundry);
    d.setWithPayment(foundry, false);
    citizen.mind.assignBehaviour(d);
    citizen.setPosition(2, 2, world);
    
    UI.selection.pushSelection(citizen, true);
  }
  
  
  private void shoppingScenario(Stage world, Base base, BaseUI UI) {
    
    final Venue archives = new Archives(base);
    Placement.establishVenue(archives, 10, 5, true, world);
  }

  
  public void updateGameState() {
    super.updateGameState();
  }
  
  
  protected void afterCreation() {
  }
}








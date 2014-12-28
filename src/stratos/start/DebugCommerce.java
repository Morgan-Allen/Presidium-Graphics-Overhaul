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
    return Base.baseWithName(world, "Player Base", false);
  }
  
  
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.setDefaults();
    GameSettings.buildFree = true;
    GameSettings.fogFree   = true;
    GameSettings.paveFree  = true;
    //GameSettings.noChat    = true;
    
    //  TODO:  Try giving the residents pots of money instead...
    //GameSettings.freeHousingLevel = 0;
    
    if (false) archivesScenario(world, base, UI);
    if (false) runnersScenario (world, base, UI);
    if (true ) shippingScenario(world, base, UI);
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
  
  
  private void archivesScenario(Stage world, Base base, BaseUI UI) {
    
    final Venue archives = new Archives(base);
    Placement.establishVenue(archives, 10, 5, true, world);
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

  
  public void updateGameState() {
    super.updateGameState();
  }
  
  
  protected void afterCreation() {
  }
}








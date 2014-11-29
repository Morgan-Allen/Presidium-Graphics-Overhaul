

package stratos.start;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.base.*;
import stratos.game.campaign.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.game.plans.*;
import stratos.user.*;
import stratos.util.*;



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
    GameSettings.hireFree  = true;
    GameSettings.buildFree = true;
    GameSettings.fogFree   = true;
    GameSettings.paveFree  = true;
    //GameSettings.noChat    = true;
    
    //  TODO:  Try giving the residents pots of money instead...
    //GameSettings.freeHousingLevel = 0;
    
    if (false) archivesScenario(world, base, UI);
    if (true ) runnersScenario (world, base, UI);
  }
  
  
  private void archivesScenario(Stage world, Base base, BaseUI UI) {
    
    final Venue archives = new Archives(base);
    Placement.establishVenue(archives, 10, 5, true, world);
  }
  
  
  private void runnersScenario(Stage world, Base base, BaseUI UI) {
    world.advanceCurrentTime(Stage.STANDARD_DAY_LENGTH / 2);
    
    final Actor runner = new Human(Backgrounds.RUNNER, base);
    final Venue runnerMarket = new RunnerLodge(base);
    Placement.establishVenue(runnerMarket, 10,  5, true, world, runner);
    
    final Venue looted = new StockExchange(base);
    for (Traded t : looted.services()) {
      looted.stocks.bumpItem(t, 10);
    }
    Placement.establishVenue(looted, 5, 10, true, world);
    
    final Looting loots = new Looting(
      runner, looted, Item.withAmount(Economy.GREENS, 1)
    );
    loots.setMotive(Plan.MOTIVE_LEISURE, Plan.PARAMOUNT);
    runner.mind.assignBehaviour(loots);
    UI.selection.pushSelection(runner, true);
    
    //  TODO:  Set up initial relationships!
  }

  
  public void updateGameState() {
    super.updateGameState();
  }
  
  
  protected void afterCreation() {
  }
}








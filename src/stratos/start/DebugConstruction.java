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



public class DebugConstruction extends Scenario {
  
  
  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new DebugConstruction());
  }
  
  
  private DebugConstruction() {
    super("debug_building", true);
  }
  
  
  public DebugConstruction(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }

  
  public void beginGameSetup() {
    super.initScenario("debug_building");
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
    GameSettings.fogFree   = true;
    GameSettings.hireFree  = true;
    
    if (true ) buildingScenario(world, base, UI);
  }
  
  
  private void buildingScenario(Stage world, Base base, BaseUI UI) {
    
    final Venue depot = new EngineerStation(base);
    Placement.establishVenue(depot, 5, 5, true, world);
    depot.stocks.bumpItem(PARTS, 20);
    depot.updateAsScheduled(0, false);
    base.setup.fillVacancies(depot, true);
    Actor tracks = depot.staff.workers().last();
    UI.selection.pushSelection(tracks);
    
    //base.commerce.updateCommerce(0);
    //base.commerce.scheduleDrop(5);
    //UI.selection.pushSelection(depot);
    
    Venue built = new SupplyDepot(base);
    Placement.establishVenue(built, depot, false, world);
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








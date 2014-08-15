

package stratos.start;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.base.*;
import stratos.game.campaign.*;
import stratos.game.maps.*;
import stratos.user.*;



public class DebugPlacing extends Scenario {
  
  
  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new DebugPlacing());
  }
  
  
  private DebugPlacing() {
    super();
  }
  
  
  public DebugPlacing(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  protected World createWorld() {
    final TerrainGen TG = new TerrainGen(
      64, 0.2f,
      Habitat.ESTUARY     , 2f,
      Habitat.MEADOW      , 3f,
      Habitat.BARRENS     , 2f,
      Habitat.DUNE        , 1f
    );
    final World world = new World(TG.generateTerrain());
    TG.setupMinerals(world, 0.6f, 0, 0.2f);
    world.terrain().readyAllMeshes();
    Flora.populateFlora(world);
    return world;
  }
  
  
  protected Base createBase(World world) {
    return Base.baseWithName(world, "Player Base", false);
  }
  
  
  protected String saveFilePrefix(World world, Base base) {
    return "debug_placing";
  }
  
  
  protected void configureScenario(World world, Base base, BaseUI UI) {
    GameSettings.setDefaults();
    
    //final PlacementGrid placeMap = new PlacementGrid(world);
    //placeMap.createLattice();
    
    //  TODO:  Create a Botanical Station, and see how long it takes for a
    //  plantation to be established.
    
    final EcologistStation station = new EcologistStation(base);
    Placement.establishVenue(station, 8, 8, true, world);
  }
  
  
  protected void afterCreation() {
  }
}







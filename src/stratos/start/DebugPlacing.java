

package stratos.start;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.base.*;
import stratos.game.campaign.*;
import stratos.game.maps.*;
import stratos.graphics.common.Rendering;
import stratos.graphics.widgets.KeyInput;
import stratos.user.*;
import stratos.util.*;





//  Get mines working.
//  Set up exclusion zone around kommando lodge, ecologist station, and
//  excavation shaft.

//  Divide venue description into two panes- one for general status, and the
//  other for specific sub-headings.  Don't bother with spill-over.

//  Rework art for roads, the shield wall, the physician station, the engineer
//  station, the polymer fab, the solar bank, and the archives.

//  Re-introduce the polymer fab.
//  Re-work the culture vats- produce either food/organs OR soma/reagents.
//  Produce medicine at the physician station.
//  Test datalink-production at the archives.
//  Test study behaviour at the archives.

//  Fix bug with stripping/paving of roads happening at same time.
//  Fix bug with resting in random locations.

//  Remove combat from wandering behaviours (make part of exploration, not
//  patrolling.)






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
  
  
  public void beginGameSetup() {
    super.initScenario("debug_placing");
  }
  
  
  protected String saveFilePrefix(World world, Base base) {
    return "debug_placing";
  }
  
  
  public void renderVisuals(Rendering rendering) {
    super.renderVisuals(rendering);
    
    final Tile over = UI().selection.pickedTile();
    if (KeyInput.wasTyped('p')) {
      I.say("TILE IS: "+over);
      I.say("  SHOULD PAVE? "+base().paving.map.needsPaving(over));
    }
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
  
  
  protected void configureScenario(World world, Base base, BaseUI UI) {
    GameSettings.setDefaults();
    GameSettings.hireFree  = true;
    GameSettings.buildFree = true;
    GameSettings.fogFree   = true;
    
    /*
    final EcologistStation station = new EcologistStation(base);
    Placement.establishVenue(station, 8, 8, true, world);
    
    final Plantation site = new Plantation(base);
    site.setPosition(8, 15, world);
    if (site.canPlace()) site.doPlacement();
    //*/
  }
  
  
  protected void afterCreation() {
  }
}











package stratos.start;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.base.*;
import stratos.game.campaign.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.graphics.common.Rendering;
import stratos.graphics.widgets.KeyInput;
import stratos.user.*;
import stratos.util.*;




//  Set up exclusion zone around kommando lodge, ecologist station, and
//  excavation shaft.  Get surface mines and plantations working inside.

//  Fix bug with stripping/paving of roads happening at same time.  It doesn't
//  show up often, so just keep an eye out for it.

//  There's still a bug with offworld trade- fuel rods etc. get delivered to a
//  depot, then delivered straight back again.  Revise FRSD options.

//  Re-introduce the polymer fab.
//  Re-work the culture lab- produce either food/organs OR soma/reagents.
//  Produce medicine at the physician station.
//  Test datalink-production at the archives.
//  Test study behaviour at the archives.
//  Introduce the Air Field.

//  Have solar banks primarily exist as water supply, not power per se.  The
//  reactor complex can provide most of that.
//  Get rid of the Drill Yard.  It's not working out for now.

//  Divide venue description into two panes- one for general status, and the
//  other for specific sub-headings.  Don't bother with spill-over.

//  Rework art for roads, the shield wall, the physician station, the engineer
//  station, the polymer fab, the solar bank, and the archives.







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
  
  
  public void updateGameState() {
    super.updateGameState();
  }
  
  
  public void renderVisuals(Rendering rendering) {
    super.renderVisuals(rendering);
    
    final Tile over = UI().selection.pickedTile();
    if (KeyInput.wasTyped('p')) {
      I.say("TILE IS: "+over);
      I.say("  SHOULD PAVE? "+base().paveRoutes.map.needsPaving(over));
    }
  }


  protected World createWorld() {
    final TerrainGen TG = new TerrainGen(
      32, 0.2f,
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
    GameSettings.paveFree  = true;
    //GameSettings.fogFree   = true;
    
    if (false) configRoadsTest(world, base, UI);
    if (false) configMinesTest(world, base, UI);
    if (true ) configPlantTest(world, base, UI);
  }
  
  
  private void configRoadsTest(World world, Base base, BaseUI UI) {
    
    final Venue pointA = new TrooperLodge(base);
    final Venue pointB = new TrooperLodge(base);
    Placement.establishVenue(pointA, 5, 5 , false, world);
    Placement.establishVenue(pointB, 5, 15, false, world);
    pointA.updateAsScheduled(0);
    pointB.updateAsScheduled(0);
    
    for (int n = 2; n-- > 0;) {
      final Human tech = new Human(Backgrounds.TECHNICIAN, base);
      tech.enterWorldAt(5, 10, world);
      final Plan roadBuild = new RoadsRepair(tech, tech.origin());
      roadBuild.setMotive(Plan.MOTIVE_DUTY, 100);
      tech.mind.assignBehaviour(roadBuild);
      UI.selection.pushSelection(tech, true);
    }
  }
  
  
  private void configMinesTest(World world, Base base, BaseUI UI) {
    final ExcavationSite station = new ExcavationSite(base);
    final Human worksA, worksB;
    Placement.establishVenue(
      station, 8, 8, true, world,
      worksA = new Human(Backgrounds.EXCAVATOR, base),
      worksB = new Human(Backgrounds.EXCAVATOR, base)
    );
    worksA.goAboard(station.mainEntrance(), world);
    worksB.goAboard(station.mainEntrance(), world);
    
    UI.selection.pushSelection(worksB, true);
  }
  
  
  private void configPlantTest(World world, Base base, BaseUI UI) {
    final EcologistStation station = new EcologistStation(base);
    Placement.establishVenue(station, 8, 8, true, world);
    
    final Nursery site = new Nursery(base);
    site.setPosition(8, 15, world);
    if (site.canPlace()) site.doPlacement();
  }
  
  
  protected void afterCreation() {
  }
}









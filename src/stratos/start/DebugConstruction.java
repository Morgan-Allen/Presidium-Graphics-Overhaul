/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.start;
import stratos.content.civic.*;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.game.plans.*;
import stratos.game.verse.*;
import stratos.user.*;
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
    return Base.settlement(world, "Player Base", Faction.FACTION_PROCYON);
  }
  
  
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.setDefaults();
    GameSettings.fogFree   = true;
    GameSettings.cashFree  = true;
    
    if (true ) buildingScenario(world, base, UI);
    if (false) pavingScenario  (world, base, UI);
  }
  
  
  private void buildingScenario(Stage world, Base base, BaseUI UI) {
    
    final Venue depot = new EngineerStation(base);
    SiteUtils.establishVenue(depot, 5, 5, true, world);
    depot.stocks.bumpItem(PARTS, 20);
    depot.updateAsScheduled(0, false);
    base.setup.fillVacancies(depot, true);
    
    final Actor tracks = depot.staff.workers().last();
    Selection.pushSelection(tracks, null);
    
    final Outcrop boulder = new Outcrop(3, 1, Outcrop.TYPE_MESA);
    boulder.enterWorldAt(13, 13, world, true);
    
    final Outcrop dune = new Outcrop(1, 1, Outcrop.TYPE_DUNE);
    dune.enterWorldAt(12, 13, world, true);
    
    final Venue built = new TrooperLodge(base);
    built.setupWith(world.tileAt(12, 12), null);
    built.doPlacement(false);
    
    final Repairs job = new Repairs(tracks, built, true);
    job.addMotives(Plan.MOTIVE_JOB, Plan.PARAMOUNT);
    tracks.mind.assignBehaviour(job);
  }
  
  
  private void pavingScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.buildFree = true;
  }
  
  
  protected void afterCreation() {
  }
}











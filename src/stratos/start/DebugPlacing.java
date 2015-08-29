/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.start;
import stratos.content.civic.*;
import stratos.content.wip.*;
import stratos.game.actors.*;
import stratos.game.base.BaseResearch;
import stratos.game.base.Verse;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.game.wild.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.graphics.terrain.*;
import stratos.user.*;
import stratos.util.*;



public class DebugPlacing extends Scenario {
  
  
  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new DebugPlacing());
  }
  
  
  private DebugPlacing() {
    super("debug_placing", true);
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
  
  
  public void renderVisuals(Rendering rendering) {
    super.renderVisuals(rendering);
    
    final Tile over = UI().selection.pickedTile();
    if (KeyInput.wasTyped('p')) {
      I.say("TILE IS: "+over);
      I.say("  SHOULD PAVE? "+base().transport.map.needsPaving(over));
      I.say("  Owned:       "+over.reserved());
      I.say("  Owned by:    "+over.reserves());
      I.say("  Above is:    "+over.above());
      I.say("  Owning tier: "+over.owningTier());
    }
  }


  protected Stage createWorld() {
    final TerrainGen TG = new TerrainGen(
      64, 0.2f,
      //Habitat.OCEAN       , 1f,
      Habitat.ESTUARY     , 2f,
      Habitat.MEADOW      , 3f,
      Habitat.BARRENS     , 2f,
      Habitat.DUNE        , 1f
    );
    final Stage world = new Stage(TG.generateTerrain());
    TG.setupMinerals(world, 0.6f, 0, 0.2f);
    TG.setupOutcrops(world);
    Flora.populateFlora(world);
    world.terrain().readyAllMeshes();
    return world;
  }
  
  
  protected Base createBase(Stage world) {
    return Base.settlement(world, "Player Base", Colour.BLUE);
  }
  
  
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.setDefaults();
    GameSettings.fogFree   = true;
    GameSettings.cashFree  = true;
    GameSettings.buildFree = true;
    base.research.initKnowledgeFrom(Verse.PLANET_HALIBAN);
    
    if (false) configEcology  (world, base, UI);
    if (false) configPerimTest(world, base, UI);
    if (false) configTradeTest(world, base, UI);
    if (false) configRoadsTest(world, base, UI);
    if (false) configMinesTest(world, base, UI);
    if (true ) configPlantTest(world, base, UI);
  }
  
  
  private void configEcology(Stage world, Base base, BaseUI UI) {
    
    Flora.populateFlora(world);
    //Ruins.populateRuins(world, 2, Drone.SPECIES, Tripod.SPECIES);
    
    I.say("\nCHECKING FOR TILE-ACCESS...");
    for (Tile t : world.tilesIn(world.area(), false)) {
      if (t.blocked()) continue;
      boolean open = false;
      for (Tile n : t.edgeAdjacent(null)) if (n != null && !n.blocked()) {
        open = true;
      }
      if (! open) I.say("  TILE SURROUNDED: "+t);
    }
    
    Nest.populateFauna(world, Qudu.SPECIES, Hareen.SPECIES, Lictovore.SPECIES);
    
    //  TODO:  Either automate the inhabitant-displacement step, or perform
    //  inhabitant-introduction in a later step.  Think about general history-
    //  simulation.
    
    final Bastion b = new Bastion(base);
    base.setup.doPlacementsFor(b);
    
    if (b.inWorld()) for (Venue v : world.claims.venuesConflicting(b)) {
      for (Actor a : v.staff.lodgers()) a.exitWorld();
      v.exitWorld();
    }
  }
  
  
  private void configPerimTest(Stage world, Base base, BaseUI UI) {
    Venue v = null;
    
    v = new EngineerStation(base);
    SiteUtils.establishVenue(v, 4, 3, true, world);
    
    v = new EngineerStation(base);
    SiteUtils.establishVenue(v, 4, 9, true, world);
    
    v = new EngineerStation(base);
    SiteUtils.establishVenue(v, 4, 6, true, world);
  }
  
  
  private void configTradeTest(Stage world, Base base, BaseUI UI) {
    
    final Venue depot = new SupplyDepot(base);
    SiteUtils.establishVenue(depot, 5 , 5 , true, world);
    depot.updateAsScheduled(0, false);
    
    final Venue works = new Fabricator(base);
    SiteUtils.establishVenue(works, 5 , 10, true, world);
    works.updateAsScheduled(0, false);
    
    base.commerce.updateCommerce(0);
    world.offworld.journeys.scheduleLocalDrop(base, 5);
  }
  
  
  private void configRoadsTest(Stage world, Base base, BaseUI UI) {
    
    final Venue pointA = new TrooperLodge(base);
    final Venue pointB = new TrooperLodge(base);
    SiteUtils.establishVenue(pointA, 5, 5 , true, world);
    SiteUtils.establishVenue(pointB, 5, 15, true, world);
    pointA.updateAsScheduled(0, false);
    pointB.updateAsScheduled(0, false);
    
    for (int n = 2; n-- > 0;) {
      final Human tech = new Human(Backgrounds.TECHNICIAN, base);
      tech.enterWorldAt(5, 10, world);
      final Plan roadBuild = new RoadsRepair(tech, tech.origin());
      roadBuild.addMotives(Plan.MOTIVE_JOB, 100);
      tech.mind.assignBehaviour(roadBuild);
      UI.selection.pushSelection(tech);
    }
  }
  
  
  private void configMinesTest(Stage world, Base base, BaseUI UI) {
    
    GameSettings.buildFree = true;
    GameSettings.paveFree  = true;
    GameSettings.hireFree  = true;
    
    final ExcavationSite station = new ExcavationSite(base);
    final Human worksA, worksB;
    SiteUtils.establishVenue(
      station, 8, 8, true, world,
      worksA = new Human(Backgrounds.EXCAVATOR, base),
      worksB = new Human(Backgrounds.EXCAVATOR, base)
    );
    worksA.goAboard(station.mainEntrance(), world);
    worksB.goAboard(station.mainEntrance(), world);
    
    UI.selection.pushSelection(worksB);
  }
  
  
  private void configPlantTest(Stage world, Base base, BaseUI UI) {
    
    GameSettings.buildFree = true;
    GameSettings.paveFree  = true;
    GameSettings.hireFree  = true;
    
    final BotanicalStation station = new BotanicalStation(base);
    SiteUtils.establishVenue(station, 8, 8, true, world);
    for (Species s : Crop.ALL_VARIETIES) {
      final Item seed = Item.with(Economy.GENE_SEED, s, 1, 4);
      station.stocks.addItem(seed);
    }
    base.setup.fillVacancies(station, true);
    
    final Nursery site = new Nursery(base);
    final SitingPass pass = new SitingPass(base, site);
    //pass.isVerbose = true;
    pass.placeState = SitingPass.PLACE_INTACT;
    pass.performFullPass();
  }
  
  
  protected void afterCreation() {
  }
  
  
  /**  Utility method for visual-debugging of large-scale pathfinding:
    */
  public static void showZonePathing() {
    final Base base = BaseUI.currentPlayed();
    if (base == null) return;
    final boolean talks = I.used60Frames;
    
    final Stage world = base.world;
    final PathingCache cache = world.pathingCache;
    final Object selected = BaseUI.current().selection.selected  ();
    final Object hovered  = BaseUI.current().selection.hovered   ();
    final Tile   picked   = BaseUI.current().selection.pickedTile();
    
    
    if (hovered instanceof Boarding && selected instanceof Boarding) {
      
      final Boarding path[] = cache.getLocalPath(
        (Boarding) selected, (Boarding) hovered, -1, null, talks
      );
      if (Visit.empty(path)) return;
      
      final Batch <Tile> inPath = new Batch();
      for (Boarding b : path) if (b instanceof Tile) inPath.add((Tile) b);
      if (inPath.size() == 0) return;
      
      final TerrainChunk forPath = world.terrain().createOverlay(
        world, inPath.toArray(Tile.class), true, Image.TRANSLUCENT_WHITE
      );
      forPath.colour = Colour.MAGENTA;
      forPath.readyFor(PlayLoop.rendering());
    }
    else {
      final Tile inPlace[] = picked == null ? null : cache.placeTiles(picked);
      if (inPlace == null) return;
      
      final TerrainChunk forPlace = world.terrain().createOverlay(
        world, inPlace, true, Image.TRANSLUCENT_WHITE
      );
      forPlace.colour = Colour.RED;
      forPlace.readyFor(PlayLoop.rendering());
      
      final Tile routes[][] = cache.placeRoutes(picked);
      if (routes == null) return;
      
      final Batch <Tile> inRoutes = new Batch();
      for (Tile route[] : cache.placeRoutes(picked)) for (Tile t : route) {
        inRoutes.add(t);
      }
      final TerrainChunk forRoutes = world.terrain().createOverlay(
        world, inRoutes.toArray(Tile.class), true, Image.TRANSLUCENT_WHITE
      );
      forRoutes.colour = Colour.BLUE;
      forRoutes.readyFor(PlayLoop.rendering());
    }
  }
  
}

















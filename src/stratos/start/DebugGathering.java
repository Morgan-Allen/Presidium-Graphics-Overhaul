/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.start;
import stratos.content.civic.*;
import stratos.content.wip.*;
import stratos.game.actors.*;
import stratos.game.base.*;
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
import static stratos.game.economic.Economy.*;



public class DebugGathering extends Scenario {
  
  
  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new DebugGathering());
  }
  
  
  private DebugGathering() {
    super("debug_gathering", true);
  }
  
  
  public DebugGathering(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  public void beginGameSetup() {
    super.initScenario("debug_gathering");
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
    world.terrain().readyAllMeshes();
    return world;
  }
  
  
  protected Base createBase(Stage world) {
    return Base.settlement(world, "Player Base", Colour.BLUE);
  }
  
  
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.setDefaults();
    GameSettings.buildFree = true;
    GameSettings.paveFree  = true;
    GameSettings.hireFree  = true;
    GameSettings.fogFree   = true;
    GameSettings.cashFree  = true;
    base.research.initKnowledgeFrom(Verse.PLANET_HALIBAN);
    
    if (false) configFarmsTest   (world, base, UI);
    if (false) configForestryTest(world, base, UI);
    if (false) configLoggingTest (world, base, UI);
    if (false) configForageTest  (world, base, UI);
    if (false) configBrowseTest  (world, base, UI);
    if (false) configSampleTest  (world, base, UI);
    if (true ) configMiningTest  (world, base, UI);
    if (false) configFormingTest (world, base, UI);
  }
  
  
  private void configFarmsTest(Stage world, Base base, BaseUI UI) {
    Flora.populateFlora(world);
    
    final BotanicalStation station = new BotanicalStation(base);
    SiteUtils.establishVenue(station, 8, 8, true, world);
    for (Species s : Crop.ALL_VARIETIES) {
      final Item seed = Item.with(Economy.GENE_SEED, s, 1, 4);
      station.stocks.addItem(seed);
    }
    base.setup.fillVacancies(station, true);
  }
  
  
  private void configForestryTest(Stage world, Base base, BaseUI UI) {
    final FormerBay former = new FormerBay(base);
    SiteUtils.establishVenue(former, 8, 8, true, world);
    base.setup.fillVacancies(former, true);
  }
  
  
  private void configLoggingTest(Stage world, Base base, BaseUI UI) {
    Flora.populateFlora(world);
    final FormerBay former = new FormerBay(base);
    SiteUtils.establishVenue(former, 8, 8, true, world);
    base.setup.fillVacancies(former, true);
  }
  
  
  private void configForageTest(Stage world, Base base, BaseUI UI) {
    Flora.populateFlora(world);
    
    final Base natives = Base.natives(world, NativeHut.TRIBE_FOREST);
    final Venue hut = NativeHut.newHut(NativeHut.TRIBE_FOREST, base);
    SiteUtils.establishVenue(hut, 8, 8, true, world);
    natives.setup.fillVacancies(hut, true);
    
    for (Actor a : hut.staff.lodgers()) {
      a.health.setCaloryLevel(0.2f);
    }
  }
  
  
  private void configBrowseTest(Stage world, Base base, BaseUI UI) {
    Flora.populateFlora(world);
    
    final Base wildlife = Base.wildlife(world);
    final Nest n = (Nest) Qudu.SPECIES.nestBlueprint().createVenue(wildlife);
    SiteUtils.establishVenue(n, 8, 8, true, world);
    wildlife.setup.fillVacancies(n, true);
    
    final Fauna first = (Fauna) n.staff.lodgers().first();
    if (first != null) {
      first.health.setCaloryLevel(0.2f);
      UI.selection.pushSelection(first);
    }
  }
  
  
  private void configSampleTest(Stage world, Base base, BaseUI UI) {
    Flora.populateFlora(world);
    
    final BotanicalStation station = new BotanicalStation(base);
    SiteUtils.establishVenue(station, 8, 8, true, world);
    for (Species s : Crop.ALL_VARIETIES) {
      final Item seed = Item.with(Economy.GENE_SEED, s, 1, 4);
      station.stocks.addItem(seed);
    }
    base.setup.fillVacancies(station, true);
  }
  
  
  private void configFormingTest(Stage world, Base base, BaseUI UI) {
    
  }
  
  
  private void configMiningTest(Stage world, Base base, BaseUI UI) {
    
    Flora.populateFlora(world);
    
    final ExcavationSite site = new ExcavationSite(base);
    SiteUtils.establishVenue(site, 8, 8, true, world);
    base.setup.fillVacancies(site, true);
    site.stocks.addItem(Item.with(SLAG, METALS, 25, 0));
    
    /*
    for (Tile t : site.reserved()) {
      world.terrain().punchTerrainTo(-2, t);
    }
    //*/
    
    final Actor first = site.staff.workers().first();
    final Mining dumps = Mining.asDumping(first, site);
    dumps.addMotives(Plan.MOTIVE_JOB, 10);
    first.mind.assignBehaviour(dumps);
    
    UI.selection.pushSelection(first);
  }
  
  
  protected void afterCreation() {
  }
}

















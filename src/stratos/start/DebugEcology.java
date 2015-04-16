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




public class DebugEcology extends Scenario {
  
  
  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new DebugEcology());
  }
  
  
  private DebugEcology() {
    super("debug_ecology", true);
  }
  
  
  public DebugEcology(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }

  
  public void beginGameSetup() {
    super.initScenario("debug_ecology");
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
    GameSettings.hireFree  = true;
    GameSettings.buildFree = true;
    GameSettings.fogFree   = true;
    GameSettings.paveFree  = true;
    GameSettings.noChat    = true;
    
    if (false) configHuntingScenario(world, base, UI);
    if (true ) nestPlaceTest        (world, base, UI);
  }
  
  
  private void configHuntingScenario(Stage world, Base base, BaseUI UI) {
    final Base wildlife = Base.wildlife(world);
    
    final Actor hunts = Lictovore.SPECIES.sampleFor(wildlife);
    hunts.enterWorldAt(3, 3, world);
    hunts.health.setCaloryLevel(0.5f);
    
    final Actor prey = Qudu.SPECIES.sampleFor(wildlife);
    prey.health.setupHealth(1, 1, 0);
    prey.enterWorldAt(world.tileAt(6, 6), world);
    prey.health.takeFatigue(prey.health.maxHealth());
    
    hunts.mind.assignBehaviour(Hunting.asFeeding(hunts, prey));
    UI.selection.pushSelection(hunts);
  }
  
  
  private void nestPlaceTest(Stage world, Base base, BaseUI UI) {
    
    final Outcrop o = new Outcrop(2, 2, Outcrop.TYPE_MESA);
    o.setPosition(10, 10, world);
    o.attachModel(Outcrop.modelFor(o, world));
    o.enterWorld();
    
    final Base wildlife = Base.wildlife(world);
    final Nest n = (Nest) Qudu.SPECIES.nestProfile().sampleVenue(wildlife);
    n.setPosition(10, 8, world);
    
    I.say("Placement possible? "+n.canPlace());
    
    n.enterWorld();
    n.structure.setState(Structure.STATE_INTACT, 1);
    wildlife.setup.fillVacancies(n, true);
  }
  
  
  public void updateGameState() {
    super.updateGameState();
  }
  
  
  protected void afterCreation() {
  }
}














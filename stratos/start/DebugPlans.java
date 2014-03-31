


package stratos.start;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.campaign.*;
import stratos.game.planet.*;
import stratos.game.base.*;
import stratos.user.*;



public class DebugPlans extends Scenario {
  

  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new DebugPlans());
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
    //TG.setupMinerals(world, 0, 0, 0);
    //TG.setupOutcrops(world);
    world.terrain().readyAllMeshes();
    //Flora.populateFlora(world);
    return world;
  }
  
  
  protected Base createBase(World world) {
    return Base.baseWithName(world, "Player Base", false);
  }
  
  
  protected void configureScenario(World world, Base base, BaseUI UI) {
    GameSettings.fogFree = true;
    
    final Actor treats = new Human(Background.PHYSICIAN, base);
    Placement.establishVenue(
      new Sickbay(base), 6, 6, true, world,
      treats,
      new Human(Background.MEDIC, base),
      new Human(Background.MEDIC, base)
    );
    
    final Actor patient = new Human(Background.VETERAN, base);
    patient.enterWorldAt(world.tileAt(10, 10), world);
    patient.health.takeInjury(patient.health.maxHealth());
    
    UI.selection.pushSelection(patient, true);
  }
  
  
  protected String saveFilePrefix(World world, Base base) {
    return "debug_plans";
  }
  
  
  protected void afterCreation() {
  }
}







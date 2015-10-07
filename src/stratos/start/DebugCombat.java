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
import stratos.graphics.common.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;
import stratos.content.abilities.PhysicianTechniques;



public class DebugCombat extends Scenario {
  
  
  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new DebugCombat());
  }
  
  
  private DebugCombat() {
    super("debug_combat", true);
  }
  
  
  public DebugCombat(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }

  
  public void beginGameSetup() {
    super.initScenario("debug_combat");
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
    world.terrain().readyAllMeshes();
    return world;
  }
  
  
  protected Base createBase(Stage world) {
    return Base.settlement(world, "Player Base", Colour.BLUE);
  }
  
  
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.setDefaults();
    GameSettings.hireFree  = true;
    GameSettings.buildFree = true;
    GameSettings.paveFree  = true;
    GameSettings.noChat    = true;
    
    if (false) raidingScenario (world, base, UI);
    if (true ) combatScenario  (world, base, UI);
  }
  
  
  public void updateGameState() {
    super.updateGameState();
  }
  
  
  private void combatScenario(Stage world, Base base, BaseUI UI) {

    Actor soldier = null;
    for (int n = 2; n-- > 0;) {
      soldier = new Human(Backgrounds.TROOPER, base);
      soldier.enterWorldAt(4, 4, world);
    }
    
    Actor support = null;
    for (int n = 1; n-- > 0;) {
      support = new Human(Backgrounds.PHYSICIAN, base);
      support.enterWorldAt(1, 1, world);
      
      support.gear.bumpItem(MEDICINE, 2);
      support.skills.addTechnique(PhysicianTechniques.HYPO_SPRAY    );
      support.skills.addTechnique(PhysicianTechniques.BOOSTER_SHOT  );
      support.skills.addTechnique(PhysicianTechniques.PAX_9         );
      support.skills.addTechnique(PhysicianTechniques.PSY_INHIBITION);
    }
    
    final Base wildlife = Base.wildlife(world);
    final Actor avrodil = Avrodil.SPECIES.sampleFor(wildlife);
    
    avrodil.health.setMaturity(0.8f);
    avrodil.health.setFatigueLevel(0.15f);
    avrodil.enterWorldAt(9, 9, world, true);
    avrodil.mind.assignBehaviour(new Combat(avrodil, soldier));
    
    base.intelMap.liftFogAround(support, 9);
    UI.selection.pushSelection(support);
  }
  
  
  private void raidingScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.fogFree  = true;
    GameSettings.hireFree = true;
    world.advanceCurrentTime(Stage.STANDARD_DAY_LENGTH * 0.3f);
    
    //  Introduce a bastion, with standard personnel.
    final Bastion bastion = new Bastion(base);
    SiteUtils.establishVenue(bastion, 11, 11, true, world);
    base.setup.fillVacancies(bastion, true);
    
    //  And introduce ruins, with a complement of artilects.
    final Base artilects = Base.artilects(world);
    artilects.relations.setRelation(base, -0.5f, true);
    
    final Ruins ruins = new Ruins(artilects);
    SiteUtils.establishVenue(ruins, 44, 44, true, world);
    final float healthLevel = (1 + Rand.avgNums(2)) / 2;
    ruins.structure.setState(Structure.STATE_INTACT, healthLevel);
    artilects.setup.doPlacementsFor(ruins);
    artilects.setup.fillVacancies(ruins, true);
    
    UI.selection.pushSelection(ruins.staff.workers().first());
  }
  
  
  protected void afterCreation() {
  }
}








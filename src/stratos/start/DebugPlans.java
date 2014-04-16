


package stratos.start;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.campaign.*;
import stratos.game.planet.*;
import stratos.game.base.*;
import stratos.game.tactical.*;
import stratos.game.civilian.*;
import stratos.game.wild.*;
import stratos.user.*;
import stratos.util.*;



public class DebugPlans extends Scenario {
  

  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new DebugPlans());
  }
  
  
  private DebugPlans() {
    super();
  }
  
  
  public DebugPlans(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  public void beginGameSetup() {
    super.initScenario("debug_plans");
  }
  
  
  protected String saveFilePrefix(World world, Base base) {
    return "debug_plans";
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
    world.terrain().readyAllMeshes();
    //Flora.populateFlora(world);
    return world;
  }
  
  
  protected Base createBase(World world) {
    return Base.baseWithName(world, "Player Base", false);
  }
  
  
  protected void configureScenario(World world, Base base, BaseUI UI) {
    GameSettings.setDefaults();
    //configMedicalScenario(world, base, UI);
    //configHuntingScenario(world, base, UI);
    //configCombatScenario(world, base, UI);
    //configDialogueScenario(world, base, UI);
    //configPurchaseScenario(world, base, UI);
    configRaidScenario(world, base, UI);
  }
  
  
  public void updateGameState() {
    super.updateGameState();
    if (base().credits() < 0) base().incCredits(100);
  }
  
  
  private void configMedicalScenario(World world, Base base, BaseUI UI) {
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
  
  
  private void configHuntingScenario(World world, Base base, BaseUI UI) {
    //GameSettings.fogFree = true;
    GameSettings.buildFree = true;
    //GameSettings.noBlood = true;
    
    final Actor hunts = new Human(Background.SURVEY_SCOUT, base);
    final Venue station = new SurveyStation(base);
    Placement.establishVenue(
      station, 6, 6, true, world,
      new Human(Background.SURVEY_SCOUT, base),
      new Human(Background.SURVEY_SCOUT, base),
      hunts
    );
    
    final Base wildlife = Base.baseWithName(world, Base.KEY_WILDLIFE, true);
    final Actor prey = new Vareen(wildlife);
    prey.enterWorldAt(world.tileAt(9, 9), world);
    
    //prey.health.takeFatigue(prey.health.maxHealth());
    //hunts.mind.assignBehaviour(Hunting.asHarvest(hunts, prey, station));
    UI.selection.pushSelection(hunts, true);
    
    Nest.placeNests(world, Species.HAREEN, Species.QUD);
  }
  
  
  private void configCombatScenario(World world, Base base, BaseUI UI) {
    GameSettings.fogFree = true;
    //GameSettings.noBlood = true;
    
    Actor soldier = null;
    for (int n = 1; n-- > 0;) {
      soldier = new Human(Background.KNIGHTED, base);
      soldier.enterWorldAt(world.tileAt(4, 4), world);
    }
    
    final Actor civilian = new Human(Background.STOCK_VENDOR, base);
    civilian.enterWorldAt(world.tileAt(5, 4), world);
    civilian.health.takeInjury(civilian.health.maxHealth() * 2);
    
    final Base artilects = Base.baseWithName(world, Base.KEY_ARTILECTS, true);
    final Actor threat = new Tripod(artilects);
    threat.enterWorldAt(world.tileAt(8, 6), world);
    
    UI.selection.pushSelection(threat, true);
  }
  
  
  private void configDialogueScenario(World world, Base base, BaseUI UI) {
    GameSettings.fogFree = true;
    GameSettings.noBlood = true;
    
    Actor citizen = null;
    for (int n = 3; n-- > 0;) {
      citizen = new Human(Background.CULTIVATOR, base);
      citizen.enterWorldAt(world.tileAt(4 + n, 4 + n), world);
    }
    UI.selection.pushSelection(citizen, true);
  }
  
  
  private void configPurchaseScenario(World world, Base base, BaseUI UI) {
    GameSettings.needsFree = true;
    
    Actor citizen = null;
    for (int n = 2; n-- > 0;) {
      citizen = new Human(Background.RUNNER, base);
      citizen.enterWorldAt(world.tileAt(10 + n, 10 + n), world);
      citizen.gear.incCredits(1000);
    }
    UI.selection.pushSelection(citizen, true);

    final Venue foundry = new Foundry(base);
    Placement.establishVenue(
      foundry, 6, 6, true, world,
      new Human(Background.TECHNICIAN, base),
      new Human(Background.TECHNICIAN, base),
      new Human(Background.ARTIFICER, base)
    );
    foundry.stocks.bumpItem(Economy.METALS, 40);
    foundry.stocks.bumpItem(Economy.PARTS, 20);
  }
  
  
  private void configRaidScenario(World world, Base base, BaseUI UI) {
    GameSettings.fogFree = true;
    GameSettings.hireFree = true;
    GameSettings.noBlood = true;
    
    //  Introduce a bastion, with standard personnel.
    final Bastion bastion = new Bastion(base);
    Placement.establishVenue(bastion, 11, 11, true, world);
    
    //  And introduce ruins, with a complement of artilects.
    final Ruins ruins = new Ruins();
    Placement.establishVenue(ruins, 44, 44, true, world);
    final Batch <Artilect> pop = Ruins.populateArtilects(world, ruins, true);
    
    UI.selection.pushSelection(pop.first(), true);
  }
  
  
  protected void afterCreation() {
  }
}












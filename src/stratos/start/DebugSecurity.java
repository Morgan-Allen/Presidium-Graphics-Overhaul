/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.start;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.base.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.game.plans.*;
import stratos.game.politic.*;
import stratos.graphics.common.Colour;
import stratos.user.*;
import stratos.util.*;




public class DebugSecurity extends Scenario {
  
  
  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new DebugSecurity());
  }
  
  
  private DebugSecurity() {
    super();
  }
  
  
  public DebugSecurity(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }

  
  public void beginGameSetup() {
    super.initScenario("debug_security");
  }
  
  
  protected String saveFilePrefix(Stage world, Base base) {
    return "debug_security";
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
    return Base.withName(world, "Player Base", Colour.BLUE);
  }
  
  
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.setDefaults();
    GameSettings.hireFree  = true;
    GameSettings.buildFree = true;
    GameSettings.fogFree   = true;
    GameSettings.paveFree  = true;
    GameSettings.noChat    = true;
    
    if (false) breedingScenario(world, base, UI);
    if (false) arrestScenario  (world, base, UI);
    if (true ) animalScenario  (world, base, UI);
  }
  
  
  private void animalScenario(Stage world, Base base, BaseUI UI) {
    //  TODO:  Introduce some animals near actors, and guage their respective
    //  reactions.
  }
  
  
  private void breedingScenario(Stage world, Base base, BaseUI UI) {
    final Actor ecologist = new Human(Backgrounds.ECOLOGIST, base);
    final Venue station = new EcologistStation(base);
    Placement.establishVenue(station, 10, 10, true, world, ecologist);
    
    station.stocks.bumpItem(Economy.CARBS  , 5);
    station.stocks.bumpItem(Economy.PROTEIN, 5);
    
    final AnimalBreeding breeding = AnimalBreeding.breedingFor(
      ecologist, station, Species.HAREEN, world.tileAt(25, 25)
    );
    station.stocks.addItem(Item.withAmount(breeding.asSeed(), 0.95f));
    
    breeding.setMotive(Plan.MOTIVE_DUTY, Plan.CASUAL);
    ecologist.mind.assignBehaviour(breeding);
    UI.selection.pushSelection(ecologist, true);
  }
  
  
  private void arrestScenario(Stage world, Base base, BaseUI UI) {
    final Actor runner = new Human(Backgrounds.RUNNER_SILVERFISH, base);
    final Venue runnerMarket = new RunnerLodge(base);
    Placement.establishVenue(runnerMarket, 10,  5, true, world, runner);
    
    final Actor vendor = new Human(Backgrounds.STOCK_VENDOR, base);
    final Venue looted = new StockExchange(base);
    for (Traded t : Economy.ALL_FOOD_TYPES) {
      looted.stocks.bumpItem(t, 10);
    }
    Placement.establishVenue(looted, 5, 10, true, world, vendor);

    final Looting loots = new Looting(
      runner, looted, Item.withAmount(Economy.GREENS, 1), runnerMarket
    );
    loots.setMotive(Plan.MOTIVE_EMERGENCY, Plan.ROUTINE);
    runner.mind.assignBehaviour(loots);
    runner.goAboard(looted.mainEntrance(), world);
    
    final Actor enforcer = new Human(Backgrounds.ENFORCER, base);
    final Venue enforcerBloc = new EnforcerBloc(base);
    Placement.establishVenue(enforcerBloc, 5, 20, true, world, enforcer);
    
    final Arrest arrests = new Arrest(enforcer, runner);
    enforcer.mind.assignBehaviour(arrests);
    UI.selection.pushSelection(enforcer, true);
  }
  
  
  private void configHuntingScenario(Stage world, Base base, BaseUI UI) {
    //GameSettings.fogFree = true;
    GameSettings.buildFree = true;
    //GameSettings.noBlood = true;
    
    final Actor hunts = new Human(Backgrounds.KOMMANDO, base);
    final Venue station = new KommandoLodge(base);
    Placement.establishVenue(
      station, 6, 6, true, world,
      new Human(Backgrounds.KOMMANDO, base),
      new Human(Backgrounds.KOMMANDO, base),
      hunts
    );
    
    final Base wildlife = Base.wildlife(world);
    final Actor prey = new Vareen(wildlife);
    prey.enterWorldAt(world.tileAt(9, 9), world);
    
    //prey.health.takeFatigue(prey.health.maxHealth());
    //hunts.mind.assignBehaviour(Hunting.asHarvest(hunts, prey, station));
    UI.selection.pushSelection(hunts, true);
    
    Nest.placeNests(world, Species.HAREEN, Species.QUDU);
  }
  
  
  private void configCombatScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.fogFree = true;
    //GameSettings.noBlood = true;
    
    Actor soldier = null;
    for (int n = 1; n-- > 0;) {
      soldier = new Human(Backgrounds.KNIGHTED, base);
      soldier.enterWorldAt(world.tileAt(4, 4), world);
    }
    
    final Actor civilian = new Human(Backgrounds.STOCK_VENDOR, base);
    civilian.enterWorldAt(world.tileAt(5, 4), world);
    civilian.health.takeInjury(civilian.health.maxHealth() * 2, true);
    
    final Base artilects = Base.artilects(world);
    final Actor threat = new Tripod(artilects);
    threat.enterWorldAt(world.tileAt(8, 6), world);
    
    UI.selection.pushSelection(threat, true);
  }
  
  
  private void configRaidScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.fogFree = false;
    //GameSettings.hireFree = true;
    
    //  Introduce a bastion, with standard personnel.
    final Bastion bastion = new Bastion(base);
    Placement.establishVenue(bastion, 11, 11, true, world);
    base.setup.fillVacancies(bastion, true);
    
    //  And introduce ruins, with a complement of artilects.
    final Base artilects = Base.artilects(world);
    final Ruins ruins = new Ruins(artilects);
    Placement.establishVenue(ruins, 44, 44, true, world);
    final float healthLevel = (1 + Rand.avgNums(2)) / 2;
    ruins.structure.setState(Structure.STATE_INTACT, healthLevel);
    
    Base.artilects(world).setup.doPlacementsFor(ruins);
    UI.selection.pushSelection(bastion, true);
  }
  
  
  public void updateGameState() {
    super.updateGameState();
  }
  
  
  protected void afterCreation() {
  }
}








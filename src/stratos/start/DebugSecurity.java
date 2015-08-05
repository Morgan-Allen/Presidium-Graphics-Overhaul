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
import stratos.game.wild.*;
import stratos.game.plans.*;
import stratos.graphics.common.Colour;
import stratos.user.*;
import stratos.util.*;




public class DebugSecurity extends Scenario {
  
  
  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new DebugSecurity());
  }
  
  
  private DebugSecurity() {
    super("debug_security", true);
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
    ///Flora.populateFlora(world);
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
    
    if (false) wallsScenario   (world, base, UI);
    if (true ) verminScenario  (world, base, UI);
    if (false) breedingScenario(world, base, UI);
    if (false) arrestScenario  (world, base, UI);
    if (false) raidingScenario (world, base, UI);
    if (false) combatScenario  (world, base, UI);
  }
  
  
  private void wallsScenario(Stage world, Base base, BaseUI UI) {
    final Bastion bastion = new Bastion(base);
    Flora.populateFlora(world);
    //
    //  Now, find a good location for the Bastion, and establish some walls
    //  around it...
    base.setup.doPlacementsFor(bastion);
    
    final Venue walls[] = SiteUtils.placeAroundPerimeter(
      ShieldWall.BLUEPRINT, bastion.areaClaimed(), base, true
    );
    for (Venue v : walls) ((ShieldWall) v).updateFacing(true);
    
    base.finance.setInitialFunding(100000, 0);
  }
  
  
  private void verminScenario(Stage world, Base base, BaseUI UI) {
    
    base.commerce.assignHomeworld(Verse.PLANET_AXIS_NOVENA);
    final Venue hatch = new Heighway(base);
    SiteUtils.establishVenue(hatch, world.tileAt(4, 4), true, world);
    
    final Base vermin = Base.vermin(world);
    /*
    Actor roach = Roach.SPECIES.sampleFor(vermin);
    roach.enterWorldAt(hatch, world);
    roach.mind.setHome(hatch);
    //*/
    Actor enemy = Roachman.SPECIES.sampleFor(vermin);
    enemy.enterWorldAt(hatch, world);
    enemy.mind.setHome(hatch);
    
    Venue raids = new StockExchange(base);
    raids.stocks.bumpItem(Economy.CARBS, 20);
    SiteUtils.establishVenue(raids, world.tileAt(20, 20), true, world);
    
    enemy.mind.assignBehaviour(new Looting(
      enemy, raids, Item.withAmount(Economy.CARBS, 5), hatch
    ).addMotives(Plan.MOTIVE_JOB, Plan.ROUTINE));
    
    //*
    Actor meets = new Human(Backgrounds.VOLUNTEER, base);
    meets.enterWorldAt(6, 6, world);
    
    meets.mind.assignBehaviour(Patrolling.aroundPerimeter(
      meets, raids, world
    ).addMotives(Plan.MOTIVE_JOB, Plan.ROUTINE));
    //*/
    
    UI.selection.pushSelection(meets);
  }
  
  
  private void breedingScenario(Stage world, Base base, BaseUI UI) {
    final Actor ecologist = new Human(Backgrounds.ECOLOGIST, base);
    final Venue station = new BotanicalStation(base);
    SiteUtils.establishVenue(station, 10, 10, true, world, ecologist);
    
    station.stocks.bumpItem(Economy.CARBS  , 5);
    station.stocks.bumpItem(Economy.PROTEIN, 5);
    
    /*
    final AnimalTending breeding = AnimalTending.breedingFor(
      ecologist, station, Hareen.SPECIES, world.tileAt(25, 25)
    );
    station.stocks.addItem(Item.withAmount(breeding.asSeed(), 0.95f));
    breeding.addMotives(Plan.MOTIVE_JOB, Plan.CASUAL);
    ecologist.mind.assignBehaviour(breeding);
    //*/
    
    UI.selection.pushSelection(ecologist);
  }
  
  
  private void arrestScenario(Stage world, Base base, BaseUI UI) {
    final Actor runner = new Human(Backgrounds.RUNNER_SILVERFISH, base);
    final Venue runnerMarket = new RunnerMarket(base);
    SiteUtils.establishVenue(runnerMarket, 10,  5, true, world, runner);
    
    final Actor vendor = new Human(Backgrounds.STOCK_VENDOR, base);
    final Venue looted = new StockExchange(base);
    for (Traded t : Economy.ALL_FOOD_TYPES) {
      looted.stocks.bumpItem(t, 10);
    }
    SiteUtils.establishVenue(looted, 5, 10, true, world, vendor);

    final Looting loots = new Looting(
      runner, looted, Item.withAmount(Economy.GREENS, 1), runnerMarket
    );
    loots.addMotives(Plan.MOTIVE_EMERGENCY, Plan.ROUTINE);
    runner.mind.assignBehaviour(loots);
    runner.goAboard(looted.mainEntrance(), world);
    
    final Actor enforcer = new Human(Backgrounds.ENFORCER, base);
    final Venue enforcerBloc = new EnforcerBloc(base);
    SiteUtils.establishVenue(enforcerBloc, 5, 20, true, world, enforcer);
    
    final Arrest arrests = new Arrest(enforcer, runner);
    enforcer.mind.assignBehaviour(arrests);
    UI.selection.pushSelection(enforcer);
  }
  
  
  private void combatScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.fogFree = true;
    //GameSettings.noBlood = true;
    
    Actor soldier = null;
    for (int n = 4; n-- > 0;) {
      soldier = new Human(Backgrounds.TROOPER, base);
      soldier.enterWorldAt(world.tileAt(4, 4), world);
    }
    
    /*
    final Actor civilian = new Human(Backgrounds.STOCK_VENDOR, base);
    civilian.enterWorldAt(world.tileAt(5, 4), world);
    civilian.health.takeInjury(civilian.health.maxHealth() * 2, true);
    //*/
    
    final Base artilects = Base.artilects(world);
    final Actor threat = new Tripod(artilects);
    threat.enterWorldAt(world.tileAt(8, 6), world);
    //UI.selection.pushSelection(threat, true);
    
    UI.selection.pushSelection(soldier);
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
  
  
  public void updateGameState() {
    super.updateGameState();
  }
  
  
  protected void afterCreation() {
  }
}








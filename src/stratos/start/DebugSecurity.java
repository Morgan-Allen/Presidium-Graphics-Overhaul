/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.start;
import static stratos.content.hooks.StratosSetting.SECTOR_ELYSIUM;

import stratos.content.civic.*;
import stratos.content.hooks.StratosSetting;
import stratos.content.wip.*;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.game.plans.*;
import stratos.game.verse.*;
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
      Habitat.FOREST      , 2f,
      Habitat.MEADOW      , 3f,
      Habitat.BARRENS     , 2f,
      Habitat.DUNE        , 1f
    );
    final Verse verse = new StratosSetting();
    final Sector at = SECTOR_ELYSIUM;
    final Stage world = Stage.createNewWorld(verse, at, TG.generateTerrain());
    TG.setupMinerals(world, 0.6f, 0, 0.2f);
    ///Flora.populateFlora(world);
    world.readyAfterPopulation();
    return world;
  }
  
  
  protected Base createBase(Stage world) {
    return Base.settlement(world, "Player Base", Faction.FACTION_ALTAIR);
  }
  
  
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.setDefaults();
    GameSettings.hireFree  = true;
    GameSettings.buildFree = true;
    GameSettings.fogFree   = true;
    GameSettings.paveFree  = true;
    GameSettings.noChat    = true;

    if (true ) verminScenario  (world, base, UI);
    if (false) wallsScenario   (world, base, UI);
    if (false) breedingScenario(world, base, UI);
    if (false) arrestScenario  (world, base, UI);
    if (false) raidingScenario (world, base, UI);
    if (false) combatScenario  (world, base, UI);
  }
  
  
  private void verminScenario(Stage world, Base base, BaseUI UI) {
    
    base.visits.assignHomeworld(StratosSetting.PLANET_AXIS_NOVENA);
    final Venue hatch = new ServiceHatch(base);
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
    
    //*
    Venue raids = new StockExchange(base);
    raids.stocks.bumpItem(Economy.CARBS, 20);
    SiteUtils.establishVenue(raids, world.tileAt(20, 20), true, world);
    
    enemy.mind.assignBehaviour(new Looting(
      enemy, raids, Item.withAmount(Economy.CARBS, 5), hatch
    ).addMotives(Plan.MOTIVE_JOB, Plan.ROUTINE));
    //*/
    
    //*
    Actor meets = new Human(Backgrounds.VOLUNTEER, base);
    meets.enterWorldAt(18, 27, world);
    
    meets.mind.assignBehaviour(Patrolling.protectionFor(
      meets, raids, Plan.ROUTINE
    ));
    //*/
    
    Selection.pushSelection(enemy, null);
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
  
  
  private void breedingScenario(Stage world, Base base, BaseUI UI) {
    final Actor ecologist = new Human(Backgrounds.ECOLOGIST, base);
    final Venue station = new EcologistStation(base);
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
    
    Selection.pushSelection(ecologist, null);
  }
  
  
  private void arrestScenario(Stage world, Base base, BaseUI UI) {
    final Actor runner = new Human(Backgrounds.RUNNER, base);
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
    Selection.pushSelection(enforcer, null);
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
    //Selection.pushSelection(threat, true);
    
    Selection.pushSelection(soldier, null);
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
    Faction.setMutualFactionRelations(artilects, base, -0.5f);
    
    final Ruins ruins = new Ruins(artilects);
    SiteUtils.establishVenue(ruins, 44, 44, true, world);
    final float healthLevel = (1 + Rand.avgNums(2)) / 2;
    ruins.structure.setState(Structure.STATE_INTACT, healthLevel);
    artilects.setup.doPlacementsFor(ruins);
    artilects.setup.fillVacancies(ruins, true);
    
    Selection.pushSelection(ruins.staff.workers().first(), null);
  }
  
  
  public void updateGameState() {
    super.updateGameState();
  }
  
  
  protected void afterCreation() {
  }
}








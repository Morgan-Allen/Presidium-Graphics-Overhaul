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
    return Base.baseWithName(world, "Player Base", false);
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

  
  public void updateGameState() {
    super.updateGameState();
  }
  
  
  protected void afterCreation() {
  }
}








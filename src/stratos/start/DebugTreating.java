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
import stratos.graphics.common.Colour;
import stratos.user.*;
import stratos.util.*;



public class DebugTreating extends Scenario {
  
  
  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new DebugTreating());
  }
  
  
  private DebugTreating() {
    super("debug_treating", true);
  }
  
  
  public DebugTreating(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }

  
  public void beginGameSetup() {
    super.initScenario("debug_treating");
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
    GameSettings.buildFree = true;
    GameSettings.fogFree   = true;
    GameSettings.paveFree  = true;
    
    if (false) configTreatmentScenario(world, base, UI);
    if (true ) configFirstAidScenario (world, base, UI);
    if (false) configRecoveryScenario (world, base, UI);
  }
  
  
  private void configTreatmentScenario(Stage world, Base base, BaseUI UI) {
    
    final Actor treats = new Human(Backgrounds.PHYSICIAN, base);
    final Venue station = new PhysicianStation(base);
    SiteUtils.establishVenue(station, 4, 4, true, world, treats);
    
    final Venue vats = new CultureVats(base);
    SiteUtils.establishVenue(vats, 10, 4, true, world);
    
    final Actor treated = new Human(Backgrounds.EXCAVATOR, base);
    treated.enterWorldAt(4, 10, world);
    
    //  And now, we proceed to do various horrible things to the subject...
    treated.traits.incLevel(Condition.HIREX_PARASITE, 0.99f);
    
    Selection.pushSelection(treats, null);
  }
  
  
  private void configFirstAidScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.fogFree = true;
    
    final Actor treats = new Human(Backgrounds.PHYSICIAN, base);
    SiteUtils.establishVenue(
      new PhysicianStation(base), 8, 8, true, world,
      treats,
      new Human(Backgrounds.MINDER, base),
      new Human(Backgrounds.MINDER, base)
    );
    
    final Batch <ShieldWall> walls = new Batch();
    for (int x = 16; x > 4; x-= 2) {
      final ShieldWall s = new ShieldWall(base);
      s.setPosition(x, 2, world);
      walls.add(s);
    }
    PlacingTask.performPlacements(walls, null, base);
    
    final Actor patient = new Human(Backgrounds.TROOPER, base);
    patient.enterWorldAt(walls.atIndex(2), world);
    patient.health.takeInjury(patient.health.maxHealth(), false);
    patient.health.setState(ActorHealth.STATE_RESTING);
    
    treats.mind.assignBehaviour(new FirstAid(treats, patient));
    Selection.pushSelection(treats, null);
  }
  
  
  private void configRecoveryScenario(Stage world, Base base, BaseUI UI) {
    
    final Venue staysAt = new Cantina(base);
    SiteUtils.establishVenue(staysAt, 4, 4, true, world);
    base.setup.fillVacancies(staysAt, true);
    
    final Actor treated = new Human(Backgrounds.EXCAVATOR, base);
    treated.enterWorldAt(staysAt, world);
    treated.health.setFatigueLevel(0.25f);
    treated.health.setInjuryLevel (0.75f);
    treated.health.setState(ActorHealth.STATE_RESTING);
    
    final Actor treats = staysAt.staff.workers().first();
    treats.goAboard(world.tileAt(2, 7), world);
    
    final FirstAid aid = new FirstAid(treats, treated);
    Item bandage = Item.with(FirstAid.TREATMENT, aid, 0.4f, Item.AVG_QUALITY);
    treated.gear.addItem(bandage);
    
    Selection.pushSelection(treated, null);
  }
  
  
  protected void afterCreation() {
  }
}













package stratos.start;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.base.*;
import stratos.game.campaign.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.game.plans.*;
import stratos.user.*;
import stratos.util.*;





//  Okay.  I think that just leaves negotiation to iron out.  Implement the
//  'first impressions' system for relations, plus the modifier for different
//  bases, (and maybe backgrounds, et cetera.)

//  The implement a system which sets relations for the base as a whole at the
//  average of relations for acquainted members.  In theory, this *should*
//  overcome the problem of individual members being hostile/attacking while
//  others are blithe and indifferent.

//  Just be sure to test this in Mission form too.


public class DebugCombat extends Scenario {
  
  
  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new DebugCombat());
  }
  
  
  private DebugCombat() {
    super();
  }
  
  
  public DebugCombat(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  public void beginGameSetup() {
    //super.beginGameSetup();
    super.initScenario("debug_combat");
  }
  
  
  protected String saveFilePrefix(Stage world, Base base) {
    return "debug_combat";
  }
  
  
  //public void renderVisuals(Rendering rendering) {
    //super.renderVisuals(rendering);
    /*
    final Tile over = UI().selection.pickedTile();
    if (KeyInput.wasTyped('p')) {
      I.say("TILE IS: "+over);
      I.say("  SHOULD PAVE? "+base().paving.map.needsPaving(over));
    }
    //*/
  //}
  
  
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
  

  //  TODO:  I want a situation where lictovores are usually hostile, vareen
  //  sometimes are, and quud basically never are, based on power and
  //  aggression.
  
  //  70%, 30%, and -10% chance to attack.  +30% if under attack.  -20% if
  //  matched by enemy, -50% if heavily outmatched.  +30% if they can't
  //  escape.
  
  //  TODO:  Test this for native tribes as well.  Both negotiation and
  //  dialogue.
  
  
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    GameSettings.setDefaults();
    GameSettings.hireFree  = true;
    GameSettings.buildFree = true;
    GameSettings.fogFree   = true;
    
    if (false) animalScenario  (world, base, UI);
    if (false) artilectScenario(world, base, UI);
    if (true ) nativeScenario  (world, base, UI);
  }
  
  
  private void nativeScenario(Stage world, Base base, BaseUI UI) {
    final Base natives = Base.baseWithName(world, Base.KEY_NATIVES, true);
    natives.relations.setRelation(base, -0.25f, true);
    
    final NativeHut hut = NativeHut.newHall(1, natives);
    Placement.establishVenue(hut, 4, 4, true, world);
    
    for (int i = 3; i-- > 0;) {
      final Actor actor = new Human(Backgrounds.GATHERER, natives);
      actor.enterWorldAt(hut.mainEntrance(), world);
      actor.mind.setHome(hut);
      actor.mind.setWork(hut);
    }
    
    for (int i = 3; i-- > 0;) {
      final Actor actor = new Human(Backgrounds.VOLUNTEER, base);
      actor.enterWorldAt(2, 2, world);
      UI.selection.pushSelection(actor, true);
    }
  }
  
  
  private void artilectScenario(Stage world, Base base, BaseUI UI) {
    final Base artilects = Base.baseWithName(world, Base.KEY_ARTILECTS, true);
    
    final Actor robot = Species.SPECIES_TRIPOD.newSpecimen(artilects);
    robot.enterWorldAt(5, 5, world);
    
    final Actor vet = new Human(Backgrounds.VETERAN, base);
    vet.enterWorldAt(4, 4, world);
    
    final Combat combat = new Combat(robot, vet);
    combat.setMotive(Plan.MOTIVE_EMERGENCY, 100);
    robot.mind.assignBehaviour(combat);
    
    for (int i = 5; i-- > 0;) {
      final Actor actor = new Human(Backgrounds.VOLUNTEER, base);
      final Tile entry = Spacing.pickRandomTile(robot, 4, world);
      actor.enterWorldAt(entry.x, entry.y, world);
      
      UI.selection.pushSelection(actor, true);
    }
  }
  
  
  private void animalScenario(Stage world, Base base, BaseUI UI) {
    final Base wildlife = Base.baseWithName(world, Base.KEY_WILDLIFE, true);
    
    final Actor actor = new Human(Backgrounds.VOLUNTEER, base);
    actor.enterWorldAt(4, 4, world);
    
    final Actor fauna = Species.LICTOVORE.newSpecimen(wildlife);
    fauna.health.setupHealth(0.5f, 1, 0);
    fauna.enterWorldAt(5, 5, world);
    
    final Combat combat = new Combat(fauna, actor);
    combat.setMotive(Plan.MOTIVE_EMERGENCY, 100);
    fauna.mind.assignBehaviour(combat);
    
    UI.selection.pushSelection(actor, true);
  }

  /*
  final TrooperLodge station = new TrooperLodge(base);
  Placement.establishVenue(station, 8, 8, true, world);
  final Base wildlife = Base.baseWithName(world, Base.KEY_WILDLIFE, true);
  
  //  ...I need to simplify their behaviour here.  Defensive/territorial-
  //  no fear as long as they're close to their lair.
  
  //  ...Let's introduce a nest of Yamagur.  See how they get along.
  final Nest nest = Species.LICTOVORE.createNest();
  nest.assignBase(wildlife);
  Placement.establishVenue(nest, 20, 20, true, world);
  
  for (int n = 4; n-- > 0;) {
    final Micovore foe = (Micovore) Species.LICTOVORE.newSpecimen(wildlife);
    foe.enterWorldAt(nest, world);
  }
  //*/
  
  
  protected void afterCreation() {
  }
}











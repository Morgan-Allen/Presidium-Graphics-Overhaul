

package stratos.start;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.base.*;
import stratos.game.campaign.*;
import stratos.game.maps.*;
import stratos.game.wild.*;

import stratos.graphics.common.Rendering;
import stratos.graphics.widgets.KeyInput;
import stratos.user.*;
import stratos.util.I;



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
  
  
  protected String saveFilePrefix(World world, Base base) {
    return "debug_combat";
  }
  
  
  public void renderVisuals(Rendering rendering) {
    super.renderVisuals(rendering);
    /*
    final Tile over = UI().selection.pickedTile();
    if (KeyInput.wasTyped('p')) {
      I.say("TILE IS: "+over);
      I.say("  SHOULD PAVE? "+base().paving.map.needsPaving(over));
    }
    //*/
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
    TG.setupMinerals(world, 0.6f, 0, 0.2f);
    world.terrain().readyAllMeshes();
    Flora.populateFlora(world);
    return world;
  }
  
  
  protected Base createBase(World world) {
    return Base.baseWithName(world, "Player Base", false);
  }
  
  
  protected void configureScenario(World world, Base base, BaseUI UI) {
    GameSettings.setDefaults();
    GameSettings.hireFree  = true;
    GameSettings.buildFree = true;
    
    final TrooperLodge station = new TrooperLodge(base);
    Placement.establishVenue(station, 8, 8, true, world);
    
    
    final Base wildlife = Base.baseWithName(world, Base.KEY_WILDLIFE, true);
    
    //  ...I need to simplify their behaviour here.  Defensive/territorial-
    //  no fear as long as they're close to their lair.
    
    //  TODO:  I need some special FX for the various techniques.
    
    //*
    //  ...Let's introduce a nest of Yamagur.  See how they get along.
    final Nest nest = Species.LICTOVORE.createNest();
    nest.assignBase(wildlife);
    Placement.establishVenue(nest, 20, 20, true, world);
    //*/
    
    for (int n = 4; n-- > 0;) {
      final Micovore foe = (Micovore) Species.LICTOVORE.newSpecimen(wildlife);
      foe.enterWorldAt(nest, world);
    }
  }
  
  
  protected void afterCreation() {
  }
}











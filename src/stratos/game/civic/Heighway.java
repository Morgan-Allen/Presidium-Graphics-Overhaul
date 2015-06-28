/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.civic;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.economic.*;
import static stratos.game.economic.Economy.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



public class Heighway extends Venue {
  
  final static String
    IMG_DIR = "media/Buildings/civilian/";
  final static CutoutModel
    ALL_MODELS[][] = CutoutModel.fromImageGrid(
      Heighway.class,
      IMG_DIR+"all_big_roads.png", 2, 2,
      2, 0, true
    ),
    NM[][] = ALL_MODELS, HUB_MODEL = NM[0][0],
    MODELS_X_AXIS[] = { HUB_MODEL, NM[0][1], HUB_MODEL },
    MODELS_Y_AXIS[] = { HUB_MODEL, NM[1][1], HUB_MODEL };
  
  final public static ImageAsset
    LINE_ICON = ImageAsset.fromImage(
      Heighway.class, "media/GUI/Buttons/mag_line_button.gif"
    ),
    HATCH_ICON = ImageAsset.fromImage(
      Heighway.class, "media/GUI/Buttons/access_hatch_button.gif"
    );
  
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    Heighway.class, "service_hatch",
    "Heighway", UIConstants.TYPE_ENGINEER, HATCH_ICON,
    "Heighways allow for long-distance power and road connections, but can "+
    "admit dangerous vermin.",
    2, 0, Structure.IS_FIXTURE | Structure.IS_LINEAR,
    Bastion.BLUEPRINT, Owner.TIER_PRIVATE,
    10,  //integrity
    25,  //armour
    20,  //build cost
    Structure.SMALL_MAX_UPGRADES
  );
  
  
  
  public Heighway(Base base) {
    super(BLUEPRINT, base);
  }
  
  
  public Heighway(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Situation and auto-placement:
    */
  final static int
    EXCLUDE_RADIUS = Stage.ZONE_SIZE / 2,
    MIN_ADJACENCY  = 2,
    BASE_ADJACENCY = 4;
  
  public float ratePlacing(Target point, boolean exact) {
    final Stage world = point.world();
    
    //  TODO:  Base this off demand for power & atmo distribution!
    //  e.g, base.demands.demandAround(point, Economy.POWER), etc...
    float need = 2;
    //
    //  We don't want hatches too close to other hatches, and they should be
    //  'hugging' some nearby walls...
    //  TODO:  Build that into the non-exact estimates too.
    if (exact) {
      final Object nearHatch = world.presences.nearestMatch(
        Heighway.class, point, EXCLUDE_RADIUS
      );
      if (nearHatch != null) return -1;
      
      int tilesAdj = 0;
      for (Tile t : Spacing.perimeter(footprint(), world)) {
        if (t == null || t.reserves() == null) continue;
        if (t.reserves().base() == base) tilesAdj++;
      }
      return need * (tilesAdj - MIN_ADJACENCY) / BASE_ADJACENCY;
    }
    else {
      return need;
    }
  }
  
  
  
  /**  Economic and behavioural methods-
    */
  public boolean setupWith(Tile position, Box2D area, Coord... others) {
    if (! super.setupWith(position, area, others)) return false;
    
    final Object model = faceModel(position, area, others);
    attachModel((ModelAsset) model);
    return true;
  }
  
  
  private Object faceModel(Tile position, Box2D area, Coord... others) {
    Object model = SiteUtils.setupMergingSegment(
      this, position, area, others,
      MODELS_X_AXIS, MODELS_Y_AXIS, HUB_MODEL, Heighway.class
    );
    /*
    if (model == MODELS_X_AXIS[1]) {
      final int step = (position.y / 2) % 4;
      if (step == 0) model = HUB_MODEL;
    }
    if (model == MODELS_Y_AXIS[1]) {
      final int step = (position.x / 2) % 4;
      if (step == 0) model = HUB_MODEL;
    }
    //*/
    return model;
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (numUpdates % 10 != 0) return;
    //
    //  Check to see if facing needs to be changed-
    final Object  oldModel = buildSprite.baseSprite().model();
    final Object  model    = faceModel(origin(), null);
    final Upgrade FC       = Structure.FACING_CHANGE;
    boolean canChange = false;
    if (model != oldModel) {
      if (GameSettings.buildFree || structure.hasUpgrade(FC)) {
        canChange = true;
      }
      else {
        structure.beginUpgrade(FC, true);
      }
    }
    if (canChange) {
      structure.resignUpgrade(FC, true);
      world.ephemera.addGhost(this, size, sprite(), 0.5f);
      attachModel((ModelAsset) model);
    }
    //
    //  And assign life-support and other tangible effects-
    if (model == HUB_MODEL) {
      stocks.forceDemand(ATMO , 2, true);
      stocks.forceDemand(POWER, 2, true);
      structure.setAmbienceVal(-2);
      //
      //  TODO:  Introduce the vermin-check here!
      Base.vermin(world);
    }
  }
  
  
  protected boolean canBuildOn(Tile t) {
    //
    //  Heighways can be used to span deserts and bodies of water, so they can
    //  be placed over anything.
    return true;
  }
  
  
  public boolean allowsEntry(Mobile m) {
    if (m instanceof Vermin) return true;
    else return false;
  }
  
  
  public int pathType() {
    return Tile.PATH_ROAD;
  }
  
  
  public Background[] careers() {
    return null;
  }
  
  
  public Traded[] services() {
    return null;
  }
  


  /**  Rendering and interface methods-
    */
  public String fullName() {
    if (inWorld()) {
      final Object model = buildSprite.baseSprite().model();
      if (model == HUB_MODEL) return "Service Hatch";
    }
    return super.fullName();
  }
  
  
  public SelectionPane configSelectPane(SelectionPane panel, BaseUI UI) {
    return VenuePane.configSimplePanel(this, panel, UI, null);
  }
}



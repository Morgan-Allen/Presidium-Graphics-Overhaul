/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.verse.*;
import stratos.game.wild.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;
import static stratos.game.actors.Qualities.*;



public class ServiceHatch extends Venue implements EntryPoints.Portal {
  
  final static String
    IMG_DIR = "media/Buildings/civilian/";
  final static CutoutModel
    ALL_MODELS[][] = CutoutModel.fromImageGrid(
      ServiceHatch.class,
      IMG_DIR+"all_big_roads.png", 2, 2,
      2, 0, true
    ),
    NM[][] = ALL_MODELS, HUB_MODEL = NM[0][0],
    MODELS_X_AXIS[] = { HUB_MODEL, NM[0][1], HUB_MODEL },
    MODELS_Y_AXIS[] = { HUB_MODEL, NM[1][1], HUB_MODEL };
  
  final public static ImageAsset
    LINE_ICON = ImageAsset.fromImage(
      ServiceHatch.class, "media/GUI/Buttons/mag_line_button.gif"
    ),
    HATCH_ICON = ImageAsset.fromImage(
      ServiceHatch.class, "media/GUI/Buttons/access_hatch_button.gif"
    );
  
  final static int
    TYPE_HUB = 0,
    TYPE_WAY = 1;
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    ServiceHatch.class, "service_hatch",
    "Causeway", Target.TYPE_WIP, HATCH_ICON,
    "Causeways allow for efficient long-distance power and road connections. ",
    2, 0, Structure.IS_FIXTURE | Structure.IS_LINEAR | Structure.IS_PUBLIC,
    Owner.TIER_PRIVATE, 10, 25
  );
  
  final public static Upgrade LEVELS[] = BLUEPRINT.createVenueLevels(
    Upgrade.SINGLE_LEVEL, Bastion.LEVELS[0],
    new Object[] { 5, ASSEMBLY },
    30
  );
  
  
  private int type = -1;
  
  
  public ServiceHatch(Base base) {
    super(BLUEPRINT, base);
  }
  
  
  public ServiceHatch(Session s) throws Exception {
    super(s);
    this.type = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(type);
  }
  
  
  
  /**  Situation and auto-placement:
    */
  final static int
    EXCLUDE_RADIUS = Stage.ZONE_SIZE / 2,
    MIN_ADJACENCY  = 2,
    BASE_ADJACENCY = 4;
  
  final static Siting SITING = new Siting(BLUEPRINT) {
    
    
    public float rateSettlementDemand(Base base) {
      return 0.5f;
    }
    
    
    public float ratePointDemand(Base base, Target point, boolean exact) {
      final Stage world = point.world();
      
      final Object nearHatch = world.presences.nearestMatch(
        ServiceHatch.class, point, EXCLUDE_RADIUS
      );
      if (nearHatch != null) return -1;
      //
      //  TODO:  Base this off demand for power & atmo distribution!
      //  e.g, base.demands.demandAround(point, Economy.POWER), etc...
      float need = 2;
      //
      //  We don't want hatches too close to other hatches, and they should be
      //  'hugging' some nearby walls...
      //  TODO:  Build that into the non-exact estimates too.
      if (exact) {
        final Tile at = (Tile) point;
        final Box2D area = at.area(null);
        area.incHigh(BLUEPRINT.size - 1);
        area.incWide(BLUEPRINT.size - 1);
        int tilesAdj = 0;
        for (Tile t : Spacing.perimeter(area, world)) {
          if (t == null || t.reserves() == null) continue;
          if (t.reserves().base() == base) tilesAdj++;
        }
        return need * (tilesAdj - MIN_ADJACENCY) / BASE_ADJACENCY;
      }
      else {
        return need;
      }
    }
  };
  
  
  
  /**  Economic and behavioural methods-
    */
  public boolean setupWith(Tile position, Box2D area, Coord... others) {
    if (! super.setupWith(position, area, others)) return false;
    
    final Object model = faceModel(position, area, others);
    attachModel((ModelAsset) model);
    this.type = model == HUB_MODEL ? TYPE_HUB : TYPE_WAY;
    setFacing(FACE_NONE);
    return true;
  }
  
  
  private Object faceModel(Tile position, Box2D area, Coord... others) {
    Object model = SiteUtils.setupMergingSegment(
      this, position, area, others,
      MODELS_X_AXIS, MODELS_Y_AXIS, HUB_MODEL, ServiceHatch.class
    );
    if (model == MODELS_X_AXIS[1]) {
      final int step = (position.y / 2) % 6;
      if (step == 0) model = HUB_MODEL;
    }
    if (model == MODELS_Y_AXIS[1]) {
      final int step = (position.x / 2) % 6;
      if (step == 0) model = HUB_MODEL;
    }
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
      world.ephemera.addGhost(this, size, sprite(), 0.5f, 1);
      attachModel((ModelAsset) model);
      this.type = model == HUB_MODEL ? TYPE_HUB : TYPE_WAY;
      setFacing(FACE_NONE);
    }
    //
    //  And assign life-support and other tangible effects-
    if (model == HUB_MODEL) {
      stocks.forceDemand(ATMO , 0, 2);
      stocks.forceDemand(POWER, 0, 2);
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
  
  
  public int pathType() {
    return Tile.PATH_ROAD;
  }
  
  
  protected Tile pickEntrance(int facing) {
    if (type == TYPE_HUB) return origin();
    else return null;
  }
  
  
  public boolean allowsEntry(Accountable m) {
    if (m instanceof Vermin) return true;
    else return false;
  }
  
  
  public boolean allowsStageExit(Mobile m) {
    return allowsEntry(m);
  }
  
  
  public int exitType() {
    return EntryPoints.Portal.TYPE_BOLT_HOLE;
  }
  
  
  public Sector leadsTo() {
    return Verse.SECTOR_UNDERGROUND;
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
  
  
  public SelectionPane configSelectPane(SelectionPane panel, HUD UI) {
    return VenuePane.configSimplePanel(this, panel, UI, null, null);
  }
}



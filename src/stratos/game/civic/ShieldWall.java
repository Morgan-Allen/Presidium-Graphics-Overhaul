/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.civic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



public class ShieldWall extends Venue {
  
  
  /**  Construction and save/load routines-
    */
  final static String
    IMG_DIR = "media/Buildings/military/";
  
  final static ModelAsset
    WALL_MODELS[][] = CutoutModel.fromImageGrid(
      ShieldWall.class, IMG_DIR+"all_shield_walls.png", 4, 3, 2, 3, false
    ),
    WM[][] = WALL_MODELS, MODEL_HUB = WM[0][0], MH = MODEL_HUB,
    CAPS_X_AXIS[] = { MH, WM[1][1], MH },
    CAPS_Y_AXIS[] = { MH, WM[1][2], MH },
    SEGMENTS_X[]  = { WM[0][1], WM[1][1], WM[2][1], WM[3][1] },
    SEGMENTS_Y[]  = { WM[0][2], WM[1][2], WM[2][2], WM[3][2] };
  
  final static ImageAsset
    ICON = ImageAsset.fromImage(
      ShieldWall.class, "media/GUI/Buttons/shield_wall_button.gif"
    );
  
  final static int
    TYPE_INIT    = -1,
    TYPE_HUB     =  0,
    TYPE_TOWER   =  1,
    TYPE_SEGMENT =  2,
    TYPE_GATE    =  3;
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    ShieldWall.class, "shield_wall",
    "Shield Wall", UIConstants.TYPE_SECURITY, ICON,
    "Shield Walls are defensive emplacements that improve base security.",
    2, 2, Structure.IS_LINEAR | Structure.IS_FIXTURE,
    Bastion.BLUEPRINT, Owner.TIER_FACILITY,
    125, 15, 40,  //integrity, armour, and build cost
    Structure.SMALL_MAX_UPGRADES
  );
  
  
  
  private Boarding entrances[] = null;
  private boolean hasFoundation = false;
  private int type = -1;
  
  
  public ShieldWall(Base base) {
    super(BLUEPRINT, base);
    this.type = TYPE_INIT;
  }
  
  
  public ShieldWall(Session s) throws Exception {
    super(s);
    type = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(type);
  }
  
  
  
  /**  Placement and setup methods-
    */
  public boolean setupWith(Tile position, Box2D area, Coord... others) {
    if (! super.setupWith(position, area, others)) return false;
    
    final Object model = faceModel(position, area, others);
    this.type = typeIndexFrom(model);
    attachModel((ModelAsset) model);
    
    hasFoundation = false;
    for (Tile u : position.world.tilesIn(footprint(), true)) {
      if (super.canBuildOn(u)) hasFoundation = true;
    }
    return true;
  }
  
  
  protected boolean canBuildOn(Tile t) {
    //
    //  In order to ensure a good 'seal' against unpathable terrain areas (such
    //  as oceans) we allow overlap on these areas as long as we have at least
    //  one normal tile to stand on.
    return hasFoundation;
  }
  
  
  protected boolean checkPerimeter(Stage world) {
    //
    //  The purpose of a shield wall is to enclose areas completely, so checks
    //  to *avoid* pathing-enclosure would be pointless:
    return true;
  }
  
  
  protected void updatePaving(boolean inWorld) {
    //
    //  Also, asking for pavement on the other side might be a trifle risky...
  }
  
  
  private Object faceModel(Tile position, Box2D area, Coord... others) {
    Object model = SiteUtils.setupMergingSegment(
      this, position, area, others,
      CAPS_X_AXIS, CAPS_Y_AXIS, MODEL_HUB, ShieldWall.class
    );
    if (model == CAPS_X_AXIS[1]) {
      model = SEGMENTS_X[(position.y / 2) % 4];
    }
    if (model == CAPS_Y_AXIS[1]) {
      model = SEGMENTS_Y[(position.x / 2) % 4];
    }
    return model;
  }
  
  
  private int typeIndexFrom(Object model) {
    if (model == MODEL_HUB) return TYPE_HUB;
    int index = -1;
    if (index == -1) index = Visit.indexOf(model, SEGMENTS_X);
    if (index == -1) index = Visit.indexOf(model, SEGMENTS_Y);
    if      (index == 0) return TYPE_TOWER  ;
    else if (index == 3) return TYPE_GATE   ;
    else                 return TYPE_SEGMENT;
  }
  
  
  
  /**  Registration, life cycle and economic functions-
    */
  public Background[] careers() {
    return null;
  }
  
  
  public Traded[] services() {
    return null;
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    
    final Upgrade FC = Structure.FACING_CHANGE;
    if (! (numUpdates % 10 == 0 || structure.hasUpgrade(FC))) return;
    updateFacing(false);
  }
  
  
  public boolean updateFacing(boolean instant) {
    final Object  model   = faceModel(origin(), null);
    final int     newType = typeIndexFrom(model);
    final Upgrade FC      = Structure.FACING_CHANGE;
    boolean canChange = false;
    
    if (type != newType) {
      if (GameSettings.buildFree || structure.hasUpgrade(FC) || instant) {
        canChange = true;
      }
      else {
        structure.beginUpgrade(FC, true);
      }
    }
    if (canChange) {
      structure.resignUpgrade(FC, true);
      this.type = newType;
      world.ephemera.addGhost(this, size, sprite(), 0.5f);
      attachModel((ModelAsset) model);
      refreshAdjacent();
    }
    return canChange;
  }
  
  
  
  /**  Boardable implementation-
    */
  public boolean isEntrance(Boarding t) {
    return Visit.arrayIncludes(canBoard(), t);
  }
  
  
  public Boarding[] canBoard() {
    if (entrances != null) return entrances;
    
    entrances = new Boarding[4];
    final Tile o = origin();
    final Stage world = o.world;
    final int h = size / 2, s = size;
    
    entrance = null;
    entrances[0] = world.tileAt(o.x + h, o.y + s);
    entrances[1] = world.tileAt(o.x + s, o.y + h);
    entrances[2] = world.tileAt(o.x + h, o.y - 1);
    entrances[3] = world.tileAt(o.x - 1, o.y + h);
    
    for (int i = 4; i-- > 0;) {
      final Tile t = (Tile) entrances[i];
      if (t == null) continue;
      
      if (t.above() instanceof ShieldWall) {
        entrances[i] = (ShieldWall) t.above();
      }
      else if (isGate() && ! t.blocked()) {
        entrances[i] = entrance = t;
      }
      else entrances[i] = null;
    }
    
    return entrances;
  }
  
  
  private void refreshAdjacent() {
    entrances = null;
    for (Boarding b : canBoard()) {
      if (b instanceof ShieldWall) ((ShieldWall) b).entrances = null;
      if (b instanceof Tile      ) ((Tile      ) b).refreshAdjacent();
    }
  }
  
  
  public boolean enterWorldAt(int x, int y, Stage world, boolean intact) {
    if (! super.enterWorldAt(x, y, world, intact)) return false;
    refreshAdjacent();
    return true;
  }
  
  
  public void exitWorld() {
    refreshAdjacent();
    super.exitWorld();
  }
  
  
  public boolean isTower() {
    return type == TYPE_TOWER;
  }
  
  
  public boolean isGate() {
    return type == TYPE_GATE;
  }
  
  
  public boolean isSection() {
    return type == TYPE_SEGMENT || type == TYPE_HUB;
  }
  
  
  
  
  /**  Rendering and interface methods-
    */
  public Vec3D viewPosition(Vec3D v) {
    return super.position(v);
  }


  public String fullName() {
    if (isGate ()) return "Blast Doors";
    if (isTower()) return "Sentry Post";
    return "Shield Wall";
  }
  
  
  public SelectionPane configSelectPane(SelectionPane panel, BaseUI UI) {
    return VenuePane.configSimplePanel(this, panel, UI, null);
  }
}




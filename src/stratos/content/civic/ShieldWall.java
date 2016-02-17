/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;



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
    "Shield Wall", Target.TYPE_SECURITY, ICON,
    "Shield Walls are defensive emplacements that improve base security.",
    2, 2, Structure.IS_LINEAR | Structure.IS_FIXTURE,
    Owner.TIER_FACILITY, 125,
    15
  );
  
  
  private int type = -1;
  private Boarding entrances[] = null;
  private boolean hasFoundation = false;
  
  
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
  //  TODO:  Fill this in!
  final static Siting SITING = new Siting(BLUEPRINT) {
  };
  
  
  public boolean setupWith(Tile position, Box2D area, Coord... others) {
    if (! super.setupWith(position, area, others)) return false;
    
    final Object config[] = configFrom(position, area, others);
    attachModel((ModelAsset) config[0]);
    this.type = (Integer) config[1];
    
    hasFoundation = false;
    for (Tile u : position.world.tilesIn(footprint(), true)) {
      if (super.canBuildOn(u)) hasFoundation = true;
    }
    return true;
  }
  
  
  private Object[] configFrom(Tile position, Box2D area, Coord... others) {
    Object model = SiteUtils.setupMergingSegment(
      this, position, area, others,
      CAPS_X_AXIS, CAPS_Y_AXIS, MODEL_HUB, ShieldWall.class
    );
    
    final boolean hasParent = parent() != null;
    final Box2D fromParent = hasParent ? parent().footprint() : null;
    int index = -1, type = -1;
    
    if (model == CAPS_X_AXIS[1]) {
      
      index = (position.y / 2) % 4;
      if (hasParent) {
        if      (position.y == fromParent.ypos() - 1.5f) {
          index = 3;
        }
        else if (position.y == fromParent.ymax() + 0.5f) {
          index = 3;
        }
        else if (index == 3) {
          index = 2;
        }
      }
      model = SEGMENTS_X[index];
    }
    if (model == CAPS_Y_AXIS[1]) {
      
      index = (position.x / 2) % 4;
      if (hasParent) {
        if (index == 3) index = 2;
        /*
        if      (position.x == fromParent.xpos() - 1.5f) {
          index = 3;
        }
        else if (position.x == fromParent.xmax() + 0.5f) {
          index = 3;
        }
        else if (index == 3) {
          index = 2;
        }
        //*/
      }
      model = SEGMENTS_Y[index];
    }
    if (model == MODEL_HUB) {
      type = TYPE_HUB;
    }
    else {
      if      (index == 0) type = TYPE_TOWER  ;
      else if (index == 3) type = TYPE_GATE   ;
      else                 type = TYPE_SEGMENT;
    }
    
    return new Object[] { model, type };
  }
  
  
  public boolean canBuildOn(Tile t) {
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
    
    if (! inWorld) {
      base.transport.updatePerimeter(this, false, null);
      return;
    }
    
    //  TODO:  Come up with a good system here!  You should pave the 'interior'
    //  side if possible.
    
    //  Well... there is a very ugly temporary hack for this.
    
    final Target HQ = world.presences.nearestMatch(Bastion.class, this, -1);
    final Batch <Tile> toPave = new Batch();
    
    if (HQ != null) for (Tile t : Spacing.perimeter(footprint(), world)) {
      if (t == null || ! t.canPave()) continue;
      if (Spacing.distance(HQ, t) > Spacing.distance(this, HQ)) continue;
      toPave.add(t);
    }
    base.transport.updatePerimeter(this, true, toPave.toArray(Tile.class));
  }
  
  
  
  /**  Registration, life cycle and economic functions-
    */
  final public static Upgrade LEVELS[] = BLUEPRINT.createVenueLevels(
    Upgrade.SINGLE_LEVEL, TrooperLodge.LEVELS[0],
    new Object[] { 10, ASSEMBLY, 5, BATTLE_TACTICS },
    40
  );
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    
    final Upgrade FC = Structure.FACING_CHANGE;
    if (! (numUpdates % 10 == 0 || structure.hasUpgrade(FC))) return;
    updateFacing(false);
  }
  
  
  public boolean updateFacing(boolean instant) {
    final Object     config[] = configFrom(origin(), null);
    final ModelAsset model    = (ModelAsset) config[0];
    final int        newType  = (Integer   ) config[1];
    
    final Upgrade FC = Structure.FACING_CHANGE;
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
      world.ephemera.addGhost(this, size, sprite(), 0.5f, 1);
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
  
  
  public SelectionPane configSelectPane(SelectionPane panel, HUD UI) {
    return VenuePane.configSimplePanel(this, panel, UI, null, null);
  }
}




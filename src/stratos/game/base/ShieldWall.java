/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.base;
import stratos.game.actors.Actor;
import stratos.game.actors.Background;
import stratos.game.actors.Behaviour;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;


//  This will need to extend Venue again, so it can show up in the install
//  UI.  Merge with the work done in the FencePylon class.




public class ShieldWall extends Venue {
  
  
  /**  Construction and save/load routines-
    */
  final static String
    IMG_DIR = "media/Buildings/military/";
  
  //  TODO:  Clean these up and get newer art.
  final static ModelAsset
    SECTION_MODELS[] = CutoutModel.fromImages(
      ShieldWall.class, IMG_DIR, 2, 4, false,
      "wall_corner.png",
      "wall_tower_left.png",
      "wall_tower_right.png"
    ),
    SECTION_MODEL_LEFT = CutoutModel.fromImage(
      ShieldWall.class, IMG_DIR+"wall_segment_left.png" , 2, 1.33f
    ),
    SECTION_MODEL_RIGHT = CutoutModel.fromImage(
      ShieldWall.class, IMG_DIR+"wall_segment_right.png", 2, 1.33f
    ),
    SECTION_MODEL_CORNER = SECTION_MODELS[0],
    TOWER_MODEL_LEFT     = SECTION_MODELS[1],
    TOWER_MODEL_RIGHT    = SECTION_MODELS[2],
    
    DOORS_MODEL_LEFT = CutoutModel.fromImage(
      ShieldWall.class, IMG_DIR+"wall_gate_left.png" , 4, 2.5f
    ),
    DOORS_MODEL_RIGHT = CutoutModel.fromImage(
      ShieldWall.class, IMG_DIR+"wall_gate_right.png", 4, 2.5f
    );
  
  final static ImageAsset
    ICON = ImageAsset.fromImage(
      ShieldWall.class, "media/GUI/Buttons/shield_wall_button.gif"
    );
  
  final static int
    TYPE_PLACING       = -1,
    TYPE_SINGLE        =  0,
    TYPE_END_CAP       =  1,
    TYPE_JUNCTION      =  2,
    TYPE_TOWER_LEFT    =  3,
    TYPE_SECTION_LEFT  =  4,
    TYPE_TOWER_RIGHT   =  5,
    TYPE_SECTION_RIGHT =  6,
    TYPE_DOOR_LEFT     =  7,
    TYPE_DOOR_RIGHT    =  8;
  
  final static ModelAsset MODEL_TYPES[] = {
    SECTION_MODEL_CORNER,
    SECTION_MODEL_CORNER,
    SECTION_MODEL_CORNER,
    TOWER_MODEL_LEFT    ,
    SECTION_MODEL_LEFT  ,
    TOWER_MODEL_RIGHT   ,
    SECTION_MODEL_RIGHT ,
    DOORS_MODEL_LEFT    ,
    DOORS_MODEL_RIGHT   ,
  };
  
  
  final List <Mobile> inside = new List <Mobile> ();
  private Boarding entrances[] = null;
  private ShieldWall barrier[] = null;  //  TODO:  Save/load this.
  
  
  public ShieldWall(Base base) {
    //  NOTE:  This constructor is intended solely for use during UI-placement-
    //         it does not actually create a shield wall object to enter the
    //         world, but a placeholder for a row of them (see below.)
    this(TYPE_PLACING, 2, 2, base);
  }
  
  
  protected ShieldWall(int type, int size, int high, Base base) {
    super(size, high, ENTRANCE_NONE, base);
    this.type = type;
    if (isSection()) {
      structure.setupStats(75, 35, 40, 0, Structure.TYPE_FIXTURE);
    }
    else {
      structure.setupStats(200, 35, 100, 0, Structure.TYPE_FIXTURE);
    }
  }
  
  
  public ShieldWall(Session s) throws Exception {
    super(s);
    s.loadObjects(inside);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObjects(inside);
  }
  

  
  /**  Registration, life cycle and economic functions-
    */
  //  TODO:  Include upgrades here to represent the transition to sturdier
  //  walls, or a different type of turret, or changes to a new facing, or the
  //  inclusion of a checkpoint.
  
  
  public Behaviour jobFor(Actor actor) {
    return null;
  }
  
  
  public Background[] careers() {
    return null;
  }
  
  
  public TradeType[] services() {
    return null;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates);
    //  TODO:  Update your own type, depending on adjacency, and possibly
    //  schedule reconstruction for the purpose.
    if (numUpdates == 10) {
      final int oldType = this.type, newType = getFacingType(null, world);
      
      if (oldType != newType) {
        if (GameSettings.buildFree) {
          attachModel(MODEL_TYPES[newType]);
        }
        //  TODO:  Implement-
        /*
        else {
        if (structure.upgradeLevel(FACING_CHANGE) > 0) {
          structure.resignUpgrade(FACING_CHANGE);
          attachModel(newModel);
        }
        else {
          structure.beginUpgrade(FACING_CHANGE, true);
          attachModel(oldModel);
        }
        }
        //*/
      }
    }
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
    final World world = o.world;
    final int h = size / 2, s = size;
    
    entrance = null;
    entrances[0] = world.tileAt(o.x + h, o.y + s);
    entrances[1] = world.tileAt(o.x + s, o.y + h);
    entrances[2] = world.tileAt(o.x + h, o.y - 1);
    entrances[3] = world.tileAt(o.x - 1, o.y + h);
    
    for (int i = 4; i-- > 0;) {
      final Tile t = (Tile) entrances[i];
      if (t == null) continue;
      if (t.onTop() instanceof ShieldWall) {
        entrances[i] = (ShieldWall) t.onTop();
      }
      else if (isGate() && ! t.blocked()) {
        entrances[i] = entrance = t;
      }
      else entrances[i] = null;
    }
    
    return entrances;
  }
  
  
  
  /**  Configuring and querying segment types-
    */
  public boolean isTower() {
    return type == TYPE_TOWER_LEFT || type == TYPE_TOWER_RIGHT;
  }
  
  
  public boolean isGate() {
    return type == TYPE_DOOR_LEFT || type == TYPE_DOOR_RIGHT;
  }
  
  
  public boolean isSection() {
    return type == TYPE_SECTION_LEFT || type == TYPE_SECTION_RIGHT;
  }
  
  
  public boolean isPlacing() {
    return type == TYPE_PLACING;
  }
  
  
  private int getFacingType(ShieldWall segments[], World world) {
    
    final boolean near[] = new boolean[8];
    int numNear = 0;
    final Tile o = origin();
    
    for (int n : N_ADJACENT) {
      final Tile t = world.tileAt(o.x + (N_X[n] * 2), o.y + (N_Y[n] * 2));
      if (t == null) continue;
      if (t.onTop() instanceof ShieldWall) near[n] = true;
      if (segments != null) for (ShieldWall s : segments) {
        if (t == s.origin()) { near[n] = true; break; }
      }
      if (near[n]) numNear++;
    }
    
    if (numNear == 0) return TYPE_SINGLE;
    if (numNear == 1) return TYPE_END_CAP;
    if (numNear == 2) {
      if (near[N] && near[S]) {
        if (o.y % 8 == 0) return TYPE_TOWER_LEFT;
        else return TYPE_SECTION_LEFT;
      }
      if (near[W] && near[E]) {
        if (o.x % 8 == 0) return TYPE_TOWER_RIGHT;
        else return TYPE_SECTION_RIGHT;
      }
    }
    return TYPE_JUNCTION;
  }
  
  
  
  /**  Placement and Installation-
    */
  final static int
    SIDE_COORDS[][] = {
      { 0, -2,  0, 0,  0, 2,  0, 4,  0, 6,  0, 8},
      { 6, -2,  6, 0,  6, 2,  6, 4,  6, 6,  6, 8},
      {-2,  0,  0, 0,  2, 0,  4, 0,  6, 0,  8, 0},
      {-2,  6,  0, 6,  2, 6,  4, 6,  6, 6,  8, 6},
    },
    SIDE_LENGTH = 8;
  
  
  private ShieldWall[] barrierForBorderNear(Tile point, Base base) {
    //
    //  Firstly, determine which sector this point lies within, and the corner
    //  tile of that sector.
    if (point == null) return null;
    final World world = point.world;
    final Tile corner = world.tileAt(
      Visit.round(point.x, SIDE_LENGTH, false),
      Visit.round(point.y, SIDE_LENGTH, false)
    );
    //
    //  Find which side of the sector the given point is closest to (measured
    //  from their midpoints.)
    int pickedCoords[] = null;
    float minDist = Float.POSITIVE_INFINITY;
    for (int coords[] : SIDE_COORDS) {
      final int midI = SIDE_LENGTH / 2;
      final Tile mid = world.tileAt(
        coords[midI    ] + corner.x,
        coords[midI + 1] + corner.y
      );
      if (mid == null) continue;
      final float dist = Spacing.distance(point, mid);
      if (dist < minDist) { minDist = dist; pickedCoords = coords; }
    }
    //
    //  Then take every second tile on that side, and generate a segment of
    //  shield wall to go with it.
    final Batch <ShieldWall> barrier = new Batch <ShieldWall> ();
    for (int n = 0; n < pickedCoords.length;) {
      final Tile under = world.tileAt(
        pickedCoords[n++] + corner.x,
        pickedCoords[n++] + corner.y
      );
      if (under == null || under.onTop() instanceof ShieldWall) continue;
      final ShieldWall segment = new ShieldWall(base);
      segment.type = TYPE_JUNCTION;
      segment.setPosition(under.x, under.y, world);
      //
      //  We only allow the ends to be capped if they abut on something else
      //  (see below.)
      if (checkForCaps(segment, n, world)) barrier.add(segment);
    }
    return barrier.toArray(ShieldWall.class);
  }
  
  
  private boolean checkForCaps(ShieldWall s, int index, World world) {
    //  TODO:  This needs more refinement.  Interior angles still count!  BLAH
    //
    //  Any segments in the middle of the wall should be fine.  Otherwise, we
    //  check to see if there would be any segments adjoining- if so, we allow
    //  the extension, so as to fill in the corner neatly.
    if (index > 2 && index <= SIDE_LENGTH + 2) return true;
    final int type = s.getFacingType(null, world);
    if (type != TYPE_SINGLE) return true;
    return false;
  }
  
  
  
  public boolean setPosition(float x, float y, World world) {
    if (! isPlacing()) return super.setPosition(x, y, world);
    
    super.setPosition(x, y, world);
    barrier = barrierForBorderNear(origin(), base);
    if (barrier == null) return false;
    
    for (ShieldWall segment : barrier) {
      final int type = segment.getFacingType(barrier, world);
      segment.type = type;
      segment.attachModel(MODEL_TYPES[type]);
    }
    return true;
  }
  
  
  public boolean canPlace() {
    if (! isPlacing()) return super.canPlace();
    
    if (barrier == null) return false;
    for (ShieldWall segment : barrier) {
      if (! segment.canPlace()) return false;
    }
    return true;
  }
  

  protected boolean checkPerimeter(World world) {
    //  TODO:  This might require some modification later.
    return true;
  }
  
  
  public void doPlacement() {
    if (! isPlacing()) {
      super.doPlacement();
      return;
    }
    for (ShieldWall segment : barrier) {
      segment.doPlacement();
    }
  }
  
  
  public void previewPlacement(boolean canPlace, Rendering rendering) {
    if (! isPlacing()) {
      super.previewPlacement(canPlace, rendering);
      return;
    }
    if (barrier != null) for (ShieldWall segment : barrier) {
      segment.previewPlacement(canPlace, rendering);
    }
  }
  
  
  
  
  /**  Rendering and interface methods-
    */
  public Vec3D viewPosition(Vec3D v) {
    return super.position(v);
  }
  
  
  public String fullName() {
    return isTower() ? "Sentry Post" : "Shield Wall";
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "shield_wall");
  }
  
  
  public String helpInfo() {
    return
      "Shield Walls are defensive emplacements that improve base security.";
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_MILITANT;
  }
}




/*
public Vec3D position(Vec3D v) {
  if (v == null) v = new Vec3D();
  super.position(v);
  if      (type == TYPE_SECTION) v.z += 1.33f;
  else if (type == TYPE_TOWER  ) v.z += 2.50f;
  else if (type == TYPE_SQUARE ) v.z += 1.00f;
  return v;
}
//*/
/*
protected Structural instance(Base base) {
  return new ShieldWall(base);
}


protected boolean lockToGrid() {
  return true;
}


protected boolean checkPerimeter(World world) {
  for (Tile n : Spacing.perimeter(area(), world)) {
    if (n == null || n.onTop() instanceof ShieldWall) continue;
    if (n.owningType() >= this.owningType()) return false;
  }
  return true;
}
//*/
/*
protected List <Structural> installedBetween(Tile start, Tile end) {
  final List <Structural> installed = super.installedBetween(start, end);
  if (installed == null || installed.size() < 4) return installed;
  //
  //  If the stretch to install is long enough, we cut out the middle two
  //  segments and install a set of Blast Doors in their place-
  final World world = start.world;
  final int cut = installed.size() / 2;
  final ShieldWall
    a = (ShieldWall) installed.atIndex(cut),
    b = (ShieldWall) installed.atIndex(cut - 1);
  if (a.facing != b.facing || a.facing == CORNER) return installed;
  //
  //  The doors occupy the exact centre of this area, with the same facing-
  final Vec3D centre = a.position(null).add(b.position(null)).scale(0.5f);
  final BlastDoors doors = new BlastDoors(a.base(), a.facing);
  doors.setPosition(centre.x - 1.5f, centre.y - 1.5f, world);
  final Box2D bound = doors.area(null);
  for (Structural v : installed) {
    if (v == a || v == b) continue;
    if (v.area(null).cropBy(bound).area() > 0) return installed;
  }
  //
  //  Update and return the list-
  installed.remove(a);
  installed.remove(b);
  installed.add(doors);
  return installed;
}
//*/
/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
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
  
  
  private Boarding entrances[] = null;
  
  
  public ShieldWall(Base base) {
    //  NOTE:  This constructor is intended solely for use during UI-placement-
    //         it does not actually create a shield wall object to enter the
    //         world, but a placeholder for a row of them (see below.)
    this(TYPE_PLACING, 2, 2, base);
  }
  
  
  protected ShieldWall(int type, int size, int high, Base base) {
    super(size, high, ENTRANCE_NONE, base);
    this.type = type;
    if (isGate()) {
      structure.setupStats(
        200, 25, 100,  //integrity, armour, and build cost
        Structure.SMALL_MAX_UPGRADES, Structure.TYPE_FIXTURE
      );
    }
    else {
      structure.setupStats(
        75, 15, 40,  //integrity, armour, and build cost
        Structure.SMALL_MAX_UPGRADES, Structure.TYPE_FIXTURE
      );
    }
  }
  
  
  public ShieldWall(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Registration, life cycle and economic functions-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
  );
  
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES; }
  final public static Upgrade
    FACING_CHANGE = new Upgrade(
      "Facing Change", "",
      0, null, 1, null,
      ShieldWall.class, ALL_UPGRADES
    ),
    BLAST_SHIELDS = new Upgrade(
      "Blast Shields", "",
      0, null, 1, null,
      ShieldWall.class, ALL_UPGRADES
    ),
    NULL_BARRIER  = new Upgrade(
      "Null Barrier", "",
      0, null, 1, null,
      ShieldWall.class, ALL_UPGRADES
    );
  
  
  public Behaviour jobFor(Actor actor) {
    return null;
  }
  
  
  public Background[] careers() {
    return null;
  }
  
  
  public Traded[] services() {
    return null;
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    
    if (numUpdates % 10 == 0) {
      final int oldType = this.type, newType = getFacingType(world, null);
      
      if (oldType != newType) {
        if (GameSettings.buildFree) {
          this.type = newType;
          attachModel(MODEL_TYPES[newType]);
        }
        else if (structure.upgradeLevel(FACING_CHANGE) < 1) {
          structure.beginUpgrade(FACING_CHANGE, true);
        }
        else {
          structure.resignUpgrade(FACING_CHANGE, true);
          this.type = newType;
          attachModel(MODEL_TYPES[newType]);
        }
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
  
  
  private int getFacingType(Stage world, ShieldWall newWall[]) {
    
    final boolean near[] = new boolean[8];
    int numNear = 0;
    final Tile o = origin();
    
    for (int n : T_ADJACENT) {
      final Tile t = world.tileAt(o.x + (T_X[n] * 2), o.y + (T_Y[n] * 2));
      if (t == null) continue;
      if (t.onTop() instanceof ShieldWall) near[n] = true;
      if (newWall != null) for (ShieldWall s : newWall) {
        if (t == s.origin()) { near[n] = true; break; }
      }
      if (near[n]) numNear++;
    }
    
    if (this instanceof BlastDoors) {
      if (facing == W || facing == E) return TYPE_DOOR_LEFT ;
      if (facing == N || facing == S) return TYPE_DOOR_RIGHT;
      return TYPE_SINGLE;
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
  
  
  
  /**  Placement and Structure.Basis-
    */
  final static int
    SIDE_COORDS[][] = {
      { 0, -2,  0, 0,  0, 2,  0, 4,  0, 6,  0, 8},
      { 6, -2,  6, 0,  6, 2,  6, 4,  6, 6,  6, 8},
      {-2,  0,  0, 0,  2, 0,  4, 0,  6, 0,  8, 0},
      {-2,  6,  0, 6,  2, 6,  4, 6,  6, 6,  8, 6},
    },
    DOOR_COORDS[][] = {
      {-1,  2},
      { 5,  2},
      { 2, -1},
      { 2,  5},
    },
    ALL_FACINGS[] = { E, W, S, N },
    SIDE_LENGTH = 8;
  
  
  private ShieldWall[] barrierForBorderNear(Tile point, Base base) {
    //
    //  Firstly, determine which sector this point lies within, and the corner
    //  tile of that sector.
    if (point == null) return null;
    final Stage world = point.world;
    final Tile corner = world.tileAt(
      Nums.round(point.x, SIDE_LENGTH, false),
      Nums.round(point.y, SIDE_LENGTH, false)
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
    final List <ShieldWall> barrier = new List <ShieldWall> ();
    for (int n = 0; n < pickedCoords.length;) {
      final Tile under = world.tileAt(
        pickedCoords[n++] + corner.x,
        pickedCoords[n++] + corner.y
      );
      if (under == null || under.onTop() instanceof ShieldWall) continue;
      final ShieldWall segment = new ShieldWall(base);
      segment.type = TYPE_JUNCTION;
      segment.setPosition(under.x, under.y, world);
      if (segment.canPlace()) barrier.add(segment);
    }
    //
    //  We only allow the ends to be capped if they abut on something else
    //  (see below), and may have to insert blast doors as well, before
    //  returning the final result.
    final Box2D mainArea = new Box2D().set(
      corner.x - 0.5f, corner.y - 0.5f,
      SIDE_LENGTH    , SIDE_LENGTH
    );
    final int sideID = Visit.indexOf(pickedCoords, SIDE_COORDS);
    checkForCaps(barrier, mainArea, world);
    if (! checkDoorInsert(barrier, sideID, corner, world)) return null;
    return barrier.toArray(ShieldWall.class);
  }
  
  
  private void checkForCaps(
    List <ShieldWall> segments, Box2D mainArea, Stage world
  ) {
    //  Any segments in the middle of the wall should be fine.  Otherwise, we
    //  check to see if there would be any segments adjoining- if so, we allow
    //  the extension, so as to fill in the corner neatly.  (Otherwise, we cull
    //  that segment from the list.)
    final ShieldWall newWall[] = segments.toArray(ShieldWall.class);
    for (ShieldWall s : newWall) {
      final Tile o = s.origin();
      if (mainArea.contains(o.x, o.y)) continue;
      final int type = s.getFacingType(world, newWall);
      if (type == TYPE_END_CAP) segments.remove(s);
    }
  }
  
  
  private boolean checkDoorInsert(
    List <ShieldWall> segments, int sideID, Tile corner, Stage world
  ) {
    if (! (this instanceof BlastDoors)) return true;
    //
    //  Firstly, we establish where the doors should be placed.
    final int coords[] = DOOR_COORDS[sideID];
    final Tile at = world.tileAt(corner.x + coords[0], corner.y + coords[1]);
    if (at == null) return false;
    //
    //  Then, initialise and set their location-
    final int facing = ALL_FACINGS[sideID];
    final BlastDoors doors = new BlastDoors(base, TYPE_SINGLE, facing);
    doors.setPosition(at.x, at.y, world);
    //
    //  Then, replace any prior intersecting segments, and return.
    for (ShieldWall s : segments) {
      final Tile o = s.origin();
      if (doors.footprint().contains(o.x, o.y)) segments.remove(s);
    }
    segments.add(doors);
    return true;
  }
  
  
  public boolean setPosition(float x, float y, Stage world) {
    if (! super.setPosition(x, y, world)) return false;
    if (! isPlacing()) return true;
    
    final ShieldWall wallGroup[] = barrierForBorderNear(origin(), base);
    if (wallGroup == null) return false;
    structure.assignGroup(wallGroup);
    
    for (ShieldWall segment : wallGroup) {
      final int type = segment.getFacingType(world, wallGroup);
      segment.type = type;
      segment.structure.assignGroup(wallGroup);
      segment.attachModel(MODEL_TYPES[type]);
    }
    return true;
  }
  
  
  public boolean canPlace() {
    if (origin() == null) return false;
    final Stage world = origin().world;
    
    //  In essence, we allow construction as long as at least one tile
    //  beneath is free, along with a tile along the perimeter.
    boolean underFree = false, perimFree = false;
    for (Tile t : world.tilesIn(footprint(), false)) {
      if (cannotUse(t)) return false;
      underFree |= t.habitat().pathClear;
    }
    for (Tile t : Spacing.perimeter(footprint(), world)) {
      if (cannotUse(t)) return false;
      perimFree |= t.habitat().pathClear;
    }
    return underFree && perimFree;
  }
  
  
  private boolean cannotUse(Tile t) {
    if (t == null) return true;
    final Element top = t.onTop();
    if (top instanceof ShieldWall) return false;
    return top != null && top.owningType() >= owningType();
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
  
  
  public String objectCategory() {
    return InstallTab.TYPE_MILITANT;
  }
}





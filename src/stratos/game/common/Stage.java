/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.common;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.start.*;
import stratos.user.*;
import stratos.util.*;



public class Stage {
  
  
  /**  Common fields, default constructors, and save/load methods-
    */
  private static boolean
    verbose = false;
  
  final public static int
    UPDATES_PER_SECOND = PlayLoop.UPDATES_PER_SECOND,
    
    PATCH_RESOLUTION  = 8 ,
    UNIT_GRID_SIZE    = 2 ,
    ZONE_SIZE         = 16,
    ZONE_AREA         = ZONE_SIZE * ZONE_SIZE,
    
    DAYS_PER_WEEK        = 6 ,
    SHIFTS_PER_DAY       = 3 ,
    DAYS_PER_YEAR        = 60,
    HOURS_PER_DAY        = 24,
    STANDARD_HOUR_LENGTH = 20, //  In real-time seconds
    
    STANDARD_DAY_LENGTH   = STANDARD_HOUR_LENGTH * HOURS_PER_DAY ,
    STANDARD_SHIFT_LENGTH = STANDARD_DAY_LENGTH  / SHIFTS_PER_DAY,
    STANDARD_WEEK_LENGTH  = STANDARD_DAY_LENGTH  * DAYS_PER_WEEK ,
    STANDARD_YEAR_LENGTH  = STANDARD_DAY_LENGTH  * DAYS_PER_YEAR ,
    
    GROWTH_INTERVAL    = STANDARD_DAY_LENGTH / 2,
    DEFAULT_INIT_TIME  = STANDARD_DAY_LENGTH / 3;
  
  
  final public int size;
  final Tile tiles[][];
  final public StageRegions sections;
  final public ClaimsGrid claims;
  
  final public Schedule schedule;
  private float currentTime = DEFAULT_INIT_TIME;
  private List <Mobile> mobiles = new List <Mobile> ();
  
  private StageTerrain terrain;
  private Ecology ecology;
  private List <Base> bases = new List <Base> ();
  
  final public Verse offworld = new Verse(this);
  
  final public Activities activities;
  final public PathingCache pathingCache;
  final public Presences presences;
  final public Ephemera ephemera;
  
  
  public Stage(int size) {
    this.size = size;
    tiles = new Tile[size][size];
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      tiles[c.x][c.y] = new Tile(this, c.x, c.y);
    }
    sections = new StageRegions(this, PATCH_RESOLUTION);
    claims   = new ClaimsGrid(this);
    schedule = new Schedule(currentTime);
    
    ecology      = new Ecology     (this);
    activities   = new Activities  (this);
    pathingCache = new PathingCache(this);
    presences    = new Presences   (this);
    ephemera     = new Ephemera    (this);
  }
  
  
  public Stage(StageTerrain terrain) {
    this(terrain.mapSize);
    this.terrain = terrain;
    terrain.initTerrainMesh();
  }
  
  
  public void loadState(Session s) throws Exception {
    //
    //  We load the tile-states first, as other objects may depend on this.
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      tiles[c.x][c.y].loadTileState(s);
    }
    claims.loadState(s);
    currentTime = s.loadFloat();
    schedule.loadFrom(s);
    
    terrain = (StageTerrain) s.loadObject();
    terrain.initTerrainMesh();
    ecology.loadState(s);
    s.loadObjects(bases);
    for (int n = s.loadInt(); n-- > 0;) {
      toggleActive((Mobile) s.loadObject(), true);
    }
    offworld.loadState(s);
    
    activities.loadState(s);
    presences.loadState(s);
    ephemera.loadState(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    //
    //  We save the tile-states first, as other objects may depend on this.
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      tiles[c.x][c.y].saveTileState(s);
    }
    claims.saveState(s);
    s.saveFloat(currentTime);
    schedule.saveTo(s);
    
    s.saveObject(terrain);
    ecology.saveState(s);
    
    s.saveObjects(bases);
    s.saveInt(mobiles.size());
    for (Mobile m : mobiles) s.saveObject(m);
    offworld.saveState(s);
    
    activities.saveState(s);
    presences.saveState(s);
    ephemera.saveState(s);
  }
  
  
  
  /**  Utility methods for visiting tiles at specific coordinates.
    */
  private static Vec3D tempV = new Vec3D();
  
  
  public Tile tileAt(float x, float y) {
    try { return tiles[(int) (x + 0.5f)][(int) (y + 0.5f)]; }
    catch (ArrayIndexOutOfBoundsException e) { return null; }
  }
  
  
  public Tile tileAtGrid(int x, int y, int unit) {
    return tileAt(Nums.round(x, unit, false), Nums.round(y, unit, false));
  }
  
  
  public Tile tileAtGrid(Tile t, int unit) {
    if (t == null) return null;
    return tileAtGrid(t.x, t.y, unit);
  }
  
  
  public Tile tileAt(int x, int y) {
    try { return tiles[x][y]; }
    catch (ArrayIndexOutOfBoundsException e) { return null; }
  }
  
  
  public Tile tileAt(Target t) {
    final Vec3D v = t.position(tempV);
    return v== null ? null : tileAt(v.x, v.y);
  }
  
  
  public Iterable <Tile> tilesIn(Box2D area, boolean safe) {
    final Box2D b = new Box2D().setTo(area);
    if (safe) b.cropBy(new Box2D().set(-0.5f, -0.5f, size, size));

    final Visit <Coord> grid = Visit.grid(b);
    return new Visit <Tile> () {

      public Tile next() {
        final Coord c = grid.next();
        return tileAt(c.x, c.y);
      }
      
      public boolean hasNext() {
        return grid.hasNext();
      }
    };
  }
  
  
  //  TODO:  Are these still needed?  Consider cutting out.
  public float surfaceAt(float x, float y, boolean floor) {
    //
    //  Sample the height of the 4 nearby tiles, and interpolate between them.
    final Tile o = tileAt((int) x, (int) y);
    final float
      xA = x - o.x, yA = y - o.y,
      TSW = heightFor(o.x    , o.y    , floor),
      TSE = heightFor(o.x + 1, o.y    , floor),
      TNW = heightFor(o.x    , o.y + 1, floor),
      TNE = heightFor(o.x + 1, o.y + 1, floor);
    return
      (((TSW * (1 - xA)) + (TSE * xA)) * (1 - yA)) +
      (((TNW * (1 - xA)) + (TNE * xA)) *      yA );
  }
  
  
  private float heightFor(int tX, int tY, boolean floor) {
    final Tile t = tileAt(Nums.clamp(tX, size), Nums.clamp(tY, size));
    if (t.above() == null) return t.elevation();
    return floor ? t.above().position(null).z : t.above().height();
  }
  
  
  
  /**  Update and scheduling methods.
    */
  public void updateWorld() {
    sections.updateBounds();
    final float oldTime = currentTime;
    currentTime += 1f / PlayLoop.UPDATES_PER_SECOND;
    
    boolean secChange = ((int) (oldTime / 2)) != ((int) (currentTime / 2));
    if (secChange && I.logEvents()) I.say("\nTime is "+currentTime);
    
    for (Base base : bases) {
      if (secChange) base.intelMap.updateFogValues();
    }
    if (secChange) offworld.updateVerse(currentTime);
    ecology.updateEcology();
    schedule.advanceSchedule(currentTime);
    for (Mobile m : mobiles) m.updateAsMobile();
  }
  
  
  protected void toggleActive(Mobile m, boolean is) {
    if (is) {
      m.setWorldEntry(mobiles.addLast(m));
      presences.togglePresence(m, m.origin(), true );
    }
    else {
      mobiles.removeEntry(m.worldEntry());
      presences.togglePresence(m, m.origin(), false);
    }
  }
  
  
  protected void registerBase(Base base, boolean active) {
    if (active) {
      bases.include(base);
      schedule.scheduleForUpdates(base);
    }
    else {
      schedule.unschedule(base);
      bases.remove(base);
    }
  }
  
  
  public void advanceCurrentTime(float interval) {
    this.currentTime += interval;
  }
  
  
  public List <Mobile> allMobiles() {
    return mobiles;
  }
  
  
  
  
  /**  Other query methods/accessors-
    */
  public StageTerrain terrain() {
    return terrain;
  }
  
  
  public Ecology ecology() {
    return ecology;
  }
  
  
  public float currentTime() {
    return currentTime;
  }
  
  
  public Box2D area() {
    return new Box2D().set(-0.5f, -0.5f, size, size);
  }
  
  
  public List <Base> bases() {
    return bases;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  //  TODO:  Move this out to wherever the Ephemera class ends up.
  public static interface Visible {
    void renderFor(Rendering r, Base b);
    Sprite sprite();
  }
  
  
  public Batch <StageRegion> visibleSections(Rendering rendering) {
    final Batch <StageRegion> visibleSections = new Batch <StageRegion> ();
    sections.compileVisible(rendering.view, null, visibleSections, null);
    return visibleSections;
  }
  
  
  public void renderFor(Rendering rendering, Base base) {
    if (verbose) I.say("Rendering world...");
    //
    //  Set a couple of basic parameters before beginning-
    final Colour c = Planet.lightValue(this);
    rendering.lighting.setup(c.r, c.g, c.b);
    //
    //  First, we obtain lists of all current visible fixtures, actors, and
    //  terrain sections.
    final Batch <StageRegion> visibleSections = new Batch <StageRegion> ();
    final List <Visible> allVisible = new List <Visible> () {
      protected float queuePriority(Visible e) {
        return e.sprite().depth;
      }
    };
    sections.compileVisible(
      rendering.view, base,
      visibleSections, allVisible
    );
    //
    //  We register terrain for rendering first, partly to update fog values
    //  for the purpose of allowing fog-culling of sprites-
    renderTerrain(visibleSections, rendering, base);
    //
    //  We also render visible mobiles and ghosted SFX-
    Vec3D viewPos = new Vec3D();
    float viewRad = -1;
    for (Mobile active : this.mobiles) {
      if (active.sprite() == null || ! active.visibleTo(base)) continue;
      active.viewPosition(viewPos);
      viewRad = (active.height() / 2) + active.radius();
      if (rendering.view.intersects(viewPos, viewRad)) {
        allVisible.add(active);
      }
    }
    //
    //  Then we register their associated media for rendering, in the correctly
    //  sorted order.
    Vec3D deep = new Vec3D();
    for (Visible visible : allVisible) {
      final Sprite sprite = visible.sprite();
      rendering.view.translateToScreen(deep.setTo(sprite.position));
      sprite.depth = deep.z;
    }
    allVisible.queueSort();
    for (Visible visible : allVisible) {
      visible.renderFor(rendering, base);
    }
    if (verbose) {
      I.say("  Total sections visible: "+visibleSections.size());
      I.say("  Total elements visible: "+allVisible     .size());
    }
    //
    //  Ephemera are rendered last, to accommodate transparency effects-
    allVisible.clear();
    for (Visible ghost : ephemera.visibleFor(rendering, base)) {
      allVisible.add(ghost);
    }
    allVisible.queueSort();
    for (Visible visible : allVisible) {
      visible.renderFor(rendering, base);
    }
    ephemera.applyScreenFade(rendering);
    if (verbose) {
      I.say("  Total ephemera visible: "+allVisible     .size());
    }
  }
  
  
  public float timeMidRender() {
    //  Updates to currentTime occur 10 times per second or so, so adding the
    //  frame-time within that fraction gives the exact answer.
    return currentTime + (PlayLoop.frameTime() / UPDATES_PER_SECOND);
  }
  
  
  protected void renderTerrain(
    Batch <StageRegion> sections, Rendering rendering, Base base
  ) {
    final float renderTime = timeMidRender();
    if (verbose) I.say("Render time: "+renderTime);
    terrain.readyAllMeshes();
    for (StageRegion section : sections) {
      terrain.renderFor(section.area, rendering, renderTime);
    }
    if (base != null && ! GameSettings.fogFree) {
      base.intelMap.updateAndRender(renderTime, rendering);
    }
  }
  
  
  public Vec3D pickedGroundPoint(
    final HUD UI, final Viewport view, float screenX, float screenY
  ) {
    //
    //  Here, we find the point of intersection between the line-of-sight
    //  underneath the mouse cursor, and the plane of the ground-
    final Vec3D origin = new Vec3D(screenX, screenY, 0);
    view.translateFromScreen(origin);
    final Vec3D vector = view.direction();
    vector.scale(0 - origin.z / vector.z);
    origin.add(vector);
    return origin;
  }
  
  
  public Tile pickedTile(final HUD UI, final Viewport port, Base base) {
    Vec3D onGround = pickedGroundPoint(UI, port, UI.mouseX(), UI.mouseY());
    return tileAt(onGround.x, onGround.y);
  }
  
  
  public Element pickedFixture(final HUD UI, final Viewport port, Base base) {
    final Tile t = pickedTile(UI, port, base);
    if (t == null) return null;
    
    //  TODO:  Revisit this.  Fade-in/fade-out would be nice.
    if (t.reserves() != null && t.reserves().visibleTo(base)) {
      return t.reserves();
    }
    else if (t.above() != null && t.above().visibleTo(base)) {
      return t.above();
    }
    else return null;
  }
  
  
  public Mobile pickedMobile(final HUD UI, final Viewport port, Base base) {
    //
    //  TODO:  You may want to use some pre-emptive culling here in future.
    Mobile nearest = null;
    float minDist = Float.POSITIVE_INFINITY;
    for (Mobile m : mobiles) {
      if (m.indoors() || ! (m instanceof Selectable)) continue;
      if (! m.visibleTo(base)) continue;
      final float selRad = (m.height() + m.radius()) / 2;
      final Vec3D selPos = m.viewPosition(null);
      if (! port.mouseIntersects(selPos, selRad, UI)) continue;
      final float dist = port.translateToScreen(selPos).z;
      if (dist < minDist) { nearest = m; minDist = dist; }
    }
    return nearest;
  }
}





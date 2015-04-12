/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.common;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import stratos.game.wild.Habitat;



public final class Tile implements
  Target, TileConstants, Boarding, Session.Saveable, Selectable
{
  
  final public static int
    PATH_ROAD    = 0,
    PATH_CLEAR   = 1,
    PATH_HINDERS = 2,
    PATH_BLOCKS  = 3;
  private static Stack <Mobile>
    NONE_INSIDE = new Stack <Mobile> ();
  
  private static boolean verbose = false;
  
  
  final public Stage world;
  final public int x, y;
  
  private Object flagged;
  private Boarding boardingCache[] = null;
  private boolean isEntrance = false;
  
  private float elevation = Float.NEGATIVE_INFINITY;
  private Habitat habitat = null;
  private Element onTop   = null;
  private Stack <Mobile> inside = NONE_INSIDE;
  
  
  
  /**  Basic constructor and save/load functionality-
    */
  protected Tile(Stage world, int x, int y) {
    this.world = world;
    this.x = x;
    this.y = y;
  }
  
  
  public static Tile loadConstant(Session s) throws Exception {
    return s.world().tileAt(s.loadInt(), s.loadInt());
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(x);
    s.saveInt(y);
  }
  
  
  protected void loadTileState(Session s) throws Exception {
    elevation = s.loadFloat();
    habitat = Habitat.ALL_HABITATS[s.loadInt()];
    onTop = (Element) s.loadObject();
    if (s.loadBool()) s.loadObjects(inside = new Stack <Mobile> ());
    else inside = NONE_INSIDE;
  }
  
  
  protected void saveTileState(Session s) throws Exception {
    s.saveFloat(elevation);
    s.saveInt(habitat().ID);
    s.saveObject(onTop);
    if (inside == NONE_INSIDE) s.saveBool(false);
    else { s.saveBool(true); s.saveObjects(inside); }
  }
  
  
  public StageSection worldSection() {
    return world.sections.sectionAt(x, y);
  }
  
  
  
  /**  Satisfying the target contract and other similar interfaces.
    */
  public boolean inWorld() { return true; }
  public boolean destroyed() { return false; }
  public boolean indoors() { return false; }
  public Stage world() { return world; }
  public Base base() { return null; }
  
  public Vec3D position(Vec3D v) {
    if (v == null) v = new Vec3D();
    return v.set(x, y, elevation());
  }
  
  public float radius() { return 0; }
  public float height() { return 0; }
  public boolean isMobile() { return false; }
  
  
  
  /**  Geographical methods-
    */
  public Habitat habitat() {
    if (habitat != null) return habitat;
    refreshHabitat();
    return habitat;
  }
  
  
  public void refreshHabitat() {
    habitat = world.terrain().habitatAt(x, y);
  }
  
  
  public float elevation() {
    if (elevation == Float.NEGATIVE_INFINITY) {
      elevation = world.terrain().trueHeight(x, y);
    }
    return elevation;
  }
  
  
  public Coord coords() {
    return new Coord(x, y);
  }
  
  
  
  /**  Setting path type and occupation-
    */
  public boolean blocked() {
    return pathType() >= PATH_BLOCKS;
  }
  
  
  public int pathType() {
    if (onTop != null) return onTop.pathType();
    if (world.terrain().isRoad(this)) return PATH_ROAD;
    return habitat().pathClear ? PATH_CLEAR : PATH_BLOCKS;
  }
  
  
  public boolean isEntrance() {
    canBoard();
    return isEntrance;
  }
  
  
  public Batch <Boarding> entranceFor() {
    final Batch <Boarding> batch = new Batch <Boarding> ();
    for (Boarding b : canBoard()) {
      if (b instanceof Element) batch.add(b);
    }
    return batch;
  }
  
  
  public boolean canPave() {
    if (! habitat().pathClear) return false;
    return onTop == null || onTop.base() == null;
  }
  
  
  public boolean reserved() {
    return onTop != null && onTop.owningTier() >= Owner.TIER_PRIVATE;
  }
  
  
  public int owningTier() {
    if (onTop == null) return Owner.TIER_NATURAL;
    else return onTop.owningTier();
  }
  
  
  public boolean buildable() {
    return habitat().pathClear && ! reserved();
  }
  
  
  public final void flagWith(final Object f) {
    flagged = f;
  }
  
  
  public final Object flaggedWith() {
    return flagged;
  }
  
  
  public Tile[] edgeAdjacent(Tile batch[]) {
    if (batch == null) batch = new Tile[T_ADJACENT.length];
    int i = 0; for (int n : T_ADJACENT) {
      batch[i++] = world.tileAt(x + T_X[n], y + T_Y[n]);
    }
    return batch;
  }
  
  
  public Tile[] allAdjacent(Tile batch[]) {
    if (batch == null) batch = new Tile[T_INDEX.length];
    for (int n : T_INDEX) {
      batch[n] = world.tileAt(x + T_X[n], y + T_Y[n]);
    }
    return batch;
  }
  
  
  public Tile[] vicinity(Tile batch[]) {
    if (batch == null) batch = new Tile[9];
    allAdjacent(batch);
    batch[8] = this;
    return batch;
  }
  
  
  public Element onTop() {
    return onTop;
  }
  
  
  public void setOnTop(Element e) {
    if (e == onTop) return;
    
    //  TODO:  AUTOMATICALLY DISPLACE PRIOR OCCUPANT (set as destroyed?)
    if (e != null && this.onTop != null) {
      I.complain("PREVIOUS OCCUPANT WAS NOT CLEARED: "+this.onTop);
    }
    
    this.onTop = e;
    
    if (this.onTop != null) {
      PavingMap.setPaveLevel(this, StageTerrain.ROAD_NONE, false);
    }
    
    if (verbose) {
      if (e != null) I.say(this+" now owned by: "+e);
      else I.say(this+" now cleared.");
    }
    
    world.sections.flagBoundsUpdate(x, y);
    refreshAdjacent();
  }
  
  
  public void clearUnlessOwned() {
    if (owningTier() > Owner.TIER_NATURAL) return;
    if (onTop != null) onTop.setAsDestroyed();
  }
  
  
  
  /**  Implementing the Boardable interface-
    */
  public void refreshAdjacent() {
    boardingCache = null;
    for (int n : T_INDEX) {
      final Tile t = world.tileAt(x + T_X[n], y + T_Y[n]);
      if (t != null) t.boardingCache = null;
    }
  }
  
  
  public Boarding[] canBoard() {
    if (boardingCache != null) return boardingCache;
    final Boarding batch[] = new Boarding[8];
    isEntrance = false;
    
    //  If you're actually occupied, allow boarding of the owner-
    if (blocked() && onTop() instanceof Boarding) {
      return boardingCache = ((Boarding) onTop()).canBoard();
    }
    
    //  Include any unblocked adjacent tiles-
    for (int n : T_INDEX) {
      batch[n] = null;
      final Tile t = world.tileAt(x + T_X[n], y + T_Y[n]);
      if (t == null || t.blocked()) continue;
      batch[n] = t;
    }
    
    //  Cull any diagonal tiles that are blocked by either adjacent neighbour-
    for (int i : Tile.T_DIAGONAL) if (batch[i] != null) {
      if (batch[(i + 7) % 8] == null) batch[i] = null;
      if (batch[(i + 1) % 8] == null) batch[i] = null;
    }
    
    //  Include anything that you're an entrance to-
    for (int n : T_ADJACENT) {
      final Tile t = world.tileAt(x + T_X[n], y + T_Y[n]);
      if (t == null || ! (t.onTop() instanceof Boarding)) continue;
      final Boarding v = (Boarding) t.onTop();
      if (v.isEntrance(this)) {
        batch[n] = v;
        isEntrance = true;
      }
    }
    
    //  Cache and return-
    boardingCache = batch;
    return batch;
  }
  
  
  public boolean isEntrance(Boarding b) {
    if (b instanceof Tile) {
      final Tile t = (Tile) b;
      if (t.blocked()) return false;
      return Spacing.maxAxisDist(this, t) < 2;
    }
    return (b == null) ? false : b.isEntrance(this);
  }
  
  
  public boolean allowsEntry(Mobile m) {
    return true;
  }
  
  
  public int boardableType() {
    return BOARDABLE_TILE;
  }
  
  
  public Box2D area(Box2D put) {
    if (put == null) put = new Box2D();
    put.set(x - 0.5f, y - 0.5f, 1, 1);
    return put;
  }
  
  
  public void setInside(Mobile m, boolean is) {
    if (is) {
      if (inside == NONE_INSIDE) inside = new Stack <Mobile> ();
      inside.include(m);
    }
    else {
      inside.remove(m);
      if (inside.size() == 0) inside = NONE_INSIDE;
    }
  }
  
  
  public Stack <Mobile> inside() {
    return inside;
  }
  
  
  
  /**  Interface and media-
    */
  public String fullName() {
    return toString();
  }
  
  
  public void whenClicked() {
    //  TODO:  Open a simple display pane to give basic information on the
    //         habitat type?
    final BaseUI UI = BaseUI.current();
    UI.selection.pushSelection(null);
    UI.tracking.lockOn(this);
  }
  
  
  public String toString() {
    if (habitat == null) return "Tile at X"+x+" Y"+y;
    return habitat.name+" at X"+x+" Y"+y;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return null;
  }
  
  
  public SelectionPane configPanel(SelectionPane panel, BaseUI UI) {
    return null;
  }
  
  
  public String helpInfo() {
    final Habitat h = this.habitat();
    if (h != null) return h.info;
    return "NO HELP ON THIS ITEM";
  }
  
  
  public String objectCategory() {
    return UIConstants.TYPE_TERRAIN;
  }
  
  
  public TargetOptions configInfo(TargetOptions info, BaseUI UI) {
    if (info == null) info = new TargetOptions(UI, this);
    return info;
  }
  
  
  public Target selectionLocksOn() {
    return this;
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    //  TODO:  Consider a gentle 'fuzz' over the area concerned?
  }
}








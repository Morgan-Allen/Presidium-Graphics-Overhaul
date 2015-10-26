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
import static stratos.game.maps.StageTerrain.*;



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
  
  private Element above = null, reserves = null;
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
    above     = (Element) s.loadObject();
    reserves  = (Element) s.loadObject();
    if (s.loadBool()) s.loadObjects(inside = new Stack <Mobile> ());
    else inside = NONE_INSIDE;
  }
  
  
  protected void saveTileState(Session s) throws Exception {
    s.saveFloat (elevation);
    s.saveObject(above    );
    s.saveObject(reserves );
    if (inside == NONE_INSIDE) s.saveBool(false);
    else { s.saveBool(true); s.saveObjects(inside); }
  }
  
  
  public StageRegion worldSection() {
    return world.regions.regionAt(x, y);
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
  
  
  public final void flagWith(final Object f) {
    flagged = f;
  }
  
  
  public final Object flaggedWith() {
    return flagged;
  }
  
  
  
  /**  Geographical methods-
    */
  public Habitat habitat() {
    return world.terrain().habitatAt(x, y);
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
  
  
  public boolean pathClear() {
    return pathType() <= PATH_CLEAR;
  }
  
  
  public int pathType() {
    if (above != null) return above.pathType();
    if (world.terrain().isRoad(this)) return PATH_ROAD;
    return habitat().pathClear ? PATH_CLEAR : PATH_BLOCKS;
  }
  
  
  public boolean isEntrance() {
    canBoard();
    return isEntrance;
  }
  
  
  public Batch <Boarding> entranceFor() {
    final Batch <Boarding> batch = new Batch <Boarding> ();
    for (Tile t : edgeAdjacent(null)) {
      if (t.reserves() instanceof Boarding) {
        final Boarding a = (Boarding) t.reserves();
        if (a.isEntrance(this)) batch.add(a);
      }
    }
    return batch;
  }
  
  
  public boolean canPave() {
    if (! habitat().pathClear) return false;
    return
      above == null ||
      above.owningTier() <  Owner.TIER_PRIVATE ||
      above.pathType  () <= Tile .PATH_CLEAR;
  }
  
  
  public boolean reserved() {
    return reserves != null && reserves.owningTier() >= Owner.TIER_PRIVATE;
  }
  
  
  public int owningTier() {
    return Nums.max(
      (above    == null ? Owner.TIER_TERRAIN : above   .owningTier()),
      (reserves == null ? Owner.TIER_TERRAIN : reserves.owningTier())
    );
  }
  
  
  public boolean buildable() {
    return habitat().pathClear && ! reserved();
  }
  
  
  public Element reserves() {
    return reserves;
  }
  
  
  public void setReserves(Element e, boolean isUnder) {
    this.reserves = e;
    
    boolean shouldShow = reserved() && above != reserves && isUnder;
    world.terrain().setReservedAt(this, shouldShow);
    refreshAdjacent();
  }
  
  
  public Element above() {
    return above;
  }
  
  
  public void setAbove(Element e, boolean reserves) {
    if (e == above && (e == this.reserves || ! reserves)) return;
    
    final Element a = this.above;
    this.above = e;
    
    final boolean pave = e != null && e.pathType() == PATH_ROAD;
    final byte roadLevel = pave ? ROAD_LIGHT : ROAD_NONE;
    PavingMap.setPaveLevel(this, roadLevel, false);
    
    setReserves(reserves ? e : this.reserves, reserves);
    world.regions.flagBoundsUpdate(x, y);
    
    if (e != null && a != null) {
      I.complain("PREVIOUS OCCUPANT WAS NOT CLEARED: "+this.above);
    }
  }
  
  
  public void clearUnlessOwned(boolean instant) {
    if (above != null && (above != reserves || ! reserved())) {
      if (instant) above.exitWorld();
      else above.setAsDestroyed(false);
    }
    refreshAdjacent();
  }
  
  
  public void clearUnlessOwned() {
    clearUnlessOwned(false);
  }
  
  
  
  /**  Implementing the Boardable interface-
    */
  public void refreshAdjacent() {
    boardingCache = null;
    elevation = Float.NEGATIVE_INFINITY;
    for (int n : T_INDEX) {
      final Tile t = world.tileAt(x + T_X[n], y + T_Y[n]);
      if (t != null) t.boardingCache = null;
    }
  }
  
  
  public Boarding[] canBoard() {
    if (boardingCache != null) return boardingCache;
    final Boarding batch[] = new Boarding[8];
    isEntrance = false;
    boolean flagReserve = false;
    
    //  If you're actually occupied, allow boarding of the owner-
    if (blocked() && above() instanceof Boarding) {
      return boardingCache = ((Boarding) above()).canBoard();
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
      if (t == null || ! (t.reserves() instanceof Boarding)) continue;
      final Boarding v = (Boarding) t.reserves();
      if (v.isEntrance(this)) {
        isEntrance = true;
        if (v == t.above()) batch[n] = v;
        else flagReserve = true;
      }
    }
    
    //  Flag yourself for reservation display ONLY if you're an entrance for
    //  something unbuilt...
    //  TODO:  Consider retaining this, but with a less obvious display- like
    //  a swing-door symbol, or a ramp-icon, or similar?
    ///if (reserves == null) world.terrain().setReservedAt(this, flagReserve);
    
    //  Cache and return-
    boardingCache = batch;
    return batch;
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
    final Habitat h = habitat();
    if (h == null) return "Tile at X"+x+" Y"+y;
    else return h.name+" at X"+x+" Y"+y;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return null;
  }
  
  
  public SelectionPane configSelectPane(SelectionPane panel, BaseUI UI) {
    return null;
  }
  
  
  public String helpInfo() {
    final Habitat h = this.habitat();
    if (h != null) return h.info;
    return "NO HELP ON THIS ITEM";
  }
  
  
  public String objectCategory() {
    return Target.TYPE_TERRAIN;
  }
  
  
  public Constant infoSubject() {
    //  TODO:  Add info here!!!
    return null;
  }
  
  
  public SelectionOptions configSelectOptions(SelectionOptions info, BaseUI UI) {
    if (info == null) info = new SelectionOptions(UI, this);
    return info;
  }
  
  
  public Target selectionLocksOn() {
    return this;
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    //  TODO:  Consider a gentle 'fuzz' over the area concerned?
  }
}








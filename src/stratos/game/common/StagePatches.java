/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.common;
import stratos.graphics.common.*;
import stratos.util.*;



public class StagePatches implements TileConstants {
  
  
  /**  Common fields, constructors and utility methods.
    */
  final Stage world;
  final public int resolution, depth, gridSize, gridCount;
  final StagePatch hierarchy[][][], root;
  
  
  private int depthFor(int size) {
    int s = 1, d = 1;
    while (s < size) { s *= 2; d++; }
    return d;
  }
  
  
  protected StagePatches(Stage world, int resolution) {
    this.world      = world;
    this.resolution = resolution;
    this.gridSize   = world.size / resolution;
    this.gridCount  = gridSize * gridSize;
    this.depth      = depthFor(gridSize);
    this.hierarchy  = new StagePatch[depth][][];
    //
    //  Next, we generate each level of the map, and initialise nodes for each-
    int gridSize = world.size / resolution, nodeSize = resolution, deep = 0;
    while (deep < depth) {
      hierarchy[deep] = new StagePatch[gridSize][gridSize];
      for (Coord c : Visit.grid(0, 0, gridSize, gridSize, 1)) {
        final StagePatch n = new StagePatch(this, c.x, c.y, deep);
        hierarchy[deep][c.x][c.y] = n;
        n.area.set(
          (c.x * nodeSize) - 0.5f,
          (c.y * nodeSize) - 0.5f,
          nodeSize,
          nodeSize
        );
        //I.say("INIT AREA: "+n.area);
        //
        //  Along with references to child nodes-
        if (deep > 0) {
          final int d = n.depth - 1, x = c.x * 2, y = c.y * 2;
          n.kids = new StagePatch[4];
          n.kids[0] = hierarchy[d][x    ][y    ];
          n.kids[1] = hierarchy[d][x + 1][y    ];
          n.kids[2] = hierarchy[d][x    ][y + 1];
          n.kids[3] = hierarchy[d][x + 1][y + 1];
          for (StagePatch k : n.kids) k.parent = n;
        }
      }
      deep++;
      gridSize /= 2;
      nodeSize *= 2;
    }
    this.root = hierarchy[deep - 1][0][0];
  }
  
  
  
  /**  Positional queries and iteration methods-
    */
  public StagePatch patchAt(int x, int y) {
    return hierarchy[0][x / resolution][y / resolution];
  }
  
  
  public StagePatch[] neighbours(StagePatch section, StagePatch[] batch) {
    if (batch == null) batch = new StagePatch[8];
    int i = 0; for (int n : T_INDEX) {
      try {
        final int x = section.x + T_X[n], y = section.y + T_Y[n];
        final StagePatch s = hierarchy[section.depth][x][y];
        batch[i++] = s;
      }
      catch (ArrayIndexOutOfBoundsException e) { batch [i++] = null; }
    }
    return batch;
  }
  
  
  public Batch <StagePatch> patchesUnder(Box2D area, int tileMargin) {
    final Batch <StagePatch> batch = new Batch <StagePatch> ();
    
    final float s = 1f / resolution, dim = world.size / resolution;
    final Box2D clip = new Box2D();
    clip.setX(area.xpos() * s, area.xdim() * s);
    clip.setY(area.ypos() * s, area.ydim() * s);
    clip.expandBy(Nums.round((1 + tileMargin) * s, 1, true));
    
    for (Coord c : Visit.grid(clip)) {
      if (c.x < 0 || c.x >= dim || c.y < 0 || c.y >= dim) continue;
      final StagePatch under = hierarchy[0][c.x][c.y];
      if (under.area.axisDistance(area) > tileMargin) continue;
      batch.add(under);
    }
    return batch;
  }
  
  
  public Batch <StagePatch> allGridPatches() {
    return patchesUnder(world.area(), 0);
  }
  
  
  
  /**  A simple interface for performing recursive descents into the hierarchy
    *  of world sections-
    */
  public static interface Descent {
    boolean descendTo(StagePatch s);
    void afterChildren(StagePatch s);
  }
  
  
  public void applyDescent(Descent d) {
    descendTo(root, d);
  }
  
  
  private void descendTo(StagePatch s, Descent d) {
    if (! d.descendTo(s)) return;
    if (s.depth > 0) for (StagePatch k : s.kids) descendTo(k, d);
    d.afterChildren(s);
  }
  
  
  /**  Updating the bounds information for a given Section-
    */
  protected void updateBounds() {
    final Descent update = new Descent() {
      public boolean descendTo(StagePatch s) {
        //
        //  If the section has already been updated, or isn't a leaf node,
        //  return.
        if (! s.updateBounds) return false;
        if (s.depth > 0) return true;
        //
        //  Bounds have to be initialised with a starting interval, so we pick
        //  the first tile in the area-
        final Tile first = world.tileAt(s.area.xpos(), s.area.ypos());
        s.bounds.set(first.x, first.y, first.elevation(), 0, 0, 0);
        final Box3D tempBounds = new Box3D();
        for (Tile t : world.tilesIn(s.area, false)) {
          s.bounds.include(t.x, t.y, t.elevation(), 1);
        }
        for (Element e : fixturesFrom(s.area, null)) {
          s.bounds.include(boundsFrom(e, tempBounds));
        }
        return true;
      }
      //
      //  All non-leaf nodes base their bounds on the limits of their children.
      public void afterChildren(StagePatch s) {
        s.updateBounds = false;
        if (s.depth == 0) return;
        s.bounds.setTo(s.kids[0].bounds);
        for (StagePatch k : s.kids) s.bounds.include(k.bounds);
      }
    };
    this.applyDescent(update);
  }
  
  
  protected Box3D boundsFrom(Element e, Box3D b) {
    final Tile t = e.origin();
    return b.set(
      t.x - 1, t.y - 1, t.elevation(),
      e.xdim() + 1, e.ydim() + 1, e.zdim()
    );
  }
  
  
  
  /**  Flags the sections hierarchy for updates, propagated up from the given
    *  tile coordinates-
    */
  protected void flagBoundsUpdate(int x, int y) {
    StagePatch toFlag = hierarchy[0][x / resolution][y / resolution];
    while (toFlag != null) {
      if (toFlag.updateBounds) break;
      toFlag.updateBounds = true;
      toFlag = toFlag.parent;
    }
  }
  
  
  
  /**  Returns a list of all static elements visible to the given viewport.
    */
  public void compileVisible(
    final Viewport view, final Base base,
    final Batch <StagePatch> visibleSections,
    final List <Stage.Visible> visibleFixtures
  ) {
    //
    //  We flag all encountered fixtures (which can occupy multiple tiles or
    //  sections) to prevent duplicate rendering-attempts.
    final Batch <Element> fixtures = new Batch(100);
    final Descent compile = new Descent() {
      
      public boolean descendTo(StagePatch s) {
        final Box3D b = s.bounds;
        return view.intersects(b.centre(), b.diagonal() / 2);
      }
      
      public void afterChildren(StagePatch s) {
        if (s.depth > 0) return;
        visibleSections.add(s);
        if (visibleFixtures != null) fixturesFrom(s.area, fixtures);
      }
    };
    this.applyDescent(compile);
    //
    //  We can then handle/unflag those in a separate pass-
    final Box3D tempBounds = new Box3D();
    for (Element e : fixtures) {
      if (e.sprite() == null) continue;
      final Box3D b = boundsFrom(e, tempBounds);
      if (! view.intersects(b.centre(), b.diagonal() / 2)) continue;
      if (e.visibleTo(base)) visibleFixtures.add(e);
    }
    for (Element e : fixtures) e.flagWith(null);
  }
  
  
  protected Batch <Element> fixturesFrom(Box2D area, Batch <Element> from) {
    final boolean ownPass = from == null;
    if (ownPass) from = new Batch <Element> ();
    
    for (Tile t : world.tilesIn(area, true)) {
      final Element o = t.above();
      if (o == null || o.flaggedWith() != null) continue;
      o.flagWith(from);
      from.add(o);
    }
    
    if (ownPass) for (Element e : from) e.flagWith(null);
    return from;
  }
}








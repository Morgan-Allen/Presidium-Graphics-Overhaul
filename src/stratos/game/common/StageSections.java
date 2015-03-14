/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.common;
import stratos.graphics.common.*;
import stratos.util.*;



public class StageSections implements TileConstants {
  
  
  /**  Common fields, constructors and utility methods.
    */
  final Stage world;
  final public int resolution, depth;
  final StageSection hierarchy[][][], root;
  
  
  private int depthFor(int size) {
    int s = 1, d = 1;
    while (s < size) { s *= 2; d++; }
    return d;
  }
  
  
  protected StageSections(Stage world, int resolution) {
    this.world = world;
    this.resolution = resolution;
    this.depth = depthFor(world.size / resolution);
    this.hierarchy = new StageSection[depth][][];
    //
    //  Next, we generate each level of the map, and initialise nodes for each-
    int gridSize = world.size / resolution, nodeSize = resolution, deep = 0;
    while (deep < depth) {
      hierarchy[deep] = new StageSection[gridSize][gridSize];
      for (Coord c : Visit.grid(0, 0, gridSize, gridSize, 1)) {
        final StageSection n = new StageSection(this, c.x, c.y, deep);
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
          n.kids = new StageSection[4];
          n.kids[0] = hierarchy[d][x    ][y    ];
          n.kids[1] = hierarchy[d][x + 1][y    ];
          n.kids[2] = hierarchy[d][x    ][y + 1];
          n.kids[3] = hierarchy[d][x + 1][y + 1];
          for (StageSection k : n.kids) k.parent = n;
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
  public StageSection sectionAt(int x, int y) {
    return hierarchy[0][x / resolution][y / resolution];
  }
  
  
  public StageSection[] neighbours(StageSection section, StageSection[] batch) {
    if (batch == null) batch = new StageSection[8];
    int i = 0; for (int n : T_INDEX) {
      try {
        final int x = section.x + T_X[n], y = section.y + T_Y[n];
        final StageSection s = hierarchy[section.depth][x][y];
        batch[i++] = s;
      }
      catch (ArrayIndexOutOfBoundsException e) { batch [i++] = null; }
    }
    return batch;
  }
  
  
  public Batch <StageSection> sectionsUnder(Box2D area, int margin) {
    final Batch <StageSection> batch = new Batch <StageSection> ();
    
    final float s = 1f / resolution, dim = world.size / resolution;
    final Box2D clip = new Box2D();
    clip.setX(area.xpos() * s, area.xdim() * s);
    clip.setY(area.ypos() * s, area.ydim() * s);
    clip.expandBy(1 + margin);
    
    for (Coord c : Visit.grid(clip)) {
      if (c.x < 0 || c.x >= dim || c.y < 0 || c.y >= dim) continue;
      final StageSection under = hierarchy[0][c.x][c.y];
      if (! under.area.overlaps(area)) continue;
      batch.add(under);
    }
    return batch;
  }
  
  
  
  /**  A simple interface for performing recursive descents into the hierarchy
    *  of world sections-
    */
  public static interface Descent {
    boolean descendTo(StageSection s);
    void afterChildren(StageSection s);
  }
  
  
  public void applyDescent(Descent d) {
    descendTo(root, d);
  }
  
  
  private void descendTo(StageSection s, Descent d) {
    if (! d.descendTo(s)) return;
    if (s.depth > 0) for (StageSection k : s.kids) descendTo(k, d);
    d.afterChildren(s);
  }
  
  
  /**  Updating the bounds information for a given Section-
    */
  protected void updateBounds() {
    final Descent update = new Descent() {
      public boolean descendTo(StageSection s) {
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
        for (Element e : world.fixturesFrom(s.area)) {
          s.bounds.include(boundsFrom(e, tempBounds));
        }
        //I.say("Area "+s.area+"\nBounds: "+s.bounds+"\n___DEPTH: "+s.depth);
        return true;
      }
      //
      //  All non-leaf nodes base their bounds on the limits of their children.
      public void afterChildren(StageSection s) {
        s.updateBounds = false;
        if (s.depth == 0) return;
        s.bounds.setTo(s.kids[0].bounds);
        for (StageSection k : s.kids) s.bounds.include(k.bounds);
        //I.say("UPDATED BOUNDS: "+s.bounds);
        //I.say("Area "+s.area+"\nBounds: "+s.bounds+"\n___DEPTH: "+s.depth);
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
    StageSection toFlag = hierarchy[0][x / resolution][y / resolution];
    while (toFlag != null) {
      if (toFlag.updateBounds) break;
      toFlag.updateBounds = true;
      toFlag = toFlag.parent;
    }
  }
  
  
  
  /**  Returns a list of all static elements visible to the given viewport.
    */
  //
  //  TODO:  This might be moved to the rendering method of the World instead?
  public void compileVisible(
    final Viewport view, final Base base,
    final Batch <StageSection> visibleSections,
    final List <Stage.Visible> visibleFixtures
  ) {
    final Descent compile = new Descent() {
      final Box3D tempBounds = new Box3D();
      
      public boolean descendTo(StageSection s) {
        final Box3D b = s.bounds;
        return view.intersects(b.centre(), b.diagonal() / 2);
      }
      
      public void afterChildren(StageSection s) {
        if (s.depth > 0) return;
        visibleSections.add(s);
        if (visibleFixtures != null) {
          for (Element e : world.fixturesFrom(s.area)) {
            if (e.sprite() == null) continue;
            final Box3D b = boundsFrom(e, tempBounds);
            if (! view.intersects(b.centre(), b.diagonal() / 2)) continue;
            if (e.visibleTo(base)) visibleFixtures.add(e);
          }
        }
      }
    };
    this.applyDescent(compile);
  }
}






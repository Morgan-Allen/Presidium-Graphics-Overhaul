/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package code.game.common ;
import org.apache.commons.math3.util.FastMath;

import code.graphics.common.*;
import code.util.*;



public class WorldSections implements TileConstants {
  
  
  /**  Common fields, constructors and utility methods.
    */
  final World world ;
  final public int resolution, depth ;
  final Section hierarchy[][][], root ;
  
  //
  //  NOTE:  x and y coordinates are relative to position in hierarchy, not to
  //  tile coordinates.
  public static class Section {
    
    private boolean updateBounds = true ;
    
    final public Box3D bounds = new Box3D() ;
    final public Box2D area = new Box2D() ;
    final public int x, y, depth ;
    protected Section kids[], parent ;
    
    
    Section(int x, int y, int d) {
      this.x = x ;
      this.y = y ;
      this.depth = d ;
    }
  }
  
  
  public static Section loadSection(Session s) throws Exception {
    return s.world().sections.sectionAt(s.loadInt(), s.loadInt()) ;
  }
  
  public static void saveSection(Section sS, Session s) throws Exception {
    s.saveInt((int) (sS.area.xpos() + 1)) ;
    s.saveInt((int) (sS.area.ypos() + 1)) ;
  }
  
  
  private int depthFor(int size) {
    int s = 1, d = 1 ;
    while (s < size) { s *= 2 ; d++ ; }
    return d ;
  }
  
  
  protected WorldSections(World world, int resolution) {
    this.world = world ;
    this.resolution = resolution ;
    this.depth = depthFor(world.size / resolution) ;
    this.hierarchy = new Section[depth][][] ;
    //
    //  Next, we generate each level of the map, and initialise nodes for each-
    int gridSize = world.size / resolution, nodeSize = resolution, deep = 0 ;
    while (deep < depth) {
      hierarchy[deep] = new Section[gridSize][gridSize] ;
      for (Coord c : Visit.grid(0, 0, gridSize, gridSize, 1)) {
        final Section n = new Section(c.x, c.y, deep) ;
        hierarchy[deep][c.x][c.y] = n ;
        n.area.set(
          (c.x * nodeSize) - 0.5f,
          (c.y * nodeSize) - 0.5f,
          nodeSize,
          nodeSize
        ) ;
        //I.say("INIT AREA: "+n.area) ;
        //
        //  Along with references to child nodes-
        if (deep > 0) {
          final int d = n.depth - 1, x = c.x * 2, y = c.y * 2 ;
          n.kids = new Section[4] ;
          n.kids[0] = hierarchy[d][x    ][y    ] ;
          n.kids[1] = hierarchy[d][x + 1][y    ] ;
          n.kids[2] = hierarchy[d][x    ][y + 1] ;
          n.kids[3] = hierarchy[d][x + 1][y + 1] ;
          for (Section k : n.kids) k.parent = n ;
        }
      }
      deep++ ;
      gridSize /= 2 ;
      nodeSize *= 2 ;
    }
    this.root = hierarchy[deep - 1][0][0] ;
  }
  
  
  public Section sectionAt(int x, int y) {
    return hierarchy[0][x / resolution][y / resolution] ;
  }
  
  
  public Section[] neighbours(Section section, Section[] batch) {
    if (batch == null) batch = new Section[8] ;
    int i = 0 ; for (int n : N_INDEX) {
      try {
        final int x = section.x + N_X[n], y = section.y + N_Y[n] ;
        final Section s = hierarchy[section.depth][x][y] ;
        batch[i++] = s ;
      }
      catch (ArrayIndexOutOfBoundsException e) { batch [i++] = null ; }
    }
    return batch ;
  }
  
  
  
  /**  A simple interface for performing recursive descents into the hierarchy
    *  of world sections-
    */
  public static interface Descent {
    boolean descendTo(Section s) ;
    void afterChildren(Section s) ;
  }
  
  
  public void applyDescent(Descent d) {
    descendTo(root, d) ;
  }
  
  
  private void descendTo(Section s, Descent d) {
    if (! d.descendTo(s)) return ;
    if (s.depth > 0) for (Section k : s.kids) descendTo(k, d) ;
    d.afterChildren(s) ;
  }
  
  
  /**  Updating the bounds information for a given Section-
    */
  protected void updateBounds() {
    final Descent update = new Descent() {
      public boolean descendTo(Section s) {
        //
        //  If the section has already been updated, or isn't a leaf node,
        //  return.
        if (! s.updateBounds) return false ;
        if (s.depth > 0) return true ;
        //
        //  Bounds have to be initialised with a starting interval, so we pick
        //  the first tile in the area-
        final Tile first = world.tileAt(s.area.xpos(), s.area.ypos()) ;
        s.bounds.set(first.x, first.y, first.elevation(), 0, 0, 0) ;
        final Box3D tempBounds = new Box3D() ;
        for (Tile t : world.tilesIn(s.area, false)) {
          s.bounds.include(t.x, t.y, t.elevation(), 1) ;
        }
        for (Element e : world.fixturesFrom(s.area)) {
          s.bounds.include(boundsFrom(e, tempBounds)) ;
        }
        //I.say("Area "+s.area+"\nBounds: "+s.bounds+"\n___DEPTH: "+s.depth) ;
        return true ;
      }
      //
      //  All non-leaf nodes base their bounds on the limits of their children.
      public void afterChildren(Section s) {
        s.updateBounds = false ;
        if (s.depth == 0) return ;
        s.bounds.setTo(s.kids[0].bounds) ;
        for (Section k : s.kids) s.bounds.include(k.bounds) ;
        //I.say("UPDATED BOUNDS: "+s.bounds) ;
        //I.say("Area "+s.area+"\nBounds: "+s.bounds+"\n___DEPTH: "+s.depth) ;
      }
    } ;
    this.applyDescent(update) ;
  }
  
  
  protected Box3D boundsFrom(Element e, Box3D b) {
    final Tile t = e.origin() ;
    return b.set(
      t.x - 1, t.y - 1, t.elevation(),
      e.xdim() + 1, e.ydim() + 1, e.zdim()
    ) ;
  }
  
  
  /**  Flags the sections hierarchy for updates, propagated up from the given
    *  tile coordinates-
    */
  protected void flagBoundsUpdate(int x, int y) {
    Section toFlag = hierarchy[0][x / resolution][y / resolution] ;
    while (toFlag != null) {
      if (toFlag.updateBounds) break ;
      toFlag.updateBounds = true ;
      toFlag = toFlag.parent ;
    }
  }
  
  
  
  /**  Returns a list of all static elements visible to the given viewport.
    */
  //
  //  TODO:  This might be moved to the rendering method of the World instead?
  public void compileVisible(
    final Viewport view, final Base base,
    final Batch <Section> visibleSections,
    final List <World.Visible> visibleFixtures
  ) {
    final Descent compile = new Descent() {
      final Box3D tempBounds = new Box3D();
      
      public boolean descendTo(Section s) {
        final Box3D b = s.bounds;
        return view.intersects(b.centre(), b.diagonal() / 2);
      }
      
      public void afterChildren(Section s) {
        if (s.depth > 0) return ;
        visibleSections.add(s) ;
        if (visibleFixtures != null) {
          for (Element e : world.fixturesFrom(s.area)) {
            if (e.sprite() == null) continue ;
            final Box3D b = boundsFrom(e, tempBounds);
            if (! view.intersects(b.centre(), b.diagonal() / 2)) continue ;
            if (e.visibleTo(base)) visibleFixtures.add(e) ;
          }
        }
      }
    } ;
    this.applyDescent(compile) ;
  }
}






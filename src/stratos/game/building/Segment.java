


package stratos.game.building ;
import java.lang.reflect.* ;

import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.terrain.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



//  TODO:  Have this extend Fixture instead of Venue!  (It just needs a
//  structure, not personnel or stocks or visitors.)


public abstract class Segment extends Venue implements TileConstants {
  
  
  /**  Setup and save/load methods-
    */
  private static boolean verbose = false;
  
  protected int facing = -1, type = -1 ;
  
  
  public Segment(int size, int high, Base base) {
    super(size, high, Venue.ENTRANCE_NONE, base) ;
  }
  
  
  public Segment(Session s) throws Exception {
    super(s) ;
    this.facing = s.loadInt() ;
    this.type = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(facing) ;
    s.saveInt(type) ;
  }
  
  
  
  /**  Behaviour implementations/overrides (not generally used/needed)-
    */
  public Behaviour jobFor(Actor actor) { return null ; }
  public Background[] careers() { return null ; }
  public Service[] services() { return null ; }
  protected void updatePaving(boolean inWorld) {}
  
  
  
  /**  Helper methods for placement-
    */
  protected abstract boolean lockToGrid();
  protected abstract Segment instance(Base base);
  protected abstract void configFromAdjacent(boolean near[], int numNear) ;
  
  
  
  protected List <Segment> installedBetween(Tile start, Tile end) {
    
    //  Basic variables setup and sanity checks-
    if (start == null) return null ;
    if (end   == null) end = start ;

    final int unit = this.size;
    final World world = start.world;
    int stepX = unit, stepY = unit;
    
    if (lockToGrid()) {
      start = world.tileAt((start.x / unit) * unit, (start.y / unit) * unit);
    }

    //  Choose the best of 2 straight lines leading outward from the origin.
    final Tile
      goesVert  = world.tileAt(end.x, start.y),
      goesHoriz = world.tileAt(start.x, end.y);
    if (Spacing.distance(end, goesVert) < Spacing.distance(end, goesHoriz)) {
      end = goesVert;
      stepY = 0;
      stepX *= (end.x > start.x) ? 1 : -1;
    }
    else {
      end = goesHoriz;
      stepX = 0;
      stepY *= (end.y > start.y) ? 1 : -1;
    }
    final int maxDist = Spacing.maxAxisDist(start, end);
    
    //  Initialise segments at regular intervals along this line.
    final List <Segment> installed = new List <Segment> ();
    int initX = start.x, initY = start.y;
    while (true) {
      final Tile t = world.tileAt(initX, initY);
      if (Spacing.maxAxisDist(start, t) > maxDist) break;
      final Segment v = instance(base());
      if (v == null) continue;
      v.setPosition(t.x, t.y, world);
      installed.add(v);
      initX += stepX;
      initY += stepY;
    }
    
    //  Then determine their facing/appearance, and return.
    for (Segment s : installed) s.refreshFromNear(installed);
    return installed ;
  }
  
  
  private void refreshFromNear(List <Segment> prior) {
    final Tile o = origin() ;
    if (o == null) return ;
    final World world = o.world ;
    
    if (prior != null) for (Segment s : prior) s.origin().flagWith(this) ;
    
    final int unit = this.size;
    final boolean near[] = new boolean[8];
    int numNear = 0;
    
    for (int i : N_ADJACENT) {
      final Tile n = world.tileAt(o.x + (N_X[i] * unit), o.y + (N_Y[i] * unit));
      if (n == null) continue ;
      boolean isNear = false ;
      if (n.owner() instanceof Segment) {
        final Segment s = (Segment) n.owner();
        if (s.origin() == n && s.getClass() == this.getClass()) isNear = true;
      }
      if (n.flaggedWith() == this) isNear = true ;
      if (isNear) { numNear++ ; near[i] = true ; }
    }
    
    if (prior != null) for (Segment s : prior) s.origin().flagWith(null) ;
    configFromAdjacent(near, numNear) ;
  }
  
  
  
  /**  Placement interface and rendering tweaks-
    */
  private List <Segment> toInstall = null;// new List() ;
  
  
  private Tile previewAt() {
    final int HS = this.size / 2;
    Tile at = origin();
    at = at.world.tileAt(at.x + HS, at.y + HS) ;
    return at;
  }
  
  
  private void superPlacing(List <Segment> prior) {
    final Tile at = previewAt();
    super.doPlace(at, at) ;
    
    final Tile o = origin() ;
    final World world = o.world ;
    for (int i : N_ADJACENT) {
      final Tile n = world.tileAt(o.x + (N_X[i] * 2), o.y + (N_Y[i] * 2)) ;
      if (n == null) continue ;
      if (n.owner() != null && n.owner().getClass() == this.getClass()) {
        final Segment s = (Segment) n.owner() ;
        if (prior != null && prior.includes(s)) continue ;
        s.refreshFromNear(prior) ;
      }
    }
  }
  
  
  private void superPreview(boolean canPlace, Rendering rendering) {
    final Tile at = previewAt();
    super.preview(canPlace, rendering, at, at) ;
  }
  
  
  private boolean superPointsOkay() {
    final Tile at = previewAt();
    return super.pointsOkay(at, at) ;
  }
  
  
  public boolean canPlace() {
    if (super.canPlace()) return true ;
    ///I.say("Couldn't place normally!") ;
    final Tile o = origin() ;
    if (o == null || o.owner() == null) return false ;
    if (o.owner().getClass() == this.getClass()) return true ;
    return false ;
  }
  
  
  public boolean pointsOkay(Tile from, Tile to) {
    toInstall = installedBetween(from, to) ;
    ///I.say("TO INSTALL IS: "+toInstall+", between "+from+" and "+to) ;
    if (toInstall == null) return false ;
    for (Segment s : toInstall) {
      if (! s.superPointsOkay()) return false ;
    }
    ///I.say("INSTALL OKAY...") ;
    return true ;
  }
  
  
  public void doPlace(Tile from, Tile to) {
    if (toInstall == null) return ;
    for (Segment v : toInstall) v.superPlacing(toInstall) ;
  }
  
  
  public void preview(
    boolean canPlace, Rendering rendering, Tile from, Tile to
  ) {
    if (toInstall == null) return ;
    for (Segment v : toInstall) v.superPreview(canPlace, rendering) ;
  }
}









package stratos.game.building ;
import java.lang.reflect.* ;

import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.terrain.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



public abstract class Segment extends Venue implements TileConstants {
  
  
  /**  Setup and save/load methods-
    */
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
  
  
  
  /**  Configuration refreshment-
    */
  private void refreshFromNear(List <Segment> prior) {
    final Tile o = origin() ;
    if (o == null) return ;
    final World world = o.world ;
    
    if (prior != null) for (Segment s : prior) s.origin().flagWith(this) ;
    
    final boolean near[] = new boolean[8] ;
    int numNear = 0 ;
    for (int i : N_ADJACENT) {
      final Tile n = world.tileAt(o.x + (N_X[i] * 2), o.y + (N_Y[i] * 2)) ;
      if (n == null) continue ;
      boolean isNear = false ;
      if (n.owner() != null && n.owner().getClass() == this.getClass()) {
        isNear = true ;
      }
      if (n.flaggedWith() == this) isNear = true ;
      if (isNear) { numNear++ ; near[i] = true ; }
    }

    if (prior != null) for (Segment s : prior) s.origin().flagWith(null) ;
    
    configFromAdjacent(near, numNear) ;
  }
  
  
  protected abstract void configFromAdjacent(boolean near[], int numNear) ;
  
  
  
  /**  Behaviour implementations/overrides (not generally used/needed)-
    */
  public Behaviour jobFor(Actor actor) { return null ; }
  public Background[] careers() { return null ; }
  public Service[] services() { return null ; }
  
  
  
  /**  Helper methods for placement-
    */
  protected Tile placeCoord(Tile hovered) {
    if (hovered == null) return null ;
    final int
      atX = 2 * (int) (hovered.x / 2f),
      atY = 2 * (int) (hovered.y / 2f) ;
    return hovered.world.tileAt(atX, atY);
  }
  
  
  public void setPosition(int x, int y, World world) {
    final Tile o = placeCoord(world.tileAt(x, y)) ;
    super.setPosition(o.x, o.y, world) ;
  }
  
  
  protected abstract Segment instance(Base base);
  
  
  protected List <Segment> installedBetween(Tile start, Tile end) {
    if (start == null) return null ;
    if (end   == null) end = start ;

    final RoadSearch search = new RoadSearch(start, end, Element.VENUE_OWNS) {
      protected boolean canEnter(Tile t) { return true ; }
    } ;
    search.doSearch() ;
    final Tile origPath[] = search.bestPath(Tile.class) ;
    
    final Batch <Tile> placePath = new Batch <Tile> () ;
    for (Tile t : origPath) {
      final Tile p = placeCoord(t) ;
      if (p.flaggedWith() != null) continue ;
      p.flagWith(placePath) ;
      placePath.add(p) ;
    }
    for (Tile p : placePath) p.flagWith(null) ;
    
    final List <Segment> installed = new List() ;
    for (Tile p : placePath) {
      final Segment v = instance(base());
      if (v == null) I.say("PROBLEM!");
      v.setPosition(p.x, p.y, p.world);
      if (! v.canPlace()) return null;
      installed.add(v);
    }
    
    for (Segment s : installed) {
      s.refreshFromNear(installed) ;
    }
    return installed ;
  }
  
  
  protected void updatePaving(boolean inWorld) {
  }
  
  
  
  /**  Placement interface and rendering tweaks-
    */
  private List <Segment> toInstall = new List() ;
  
  
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
    I.say("Couldn't place normally!") ;
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






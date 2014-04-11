/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.building ;
import stratos.game.common.*;
import stratos.util.*;



//
//  TODO:  Unify with the Placement class?


public abstract class TileSpread extends Search <Tile> {
  
  
  /**  Basic constructor and override methods-
    */
  final Tile batch[] = new Tile[4] ;
  
  public TileSpread(Tile init) {
    super(init, -1) ;
  }
  
  
  protected abstract boolean canAccess(Tile t) ;
  protected abstract boolean canPlaceAt(Tile t) ;
  
  
  
  /**  Search method implementations-
    */
  protected boolean canEnter(Tile spot) {
    return canAccess(spot) ;
  }
  
  protected Tile[] adjacent(Tile spot) {
    return spot.edgeAdjacent(batch) ;
  }
  
  protected boolean endSearch(Tile best) {
    return canPlaceAt(best) ;
  }
  
  protected float cost(Tile prior, Tile spot) {
    return 1 ;
  }
  
  protected float estimate(Tile spot) {
    return 0;// Spacing.distance(spot, init) ;
  }
  
  protected void setEntry(Tile spot, Entry flag) {
    spot.flagWith(flag) ;
  }
  
  protected Entry entryFor(Tile spot) {
    return (Entry) spot.flaggedWith() ;
  }
  
  
  /**  Convenience methods-
    */
  public static Fixture tryPlacement(
    final Tile initTile, final Box2D limit,
    final Fixture parent, final Fixture toPlace
  ) {
    final int owningType = toPlace.owningType() ;
    final TileSpread spread = new TileSpread(initTile) {
      protected boolean canAccess(Tile t) {
        if (limit != null && ! limit.contains(t.x, t.y)) return false ;
        if (parent != null && t.owner() == parent) return true ;
        return t.owningType() < owningType && t.habitat().pathClear ;
      }
      protected boolean canPlaceAt(Tile t) {
        toPlace.setPosition(t.x, t.y, t.world) ;
        if (limit != null && ! toPlace.area().containedBy(limit)) return false ;
        return toPlace.canPlace() ;
      }
    } ;
    spread.doSearch() ;
    if (spread.success()) return toPlace ;
    return null ;
  }
  
  
  public static Fixture[] tryPlacement(
    final Tile initTile, final Box2D limit,
    final Fixture parent, final Fixture... toPlace
  ) {
    final Coord relPos[] = new Coord[toPlace.length] ;
    final Tile root = toPlace[0].origin() ;
    for (int i = toPlace.length ; i-- > 0 ;) {
      final Tile t = toPlace[i].origin() ;
      final Coord c = relPos[i] = new Coord() ;
      c.x = t.x - root.x ;
      c.y = t.y - root.y ;
    }
    final int owningType = toPlace[0].owningType() ;
    
    final TileSpread spread = new TileSpread(initTile) {
      protected boolean canAccess(Tile t) {
        if (limit != null && ! limit.contains(t.x, t.y)) return false ;
        if (parent != null && t.owner() == parent) return true ;
        return t.owningType() < owningType && t.habitat().pathClear ;
      }
      protected boolean canPlaceAt(Tile t) {
        for (int i = toPlace.length ; i-- > 0 ;) {
          final Fixture f = toPlace[i] ;
          final Coord c = relPos[i] ;
          final Tile o = t.world.tileAt(t.x + c.x, t.y + c.y) ;
          if (o == null) return false ;
          f.setPosition(o.x, o.y, o.world) ;
          if (limit != null && ! f.area().containedBy(limit)) return false ;
          if (! f.canPlace()) return false ;
        }
        return true ;
      }
    } ;
    spread.doSearch() ;
    if (spread.success()) return toPlace ;
    return null ;
  }
}













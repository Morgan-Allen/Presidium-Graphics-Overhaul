

package stratos.game.actors ;
import stratos.game.common.*;
import stratos.util.*;


public class Route {
  
  
  final public Tile start, end ;
  final private int hash ;
  
  public Tile path[] ;
  public float cost ;
  
  
  public static void saveRoute(Route r, Session s) throws Exception {
    s.saveTarget(r.start) ;
    s.saveTarget(r.end) ;
    s.saveInt(r.hash) ;
    s.saveTargetArray(r.path) ;
    s.saveFloat(r.cost) ;
  }

  
  private Route(Session s) throws Exception {
    start = (Tile) s.loadTarget() ;
    end = (Tile) s.loadTarget() ;
    hash = s.loadInt() ;
    path = (Tile[]) s.loadTargetArray(Tile.class) ;
    cost = s.loadFloat() ;
  }
  
  
  public static Route loadRoute(Session s) throws Exception {
    return new Route(s) ;
  }
  
  
  //
  //  We have to ensure a consistent ordering here so that the results of
  //  pathing searches between the two points remain stable.
  public Route(Tile a, Tile b) {
    final int s = a.world.size ;
    final int cA = (a.x * s) + a.y, cB = (b.x * s) + b.y ;
    final boolean flip = cA > cB ;
    if (flip) {
      start = b ; end = a ;
      hash = (cA * 13) + (cB % 13) ;
    }
    else {
      start = a ; end = b ;
      hash = (cB * 13) + (cA % 13) ;
    }
  }
  
  
  public boolean equals(Object o) {
    if (! (o instanceof Route)) return false ;
    final Route r = (Route) o ;
    return
      (r.start == start && r.end == end) ;
  }
  
  
  public int hashCode() {
    return hash ;
  }
}




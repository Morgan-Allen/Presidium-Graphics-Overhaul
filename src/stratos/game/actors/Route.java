

package stratos.game.actors;
import stratos.game.common.*;
import stratos.util.*;


//
//  TODO:  Generalise this to arbitrary destinations, including offworld or
//  between sectors?


public class Route {
  
  
  final public Tile start, end;
  final private int hash;
  
  public Tile path[] = null;
  public float cost = 0;
  
  
  public static void saveRoute(Route r, Session s) throws Exception {
    s.saveTarget(r.start);
    s.saveTarget(r.end);
    s.saveInt(r.hash);
    s.saveTargetArray(r.path);
    s.saveFloat(r.cost);
  }

  
  private Route(Session s) throws Exception {
    start = (Tile) s.loadTarget();
    end = (Tile) s.loadTarget();
    hash = s.loadInt();
    path = (Tile[]) s.loadTargetArray(Tile.class);
    cost = s.loadFloat();
  }
  
  
  public static Route loadRoute(Session s) throws Exception {
    return new Route(s);
  }
  
  
  //
  //  We have to ensure a consistent ordering here so that the results of
  //  pathing searches between the two points remain stable.
  public Route(Tile a, Tile b) {
    final int s = a.world.size;
    final int cA = (a.x * s) + a.y, cB = (b.x * s) + b.y;
    final boolean flip = cA > cB;
    if (flip) {
      start = b; end = a;
      hash = (cA * 13) + (cB % 13);
    }
    else {
      start = a; end = b;
      hash = (cB * 13) + (cA % 13);
    }
  }
  
  
  public Tile opposite(Tile t) {
    if (t == start) return end  ;
    if (t == end  ) return start;
    I.complain("TILE GIVEN IS NEITHER START OR END POINT FOR: "+this);
    return null;
  }
  
  
  public boolean routeEquals(Route other) {
    if (
      other       == null ||
      other.path  == null ||
      this.path   == null ||
      other.start != this.start ||
      other.end   != this.end   ||
      other.path.length != this.path.length
    ) return false;
    
    boolean match = true;
    for (Tile t : other.path) t.flagWith(other);
    int numMatched = 0;
    for (Tile t : path) {
      if (t.flaggedWith() != other) {
        match = false;
        break;
      }
      else numMatched++;
    }
    for (Tile t : other.path) t.flagWith(null);
    if (numMatched != other.path.length) match = false;
    return match;
  }
  
  
  public boolean equals(Object o) {
    if (! (o instanceof Route)) return false;
    final Route r = (Route) o;
    return
      (r.start == start && r.end == end);
  }
  
  
  public int hashCode() {
    return hash;
  }
  
  
  public String toString() {
    return "Route between "+start+" and "+end;
  }
}









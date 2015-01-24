


package stratos.game.economic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.util.*;


//  TODO:  You might consider replacing this with something simplier or just
//  hackier.

/**  A specialised search algorithm used especially for road connections.
  */
public class RoadSearch extends Search <Tile> implements TileConstants {
  
  
  final static float
    MAX_DISTANCE_MULT = 4.0f;
  
  final StageTerrain terrain ;
  final Tile destination;
  //final int priority;
  final private Tile edges[] = new Tile[4];
  private int bestDir = -1;
  
  
  public RoadSearch(Tile start, Tile end) {//, int priority) {
    super(start, (Spacing.sumAxisDist(start, end) * 20) + 20);
    this.destination = end;
    this.terrain     = end.world.terrain();
    //this.priority    = priority;
  }
  
  
  private int directionBetween(Tile prior, Tile spot) {
    final int dir;
    if (prior.x == spot.x) dir = prior.y > spot.y ? E : W;
    else                   dir = prior.x > spot.x ? N : S;
    return dir;
  }
  
  
  protected boolean endSearch(Tile t) {
    return t == destination;
  }
  
  
  protected float estimate(Tile spot) {
    return Spacing.sumAxisDist(spot, destination) / 2;
  }
  

  protected boolean stepSearch() {
    if (! super.stepSearch()) return false;
    final Tile best = bestFound(), prior = priorTo(best);
    if (prior != null) bestDir = directionBetween(prior, best);
    return true;
  }
  
  
  protected float cost(Tile prior, Tile spot) {
    final float cost = PavingMap.pavingReserved(spot) ? 0.5f : 1;
    if (bestDir != -1) {
      final int dir = directionBetween(prior, spot);
      if (dir != bestDir) return cost * 2.5f;
    }
    return cost;
  }
  
  
  protected Tile[] adjacent(Tile t) {
    return t.edgeAdjacent(edges);
  }
  
  
  protected boolean canEnter(Tile t) {
    if (t == init || t == destination) return true;
    return ! t.reserved();// t.owningType() < priority;
  }
  
  
  protected void setEntry(Tile spot, Entry flag) {
    spot.flagWith(flag);
  }
  
  
  protected Entry entryFor(Tile spot) {
    return (Entry) spot.flaggedWith();
  }
}







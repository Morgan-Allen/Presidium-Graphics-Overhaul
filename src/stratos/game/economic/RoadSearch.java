


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
  
  final private Tile edges[] = new Tile[4];  //Cached for efficiency.
  
  
  public RoadSearch(Tile start, Tile end) {
    super(start, (Spacing.sumAxisDist(start, end) * 20) + 20);
    this.destination = end;
    this.terrain     = end.world.terrain();
  }
  
  
  protected boolean endSearch(Tile t) {
    return t == destination;
  }
  
  
  protected float estimate(Tile spot) {
    final int div = (
      spot.x == init.x || spot.x == destination.x ||
      spot.y == init.y || spot.y == destination.y
    ) ? 5 : 2;
    return Spacing.sumAxisDist(spot, destination) / div;
  }
  
  
  protected float cost(Tile prior, Tile spot) {
    return 1;
  }
  
  
  protected Tile[] adjacent(Tile t) {
    return t.edgeAdjacent(edges);
  }
  
  
  protected boolean canEnter(Tile t) {
    if (t == init || t == destination) return true;
    return t.canPave();
  }
  
  
  protected void setEntry(Tile spot, Entry flag) {
    spot.flagWith(flag);
  }
  
  
  protected Entry entryFor(Tile spot) {
    return (Entry) spot.flaggedWith();
  }
}







/*
private int directionBetween(Tile prior, Tile spot) {
  final int dir;
  if (prior.x == spot.x) dir = prior.y > spot.y ? E : W;
  else                   dir = prior.x > spot.x ? N : S;
  return dir;
}
//*/
/*
protected boolean stepSearch() {
  if (! super.stepSearch()) return false;
  final Tile best = bestFound(), prior = priorTo(best);
  if (prior != null) bestDir = directionBetween(prior, best);
  return true;
}
//*/
/*
final float cost = 1;// PavingMap.pavingReserved(spot) ? 0.5f : 1;
if (bestDir != -1) {
  final int dir = directionBetween(prior, spot);
  if (dir != bestDir) return cost * 2.5f;
}
return cost;
//*/



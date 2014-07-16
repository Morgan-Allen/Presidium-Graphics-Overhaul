


package stratos.game.building;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.util.*;



/**  A specialised search algorithm used especially for road connections.
  */
public class RoadSearch extends Search <Tile> {
  
  
  final static float
    MAX_DISTANCE_MULT = 4.0f;
  
  final WorldTerrain terrain ;
  final Tile destination;
  final int priority;
  final private Tile edges[] = new Tile[4];
  
  
  public RoadSearch(Tile start, Tile end, int priority) {
    super(start, (Spacing.sumAxisDist(start, end) * 20) + 20);
    this.destination = end;
    this.terrain = end.world.terrain();
    this.priority = priority;
  }
  
  
  protected boolean endSearch(Tile t) {
    return t == destination;
  }
  
  
  protected float estimate(Tile spot) {
    return Spacing.sumAxisDist(spot, destination) / 2;
  }
  
  
  protected float cost(Tile prior, Tile spot) {
    if (terrain.isRoad(spot)) return 0.5f;
    return 1;
  }
  
  
  protected Tile[] adjacent(Tile t) {
    return t.edgeAdjacent(edges);
  }
  
  
  protected boolean canEnter(Tile t) {
    if (t == init || t == destination) return true;
    return t.habitat().pathClear && (
      t.owner() == null ||
      t.owner().owningType() < priority
    );
  }
  
  
  protected void setEntry(Tile spot, Entry flag) {
    spot.flagWith(flag);
  }
  
  
  protected Entry entryFor(Tile spot) {
    return (Entry) spot.flaggedWith();
  }
}







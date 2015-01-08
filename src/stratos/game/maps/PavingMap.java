

package stratos.game.maps;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.util.*;
import static stratos.game.maps.StageTerrain.*;



public class PavingMap {
  
  private static boolean
    warnVerbose   = false,
    searchVerbose = false;
  
  final Stage world;
  final PavingRoutes paving;
  final int size;
  
  final byte roadCounter[][];
  final MipMap flagMap;
  
  
  public PavingMap(Stage world, PavingRoutes paving) {
    this.world  = world ;
    this.paving = paving;
    
    this.size = world.size;
    this.roadCounter = new byte[size][size];
    this.flagMap = new MipMap(size);
  }
  
  
  public void loadState(Session s) throws Exception {
    s.loadByteArray(roadCounter);
    flagMap.loadFrom(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveByteArray(roadCounter);
    flagMap.saveTo(s.output());
  }
  
  
  
  /**  Modifier methods-
    */
  public void flagForPaving(Tile t, boolean is) {
    
    final byte c = (roadCounter[t.x][t.y] += is ? 1 : -1);
    if (c < 0) I.complain("CANNOT HAVE NEGATIVE ROAD COUNTER: "+t);
    
    final boolean flag = needsPaving(t);
    flagMap.set((byte) (flag ? 1 : 0), t.x, t.y);
    
    if (GameSettings.paveFree) {
      setPaveLevel(t, c > 0 ? ROAD_LIGHT : ROAD_NONE, is);
    }
  }
  
  
  public void flagForPaving(Tile tiles[], boolean is) {
    if (tiles == null || tiles.length == 0) return;
    for (Tile t : tiles) if (t != null) flagForPaving(t, is);
  }
  
  
  public boolean refreshPaving(Tile tiles[]) {
    for (Tile t : tiles) if (t.owningType() <= Element.ELEMENT_OWNS) {
      final byte c = roadCounter[t.x][t.y];
      
      final boolean flag = needsPaving(t);
      flagMap.set((byte) (flag ? 1 : 0), t.x, t.y);
      
      if (GameSettings.paveFree) {
        setPaveLevel(t, c > 0 ? ROAD_LIGHT : ROAD_NONE, true);
      }
    }
    return true;
  }
  
  
  protected void updateFlags(Tile t) {
    final boolean flag = needsPaving(t);
    flagMap.set((byte) (flag ? 1 : 0), t.x, t.y);
  }
  

  public static void setPaveLevel(Tile t, byte level, boolean clear) {
    if (level > ROAD_NONE && t.onTop() != null && clear) {
      t.onTop().setAsDestroyed();
    }
    t.world.terrain().setRoadType(t, level);
  }
  
  
  public static boolean isRoad(Tile t) {
    return t.world.terrain().isRoad(t);
  }
  
  
  public int roadCounter(Tile t) {
    return roadCounter[t.x][t.y];
  }
  
  
  public static boolean pavingReserved(Tile t) {
    final Stage world = t.world;
    if (isRoad(t)) return true;
    for (Base b : world.bases()) {
      if (b.paveRoutes.map.roadCounter(t) > 0) return true;
    }
    return false;
  }
  
  
  public boolean needsPaving(Tile t) {
    //  If flagged positive by *your own* base, and unpaved, return true.
    if (roadCounter[t.x][t.y] > 0 && ! isRoad(t)) return true;
    //  If paved and flagged negative by *all* bases, return true.
    if (! isRoad(t) && pavingReserved(t)) return true;
    //  Otherwise return false.
    return false;
  }
  

  /**  Search method for getting the next tile that needs paving...
    */
  
  private boolean refreshed = false;
  
  public void refreshFlags() {
    if (refreshed) return;
    refreshed = true;
    /*
    MipMap.printVals(flagMap);
    flagMap.clear();
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      final boolean flag = needsPaving(world.tileAt(c.x, c.y));
      flagMap.set((byte) (flag ? 1 : 0), c.x, c.y);
    }
    MipMap.printVals(flagMap);
    //*/
  }
  
  
  //  TODO:  To boost efficiency further, you might consider caching results
  //  for this periodically?
  public Tile nextTileToPave(final Target client, final StageSection limit) {
    this.refreshFlags();  //  Used strictly for debugging.
    final boolean report = searchVerbose && I.talkAbout == client;
    
    final Box2D limitBox = limit == null ? null : limit.area;
    final Tile  o = world.tileAt(client);
    final Coord c = flagMap.nearest(o.x, o.y, -1, limitBox);
    final Tile  t = c == null ? null : world.tileAt(c.x, c.y);
    
    //  TODO:  There's an occasional problem here, but it's with the flagging
    //  system rather than the mipmap-search algorithm itself.  Lock that down.
    final boolean warn = report && warnVerbose;
    if (t != null && ! needsPaving(t)) {
      if (warn) {
        I.say("\nWARNING: GOT TILE WHICH DOES NOT NEED PAVING: "+t);
        I.say("  Flagged value? "+flagMap.getTotalAt(t.x, t.y, 0));
      }
      updateFlags(t);
      return null;
    }
    if (report) I.say("Next tile to pave: "+t);
    return t;
  }
}









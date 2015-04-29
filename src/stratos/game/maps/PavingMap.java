

package stratos.game.maps;
import stratos.game.base.BaseTransport;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.user.BaseUI;
import stratos.util.*;
import static stratos.game.maps.StageTerrain.*;



public class PavingMap {
  
  private static boolean
    warnVerbose   = false,
    searchVerbose = false;
  
  final Stage world;
  final BaseTransport paving;
  final int size;
  
  final byte roadCounter[][];
  final MipMap flagMap;
  
  
  public PavingMap(Stage world, BaseTransport paving) {
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
    refreshPaving(t);
  }
  
  
  public void flagForPaving(Tile tiles[], boolean is) {
    if (tiles == null || tiles.length == 0) return;
    for (Tile t : tiles) if (t != null) flagForPaving(t, is);
  }
  
  
  public boolean isFlagged(Tile t) {
    return flagMap.getBaseValue(t.x, t.y) != 0;
  }
  
  
  public boolean refreshPaving(Tile tiles[]) {
    for (Tile t : tiles) if (! t.reserved()) refreshPaving(t);
    return true;
  }
  
  
  public void refreshPaving(Tile t) {
    final boolean flag = needsPaving(t);
    if (flag && GameSettings.paveFree) {
      final byte c = roadCounter[t.x][t.y];
      setPaveLevel(t, c > 0 ? ROAD_LIGHT : ROAD_NONE, true);
    }
    flagMap.set((byte) (flag ? 1 : 0), t.x, t.y);
  }
  
  
  public static void setPaveLevel(Tile t, byte level, boolean clear) {
    if (level > ROAD_NONE && ! t.canPave()) return;
    if (clear && level > ROAD_NONE && t.onTop() != null) {
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
  
  
  public boolean needsPaving(Tile t) {
    final byte c = roadCounter[t.x][t.y];
    final boolean road = isRoad(t);
    if (c > 0 && ! road) return true;
    if (road && ! pavingReserved(t, false)) return true;
    return false;
  }
  
  
  public static boolean pavingReserved(Tile t, boolean alreadyPaved) {
    final Stage world = t.world;
    if (alreadyPaved && isRoad(t)) return true;
    for (Base b : world.bases()) {
      if (b.transport.map.roadCounter(t) > 0) return true;
    }
    return false;
  }
  
  
  
  /**  Search method for getting the next tile that needs paving...
    */
  private boolean refreshed = false;
  
  
  public static void cleanupAllRoads(Stage world) {
    for (Tile t : world.tilesIn(world.area(), false)) {
      if (PavingMap.pavingReserved(t, false)) {
        PavingMap.setPaveLevel(t, StageTerrain.ROAD_LIGHT, true );
      }
      else {
        PavingMap.setPaveLevel(t, StageTerrain.ROAD_NONE , false);
      }
    }
  }
  
  
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
  public Tile nextTileToPave(final Target client, final StageRegion limit) {
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
      refreshPaving(t);
      return null;
    }
    if (report) I.say("Next tile to pave: "+t);
    return t;
  }
}













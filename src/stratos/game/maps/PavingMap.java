

package stratos.game.maps;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.util.*;
import static stratos.game.maps.WorldTerrain.*;



public class PavingMap {
  
  private static boolean verbose = false, searchVerbose = false;
  
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
  
  
  public boolean refreshTiles(Tile tiles[]) {
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
  
  
  public int roadCounter(Tile t) {
    return roadCounter[t.x][t.y];
  }
  
  
  public boolean needsPaving(Tile t) {
    return world.terrain().isRoad(t) != (roadCounter(t) > 0);
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
  
  
  public static boolean pavingReserved(Tile t) {
    final Stage world = t.world;
    if (world.terrain().isRoad(t)) return true;
    for (Base b : world.bases()) {
      if (b.paveRoutes.map.roadCounter(t) > 0) return true;
    }
    return false;
  }
  

  /**  Search method for getting the next tile that needs paving.
    * 
    */
  //  TODO:  Try to improve efficiency here.  In the worst-case scenario
  //  you're looking at checking half the tiles in the world...
  //  TODO:  Limit to the maximum distance that auto-paving can extend, then
  //  just check around buildings?
  public Tile nextTileToPave(final Target client, final Class paveClass) {
    this.refreshFlags();
    
    final boolean report = searchVerbose && I.talkAbout == client;
    
    final Vars.Ref <Tile> result = new Vars.Ref <Tile> ();
    float minDist = Float.POSITIVE_INFINITY;
    final int depth = MipMap.sizeToDepth(world.sections.resolution);
    final Tile o = world.tileAt(client);
    
    
    for (WorldSection s : world.sections.sectionsUnder(world.area())) {
      final float distS = s.area.distance(o.x, o.y) + 1;
      if (distS >= minDist) continue;
      
      final float pop = flagMap.getAvgAt(s.x, s.y, depth);
      if (pop <= 0) continue;
      if (report) {
        I.say("Trying section at: "+s.x+"|"+s.y);
        I.say("  Population: "+pop);
      }
      
      for (Tile t : world.tilesIn(s.area, false)) {
        if (! needsPaving(t)) continue;
        final float distT = Spacing.distance(t, client) - 1;
        if (distT > minDist) continue;
        
        if (report) I.say("    Trying: "+t.x+"|"+t.y);
        result.value = t;
        minDist = distT;
      }
    }
    
    if (report) I.say("Next tile to pave: "+result.value);
    
    return result.value;
  }
}









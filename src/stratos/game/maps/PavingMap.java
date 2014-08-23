

package stratos.game.maps;
import stratos.game.building.Paving;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.util.*;



public class PavingMap {
  
  
  private static boolean verbose = false, searchVerbose = false;
  
  final World world;
  final Paving paving;
  final int size;
  
  final byte roadCounter[][];
  final MipMap flagMap;
  
  
  public PavingMap(World world, Paving paving) {
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
  }
  
  
  public void flagForPaving(Tile tiles[], boolean is) {
    if (tiles == null || tiles.length == 0) return;
    for (Tile t : tiles) if (t != null) flagForPaving(t, is);
  }
  
  
  public int roadCounter(Tile t) {
    return roadCounter[t.x][t.y];
  }
  
  
  public boolean needsPaving(Tile t) {
    return world.terrain().isRoad(t) != (roadCounter(t) > 0);
  }
  
  
  
  public Tile nextTileToPave(final Target client, final Class paveClass) {
    this.refreshFlags();
    
    final boolean report = searchVerbose && I.talkAbout == client;
    
    final Vars.Ref <Tile> result = new Vars.Ref <Tile> ();
    float minDist = Float.POSITIVE_INFINITY;
    final int depth = MipMap.sizeToDepth(world.sections.resolution);
    final Tile o = world.tileAt(client);
    
    //  TODO:  Try to improve efficiency here.  In the worst-case scenario
    //  you're looking at checking half the tiles in the world...
    
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
  

  public static void setPaveLevel(Tile t, byte level) {
    t.world.terrain().setRoadType(t, level);
  }
  
  
  protected void updateFlags(Tile t) {
    final boolean flag = needsPaving(t);
    flagMap.set((byte) (flag ? 1 : 0), t.x, t.y);
  }
}











package stratos.game.maps;
import stratos.game.building.Paving;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.util.*;
import stratos.game.common.WorldSections.Section;



public class PavingMap {
  
  
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
  
  
  
  
  
  
  public void maskAsPaved(Tile t, boolean is) {
    final byte c = (roadCounter[t.x][t.y] += is ? 1 : -1);
    if (c < 0) I.complain("CANNOT HAVE NEGATIVE ROAD COUNTER: "+t);
    final boolean flag = world.terrain().isRoad(t) != is;
    flagMap.set((byte) (flag ? 1 : 0), t.x, t.y);
  }
  
  
  public void maskAsPaved(Tile tiles[], boolean is) {
    if (tiles == null || tiles.length == 0) return;
    for (Tile t : tiles) if (t != null) maskAsPaved(t, is);
  }
  
  
  public int roadCounter(Tile t) {
    return roadCounter[t.x][t.y];
  }
  
  
  public boolean needsPaving(Tile t) {
    return world.terrain().isRoad(t) != (roadCounter(t) > 0);
  }
  
  
  
  public Tile nextTileToPave(final Target client, final Class paveClass) {
    final Vars.Ref <Tile> result = new Vars.Ref <Tile> ();
    final Vec3D o = client.position(null);
    
    //  TODO:  Try to improve/guarantee efficiency here.  In the worst case
    //         scenario, you're looking at 256 iterations or so.
    
    world.sections.applyDescent(new WorldSections.Descent() {
      
      float minDist = Float.POSITIVE_INFINITY;
      
      public boolean descendTo(Section s) {
        final float dist = s.bounds.distance(o.x, o.y, 0);
        if (dist > minDist) return false;
        
        if (s.size == world.sections.resolution) {
          //  TODO:  Also confine search to areas accessible from the actor's
          //         pathing position!  ...And use that to estimate proximity.
          if (world.activities.includes(s, paveClass)) return false;
        }
        
        if (flagMap.getAvgAt(s.x, s.y, s.depth) > 0) {
          if (s.depth == 0) {
            final Tile tile = world.tileAt(s.x, s.y);
            result.value = tile;
            minDist = dist;
          }
          return true;
        }
        return false;
      }
      
      public void afterChildren(Section s) {}
    });
    
    return result.value;
  }
  
  
  public void setPaveLevel(Tile t, byte level) {
    world.terrain().setRoadType(t, level);
    final boolean flag = (level > 0) != (roadCounter(t) > 0);
    flagMap.set((byte) (flag ? 1 : 0), t.x, t.y);
  }
}






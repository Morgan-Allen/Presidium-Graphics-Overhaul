


package stratos.game.tactical ;
import stratos.game.common.*;
import stratos.game.planet.*;
import stratos.graphics.common.*;
import stratos.graphics.terrain.*;
import stratos.util.*;


//
//  TODO:  Now implement gradual decay of fog-values over time.

//  Note:  Any previously-explored tile will have a minimum fog value of
//  0.5, or 0.3, or something.  That way, you still get the 'fadeout' effect
//  at the edge of an actor's vision.

public class IntelMap {
  
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  final Base base ;
  private World world = null ;
  
  float fogVals[][] ;
  MipMap fogMap ;
  
  private FogOverlay fogOver ;
  //private float lastFogTime = -1 ;
  
  
  
  public IntelMap(Base base) {
    this.base = base ;
  }
  
  
  public void initFog(World world) {
    this.world = world ;
    final int size = world.size ;
    fogVals = new float[size][size] ;
    fogMap = new MipMap(size) ;
    fogOver = new FogOverlay(size) ;
  }
  
  
  public void loadState(Session s) throws Exception {
    initFog(world = s.world()) ;
    for (Coord c : Visit.grid(0,  0, world.size, world.size, 1)) {
      fogVals[c.x][c.y] = s.loadFloat() ;
    }
    fogMap.loadFrom(s.input()) ;
    fogOver.updateVals(-1, fogVals) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    final int size = world.size ;
    for (Coord c : Visit.grid(0,  0, size, size, 1)) {
      s.saveFloat(fogVals[c.x][c.y]) ;
    }
    fogMap.saveTo(s.output()) ;
  }
  
  
  public MipMap fogMap() {
    return fogMap ;
  }
  
  
  public World world() {
    return world ;
  }
  
  
  public FogOverlay fogOver() {
    if (GameSettings.fogFree) return null;
    return fogOver ;
  }
  
  
  
  /**  Visual refreshment-
    */
  public void updateAndRender(float fogTime, Rendering rendering) {
    fogOver.updateVals(fogTime, fogVals);
    fogOver.registerFor(rendering);
  }
  
  
  public float displayFog(Tile t, Object client) {
    if (GameSettings.fogFree) return 1;
    return fogOver.sampleAt(t.x, t.y, client);
  }
  
  
  
  /**  Queries and modifications-
    */
  public void updateFogValues() {
  }
  
  
  public float fogAt(Tile t) {
    if (GameSettings.fogFree) return 1 ;
    return fogVals[t.x][t.y];
  }
  
  
  public int liftFogAround(Target t, float radius) {
    final Vec3D p = t.position(null) ;
    return liftFogAround(p.x, p.y, radius) ;
  }
  
  
  public int liftFogAround(float x, float y, float radius) {
    //
    //  We record and return the number of new tiles seen-
    final Box2D area = new Box2D().set(
      x - radius, y - radius,
      radius * 2, radius * 2
    ) ;
    float tilesSeen = 0 ;
    //
    //  Iterate over any tiles within a certain distance of the target point-
    for (Tile t : world.tilesIn(area, true)) {
      final float xd = t.x - x, yd = t.y - y ;
      final float distance = (float) Math.sqrt((xd * xd) + (yd * yd)) ;
      if (distance > radius) continue ;
      //
      //  Calculate the minimum fog value, based on target proximity-
      final float oldVal = fogVals[t.x][t.y] ;
      final float lift = Visit.clamp((1 - (distance / radius)) * 1.5f, 0, 1) ;
      final float newVal = Math.max(lift, oldVal) ;
      fogVals[t.x][t.y] = newVal ;
      //
      //  If there's been a change in fog value, update the reference and
      //  rendering data-
      if (oldVal != newVal) {
        if (newVal == 1) fogMap.set((byte) 1, t.x, t.y) ;
        //stuffDisplayVal(newVal, t.x, t.y) ;
      }
      tilesSeen += lift - oldVal ;
    }
    return (int) tilesSeen ;
  }
}


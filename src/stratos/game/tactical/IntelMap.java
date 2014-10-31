


package stratos.game.tactical;
import org.apache.commons.math3.util.FastMath;

import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.graphics.terrain.*;
import stratos.util.*;



public class IntelMap {
  
  /**  Field definitions, constructors and save/load methods-
    */
  final static float
    MIN_FOG        = 0,
    MAX_FOG        = 1.5f,
    FOG_DECAY_TIME = Stage.STANDARD_DAY_LENGTH,
    FOG_SEEN_MIN   = 0.5f;
  
  final Base base;
  
  private Stage      world;
  private float      fogVals[][];
  private MipMap     fogMap;
  private FogOverlay fogOver;
  
  
  
  public IntelMap(Base base) {
    this.base = base;
  }
  
  
  public void initFog(Stage world) {
    this.world = world;
    final int size = world.size;
    fogVals = new float[size][size];
    fogMap  = new MipMap(size);
    fogOver = new FogOverlay(size);
  }
  
  
  public void loadState(Session s) throws Exception {
    for (Coord c : Visit.grid(0,  0, world.size, world.size, 1)) {
      fogVals[c.x][c.y] = s.loadFloat();
    }
    fogMap.loadFrom(s.input());
    fogOver.updateVals(-1, fogVals);
  }
  
  
  public void saveState(Session s) throws Exception {
    for (Coord c : Visit.grid(0,  0, world.size, world.size, 1)) {
      s.saveFloat(fogVals[c.x][c.y]);
    }
    fogMap.saveTo(s.output());
  }
  
  
  public MipMap fogMap() {
    return fogMap;
  }
  
  
  public Stage world() {
    return world;
  }
  
  
  public FogOverlay fogOver() {
    if (GameSettings.fogFree || base.primal) return null;
    return fogOver;
  }
  
  
  
  /**  Visual refreshment-
    */
  public void updateAndRender(float fogTime, Rendering rendering) {
    if (GameSettings.fogFree || base.primal) return;
    fogOver.updateVals(fogTime, fogVals);
    fogOver.registerFor(rendering);
  }
  
  
  public float displayFog(Tile t, Object client) {
    if (GameSettings.fogFree || base.primal) return 1;
    return Visit.clamp(fogOver.sampleAt(t.x, t.y, client), 0, 1);
  }
  
  
  public float displayFog(float x, float y, Object client) {
    if (GameSettings.fogFree || base.primal) return 1;
    return Visit.clamp(fogOver.sampleAt(x, y, client), 0, 1);
  }
  
  
  
  /**  Queries and modifications-
    */
  public void updateFogValues() {
    for (Coord c : Visit.grid(0,  0, world.size, world.size, 1)) {
      float val = fogVals[c.x][c.y];
      final boolean seen = val >= FOG_SEEN_MIN;
      
      val -= 1f / FOG_DECAY_TIME;
      val = Visit.clamp(val, seen ? FOG_SEEN_MIN : MIN_FOG, MAX_FOG);
      
      fogVals[c.x][c.y] = val;
      if (val < FOG_SEEN_MIN) fogMap.set(0, c.x, c.y);
    }
  }
  
  
  public float fogAt(Tile t) {
    if (GameSettings.fogFree || base.primal) return 1;
    return fogVals[t.x][t.y];
  }
  
  
  public float fogAt(Target t) {
    if (GameSettings.fogFree || base.primal) return 1;
    return fogAt(world.tileAt(t));
  }
  
  
  public int liftFogAround(Target t, float radius) {
    if (GameSettings.fogFree || base.primal) return (int) radius;
    final Vec3D p = t.position(null);
    return liftFogAround(p.x, p.y, radius);
  }
  
  
  public int liftFogAround(float x, float y, float radius) {
    if (GameSettings.fogFree || base.primal) return (int) radius;
    //
    //  We record and return the number of new tiles seen-
    final Box2D area = new Box2D().set(
      x - radius, y - radius,
      radius * 2, radius * 2
    );
    float tilesSeen = 0;
    //
    //  Iterate over any tiles within a certain distance of the target point-
    for (Tile t : world.tilesIn(area, true)) {
      final float xd = t.x - x, yd = t.y - y;
      final float distance = (float) Math.sqrt((xd * xd) + (yd * yd));
      if (distance > radius) continue;
      //
      //  Calculate the minimum fog value, based on target proximity-
      final float oldVal = fogVals[t.x][t.y];
      final float lift = (1 - (distance / radius)) * MAX_FOG;
      final float newVal = FastMath.max(lift, oldVal);
      fogVals[t.x][t.y] = newVal;
      //
      //  If there's been a change in fog value, update the reference and
      //  rendering data-
      if (oldVal != newVal && newVal >= FOG_SEEN_MIN) fogMap.set(1, t.x, t.y);
      tilesSeen += lift - oldVal;
    }
    return (int) tilesSeen;
  }
}


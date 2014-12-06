


package stratos.game.common;
import stratos.util.*;



//  TODO:  Make this into a SampleMap class instead?  Or merge with MipMap?
//  TODO:  Give maps a name, to make debugging a tad easier.



public class FadingMap implements TileConstants {
  
  
  final public Stage world;
  final public int resolution, gridSize, patchSize, interval;
  private float averages[][], overall, lastTime = -1;
  

  public FadingMap(Stage world, int resolution, int interval) {
    this.world = world;
    this.resolution = resolution;
    this.gridSize = world.size / resolution;
    this.patchSize = resolution * resolution;
    this.interval = interval > 0 ? interval : Stage.STANDARD_DAY_LENGTH;
    this.averages = new float[gridSize][gridSize];
  }
  
  
  public void loadState(Session s) throws Exception {
    for (Coord c : Visit.grid(0, 0, gridSize, gridSize, 1)) {
      averages[c.x][c.y] = s.loadFloat();
    }
    overall = s.loadFloat();
    lastTime = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    for (Coord c : Visit.grid(0, 0, gridSize, gridSize, 1)) {
      s.saveFloat(averages[c.x][c.y]);
    }
    s.saveFloat(overall);
    s.saveFloat(lastTime);
  }
  
  
  public void accumulate(float value, float duration, int x, int y) {
    x /= resolution;
    y /= resolution;
    final float inc = value * duration / interval;
    averages[x][y] += inc;
    overall += inc;
  }
  
  
  public void update() {
    final float time = world.currentTime();
    if (lastTime == -1) { lastTime = time; return; }
    final float gap = time - lastTime;
    if (gap < 1) return;
    else lastTime = time;
    
    final float fadeValue = 1 - (gap / interval);
    for (Coord c : Visit.grid(0, 0, gridSize, gridSize, 1)) {
      averages[c.x][c.y] *= fadeValue;
    }
    overall *= fadeValue;
  }
  
  
  
  /**  Sampling methods-
    */
  private Vec3D sampled = new Vec3D();
  
  
  public float sampleAt(float x, float y) {
    return Nums.sampleMap(world.size, averages, x, y) / patchSize;
  }
  
  
  public float sampleAt(Target t) {
    final Vec3D v = t.position(sampled);
    return sampleAt(v.x, v.y);
  }
  
  
  public float patchValue(int x, int y) {
    return averages[x / resolution][y / resolution];
  }
  
  
  public float overallValue() {
    return overall / (world.size * world.size);
  }
  
  
  public void presentVals(String label, float mult, boolean shortTerm) {
    final float squareA = patchSize * mult;
    I.present(averages, label, 256, 256, squareA, -squareA);
  }
}





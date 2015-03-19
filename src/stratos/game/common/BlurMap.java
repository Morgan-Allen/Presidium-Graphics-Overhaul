/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.common;
import stratos.game.economic.Economy;
import stratos.game.maps.*;
import stratos.util.*;



public class BlurMap {
  
  
  /**  Data fields, construction and save/load methods-
    */
  final protected int trueSize, patchSize, gridSize;
  final float patchValues[][];
  private float globalValue;
  
  final protected Object parent, key;
  
  
  protected BlurMap(int size, int patchSize, Object parent, Object key) {
    this.trueSize    = size;
    this.patchSize   = patchSize;
    this.gridSize    = trueSize / patchSize;
    this.patchValues = new float[gridSize][gridSize];
    
    this.parent = parent;
    this.key    = key;
  }
  
  
  protected void loadState(Session s) throws Exception {
    for (Coord c : Visit.grid(0, 0, gridSize, gridSize, 1)) {
      patchValues[c.x][c.y] = s.loadFloat();
    }
    globalValue = s.loadFloat();
  }
  
  
  protected void saveState(Session s) throws Exception {
    for (Coord c : Visit.grid(0, 0, gridSize, gridSize, 1)) {
      s.saveFloat(patchValues[c.x][c.y]);
    }
    s.saveFloat(globalValue);
  }
  
  
  
  /**  Updates and modifications-
    */
  protected void updateAllValues(float decay) {
    final boolean report = false;
    if (report) I.say("\nReporting value for "+key);
    
    for (Coord c : Visit.grid(0, 0, gridSize, gridSize, 1)) {
      final float value = patchValues[c.x][c.y], DV = 0 - value * decay;
      impingeValue(DV, c.x * gridSize, c.y * gridSize);
      if (report && value > 0) I.say("  "+value+" at "+c+", decay "+DV);
    }
    globalValue -= globalValue * decay;
  }
  
  
  protected void impingeValue(float value, int x, int y) {
    x /= gridSize;
    y /= gridSize;
    value = patchValues[x][y] += value;
    globalValue += value;
  }
  
  
  
  /**  External query functions-
    */
  protected int globalValue() {
    return (int) globalValue;
  }
  
  
  protected float sampleValue(float x, float y) {
    return Nums.sampleMap(trueSize, patchValues, x, y);
  }
  
  
  protected float patchValue(float x, float y) {
    return patchValues[(int) (x / patchSize)][(int) (y / patchSize)];
  }
}










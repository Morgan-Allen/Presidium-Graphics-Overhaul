/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.maps;
import stratos.game.common.Session;
import stratos.util.*;



public class BlurMap {
  
  
  /**  Data fields, construction and save/load methods-
    */
  final protected int trueSize, patchSize, gridSize;
  final float patchValues[][];
  private float globalValue;
  
  final public Object parent, key;
  
  
  public BlurMap(int size, int patchSize, Object parent, Object key) {
    this.trueSize    = size;
    this.patchSize   = patchSize;
    this.gridSize    = trueSize / patchSize;
    this.patchValues = new float[gridSize][gridSize];
    
    this.parent = parent;
    this.key    = key;
  }
  
  
  public void loadState(Session s) throws Exception {
    for (Coord c : Visit.grid(0, 0, gridSize, gridSize, 1)) {
      patchValues[c.x][c.y] = s.loadFloat();
    }
    globalValue = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    for (Coord c : Visit.grid(0, 0, gridSize, gridSize, 1)) {
      s.saveFloat(patchValues[c.x][c.y]);
    }
    s.saveFloat(globalValue);
  }
  
  
  
  /**  Updates and modifications-
    */
  public void updateAllValues(float decay) {
    final boolean report = false;
    if (report) I.say("\nReporting value for "+key);
    
    for (Coord c : Visit.grid(0, 0, gridSize, gridSize, 1)) {
      final float value = patchValues[c.x][c.y], DV = 0 - value * decay;
      if (value > 0 && report) I.say("  "+value+" at "+c+", decay "+DV);
      patchValues[c.x][c.y] += DV;
    }
    
    final float DV = 0 - globalValue * decay;
    if (report) I.say("  Global value: "+globalValue+", decay: "+DV);
    globalValue += DV;
  }
  
  
  public void impingeValue(float value, int x, int y) {
    x /= patchSize;
    y /= patchSize;
    globalValue       += value;
    patchValues[x][y] += value;
  }
  
  
  
  /**  External query functions-
    */
  public int globalValue() {
    return Nums.round(globalValue, 1, true);
  }
  
  
  public float sampleValue(float x, float y) {
    return Nums.sampleMap(trueSize, patchValues, x, y);
  }
  
  
  public float patchValue(float x, float y) {
    return patchValues[(int) (x / patchSize)][(int) (y / patchSize)];
  }
}










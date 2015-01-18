/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.common;
import stratos.game.maps.*;
import stratos.util.*;



public class BlurMap {
  
  
  /**  Data fields, construction and save/load methods-
    */
  final protected int trueSize, patchSize, gridSize;
  final float patchValues[][], blurValues[][];
  final MipMap mipValues;
  
  final protected Object parent, key;
  
  
  protected BlurMap(int size, int patchSize, Object parent, Object key) {
    this.trueSize    = size;
    this.patchSize   = patchSize;
    this.gridSize    = trueSize / patchSize;
    this.patchValues = new float[gridSize][gridSize];
    this.blurValues  = new float[gridSize][gridSize];
    this.mipValues   = new MipMap(gridSize);
    
    this.parent = parent;
    this.key    = key;
  }
  
  
  protected void loadState(Session s) throws Exception {
    for (Coord c : Visit.grid(0, 0, gridSize, gridSize, 1)) {
      patchValues[c.x][c.y] = s.loadFloat();
      blurValues [c.x][c.y] = s.loadFloat();
    }
    mipValues.loadFrom(s.input());
  }
  
  
  protected void saveState(Session s) throws Exception {
    for (Coord c : Visit.grid(0, 0, gridSize, gridSize, 1)) {
      s.saveFloat(patchValues[c.x][c.y]);
      s.saveFloat(blurValues [c.x][c.y]);
    }
    mipValues.saveTo(s.output());
  }
  
  
  
  /**  Updates and modifications-
    */
  protected void updateAllValues(float decay) {
    for (Coord c : Visit.grid(0, 0, gridSize, gridSize, 1)) {
      impingeValue(0 - patchValues[c.x][c.y] * decay, c.x, c.y);
    }
    for (Coord c : Visit.grid(0, 0, gridSize, gridSize, 1)) {
      float blur = patchValues[c.x][c.y];
      blur += mipValues.blendValAt(c.x, c.y, 0.5f) / 2;
      blurValues[c.x][c.y] = blur / 1.5f;
    }
  }
  
  
  protected void impingeValue(float value, int x, int y) {
    x /= gridSize;
    y /= gridSize;
    value = patchValues[x][y] += value;
    mipValues.set((int) Nums.clamp(Nums.ceil(value), 0, 100), x, y);
  }
  
  
  
  /**  External query functions-
    */
  protected int globalValue() {
    return mipValues.getRootValue();
  }
  
  
  protected float sampleValue(float x, float y) {
    return Nums.sampleMap(trueSize, blurValues, x, y);
  }
  
  
  protected float patchValue(float x, float y) {
    return blurValues[(int) (x / patchSize)][(int) (y / patchSize)];
  }
}





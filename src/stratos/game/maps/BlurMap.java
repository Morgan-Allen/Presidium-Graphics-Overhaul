/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.maps;
import stratos.game.common.*;
import stratos.util.*;



public class BlurMap {
  
  
  /**  Data fields, construction and save/load methods-
    */
  private boolean verbose = false;
  private Object verboseKey = null;
  
  final protected int trueSize, patchSize, gridSize, timePeriod;
  final float patchValues[][];
  private float globalValue = 0;
  private float samplerSums = 0;
  
  final public Object parent, key;
  
  
  public BlurMap(
    int size, int patchSize, int timePeriod, Object parent, Object key
  ) {
    this.trueSize    = size;
    this.patchSize   = patchSize;
    this.gridSize    = trueSize / patchSize;
    this.timePeriod  = timePeriod;
    this.patchValues = new float[gridSize][gridSize];
    
    this.parent = parent;
    this.key    = key;
  }
  
  
  public void loadState(Session s) throws Exception {
    for (Coord c : Visit.grid(0, 0, gridSize, gridSize, 1)) {
      patchValues[c.x][c.y] = s.loadFloat();
    }
    globalValue = s.loadFloat();
    samplerSums = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    for (Coord c : Visit.grid(0, 0, gridSize, gridSize, 1)) {
      s.saveFloat(patchValues[c.x][c.y]);
    }
    s.saveFloat(globalValue);
    s.saveFloat(samplerSums);
  }
  
  
  
  /**  Updates and modifications-
    */
  public void updateAllValues(int period) {
    final boolean report = verbose && key == verboseKey;
    final float decay = period * 1f / timePeriod;
    if (report) I.say("\nReporting blur-values for "+key);
    
    for (Coord c : Visit.grid(0, 0, gridSize, gridSize, 1)) {
      final float value = patchValues[c.x][c.y], DV = 0 - value * decay;
      if (value > 0 && report) I.say("  "+value+" at "+c+", decay "+DV);
      patchValues[c.x][c.y] += DV;
    }
    
    final float DV = 0 - globalValue * decay;
    if (report) I.say("  Global value: "+globalValue+", decay: "+DV);
    globalValue += DV;
    
    final float DS = 0 - samplerSums * decay;
    if (report) I.say("  Sampler sums: "+samplerSums+", decay: "+DS);
    samplerSums += DS;
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
  
  
  public float sampleAsFraction(float x, float y, int period) {
    final float sample = gridSize > 1 ?
      Nums.sampleMap(trueSize, patchValues, x, y) :
      patchValues[0][0];
    if (period <= 0 || sample <= 0) return sample;
    samplerSums += period * sample / timePeriod;
    samplerSums = Nums.max(samplerSums, sample);
    return globalValue * sample / samplerSums;
  }
  
  
  public float sumSampling() {
    return samplerSums;
  }
  
  
  public float patchValue(float x, float y) {
    return patchValues[(int) (x / patchSize)][(int) (y / patchSize)];
  }
}










/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.actors;
import stratos.game.common.*;
import stratos.util.*;



public class Trait extends Index.Entry implements Qualities, Session.Saveable {
  
  
  private static boolean verboseInit = false;
  
  final public static Index <Trait> TRAIT_INDEX = new Index <Trait> ();
  
  final String name;
  final String descriptors[];
  
  final int type;
  final int minVal, maxVal;
  final int descValues[];
  
  private Trait correlates[], opposite;
  private float weightings[];
  
  
  
  protected Trait(String name, int type, String... descriptors) {
    super(TRAIT_INDEX, name);
    //this.keyID = keyID;
    //this.indexEntry = TRAIT_INDEX.addEntry(this, keyID);
    
    this.name = name;
    this.type = type;
    this.descriptors = descriptors;
    this.descValues = new int[descriptors.length];
    
    if (verboseInit) {
      I.say("\n  Initialising new trait: "+name);
    }
    
    int zeroIndex = 0, min = -1, max = 1, val;
    for (String s : descriptors) { if (s == null) break; else zeroIndex++; }
    for (int i = descriptors.length; i-- > 0;) {
      val = descValues[i] = zeroIndex - i;
      
      final String desc = descriptors[i];
      if (verboseInit) {
        if (desc != null) I.say("  Value for "+desc+" is "+val);
        else I.say("  Empty value: "+val);
      }
      
      if (val > max) max = val;
      if (val < min) min = val;
    }
    this.minVal = min;
    this.maxVal = max;
    
    if (verboseInit) I.say("  Min/max vals: "+minVal+"/"+maxVal);
  }

  
  public static Trait loadConstant(Session s) throws Exception {
    return TRAIT_INDEX.loadFromEntry(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    TRAIT_INDEX.saveEntry(this, s.output());
  }
  
  
  
  /**  Mechanical effects-
    */
  public void affect(Actor a) {
  }
  
  
  
  /**  Correlations-
    */
  protected void assignCorrelates(Trait t[], float w[]) {
    this.correlates = t;
    this.weightings = w;
    
    for (int n = t.length; n-- > 0;) {
      if (w[n] == -1) { this.opposite = t[n]; break; }
    }
  }
  
  
  protected float correlation(Trait other) {
    final int index = Visit.indexOf(other, correlates);
    return (index == -1) ? 0 : weightings[index];
  }
  
  
  public Trait opposite() { return opposite; }
  public Trait[] correlates() { return correlates; }
  public float[] correlateWeights() { return weightings; }
  
  
  
  /**  Interface, feedback and debug methods-
    */
  public static String descriptionFor(Trait trait, float level) {
    if (trait.opposite != null && level < 0) {
      return descriptionFor(trait.opposite, 0 - level);
    }
    
    if (trait.descriptors.length == 1) {
      if (level == 0) return null;
      return (level > 0 ? "" : "Not ") + trait.descriptors[0];
    }
    
    String bestDesc = null;
    float minDiff = Float.POSITIVE_INFINITY;
    
    int i = 0; for (String s : trait.descriptors) {
      float value = trait.descValues[i];
      if (value > 0) value /= trait.maxVal;
      if (value < 0) value /= 0 - trait.minVal;
      
      final float diff = Math.abs(level - value);
      if (diff < minDiff) { minDiff = diff; bestDesc = s; }
      i++;
    }
    return bestDesc;
  }
  
  
  public String toString() {
    return name;
  }
}





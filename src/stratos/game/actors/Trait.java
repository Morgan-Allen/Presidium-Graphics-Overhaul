/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors;
import stratos.game.common.*;
import stratos.user.Selectable;
import stratos.util.*;



public class Trait extends Constant implements Qualities {
  
  
  private static boolean verboseInit = false;
  
  final public static Index <Trait> TRAIT_INDEX = new Index <Trait> ();
  
  final public String description;
  final String labels[];
  
  final public int type;
  final public int minVal, maxVal;
  final int labelValues[];
  
  private Trait correlates[], opposite;
  private float weightings[];
  
  

  protected Trait(
    String name, int type, String... labels
  ) {
    this(name, "NO DESCRIPTION YET", type, labels);
  }
  
  
  protected Trait(
    String name, String description, int type, String... labels
  ) {
    super(TRAIT_INDEX, name, name);
    
    this.description = description;
    this.type        = type;
    this.labels      = labels;
    this.labelValues = new int[labels.length];
    
    if (verboseInit) {
      I.say("\n  Initialising new trait: "+name);
    }
    
    int zeroIndex = 0, min = -1, max = 1, val;
    for (String s : labels) { if (s == null) break; else zeroIndex++; }
    for (int i = labels.length; i-- > 0;) {
      val = labelValues[i] = zeroIndex - i;
      
      final String desc = labels[i];
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
    return TRAIT_INDEX.loadEntry(s.input());
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
    
    if (trait.labels.length == 1) {
      if (level == 0) return null;
      return (level > 0 ? "" : "Not ") + trait.labels[0];
    }
    
    String bestDesc = null;
    float minDiff = Float.POSITIVE_INFINITY;
    
    int i = 0; for (String s : trait.labels) {
      float value = trait.labelValues[i];
      if (value > 0) value /= trait.maxVal;
      if (value < 0) value /= 0 - trait.minVal;
      
      final float diff = Nums.abs(level - value);
      if (diff < minDiff) { minDiff = diff; bestDesc = s; }
      i++;
    }
    return bestDesc;
  }
  
  
  public void describeHelp(Description d, Selectable prior) {
    d.append(description);
  }
}





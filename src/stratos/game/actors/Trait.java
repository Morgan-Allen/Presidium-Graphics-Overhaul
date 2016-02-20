/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.start.Assets;
import stratos.user.*;
import stratos.util.*;



public class Trait extends Constant {
  
  
  private static boolean verboseInit = false;
  
  final public static Index <Trait> TRAIT_INDEX = new Index <Trait> ();
  
  final public ImageAsset icon;
  final public CutoutModel iconModel;
  
  final public String description;
  final String labels[];
  
  final public int type;
  final public int minVal, maxVal;
  final int labelValues[];
  
  private Trait correlates[], opposite;
  private float weightings[];
  
  

  protected Trait(
    Class baseClass, String name,
    int type, String... labels
  ) {
    this(baseClass, name, null, null, type, labels);
  }
  
  
  protected Trait(
    Class baseClass, String name, String description, String iconPath,
    int type, String... labels
  ) {
    super(TRAIT_INDEX, name, name);
    
    if (description == null) {
      description = "NO DESCRIPTION YET";
    }
    if (Assets.exists(iconPath)) {
      icon      = ImageAsset.fromImage(baseClass, iconPath);
      iconModel = CutoutModel.fromImage(baseClass, iconPath, 1, 1);
    }
    else {
      icon      = null;
      iconModel = null;
    }
    
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
  public void onAddition(Actor a) {
  }
  
  
  public void affect(Actor a) {
  }
  
  
  public void onRemoval(Actor a) {
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
  
  
  public static Trait[] correlate(Trait t, Trait other, float weight) {
    t    .assignCorrelates(new Trait[] { other }, new float[] { weight });
    other.assignCorrelates(new Trait[] { t     }, new float[] { weight });
    return new Trait[] { t, other };
  }
  
  
  protected float correlation(Trait other) {
    final int index = Visit.indexOf(other, correlates);
    return (index == -1) ? 0 : weightings[index];
  }
  
  
  public Trait opposite() { return opposite; }
  public Trait[] correlates() { return correlates; }
  public float[] correlateWeights() { return weightings; }
  
  
  public static float traitChance(Trait t, Actor a) {
    final Trait cA[] = t.correlates();
    if (cA == null) return 0;
    
    float plus = 0, minus = 0;
    final float wA[] = t.correlateWeights();
    
    for (int n = cA.length; n-- > 0;) {
      final Trait c = cA[n];
      final float w = wA[n];
      final float level = a.traits.traitLevel(c) * w;
      if (level > 0) plus  += (1 - plus ) * level;
      if (level < 0) minus -= (1 - minus) * level;
    }
    return plus - minus;
  }
  
  
  
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
    float rangeScale = Nums.max(trait.maxVal, 0 - trait.minVal);
    
    int i = 0; for (String s : trait.labels) {
      final float value = trait.labelValues[i] / rangeScale;
      final float diff = Nums.abs(level - value);
      if (diff < minDiff && s != null) { minDiff = diff; bestDesc = s; }
      i++;
    }
    
    //level = Nums.round(level * 10, 1, true) / 10f;
    return bestDesc;//+" ("+I.shorten(level, 2)+")";
  }
  
  
  public void describeHelp(Description d, Selectable prior) {
    substituteReferences(description, d);
  }
}














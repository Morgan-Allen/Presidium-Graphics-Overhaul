/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.actors ;
import stratos.game.civilian.Accountable;
import stratos.game.common.*;
import stratos.util.*;
import org.apache.commons.math3.util.FastMath;



//  TODO:  Have relation changes NOT due to dialogue *increase* novelty
public class Relation {
  
  
  /**  Fields, constructors, save/load methods and identity functions-
    */
  final public static int
    TYPE_GENERIC = 0,
    TYPE_CHILD   = 1,
    TYPE_PARENT  = 2,
    TYPE_SPOUSE  = 3,
    TYPE_SIBLING = 4,
    TYPE_LORD    = 5,
    TYPE_VASSAL  = 6;
  
  final public static float
    MIN_ADJUST    =  0.1f,
    MAG_KILLING   = -1.0f,
    MAG_HARMING   = -0.5f,
    MAG_CHATTING  = 0.33f,
    MAG_HELPING   = 0.66f,
    MAG_SAVE_LIFE = 1.00f;
  
  
  final public static int
    MAX_VALUE        = 100,
    NOVELTY_DAYS     = 2,
    NOVELTY_INTERVAL = World.STANDARD_DAY_LENGTH * NOVELTY_DAYS,
    FAMILIARITY_UNIT = 10,
    BASE_NUM_FRIENDS = 5 ,
    MAX_RELATIONS    = 10;
  
  final static String DESCRIPTORS[] = {
    "Soulmate",
    "Close",
    "Friendly",
    "Civil",
    "Ambivalent",
    "Tense",
    "Strained",
    "Hostile",
    "Nemesis"
  };
  
  private static boolean verbose = false;
  
  
  
  final public Accountable object, subject ;
  final private int hash ;
  
  private float attitude = 0, novelty = 0 ;
  private int type = TYPE_GENERIC ;
  
  
  public Relation(
    Accountable object, Accountable subject, float initLevel, float initNovelty
  ) {
    this.object = object ;
    this.subject = subject ;
    
    this.hash = Table.hashFor(object, subject) ;
    this.attitude = initLevel * MAX_VALUE;
    this.novelty = initNovelty * MAX_VALUE;
  }
  
  
  public boolean equals(Object o) {
    final Relation r = (Relation) o ;
    return r.object == object && r.subject == subject ;
  }
  
  
  public int hashCode() {
    return hash ;
  }
  
  
  
  private Relation(Session s) throws Exception {
    object = (Accountable) s.loadObject() ;
    subject = (Accountable) s.loadObject() ;
    hash = Table.hashFor(object, subject) ;
    attitude = s.loadFloat() ;
    novelty = s.loadFloat();
    type = s.loadInt() ;
  }
  
  
  public static Relation loadFrom(Session s) throws Exception {
    return new Relation(s) ;
  }
  
  
  public static void saveTo(Session s, Relation r) throws Exception {
    s.saveObject((Session.Saveable) r.object ) ;
    s.saveObject((Session.Saveable) r.subject) ;
    s.saveFloat(r.attitude) ;
    s.saveFloat(r.novelty);
    s.saveInt(r.type) ;
  }
  
  
  
  /**  Accessing and modifying the content of the relationship-
    */
  protected void update() {
    //  
    final float inRange = (1 + value()) / 2f;
    novelty += MAX_VALUE * 1f * inRange / NOVELTY_INTERVAL;
  }
  
  
  public float value() {
    return attitude / MAX_VALUE ;
  }
  
  
  public float novelty() {
    return novelty / MAX_VALUE;
  }
  
  
  public int type() {
    return type ;
  }
  
  
  protected void initRelation(float attitude, int type) {
    this.attitude = attitude ;
    this.type = type ;
  }
  
  
  public void incValue(float target, float weight) {
    if (target == 0 || weight <= 0) return;
    final boolean report = verbose && (
      I.talkAbout == subject ||
      I.talkAbout == object
    );
    final float value = value();
    weight = Visit.clamp(weight, 0, 1);
    
    if (report) {
      I.say("\nIncrementing relation between "+object+" and "+subject);
      I.say("  Current value: "+value+", level/weight: "+target+" / "+weight);
    }
    
    //  Relations are most easily adjustable when not at either extreme of the
    //  range, but we ensure that positive/negative target level always have
    //  *some* effect.
    final float
      min     = MIN_ADJUST * FastMath.abs(target),
      inRange = (1 + value) / 2f,
      budge   = Visit.clamp(inRange * (1 - inRange) * 4, MIN_ADJUST, 1);
    float gap = target - value;
    if (target  < 0 && gap > -min) gap = -min;
    if (target >= 0 && gap <  min) gap =  min;
    
    attitude += gap * budge * weight * MAX_VALUE;
    attitude = Visit.clamp(attitude, 0 - MAX_VALUE, MAX_VALUE);
    
    if (report) {
      I.say("  Budge factor: "+budge+", gap factor: "+gap);
      I.say("  Final value: "+value());
    }
    
    //  TODO:  Only decrease novelty as a result of conversation- other
    //  impact factors actually *increase* the novelty of the relationship.
    novelty -= FAMILIARITY_UNIT * (MIN_ADJUST + 1 - value);
  }
  
  
  public void setValue(float value, float novelty) {
    this.attitude = value   * MAX_VALUE;
    this.novelty  = novelty * MAX_VALUE;
  }
  
  
  public void setType(int type) {
    this.type = type ;
  }

  
  public static String describe(Relation r) {
    if (r == null) return "None";
    final float
      attSpan = MAX_VALUE * 2,
      level   = (MAX_VALUE - r.attitude) / attSpan ;
    final int DL = DESCRIPTORS.length ;
    return DESCRIPTORS[Visit.clamp((int) (level * (DL + 1)), DL)] ;
  }
  
  
  
  public String toString() {
    return describe(this)+" relation between "+object+" and "+subject;
  }
}









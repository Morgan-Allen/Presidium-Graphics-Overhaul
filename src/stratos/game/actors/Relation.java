/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors;
import stratos.game.common.*;
import stratos.util.*;




//  TODO:  These should mediate trade & defence dynamics.
//  Vassal.  Liege.  Alliance.
//  Vendetta.  Rebel.  Uprising.
//  Closed.  Neutral.  Trading.


public class Relation {
  
  
  /**  Fields, constructors, save/load methods and identity functions-
    */
  final public static int
    TYPE_GENERIC = 0,
    
    TYPE_CHILD   = 1,
    TYPE_PARENT  = 2,
    TYPE_SPOUSE  = 3,
    TYPE_SIBLING = 4,
    TYPE_MASTER  = 5,
    TYPE_SERVANT = 6,
    
    TYPE_TRADED  = 7,
    TYPE_GEAR    = 8;
  
  final static String TYPE_DESCRIPTORS[] = {
    "Generic",
    "Child", "Parent", "Spouse", "Sibling",
    "Master", "Servant", "Traded", "Gear"
  };
  
  final static float
    MIN_ADJUST   = 0.1f,
    MAX_VALUE    = 100 ;

  final static String AFFECT_DESCRIPTORS[] = {
    "Soulmate"  ,
    "Close"     ,
    "Friendly"  ,
    "Civil"     ,
    "Ambivalent",
    "Tense"     ,
    "Strained"  ,
    "Hostile"   ,
    "Nemesis"   ,
  };
  
  private static boolean verbose = false;
  
  
  
  final public Accountable object, subject;
  final private int hash;
  
  private float attitude = 0, novelty = 0;
  private int type = TYPE_GENERIC;
  
  
  public Relation(
    Accountable object, Accountable subject, float initValue, float initNovelty
  ) {
    this.object   = object;
    this.subject  = subject;
    this.hash     = Table.hashFor(object, subject);
    this.attitude = initValue   * MAX_VALUE;
    this.novelty  = initNovelty * MAX_VALUE;
  }
  
  
  public boolean equals(Object o) {
    final Relation r = (Relation) o;
    return r.object == object && r.subject == subject;
  }
  
  
  public int hashCode() {
    return hash;
  }
  
  
  
  private Relation(Session s) throws Exception {
    object   = (Accountable) s.loadObject();
    subject  = (Accountable) s.loadObject();
    hash     = Table.hashFor(object, subject);
    attitude = s.loadFloat();
    novelty  = s.loadFloat();
    type     = s.loadInt  ();
  }
  
  
  public static Relation loadFrom(Session s) throws Exception {
    return new Relation(s);
  }
  
  
  public static void saveTo(Session s, Relation r) throws Exception {
    s.saveObject((Session.Saveable) r.object );
    s.saveObject((Session.Saveable) r.subject);
    s.saveFloat(r.attitude);
    s.saveFloat(r.novelty);
    s.saveInt(r.type);
  }
  
  
  
  /**  Accessing and modifying the content of the relationship-
    */
  public float value() {
    return attitude / MAX_VALUE;
  }
  
  
  public float novelty() {
    return novelty / MAX_VALUE;
  }
  
  
  public int type() {
    return type;
  }
  
  
  protected void initRelation(float attitude, int type) {
    this.attitude = attitude;
    this.type = type;
  }
  
  
  public void setValue(float value, float novelty) {
    this.attitude = value   * MAX_VALUE;
    this.novelty  = novelty * MAX_VALUE;
  }
  
  
  public void setType(int type) {
    this.type = type;
  }
  
  
  public void incValue(float target, float weight) {
    if (target == 0 || weight <= 0) return;
    final boolean report = verbose && (
      I.talkAbout == subject ||
      I.talkAbout == object
    );
    final float value = value();
    weight = Nums.clamp(weight, 0, 1);
    
    if (report) {
      I.say("\nIncrementing relation between "+object+" and "+subject);
      I.say("  Current value: "+value+", level/weight: "+target+" / "+weight);
    }
    
    //  Relations are most easily adjustable when not at either extreme of the
    //  range, but we ensure that positive/negative target levels always have
    //  *some* (positive/negative) effect.
    final float
      min     = MIN_ADJUST * Nums.abs(target),
      inRange = (1 + value) / 2f,
      budge   = Nums.clamp(inRange * (1 - inRange) * 4, MIN_ADJUST, 1);
    float gap = target - value;
    if (target  < 0 && gap > -min) gap = -min;
    if (target >= 0 && gap <  min) gap =  min;
    
    attitude += gap * budge * weight * MAX_VALUE;
    attitude = Nums.clamp(attitude, 0 - MAX_VALUE, MAX_VALUE);
    
    if (report) {
      I.say("  Budge factor: "+budge+", gap factor: "+gap);
      I.say("  Final value: "+value());
    }
  }
  
  
  public void incNovelty(float inc) {
    novelty = Nums.clamp(novelty + (inc * MAX_VALUE), 0, MAX_VALUE);
  }

  
  
  /**  Interface and printout methods-
    */
  public static String describe(Relation r) {
    if (r == null) return "None";
    
    final String affectDesc = describeRelation(r.attitude / MAX_VALUE);
    if (r.type != TYPE_GENERIC) {
      return TYPE_DESCRIPTORS[r.type]+", "+affectDesc;
    }
    else {
      return affectDesc;
    }
  }
  
  
  public static String describeRelation(float level) {
    final int DL = AFFECT_DESCRIPTORS.length;
    final float absLevel = 1 - ((level + 1) / 2);
    return AFFECT_DESCRIPTORS[Nums.clamp((int) (absLevel * DL), DL)];
  }
  
  
  public String toString() {
    return describe(this)+" relation between "+object+" and "+subject;
  }
}


















/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */



package stratos.game.actors ;
//import stratos.game.actors.Behaviour;
import org.apache.commons.math3.util.FastMath;

import stratos.game.civilian.Accountable;
import stratos.game.common.*;
import stratos.util.*;



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
    MAG_KILLING   = -1.0f,
    MAG_HARMING   = -0.5f,
    MAG_CHATTING  = 0.33f,
    MAG_HELPING   = 0.66f,
    MAG_SAVE_LIFE = 1.00f;
  
  
  final public static int
    MAX_VALUE        = 100,
    NOVELTY_INTERVAL = World.STANDARD_DAY_LENGTH * 5,
    FAMILIARITY_UNIT = 10,
    BASE_NUM_FRIENDS = 5 ,
    MAX_RELATIONS    = 25;
  
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
  } ;
  
  
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
    novelty += MAX_VALUE * 1f / NOVELTY_INTERVAL;
  }
  
  
  public float value() {
    return attitude / MAX_VALUE ;
  }
  
  
  public float novelty() {
    return novelty / MAX_VALUE;
  }
  
  
  /*
  public float novelty(World world) {
    final float delay = (world.currentTime() - initTime) / NOVELTY_INTERVAL ;
    return Visit.clamp(delay - (familiarity / FAMILIARITY_DIVISOR), 0, 1) ;
  }
  //*/
  
  
  public int type() {
    return type ;
  }
  
  
  protected void initRelation(float attitude, int type) {
    this.attitude = attitude ;
    this.type = type ;
  }
  
  
  public void incValue(float level, float weight) {
    if (level == 0 || weight == 0) return;
    final float value = value();
    
    //  If the magnitude of the desired level is greater than the current level,
    //  or of opposite sign, make the adjustment.
    if (FastMath.abs(value / level) < 1 || value * level < 0) {
      final float gap = level - value;
      attitude += gap * weight * MAX_VALUE;
      attitude = Visit.clamp(attitude, -MAX_VALUE, MAX_VALUE);
      //I.say(this+" has value: "+attitude);
    }
    
    novelty -= FAMILIARITY_UNIT;
  }
  
  
  public void setValue(float value, float novelty) {
    this.attitude = value * MAX_VALUE;
    this.novelty = novelty * MAX_VALUE;
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









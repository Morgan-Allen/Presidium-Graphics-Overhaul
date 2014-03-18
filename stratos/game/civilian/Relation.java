/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */



package stratos.game.civilian ;
//import stratos.game.actors.Behaviour;
import stratos.game.common.*;
import stratos.util.*;



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
    TYPE_VASSAL  = 6 ;
  
  /*
  final public static float
    MAG_CHATTING  = 0.33f,
    MAG_HELPING   = 0.66f,
    MAG_SAVE_LIFE = 1.00f ;
  //*/
  
  
  final public static int
    MIN_ATTITUDE  = -100,
    MAX_ATTITUDE  =  100,
    ATTITUDE_SPAN =  MAX_ATTITUDE - MIN_ATTITUDE,
    ATTITUDE_DIE  =  10 ,
    NOVELTY_INTERVAL    = World.STANDARD_DAY_LENGTH * 5,
    FAMILIARITY_DIVISOR = 10,
    BASE_NUM_FRIENDS    = 5 ,
    MAX_RELATIONS       = 25 ;
  
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
  final int initTime ;  //...This has to be the time the relation was founded.
  
  private float attitude = 0 ;
  private int familiarity = 0 ;
  private int type = TYPE_GENERIC ;
  
  
  public Relation(
    Accountable object, Accountable subject, float initLevel, int initTime
  ) {
    this.object = object ;
    this.subject = subject ;
    this.initTime = initTime ;
    
    this.hash = Table.hashFor(object, subject) ;
    this.attitude = initLevel * MAX_ATTITUDE ;
    this.familiarity = 0 ;
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
    initTime = s.loadInt() ;
    hash = Table.hashFor(object, subject) ;
    attitude = s.loadFloat() ;
    familiarity = s.loadInt() ;
    type = s.loadInt() ;
  }
  
  
  public static Relation loadFrom(Session s) throws Exception {
    return new Relation(s) ;
  }
  
  
  public static void saveTo(Session s, Relation r) throws Exception {
    s.saveObject((Session.Saveable) r.object ) ;
    s.saveObject((Session.Saveable) r.subject) ;
    s.saveInt(r.initTime) ;
    s.saveFloat(r.attitude) ;
    s.saveInt(r.familiarity) ;
    s.saveInt(r.type) ;
  }
  
  
  
  /**  Accessing and modifying the content of the relationship-
    */
  public float value() {
    return attitude / MAX_ATTITUDE ;
  }
  
  
  public float novelty(World world) {
    final float delay = (world.currentTime() - initTime) / NOVELTY_INTERVAL ;
    return Visit.clamp(delay - (familiarity / FAMILIARITY_DIVISOR), 0, 1) ;
  }
  
  
  public int type() {
    return type ;
  }
  
  
  public String descriptor() {
    final float
      attSpan = MAX_ATTITUDE - MIN_ATTITUDE,
      level   = (MAX_ATTITUDE - attitude) / attSpan ;
    final int DL = DESCRIPTORS.length ;
    return DESCRIPTORS[Visit.clamp((int) (level * (DL + 1)), DL)] ;
  }
  
  
  public void initRelation(float attitude, int type) {
    this.attitude = attitude ;
    this.type = type ;
  }
  
  
  //
  //  TODO:  This is going to need a significant overhaul.
  
  public void incValue(float inc) {
    //Include weight, and use it to modify familiarity?
    //
    //  Roll dice matching current relationship against magnitude of event.
    final int numDice = (int) (Math.abs(attitude / ATTITUDE_DIE) + 0.5f) ;
    int roll = 0 ;
    for (int n = numDice ; n-- > 0 ;) roll += Rand.yes() ? 1 : 0 ;
    final float diff = (Math.abs(inc) * MAX_ATTITUDE) - (roll * ATTITUDE_DIE) ;
    //
    //  Raise/lower by half the margin of failure, and increment familiarity
    //  either way.
    if (diff > 0) {
      attitude += (inc > 0) ? (diff / 2) : (diff / -2) ;
      attitude = Visit.clamp(attitude, MIN_ATTITUDE, MAX_ATTITUDE) ;
    }
    familiarity++ ;
  }
  
  
  public void setType(int type) {
    this.type = type ;
  }
}









/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.economic;
import stratos.game.common.*;
import stratos.util.*;

import java.lang.reflect.*;



//  TODO:  Later, you'll want to try and migrate as many relevant attributes
//  over to this class as possible:
  /*
  final public int facilityType;
  //  TODO:  Also, icon, possibly model and any construction dependancies-
  //  you need to be able to filter this, for both the player and base AI.
  
  final public int
    size, maxIntegrity, armouring, ambience;
  
  final public Traded  materials[];
  final public Background careers  [];
  final public Conversion services [];
  //*/
 

public class Blueprint extends Index.Entry implements Session.Saveable {
  
  
  final public static Index <Blueprint> INDEX = new Index <Blueprint> ();
  
  final public Class <? extends Venue> baseClass;
  final public String name;
  final public String category;

  final public int size, high;
  final public int properties;
  
  final public Blueprint required[];
  final public int owningTier;
  final public Conversion processed[];
  
  final public int maxIntegrity = Structure.DEFAULT_INTEGRITY;
  
  private Batch <Blueprint> allows = new Batch <Blueprint> ();
  private Batch <Blueprint> denies = new Batch <Blueprint> ();
  
  
  public Blueprint(
    Class <? extends Venue> baseClass, String key,
    String name, String category,
    int size, int high, int properties,
    Blueprint required, int owningTier, Conversion... processed
  ) {
    this(
      baseClass, key, name, category, size, high, properties,
      required == null ? null : new Blueprint[] { required },
      owningTier, processed
    );
  }
  

  public Blueprint(
    Class <? extends Venue> baseClass, String key,
    String name, String category,
    int size, int high, int properties,
    Blueprint required[], int owningTier, Conversion... processed
  ) {

    super(INDEX, key);
    this.baseClass = baseClass;
    this.name     = name;
    this.category = category;
    
    this.size = size;
    this.high = high;
    
    this.properties = properties;
    this.required   = required == null ? Venue.NO_REQUIREMENTS : required;
    this.owningTier = owningTier;
    this.processed  = processed ;
    
    for (Blueprint p : required) p.allows.include(this);
  }
  
  
  public static Blueprint loadConstant(Session s) throws Exception {
    return INDEX.loadEntry(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    INDEX.saveEntry(this, s.output());
  }
  
  
  
  /**  Property queries-
    */
  public Series <Blueprint> allows() {
    return allows;
  }
  
  
  public boolean hasProperty(int property) {
    return (properties & property) == property;
  }
  
  
  public boolean isFixture() {
    return hasProperty(Venue.IS_FIXTURE);
  }
  
  
  public boolean isSingle() {
    return ! (hasProperty(Venue.IS_LINEAR));// || hasProperty(Venue.IS_ZONED));
  }
  
  
  public boolean isStandard() {
    return properties == Venue.IS_NORMAL;
  }
  
  
  public boolean isUnique() {
    return hasProperty(Venue.IS_UNIQUE);
  }
  
  
  public boolean isWild() {
    return hasProperty(Venue.IS_WILD);
  }
  
  
  public Conversion producing(Object t) {
    for (Conversion c : processed) if (c.out.type == t) return c;
    return null;
  }
  
  
  public Conversion consuming(Object t) {
    for (Conversion c : processed) {
      for (Item i : c.raw) if (i.type == t) return c;
    }
    return null;
  }
  
  
  
  /**  Save and load functions for external reference.
    */
  private static Blueprint CIVIC_BP[];
  
  
  public static Blueprint[] allBlueprints() {
    return INDEX.allEntries(Blueprint.class);
  }
  
  
  public static Blueprint[] allCivicBlueprints() {
    if (CIVIC_BP != null) return CIVIC_BP;
    final Batch <Blueprint> matches = new Batch <Blueprint> ();
    for (Blueprint p : allBlueprints()) if (! p.isWild()) matches.add(p);
    return CIVIC_BP = matches.toArray(Blueprint.class);
  }
  
  
  public static Venue[] sampleVenues(
    int owningTier, Blueprint... canPlace
  ) {
    if (canPlace == null) canPlace = allBlueprints();
    final Batch <Venue> typeBatch = new Batch <Venue> ();
    
    for (Blueprint blueprint : canPlace) {
      final Venue sample = blueprint.createVenue(null);
      if (sample == null || sample.owningTier() > owningTier) continue;
      typeBatch.add(sample);
    }
    return typeBatch.toArray(Venue.class);
  }
  
  
  public Venue createVenue(Base base) {
    try {
      if (! Venue.class.isAssignableFrom(baseClass)) return null;
      final Constructor c = baseClass.getConstructor(Base.class);
      return (Venue) c.newInstance(base);
    }
    catch (NoSuchMethodException e) {
      I.say(
        "\n  WARNING: NO BASE CONSTRUCTOR FOR: "+baseClass.getName()+
        "\n  All Venues should implement a public constructor taking a Base "+
        "\n  as the sole argument, or else their blueprint should override "+
        "\n  the createVenue() method.  Thank you.\n"
      );
      return null;
    }
    catch (Exception e) {
      I.say("ERROR INSTANCING "+baseClass.getSimpleName()+": "+e);
      e.printStackTrace();
      return null;
    }
  }
  
  
  
  /**  Interface and debugging-
    */
  public String toString() {
    return name;
  }
}






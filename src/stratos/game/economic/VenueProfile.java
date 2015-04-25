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

//  TODO:  Get rid of entry-face and allow structures being placed to find
//  their own.
 

public class VenueProfile extends Index.Entry implements Session.Saveable {
  
  
  final public static Index <VenueProfile> INDEX = new Index <VenueProfile> ();
  
  final public Class <? extends Venue> baseClass;
  final public String name;

  final public int size, high;
  final public int properties;
  
  final public VenueProfile required[];
  final public int owningTier;
  final public Conversion processed[];
  
  final public int maxIntegrity = Structure.DEFAULT_INTEGRITY;
  
  private Batch <VenueProfile> allows = new Batch <VenueProfile> ();
  private Batch <VenueProfile> denies = new Batch <VenueProfile> ();
  
  
  public VenueProfile(
    Class <? extends Venue> baseClass, String key, String name,
    int size, int high, int properties,
    VenueProfile required, int owningTier, Conversion... processed
  ) {
    this(
      baseClass, key, name,
      size, high, properties,
      required == null ? null : new VenueProfile[] { required },
      owningTier, processed
    );
  }
  

  public VenueProfile(
    Class <? extends Venue> baseClass, String key, String name,
    int size, int high, int properties,
    VenueProfile required[], int owningTier, Conversion... processed
  ) {

    super(INDEX, key);
    this.baseClass = baseClass;
    this.name = name;
    
    this.size = size;
    this.high = high;
    
    this.properties = properties;
    this.required   = required == null ? Venue.NO_REQUIREMENTS : required;
    this.owningTier = owningTier;
    this.processed  = processed ;
    
    for (VenueProfile p : required) p.allows.include(this);
  }
  
  
  public static VenueProfile loadConstant(Session s) throws Exception {
    return INDEX.loadEntry(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    INDEX.saveEntry(this, s.output());
  }
  
  
  
  /**  Property queries-
    */
  public Series <VenueProfile> allows() {
    return allows;
  }
  
  
  public boolean hasProperty(int property) {
    return (properties & property) == property;
  }
  
  
  public boolean isFixture() {
    return hasProperty(Venue.IS_FIXTURE);
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
  
  
  public Batch <Conversion> consuming(Object t) {
    final Batch <Conversion> matches = new Batch <Conversion> ();
    for (Conversion c : processed) for (Item i : c.raw) {
      if (i.type == t) matches.add(c);
    }
    return matches;
  }
  
  
  
  /**  Save and load functions for external reference.
    */
  private static VenueProfile CIVIC_PROFILES[];
  
  
  public static VenueProfile[] allProfiles() {
    return INDEX.allEntries(VenueProfile.class);
  }
  
  
  public static VenueProfile[] allCivicProfiles() {
    if (CIVIC_PROFILES != null) return CIVIC_PROFILES;
    final Batch <VenueProfile> matches = new Batch <VenueProfile> ();
    for (VenueProfile p : allProfiles()) if (! p.isWild()) matches.add(p);
    return CIVIC_PROFILES = matches.toArray(VenueProfile.class);
  }
  
  
  public static Venue[] sampleVenues(
    int owningTier, VenueProfile... canPlace
  ) {
    if (canPlace == null) canPlace = allProfiles();
    final Batch <Venue> typeBatch = new Batch <Venue> ();
    
    for (VenueProfile profile : canPlace) {
      final Venue sample = profile.sampleVenue(null);
      if (sample == null || sample.owningTier() > owningTier) continue;
      typeBatch.add(sample);
    }
    return typeBatch.toArray(Venue.class);
  }
  
  
  public Venue sampleVenue(Base base) {
    try {
      if (! Venue.class.isAssignableFrom(baseClass)) return null;
      final Constructor c = baseClass.getConstructor(Base.class);
      return (Venue) c.newInstance(base);
    }
    catch (NoSuchMethodException e) {
      I.say(
        "\n  WARNING: NO BASE CONSTRUCTOR FOR: "+baseClass.getName()+
        "\n  All Venues should implement a public constructor taking a Base "+
        "\n  as the sole argument, or else their profile should override the "+
        "\n  sampleVenue() method.  Thank you.\n"
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






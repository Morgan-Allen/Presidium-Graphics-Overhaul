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
  //  TODO:  Also, name, icon, possibly model and any construction
  //  dependancies- you need to be able to filter this, for both the player and
  //  base AI.
  
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
  
  final public VenueProfile required[];
  final public int size, high, entryFace;
  final public Conversion processed[];
  final public int maxIntegrity = Structure.DEFAULT_INTEGRITY;
  
  
  public VenueProfile(
    Class <? extends Venue> baseClass, String key, String name,
    int size, int high, int entryFace,  //  TODO:  GET RID OF ENTRY FACE!
    VenueProfile required,
    Conversion... processed
  ) {
    super(INDEX, key);
    this.baseClass = baseClass;
    
    this.name = name;
    this.required = required == null ?
      new VenueProfile[0] :
      new VenueProfile[] { required };
    
    this.size = size;
    this.high = high;
    this.entryFace = entryFace;
    
    this.processed = processed;
  }
  
  
  
  /**  Save and load functions for external reference.
    */
  public static VenueProfile loadConstant(Session s) throws Exception {
    return INDEX.loadFromEntry(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    INDEX.saveEntry(this, s.output());
  }
  
  
  public static VenueProfile[] allProfiles() {
    return INDEX.allEntries(VenueProfile.class);
  }
  
  
  public static Venue[] sampleVenues(
    int owningTier, VenueProfile... canPlace
  ) {
    if (canPlace == null || canPlace.length == 0) canPlace = allProfiles();
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






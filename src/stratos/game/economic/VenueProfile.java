/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.economic;

import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.politic.BaseSetup;
import stratos.start.Assets;
import stratos.util.*;

import java.lang.reflect.*;



//  TODO:  Later, you'll want to try and migrate as many relevant attributes
//  over to this class as possible.


public class VenueProfile implements Session.Saveable {
  
  
  final public Class <? extends Venue> baseClass;
  private static Table <Object, VenueProfile> allProfiles = new Table();
  
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
  //  TODO:  Have more of these, and specify within constructor.  Obviously...
  final public int maxIntegrity = Structure.DEFAULT_INTEGRITY;
  
  public VenueProfile(
    Class <? extends Venue> baseClass, Object key
    /*, int facilityType,
    int size, int maxIntegrity, int armouring, int ambience,
    Traded materials[],
    Background careers[],
    Conversion... services
    //*/
  ) {
    this.baseClass    = baseClass   ;
    /*
    this.facilityType = facilityType;
    
    this.size         = size        ;
    this.maxIntegrity = maxIntegrity;
    this.armouring    = armouring   ;
    this.ambience     = ambience    ;
    
    this.materials = materials;
    this.careers   = careers  ;
    this.services  = services ;
    //Conversion.parse(baseClass, conversionArgs);  //  TODO:  use this
    //*/
    
    allProfiles.put(key, this);
  }
  
  
  public static Venue sampleVenue(Class baseClass, Base base) {
    try {
      if (! Venue.class.isAssignableFrom(baseClass)) return null;
      final Constructor c = baseClass.getConstructor(Base.class);
      return (Venue) c.newInstance(base);
    }
    catch (NoSuchMethodException e) {
      I.say(
        "\n  WARNING: NO BASE CONSTRUCTOR FOR: "+baseClass.getName()+
        "\n  All Venues should implement a public constructor taking a Base "+
        "\n  as the sole argument, or they may not save & load properly.\n"
      );
      return null;
    }
    catch (Exception e) {
      I.say("ERROR INSTANCING "+baseClass.getSimpleName()+": "+e);
      e.printStackTrace();
      return null;
    }
  }
  
  
  public Venue sampleVenue(Base base) {
    return sampleVenue(baseClass, base);
  }
  
  
  
  /**  Save and load functions for external reference.
    */
  
  public static VenueProfile loadConstant(Session s) throws Exception {
    venueTypes();
    final String className = s.loadString();
    final Class key = Class.forName(className);
    final VenueProfile profile = key == null ? null : allProfiles.get(key);
    
    if (profile == null) I.say(
      "NO PROFILE FOUND FOR "+className+
      ", WILL NOT LOAD PROPERLY AFTER FIRST REFERENCE!"
    );
    return profile;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveString(baseClass.getName());
  }
  
  
  public static VenueProfile profileFor(Class venueClass) {
    return allProfiles.get(venueClass);
  }
  

  
  /**  Compiles a list of all facility types and their behaviour profiles for
    *  ease of iteration, etc.
    */
  private static Class        allFT[] = null;
  private static VenueProfile allFP[] = null;
  
  
  public static Class[] venueTypes() {
    if (allFT != null) return allFT;
    
    final Batch <Class       > allTypes    = new Batch();
    final Batch <VenueProfile> allProfiles = new Batch();
    
    for (Class baseClass : Assets.loadPackage("stratos.game")) {
      final Venue sample = VenueProfile.sampleVenue(baseClass, null);
      if (sample != null) {
        allTypes.add(baseClass);
        allProfiles.add(sample.profile);
      }
    }
    
    allFT = allTypes   .toArray(Class       .class);
    allFP = allProfiles.toArray(VenueProfile.class);
    return allFT;
  }
  
  
  public static VenueProfile[] facilityProfiles() {
    venueTypes();
    return allFP;
  }
  
  
  public static Venue[] sampleVenues(
    int owningTier, VenueProfile... canPlace
  ) {
    if (canPlace == null || canPlace.length == 0) canPlace = allFP;
    final Batch <Venue> typeBatch = new Batch <Venue> ();
    
    for (VenueProfile profile : canPlace) {
      final Venue sample = profile.sampleVenue(null);
      if (sample == null || sample.owningTier() > owningTier) continue;
      typeBatch.add(sample);
    }
    return typeBatch.toArray(Venue.class);
  }
  
  
  
  /**  Interface and debugging-
    */
  public String toString() {
    //  TODO:  SPECIFY THE NAME FIELD HERE!
    return baseClass.getSimpleName();
  }
}






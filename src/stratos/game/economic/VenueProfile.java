/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.economic;

import stratos.game.actors.*;
import stratos.game.campaign.BaseSetup;
import stratos.game.common.*;
import stratos.start.Assets;
import stratos.util.*;

import java.lang.reflect.*;



public class VenueProfile implements Session.Saveable {
  
  
  final public Class <? extends Venue> baseClass;
  final public int facilityType;
  
  //  TODO:  Also, name, icon, possibly model and any construction
  //  dependancies- you need to be able to filter this, for both the player and
  //  base AI.
  
  final public int
    size, maxIntegrity, armouring, ambience;
  
  final public Traded  materials[];
  final public Background careers  [];
  final public Conversion services [];
  
  
  public VenueProfile(
    Class <? extends Venue> baseClass, int facilityType,
    int size, int maxIntegrity, int armouring, int ambience,
    Traded materials[],
    Background careers[],
    Conversion... services
  ) {
    this.baseClass    = baseClass   ;
    this.facilityType = facilityType;
    
    this.size         = size        ;
    this.maxIntegrity = maxIntegrity;
    this.armouring    = armouring   ;
    this.ambience     = ambience    ;
    
    this.materials = materials;
    this.careers   = careers  ;
    this.services  = services ;
    //Conversion.parse(baseClass, conversionArgs);  //  TODO:  use this
    
    allProfiles.put(baseClass, this);
  }
  
  
  public static Venue sampleVenue(Class baseClass) {
    try {
      if (! Venue.class.isAssignableFrom(baseClass)) return null;
      final Constructor c = baseClass.getConstructor(Base.class);
      return (Venue) c.newInstance((Base) null);
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
  
  
  
  /**  Save and load functions for external reference.
    */
  private static Table <Class, VenueProfile> allProfiles = new Table();
  
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
      final Venue sample = VenueProfile.sampleVenue(baseClass);
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
  
  
  public static Venue[] sampleVenues(int owningType, boolean privateProperty) {
    final Batch <Venue> typeBatch = new Batch <Venue> ();
    
    for (VenueProfile p : facilityProfiles()) {
      final Venue sample = VenueProfile.sampleVenue(p.baseClass);
      if (sample.owningType() > owningType) continue;
      if (sample.privateProperty() != privateProperty) continue;
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






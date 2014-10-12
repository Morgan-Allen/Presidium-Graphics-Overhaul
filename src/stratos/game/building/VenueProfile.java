/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.building;

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
  
  final public TradeType  materials[];
  final public Background careers  [];
  final public Conversion services [];
  
  
  public VenueProfile(
    Class <? extends Venue> baseClass, int facilityType,
    int size, int maxIntegrity, int armouring, int ambience,
    TradeType materials[],
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
    catch (Exception e) { return null; }
  }
  
  
  
  /**  Save and load functions for external reference.
    */
  private static Table <Class, VenueProfile> allProfiles = new Table();
  
  
  public static VenueProfile loadConstant(Session s) throws Exception {
    venueTypes();
    final Class key = s.loadClass();
    return allProfiles.get(key);
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveClass(baseClass);
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
    
    for (Class baseClass : Assets.loadPackage("stratos.game.base")) {
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









package stratos.game.building;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.util.*;






public class FacilityProfile implements Session.Saveable {
  
  
  final public Class <? extends Venue> baseClass;
  final public int facilityType;
  
  //  TODO:  Also, name, icon, possibly model and any construction dependancies.
  
  final public int
    size, maxIntegrity, armouring, ambience;
  
  final public TradeType  materials[];
  final public Background careers  [];
  final public Conversion services [];
  
  
  public FacilityProfile(
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
    this.services  = services ;//Conversion.parse(baseClass, conversionArgs);
    
    allProfiles.put(baseClass, this);
  }
  
  
  
  /**  Save/load functionality...
    */
  private static Table <Class, FacilityProfile> allProfiles = new Table();
  
  
  public static FacilityProfile loadConstant(Session s) throws Exception {
    final Class key = s.loadClass();
    return allProfiles.get(key);
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveClass(baseClass);
  }
  
  
  public static FacilityProfile profileFor(Class venueClass) {
    return allProfiles.get(venueClass);
  }
  
  
  
  /**  Interface and debugging-
    */
  public String toString() {
    //  TODO:  SPECIFY THE NAME FIELD HERE!
    return baseClass.getSimpleName();
  }
}











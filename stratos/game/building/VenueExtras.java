


package stratos.game.building ;
import stratos.game.common.*;
import stratos.util.*;



//  Used to keep track of satellite structures and associated vehicles, etc.
//  TODO:  Move upgrades functionality here as well...?  It's not really being
//         used by, say, vehicles.


public class VenueExtras {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final Venue venue ;
  final List <Element> extras = new List <Element> () ;
  
  
  VenueExtras(Venue venue) {
    this.venue = venue ;
  }
  
  
  void loadState(Session s) throws Exception {
    s.loadObjects(extras) ;
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveObjects(extras) ;
  }
  
  
  
  
  /**  Mutation and update methods-
    */
  void toggleExtra(Element e, boolean is) {
    if (is) {
      extras.include(e) ;
    }
    else {
      extras.remove(e) ;
    }
  }
  
  
  void updateExtras() {
    for (ListEntry <Element> e = extras ; (e = e.nextEntry()) != extras ;) {
      if (e.refers.destroyed()) e.delete() ;
    }
  }
  
  
  void onWorldExit() {
    for (Element e : extras) {
      
      if (e instanceof Venue) {
        final Venue venue = (Venue) e ;
        venue.structure.setState(Structure.STATE_SALVAGE, -1) ;
      }
      
      else if (e instanceof Vehicle) {
        final Vehicle vehicle = (Vehicle) e ;
        vehicle.setAsDestroyed() ;
      }
      
      else {
        e.setAsDestroyed() ;
      }
    }
  }
  
  
  Batch <Element> extrasOfType(Class extraClass) {
    final Batch <Element> matches = new Batch <Element> () ;
    for (Element e : extras) {
      if (extraClass.isAssignableFrom(e.getClass())) matches.add(e) ;
    }
    return matches ;
  }
  
  
  
  /**  TODO:  Include various helper methods for siting new satellite
    *  structures...
    */
  boolean lookForSite(Fixture f, Visit rating) {
    
    return false ;
  }
}









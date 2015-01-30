

package stratos.game.economic;
import stratos.game.common.*;


public class BaseSetup2 {
  
  
  Stage world;
  Base base;
  
  
  VenueProfile canPlace[];
  
  
  
  private class SiteAttempt {
    
    int maxEval;
    int stage;
    int atX, atY;
    Target bestPoint;
    float bestRating;
    
  }
  
  
  
  
  //  Alright.  As long as demands are unsatisfied, and as long as the number
  //  of structures being built doesn't exceed a certain threshold, try to find
  //  new placements for structures which can address those demands.
  
  
  //  Okay.  You have a bunch of demands.  Which are associated with certain
  //  locations.
  
  //  And you have structures which can, in principle, fulfill certain of those
  //  demands- and will, in turn, make demands of their own.
  
  //  Use the VenueProfile class for this- it can store Conversions.
}












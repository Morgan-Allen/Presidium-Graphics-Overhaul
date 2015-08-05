/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.maps;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.user.*;
import stratos.util.*;


//
//  I need the following function-implementations for a Siting...
//
//  1.  An overall rating of demand for a given venue within the settlement as
//      a whole.
//  2.  A measure of the degree to which an existing venue can satisfy such
//      demands at it's current location.
//  3.  Ratings for the suitability of siting within broad areas.
//  4.  Ratings for the suitability of siting at exact locations.
//  5.  The ability to time-slice these operations (and even save them.)
//  6.  The ability to customise or override these methods for use in once-off
//      placements or unique structure-types.

public class Siting extends Constant {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final static Index <Siting> INDEX = new Index();
  
  final public Blueprint blueprint;
  
  
  public Siting(Blueprint origin) {
    super(INDEX, "siting_for_"+origin.keyID, "Siting for "+origin.name);
    this.blueprint = origin;
    this.blueprint.linkWith(this);
  }
  
  
  public static Siting loadConstant(Session s) throws Exception {
    return INDEX.loadEntry(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    INDEX.saveEntry(this, s.output());
  }
  
  
  public void describeHelp(Description d, Selectable prior) {
    d.append(name);
  }
  
  
  
  /**  Default methods for rating placement within a given map...
    */
  public float rateSettlementDemand(Base base) {
    float sumDemand = 0;
    for (Conversion c : blueprint.production()) if (c.out != null) {
      sumDemand += c.out.amount * base.demands.globalDemand(c.out.type);
    }
    return sumDemand;
  }
  
  
  public float rateEfficiency(Venue v, Target point) {
    //  TODO:  This could stand some elaboration.  Unify with method below...?
    return 1;
  }
  
  
  public float ratePointDemand(Base base, Target point, boolean exact) {
    float sumRating = 1;
    
    //  TODO:  Also include the effects of ambience & danger!  (And try to
    //  make sure that venues cluster together a bit, for neatness' sake.)
    
    for (Conversion c : blueprint.production()) {
      float supply = 0, demand = 0;
      
      if (c.out == null) demand = 1;
      else {
        demand += base.demands.demandAround(point, c.out.type, -1);
        demand /= c.out.amount;
      }
      
      if (Visit.empty(c.raw)) supply = 1;
      else for (Item i : c.raw) {
        supply += base.demands.supplyAround(point, i.type, -1);
        supply /= i.amount * c.raw.length;
      }
      
      sumRating += supply * demand / 10;
    }
    return sumRating;
  }
  
  
}








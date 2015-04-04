


package stratos.game.economic;
import stratos.game.common.*;
import stratos.graphics.sfx.TalkFX;



public interface Owner extends Target, Session.Saveable {
  
  final static int
    TIER_NATURAL  = -2,
    TIER_CARRIES  = -1,
    TIER_PRIVATE  =  0,
    TIER_FACILITY =  1,
    TIER_DEPOT    =  2,
    TIER_SHIPPING =  3;
  
  Base base();
  Inventory inventory();
  int owningTier();
  
  float priceFor(Traded service);
  int spaceFor(Traded good);
  void afterTransaction(Item item, float amount);
  
  
  
  /**  Rendering, debug and interface methods-
    */
  //  TODO:  You might move chat displays to the afterTransaction method.
  TalkFX chat();
  
  final public static String TIER_NAMES[] = {
    "Natural", "Carries", "Private",
    "Facility", "Depot", "Shipping"
  };
}









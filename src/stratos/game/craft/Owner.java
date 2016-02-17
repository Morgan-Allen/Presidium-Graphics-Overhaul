/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.craft;
import stratos.game.common.*;
import stratos.graphics.sfx.TalkFX;



public interface Owner extends Target, Session.Saveable {
  
  final static int
    TIER_TERRAIN  = -2,
    TIER_OBJECT   = -1,
    TIER_PRIVATE  =  0,
    TIER_FACILITY =  1,
    TIER_TRADER   =  2,
    TIER_SHIPPING =  3;
  
  Base base();
  Inventory inventory();
  int owningTier();
  
  int spaceCapacity();
  float priceFor(Traded good, boolean sold);
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









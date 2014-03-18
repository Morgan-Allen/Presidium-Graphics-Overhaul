

package code.game.building ;
import code.game.common.*;
import code.graphics.common.*;
import code.graphics.widgets.*;
import code.user.*;
import code.util.*;



/**  This class is intended specifically to work with the InstallTab class to
  *  enable placement of irregularly-shaped fixtures and venues.
  */
public interface Installation extends Inventory.Owner, Session.Saveable {
  
  int buildCost() ;
  boolean pointsOkay(Tile from, Tile to) ;
  void doPlace(Tile from, Tile to) ;
  void preview(boolean canPlace, Rendering rendering, Tile from, Tile to) ;
  
  String fullName() ;
  Composite portrait(HUD UI) ;
  String helpInfo() ;
  String buildCategory() ;
  
  Index <Upgrade> allUpgrades() ;
  void onCompletion() ;
  void onDestruction() ;
  Structure structure() ;
}
















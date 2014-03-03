

package src.game.building ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;



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
















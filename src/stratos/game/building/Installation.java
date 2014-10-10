

package stratos.game.building;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



/**  This class is intended specifically to work with the InstallTab class to
  *  enable placement of irregularly-shaped fixtures and venues.
  */

//  TODO:  Replace this with a 'FacilityProfile' class, so that the various
//  economic aspects of a structure can be passed with a single object rather
//  than a dozen methods- and can be cached for reference by the base AI.


public interface Installation extends Session.Saveable, Target, Selectable {
  
  int buildCost();
  Base base();

  Index<Upgrade> allUpgrades();
  void onCompletion();
  void onDestruction();
  Structure structure();
  
  boolean setPosition(float x, float y, World world);
  boolean canPlace();
  void doPlacement();
  void previewPlacement(boolean canPlace, Rendering rendering);

  String fullName();
  Composite portrait(BaseUI UI);
  String helpInfo();
  String buildCategory();
}










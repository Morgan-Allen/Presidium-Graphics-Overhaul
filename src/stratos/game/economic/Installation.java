

package stratos.game.economic;
import stratos.game.common.*;
import stratos.game.civilian.*;
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


public interface Installation extends
  Session.Saveable, Target, Selectable, Accountable
{
  
  Base base();
  int buildCost();
  Box2D footprint();
  int owningType();

  Index<Upgrade> allUpgrades();
  void onCompletion();
  void onDestruction();
  Structure structure();
  
  boolean setPosition(float x, float y, Stage world);
  boolean canPlace();
  void previewPlacement(boolean canPlace, Rendering rendering);
  void doPlacement();
}










/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.economic;
import stratos.game.common.*;
import stratos.graphics.common.Rendering;
import stratos.user.*;
import stratos.util.*;



/**  Defines an external interface so that, e.g, vehicles and buildings can
  *  both possess a structure:
  */
public interface Placeable extends
  Session.Saveable, Target, Selectable, Accountable
{
  
  Box2D footprint();
  Structure structure();
  Index <Upgrade> allUpgrades();
  
  boolean setupWith(Tile position, Box2D area, Coord... others);
  void doPlacement(boolean intact);
  void onCompletion();
  void onDestruction();
  
  boolean canPlace(Account reasons);
  void previewPlacement(boolean canPlace, Rendering rendering);
}

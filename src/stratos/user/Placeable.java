

package stratos.user;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.graphics.common.*;
import stratos.util.*;



public interface Placeable extends Structure.Basis {
  
  
  boolean setupWith(Tile position, Box2D area, Coord... others);
  boolean canPlace(Account reasons);
  void previewPlacement(boolean canPlace, Rendering rendering);
}
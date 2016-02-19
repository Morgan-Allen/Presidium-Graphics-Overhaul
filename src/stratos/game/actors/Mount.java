/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.graphics.common.*;
import stratos.util.*;


//  TODO:  Have this extend Boarding for consistency, and merge with Captivity.


public interface Mount extends Session.Saveable, Target {
  
  boolean allowsActivity(Plan activity);
  
  boolean setMounted(Actor mounted, boolean is);
  Property mountStoresAt();
  
  boolean actorVisible(Actor mounted);
  void configureSpriteFrom(Actor mounted, Action a, Sprite s, Rendering r);
  void describeActor(Actor mounted, Description d);
}
/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.graphics.common.*;


public interface Mount extends Session.Saveable, Target {
  
  boolean setMounted(Actor mounted, boolean is);
  Property mountStoresAt();
  
  boolean allowsActivity(Plan activity);
  
  boolean actorVisible(Actor mounted);
  void configureSpriteFrom(Actor mounted, Action action, Sprite actorSprite);
}
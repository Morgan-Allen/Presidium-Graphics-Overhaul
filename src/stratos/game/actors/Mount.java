

package stratos.game.actors;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.graphics.common.*;


public interface Mount extends Session.Saveable, Target {
  
  Actor mounted();
  boolean setMounted(Actor mounted, boolean is);
  
  Property storedAt();
  boolean allowsActivity(Plan activity);
  
  boolean actorVisible(Actor mounted);
  void configureSpriteFrom(Actor mounted, Action action, Sprite actorSprite);
}
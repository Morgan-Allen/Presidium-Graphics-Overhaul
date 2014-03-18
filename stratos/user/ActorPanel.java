

package stratos.user;
import stratos.game.actors.*;
import stratos.graphics.widgets.*;
import stratos.util.*;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;



public class ActorPanel extends InfoPanel {
  
  
  final public static int PORTRAIT_SIZE = 80;
  

  final Composite portrait;
  final UINode portraitFrame;
  //private Texture panelBack
  
  
  public ActorPanel(BaseUI UI, Actor actor, boolean simple) {
    super(UI, actor, PORTRAIT_SIZE + 10) ;
    portrait = actor.portrait(UI);
    portraitFrame = createFrame(UI);
    
    final int PS = PORTRAIT_SIZE;
    portraitFrame.relBound.set(0, 1, 0, 0);
    portraitFrame.absBound.set(5, 5, PS, PS);
    portraitFrame.attachTo(innerRegion);
  }
  
  
  private UINode createFrame(BaseUI UI) {
    return new UINode(UI) {
      protected void render(SpriteBatch batch2d) {
        if (portrait == null) return;
        portrait.drawTo(batch2d, bounds);
      }
    };
  }
}







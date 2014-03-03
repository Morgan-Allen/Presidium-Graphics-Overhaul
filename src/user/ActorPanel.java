

package src.user ;
import src.game.actors.* ;



public class ActorPanel extends InfoPanel {
  
  
  Composite portrait ;
  
  
  public ActorPanel(BaseUI UI, Actor actor, boolean simple) {
    super(UI, actor, InfoPanel.DEFAULT_TOP_MARGIN) ;
    /*
    portrait = actor.portrait(UI) ;
    if (simple) {
      portrait.relBound.set(0, 1, 1, 0) ;
      portrait.absBound.set(10, -110, -20, 100) ;
    }
    else {
      portrait.relBound.set(0, 1, 0, 0) ;
      portrait.absBound.set(10, -110, 100, 100) ;
    }
    portrait.attachTo(this) ;
    //*/
  }
}
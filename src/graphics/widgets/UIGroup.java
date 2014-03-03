/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.graphics.widgets ;
import src.util.* ;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
//import org.lwjgl.opengl.GL11 ;
import com.badlogic.gdx.math.* ;


public class UIGroup extends UINode {
  
  
  final List <UINode> kids = new List <UINode> () ;
  
  
  public UIGroup(HUD myHUD) {
    super(myHUD) ;
    if (myHUD == null && ! (this instanceof HUD)) I.complain("No HUD!") ;
  }
  
  
  public void render(SpriteBatch batch2D) {
    for (UINode kid : kids) if (! kid.hidden) {
      kid.render(batch2D) ;
    }
  }
  
  
  protected UINode selectionAt(Vector2 mousePos) {
    UINode selected = null ;
    for (UINode kid : kids) if (! kid.hidden) {
      final UINode kidSelect = kid.selectionAt(mousePos) ;
      if (kidSelect != null) selected = kidSelect ;
    }
    //  Return children, if possible.
    return selected ;
  }

  
  public void updateAsBase(Box2D bound) {
    updateState() ;
    updateRelativeParent(bound) ;
    updateAbsoluteBounds(bound) ;
  }
  
  
  protected void updateState() {
    super.updateState() ;
    for (UINode kid : kids) kid.updateState() ;
  }
  
  
  void updateRelativeParent(Box2D base) {
    super.updateRelativeParent(base) ;
    for (UINode kid : kids) if (! kid.hidden) kid.updateRelativeParent() ;
  }
  
  
  void updateAbsoluteBounds(Box2D base) {
    super.updateAbsoluteBounds(base) ;
    for (UINode kid : kids) if (! kid.hidden) kid.updateAbsoluteBounds() ;
  }
}






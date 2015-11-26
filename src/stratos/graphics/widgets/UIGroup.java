/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.graphics.widgets;
import stratos.util.*;

import com.badlogic.gdx.math.*;



public class UIGroup extends UINode {
  
  
  /**  Data fields, constructors, and basic access/setter methods-
    */
  final List <UINode> kids = new List <UINode> () {
    protected float queuePriority(UINode r) {
      return 0 - r.relDepth;
    }
  };
  
  
  public UIGroup(HUD myHUD) {
    super(myHUD);
    if (myHUD == null && ! (this instanceof HUD)) I.complain("No HUD!");
  }
  
  
  protected Series <UINode> kids() {
    return kids;
  }
  
  
  
  /**  Overrides for general UI functions-
    */
  protected void render(WidgetsPass pass) {
    for (UINode kid : kids) if (! kid.hidden) {
      kid.render(pass);
    }
  }
  
  
  protected UINode selectionAt(Vector2 mousePos) {
    UINode selected = null;
    for (UINode kid : kids) if (! kid.hidden) {
      final UINode kidSelect = kid.selectionAt(mousePos);
      if (kidSelect != null) selected = kidSelect;
    }
    //  Return children, if possible.
    return selected;
  }

  
  public void updateAsBase(Box2D bound) {
    updateState();
    updateRelativeParent(bound);
    updateAbsoluteBounds(bound);
  }
  
  
  protected void updateState() {
    super.updateState();
    for (UINode kid : kids) kid.updateState();
  }
  
  
  void updateRelativeParent(Box2D base) {
    super.updateRelativeParent(base);
    for (UINode kid : kids) if (! kid.hidden) kid.updateRelativeParent();
  }
  
  
  void updateAbsoluteBounds(Box2D base) {
    super.updateAbsoluteBounds(base);
    for (UINode kid : kids) if (! kid.hidden) kid.updateAbsoluteBounds();
  }
  
  
  
  /**  Other utility methods related to display-
    */
  protected void sortKidsByDepth() {
    kids.queueSort();
  }
}












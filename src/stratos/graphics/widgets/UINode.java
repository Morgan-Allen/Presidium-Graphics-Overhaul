/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.graphics.widgets;
import stratos.graphics.common.*;
import stratos.util.*;

import com.badlogic.gdx.math.*;
import org.apache.commons.math3.util.FastMath;



public abstract class UINode {
  
  
  /**  Data fields, constructors and accessor methods-
    */
  final public static byte
    HOVERED = 0,
    CLICKED = 1,
    PRESSED = 2,
    DRAGGED = 3;
  final public static float
    DEFAULT_FADE_TIME = 0.25f,
    DEFAULT_FADE_INC = 1f / (DEFAULT_FADE_TIME * Rendering.FRAMES_PER_SECOND),
    
    DEFAULT_HOVER_ALPHA = 0.5f,
    DEFAULT_PRESS_ALPHA = 0.75f;
  
  final public Box2D
    relBound = new Box2D(),
    absBound = new Box2D();
  public float
    relDepth = 0,
    absDepth = 0;
  final protected Box2D
    bounds = new Box2D();
  public float
    relAlpha = 1,
    absAlpha = 1;
  public boolean
    hidden  = false,
    stretch = true;
  
  
  final protected HUD UI;
  private UIGroup parent;
  private ListEntry <UINode> kidEntry;
  
  
  public UINode(HUD myHUD) {
    this.UI = myHUD ;
  }
  
  
  protected UINode selectionAt(Vector2 mousePos) {
    return (bounds.contains(mousePos.x, mousePos.y)) ? this : null;
  }
  
  
  public float xpos() { return bounds.xpos() ; }
  public float ypos() { return bounds.ypos() ; }
  public float xdim() { return bounds.xdim() ; }
  public float ydim() { return bounds.ydim() ; }
  public Box2D trueBounds() { return bounds ; }
  
  
  
  /**  Methods intended for implementation by subclasses-
    */
  protected abstract void render(WidgetsPass pass);
  protected String info() { return null ; }
  
  
  
  /**  Methods to control the order of rendering for UINodes.
    */
  public void attachTo(UIGroup group) {
    detach() ;
    parent = group ;
    kidEntry = parent.kids.addLast(this) ;
  }
  
  
  public void detach() {
    if (parent == null) return ;
    parent.kids.removeEntry(kidEntry) ;
    kidEntry = null ;
    parent = null ;
  }
  
  
  protected void updateState() {
    absDepth = relDepth + (parent == null ? 0 : parent.absDepth) ;
    absAlpha = relAlpha * (parent == null ? 1 : parent.absAlpha) ;
  }
  
  
  
  /**  Sets the absolute size and relative (to parent) position of this node.
    */
  protected void updateRelativeParent() {
    if (parent == null) updateRelativeParent(new Box2D()) ;
    else updateRelativeParent(parent.bounds) ;
  }
  
  
  protected void updateAbsoluteBounds() {
    if (parent == null) updateAbsoluteBounds(new Box2D()) ;
    else {
      updateAbsoluteBounds(parent.bounds) ;
    }
  }
  
  
  void updateRelativeParent(Box2D base) {
    
    //boolean stretch = true;
    float
      wide = absBound.xdim() + (base.xdim() * relBound.xdim()),
      high = absBound.ydim() + (base.ydim() * relBound.ydim()),
      x = absBound.xpos(),
      y = absBound.ypos();
    
    if (! stretch) {
      //  In this case we shrink either width or height to maintain a constant
      //  aspect ratio.
      final float
        oldWide = wide, oldHigh = high,
        aspW  = wide / absBound.xdim(),
        aspH  = high / absBound.ydim(),
        scale = FastMath.max(aspW, aspH);
      wide = absBound.xdim() * scale;
      high = absBound.ydim() * scale;
      x += (oldWide - wide) / 2;
      y += (oldHigh - high) / 2;
    }
    
    bounds.xdim(wide);
    bounds.ydim(high);
    bounds.xpos(x);
    bounds.ypos(y);
  }
  
  
  void updateAbsoluteBounds(Box2D base) {
    final float
      x = bounds.xpos() + base.xpos() + (relBound.xpos() * base.xdim()),
      y = bounds.ypos() + base.ypos() + (relBound.ypos() * base.ydim());
    bounds.xpos(x);
    bounds.ypos(y);
  }
  
  
  
  /**  Blank feedback methods for override by subclasses-
    */
  protected void whenHovered() {}
  protected void whenClicked() {}
  protected void whenPressed() {}
  protected void whenDragged() {}
  protected boolean amHovered() { return UI.amSelected(this, HOVERED) ; }
  protected boolean amClicked() { return UI.amSelected(this, CLICKED) ; }
  protected boolean amPressed() { return UI.amSelected(this, PRESSED) ; }
  protected boolean amDragged() { return UI.amSelected(this, DRAGGED) ; }
}














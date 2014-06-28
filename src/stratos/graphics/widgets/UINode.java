/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.graphics.widgets;
import stratos.graphics.common.Rendering;
import stratos.util.*;

import com.badlogic.gdx.math.* ;



public abstract class UINode {
  
  
  /**  Data fields, constructors and accessor methods-
    */
  final public static byte
    HOVERED = 0,
    CLICKED = 1,
    PRESSED = 2,
    DRAGGED = 3 ;
  final public static float
    DEFAULT_FADE_TIME = 0.25f,
    DEFAULT_FADE_INC = 1f / (DEFAULT_FADE_TIME * Rendering.FRAMES_PER_SECOND),
    
    DEFAULT_HOVER_ALPHA = 0.5f,
    DEFAULT_PRESS_ALPHA = 0.75f;
  
  final public Box2D
    relBound = new Box2D(),
    absBound = new Box2D() ;
  public float
    relDepth = 0,
    absDepth = 0 ;
  final protected Box2D
    bounds = new Box2D() ;
  public float
    relAlpha = 1,
    absAlpha = 1 ;
  public boolean
    hidden = false ;
  
  
  final protected HUD UI ;
  private UIGroup parent ;
  private ListEntry <UINode> kidEntry ;
  
  
  public UINode(HUD myHUD) {
    this.UI = myHUD ;
  }
  
  
  protected UINode selectionAt(Vector2 mousePos) {
    return (bounds.contains(mousePos.x, mousePos.y)) ? this : null ;
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
  
  
  void updateRelativeParent(Box2D base) {
    bounds.xdim(absBound.xdim() + (base.xdim() * relBound.xdim())) ;
    bounds.ydim(absBound.ydim() + (base.ydim() * relBound.ydim())) ;
    bounds.xpos(absBound.xpos()) ;
    bounds.ypos(absBound.ypos()) ;
  }
  
  
  
  /**  Sets the absolute position and bounds of this node.
    */
  protected void updateAbsoluteBounds() {
    if (parent == null) updateAbsoluteBounds(new Box2D()) ;
    else {
      updateAbsoluteBounds(parent.bounds) ;
    }
  }
  
  
  void updateAbsoluteBounds(Box2D base) {
    bounds.xpos(bounds.xpos() + base.xpos() + (relBound.xpos() * base.xdim())) ;
    bounds.ypos(bounds.ypos() + base.ypos() + (relBound.ypos() * base.ydim())) ;
  }
  
  
  protected void whenHovered() {}
  protected void whenClicked() {}
  protected void whenPressed() {}
  protected void whenDragged() {}
  protected boolean amHovered() { return UI.amSelected(this, HOVERED) ; }
  protected boolean amClicked() { return UI.amSelected(this, CLICKED) ; }
  protected boolean amPressed() { return UI.amSelected(this, PRESSED) ; }
  protected boolean amDragged() { return UI.amSelected(this, DRAGGED) ; }
}














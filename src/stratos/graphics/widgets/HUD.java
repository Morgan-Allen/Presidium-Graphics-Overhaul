/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.graphics.widgets;
import stratos.graphics.common.*;
import stratos.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.g2d.*;



/**  This is the 'master' UI class.
  */
public class HUD extends UIGroup {
  
  
  
  final private static float
    DRAG_DIST = 3.0f,
    HOVER_DELAY = 0.5f ;
  
  
  final public Rendering rendering;
  
  private long
    hoverStart = -1 ;
  private Vector2
    nextMP = new Vector2(),
    dragMP = new Vector2() ;
  private boolean
    nextMB ;
  
  private UINode
    selected ;
  private boolean
    mouseB ;
  private byte
    mouseState = HOVERED ;
  private Vector2
    mousePos = new Vector2() ;
  
  
  public HUD(Rendering rendering) {
    super(null) ;
    this.rendering = rendering;
  }
  
  
  public void updateInput() {
    nextMB = Gdx.input.isButtonPressed(Buttons.LEFT);
    nextMP.x = Gdx.input.getX();
    nextMP.y = Gdx.graphics.getHeight() - Gdx.input.getY();
    
    if (mouseB && (! nextMB)) {
      mouseState = HOVERED ;
    }
    if ((! mouseB) && nextMB) {
      mouseState = CLICKED ;
      dragMP.set(nextMP);
    }
    if (mouseB && nextMB && (mouseState != DRAGGED)) {
      mouseState = (dragMP.dst(nextMP) > DRAG_DIST) ? DRAGGED : PRESSED ;
    }
    mousePos.set(nextMP) ;
    mouseB = nextMB ;
    //  TODO:  Perform the selection mechanics here?
  }
  
  
  //  This is used for two-dimensional GUI elements in the conventional drawing
  //  hierarchy.
  public void renderHUD(Rendering rendering) {
    relBound.set(0, 0, 1, 1) ;
    absBound.set(0, 0, 0, 0) ;
    final Box2D size = new Box2D();
    size.xdim(Gdx.graphics.getWidth());
    size.ydim(Gdx.graphics.getHeight());
    updateAsBase(size) ;
    
    final UINode oldSelect = selected ;
    if ((selected == null) || (mouseState != DRAGGED)) {
      selected = selectionAt(mousePos) ;
    }
    if (mouseState != HOVERED) {
      //hoverStart = System.currentTimeMillis() ;
    }
    else if (selected != null && selected != oldSelect) {
      hoverStart = System.currentTimeMillis() ;
    }
    if (selected != null) switch (mouseState) {
      case (HOVERED) : selected.whenHovered() ; break ;
      case (CLICKED) : selected.whenClicked() ; break ;
      case (PRESSED) : selected.whenPressed() ; break ;
      case (DRAGGED) : selected.whenDragged() ; break ;
    }
    
    super.render(rendering.batch2D);
  }
  
  
  
  public boolean amSelected(UINode node, byte state) {
    return (selected == node) && (mouseState == state) ;
  }
  
  
  public float timeHovered() {
    //if (mouseState != HOVERED) return -1 ;
    final long time = System.currentTimeMillis() - hoverStart ;
    return time / 1000f ;
  }
  
  
  
  public Vector2 mousePos() { return mousePos ; }
  public Vector2 dragOrigin() { return dragMP ; }
  
  public int mouseX() { return (int) mousePos.x ; }
  public int mouseY() { return (int) mousePos.y ; }
  
  
  public boolean mouseDown() { return mouseB ;  }
  public boolean mouseClicked() { return isMouseState(CLICKED) ; }
  public boolean mouseHovered() { return isMouseState(HOVERED) ; }
  public boolean mouseDragged() { return isMouseState(DRAGGED) ; }
  public boolean mousePressed() { return isMouseState(PRESSED) ; }
  
  
  public boolean isMouseState(final byte state) {
    return mouseState == state ;
  }
  
  
  public UINode selected() {
    return selected ;
  }
  
  
  public Box2D screenBounds() {
    return bounds ;
  }
}



/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package graphics.widgets ;
import util.* ;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.math.* ;
import com.badlogic.gdx.graphics.g2d.*;



/*
//  TODO:  Use this to perform scissor clipping around text fields, et cetera-

Rectangle scissors = new Rectangle();
Rectangle clipBounds = new Rectangle(x,y,w,h);
ScissorStack.calculateScissors(
    camera, spriteBatch.getTransformMatrix(), clipBounds, scissors
);
ScissorStack.pushScissors(scissors);
spriteBatch.draw(...);
spriteBatch.flush();
ScissorStack.popScissors();
This will limit rendering to within the bounds of the rectangle "clipBounds".

It is also possible push multiple rectangles. Only the pixels of the sprites
that are within all of the rectangles will be rendered.
//*/

//  Alternatively, this apparently works-
/*
Gdx.gl.glEnable(GL10.GL_SCISSOR_TEST);
Gdx.gl.glScissor(clipX, clipY, clipWidth, clipHeight);

batch.begin();
//draw sprites to be clipped
batch.draw(sprite, 0, 0, 250, 250);
batch.end();

Gdx.gl.glDisable(GL10.GL_SCISSOR_TEST);
//*/



/**  This is the 'master' UI class.
  */
public class HUD extends UIGroup {
  
  
  
  final private static float
    DRAG_DIST = 3.0f,
    HOVER_DELAY = 0.5f ;
  
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

  
  final private SpriteBatch batch2D ;
  
  
  public HUD() {
    super(null) ;
    batch2D = new SpriteBatch();
  }
  
  
  public void updateMouse() {
    nextMB = Gdx.input.isButtonPressed(Buttons.LEFT);
    nextMP.x = Gdx.input.getX() ;
    nextMP.y = Gdx.graphics.getHeight() - Gdx.input.getY() ;
    
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
  }
  
  //
  //  This is used for rendering GUI elements that share the viewport transform,
  //  but are superimposed on top of actual world-sprites.
  public void renderWorldFX() {} ;
  
  //
  //  This is used for two-dimensional GUI elements in the conventional drawing
  //  hierarchy.
  public void renderHUD() {
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
    
    /*
    GL11.glMatrixMode(GL11.GL_PROJECTION) ;
    GL11.glLoadIdentity() ;
    GL11.glOrtho(
      0, bounds.xdim(),
      0, bounds.ydim(),
      -100, 100
    ) ;
    GL11.glMatrixMode(GL11.GL_MODELVIEW) ;
    GL11.glLoadIdentity() ;
    GL11.glDisable(GL11.GL_LIGHTING) ;
    GL11.glDisable(GL11.GL_CULL_FACE) ;
    GL11.glDisable(GL11.GL_DEPTH_TEST) ;
    //*/
    batch2D.begin();
    super.render(batch2D);
    batch2D.end();
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



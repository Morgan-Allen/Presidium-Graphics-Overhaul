/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package code.graphics.widgets ;
import code.graphics.common.*;
import code.util.*;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.*;



/**  Implements a scrollbar for use by other GUI elements.  Note that this
  *  object keeps track of the 'mapped' area by maintaining a direct reference
  *  to the Box2D object passed to it.
  */
public class Scrollbar extends UINode {
  

  final public static float
    DEFAULT_SCROLL_WIDTH = 10,  //default width for a scrollbar.
    DEFAULT_TAB_HEIGHT = 25,  //default size of 'rounded edge' for grab-widget.
    DEFAULT_TAB_UV = 0.25f,  //default UV portion for that rounded edge.
    MAX_GRAB_PORTION = 0.5f,  //max area the grab-widget will occupy.
    MIN_SCROLL_DIST = 5 ;
  
  
  final Box2D mapArea ;
  private float scrollPos = 1, initScrollPos = -1 ;
  
  final Texture scrollTex ;
  final private Box2D grabArea = new Box2D() ;
  private boolean showScroll = false ;
  
  
  
  protected Scrollbar(HUD myHUD, Texture tex, Box2D mapArea) {
    super(myHUD) ;
    this.mapArea = mapArea ;
    this.scrollTex = tex ;
  }
  
  
  protected float scrollPos() {
    return scrollPos ;
  }
  
  
  private float mapRatio() {
    return ydim() / mapArea.ydim() ;
  }
  
  
  protected void updateAbsoluteBounds() {
    super.updateAbsoluteBounds() ;
    grabArea.setTo(bounds) ;
    final float mapRatio = mapRatio() ;
    if (mapRatio < 1) {
      final float grabSize = Math.min(MAX_GRAB_PORTION, mapRatio) ;
      showScroll = true ;
      final float offset = scrollPos * (1 - grabSize) ;
      grabArea.ydim(ydim() * grabSize) ;
      grabArea.ypos(ypos() + (ydim() * offset)) ;
    }
    else showScroll = false ;
  }
  
  
  protected void whenClicked() {
    if (! showScroll) return ;
    final float mX = UI.mouseX(), mY = UI.mouseY() ;
    if (grabArea.contains(mX, mY)) {
      initScrollPos = scrollPos ;
    }
    else {
      initScrollPos = -1 ;
      final float inc = Math.max(
        MIN_SCROLL_DIST / (mapArea.ydim() - ydim()), 0.1f
      ) ;
      if (mY > grabArea.ymax()) scrollPos += inc ;
      if (mY < grabArea.ypos()) scrollPos -= inc ;
      if (scrollPos < 0) scrollPos = 0 ;
      if (scrollPos > 1) scrollPos = 1 ;
    }
  }
  
  
  protected void whenPressed() {
    whenDragged() ;
  }
  
  
  protected void whenDragged() {
    if (initScrollPos == -1 || ! showScroll) return ;
    final Vector2 mP = UI.mousePos(), dP = UI.dragOrigin() ;
    final float mapRatio = Math.min(mapRatio(), MAX_GRAB_PORTION) ;
    final float stretch = (mP.y - dP.y) / (ydim() * (1 - mapRatio)) ;
    scrollPos = initScrollPos + stretch ;
    if (scrollPos < 0) scrollPos = 0 ;
    if (scrollPos > 1) scrollPos = 1 ;
  }
  
  
  protected void render(SpriteBatch batch2D) {
    if (! showScroll) return ;
    final float
      x = grabArea.xpos(), y = grabArea.ypos(),
      w = grabArea.xdim(), h = grabArea.ydim();
    
    batch2D.setColor(1, 1, 1, 1);
    batch2D.draw(
      scrollTex,
      x, y, w, DEFAULT_TAB_HEIGHT,
      0, DEFAULT_TAB_UV, 1, 0
    );
    batch2D.draw(
      scrollTex,
      x, y + DEFAULT_TAB_HEIGHT, w, h - (DEFAULT_TAB_HEIGHT * 2),
      0, DEFAULT_TAB_UV, 1, 0.5f
    );
    batch2D.draw(
      scrollTex,
      x, y + h - DEFAULT_TAB_HEIGHT, w, DEFAULT_TAB_HEIGHT,
      0, 0, 1, DEFAULT_TAB_UV
    );
  }
}



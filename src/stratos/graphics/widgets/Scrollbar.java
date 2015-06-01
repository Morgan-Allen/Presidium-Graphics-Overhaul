/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.graphics.widgets;
import stratos.graphics.common.*;
import stratos.util.*;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.*;



/**  Implements a scrollbar for use by other GUI elements.  Note that this
  *  object keeps track of the 'mapped' area by maintaining a direct reference
  *  to the Box2D object passed to it.
  */
public class Scrollbar extends UINode {
  
  
  final public static float
    DEFAULT_SCROLL_WIDTH = 10   ,  //default width for a scrollbar.
    MINIMUM_GRAB_HEIGHT  = 50   ,  //minimum height of grab area.
    DEFAULT_TAB_HEIGHT   = 25   ,  //default size of 'rounded edge' for grabber.
    DEFAULT_TAB_UV       = 0.25f,  //default UV portion for that rounded edge.
    MAX_GRAB_PORTION     = 0.5f ,  //max area the grab-widget will occupy.
    MIN_SCROLL_DIST      = 5    ;
  
  
  final Texture scrollTex;
  final private Box2D grabArea = new Box2D();
  
  final Text tracked;
  private float
    scrollPos     =  1,
    initScrollPos = -1;
  private boolean showScroll = false;
  
  
  protected Scrollbar(HUD myHUD, ImageAsset tex, Text tracked) {
    super(myHUD);
    this.tracked = tracked;
    this.scrollTex = tex.asTexture();
  }
  
  
  public float scrollPos() {
    return scrollPos;
  }
  
  
  public void setScrollPos(float pos) {
    this.scrollPos = pos;
  }
  
  
  private float mapRatio() {
    return tracked.ydim() / tracked.fullTextArea().ydim();
  }
  
  
  protected void updateAbsoluteBounds() {
    super.updateAbsoluteBounds();
    grabArea.setTo(bounds);
    final float mapRatio = mapRatio();
    if (mapRatio < 1) {
      showScroll = true;
      float grabSize = Nums.min(MAX_GRAB_PORTION, mapRatio);
      grabSize = Nums.max(grabSize, MINIMUM_GRAB_HEIGHT / ydim());
      final float offset = scrollPos * (1 - grabSize);
      grabArea.ydim(ydim() * grabSize);
      grabArea.ypos(ypos() + (ydim() * offset));
    }
    else showScroll = false;
  }
  
  
  protected void whenClicked() {
    if (! showScroll) return;
    final float mX = UI.mouseX(), mY = UI.mouseY();
    if (grabArea.contains(mX, mY)) {
      initScrollPos = scrollPos;
    }
    else {
      initScrollPos = -1;
      final float scrollGap = tracked.fullTextArea().ydim() - tracked.ydim();
      final float inc = Nums.max(MIN_SCROLL_DIST / scrollGap, 0.1f);
      if (mY > grabArea.ymax()) scrollPos += inc;
      if (mY < grabArea.ypos()) scrollPos -= inc;
      if (scrollPos < 0) scrollPos = 0;
      if (scrollPos > 1) scrollPos = 1;
    }
  }
  
  
  protected void whenPressed() {
    whenDragged();
  }
  
  
  protected void whenDragged() {
    if (initScrollPos == -1 || ! showScroll) return;
    final Vector2 mP = UI.mousePos(), dP = UI.dragOrigin();
    final float stretch = (mP.y - dP.y) / (ydim() - grabArea.ydim());
    scrollPos = initScrollPos + stretch;
    if (scrollPos < 0) scrollPos = 0;
    if (scrollPos > 1) scrollPos = 1;
  }
  
  
  protected void render(WidgetsPass pass) {
    if (! showScroll) return;
    final int
      side = (int) (grabArea.xdim() / 2),
      cap  = (int) DEFAULT_TAB_HEIGHT;
    
    Bordering.renderBorder(
      pass, grabArea,
      side, side, cap, cap,
      0.5f, 0.5f, 0.25f, 0.25f,
      scrollTex, Colour.transparency(relAlpha)
    );
  }
}



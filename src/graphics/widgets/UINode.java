/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package graphics.widgets ;
//import src.graphics.common.Texture;
import util.* ;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.* ;



public abstract class UINode {
  
  
  /**  Data fields, constructors and accessor methods-
    */
  final public static byte
    HOVERED = 0,
    CLICKED = 1,
    PRESSED = 2,
    DRAGGED = 3 ;
  
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
  
  
  final protected HUD myHUD ;
  private UIGroup parent ;
  private ListEntry <UINode> kidEntry ;
  
  
  public UINode(HUD myHUD) {
    this.myHUD = myHUD ;
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
  protected abstract void render(SpriteBatch batch2D) ;
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
  protected boolean amHovered() { return myHUD.amSelected(this, HOVERED) ; }
  protected boolean amClicked() { return myHUD.amSelected(this, CLICKED) ; }
  protected boolean amPressed() { return myHUD.amSelected(this, PRESSED) ; }
  protected boolean amDragged() { return myHUD.amSelected(this, DRAGGED) ; }
  
  
  
  /**  Utility methods for drawing/graphic display:
    */
  static Texture loadTexture(String name) {
    Texture cached = (Texture) LoadService.getResource(name);
    if (cached != null) return cached;
    cached = new Texture(Gdx.files.internal(name));
    LoadService.cacheResource(cached, name);
    return cached;
  }
  
  
  /*
  final public static void drawQuad(
    float xmin, float ymin,
    float xmax, float ymax,
    float umin, float vmin,
    float umax, float vmax,
    float absDepth
  ) {
    GL11.glTexCoord2f(umin, vmax) ;
    GL11.glVertex3f(xmin, ymin, absDepth) ;
    GL11.glTexCoord2f(umin, vmin) ;
    GL11.glVertex3f(xmin, ymax, absDepth) ;
    GL11.glTexCoord2f(umax, vmin) ;
    GL11.glVertex3f(xmax, ymax, absDepth) ;
    GL11.glTexCoord2f(umax, vmax) ;
    GL11.glVertex3f(xmax, ymin, absDepth) ;
  }
  
  
  //
  //  TODO:  Consider returning a Texture instead, or just a GL texture ID?
  final public static ByteBuffer copyPixels(Box2D area, ByteBuffer pixels) {
    final int size = ((int) area.xdim()) * ((int) area.ydim()) * 4 ;
    if (pixels == null || pixels.capacity() != size) {
      pixels = BufferUtils.createByteBuffer(size) ;
    }
    else pixels.rewind() ;
    GL11.glReadPixels(
      (int) area.xpos(), (int) area.ypos(),
      (int) area.xdim(), (int) area.ydim(),
      GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels
    ) ;
    return pixels ;
  }
  

  private static int glID ;
  private static boolean cached = false ;
  
  final public static void drawPixels(Box2D area, ByteBuffer pixels) {
    if (! cached) {
      final IntBuffer tmpID = BufferUtils.createIntBuffer(1) ;
      GL11.glGenTextures(tmpID) ;
      glID = tmpID.get(0) ;
      cached = true ;
    }
    
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, glID) ;
    Texture.setDefaultTexParams(GL11.GL_TEXTURE_2D) ;
    GL11.glTexImage2D(
      GL11.GL_TEXTURE_2D,
      0, 4,
      (int) area.xdim(), (int) area.ydim(), 0,
      GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE,
      pixels
    ) ;
    
    GL11.glBegin(GL11.GL_QUADS) ;
    UINode.drawQuad(
      (int) area.xpos(), (int) area.ypos(),
      (int) area.xmax(), (int) area.ymax(), 0, 1, 1, 0, 0
    ) ;
    GL11.glEnd() ;
  }
  //*/
  
}














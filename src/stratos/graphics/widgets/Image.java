/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.graphics.widgets;
import stratos.graphics.common.*;
import stratos.util.*;
import static stratos.graphics.common.Colour.*;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.Vector2;




/**  Note- images set their own dimensions to match that of their texture (
  *  times scale.)  If you wish to disable this behaviour, set scale to zero.
  */
public class Image extends UINode {
  
  
  /**  Data fields, constructors and setup methods-
    */
  final public static ImageAsset
    TRANSLUCENT_WHITE = ImageAsset.withColor(15, SOFT_WHITE, Image.class),
    TRANSLUCENT_GREY  = ImageAsset.withColor(16, SOFT_GREY , Image.class),
    TRANSLUCENT_BLACK = ImageAsset.withColor(16, SOFT_BLACK, Image.class),
    SOLID_WHITE       = ImageAsset.withColor(16, WHITE     , Image.class),
    SOLID_BLACK       = ImageAsset.withColor(16, BLACK     , Image.class),
    FULL_TRANSPARENCY = ImageAsset.withColor(16, NONE      , Image.class);
  
  public boolean
    lockToPixels = false,
    blocksSelect = false,
    enabled      = true ;
  
  protected Texture texture;
  protected Texture greyOut = TRANSLUCENT_GREY.asTexture();
  protected Batch <Texture> overlaid = null;
  
  
  public Image(HUD UI, String imagePath) {
    super(UI);
    texture = ImageAsset.getTexture(imagePath);
  }
  
  
  public Image(HUD myHUD, ImageAsset tex) {
    this(myHUD, tex.asTexture());
  }
  
  
  public Image(HUD myHUD, Texture t) {
    super(myHUD);
    this.texture = t;
  }
  
  
  public void setBaseTexture(Texture t) {
    this.texture = t;
  }
  
  
  public void setDisabledOverlay(ImageAsset g) {
    this.greyOut = g.asTexture();
  }
  
  
  public void addOverlay(ImageAsset g) {
    if (overlaid == null) overlaid = new Batch();
    overlaid.add(g.asTexture());
  }
  
  
  public void setToPreferredSize() {
    expandToTexSize(1, false);
  }
  
  
  public int texWide() {
    return texture.getWidth();
  }
  
  
  public int texHigh() {
    return texture.getHeight();
  }
  
  
  public void expandToTexSize(float scale, boolean centre) {
    absBound.xdim(texture.getWidth()  * scale);
    absBound.ydim(texture.getHeight() * scale);
    if (centre) {
      absBound.xpos(absBound.xpos() - absBound.xdim() / 2);
      absBound.ypos(absBound.ypos() - absBound.ydim() / 2);
    }
  }
  
  
  /**  Rendering and feedback methods-
    */
  protected UINode selectionAt(Vector2 mousePos) {
    if (blocksSelect) return super.selectionAt(mousePos);
    else return null;
  }
  

  protected void setUnstretched(
    float x, float y, float wide, float high, Box2D bounds
  ) {
    final float
      texWide = texture.getWidth (),
      texHigh = texture.getHeight(),
      scale   = Nums.min(wide / texWide, high / texHigh);
    bounds.set(x, y, texWide * scale, texHigh * scale);
  }
  
  
  protected void render(WidgetsPass pass) {
    renderTex(texture, absAlpha, pass);
    if (! enabled) renderTex(greyOut, absAlpha, pass);
    if (overlaid != null) for (Texture t : overlaid) {
      renderTex(t, absAlpha, pass);
    }
  }
  
  
  protected void renderTex(Texture tex, float alpha, WidgetsPass pass) {
    final Box2D drawn = new Box2D(bounds);
    if (lockToPixels) {
      drawn.xpos((int) drawn.xpos());
      drawn.ypos((int) drawn.ypos());
      drawn.xdim((int) drawn.xdim());
      drawn.ydim((int) drawn.ydim()); 
    }
    pass.draw(
      tex, Colour.transparency(alpha),
      drawn.xpos(), drawn.ypos(), drawn.xdim(), drawn.ydim(),
      0.0f, 1.0f, 1.0f, 0.0f
    );
  }
}





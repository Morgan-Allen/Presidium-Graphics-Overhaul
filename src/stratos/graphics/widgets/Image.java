/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.graphics.widgets;
import stratos.graphics.common.*;
import stratos.start.PlayLoop;
import stratos.util.*;
import com.badlogic.gdx.graphics.*;



/**  Not- images set their own dimensions to match that of their texture (times
  *  scale.)  If you wish to disable this behaviour, set scale to zero.
  */
public class Image extends UINode {
  
  
  public boolean stretch = false ;
  public boolean lockToPixels = false;
  protected Texture texture;
  
  
  public Image(HUD UI, String imagePath) {
    super(UI);
    if (! PlayLoop.onRenderThread()) I.complain("ONLY ON RENDER THREAD!");
    texture = ImageAsset.getTexture(imagePath);
  }
  
  
  public Image(HUD myHUD, ImageAsset tex) {
    this(myHUD, tex.asTexture()) ;
  }
  
  
  public Image(HUD myHUD, Texture t) {
    super(myHUD) ;
    texture = t ;
  }
  
  
  public void expandToTexSize(float scale, boolean centre) {
    absBound.xdim(texture.getWidth()  * scale);
    absBound.ydim(texture.getHeight() * scale);
    if (centre) {
      absBound.xpos(absBound.xpos() - absBound.xdim() / 2);
      absBound.ypos(absBound.ypos() - absBound.ydim() / 2);
    }
  }
  
  
  protected void render(WidgetsPass pass) {
    renderTex(texture, relAlpha, pass);
  }
  
  
  protected void renderTex(Texture tex, float alpha, WidgetsPass pass) {
    final float scale = stretch ? 1 : Math.min(
      bounds.xdim() / texture.getWidth(),
      bounds.ydim() / texture.getHeight()
    );
    final Box2D drawn = new Box2D().set(
      bounds.xpos(), bounds.ypos(),
      stretch ? bounds.xdim() : (texture.getWidth()  * scale),
      stretch ? bounds.ydim() : (texture.getHeight() * scale)
    );
    if (! stretch) {
      final float
        gapX = bounds.xdim() - drawn.xdim(),
        gapY = bounds.ydim() - drawn.ydim();
      drawn.xpos(drawn.xpos() + (gapX / 2));
      drawn.ypos(drawn.ypos() + (gapY / 2));
    }
    
    if (lockToPixels) {
      drawn.xpos((int) drawn.xpos()); 
      drawn.ypos((int) drawn.ypos()); 
      drawn.xdim((int) drawn.xdim()); 
      drawn.ydim((int) drawn.ydim()); 
    }
    
    pass.setColor(1, 1, 1, alpha);
    pass.draw(
      tex,
      drawn.xpos(), drawn.ypos(), drawn.xdim(), drawn.ydim(),
      0.0f, 1.0f, 1.0f, 0.0f
    );
  }
}





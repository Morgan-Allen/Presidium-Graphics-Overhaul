/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package graphics.widgets ;
import util.* ;
import graphics.common.* ;
//import org.lwjgl.opengl.* ;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.*;



/**  Not- images set their own dimensions to match that of their texture (times
  *  scale.)  If you wish to disable this behaviour, set scale to zero.
  */
public class Image extends UINode {
  
  
  public boolean stretch = false ;
  protected Texture texture ;
  
  
  public Image(HUD myHUD, String textureName) {
    this(myHUD, loadTexture(textureName)) ;
  }
  
  
  public Image(HUD myHUD, Texture t) {
    super(myHUD) ;
    texture = t ;
  }
  
  
  protected void render(SpriteBatch batch2D) {
    renderTex(texture, 1, batch2D);
  }
  
  
  protected void renderTex(Texture tex, float alpha, SpriteBatch batch2D) {
    final float scale = stretch ? 1 : Math.min(
      bounds.xdim() / texture.getWidth() ,
      bounds.ydim() / texture.getHeight()
    );
    final Box2D drawn = new Box2D().set(
      bounds.xpos(), bounds.ypos(),
      stretch ? bounds.xdim() : (texture.getWidth()  * scale),
      stretch ? bounds.ydim() : (texture.getHeight() * scale)
    );
    batch2D.setColor(1, 1, 1, alpha);
    batch2D.draw(
      tex,
      drawn.xpos(), drawn.ypos(), drawn.xdim(), drawn.ydim(),
      0.0f, 0.0f, 1.0f, 1.0f
    );
  }
}



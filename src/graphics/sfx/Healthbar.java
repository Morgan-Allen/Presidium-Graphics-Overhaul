

package src.graphics.sfx ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;
import src.util.*;
import java.io.* ;
import org.lwjgl.opengl.* ;



public class Healthbar extends Sprite {
  
  
  
  /**  Constants, field definitions, constructors and save/load methods-
    */
  final public static ModelAsset
    BAR_MODEL = new ModelAsset("health_bar_model", Healthbar.class) {
      public boolean isLoaded() { return true ; }
      protected void loadAsset() {}
      protected void disposeAsset() {}
      public Sprite makeSprite() { return new Healthbar() ; }
    } ;
  
  final public static int
    BAR_HEIGHT = 3,
    DEFAULT_WIDTH = 40 ;
  
  public float level = 0.5f, yoff = 0 ;
  public float size = DEFAULT_WIDTH ;
  public Colour full = Colour.BLUE, empty = Colour.RED ;
  
  private float flash = 0 ;
  
  
  public Healthbar() {}
  public ModelAsset model() { return BAR_MODEL ; }
  
  
  
  /**  Updates and rendering-
    */
  public void update() {
  }
  
  
  public void setAnimation(String animName, float progress) {}
  public int[] GL_disables() { return null ; }
  
  
  public void registerFor(Rendering rendering) {
    /*
    //
    //  First, establish screen coordinates for the bottom-left corner.
    final Vec3D base = new Vec3D().setTo(position) ;
    rendering.view.translateToScreen(base) ;
    final int
      x = (int) (base.x - (size / 2)),
      y = (int) (base.y + yoff - (BAR_HEIGHT / 2)) ;
    //
    //  Begin actual rendering-
    rendering.view.setScreenMode() ;
    Texture.WHITE_TEX.bindTex() ;
    GL11.glBegin(GL11.GL_QUADS) ;
    //
    //  First, do the background-
    final Colour a = full, b = empty ;
    final Colour c = colour == null ? Colour.WHITE : colour ;
    final float s = 1 - level, f = fog ;
    GL11.glColor4f(
      0.25f * f * c.r,
      0.25f * f * c.g,
      0.25f * f * c.b,
      1 * c.a
    ) ;
    UINode.drawQuad(
      x, y, x + (int) size, y + BAR_HEIGHT,
      0, 0, 1, 1, base.z
    ) ;
    //
    //  When at less than half health, you need to flash-
    if (level < 0.5f) {
      float flashAlpha = 0 ;
      flashAlpha = (0.5f - level) * 2 ;
      flash += 0.04f * Math.PI / 2f ;
      flashAlpha *= f * c.a * Math.abs(Math.sin(flash)) ;
      GL11.glColor4f(1, 0, 0, flashAlpha) ;
      UINode.drawQuad(
        x, y, x + (int) size, y + BAR_HEIGHT,
        0, 0, 1, 1, base.z
      ) ;
    }
    //
    //  Then, the filled section-
    final Colour mix = new Colour().set(
      (a.r * level) + (b.r * s),
      (a.g * level) + (b.g * s),
      (a.b * level) + (b.b * s),
      1
    ) ;
    mix.setValue((a.value() * level) + (b.value() * s)) ;
    GL11.glColor4f(
      mix.r * c.r * f,
      mix.g * c.g * f,
      mix.b * c.b * f,
      1 * c.a
    ) ;
    UINode.drawQuad(
      x, y, x + (int) (size * level), y + BAR_HEIGHT,
      0, 0, 1, 1, base.z
    ) ;
    //
    //  And finish off-
    GL11.glEnd() ;
    rendering.view.setIsoMode() ;
    //*/
  }
}







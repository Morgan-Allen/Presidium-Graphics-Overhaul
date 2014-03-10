

package src.graphics.sfx ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;
import src.util.*;
import java.io.* ;
//import org.lwjgl.opengl.* ;



public class Healthbar extends SFX {
  
  
  
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
    BAR_HEIGHT = 5,
    DEFAULT_WIDTH = 40 ;
  
  public float level = 0.5f, yoff = 0 ;
  public float size = DEFAULT_WIDTH ;
  public Colour full = Colour.BLUE, empty = Colour.RED ;
  
  private float flash = 0 ;
  
  
  public Healthbar() {}
  public ModelAsset model() { return BAR_MODEL ; }
  
  
  
  /**  Updates and rendering-
    */
  protected void renderInPass(SFXPass pass) {
    
    //  First, establish screen coordinates for the bottom-left corner.
    final Vec3D base = new Vec3D().setTo(position) ;
    pass.rendering.view.translateToScreen(base) ;
    final int
      x = (int) (base.x - (size / 2)),
      y = (int) (base.y + yoff - (BAR_HEIGHT / 2)) ;
    
    //  First, do the background-
    final Colour a = full, b = empty ;
    final Colour c = colour == null ? Colour.WHITE : colour ;
    final float s = 1 - level, f = fog ;
    
    pass.compileQuad(
      ImageAsset.WHITE_TEX, c,
      x, y, size, BAR_HEIGHT,
      0, 0, 1, 1,
      base.z, true, false
    );
    
    //  When at less than half health, you need to flash-
    if (level < 0.5f) {
      float flashAlpha = 0 ;
      flashAlpha = (0.5f - level) * 2 ;
      flash += 0.04f * Math.PI / 2f ;
      flashAlpha *= f * c.a * Math.abs(Math.sin(flash)) ;
      final Colour flashed = new Colour().set(1, 0, 0, flashAlpha);

      pass.compileQuad(
        ImageAsset.WHITE_TEX, flashed,
        x, y, size, BAR_HEIGHT,
        0, 0, 1, 1,
        base.z + 0.05f, true, false
      );
    }
    //
    //  Then, the filled section-
    final Colour mix = new Colour().set(
      (a.r * level) + (b.r * s),
      (a.g * level) + (b.g * s),
      (a.b * level) + (b.b * s),
      1
    ) ;
    mix.setValue((a.value() * level) + (b.value() * s));
    pass.compileQuad(
      ImageAsset.WHITE_TEX, mix,
      x, y, size * level, BAR_HEIGHT,
      0, 0, 1, 1,
      base.z + 0.1f, true, false
    );
  }
}







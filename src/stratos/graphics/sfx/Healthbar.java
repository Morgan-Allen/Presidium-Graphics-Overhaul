

package stratos.graphics.sfx;
import stratos.graphics.common.*;
import stratos.util.*;



public class Healthbar extends SFX {
  
  
  
  /**  Constants, field definitions, constructors and save/load methods-
    */
  final public static ModelAsset BAR_MODEL = new ClassModel(
    "shield_fx_model", Healthbar.class
  ) {
    public Sprite makeSprite() { return new Healthbar(); }
  };
  
  final public static int
    BAR_HEIGHT = 5,
    DEFAULT_WIDTH = 40;
  final public static Colour
    AMBER_FLASH = new Colour().set(1, 0.75f, 0, 1);
  
  
  public float level = 0.5f, yoff = 0;
  public float size = DEFAULT_WIDTH;
  
  public Colour
    back  = Colour.DARK_GREY,
    warn  = Colour.RED,
    flash = AMBER_FLASH;
  public boolean alarm = false;
  private float flashTime = 0;
  
  
  public Healthbar() {
    super(PRIORITY_FIRST);
  }
  
  public ModelAsset model() { return BAR_MODEL; }
  
  
  
  /**  Updates and rendering-
    */
  protected void renderInPass(SFXPass pass) {
    
    //  First, establish screen coordinates for the bottom-left corner.
    final Vec3D base = new Vec3D().setTo(position);
    pass.rendering.view.translateToScreen(base);
    final int
      x = (int) (base.x - (size / 2)),
      y = (int) (base.y + yoff - (BAR_HEIGHT / 2));
    
    //  Then, establish correct colours for the fill, back, and warning-
    Colour colour = this.colour;
    if (colour == null) colour = Colour.LIGHT_GREY;
    Colour back = new Colour(this.back == null ? Colour.WHITE : this.back);
    back.a = colour.a * fog;
    warn.a = colour.a * fog;
    final float s = 1 - level, f = fog;
    
    pass.compileQuad(
      ImageAsset.WHITE_TEX(), back,
      x, y, size, BAR_HEIGHT,
      0, 0, 1, 1,
      base.z, true, false
    );
    
    //  When in alarm mode, you need to flash-
    if (alarm) {
      final float urgency = (1 - level) * 2;
      flashTime += ((urgency / Rendering.FRAMES_PER_SECOND) * Nums.PI / 2f);
      flashTime %= (Nums.PI * 2);
      Colour flash = new Colour(this.flash == null ? Colour.WHITE : this.flash);
      flash.a *= f * colour.a;
      flash.a *= Nums.abs(Nums.sin(flashTime));
      flash.calcFloatBits();
      
      pass.compileQuad(
        ImageAsset.WHITE_TEX(), flash,
        x, y, size, BAR_HEIGHT,
        0, 0, 1, 1,
        base.z + 0.05f, true, false
      );
    }

    Colour warn = new Colour(this.warn == null ? Colour.WHITE : this.warn);
    //  Then, the filled section-
    final Colour mix = new Colour().set(
      (colour.r * level) + (warn.r * s),
      (colour.g * level) + (warn.g * s),
      (colour.b * level) + (warn.b * s),
       colour.a
    );
    mix.setValue((colour.value() * level) + (warn.value() * s));
    pass.compileQuad(
      ImageAsset.WHITE_TEX(), mix,
      x, y, size * level, BAR_HEIGHT,
      0, 0, 1, 1,
      base.z + 0.1f, true, false
    );
  }
}







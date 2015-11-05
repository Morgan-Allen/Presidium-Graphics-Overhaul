/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.sfx;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;



//  TODO:  You'll want to be able to display a fatigue component as well, and
//  maybe tint the bar green/yellow to indicate poison?


public class Healthbar extends SFX {
  
  
  
  /**  Constants, field definitions, constructors and save/load methods-
    */
  final public static ModelAsset BAR_MODEL = new ClassModel(
    "healthbar_model", Healthbar.class
  ) {
    public Sprite makeSprite() { return new Healthbar(); }
  };
  
  final public static int
    BAR_HEIGHT = 5,
    DEFAULT_WIDTH = 40;
  final public static Colour
    TIRED_GREY  = new Colour(0.8f, 0.8f , 0.8f, 1),
    AMBER_FLASH = new Colour(1   , 0.75f, 0   , 1);
  
  
  public float hurtLevel = 0, tireLevel = 0, yoff = 0;
  public float size = DEFAULT_WIDTH;
  public boolean alarm = false;
  public Colour
    back  = Colour.DARK_GREY,
    tire  = TIRED_GREY,
    flash = AMBER_FLASH;
  
  private float flashTime = 0;
  
  
  public Healthbar() {
    super(PRIORITY_FIRST);
  }
  
  public ModelAsset model() { return BAR_MODEL; }
  
  
  
  /**  Updates and rendering-
    */
  public void renderForWidget(WidgetsPass pass, UINode basis) {
    doRendering(null, pass, basis);
  }
  
  
  protected void renderInPass(SFXPass pass) {
    doRendering(pass, null, null);
  }
  
  
  private void doRendering(SFXPass passS, WidgetsPass passW, UINode basis) {
    
    //  First, establish screen coordinates for the bottom-left corner.
    final int x, y; final float z;
    final boolean widget = passW != null && basis != null;
    if (widget) {
      x    = (int)  basis.xpos();
      y    = (int) (basis.ypos() + (basis.ydim() / 2));
      size = (int)  basis.xdim();
      z    =        basis.absDepth;
    }
    else {
      final Vec3D base = new Vec3D().setTo(position);
      passS.rendering.view.translateToScreen(base);
      x = (int) (base.x - (size / 2));
      y = (int) (base.y + yoff - (BAR_HEIGHT / 2));
      z = base.z;
    }
    
    //  Then, establish correct colours for the fill, back, and warning-
    final ImageAsset blank = Image.SOLID_WHITE;
    Colour back = new Colour(this.back);
    back.blend(Colour.BLACK, fog);
    back.calcFloatBits();
    renderPortion(back, blank, x, y, size, z, passS, passW, basis, widget);
    
    //  When in alarm mode, you need to flash-
    if (alarm) {
      final float urgency = hurtLevel * 2;
      flashTime += ((urgency / Rendering.FRAMES_PER_SECOND) * Nums.PI / 2f);
      flashTime %= (Nums.PI * 2);
      
      Colour flash = new Colour(this.flash);
      flash.a *= fog * Nums.abs(Nums.sin(flashTime));
      flash.calcFloatBits();
      renderPortion(flash, blank, x, y, size, z, passS, passW, basis, widget);
    }
    
    //  If level/tiredLevel are > 0, then show them:
    float fillLevel = 1 - Nums.clamp(hurtLevel + tireLevel, 0, 1);
    if (tireLevel > 0) {
      float across = size * Nums.min(1 - hurtLevel, fillLevel + tireLevel);
      renderPortion(tire, blank, x, y, across, z, passS, passW, basis, widget);
    }
    if (fillLevel > 0) {
      float across = size * fillLevel;
      Colour mix = new Colour(colour);
      mix.blend(flash, (1 - fillLevel));
      mix.calcFloatBits();
      renderPortion(mix, blank, x, y, across, z, passS, passW, basis, widget);
    }
  }
  
  
  private void renderPortion(
    Colour tone, ImageAsset tex,
    int x, int y, float across, float z,
    SFXPass passS, WidgetsPass passW, UINode basis, boolean widget
  ) {
    if (widget) passW.draw(
      tex.asTexture(), tone,
      x, y, across, BAR_HEIGHT,
      0, 0, 1, 1
    );
    else passS.compileQuad(
      tex.asTexture(), tone, false,
      x, y, across, BAR_HEIGHT,
      0, 0, 1, 1,
      z, true
    );
  }
}












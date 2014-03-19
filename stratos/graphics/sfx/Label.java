


package stratos.graphics.sfx;
import stratos.graphics.common.*;
import stratos.graphics.widgets.Alphabet;
import stratos.user.UIConstants;
import stratos.util.*;


public class Label extends SFX {
  
  
  final public static ModelAsset LABEL_MODEL = new Assets.ClassModel(
    "label_fx_model", Label.class
  ) {
    public Sprite makeSprite() { return new Label() ; }
  };
  
  final static Alphabet FONT = UIConstants.INFO_FONT;
  final static float FONT_SCALE = 0.8f;
  
  public String phrase = "";
  
  
  public Label() {
    super();
  }
  
  
  public ModelAsset model() { return LABEL_MODEL; }
  
  
  protected void renderInPass(SFXPass pass) {
    final Vec3D flatPoint = new Vec3D(position);
    pass.rendering.view.translateToScreen(flatPoint);
    final float width = phraseWidth(phrase, FONT, FONT_SCALE);
    renderPhrase(
      phrase, FONT, FONT_SCALE, this.colour,
      flatPoint.x - (width / 2), flatPoint.y, flatPoint.z,
      pass, false
    );
  }
  
  
  static float phraseWidth(
    String phrase, Alphabet font, float fontScale
  ) {
    float width = 0;
    for (char c : phrase.toCharArray()) {
      Alphabet.Letter l = FONT.letterFor(c);
      if (l == null) l = FONT.letterFor(' ');
      width += l.width * fontScale;
    }
    return width;
  }
  
  
  static void renderPhrase(
    String phrase, Alphabet font, float fontScale, Colour colour,
    float screenX, float screenY, float screenZ,
    SFXPass pass, boolean vivid
  ) {
    float scanW = 0;
    for (char c : phrase.toCharArray()) {
      Alphabet.Letter l = FONT.letterFor(c);
      if (l == null) l = FONT.letterFor(' ');
      
      pass.compileQuad(
        FONT.texture(), colour,
        screenX + scanW, screenY,
        l.width * fontScale, l.height * fontScale,
        l.umin, l.vmin, l.umax, l.vmax,
        screenZ, true, vivid
      );
      scanW += l.width * fontScale;
    }
  }
}

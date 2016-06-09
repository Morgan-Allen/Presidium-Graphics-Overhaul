/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.sfx;
import stratos.start.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;



public class Label extends SFX {
  
  
  public static class LabelModel extends ModelAsset {
    
    final Alphabet font;
    
    public LabelModel(
      String modelName, Class sourceClass,
      String assetsDir, String fontFile
    ) {
      super(sourceClass, modelName);
      this.font = Alphabet.loadAlphabet(
        sourceClass, modelName+"_font_asset", assetsDir, fontFile
      );
    }


    public Object sortingKey() {
      return this;
    }
    
    
    public Label makeSprite() {
      return new Label(this);
    }
    
    
    protected State loadAsset() {
      Assets.loadNow(font);
      if (font.stateLoaded()) return state = State.LOADED;
      else return state = State.ERROR;
    }
    
    
    protected State disposeAsset() {
      Assets.disposeOf(font);
      if (font.stateDisposed()) return state = State.DISPOSED;
      else return state = State.ERROR;
    }
  }
  
  
  final LabelModel model;
  public String phrase = "";
  public float fontScale = 0.8f;
  
  
  private Label(LabelModel model) {
    super(PRIORITY_FIRST);
    this.model = model;
  }
  
  
  public ModelAsset model() { return model; }
  
  
  protected void renderInPass(SFXPass pass) {
    if (phrase == null) return;
    
    final Vec3D flatPoint = new Vec3D(position);
    pass.rendering.view.translateToScreen(flatPoint);
    final float width = phraseWidth(phrase, model.font, fontScale);
    renderPhrase(
      phrase, model.font, fontScale, this.colour,
      flatPoint.x - (width / 2), flatPoint.y, flatPoint.z,
      pass, true
    );
  }
  
  
  public static float phraseWidth(
    String phrase, Alphabet font, float fontScale
  ) {
    float width = 0;
    for (char c : phrase.toCharArray()) {
      Alphabet.Letter l = font.letterFor(c);
      if (l == null) l = font.letterFor(' ');
      width += l.width * fontScale;
    }
    return width;
  }
  
  
  public static void renderPhrase(
    String phrase, Alphabet font, float fontScale, Colour colour,
    float screenX, float screenY, float screenZ,
    SFXPass pass, boolean vivid
  ) {
    float scanW = 0;
    for (char c : phrase.toCharArray()) {
      Alphabet.Letter l = font.letterFor(c);
      if (l == null) l = font.letterFor(' ');
      
      pass.compileQuad(
        font.texture(), colour,
        vivid, screenX + scanW,
        screenY, l.width * fontScale,
        l.height * fontScale, l.umin, l.vmin, l.umax,
        l.vmax, screenZ, true
      );
      scanW += l.width * fontScale;
    }
  }
}









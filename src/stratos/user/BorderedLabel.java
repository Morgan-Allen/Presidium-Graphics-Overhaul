/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;
import com.badlogic.gdx.math.*;



public class BorderedLabel extends UIGroup implements UIConstants {
  
  
  final static ImageAsset
    BLACK_BAR = ImageAsset.fromImage(
      BorderedLabel.class, "media/GUI/black_bar.png"
    );
  
  
  final Bordering bordering;
  final public Text text;
  private boolean doFade = false;
  private float fadeLevel = 0;
  
  
  public BorderedLabel(BaseUI UI) {
    super(UI);
    this.bordering = new Bordering(UI, BLACK_BAR);
    bordering.setInsets(20, 20, 10, 10);
    bordering.setUV(0.33f, 0.33f, 0.5f, 0.5f);
    bordering.alignAcross(0, 1);
    bordering.alignDown  (0, 1);
    bordering.attachTo(this);
    
    this.text = new Text(UI, INFO_FONT);
    text.alignAcross(0, 1);
    text.alignDown  (0, 1);
    text.attachTo(this);
  }
  
  
  public void setMessage(String message, boolean doFade) {
    text.setText(message);
    this.doFade = doFade;
    this.fadeLevel = 1;

    text.setToPreferredSize(UI.xdim() / 2);
    text.alignHorizontal(0.5f, (int) text.preferredSize().xdim(), 0);
    text.alignVertical  (0.5f, (int) text.preferredSize().ydim(), 0);
  }
  
  
  protected UINode selectionAt(Vector2 mousePos) {
    if (doFade) return null;
    else return super.selectionAt(mousePos);
  }
  
  
  protected void updateState() {
    bordering.alignToMatch(text, 10, 2);
    text     .relAlpha = fadeLevel;
    bordering.relAlpha = fadeLevel;
    
    if (doFade) {
      fadeLevel = Nums.clamp(fadeLevel - DEFAULT_FADE_INC, 0, 1);
      if (fadeLevel == 0) text.setText("");
    }
    super.updateState();
  }
}











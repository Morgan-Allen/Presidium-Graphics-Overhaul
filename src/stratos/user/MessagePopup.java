

package stratos.user;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;



public class MessagePopup extends UIGroup implements UIConstants {
  
  
  final static ImageAsset
    BLACK_BAR = ImageAsset.fromImage(
      MessagePopup.class, "media/GUI/black_bar.png"
    );
  
  
  final Bordering bordering;
  final Text label;
  private float fade;
  
  
  public MessagePopup(BaseUI UI) {
    super(UI);
    this.bordering = new Bordering(UI, BLACK_BAR);
    bordering.setInsets(20, 20, 10, 10);
    bordering.setUV(0.33f, 0.33f, 0.5f, 0.5f);
    bordering.alignAcross(0, 1);
    bordering.alignDown  (0, 1);
    bordering.attachTo(this);
    
    this.label = new Text(UI, INFO_FONT);
    label.alignAcross(0, 1);
    label.alignDown  (0, 1);
    label.attachTo(this);
  }
  
  
  public void setMessage(String message) {
    label.setText(message);
    fade = 1;
  }
  
  
  protected void updateState() {
    
    label.setToPreferredSize(UI.xdim() / 2);
    label.alignHorizontal(0.5f, (int) label.preferredSize().xdim(), 0);
    label.alignVertical  (0.5f, (int) label.preferredSize().ydim(), 0);
    label.relAlpha = fade;
    
    bordering.surround(label);
    bordering.relAlpha = fade;
    
    fade = Nums.clamp(fade - DEFAULT_FADE_INC, 0, 1);
    if (fade == 0) label.setText("");
    
    super.updateState();
  }
}











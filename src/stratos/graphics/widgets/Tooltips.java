


package stratos.graphics.widgets;
import stratos.graphics.common.*;
import stratos.user.UIConstants;
import stratos.util.*;

import com.badlogic.gdx.math.*;



public class Tooltips extends UIGroup {

  
  final public static ImageAsset TIPS_TEX = ImageAsset.fromImage(
    UIConstants.class, "media/GUI/tips_frame.png"
  );
  //  TODO:  Pass in the texture as a parameter.
  
  Bordering bordering;
  Text infoText;
  
  
  public Tooltips(HUD UI) {
    super(UI);
    bordering = new Bordering(UI, TIPS_TEX);
    bordering.relBound.set(0, 0, 1, 1);
    bordering.absBound.set(-10, -10, 20, 20);
    bordering.left   = bordering.right = 10;
    bordering.bottom = bordering.top   = 10;
    bordering.attachTo(this);
    infoText = new Text(UI, UIConstants.INFO_FONT);
    infoText.scale = 0.75f;
    infoText.attachTo(this);
  }
  
  
  protected UINode selectionAt(Vector2 mousePos) {
    return null;
  }


  protected void updateState() {
    final float HOVER_TIME = 0.5f, HOVER_FADE = 0.25f;
    final int MAX_TIPS_WIDTH = 200;
    hidden = true;
    if (
      UI.selected() != null &&
      UI.timeHovered() > HOVER_TIME &&
      UI.selected().info() != null
    ) {
      //
      //  Firstly, determine the alpha at the current point in time.
      final float alpha = Nums.clamp(
        (UI.timeHovered() - HOVER_TIME) / HOVER_FADE, 0, 1
      );
      hidden = false;
      this.relAlpha = alpha;
      infoText.setText(UI.selected().info());
      infoText.setToPreferredSize(MAX_TIPS_WIDTH);
      //
      //  You need to constrain your bounds to fit within the visible area of
      //  the screen, but still accomodate visible text.
      final Box2D
        TB = infoText.preferredSize(),
        SB = UI.screenBounds();
      final float wide = TB.xdim(), high = TB.ydim();
      absBound.xdim(wide);
      absBound.ydim(high);
      absBound.xpos(Nums.clamp(
        UI.mousePos().x, 0 - bordering.left,
        SB.xdim() - (wide + bordering.right)
      ));
      absBound.ypos(Nums.clamp(
        UI.mousePos().y, 0 - bordering.bottom,
        SB.ydim() - (high + bordering.top)
      ));
    }
    super.updateState();
  }
}



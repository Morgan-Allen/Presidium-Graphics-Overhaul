


package stratos.graphics.widgets;
import stratos.graphics.common.*;
import stratos.user.UIConstants;
import stratos.util.*;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.*;



public class Tooltips extends UIGroup {

  
  final public static ImageAsset TIPS_TEX = ImageAsset.fromImage(
    "media/GUI/tips_frame.png", UIConstants.class
  );
  //final static Box2D TIPS_INSETS = new Box2D().set(-10, -10, 20, 20);
  
  Bordering bordering ;
  Text infoText ;
  
  
  public Tooltips(HUD UI) {
    super(UI) ;
    bordering = new Bordering(UI, TIPS_TEX.asTexture());
    bordering.relBound.set(0, 0, 1, 1);
    bordering.absBound.set(-10, -10, 20, 20);
    bordering.drawInset.set(10, 10, -20, -20);
    bordering.attachTo(this);
    infoText = new Text(UI, UIConstants.INFO_FONT);
    infoText.scale = 0.75f;
    infoText.attachTo(this);
  }
  
  
  protected UINode selectionAt(Vector2 mousePos) {
    return null ;
  }
  
  
  public void render(SpriteBatch batch2D) {
    super.render(batch2D) ;
    //if (! hidden) I.say("rendering tooltip...") ;
  }


  protected void updateState() {
    final float HOVER_TIME = 1.25f, HOVER_FADE = 0.25f ;
    final int MAX_TIPS_WIDTH = 200 ;
    hidden = true ;
    if (
      UI.selected() != null &&
      UI.timeHovered() > HOVER_TIME
    ) {
      final String info = UI.selected().info() ;
      if (info != null) {
        final float alpha = Visit.clamp(
          (UI.timeHovered() - HOVER_TIME) / HOVER_FADE, 0, 1
        ) ;
        hidden = false ;
        this.relAlpha = alpha ;
        infoText.setText(info) ;
        infoText.setToPreferredSize(MAX_TIPS_WIDTH) ;
        //
        //  You need to constrain your bounds to fit within the visible area of
        //  the screen, but still accomodate visible text.
        final Box2D
          TB = infoText.preferredSize(),
          SB = UI.screenBounds(),
          BI = bordering.drawInset ;
        final float wide = TB.xdim(), high = TB.ydim() ;
        absBound.xdim(wide) ;
        absBound.ydim(high) ;
        absBound.xpos(Visit.clamp(
          UI.mousePos().x, 0 - BI.xpos(),
          SB.xdim() - (wide + BI.xmax())
        )) ;
        absBound.ypos(Visit.clamp(
          UI.mousePos().y, 0 - BI.ypos(),
          SB.ydim() - (high + BI.ymax())
        )) ;
      }
    }
    super.updateState() ;
  }
}



/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user.mainscreen;
import com.badlogic.gdx.math.Vector2;

import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



public abstract class MenuPane extends ListingPane {
  
  
  final public static ImageAsset
    BORDER_TEX = ImageAsset.fromImage(
      MenuPane.class, "media/GUI/Front/Panel.png"
    ),
    BUTTON_FRAME_TEX = ImageAsset.fromImage(
      MenuPane.class, "media/GUI/tips_frame.png"
    ),
    WIDGET_BACK = ImageAsset.fromImage(
      MenuPane.class, "media/GUI/Front/widget_back.png"
    ),
    WIDGET_BACK_LIT = ImageAsset.fromImage(
      MenuPane.class, "media/GUI/Front/widget_back_lit.png"
    );
  
  
  final int stateID;
  Bordering border;
  Button backButton;
  
  
  public MenuPane(HUD UI, int stateID) {
    super(UI);
    this.stateID = stateID;
  }
  
  
  protected void initBackground() {
    this.border = new Bordering(UI, BORDER_TEX);
    border.attachAndSurround(this);
  }
  
  
  protected void initForeground() {
    this.backButton = new Button(
      UI, "back", WIDGET_BACK, WIDGET_BACK_LIT, "Back"
    ) {
      protected void whenClicked() {
        navigateBack();
      }
    };
    backButton.alignTop  (-12, 18);
    backButton.alignRight( 18, 48);
    backButton.attachTo(this);
  }
  
  
  protected float listSpacing() {
    return 5;
  }
  
  
  protected void updateState() {
    backButton.hidden = rootPane() == this;
    super.updateState();
  }
  
  
  
  /**  Utility methods for manufacturing common widgets-
    */
  protected abstract class TextButton extends UIGroup {
    
    final Bordering around;
    
    
    public TextButton(HUD UI, String text, float scale) {
      super(UI);
      
      final Text t = new Text(UI, UIConstants.INFO_FONT);
      t.append(text);
      t.scale = scale;
      t.setToLineSize();

      this.alignToMatch(t);
      
      around = new Bordering(UI, BUTTON_FRAME_TEX);
      around.alignToFill();
      around.attachTo(this);
      
      t.alignToFill();
      t.attachTo(this);
    }
    
    
    protected UINode selectionAt(Vector2 mousePos) {
      return (trueBounds().contains(mousePos.x, mousePos.y)) ? this : null;
    }
    
    
    protected abstract void whenClicked();
    
    
    protected boolean toggled() {
      return false;
    }
    
    
    protected void updateState() {
      if      (toggled()  ) around.relAlpha = 1.0f;
      else if (amHovered()) around.relAlpha = 0.5f;
      else                  around.relAlpha = 0.0f;
      around.hidden = around.relAlpha == 0;
      
      super.updateState();
    }
  }
  
  
  protected UINode createTextButton(
    String text, float scale, final Description.Link link
  ) {
    return new TextButton(UI, text, scale) {
      protected void whenClicked() {
        link.whenClicked();
      }
    };
  }
  
  
  protected UINode createTextItem(
    String text, float scale, Colour c
  ) {
    final Text t = new Text(UI, UIConstants.INFO_FONT);
    t.append(text, c == null ? Colour.WHITE : c);
    t.scale = scale;
    t.setToPreferredSize(MainScreen.MENU_PANEL_WIDE);
    
    final UIGroup item = new UIGroup(UI);
    item.alignToMatch(t);
    //final Bordering b = new Bordering(UI, BUTTON_FRAME_TEX);
    //b.alignToFill();
    //b.attachTo(item);
    t.alignToFill();
    t.attachTo(item);
    return item;
  }
  
  
}
















/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user.mainscreen;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



public abstract class MenuPane extends ListingPane {
  
  
  final public static ImageAsset
    BORDER_TEX = ImageAsset.fromImage(
      MenuPane.class, "media/GUI/Front/Panel.png"
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
  protected UINode createTextButton(
    String text, float scale, Text.Clickable link
  ) {
    final Text t = new Text(UI, UIConstants.INFO_FONT);
    if (link != null) t.append(text, link);
    else t.append(text);
    t.scale = scale;
    t.setToLineSize();
    return t;
  }
  
  
  protected UINode createTextItem(
    String text, float scale, Colour c
  ) {
    final Text t = new Text(UI, UIConstants.INFO_FONT);
    t.append(text, c == null ? Colour.WHITE : c);
    t.scale = scale;
    t.setToPreferredSize(MainScreen.MENU_PANEL_WIDE);
    return t;
  }
  
  
}
















/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user.mainmenu;
import stratos.graphics.common.ImageAsset;
import stratos.graphics.widgets.*;
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
  
  
  Bordering border;
  Button backButton;
  
  
  public MenuPane(HUD UI) {
    super(UI);
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
    backButton.alignTop  ( 0, 24);
    backButton.alignRight(28, 48);
    backButton.attachTo(this);
  }
  
  
  protected float listSpacing() {
    return 5;
  }
  
  
  protected void updateState() {
    backButton.hidden = rootPane() == this;
    super.updateState();
  }
  
  
  
}











package stratos.user;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.game.actors.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;



public class CommercePane extends SelectionInfoPane {
  
  
  final static ImageAsset
    COMMERCE_ICON = ImageAsset.fromImage(
      CommercePane.class, "media/GUI/Panels/edicts_tab.png"  //  TODO:  CHANGE
    ),
    COMMERCE_ICON_LIT = Button.CIRCLE_LIT;
  
  
  public CommercePane(BaseUI UI) {
    super(UI, null, false, false);
  }
  
  
  static Button createButton(final BaseUI baseUI) {
    final CommercePane pane = new CommercePane(baseUI);
    final Button button = new Button(
      baseUI, COMMERCE_ICON, COMMERCE_ICON_LIT, "Finance and Legislation"
    ) {
      protected void whenClicked() {
        if (baseUI.currentPane() == pane) {
          baseUI.setInfoPanels(null, null);
        }
        else {
          baseUI.setInfoPanels(pane, null);
        }
      }
    };
    return button;
  }
  
  
  protected void updateText(
    BaseUI UI, Text headerText, Text detailText, Text listingText
  ) {
    super.updateText(UI, headerText, detailText, listingText);
    headerText.setText("Finance and Legislation");
    
    final Base base = UI.played();
    final Description d = detailText;
    base.finance.describeTo(d);
  }
}







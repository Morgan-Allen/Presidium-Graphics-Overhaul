

package stratos.user;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.graphics.widgets.Text.Clickable;
import stratos.util.*;




public class CommsPanel extends SelectionInfoPane {
  
  
  final static ImageAsset
    COMMS_ICON = ImageAsset.fromImage(
      "media/GUI/Panels/comms_tab.png", CommsPanel.class
    ),
    COMMS_ICON_LIT = Button.CIRCLE_LIT;
  final static String ALL_CATEGORIES[] = {};
  
  
  private class Message {
    String keyTitle;
    DialoguePanel panel;
  }
  final List <Message> messages = new List <Message> ();
  
  
  
  public CommsPanel(BaseUI UI) {
    super(UI, null, null, true, ALL_CATEGORIES);
  }
  
  
  public boolean hasMessage(String keyTitle) {
    return messageWith(keyTitle) != null;
  }
  
  
  public DialoguePanel messageWith(String keyTitle) {
    for (Message m : messages) if (m.keyTitle.equals(keyTitle)) {
      return m.panel;
    }
    return null;
  }
  
  
  public DialoguePanel addMessage(
    String title, Composite portrait, String mainText, Clickable... options
  ) {
    final Message message = new Message();
    
    final Clickable navOptions[] = {
      new Clickable() {
        public String fullName() { return "View all messages"; }
        public void whenTextClicked() {
          UI.setInfoPanels(UI.commsPanel(), null);
        }
      }
    };
    
    message.keyTitle = title;
    message.panel = new DialoguePanel(
      UI, portrait, title, mainText,
      (Clickable[]) Visit.compose(Clickable.class, options, navOptions)
    );
    messages.add(message);
    
    return message.panel;
  }
  
  
  protected void updateText(
    final BaseUI UI, Text headerText, Text detailText
  ) {
    super.updateText(UI, headerText, detailText);
    headerText.setText("COMMUNICATIONS");
    
    for (final Message message : messages) {
      detailText.append("\n  ");
      detailText.append(new Clickable() {
        public String fullName() { return message.keyTitle; }
        public void whenTextClicked() { UI.setInfoPanels(message.panel, null); }
      });
    }
  }
}




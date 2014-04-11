

package stratos.user;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.graphics.widgets.Text.Clickable;
import stratos.util.*;




public class CommsPanel extends InfoPanel {
  
  
  final static ImageAsset
    COMMS_ICON = ImageAsset.fromImage(
      "media/GUI/Panels/comms_panel.png", CommsPanel.class
    );
  final static String
    ALL_CATEGORIES[] = {};
  
  
  private class Message {
    String keyTitle;
    InfoPanel panel;
  }
  final List <Message> messages = new List <Message> ();
  
  
  
  public CommsPanel(BaseUI UI) {
    super(
      UI, null, Composite.withImage(COMMS_ICON, "comms_panel"),
      true, ALL_CATEGORIES
    );
  }
  
  
  public boolean hasMessage(String keyTitle) {
    for (Message m : messages) if (m.keyTitle.equals(keyTitle)) {
      return true;
    }
    return false;
  }
  
  
  public boolean pushMessage(
    String title, Composite portrait, String mainText, Clickable... options
  ) {
    final Message message = new Message();
    message.keyTitle = title;
    message.panel = new DialoguePanel(
      UI, portrait, title, mainText, options
    );
    messages.add(message);
    UI.setInfoPanels(message.panel, null);
    
    //  TODO:  You need to add a 'go back' option in the dialogue panel.
    return true;
  }
  
  
  protected void updateText(
    final BaseUI UI, Text headerText, Text detailText
  ) {
    super.updateText(UI, headerText, detailText);
    
    for (final Message message : messages) {
      detailText.append("\n  ");
      detailText.append(new Clickable() {
        public String fullName() { return message.keyTitle; }
        public void whenClicked() { UI.setInfoPanels(message.panel, null); }
      });
    }
  }
}






package stratos.user;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.graphics.widgets.Text.Clickable;
import stratos.util.*;




public class CommsPanel extends SelectionInfoPane {
  
  
  final static ImageAsset
    COMMS_ICON = ImageAsset.fromImage(
      CommsPanel.class, "media/GUI/Panels/comms_tab.png"
    ),
    COMMS_ICON_LIT = Button.CIRCLE_LIT;
  final static String ALL_CATEGORIES[] = {};
  
  
  public static interface CommSource extends Session.Saveable {
    MessagePanel messageFor(String title, CommsPanel comms, boolean useCache);
  }
  
  private class Message {
    CommSource source;
    String keyTitle;
    MessagePanel panel;
    //  TODO:  Optional time-stamps?
  }
  
  final List <Message> messages = new List <Message> ();
  
  
  
  public CommsPanel(BaseUI UI) {
    super(UI, null, null, false, ALL_CATEGORIES);
  }
  
  
  public void loadState(Session s) throws Exception {
    for (int n = s.loadInt(); n-- > 0;) {
      final CommSource    source = (CommSource) s.loadObject();
      final String        key    = s.loadString();
      final MessagePanel panel  = source.messageFor(key, this, false);
      
      if (messageWith(key) == null && panel != null) {
        final Message m = new Message();
        m.source   = source;
        m.keyTitle = key   ;
        m.panel    = panel ;
        messages.add(m);
      }
    }
    
    final int index = s.loadInt();
    if (index != -1) {
      final Message m = messages.atIndex(index);
      UI.setPanelsInstant(m.panel, null);
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(messages.size());
    for (Message m : messages) {
      s.saveObject(m.source  );
      s.saveString(m.keyTitle);
    }
    
    final Object pane = UI.currentPane();
    int index = 0;
    for (Message m : messages) if (m.panel == pane) break; else index++;
    if (index == messages.size()) index = -1;
    s.saveInt(index);
  }
  
  
  
  public boolean hasMessage(String keyTitle) {
    return messageWith(keyTitle) != null;
  }
  
  
  public Batch <String> messageTitles() {
    final Batch <String> titles = new Batch <String> ();
    for (Message m : messages) titles.add(m.keyTitle);
    return titles;
  }
  
  
  public MessagePanel messageWith(String keyTitle) {
    for (Message m : messages) if (m.keyTitle.equals(keyTitle)) {
      return m.panel;
    }
    return null;
  }
  
  
  public MessagePanel addMessage(
    CommSource source, String title, MessagePanel panel
  ) {
    final MessagePanel old = messageWith(title);
    if (old != null) return old;
    if (panel == null) I.complain("CANNOT PUSH NULL PANEL! "+title);
    
    final Message message = new Message();
    message.source   = source;
    message.keyTitle = title ;
    message.panel    = panel ;
    messages.include(message);
    return message.panel;
  }
  
  
  public MessagePanel addMessage(
    CommSource source, String title, Composite portrait,
    String mainText, Clickable... options
  ) {
    final Clickable navOptions[] = {
      new Clickable() {
        public String fullName() { return "View all messages"; }
        public void whenClicked() {
          UI.setInfoPanels(UI.commsPanel(), null);
        }
      }
    };
    final MessagePanel panel = new MessagePanel(
      UI, portrait, title, mainText, null,
      (Clickable[]) Visit.compose(Clickable.class, options, navOptions)
    );
    return addMessage(source, title, panel);
  }
  
  
  protected void updateText(
    final BaseUI UI, Text headerText, Text detailText, Text listingText
  ) {
    super.updateText(UI, headerText, detailText, listingText);
    headerText.setText("COMMUNICATIONS");
    
    for (final Message message : messages) {
      detailText.append("\n  ");
      detailText.append(new Clickable() {
        
        public String fullName() { return message.keyTitle; }
        
        public void whenClicked() {
          UI.setInfoPanels(message.panel, null);
        }
      });
    }
  }
}




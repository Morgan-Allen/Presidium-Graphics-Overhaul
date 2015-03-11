

package stratos.user;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.graphics.widgets.Text.Clickable;
import stratos.util.*;




public class CommsPane extends SelectionInfoPane {
  
  
  final static ImageAsset
    COMMS_ICON = ImageAsset.fromImage(
      CommsPane.class, "media/GUI/Panels/comms_tab.png"
    ),
    COMMS_ICON_LIT = Button.CIRCLE_LIT;
  
  
  public static Button createButton(
    final BaseUI baseUI, final CommsPane commsPanel
  ) {
    return new Button(baseUI, COMMS_ICON, COMMS_ICON_LIT, "Messages") {
      protected void whenClicked() {
        baseUI.setInfoPanels(commsPanel, null);
      }
    };
  }
  
  
  public static interface CommSource extends Session.Saveable {
    MessagePane messageFor(String title, CommsPane comms, boolean useCache);
  }
  
  private class Message {
    CommSource source;
    String keyTitle;
    MessagePane panel;
    //  TODO:  Optional time-stamps?
  }
  
  final List <Message> messages = new List <Message> ();
  
  
  
  public CommsPane(BaseUI UI) {
    super(UI, null, null, false);
  }
  
  
  public void loadState(Session s) throws Exception {
    for (int n = s.loadInt(); n-- > 0;) {
      final CommSource    source = (CommSource) s.loadObject();
      final String        key    = s.loadString();
      final MessagePane panel  = source.messageFor(key, this, false);
      
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
  
  
  public MessagePane messageWith(String keyTitle) {
    for (Message m : messages) if (m.keyTitle.equals(keyTitle)) {
      return m.panel;
    }
    return null;
  }
  
  
  public MessagePane addMessage(
    CommSource source, String title, MessagePane panel
  ) {
    final MessagePane old = messageWith(title);
    if (old != null) return old;
    if (panel == null) I.complain("CANNOT PUSH NULL PANEL! "+title);
    
    final Message message = new Message();
    message.source   = source;
    message.keyTitle = title ;
    message.panel    = panel ;
    messages.include(message);
    return message.panel;
  }
  
  
  public MessagePane addMessage(
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
    final MessagePane panel = new MessagePane(
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




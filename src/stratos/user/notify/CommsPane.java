/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user.notify;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.graphics.widgets.Text.Clickable;
import stratos.user.BaseUI;
import stratos.user.SelectionInfoPane;
import stratos.util.*;



/*

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
        if (baseUI.currentPane() == commsPanel) {
          baseUI.setInfoPanels(null, null);
        }
        else {
          baseUI.setInfoPanels(commsPanel, null);
        }
      }
    };
  }
  
  //*
  public static interface DialogueSource extends Session.Saveable {
    DialoguePane messageFor(String title, CommsPane comms, boolean useCache);
  }
  
  private class Message {
    DialogueSource source;
    String keyTitle;
    DialoguePane panel;
    boolean isUrgent;
    //  TODO:  Optional time-stamps?
  }
  //*/
/*
  
  final private List <Message> messages = new List <Message> ();
  
  
  
  public CommsPane(BaseUI UI) {
    super(UI, null, null, false);
  }
  
  
  public void loadState(Session s) throws Exception {
    for (int n = s.loadInt(); n-- > 0;) {
      final DialogueSource source = (DialogueSource) s.loadObject();
      final String         key    = s.loadString();
      final boolean        urgent = s.loadBool  ();
      final DialoguePane   panel  = source.messageFor(key, this, false);
      
      if (messageWith(key) == null && panel != null) {
        final Message m = new Message();
        m.source   = source;
        m.keyTitle = key   ;
        m.panel    = panel ;
        m.isUrgent = urgent;
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
      s.saveBool  (m.isUrgent);
    }
    
    final Object pane = UI.currentPane();
    int index = 0;
    for (Message m : messages) if (m.panel == pane) break; else index++;
    if (index == messages.size()) index = -1;
    s.saveInt(index);
  }
  
  
  public Batch <DialoguePane> urgentMessages() {
    final Batch <DialoguePane> panes = new Batch <DialoguePane> ();
    for (Message m : messages) if (m.isUrgent) panes.add(m.panel);
    return panes;
  }
  
  
  public boolean hasMessage(String keyTitle) {
    return messageWith(keyTitle) != null;
  }
  
  
  public Batch <String> messageTitles() {
    final Batch <String> titles = new Batch <String> ();
    for (Message m : messages) titles.add(m.keyTitle);
    return titles;
  }
  
  
  public DialoguePane messageWith(String keyTitle) {
    for (Message m : messages) if (m.keyTitle.equals(keyTitle)) {
      return m.panel;
    }
    return null;
  }
  
  
  public boolean setAsUrgent(String messageName, boolean isUrgent) {
    for (Message m : messages) if (m.keyTitle.equals(messageName)) {
      m.isUrgent = isUrgent;
      return true;
    }
    return false;
  }
  
  
  public DialoguePane addMessage(
    DialogueSource source, String title, DialoguePane panel
  ) {
    final DialoguePane old = messageWith(title);
    if (old != null) return old;
    if (panel == null) I.complain("CANNOT PUSH NULL PANEL! "+title);
    
    final Message message = new Message();
    message.source   = source;
    message.keyTitle = title ;
    message.panel    = panel ;
    messages.include(message);
    return message.panel;
  }
  
  
  public DialoguePane addMessage(
    DialogueSource source, String title, Composite portrait,
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
    final DialoguePane panel = new DialoguePane(
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
//*/



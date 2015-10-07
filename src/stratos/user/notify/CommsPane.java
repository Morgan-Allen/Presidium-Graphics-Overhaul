/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user.notify;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.start.SaveUtils;
import stratos.user.*;
import stratos.util.*;



public class CommsPane extends SelectionPane {
  
  
  final static ImageAsset
    COMMS_ICON = ImageAsset.fromImage(
      CommsPane.class, "media/GUI/Panels/comms_tab.png"
    ),
    COMMS_ICON_LIT = Button.CIRCLE_LIT;
  
  
  final ReminderListing listing;
  
  public CommsPane(BaseUI UI, ReminderListing listing) {
    super(UI, null, null, false);
    this.listing = listing;
  }
  
  
  protected void updateText(
    final BaseUI UI, Text headerText, Text detailText, Text listingText
  ) {
    super.updateText(UI, headerText, detailText, listingText);
    headerText.setText("COMMUNICATIONS");
    
    final List <MessagePane> sorting = new List <MessagePane> () {
      protected float queuePriority(MessagePane r) {
        return r.receiptDate();
      }
    };
    Visit.appendTo(sorting, listing.oldMessages);
    sorting.queueSort();
    
    for (final MessagePane message : sorting) {
      detailText.append("\n  ");
      detailText.append(new Description.Link(message.title) {
        public void whenClicked() {
          UI.setMessagePane(message);
        }
      });
      
      final String timeStamp = SaveUtils.timeStamp(message.receiptDate());
      detailText.append("\n    (Received: "+timeStamp+") ", Colour.GREY);
    }
  }
  
  
  /**  Utility class to allow access from the parent listing-
    */
  protected static class Reminder extends LabelledReminder {
    
    final CommsPane pane;
    
    public Reminder(BaseUI UI, Object refers, ReminderListing listing) {
      super(
        UI, refers, "communications",
        COMMS_ICON, COMMS_ICON_LIT, "Old Messages"
      );
      pane = new CommsPane(UI, listing);
    }
    
    
    protected void whenButtonClicked() {
      if (UI.currentInfoPane() == pane) {
        UI.setInfoPane(null);
      }
      else {
        UI.setInfoPane(pane);
      }
    }
    
    
    protected void updateState() {
      final int numOld = pane.listing.oldMessages.size();
      setLabel(numOld+" Old Messages");
      super.updateState();
    }
  }
  
}





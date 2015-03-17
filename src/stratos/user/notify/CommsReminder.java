/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user.notify;
import stratos.graphics.widgets.*;
import stratos.graphics.common.*;
import stratos.user.*;
import stratos.util.*;



public class CommsReminder extends ReminderListing.Entry {
  
  
  final static ImageAsset
    COMM_IMAGE = ImageAsset.fromImage(
      CommsReminder.class, "media/GUI/Panels/comms_alert.png"
    );
  
  final DialoguePane message;
  
  
  CommsReminder(final BaseUI baseUI, final DialoguePane message) {
    super(baseUI, message, 40, 20);
    this.message = message;
    
    final Button button = new Button(
      baseUI, COMM_IMAGE.asTexture(), message.title
    ) {
      protected void whenClicked() {
        baseUI.setInfoPanels(message, null);
      }
    };
    button.stretch = false;
    button.alignToFill();
    button.attachTo(this);

    final BorderedLabel label = new BorderedLabel(baseUI);
    label.alignLeft  (0, 0);
    label.alignBottom(-DEFAULT_MARGIN, 0);
    label.text.scale = SMALL_FONT_SIZE;
    label.setMessage(message.title, false, 0);
    label.attachTo(this);
  }
  
}








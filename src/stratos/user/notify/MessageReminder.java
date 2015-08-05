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



public class MessageReminder extends LabelledReminder {
  
  
  final static ImageAsset
    COMM_IMAGE = ImageAsset.fromImage(
      MessageReminder.class, "media/GUI/Front/comms_alert.png"
    ),
    COMM_IMAGE_LIT = Button.DEFAULT_LIT;
  
  final MessagePane message;
  
  
  MessageReminder(BaseUI UI, Object refers, final MessagePane message) {
    super(
      UI, refers, message.title,
      COMM_IMAGE, COMM_IMAGE_LIT, message.title
    );
    this.message = message;
    setUrgent(true);
  }
  
  
  protected void whenButtonClicked() {
    UI.setMessagePane(message);
    setUrgent(false);
  }
}







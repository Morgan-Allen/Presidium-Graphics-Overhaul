/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user.notify;
import stratos.game.economic.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;



public class ResearchReminder extends LabelledReminder {
  
  
  //  TODO:  Use a unique icon here!
  final static ImageAsset
    SCIENCE_IMAGE = ImageAsset.fromImage(
      MessageReminder.class, "media/GUI/Front/comms_alert.png"
    ),
    SCIENCE_IMAGE_LIT = Button.DEFAULT_LIT;
  
  
  ResearchReminder(BaseUI UI, Upgrade refers) {
    super(
      UI, refers, refers.entryKey(),
      SCIENCE_IMAGE, SCIENCE_IMAGE_LIT, refers.baseName
    );
    setUrgent(false);
  }
  
  
  protected void updateState() {
    final Upgrade u = (Upgrade) refers;
    float remaining = UI.played().research.researchRemaining(u);
    int percent = (int) ((1 - remaining) * 100);
    
    setLabel("Researching "+u.name+": "+percent+"%");
    super.updateState();
  }
  
  
  protected void whenButtonClicked() {
    ((Upgrade) refers).whenClicked();
  }
}










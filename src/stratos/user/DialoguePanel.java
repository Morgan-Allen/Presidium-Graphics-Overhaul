/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.user;
import stratos.graphics.widgets.*;
import stratos.graphics.widgets.Text.Clickable;
import stratos.util.*;



public class DialoguePanel extends InfoPanel implements UIConstants {
  
  
  final String title;
  private String initText;
  private Clickable options[];
  
  
  public DialoguePanel(
    final BaseUI UI, final Composite portrait,
    String title, String initText, Clickable... options
  ) {
    super(UI, null, portrait, false);
    this.title = title;
    this.initText = initText;
    this.options = options;
  }
  
  
  public void assignContent(String initText, Clickable... options) {
    this.initText = initText;
    this.options = options;
  }
  
  
  protected void updateText(
    final BaseUI UI, Text headerText, Text detailText
  ) {
    headerText.setText(title);
    detailText.setText(initText);
    for (Clickable option : options) {
      detailText.append("\n  ");
      detailText.append(option);
    }
  }
}




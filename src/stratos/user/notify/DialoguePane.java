/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.user.notify;
import stratos.game.common.*;
import stratos.graphics.widgets.*;
import stratos.graphics.widgets.Text.Clickable;
import stratos.user.BaseUI;
import stratos.user.SelectionPane;
import stratos.user.UIConstants;
import stratos.util.*;



public class DialoguePane extends SelectionPane implements UIConstants {
  
  
  final String title;
  final Target focus;
  
  private String initText;
  private Clickable options[];
  
  
  
  public DialoguePane(
    BaseUI UI, Composite portrait,
    String title, String initText,
    Target focus,
    Series <? extends Clickable> options
  ) {
    this(
      UI, portrait, title, initText,
      focus, options.toArray(Clickable.class)
    );
  }
  
  
  public DialoguePane(
    BaseUI UI, Composite portrait,
    String title, String initText,
    Target focus,
    Clickable... options
  ) {
    super(UI, null, portrait, false);
    this.title    = title   ;
    this.focus    = focus   ;
    this.initText = initText;
    this.options  = options ;
  }
  
  
  public void assignContent(String initText, Clickable... options) {
    this.initText = initText;
    this.options = options;
  }
  
  
  public void assignContent(String initText, Series <Clickable> options) {
    assignContent(initText, options.toArray(Clickable.class));
  }
  
  
  protected void updateText(
    final BaseUI UI, Text headerText, Text detailText, Text listingText
  ) {
    headerText.setText(title);
    detailText.setText(initText);
    detailText.append("\n  ");
    for (Clickable option : options) {
      detailText.append("\n  ");
      detailText.append(option);
    }
  }
  
  
  public static boolean hasFocus(Target subject) {
    final BaseUI UI = BaseUI.current();
    if (UI == null) return false;
    if (UI.currentPane() instanceof DialoguePane) {
      final DialoguePane panel = (DialoguePane) UI.currentPane();
      return panel.focus == subject;
    }
    return false;
  }
}






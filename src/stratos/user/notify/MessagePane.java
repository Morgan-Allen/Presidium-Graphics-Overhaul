/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user.notify;
import stratos.game.common.*;
import stratos.graphics.widgets.*;
import stratos.graphics.widgets.Text.Clickable;
import stratos.user.*;
import stratos.util.*;



public class MessagePane extends SelectionPane implements UIConstants {
  
  
  final String title;
  final Target focus;
  final MessageSource source;
  
  private String initText;
  private Clickable options[];
  
  
  
  public MessagePane(
    BaseUI UI, Composite portrait,
    String title, String initText,
    Target focus, MessageSource source,
    Series <? extends Clickable> options
  ) {
    this(
      UI, portrait, title, initText,
      focus, source, options.toArray(Clickable.class)
    );
  }
  
  
  public MessagePane(
    BaseUI UI, Composite portrait,
    String title, String initText,
    Target focus, MessageSource source,
    Clickable... options
  ) {
    super(UI, null, portrait, false);
    this.title    = title   ;
    this.focus    = focus   ;
    this.source   = source  ;
    this.initText = initText;
    this.options  = options ;
  }
  
  
  public static interface MessageSource extends Session.Saveable {
    MessagePane messageFor(String title, BaseUI UI);
  }
  
  
  public static void saveMessage(
    MessagePane message, Session s
  ) throws Exception {
    if (message.source == null) {
      I.complain("\nNO SOURCE FOR MESSAGE: "+message.title);
    }
    s.saveObject(message.source);
    s.saveString(message.title );
  }
  
  
  public static MessagePane loadMessage(
    Session s, BaseUI UI
  ) throws Exception {
    final MessageSource source = (MessageSource) s.loadObject();
    final String titleKey = s.loadString();
    return source.messageFor(titleKey, UI);
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
    if (UI.currentPane() instanceof MessagePane) {
      final MessagePane panel = (MessagePane) UI.currentPane();
      return panel.focus == subject;
    }
    return false;
  }
}






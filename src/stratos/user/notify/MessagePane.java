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
  
  
  /**  Data fields, setup and save/load methods-
    */
  final String title;
  final Target focus;
  final MessageSource source;
  
  private boolean contentSet = false;
  private String initText;
  private Clickable options[];
  private float receiptDate = -1;
  
  
  public MessagePane(
    BaseUI UI, Composite portrait,
    String title, Target focus, MessageSource source
  ) {
    super(UI, null, portrait, false);
    this.title  = title ;
    this.focus  = focus ;
    this.source = source;
  }
  
  
  public static interface MessageSource extends Session.Saveable {
    MessagePane configMessage(String titleKey, BaseUI UI);
    void messageWasOpened(String titleKey, BaseUI UI);
  }
  
  
  public static void saveMessage(
    MessagePane message, Session s
  ) throws Exception {
    if (message.source == null) {
      I.complain("\nNO SOURCE FOR MESSAGE: "+message.title);
    }
    s.saveObject(message.source     );
    s.saveString(message.title      );
    s.saveFloat (message.receiptDate);
  }
  
  
  public static MessagePane loadMessage(
    Session s, BaseUI UI
  ) throws Exception {
    final MessageSource source = (MessageSource) s.loadObject();
    final String titleKey = s.loadString();
    final float  receipt  = s.loadFloat ();
    final MessagePane pane = source.configMessage(titleKey, UI);
    pane.receiptDate = receipt;
    return pane;
  }
  
  
  public MessagePane assignContent(String initText, Clickable... options) {
    this.contentSet = true    ;
    this.initText   = initText;
    this.options    = options ;
    return this;
  }
  
  
  public MessagePane assignContent(
    String initText, Series <? extends Clickable> links
  ) {
    return assignContent(initText, links.toArray(Clickable.class));
  }
  
  
  protected void assignReceiptDate(float time) {
    this.receiptDate = time;
  }
  
  
  
  /**  Update methods-
    */
  protected void updateText(
    BaseUI UI, Text headerText, Text detailText, Text listingText
  ) {
    if (contentSet) {
      super.updateText(UI, headerText, detailText, listingText);
      headerText.setText(title);
      final Text d = detailText;
      d.setText(initText);
      d.append("\n  ");
      for (Clickable option : options) {
        d.append("\n  ");
        d.append(option);
      }
    }
    else return;
  }
  
  
  protected float receiptDate() {
    return receiptDate;
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










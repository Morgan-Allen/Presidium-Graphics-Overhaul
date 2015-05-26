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
import static stratos.user.SelectionPane.*;


//  TODO:  Consider having this and SelectionPane extend a common ancestor, if
//  only for the frame-widgets and other UI constants?

public class MessagePane extends UIGroup implements UIConstants {
  
  
  /**  Data fields, setup and save/load methods-
    */
  final public String title;
  final public Target focus;
  final MessageSource source;
  
  final Bordering border;
  final Text header, detail;
  final Button closeButton;
  
  private boolean contentSet = false;
  private String initText;
  private Clickable options[];
  private float receiptDate = -1;
  
  
  public MessagePane(
    final BaseUI baseUI, Composite portrait,
    String title, Target focus, MessageSource source
  ) {
    super(baseUI);
    this.title  = title ;
    this.focus  = focus ;
    this.source = source;
    
    //  TODO:  Constrain this better?
    this.alignAcross(0, 1);
    this.alignDown  (0, 1);
    
    //  TODO:  Try to find some constants for these...
    this.border = new Bordering(baseUI, BORDER_TEX);
    border.left   = 20;
    border.right  = 20;
    border.bottom = 20;
    border.top    = 20;
    border.alignAcross(0, 1);
    border.alignDown  (0, 1);
    border.attachTo(this);
    
    this.header = new Text(baseUI, INFO_FONT);
    header.alignTop   (0, HEADER_HIGH);
    header.alignAcross(0, 1          );
    header.scale = BIG_FONT_SIZE;
    header.attachTo(border.inside);
    
    this.detail = new Text(baseUI, INFO_FONT);
    detail.alignVertical(0, HEADER_HIGH);
    detail.alignAcross  (0, 1          );
    detail.scale = SMALL_FONT_SIZE;
    detail.attachTo(border.inside);
    
    this.closeButton = new Button(
      baseUI, "close", WIDGET_CLOSE, WIDGET_CLOSE_LIT, "Close"
    ) {
      protected void whenClicked() {
        baseUI.clearMessagePane();
      }
    };
    closeButton.alignTop  (0, 30);
    closeButton.alignRight(0, 30);
    closeButton.attachTo(this);
  }
  
  
  public static interface MessageSource extends Session.Saveable {
    MessagePane configMessage(String titleKey, BaseUI UI);
    void messageWasOpened(String titleKey, BaseUI UI);
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
  
  
  public float receipt() {
    return receiptDate;
  }
  
  
  public Text header() {
    return header;
  }
  
  
  public Text detail() {
    return detail;
  }
  
  
  public String toString() {
    return title;
  }
  
  
  
  /**  Update methods-
    */
  protected void updateText(
    BaseUI UI, Text headerText, Text detailText, Text listingText
  ) {
    if (contentSet) {
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
}










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

  final   UINode    portraitFrame;
  private Composite portrait     ;
  
  final Bordering border;
  final Text header, detail;
  final Button closeButton;
  
  private boolean contentSet = false;
  private SelectionPane parent = null;
  
  private String initText;
  private Clickable options[];
  private float receiptDate = -1;
  
  
  public MessagePane(
    final BaseUI baseUI, final Composite portrait,
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
    final int rightInset;
    
    if (portrait != null) {
      portraitFrame = new UINode(baseUI) {
        protected void render(WidgetsPass batch2d) {
          portrait.drawTo(batch2d, bounds, absAlpha);
        }
      };
      portraitFrame.alignTop  (0, PORTRAIT_SIZE);
      portraitFrame.alignRight(0, PORTRAIT_SIZE);
      portraitFrame.attachTo(border.inside);
      rightInset = PORTRAIT_SIZE + DEFAULT_MARGIN;
    }
    else {
      this.portrait      = null;
      this.portraitFrame = null;
      rightInset = 0;
    }
    
    this.header = new Text(baseUI, INFO_FONT);
    header.alignTop       (0, HEADER_HIGH);
    header.alignHorizontal(0, rightInset );
    header.scale = BIG_FONT_SIZE;
    header.attachTo(border.inside);
    
    this.detail = new Text(baseUI, INFO_FONT);
    detail.alignVertical  (0, HEADER_HIGH);
    detail.alignHorizontal(0, rightInset );
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
  
  
  public MessagePane assignParent(SelectionPane parent) {
    this.parent = parent;
    return this;
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
  protected void updateState() {
    
    final BaseUI UI = BaseUI.current();
    if (parent == null) {
      closeButton.hidden = false;
    }
    else {
      if (UI.currentInfoPane() != parent && UI.currentMessage() == this) {
        UI.clearMessagePane();
      }
      closeButton.hidden = true;
    }
    
    if (contentSet) {
      header().setText(title);
      final Text d = detail();
      d.setText(initText);
      d.append("\n  ");
      for (Clickable option : options) {
        d.append("\n  ");
        d.append(option);
      }
    }
    super.updateState();
  }
  
  
  protected float receiptDate() {
    return receiptDate;
  }
}










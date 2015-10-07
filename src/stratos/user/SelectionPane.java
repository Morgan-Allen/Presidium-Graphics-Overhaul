/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.user;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;



//  TODO:  Adapt to work in an abbreviated horizontal format?  (More of a long-
//         term project.)

//  TODO:  Create proper, separately-instanced sub-panes for the different
//         categories...


public class SelectionPane extends UIGroup implements UIConstants {
  
  
  /**  Constants, fields and setup methods-
    */
  final public static ImageAsset
    BORDER_TEX = ImageAsset.fromImage(
      SelectionPane.class, "media/GUI/Front/Panel.png"
    ),
    SCROLL_TEX = ImageAsset.fromImage(
      SelectionPane.class, "media/GUI/scroll_grab.gif"
    ),
    WIDGET_BACK = ImageAsset.fromImage(
      SelectionPane.class, "media/GUI/Front/widget_back.png"
    ),
    WIDGET_BACK_LIT = ImageAsset.fromImage(
      SelectionPane.class, "media/GUI/Front/widget_back_lit.png"
    ),
    WIDGET_CLOSE = ImageAsset.fromImage(
      SelectionPane.class, "media/GUI/Front/widget_close.png"
    ),
    WIDGET_CLOSE_LIT = ImageAsset.fromImage(
      SelectionPane.class, "media/GUI/Front/widget_close_lit.png"
    ),
    WIDGET_INFO = ImageAsset.fromImage(
      SelectionPane.class, "media/GUI/Front/widget_info.png"
    ),
    WIDGET_INFO_LIT = ImageAsset.fromImage(
      SelectionPane.class, "media/GUI/Front/widget_info_lit.png"
    );
  final public static int
    CORE_INFO_HIGH = 160,
    PORTRAIT_SIZE  = 80 ;
  
  
  final protected BaseUI UI;
  final public Selectable selected;
  private SelectionPane previous;
  
  final Bordering border;
  final Text
    headerText ,
    detailText ,
    listingText;
  final Scrollbar scrollbar;

  final   UINode    portraitFrame;
  private Composite portrait     ;
  
  final   String categories[];
  final   float  catScrolls[];
  private int    categoryID  ;
  
  private Button backButton ;
  private Button infoButton ;
  private Button closeButton;
  

  public SelectionPane(
    BaseUI UI, Selectable selected, Composite portrait,
    boolean hasListing, String... categories
  ) {
    this(UI, selected, null, portrait != null, hasListing, 0, categories);
    this.portrait = portrait;
  }
  
  
  public SelectionPane(
    final BaseUI UI, SelectionPane previous, Composite portrait
  ) {
    this(UI, null, previous, portrait != null, false, 0);
    this.portrait = portrait;
  }
  
  
  public SelectionPane(
    final BaseUI baseUI,
    Selectable selected, SelectionPane previous,
    boolean hasPortrait, boolean hasListing, int topPadding,
    String... categories
  ) {
    super(baseUI);
    this.UI = baseUI;
    this.alignVertical(0, PANEL_TABS_HIGH);
    this.alignRight   (0, INFO_PANEL_WIDE);
    
    this.selected = selected;
    this.previous = previous;
    
    //
    //  Firstly set up the main border, framing elements, header and portrait
    //  view, if provided-
    final int TP = topPadding;
    int down = hasPortrait ? (PORTRAIT_SIZE + MARGIN_SIZE) : 0;
    down += HEADER_HIGH + TP;
    
    this.border = new Bordering(baseUI, BORDER_TEX);
    border.left   = 20;
    border.right  = 20;
    border.bottom = 20;
    border.top    = 20;
    border.alignAcross(0, 1);
    border.alignDown  (0, 1);
    border.attachTo(this);
    
    headerText = new Text(baseUI, BaseUI.INFO_FONT);
    headerText.alignTop   (TP, HEADER_HIGH);
    headerText.alignAcross(0 , 1          );
    headerText.scale = BIG_FONT_SIZE;
    headerText.attachTo(border.inside);
    
    if (hasPortrait) {
      portraitFrame = new UINode(baseUI) {
        protected void render(WidgetsPass batch2d) {
          if (portrait == null) return;
          portrait.drawTo(batch2d, bounds, absAlpha);
        }
      };
      portraitFrame.alignTop (HEADER_HIGH + TP, PORTRAIT_SIZE);
      portraitFrame.alignLeft(0               , PORTRAIT_SIZE);
      portraitFrame.attachTo(border.inside);
    }
    else {
      this.portrait      = null;
      this.portraitFrame = null;
    }
    
    //
    //  Set up the detail and, if applicable, listing-text widgets, along with
    //  the associated scrollbar-widget at the side:
    detailText = new Text(baseUI, BaseUI.INFO_FONT) {
      protected void whenLinkClicked(Clickable link) {
        super.whenLinkClicked(link);
        ((BaseUI) UI).beginPanelFade();
      }
    };
    detailText.scale = SMALL_FONT_SIZE;
    detailText.attachTo(border.inside);
    Text scrollParent = detailText;

    if (hasListing) {
      listingText = new Text(baseUI, BaseUI.INFO_FONT) {
        protected void whenLinkClicked(Clickable link) {
          super.whenLinkClicked(link);
          ((BaseUI) UI).beginPanelFade();
        }
      };
      listingText.alignVertical  (0, CORE_INFO_HIGH + down);
      listingText.alignHorizontal(0, 0);
      listingText.scale = SMALL_FONT_SIZE;
      listingText.attachTo(border.inside);
      
      detailText.alignHorizontal(0   , 0             );
      detailText.alignTop       (down, CORE_INFO_HIGH);
      scrollParent = listingText;
    }
    else {
      detailText.alignHorizontal(0, 0);
      detailText.alignVertical  (0, down          );
      listingText = null;
    }
    
    scrollbar = scrollParent.makeScrollBar(SCROLL_TEX);
    scrollbar.alignToMatch(scrollParent);
    scrollbar.alignRight(0 - SCROLLBAR_WIDE, SCROLLBAR_WIDE);
    scrollbar.attachTo(border.inside);
    
    //
    //  Then set up the category-headings and other, more generalised
    //  navigation-widgets-
    final SelectionPane pane = this;
    this.categories = categories;
    categoryID = defaultCategory();
    this.catScrolls = new float[categories == null ? 0 :categories.length];
    
    this.backButton = new Button(
      baseUI, "back", WIDGET_BACK, WIDGET_BACK_LIT, "Back"
    ) {
      protected void whenClicked() {
        baseUI.setInfoPane(pane.previous);
      }
    };
    backButton.alignTop  ( 0, 24);
    backButton.alignRight(28, 48);
    backButton.attachTo(this);
    
    this.closeButton = new Button(
      baseUI, "close", WIDGET_CLOSE, WIDGET_CLOSE_LIT, "Close"
    ) {
      protected void whenClicked() {
        if (pane.selected == baseUI.selection.selected()) {
          baseUI.clearOptionsList();
          baseUI.selection.pushSelection(null);
        }
        baseUI.clearInfoPane();
      }
    };
    closeButton.alignTop  (-3, 30);
    closeButton.alignRight( 0, 30);
    closeButton.attachTo(this);
    
    this.infoButton = new Button(
      baseUI, "info", WIDGET_INFO, WIDGET_INFO_LIT, "Info"
    ) {
      protected void whenClicked() {
        final Constant type = pane.selected.infoSubject();
        type.whenClicked();
      }
    };
    infoButton.alignTop  (27, 30);
    infoButton.alignRight( 0, 30);
    infoButton.attachTo(this);
  }
  
  
  public Text header() {
    return headerText;
  }
  
  
  public Text detail() {
    return detailText;
  }
  
  
  public Text listing() {
    return listingText;
  }
  
  
  
  /**  Utility methods for creating sub-panes and categories:
    */
  protected static class PaneButton extends Button {
    
    final BaseUI baseUI;
    final UIGroup pane;
    
    protected PaneButton(
      UIGroup pane, BaseUI baseUI,
      String widgetID, ImageAsset icon, ImageAsset iconLit, String helpInfo
    ) {
      super(baseUI, widgetID, icon, iconLit, helpInfo);
      this.baseUI = baseUI;
      this.pane   = pane  ;
    }

    protected void whenClicked() {
      if (baseUI.currentInfoPane() == pane) {
        baseUI.clearInfoPane();
      }
      else {
        baseUI.setInfoPane(pane);
        baseUI.clearOptionsList();
      }
    }
  }
  
  
  public void setPrevious(SelectionPane before) {
    this.previous = before;
  }
  
  
  
  /**  Handling category-selection:
    */
  final static Class SELECT_TYPES[] = {
    Venue.class,
    Actor.class,
    Vehicle.class,
    Object.class
  };
  private static Table <Class, String> defaults = new Table();
  
  
  private int defaultCategory() {
    if (selected == null) return 0;
    final String match = defaults.get(selectType());
    if (match == null) return 0;
    for (String s : categories) if (s.equals(match)) {
      return Visit.indexOf(s, categories);
    }
    return 0;
  }
  
  
  private Class selectType() {
    if (selected == null) return null;
    for (Class c : SELECT_TYPES) {
      if (selected.getClass().isAssignableFrom(c)) return c;
    }
    return null;
  }
  
  
  public String category() {
    if (categories.length == 0) return null;
    return categories[Nums.clamp(categoryID, categories.length)];
  }
  

  private void setCategory(int catID) {
    UI.beginPanelFade();
    
    catScrolls[categoryID] = 1 - scrollbar.scrollPos();
    this.categoryID = catID;
    scrollbar.setScrollPos(1 - catScrolls[categoryID]);
    
    if (selected != null) defaults.put(selectType(), categories[catID]);
  }
  
  
  
  /**  Display and updates-
    */
  protected void updateState() {
    updateText(UI, headerText, detailText, listingText);
    this.backButton.hidden = previous == null;
    
    if (selected != null) {
      selected.configSelectPane(this, UI);
      this.infoButton.hidden = selected.infoSubject() == null;
    }
    else {
      this.infoButton.hidden = true;
    }
    super.updateState();
  }
  
  
  protected void updateText(
    final BaseUI UI, Text headerText, Text detailText, Text listingText
  ) {
    if (selected != null) {
      headerText.setText(selected.fullName());
      headerText.append("\n");
    }
    else headerText.setText("");
    
    if (categories != null) {
      for (int i = 0; i < categories.length; i++) {
        final int index = i;
        final boolean CC = categoryID == i;
        headerText.append(new Text.Clickable() {
          public String fullName() { return ""+categories[index]+" "; }
          public void whenClicked() { setCategory(index); }
        }, CC ? Colour.GREEN : Text.LINK_COLOUR);
      }
    }
    
    detailText.setText("");
    if (listingText != null) listingText.setText("");
  }
}




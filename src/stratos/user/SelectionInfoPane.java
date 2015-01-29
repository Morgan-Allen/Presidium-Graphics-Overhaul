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



//  TODO:  Adapt this to work in either vertical or horizontal format.


public class SelectionInfoPane extends UIGroup implements UIConstants {
  
  
  
  /**  Constants, fields and setup methods-
    */
  final public static ImageAsset
    BORDER_TEX = ImageAsset.fromImage(
      SelectionInfoPane.class, "media/GUI/Panel.png"
    ),
    SCROLL_TEX = ImageAsset.fromImage(
      SelectionInfoPane.class, "media/GUI/scroll_grab.gif"
    );
  final public static int
    MARGIN_WIDTH  = 10,
    HEADER_HEIGHT = 35,
    PORTRAIT_SIZE = 80;
  
  
  final static Class INFO_CLASSES[] = {
    Vehicle.class,
    Actor.class,
    Venue.class
  };
  private static Table <Class, Integer> DEFAULT_CATS = new Table();
  
  private static Class infoClass(Selectable s) {
    if (s == null) return null;
    for (Class c : INFO_CLASSES) {
      if (c.isAssignableFrom(s.getClass())) return c;
    }
    return null;
  }
  
  
  
  final BaseUI UI;
  final Selectable selected;
  
  final Bordering border;
  final UIGroup
    innerRegion;
  final Text
    headerText ,
    detailText ,
    listingText;
  final Scrollbar scrollbar;

  final   UINode    portraitFrame;
  private Composite portrait     ;
  
  final   String categories[];
  private int    categoryID  ;
  

  public SelectionInfoPane(
    final BaseUI UI, Selectable selected,
    final Composite portrait, boolean hasListing,
    String... categories
  ) {
    this(UI, selected, portrait != null, hasListing, categories);
    this.portrait = portrait;
  }
  
  
  public SelectionInfoPane(
    final BaseUI UI, Selectable selected,
    boolean hasPortrait, boolean hasListing,
    String... categories
  ) {
    super(UI);
    this.UI = UI;
    this.alignTop       (0   , INFO_PANEL_HIGH   );
    this.alignHorizontal(0.5f, INFO_PANEL_WIDE, 0);
    
    final int across = hasPortrait ? (PORTRAIT_SIZE + 10) : 0;
    final int
      TM = 40, BM = 40,  //top and bottom margins
      LM = 40, RM = 40;  //left and right margins
    
    this.border = new Bordering(UI, BORDER_TEX);
    border.left   = LM;
    border.right  = RM;
    border.bottom = BM;
    border.top    = TM;
    border.alignAcross(0, 1);
    border.alignDown  (0, 1);
    border.attachTo(this);
    
    this.innerRegion = new UIGroup(UI);
    innerRegion.alignHorizontal(-15 + across, -15);
    innerRegion.alignVertical  (-15         , -15);
    innerRegion.attachTo(border.inside);
    
    headerText = new Text(UI, BaseUI.INFO_FONT);
    headerText.alignTop   (0, HEADER_HEIGHT);
    headerText.alignAcross(0, 1            );
    headerText.attachTo(innerRegion);

    detailText = new Text(UI, BaseUI.INFO_FONT) {
      protected void whenLinkClicked(Clickable link) {
        super.whenLinkClicked(link);
        ((BaseUI) UI).beginPanelFade();
      }
    };
    detailText.scale = 0.75f;
    detailText.attachTo(innerRegion);
    
    if (hasListing) {
      listingText = new Text(UI, BaseUI.INFO_FONT) {
        protected void whenLinkClicked(Clickable link) {
          super.whenLinkClicked(link);
          ((BaseUI) UI).beginPanelFade();
        }
      };
      listingText.alignVertical(0   , HEADER_HEIGHT);
      listingText.alignAcross  (0.5f, 1            );
      listingText.scale = 0.75f;
      listingText.attachTo(innerRegion);

      detailText.alignVertical(0, HEADER_HEIGHT);
      detailText.alignAcross  (0, 0.5f         );
      scrollbar = listingText.makeScrollBar(SCROLL_TEX);
    }
    else {
      listingText = null;
      detailText.alignVertical(0, HEADER_HEIGHT);
      detailText.alignAcross  (0, 1.0f         );
      scrollbar = detailText .makeScrollBar(SCROLL_TEX);
    }
    
    scrollbar.alignRight(0, SCROLLBAR_WIDE);
    scrollbar.alignDown (0, 1             );
    scrollbar.attachTo(innerRegion);
    
    this.selected = selected;
    this.categories = categories;
    categoryID = 0;
    
    final Class IC = infoClass(selected);
    if (IC != null && categories.length > 0) {
      final Integer catID = DEFAULT_CATS.get(IC);
      if (catID != null) categoryID = Nums.clamp(catID, categories.length);
    }
    else categoryID = 0;
    
    if (hasPortrait) {
      portraitFrame = new UINode(UI) {
        protected void render(WidgetsPass batch2d) {
          if (portrait == null) return;
          portrait.drawTo(batch2d, bounds, absAlpha);
        }
      };
      portraitFrame.alignTop (25, PORTRAIT_SIZE);
      portraitFrame.alignLeft(25, PORTRAIT_SIZE);
      portraitFrame.attachTo(border);
    }
    else {
      this.portrait = null;
      this.portraitFrame = null;
    }
  }
  
  
  public void assignPortrait(Composite portrait) {
    this.portrait = portrait;
  }
  
  
  protected Vec2D screenTrackPosition() {
    Vec2D middle = UI.trueBounds().centre();
    middle.y -= INFO_PANEL_HIGH / 2;
    return middle;
  }
  
  
  public int categoryID() {
    return categoryID;
  }
  
  
  public String category() {
    if (categories.length == 0) return null;
    return categories[Nums.clamp(categoryID, categories.length)];
  }
  
  
  public Description detail() {
    return detailText;
  }
  
  
  public Description listing() {
    return listingText;
  }
  
  
  
  /**  Display and updates-
    */
  private void setCategory(int catID) {
    UI.beginPanelFade();
    this.categoryID = catID;
    final Class IC = infoClass(selected);
    if (IC != null) DEFAULT_CATS.put(IC, catID);
  }
  
  
  protected void updateState() {
    updateText(UI, headerText, detailText, listingText);
    if (selected != null) selected.configPanel(this, UI);
    super.updateState();
  }
  
  
  protected void updateText(
    final BaseUI UI, Text headerText, Text detailText, Text listingText
  ) {
    if (selected != null) {
      headerText.setText(selected.fullName());
      headerText.append("\n");
    }
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
    
    detailText .setText("");
    if (listingText != null) listingText.setText("");
  }
}






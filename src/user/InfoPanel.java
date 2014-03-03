/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.user ;
import src.util.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;



public class InfoPanel extends UIGroup implements UIConstants {
  
  
  
  /**  Constants, fields and setup methods-
    */
  final public static ImageAsset BORDER_TEX = ImageAsset.fromImage(
    "media/GUI/Panel.png", InfoPanel.class
  ) ;
  final public static int
    DEFAULT_TOP_MARGIN = 50,
    MARGIN_WIDTH = 10,
    HEADER_HEIGHT = 35 ;
  
  final static Class INFO_CLASSES[] = {
    Vehicle.class,
    Actor.class,
    Venue.class
  } ;
  private static Table <Class, Integer> DEFAULT_CATS = new Table() ;
  
  private static Class infoClass(Selectable s) {
    if (s == null) return null ;
    for (Class c : INFO_CLASSES) {
      if (c.isAssignableFrom(s.getClass())) return c ;
    }
    return null ;
  }
  
  
  final protected BaseUI UI ;
  
  final Bordering border ;
  final Text headerText, detailText ;
  final protected Selectable selected ;
  
  private Selectable previous ;
  private int categoryID ;
  
  
  
  public InfoPanel(BaseUI UI, Selectable selected, int topMargin) {
    super(UI) ;
    this.UI = UI ;
    this.relBound.set(0, 0, 1, 1) ;
    
    this.absBound.set(20, 20, -40, -40) ;
    final int MW = MARGIN_WIDTH, HH = HEADER_HEIGHT ;
    this.border = new Bordering(UI, BORDER_TEX.asTexture());
    border.drawInset.set(-40, -40, 80, 80) ;
    border.absBound.set(MW, MW, -2 * MW, -2 * MW) ;
    border.relBound.set(0, 0, 1, 1) ;
    border.attachTo(this) ;
    
    headerText = new Text(UI, BaseUI.INFO_FONT) ;
    headerText.relBound.set(0, 1, 1, 0) ;
    headerText.absBound.set(0, -MW - (topMargin + HH), 0, HH) ;
    headerText.attachTo(this) ;
    
    detailText = new Text(UI, BaseUI.INFO_FONT) ;
    detailText.relBound.set(0, 0, 1, 1) ;
    detailText.absBound.set(0, MW, 0, (-2 * MW) - (topMargin + HH)) ;
    detailText.attachTo(this) ;
    //detailText.getScrollBar().attachTo(this) ;
    detailText.scale = 0.75f ;
    
    this.selected = selected ;
    final String cats[] = (selected == null) ?
      null : selected.infoCategories() ;

    categoryID = 0 ;
    final Class IC = infoClass(selected) ;
    if (IC != null) {
      final Integer catID = DEFAULT_CATS.get(IC) ;
      if (catID != null) categoryID = catID ;
    }
  }
  
  
  protected void setPrevious(Selectable previous) {
    this.previous = previous ;
  }
  
  
  protected Selectable previous() {
    return previous ;
  }
  
  
  
  /**  Display and updates-
    */
  private void setCategory(int catID) {
    UI.beginPanelFade() ;
    this.categoryID = catID ;
    final Class IC = infoClass(selected) ;
    if (IC != null) DEFAULT_CATS.put(IC, catID) ;
  }
  
  
  protected void updateState() {
    if (selected != null && selected.subject().destroyed()) {
      ///I.say("INFO SUBJECT IS DESTROYED.") ;
      UI.selection.pushSelection(previous, false) ;
      return ;
    }
    updateText(UI, headerText, detailText) ;
    super.updateState() ;
  }
  
  
  protected void updateText(
    final BaseUI UI, Text headerText, Text detailText
  ) {
    if (selected == null) return ;
    headerText.setText(selected.fullName()) ;
    
    headerText.append("\n") ;
    final String cats[] = selected.infoCategories() ;
    if (cats != null) {
      for (int i = 0 ; i < cats.length ; i++) {
        final int index = i ;
        final boolean CC = categoryID == i ;
        headerText.append(new Text.Clickable() {
          public String fullName() { return ""+cats[index]+" " ; }
          public void whenClicked() { setCategory(index) ; }
        }, CC ? Colour.GREEN : Text.LINK_COLOUR) ;
      }
    }
    if (previous != null) {
      headerText.append(new Description.Link("UP") {
        public void whenClicked() {
          UI.selection.pushSelection(previous, false) ;
        }
      }) ;
    }
    
    detailText.setText("") ;
    selected.writeInformation(detailText, categoryID, UI) ;
  }
}












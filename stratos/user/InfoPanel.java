/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.user ;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;



public class InfoPanel extends UIGroup implements UIConstants {
  
  
  
  /**  Constants, fields and setup methods-
    */
  final public static ImageAsset BORDER_TEX = ImageAsset.fromImage(
    "media/GUI/Panel.png", InfoPanel.class
  ) ;
  final public static int
    MARGIN_WIDTH  = 10,
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
  final UIGroup innerRegion;
  final Text headerText, detailText ;
  final protected Selectable selected ;
  
  private Selectable previous ;
  private int categoryID ;
  
  
  
  public InfoPanel(BaseUI UI, Selectable selected, int topPadding) {
    super(UI);
    this.UI = UI;
    this.relBound.set(0, 0, 1, 1);
    
    //  TODO:  Use topPadding again, once you have portrait composition done.
    final int
      TM = 40, BM = 40,  //top and bottom margins
      LM = 40, RM = 40;  //left and right margins
    
    this.border = new Bordering(UI, BORDER_TEX.asTexture());
    border.drawInset.set(LM, BM, -(LM + RM), -(BM + TM));
    border.relBound.set(0, 0, 1, 1);
    border.attachTo(this);
    
    this.innerRegion = new UIGroup(UI);
    innerRegion.relBound.set(0, 0, 1, 1);
    innerRegion.absBound.set(
      25, 20, -(25 + 20), -(20 + 25 + topPadding)
    );
    innerRegion.attachTo(this);
    
    headerText = new Text(UI, BaseUI.INFO_FONT);
    headerText.relBound.set(0, 1, 1, 0);
    headerText.absBound.set(
      0, -HEADER_HEIGHT,
      0, HEADER_HEIGHT
    );
    headerText.attachTo(innerRegion);
    
    detailText = new Text(UI, BaseUI.INFO_FONT);
    detailText.relBound.set(0, 0, 1, 1);
    detailText.absBound.set(
      0, BM,
      0, -(BM + HEADER_HEIGHT)
    );
    detailText.attachTo(innerRegion);
    detailText.scale = 0.75f;
    //detailText.getScrollBar().attachTo(this) ;
    
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












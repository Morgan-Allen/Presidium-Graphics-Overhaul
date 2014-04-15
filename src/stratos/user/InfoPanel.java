/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.user;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

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
    HEADER_HEIGHT = 35,
    PORTRAIT_SIZE = 80;
  
  
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
  
  
  
  final BaseUI UI ;
  final Selectable selected ;
  
  final Bordering border ;
  final UIGroup innerRegion;
  final Text headerText, detailText, spillText ;
  
  final String categories[];
  private Selectable previous ;
  private int categoryID ;

  final Composite portrait;
  final UINode portraitFrame;
  
  

  public InfoPanel(
    final BaseUI UI, Selectable selected,
    final Composite portrait,
    String... categories
  ) {
    this(UI, selected, portrait, true, categories);
  }
  
  
  protected InfoPanel(
    final BaseUI UI, Selectable selected,
    final Composite portrait, boolean splitText,
    String... categories
  ) {
    super(UI);
    this.UI = UI;
    this.relBound.set(0, 0, 1, 1);
    
    final int across = portrait == null ? 0 : PORTRAIT_SIZE + 10;
    final int
      TM = 40, BM = 40,  //top and bottom margins
      LM = 40, RM = 40;  //left and right margins
    
    this.border = new Bordering(UI, BORDER_TEX);
    border.left   = LM;
    border.right  = RM;
    border.bottom = BM;
    border.top    = TM;
    border.relBound.set(0, 0, 1, 1);
    border.attachTo(this);
    
    this.innerRegion = new UIGroup(UI);
    innerRegion.relBound.set(0, 0, 1, 1);
    innerRegion.absBound.set(
      25 + across, 20, -(25 + 20 + across), -(20 + 25)
    );
    innerRegion.attachTo(this);
    
    headerText = new Text(UI, BaseUI.INFO_FONT);
    headerText.relBound.set(0, 1, 1, 0);
    headerText.absBound.set(
      0, -HEADER_HEIGHT,
      0, HEADER_HEIGHT
    );
    headerText.attachTo(innerRegion);

    detailText = new Text(UI, BaseUI.INFO_FONT) {
      protected void whenLinkClicked(Clickable link) {
        super.whenLinkClicked(link);
        ((BaseUI) UI).beginPanelFade();
      }
    };
    detailText.absBound.set(
      0, BM,
      0, -(BM + HEADER_HEIGHT)
    );
    detailText.scale = 0.75f;
    
    if (splitText) {
      spillText = new Text(UI, BaseUI.INFO_FONT) {
        protected void whenLinkClicked(Clickable link) {
          super.whenLinkClicked(link);
          ((BaseUI) UI).beginPanelFade();
        }
      };
      spillText.absBound.set(
        0, BM,
        0, -(BM + HEADER_HEIGHT)
      );
      spillText.scale = 0.75f;
      
      detailText.relBound.set(0, 0, 0.5f, 1);
      spillText.relBound.set(0.5f, 0, 0.5f, 1);
      detailText.attachTo(innerRegion);
      spillText.attachTo(innerRegion);
    }
    else {
      spillText = null;
      detailText.relBound.set(0, 0, 1, 1);
      detailText.attachTo(innerRegion);
    }
    
    this.selected = selected;
    this.categories = categories;
    categoryID = 0;
    
    final Class IC = infoClass(selected) ;
    if (IC != null && categories.length > 0) {
      final Integer catID = DEFAULT_CATS.get(IC) ;
      if (catID != null) categoryID = Visit.clamp(catID, categories.length);
    }
    else categoryID = -1;
    
    if (portrait != null) {
      this.portrait = portrait;
      portraitFrame = new UINode(UI) {
        protected void render(SpriteBatch batch2d) {
          portrait.drawTo(batch2d, bounds, absAlpha);
        }
      };
      final int PS = PORTRAIT_SIZE;
      portraitFrame.relBound.set(0, 1, 0, 0);
      portraitFrame.absBound.set(25 + 5, -(5 + PS), PS, PS);
      portraitFrame.attachTo(this);
    }
    else {
      this.portrait = null;
      this.portraitFrame = null;
    }
  }
  
  
  protected void setPrevious(Selectable previous) {
    this.previous = previous ;
  }
  
  
  protected Selectable previous() {
    return previous ;
  }
  
  
  
  public int categoryID() {
    return categoryID;
  }
  
  
  public Description detail() {
    return detailText;
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
    if (selected != null && selected.selectionLocksOn().destroyed()) {
      UI.selection.pushSelection(previous, false) ;
      return ;
    }
    updateText(UI, headerText, detailText);
    if (selected != null) selected.configPanel(this, UI);
    if (spillText != null) detailText.continueWrap(spillText);
    //  TODO:  Otherwise, attach a scroll bar.
    super.updateState() ;
  }
  
  
  protected void updateText(
    final BaseUI UI, Text headerText, Text detailText
  ) {
    if (selected != null) {
      headerText.setText(selected.fullName()) ;
      headerText.append("\n") ;
    }
    if (categories != null) {
      for (int i = 0 ; i < categories.length ; i++) {
        final int index = i ;
        final boolean CC = categoryID == i ;
        headerText.append(new Text.Clickable() {
          public String fullName() { return ""+categories[index]+" " ; }
          public void whenTextClicked() { setCategory(index) ; }
        }, CC ? Colour.GREEN : Text.LINK_COLOUR) ;
      }
    }
    if (selected != null && previous != null) {
      headerText.append(new Description.Link("UP") {
        public void whenTextClicked() {
          UI.selection.pushSelection(previous, false) ;
        }
      }) ;
    }
    detailText.setText("");
  }
}





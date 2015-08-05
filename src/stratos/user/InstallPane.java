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


//  Okay.  A single panel.  Grid-style layout.  Either no tabs, or tabs as
//  options.

//  TODO:  Allow a general summary of demand for structures of this type.
//  TODO:  Expand a little on the category-selection system?


public class InstallPane extends SelectionPane {
  
  
  /**  Initial background setup-
    */
  final static ImageAsset
    BUILD_ICON = ImageAsset.fromImage(
      InstallPane.class, "media/GUI/Panels/installations_tab.png"
    ),
    BUILD_ICON_LIT = Button.CIRCLE_LIT;
  
  
  static Button createButton(final BaseUI baseUI) {
    return new PaneButton(
      new InstallPane(baseUI), baseUI,
      INSTALL_BUTTON_ID, BUILD_ICON, BUILD_ICON_LIT,
      "Installations"
    );
  }
  
  
  InstallPane(BaseUI UI) {
    super(UI, null, null, false, false, 0);
    setWidgetID(INSTALL_PANE_ID);
    if (! setupDone) setupTypes();
  }
  
  
  
  /**  Setting up categories and their buttons-
    */
  final static String DEFAULT_CATEGORY = TYPE_SECURITY;
  
  static class Category {
    String name;
    List <Blueprint> belong = new List();
    boolean toggled = false;
  }
  
  static Table <String, Category> categories = new Table <String, Category> ();
  static List <Blueprint> allBlueprints = new List();
  static boolean setupDone = false;
  static Blueprint lastSelected = null;
  
  
  protected static void setupTypes() {
    initCategory(TYPE_SECURITY );
    initCategory(TYPE_COMMERCE );
    initCategory(TYPE_ENGINEER );
    initCategory(TYPE_ECOLOGIST);
    
    for (Blueprint blueprint : Blueprint.allBlueprints()) {
      final Category category = categories.get(blueprint.category);
      if (category == null) continue;
      category.belong.add(blueprint);
      allBlueprints.add(blueprint);
    }
    setupDone = true;
  }
  
  
  private static void initCategory(String typeID) {
    final Category category = new Category();
    category.name = typeID;
    category.toggled = true;
    categories.put(typeID, category);
  }
  
  
  
  /**  Regular updates and placement-kickoff:
    */
  protected void updateText(
    final BaseUI UI, Text headerText, Text detailText, Text listingText
  ) {
    headerText.setText("Install Structures");
    detailText.setText("");
    
    final Base base = UI.played();
    for (String catName : INSTALL_CATEGORIES) {
      final Category c = categories.get(catName);
      if (! c.toggled) continue;
      
      detailText.append(c.name+" Structures\n");
      
      for (Blueprint b : c.belong) {
        boolean enabled = base.checkPrerequisites(b, Account.NONE);
        describeVenueOptions(b, detailText, enabled, base);
      }
      detailText.append("\n");
    }
    
    if (lastSelected != null) describeCurrentType(lastSelected, detailText);
  }
  

  private void describeVenueOptions(
    final Blueprint type, Text text, boolean enabled, Base base
  ) {
    final Composite icon = Composite.withImage(type.icon, type.keyID);
    if (icon == null) return;
    
    final Button b = new Button(UI, type.keyID, icon.texture(), type.name) {
      protected void whenClicked() { toggleSelected(type, this); }
    };
    b.setDisabledOverlay(Image.TRANSLUCENT_BLACK);
    b.enabled = enabled;
    b.toggled = type == PlacingTask.currentPlaceType();
    Text.insert(b, 40, 40, false, text);
  }
  
  
  private void toggleSelected(Blueprint type, Button b) {
    if (b.enabled) UI.beginTask(new PlacingTask(UI, type));
    lastSelected = type;
  }
  
  
  private void describeCurrentType(Blueprint type, Text text) {
    final int cost = type.buildCost;
    
    text.append("\n\n");
    text.append(type.name+" ");
    Text.insert(
      SelectionPane.WIDGET_INFO.asTexture(),
      15, 15, type, false, text
    );
    text.append(" ("+cost+" credits)", Colour.LITE_GREY);
    
    text.append("\n\n");
    text.append(type.description);
    text.append("\n\n");
  }
  
}





  
  /*
  private void setupCategoryButtons() {
    final UIGroup bar = new UIGroup(UI);
    bar.attachTo(border.inside);
    bar.alignToFill();
    
    for (int i = 0; i < NUM_INSTALL_CATEGORIES; i++) {
      final String catName = INSTALL_CATEGORIES[i];
      
      final Button button = new Button(
        UI, catName, GUILD_IMAGE_ASSETS[i], null
      ) {
        
        protected void whenClicked() {
          final BaseUI UI = BaseUI.current();
          UI.beginPanelFade();
          final Category match = categories.get(catName);
          match.toggled = ! match.toggled;
          this.toggled  = match.toggled;
        }
        
        protected String info() {
          if (toggled) return "Filter Off";
          else return "Filter "+catName+" Structures";
        }
      };
      button.stretch = true;
      catButtons[i] = button;
      
      final int
        barW = INFO_PANEL_WIDE - 50,
        wide = (int) (barW / INSTALL_CATEGORIES.length);
      button.alignTop(0, BAR_BUTTON_SIZE - 10);
      button.alignLeft  ((wide * i), wide);
      button.attachTo(bar);
    }
  }
  //*/






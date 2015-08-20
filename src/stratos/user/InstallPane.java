/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
import stratos.game.base.*;
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
    BUILD_ICON_LIT = Button.CROSSHAIRS_LIT,
    
    STATE_FRAMES[] = ImageAsset.fromImages(
      InstallPane.class, "media/GUI/Buttons/",
      "selected_icon_frame.png"   ,
      "prototype_icon_frame.png"  ,
      "research_icon_frame.png"   ,
      "theoretical_icon_frame.png"
    ),
    SELECTED_FRAME    = STATE_FRAMES[0],
    PROTOTYPE_FRAME   = STATE_FRAMES[1],
    RESEARCH_FRAME    = STATE_FRAMES[2],
    THEORETICAL_FRAME = STATE_FRAMES[3];

  
  
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
    
    for (String s : INSTALL_CATEGORIES) initCategory(s);
    
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
    
    final List <Blueprint> sorting = new List <Blueprint> () {
      protected float queuePriority(Blueprint r) {
        if (r.baseUpgrade() == null) return -100;
        return r.baseUpgrade().tier;
      }
    };
    
    for (String catName : INSTALL_CATEGORIES) {
      final Category c = categories.get(catName);
      if (! c.toggled) continue;
      
      sorting.clear();
      for (Blueprint b : allBlueprints) {
        if (! b.category.equals(catName)) continue;
        if (b.icon == null || b.baseUpgrade() == null) continue;
        if (! b.baseUpgrade().hasRequirements(base)) continue;
        
        //if (! base.research.allows(b.baseUpgrade())) continue;
        sorting.add(b);
      }
      if (sorting.empty()) continue;
      else sorting.queueSort();
      
      detailText.append("\n "+c.name+" Structures\n");
      final int MAX_IN_ROW = 4;
      int numInRow = 0;
      
      for (Blueprint b : sorting) {
        describeVenueOptions(b, detailText, base);
        if ((++numInRow % MAX_IN_ROW) == 0) detailText.append("\n");
      }
      detailText.append("\n");
    }
    
    if (lastSelected != null) {
      boolean enabled = base.checkPrerequisites(lastSelected, Account.NONE);
      describeCurrentType(lastSelected, base, enabled, detailText);
    }
  }
  

  private void describeVenueOptions(
    final Blueprint type, Text text, Base base
  ) {
    final Composite icon = Composite.withImage(type.icon, type.keyID);
    if (icon == null || type.baseUpgrade() == null) return;
    
    final Upgrade forType = type.baseUpgrade();
    final int state = (int) base.research.getResearchLevel(forType);
    
    final Button b = new Button(UI, type.keyID, icon.texture(), type.name) {
      protected void whenClicked() { toggleSelected(type, this, state); }
    };
    
    if (state <= BaseResearch.LEVEL_ALLOWS) {
      if (forType.researchDone(base) != null) {
        b.addOverlay(THEORETICAL_FRAME);
        b.addOverlay(RESEARCH_FRAME);
      }
      else {
        b.addOverlay(THEORETICAL_FRAME);
      }
    }
    if (state == BaseResearch.LEVEL_THEORY) {
      b.addOverlay(PROTOTYPE_FRAME);
    }
    b.setHighlight(SELECTED_FRAME.asTexture());
    
    b.toggled = type == lastSelected;
    text.append(" ");
    Text.insert(b, 40, 40, false, text);
  }
  
  
  private void toggleSelected(Blueprint type, Button b, int state) {
    UI.beginPanelFade();
    UI.endCurrentTask();
    lastSelected = type;
  }
  
  
  private void describeCurrentType(
    final Blueprint type, final Base base, boolean enabled, Text text
  ) {
    text.append("\n\n");
    text.append(type.name+" ");
    Text.insert(
      SelectionPane.WIDGET_INFO.asTexture(),
      15, 15, type, false, text
    );
    ///text.append("\n  Tier: "+type.baseUpgrade().tier);
    
    final Upgrade basis = type.baseUpgrade();
    if (basis != null) {
      text.append("\n");
      basis.describeResearchStatus(text, base);
    }
    text.append("\n\n");
    text.append(type.description);
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






/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.craft.*;
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
  final public static ImageAsset
    BUILD_ICON = ImageAsset.fromImage(
      InstallPane.class, "install_pane_icon",
      "media/GUI/Panels/installations_tab.png"
    ),
    BUILD_ICON_LIT = Button.CROSSHAIRS_LIT,
    
    STATE_FRAMES[] = ImageAsset.fromImages(
      InstallPane.class, "install_pane_research_state_frames",
      "media/GUI/Buttons/",
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
    setupCategoryButtons();
  }
  
  
  private void setupCategoryButtons() {
    final UIGroup bar = new UIGroup(UI);
    bar.attachTo(border.inside);
    bar.alignHorizontal(0, 5);
    bar.alignTop(20, 20);
    
    final Button catButtons[] = new Button[MAIN_INSTALL_CATEGORIES.length];
    
    for (int i = 0; i < catButtons.length; i++) {
      final String catName = MAIN_INSTALL_CATEGORIES[i];
      
      final Button button = new Button(
        UI, catName, GUILD_IMAGE_ASSETS[i], null
      ) {
        
        protected void whenClicked() {
          BaseUI.beginPanelFade();
          final Category match = categories.get(catName);
          match.toggled = ! match.toggled;
        }
        
        protected boolean toggled() {
          final Category match = categories.get(catName);
          return ! match.toggled;
        }
        
        protected String info() {
          if (toggled()) return "Show "+catName+" structures";
          else return "Hide "+catName+" structures";
        }
      };
      button.stretch = true;
      button.setHighlight(Image.TRANSLUCENT_BLACK.asTexture());
      catButtons[i] = button;
      
      final float divW = 1f / catButtons.length;
      button.alignVertical(0, 0);
      button.alignAcross((i + 0.1f) * divW, (i + 1f) * divW);
      button.attachTo(bar);
    }
  }
  
  
  
  /**  Setting up categories and their buttons-
    */
  final static String DEFAULT_CATEGORY = Target.TYPE_SECURITY;
  
  static class Category {
    String name;
    List <Blueprint> belong = new List();
    boolean toggled = false;
  }
  
  static Table <String, Category> categories = new Table();
  static List <Blueprint> allBlueprints = new List();
  static boolean setupDone = false;
  static Blueprint lastSelected = null;
  
  
  protected static void setupTypes() {
    for (String s : MAIN_INSTALL_CATEGORIES) initCategory(s);
    
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
    Text headerText, Text detailText, Text listingText
  ) {
    headerText.setText("Install Structures");
    detailText.setText("");
    final Base base = BaseUI.currentPlayed();
    
    final List <Blueprint> sorting = new List <Blueprint> () {
      protected float queuePriority(Blueprint r) {
        if (r.baseUpgrade() == null) return -100;
        int catIndex = Visit.indexOf(r.category, MAIN_INSTALL_CATEGORIES);
        return (catIndex * 100) + r.baseUpgrade().tier;
      }
    };
    final List <Blueprint>
      current  = new List(),
      possible = new List(),
      listed   = new List();
    
    for (Blueprint b : allBlueprints) {
      if (b.icon == null || b.baseUpgrade() == null) continue;
      if (! b.baseUpgrade().hasRequirements(base)) continue;
      
      Category c = categories.get(b.category);
      if (c != null && ! c.toggled) continue;
      if (base.research.hasTheory(b.baseUpgrade())) {
        
        //  TODO:  This only comes up in the case where a unique building (e.g,
        //  the bastion) is already placed.
        //  Find a more elegant way to present this!
        final boolean allowed = base.checkPrerequisites(b, Account.NONE);
        if (allowed) current.add(b);
      }
      else possible.add(b);
    }
    
    sorting.clear();
    Visit.appendTo(sorting, current);
    sorting.queueSort();
    Visit.appendTo(listed, sorting);
    
    sorting.clear();
    Visit.appendTo(sorting, possible);
    sorting.queueSort();
    Visit.appendTo(listed, sorting);
    
    for (Blueprint b : listed) {
      Text.cancelBullet(detailText);
      describeVenueOptions(b, detailText, base);
    }
  }
  

  private void describeVenueOptions(
    final Blueprint type, Text text, Base base
  ) {
    //*
    final Composite icon = Composite.withImage(type.icon, type.keyID);
    if (icon == null || type.baseUpgrade() == null) return;
    
    final Upgrade forType = type.baseUpgrade();
    final int state = (int) base.research.getResearchLevel(forType);
    
    final Image b = new Image(UI, icon.texture());
    b.setWidgetID(type.keyID);
    if (state <= BaseResearch.LEVEL_ALLOWS) {
      if (forType.researchDone(base) != null) {
        b.addOverlay(THEORETICAL_FRAME);
        b.addOverlay(RESEARCH_FRAME);
      }
      else {
        b.addOverlay(Image.TRANSLUCENT_BLACK);
        b.addOverlay(THEORETICAL_FRAME);
      }
    }
    if (state == BaseResearch.LEVEL_THEORY) {
      b.addOverlay(PROTOTYPE_FRAME);
    }
    
    Text.insert(b, 40, 40, true, text);
    
    if (forType != null) {
      forType.appendBaseOrders(text, base);
    }
  }
}



  
  
  /*
  private void toggleSelected(Blueprint type, Button b, int state) {
    UI.beginPanelFade();
    UI.endCurrentTask();
    if (lastSelected == type) lastSelected = null;
    else lastSelected = type;
  }
  
  
  private void describeCurrentType(
    final Blueprint type, final Base base, boolean enabled, Text text
  ) {
    text.append("\n\n");
    
    final Upgrade basis = type.baseUpgrade();
    if (basis != null) {
      basis.appendBaseOrders(text, base);
    }
    else {
      text.append(type.name+" ");
      Text.insert(
        SelectionPane.WIDGET_INFO.asTexture(),
        15, 15, type, false, text
      );
    }
    
    text.append("\n     ");
    text.append(new Description.Link("Remove Type") {
      public void whenClicked() {
        allBlueprints.remove(type);
      }
    });
    
    text.append("\n\n");
    text.append(type.description);
  }
  //*/



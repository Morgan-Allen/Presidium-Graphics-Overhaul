/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.user;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;

import com.badlogic.gdx.Input.Keys;



//  TODO:  Allow listing of current structures, and greyed-out options.
//  TODO:  Allow a general summary of demand for structures of this type.
//  TODO:  Expand a little on the category-selection system?


public class InstallationPane extends SelectionPane {
  
  
  /**  Initial background setup-
    */
  final static ImageAsset GUILD_IMAGE_ASSETS[] = ImageAsset.fromImages(
    Quickbar.class, BUTTONS_PATH,
    "militant_category_button.png",
    "merchant_category_button.png",
    "aesthete_category_button.png",
    "artificer_category_button.png",
    "ecologist_category_button.png",
    "physician_category_button.png"
  );
  
  final static ImageAsset
    BUILD_ICON = ImageAsset.fromImage(
      InstallationPane.class, "media/GUI/Panels/installations_tab.png"
    ),
    BUILD_ICON_LIT = Button.CIRCLE_LIT;
  
  
  
  /**  Interface presented-
    */
  public static Button createButton(
    final BaseUI baseUI
  ) {
    final InstallationPane pane = new InstallationPane(baseUI);
    return new Button(baseUI, BUILD_ICON, BUILD_ICON_LIT, "Installations") {
      protected void whenClicked() {
        if (baseUI.currentPane() == pane) {
          baseUI.setInfoPanels(null, null);
        }
        else {
          baseUI.setInfoPanels(pane, null);
        }
      }
    };
  }
  
  
  private Venue helpFor;
  private Category category = null;
  final private Button catButtons[];
  
  
  InstallationPane(BaseUI UI) {
    super(UI, null, false, false, BAR_BUTTON_SIZE);
    if (! setupDone) setupTypes();
    
    catButtons = new Button[NUM_GUILDS];
    setupCategoryButtons();
  }
  
  
  
  /**  Setting up categories and their buttons-
    */
  final static String DEFAULT_CATEGORY = TYPE_SECURITY;
  
  static class Category {
    String name;
    List <Venue> samples = new List <Venue> ();
  }
  
  static Table <String, Category> categories = new Table <String, Category> ();
  static List <Venue> allSampled = new List <Venue> ();
  static boolean setupDone = false;
  
  
  protected static void setupTypes() {
    initCategory(TYPE_SECURITY );
    initCategory(TYPE_COMMERCE );
    initCategory(TYPE_AESTHETIC);
    initCategory(TYPE_ENGINEER );
    initCategory(TYPE_ECOLOGIST);
    initCategory(TYPE_PHYSICIAN);
    
    for (Blueprint blueprint : Blueprint.allBlueprints()) {
      final Venue sample = blueprint.createVenue(null);
      final String catName = sample.objectCategory();
      final Category category = categories.get(catName);
      
      if (category != null) {
        category.samples.add(sample);
        allSampled.add(sample);
      }
    }
    setupDone = true;
  }
  
  
  public static String categoryFor(Blueprint venueType) {
    for (Category c : categories.values()) {
      for (Venue v : c.samples) if (v.blueprint == venueType) return c.name;
    }
    return null;
  }
  
  
  private static void initCategory(String typeID) {
    final Category category = new Category();
    category.name = typeID;
    categories.put(typeID, category);
  }
  
  
  protected static List <Venue> samplesForCategory(String typeID) {
    return categories.get(typeID).samples;
  }
  
  
  private void setupCategoryButtons() {
    final UIGroup bar = new UIGroup(UI);
    bar.attachTo(innerRegion);
    bar.alignToFill();
    
    for (int i = 0; i < NUM_GUILDS; i++) {
      final String catName = INSTALL_CATEGORIES[i];
      
      final Button button = new Button(UI, GUILD_IMAGE_ASSETS[i], null) {
        
        protected void whenClicked() {
          final BaseUI UI = BaseUI.current();
          UI.beginPanelFade();
          final Category match = categories.get(catName);
          category = match;
          helpFor = null;
          
          for (Button b : catButtons) {
            if (b == this) b.toggled = true;
            else b.toggled = false;
          }
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
  
  
  
  /**  Regular updates and placement-kickoff:
    */
  protected void updateText(
    final BaseUI UI, Text headerText, Text detailText, Text listingText
  ) {
    if (category == null) category = categories.get(DEFAULT_CATEGORY);
    String name = category.name;
    headerText.setText(name+" Structures");
    detailText.setText("");
    
    final Base base = UI.played();
    final Series <Venue> sampled = new List <Venue> ();
    
    for (Category cat : categories.values()) {
      if (category != null && cat != category) continue;
      for (Venue sample : cat.samples) {
        if (! base.checkPrerequisites(sample.blueprint, Account.NONE)) continue;
        else sampled.add(sample);
      }
    }
    
    final Text text = detailText;
    
    if (helpFor != null) {
      final Venue     sample   = helpFor            ;
      final Blueprint type     = sample.blueprint   ;
      final Composite icon     = sample.portrait(UI);
      final String    typeName = type.name          ;
      final String    typeDesc = sample.helpInfo()  ;
      final int       cost     = sample.structure.buildCost();
      
      if (icon != null) Text.insert(icon.texture(), 80, 80, false, text);
      text.append("\n\nFacility Name: "+typeName);
      text.append("\nBuild cost: "+cost);
      text.append("\n\n");
      text.append(typeDesc, Colour.LITE_GREY);
      text.append("\n");
      if (type.required.length > 0) {
        text.appendList("\nRequires: ", (Object[]) type.required);
      }
      if (type.allows().size() > 0) {
        text.appendList("\nAllows: ", type.allows());
      }
      
      final Batch <Venue> built = base.listInstalled(type, false);
      text.append("\n\nCurrently Built:");
      if (built.size() == 0) text.append(" None");
      else for (Venue v : built) {
        text.append("\n  ");
        text.append(v);
      }
      
      text.append("\n\n");
      text.append(new Description.Link("Back") {
        public void whenClicked() { helpFor = null; }
      });
    }
    else if (sampled.size() == 0) {
      text.append("No structures available!");
    }
    else for (final Venue sample : sampled) {
      final Composite icon     = sample.portrait(UI);
      final Blueprint type     = sample.blueprint   ;
      final String    typeName = type.name          ;
      final int       cost     = sample.structure.buildCost();
      
      if (icon != null) Text.insert(icon.texture(), 40, 40, true, text);
      else text.append("\n  ");
      text.append(" "+typeName+" ("+cost+" credits)");
      text.append("\n  ");
      text.append(new Description.Link("(BUILD) ") {
        public void whenClicked() { UI.beginTask(new PlacingTask(UI, type)); }
      });
      text.append(new Description.Link("(INFO) ") {
        public void whenClicked() { helpFor = sample; }
      });
    }
  }
}











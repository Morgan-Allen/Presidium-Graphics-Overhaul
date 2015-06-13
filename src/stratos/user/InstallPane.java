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



//  TODO:  Allow a general summary of demand for structures of this type.
//  TODO:  Expand a little on the category-selection system?


public class InstallPane extends SelectionPane {
  
  
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
      InstallPane.class, "media/GUI/Panels/installations_tab.png"
    ),
    BUILD_ICON_LIT = Button.CIRCLE_LIT;
  
  
  
  /**  Interface presented-
    */
  static Button createButton(final BaseUI baseUI) {
    return new PaneButton(
      new InstallPane(baseUI), baseUI,
      INSTALL_BUTTON_ID, BUILD_ICON, BUILD_ICON_LIT, "Installations"
    );
  }
  
  
  private Category category = null;
  final private Button catButtons[];
  
  
  InstallPane(BaseUI UI) {
    super(UI, null, null, false, false, BAR_BUTTON_SIZE);
    setWidgetID(INSTALL_PANE_ID);
    
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
    category.name  = typeID;
    categories.put(typeID, category);
  }
  
  
  protected static List <Venue> samplesForCategory(String typeID) {
    return categories.get(typeID).samples;
  }
  
  
  private void setupCategoryButtons() {
    final UIGroup bar = new UIGroup(UI);
    bar.attachTo(border.inside);
    bar.alignToFill();
    
    for (int i = 0; i < NUM_GUILDS; i++) {
      final String catName = INSTALL_CATEGORIES[i];
      
      final Button button = new Button(
        UI, catName, GUILD_IMAGE_ASSETS[i], null
      ) {
        
        protected void whenClicked() {
          final BaseUI UI = BaseUI.current();
          UI.beginPanelFade();
          final Category match = categories.get(catName);
          category = match;
          
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
  
  
  public String category() {
    return category == null ? null : category.name;
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
    final Series <Venue> sampled = new List(), disabled = new List();
    
    for (Category cat : categories.values()) {
      if (category != null && cat != category) continue;
      for (Venue sample : cat.samples) {
        final Account reasons = new Account();
        base.checkPrerequisites(sample.blueprint, reasons);
        //
        //  TODO:  Have a more nuanced evaluation for skipping disabled venues.
        if (reasons.wasSuccess()) sampled.add(sample);
        else if (! sample.blueprint.isUnique()) disabled.add(sample);
      }
    }
    
    final Text text = detailText;
    
    if (sampled.empty() && disabled.empty()) {
      text.append("No structures available!");
    }
    else {
      for (final Venue sample : sampled) {
        describeVenueOptions(sample, text, true , base);
      }
      for (final Venue sample : disabled) {
        describeVenueOptions(sample, text, false, base);
      }
    }
  }
  
  
  private void describeVenueOptions(
    final Venue sample, Text text, boolean enabled, Base base
  ) {
    final Composite icon     = sample.portrait(UI);
    final Blueprint type     = sample.blueprint   ;
    final String    typeName = type.name          ;
    final int       cost     = sample.structure.buildCost();
    final Colour    greyed   = enabled ? Colour.WHITE : Colour.GREY;
    
    if (icon != null) {
      final Image iconImage = icon.delayedImage(UI, type.keyID);
      iconImage.setDisabledOverlay(Image.TRANSLUCENT_BLACK);
      iconImage.enabled = enabled;
      Text.insert(iconImage, 40, 40, true, text);
    }
    else text.append("\n  ");
    text.append(" "+typeName);
    text.append("  ");
    Text.insert(
      SelectionPane.WIDGET_INFO.asTexture(), 15, 15,
      sample.blueprint, false, text
    );
    text.append("\n");
    
    final String buildDesc = " (BUILD)";
    if (enabled) text.append(new Description.Link(buildDesc) {
      public void whenClicked() { UI.beginTask(new PlacingTask(UI, type)); }
    });
    else text.append(buildDesc, greyed);
    text.append(" ("+cost+" credits)", Colour.LITE_GREY);
    text.append("\n");
  }
}











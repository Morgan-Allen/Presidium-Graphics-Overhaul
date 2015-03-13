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
//import stratos.graphics.terrain.*;
import stratos.util.*;
import stratos.game.economic.Inventory.Owner;

import com.badlogic.gdx.Input.Keys;


//  TODO:  Allow listing of current structures, and greyed-out options.
//  TODO:  Allow a general summary of demand for structures of this type.
//  TODO:  Expand a little on the category-selection system.


public class InstallationPane extends SelectionInfoPane {
  
  
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
    FOOTPRINT_TEX = ImageAsset.fromImage(
      InstallationPane.class, "media/GUI/blank_back.png"
    ),
    BUILD_ICON_LIT = Button.CIRCLE_LIT;
  
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
    
    for (VenueProfile profile : VenueProfile.allProfiles()) {
      final Venue sample = profile.sampleVenue(null);
      
      final String catName = sample.objectCategory();
      final Category category = categories.get(catName);
      if (category != null) {
        category.samples.add(sample);
        allSampled.add(sample);
      }
    }
    setupDone = true;
  }
  
  
  private static void initCategory(String typeID) {
    final Category category = new Category();
    category.name = typeID;
    categories.put(typeID, category);
  }
  
  
  protected static List <Venue> samplesForCategory(String typeID) {
    return categories.get(typeID).samples;
  }
  
  
  
  /**  Interface presented-
    */
  private VenueProfile helpFor;
  private Category category = null;
  
  
  InstallationPane(BaseUI UI) {
    super(UI, null, true, true);
    if (! setupDone) setupTypes();
    
    final UIGroup bar = new UIGroup(UI);
    bar.alignToMatch(detailText);
    bar.attachTo(innerRegion);
    
    for (int i = 0; i < NUM_GUILDS; i++) {
      final String
        help    = INSTALL_CATEGORIES[i]+" Structures",
        catName = INSTALL_CATEGORIES[i];
      
      final Button button = new Button(UI, GUILD_IMAGE_ASSETS[i], help) {
        protected void whenClicked() {
          final BaseUI UI = BaseUI.current();
          UI.beginPanelFade();
          final Category match = categories.get(catName);
          if (match != null && category == match) category = null;
          else { category = match; helpFor = null; }
        }
      };
      button.stretch = true;
      final int
        barW = INFO_PANEL_WIDE - 50,
        wide = (int) (barW / INSTALL_CATEGORIES.length);
      
      button.alignBottom(10, BAR_BUTTON_SIZE - 10);
      button.alignLeft  ((wide * i), wide);
      button.attachTo(bar);
    }
  }

  
  
  final Batch <Structure.Basis> listInstalled(BaseUI UI, Class type) {
    Batch <Structure.Basis> installed = new Batch <Structure.Basis> ();
    final Tile zero = UI.world().tileAt(0, 0);
    final Presences presences = UI.world().presences;
    for (Object o : presences.matchesNear(type, zero, -1)) {
      if (! (o instanceof Structure.Basis)) continue;
      installed.add((Structure.Basis) o);
    }
    return installed;
  }
  
  
  protected void updateText(
    final BaseUI UI, Text headerText, Text detailText, Text listingText
  ) {
    String name = category == null ? "All" : category.name;
    headerText.setText(name+" Structures");
    detailText .setText("");
    listingText.setText("");
    
    final Series <Venue> sampled = new List <Venue> ();
    for (Category cat : categories.values()) {
      if (category != null && cat != category) continue;
      for (Venue sample : cat.samples) {
        if (checkPrerequisites(sample, UI.played().world) != null) continue;
        else sampled.add(sample);
      }
    }
    
    if (sampled.size() == 0) {
      detailText.append("No structures available!");
    }
    else for (final Venue sample : sampled) {
      if (helpFor == null) helpFor = sample.profile;
      
      final Composite otherIcon = sample.portrait(UI);
      if (otherIcon != null) listingText.insert(
        otherIcon.texture(), 40, new Description.Link(sample.fullName()) {
          public void whenClicked() {
            helpFor = sample.profile;
          }
        }, false
      );
      listingText.append(" ");
    }
    
    if (helpFor != null) {
      final Venue sample = helpFor.sampleVenue(UI.played());
      final Composite icon      = sample.portrait(UI);
      final String    typeName  = sample.fullName()  ;
      final String    typeDesc  = sample.helpInfo()  ;
      final VenueProfile type   = sample.profile;
      
      assignPortrait(icon);
      detailText.append(typeName+"  ");
      detailText.append(new Description.Link("(BUILD)") {
        public void whenClicked() { initInstallTask(UI, type); }
      });
      
      detailText.append("\n\n");
      detailText.append(typeDesc, Colour.LITE_GREY);
      if (type.required.length > 0) {
        detailText.appendList("\nRequires: ", (Object[]) type.required);
      }
      if (type.allows().size() > 0) {
        detailText.appendList("\nAllows: ", type.allows());
      }
    }
  }
  
  
  /**  Getting a listing of current structures-
    */
  private Batch <Venue> listInstalled(
    VenueProfile type, Stage world, boolean intact
  ) {
    final Batch <Venue> installed = new Batch <Venue> ();
    for (Object o : world.presences.matchesNear(type.baseClass, null, -1)) {
      final Venue v = (Venue) o;
      if (intact && ! v.structure.intact()) continue;
      if (v.base() == UI.played()) installed.add(v);
    }
    return installed;
  }
  
  
  //  TODO:  MOVE THIS OUT TO THE VENUEPROFILE OR BASESETUP CLASS!
  
  private String checkPrerequisites(Venue sample, Stage world) {
    if (sample.owningTier() == Owner.TIER_UNIQUE) {
      if (listInstalled(sample.profile, world, false).size() > 0) {
        return "You cannot have more than one "+sample;
      }
    }
    for (VenueProfile req : sample.profile.required) {
      if (listInstalled(req, world, true).size() == 0) return "Requires "+req;
    }
    return null;
  }
  
  
  private List <Tile> tilesAround(final Tile picked, int radius) {
    final Stage world = picked.world;
    final Box2D area = picked.area(null).expandBy(radius);
    
    final List <Tile> sorting = new List <Tile> () {
      protected float queuePriority(Tile r) {
        return Spacing.distance(r, picked);
      }
    };
    for (Tile t : world.tilesIn(area, true)) sorting.add(t);
    sorting.queueSort();
    return sorting;
  }
  
  
  
  /**  Actual placement of buildings-
    */
  private void initInstallTask(BaseUI UI, VenueProfile type) {
    final Venue venue = type.sampleVenue(UI.played());
    if (venue == null) return;
    else venue.assignBase(UI.played());
    
    final InstallTask task = new InstallTask();
    task.tab  = this;
    task.UI   = UI;
    task.type = type;
    task.toInstall = venue;
    UI.beginTask(task);
  }
  
  
  class InstallTask implements UITask {
    
    InstallationPane tab;
    BaseUI UI;
    VenueProfile type;
    Venue toInstall;
    private boolean onStage, canPlace;
    private String message;
    
    
    public void doTask() {
      final Tile picked = UI.selection.pickedTile();
      if (picked == null) return;
      
      onStage = canPlace = false;
      findPlacePointFrom(picked, 2);
      if (! onStage) return;
      
      if ((message = checkPrerequisites(toInstall, picked.world)) != null) {
        canPlace = false;
      }
      if (UI.played().finance.credits() < toInstall.structure().buildCost()) {
        canPlace = false;
        if (message == null) message = "Insufficient funds!";
      }
      if (! canPlace) {
        onStage &= toInstall.setPosition(picked.x, picked.y, picked.world);
        if (! onStage) return;
        
        //  TODO:  Get an appropriate message from the canPlace() method?  It
        //  might be un-buildable terrain, or overlapping a road-buffer-zone,
        //  for example.
        if (message == null) message = "Too close to another structure!";
      }
      
      final Structure.Basis group[] = toInstall.structure().asGroup();
      final int tier = toInstall.owningTier();
      boolean confirmed = UI.mouseClicked() || KeyInput.wasTyped(Keys.ENTER);
      boolean multiples = (group.length > 1 || tier <= Owner.TIER_PRIVATE);
      
      if (canPlace && confirmed) {
        for (Structure.Basis i : group) i.doPlacement();
        UI.endCurrentTask();
        if (multiples) tab.initInstallTask(UI, type);
        if (I.logEvents()) I.say("\nPLACED "+toInstall+" AT "+picked);
      }
      
      else for (Structure.Basis i : group) {
        i.previewPlacement(canPlace, UI.rendering);
        if (canPlace) message =
          "(Enter to place, Esc to cancel, E to change entrance)"
        ;
      }
      BaseUI.setPopupMessage(message);
      
      if (KeyInput.wasTyped('e')) {
        toInstall.setFacing(toInstall.facing() + 1);
      }
      
      //
      //  Finally, render a suitable terrain overlay to show the entrance:
      final Object o[];
      if (group.length == 1) {
        o = new Object[] { toInstall, toInstall.mainEntrance() };
      }
      else o = group;
      
      UI.selection.renderTileOverlay(
        UI.rendering, picked.world,
        canPlace ? Colour.SOFT_GREEN : Colour.SOFT_RED,
        FOOTPRINT_TEX, "install_preview", false, o
      );
    }
    
    
    private boolean findPlacePointFrom(final Tile picked, int radius) {
      //  TODO:  Hook directly into the utility methods in Placement.class.
      
      final IntelMap map = UI.played().intelMap;
      final Stage world = picked.world;
      final int HS = toInstall.size / 2;
      
      for (Tile t : tilesAround(picked, radius)) {
        canPlace = true;
        canPlace &= toInstall.setPosition(t.x - HS, t.y - HS, world);
        if (! canPlace) continue;
        onStage = true;
        
        //  We then determine whether all the components of that larger
        //  structure are in fact place-able:
        final Structure.Basis group[] = toInstall.structure().asGroup();
        for (Structure.Basis i : group) {
          canPlace &= map.fogAt(i) > 0 && i.canPlace();
        }
        if (canPlace) return true;
      }
      return false;
    }
    
    
    public void cancelTask() {
      UI.endCurrentTask();
    }
    
    
    public ImageAsset cursorImage() {
      return null;
    }
    
    
    public String toString() {
      return "Installing "+toInstall;
    }
  }
}



//  TODO:  DERIVE THIS FROM BASESETUP OR VENUEPROFILE!
/*
for (final Venue other : category.samples) {
  final String    otherName = other.fullName()  ;
  final Composite otherIcon = other.portrait(UI);
  final int       otherCost = other.buildCost() ;
  
  listingText.append("\n  ");
  if (otherIcon != null) {
    listingText.insert(otherIcon.texture(), 40);
  }
  listingText.append("  ");
  listingText.append(new Description.Link(otherName) {
    public void whenTextClicked() {
      helpShown = other.getClass();
    }
  });
  //  TODO:  List Claim area, structure type, etc.?
  listingText.append("\n  Cost: "+otherCost+" credits");
}
//*/



/*
  detailText.append(new Text.Clickable() {
    public void whenTextClicked() {
      listShown = (type == listShown) ? null : type;
      helpShown = null;
    }
    public String fullName() { return "  (LIST)"; }
  });
  detailText.append(new Text.Clickable() {
    public void whenTextClicked() {
      helpShown = (type == helpShown) ? null : type;
      listShown = null;
    }
    public String fullName() { return "  (INFO)"; }
  });
  
  if (listShown == type) {
    final Batch <Structure.Basis> installed = listInstalled(UI, type);
    int ID = 0;
    if (installed.size() == 0) {
      detailText.append("\n  (no current installations)");
    }
    else for (Structure.Basis i : installed) {
      detailText.append("\n    ");
      final String label = i.fullName()+" No. "+(++ID);
      detailText.append(label, (Text.Clickable) i);
      //  You might also list location.
    }
}
//*/
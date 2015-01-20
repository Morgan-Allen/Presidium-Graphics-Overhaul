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



public class InstallTab extends SelectionInfoPane {
  
  
  /**  Initial background setup-
    */
  static class Category {
    String name;
    List <Venue> samples = new List <Venue> ();
  }
  
  static Table <String, Category> categories = new Table <String, Category> ();
  static List <Venue> allSampled = new List <Venue> ();
  static boolean setupDone = false;
  
  
  protected static void setupTypes() {
    initCategory(TYPE_MILITANT );
    initCategory(TYPE_MERCHANT );
    initCategory(TYPE_AESTHETE );
    initCategory(TYPE_ARTIFICER);
    initCategory(TYPE_ECOLOGIST);
    initCategory(TYPE_PHYSICIAN);
    
    for (VenueProfile profile : VenueProfile.facilityProfiles()) {
      final Venue sample = profile.sampleVenue(null);
      if (sample.privateProperty()) continue;
      
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
  final Category category;
  private Class helpShown = null, listShown = null;
  
  
  InstallTab(BaseUI UI, String catName) {
    super(UI, null, true, true);
    if (! setupDone) setupTypes();
    this.category = categories.get(catName);
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
  
  
  //  TODO:  This could use some elaboration...
  
  protected void updateText(
    final BaseUI UI, Text headerText, Text detailText, Text listingText
  ) {
    final String name = category.name;
    detailText .setText("");
    headerText .setText("");
    listingText.setText("");
    
    final Series <Venue> sampled = category.samples;
    if (helpShown == null) helpShown = sampled.first().getClass();
    final Venue sample = (Venue) Visit.matchFor(helpShown, sampled);
    
    final Composite icon      = sample.portrait(UI);
    final String    typeName  = sample.fullName()  ;
    final String    typeDesc  = sample.helpInfo()  ;
    final Class     type      = sample.getClass()  ;

    headerText.setText(name+" structures");
    assignPortrait(icon);
    
    detailText.append("\n"+typeName+"\n\n");
    detailText.append(typeDesc, Colour.LITE_GREY);
    detailText.append("\n\n");
    detailText.append(new Description.Link("(BUILD)") {
      public void whenClicked() { initInstallTask(UI, type); }
    });
    //  TODO:  Allow listing of current structures.
    //  TODO:  Allow a general summary of demand for structures of this type.
    //  TODO:  Give multiple options for listing the various structure types!
    
    listingText.append("All Types: \n");
    for (final Venue other : sampled) {
      final Composite otherIcon = other.portrait(UI);
      if (otherIcon == null) continue;
      
      listingText.insert(
        otherIcon.texture(), 40, new Description.Link(other.fullName()) {
          public void whenClicked() {
            helpShown = other.getClass();
          }
        }, false
      );
      listingText.append(" ");
    }
  }
  
  
  
  /**  Actual placement of buildings-
    */
  private void initInstallTask(BaseUI UI, Class type) {
    final Venue venue = VenueProfile.sampleVenue(type, null);
    if (venue == null) return;
    else venue.assignBase(UI.played());
    
    final InstallTask task = new InstallTask();
    task.tab  = this;
    task.UI   = UI;
    task.type = type;
    task.toInstall = venue;
    UI.beginTask(task);
  }
  
  
  static class InstallTask implements UITask {
    
    InstallTab tab;
    BaseUI UI;
    Class type;
    Structure.Basis toInstall;
    
    
    public void doTask() {
      final IntelMap map = UI.played().intelMap;
      final Tile picked = UI.selection.pickedTile();
      
      //  First of all, we place the main structure at the selected tile (which
      //  gives it the chance to update the location of any kids it has.)
      boolean canPlace =
        picked != null &&
        toInstall.setPosition(picked.x, picked.y, picked.world);
      
      //  TODO:  Hook directly into the utility methods in Placement.class?
      
      //  We then determine whether all the components of that larger structure
      //  are in fact place-able:
      final Structure.Basis group[] = toInstall.structure().asGroup();
      for (Structure.Basis i : group) {
        canPlace &= map.fogAt(i) > 0 && i.canPlace();
      }
      if (canPlace && UI.mouseClicked()) {
        for (Structure.Basis i : group) i.doPlacement();
        UI.endCurrentTask();
        if (group[0].structure().isFixture()) tab.initInstallTask(UI, type);
        else UI.selection.pushSelection(group[0], true);
      }
      else for (Structure.Basis i : group) {
        i.previewPlacement(canPlace, UI.rendering);
      }
      
      if (! canPlace) {
        //  TODO:  Get the appropriate message from the canPlace() method.
        String message = "Too close to another structure!";
        BaseUI.setPopupMessage(message);
      }
    }
    
    
    public void cancelTask() {
      UI.endCurrentTask();
    }
    
    
    public ImageAsset cursorImage() {
      return null;
    }
  }
}




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
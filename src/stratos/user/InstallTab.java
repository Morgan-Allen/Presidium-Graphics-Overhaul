/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.user;
import stratos.game.building.*;
import stratos.game.campaign.*;
import stratos.game.common.*;
import stratos.game.tactical.*;
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
  static boolean setupDone = false;
  
  
  protected static void setupTypes() {
    initCategory(TYPE_MILITANT );
    initCategory(TYPE_MERCHANT );
    initCategory(TYPE_AESTHETE );
    initCategory(TYPE_ARTIFICER);
    initCategory(TYPE_ECOLOGIST);
    initCategory(TYPE_PHYSICIAN);
    for (Class baseClass : BaseSetup.venueTypes()) {
      //
      //  Construct the building type with an appropriate instance.
      final Venue sample = VenueProfile.sampleVenue(baseClass);
      if (sample.privateProperty()) continue;
      final String catName = sample.buildCategory();
      final Category category = categories.get(catName);
      if (category != null) category.samples.add(sample);
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
    super(UI, null, null);
    if (! setupDone) setupTypes();
    this.category = categories.get(catName);
  }
  
  
  final Batch <Installation> listInstalled(BaseUI UI, Class type) {
    Batch <Installation> installed = new Batch <Installation> ();
    final Tile zero = UI.world().tileAt(0, 0);
    final Presences presences = UI.world().presences;
    for (Object o : presences.matchesNear(type, zero, -1)) {
      if (! (o instanceof Installation)) continue;
      installed.add((Installation) o);
    }
    return installed;
  }
  
  
  protected void updateText(
    final BaseUI UI, Text headerText, Text detailText
  ) {
    detailText.setText("");
    headerText.setText("");
    final String name = category.name;
    headerText.setText(name+" structures:");
    
    for (final Venue sample : category.samples) {
      final Composite icon      = sample.portrait(UI);
      final String    typeName  = sample.fullName()  ;
      final String    typeDesc  = sample.helpInfo()  ;
      final int       buildCost = sample.buildCost() ;
      final Class     type      = sample.getClass();
      
      if (icon != null) detailText.insert(icon.texture(), 40);
      detailText.append("  "+typeName);
      detailText.append("\n Cost: "+buildCost+" Credits");
      //  TODO:  List construction materials too?
      
      detailText.append(new Text.Clickable() {
        public void whenTextClicked() { initInstallTask(UI, type); }
        public String fullName() { return "\n  (BUILD)"; }
      });
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
      
      if (helpShown == type) {
        detailText.append("\n");
        detailText.append(typeDesc);
        detailText.append("\n");
      }
      if (listShown == type) {
        final Batch <Installation> installed = listInstalled(UI, type);
        int ID = 0;
        if (installed.size() == 0) {
          detailText.append("\n  (no current installations)");
        }
        else for (Installation i : installed) {
          detailText.append("\n    ");
          final String label = i.fullName()+" No. "+(++ID);
          detailText.append(label, (Text.Clickable) i);
          //  You might also list location.
        }
        detailText.append("\n");
      }
      detailText.append("\n\n");
    }
  }
  
  
  
  /**  Actual placement of buildings-
    */
  private void initInstallTask(BaseUI UI, Class type) {
    final Venue venue = VenueProfile.sampleVenue(type);
    if (venue == null) return;
    else venue.assignBase(UI.played());
    
    final InstallTask task = new InstallTask();
    task.UI = UI;
    task.type = type;
    task.toInstall = venue;
    UI.beginTask(task);
  }
  
  
  static class InstallTask implements UITask {
    
    BaseUI UI;
    Class type;
    Installation toInstall;
    private boolean hasPressed = false;
    Tile from, to;
    
    
    public void doTask() {
      final IntelMap map = UI.played().intelMap;
      Tile picked = UI.selection.pickedTile();
      //if (picked != null && map.fogAt(picked) == 0) picked = null;
      
      if (hasPressed) {
        if (picked != null) to = picked;
      }
      else {
        if (picked != null) to = from = picked;
        if (UI.mouseDown() && from != null) {
          hasPressed = true;
        }
      }
      //
      //  TODO:  Consider a different rendering mode for stuck-in-fog.
      final boolean canPlace =
        (from == null || map.fogAt(from) > 0) &&
        (to   == null || map.fogAt(to  ) > 0) &&
        toInstall.pointsOkay(from, to);
      
      if (canPlace && hasPressed && ! UI.mouseDown()) {
        toInstall.doPlace(from, to);
        UI.endCurrentTask();
      }
      else {
        toInstall.preview(canPlace, UI.rendering, from, to);
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



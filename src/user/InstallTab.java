/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.user ;
import src.game.common.* ;
import src.game.tactical.* ;
import src.game.building.* ;
import src.util.* ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;
import java.lang.reflect.* ;




public class InstallTab extends InfoPanel {

  
  /**  Field, constant and internal class definitions-
    */
  static class InstallType {
    Class <Installation> buildClass ;
    Constructor buildCons ;
    Installation sample ;
  }
  
  
  static class Category {
    String name ;
    final List <InstallType> types = new List <InstallType> () ;
  }
  

  static Table <String, Category> categories = new Table <String, Category> () ;
  static Batch <InstallType> allTypes = new Batch <InstallType> () ;
  static boolean setupDone = false ;
  
  
  
  /**  Initial background setup-
    */
  protected static void setupTypes() {
    initCategory(TYPE_MILITANT ) ;
    initCategory(TYPE_MERCHANT ) ;
    initCategory(TYPE_AESTHETE ) ;
    initCategory(TYPE_ARTIFICER) ;
    initCategory(TYPE_ECOLOGIST) ;
    initCategory(TYPE_PHYSICIAN) ;
    for (Class baseClass : LoadService.loadPackage("src.game.base")) {
      //
      //  Firstly, we need to ensure that the class refers to a type of venue
      //  and has an appropriate constructor.
      if (! Installation.class.isAssignableFrom(baseClass)) continue ;
      final Constructor cons ;
      try { cons = baseClass.getConstructor(Base.class) ; }
      catch (Exception e) { continue ; }
      //
      //  Secondly, construct the building type with an appropriate instance.
      final InstallType type = new InstallType() ;
      type.buildClass = baseClass ;
      type.buildCons = cons ;
      refreshSample(type, null) ;
      allTypes.add(type) ;
      final String catName = type.sample.buildCategory() ;
      final Category category = categories.get(catName) ;
      if (category != null) category.types.add(type) ;
    }
    setupDone = true ;
  }
  
  
  private static void initCategory(String typeID) {
    final Category category = new Category() ;
    category.name = typeID ;
    categories.put(typeID, category) ;
  }
  
  
  private static void refreshSample(InstallType type, Base base) {
    try {
      type.sample = (Installation) type.buildCons.newInstance(base) ;
    }
    catch (Exception e) {
      I.say("PROBLEM REFRESHING SAMPLE OF: "+type.buildCons.getName()) ;
      I.report(e) ;
    }
  }
  
  
  protected static List <InstallType> typesForCategory(String typeID) {
    return categories.get(typeID).types ;
  }
  
  
  

  
  /**  Interface presented-
    */
  final Category category ;
  private InstallType helpShown = null, listShown = null ;
  
  
  InstallTab(BaseUI UI, String catName) {
    super(UI, null, 0) ;
    if (! setupDone) setupTypes() ;
    this.category = categories.get(catName) ;
  }
  
  
  
  protected void updateText(
    final BaseUI UI, Text headerText, Text detailText
  ) {
    detailText.setText("") ;
    headerText.setText("") ;
    final String name = category.name ;
    headerText.setText(name+" structures:") ;
    for (final InstallType type : category.types) {
      final Composite icon = type.sample.portrait(UI) ;
      final String typeName = type.sample.fullName() ;
      final String typeDesc = type.sample.helpInfo() ;
      final int buildCost = type.sample.buildCost() ;
      
      if (icon != null) detailText.insert(icon.texture(), 40) ;
      detailText.append("  "+typeName) ;
      detailText.append("\n Cost: "+buildCost+" Credits") ;
      //  TODO:  List construction materials too?
      
      detailText.append(new Text.Clickable() {
        public void whenClicked() { initInstallTask(UI, type) ; }
        public String fullName() { return "\n  (BUILD)" ; }
      }) ;
      detailText.append(new Text.Clickable() {
        public void whenClicked() {
          listShown = (type == listShown) ? null : type ;
          helpShown = null ;
        }
        public String fullName() { return "  (LIST)" ; }
      }) ;
      detailText.append(new Text.Clickable() {
        public void whenClicked() {
          helpShown = (type == helpShown) ? null : type ;
          listShown = null ;
        }
        public String fullName() { return "  (INFO)" ; }
      }) ;
      
      if (helpShown == type) {
        detailText.append("\n") ;
        detailText.append(typeDesc) ;
        detailText.append("\n") ;
      }
      if (listShown == type) {
        final Batch <Installation> installed = listInstalled(UI, type) ;
        int ID = 0 ;
        if (installed.size() == 0) {
          detailText.append("\n  (no current installations)") ;
        }
        else for (Installation i : installed) {
          detailText.append("\n    ") ;
          final String label = i.fullName()+" No. "+(++ID) ;
          detailText.append(label, (Text.Clickable) i) ;
          //  You might also list location.
        }
        detailText.append("\n") ;
      }
      detailText.append("\n\n") ;
    }
  }
  
  
  Batch <Installation> listInstalled(BaseUI UI, InstallType type) {
    Batch <Installation> installed = new Batch <Installation> () ;
    final Tile zero = UI.world().tileAt(0, 0) ;
    final Presences presences = UI.world().presences ;
    for (Object o : presences.matchesNear(type.buildClass, zero, -1)) {
      if (! (o instanceof Installation)) continue ;
      installed.add((Installation) o) ;
    }
    return installed ;
  }
  
  
  
  /**  Actual placement of buildings-
    */
  static void initInstallTask(BaseUI UI, InstallType type) {
    refreshSample(type, UI.played()) ;
    final InstallTask task = new InstallTask() ;
    task.UI = UI ;
    task.type = type ;
    task.toInstall = type.sample ;
    UI.beginTask(task) ;
    refreshSample(type, null) ;
  }
  
  
  static class InstallTask implements UITask {
    
    
    BaseUI UI ;
    InstallType type ;
    Installation toInstall ;
    private boolean hasPressed = false ;
    Tile from, to ;
    
    
    public void doTask() {
      final IntelMap map = UI.played().intelMap ;
      Tile picked = UI.selection.pickedTile() ;
      //if (picked != null && map.fogAt(picked) == 0) picked = null ;
      
      if (hasPressed) {
        if (picked != null) to = picked ;
      }
      else {
        if (picked != null) to = from = picked ;
        if (UI.mouseDown() && from != null) {
          hasPressed = true ;
        }
      }
      //
      //  TODO:  Consider a different rendering mode for stuck-in-fog.
      final boolean canPlace =
        (from == null || map.fogAt(from) > 0) &&
        (to   == null || map.fogAt(to  ) > 0) &&
        toInstall.pointsOkay(from, to) ;
      
      if (canPlace && hasPressed && ! UI.mouseDown()) {
        toInstall.doPlace(from, to) ;
        if (! GameSettings.buildFree) {
          final float cost = toInstall.buildCost() ;
          toInstall.inventory().incCredits(cost) ;
          toInstall.inventory().taxDone() ;
          UI.played().incCredits(0 - cost) ;
        }
        UI.endCurrentTask() ;
        if (toInstall instanceof Segment) {
          initInstallTask(UI, type) ;
        }
      }
      else {
        //  TODO:  RESTORE THIS
        //UI.rendering.clearDepth();
        toInstall.preview(canPlace, UI.rendering, from, to) ;
      }
    }
    
    
    public void cancelTask() {
      UI.endCurrentTask() ;
    }
    
    
    public ImageAsset cursorImage() {
      return null ;
    }
  }
}



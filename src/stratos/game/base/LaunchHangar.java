

package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;





public class LaunchHangar extends Venue {
  
  
  
  /**  Constructors, data fields, setup and save/load methods-
    */
  final static String IMG_DIR = "media/Buildings/merchant/";
  final public static ModelAsset MODEL = CutoutModel.fromSplatImage(
    LaunchHangar.class, IMG_DIR+"landing_strip.png", 4.25f
  );
  
  
  private Dropship docking;
  
  
  
  public LaunchHangar(FRSD belongs) {
    super(4, 1, ENTRANCE_NORTH, belongs.base());
    structure.setupStats(50, 10, 25, 0, Structure.TYPE_FIXTURE);
    attachModel(MODEL);
  }
  

  public LaunchHangar(Session s) throws Exception {
    super(s);
    docking = (Dropship) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(docking);
  }
  
  
  
  /**  Owning/pathing modifications-
    */
  /*
  public int owningType() {
    return VENUE_OWNS;
  }
  
  
  public int pathType() {
    return Tile.PATH_BLOCKS;
  }
  
  
  public boolean privateProperty() {
    return true;
  }
  
  
  public boolean canPlace() {
    if (! super.canPlace()) return false;
    if (! Spacing.adjacent(this, belongs)) return false;
    return true;
  }
  
  
  protected boolean canTouch(Element e) {
    return (e.owningType() < this.owningType()) || e == belongs;
  }
  //*/
  
  
  
  /**  Docking functions-
    */
  public Boarding[] canBoard(Boarding batch[]) {
    final Batch <Boarding> CB = new Batch <Boarding> ();
    if (mainEntrance() != null) CB.add(mainEntrance());
    if (docking != null) CB.add(docking);
    int i = 2; for (Mobile m : inside()) if (m instanceof Boarding) {
      CB.add((Boarding) m);
    }
    return CB.toArray(Boarding.class);
  }
  
  
  public Dropship docking() {
    return docking;
  }
  
  
  public void setToDock(Dropship ship) {
    docking = ship;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates);
    if (! structure.intact()) return;
    if (docking != null && ! docking.inWorld()) docking = null;
  }
  
  
  public Service[] services() { return null; }
  
  public Background[] careers() { return null; }
  
  public Behaviour jobFor(Actor actor) { return null; }
  
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(FRSD.ICON, "landing_strip");
  }
  
  
  public String fullName() {
    return "Launch Hangar";
  }
  
  
  public String helpInfo() {
    return
      "The Launch Hangar allows you to establish a local air force, while "+
      "providing smaller dropships with a convenient site to land and "+
      "refuel, so facilitating offworld trade and migration.";
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_MILITANT;
  }
  
  
  /*
  public TargetInfo configInfo(TargetInfo info, BaseUI UI) {
    return belongs.configInfo(info, UI);
  }
  
  
  public InfoPanel configPanel(InfoPanel panel, BaseUI UI) {
    return belongs.configPanel(panel, UI);
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    belongs.renderSelection(rendering, hovered);
  }
  //*/
}

















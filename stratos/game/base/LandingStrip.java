

package stratos.game.base ;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.game.planet.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;





public class LandingStrip extends Venue {
  
  
  
  /**  Constructors, data fields, setup and save/load methods-
    */
  final static String IMG_DIR = "media/Buildings/merchant/" ;
  final public static ModelAsset MODEL = CutoutModel.fromSplatImage(
    LandingStrip.class, IMG_DIR+"landing_strip.png", 4.25f
  );
  
  
  
  final SupplyDepot belongs ;
  private Dropship docking ;
  
  
  public LandingStrip(SupplyDepot belongs) {
    super(4, 1, ENTRANCE_NORTH, belongs.base()) ;
    structure.setupStats(50, 10, 25, 0, Structure.TYPE_FIXTURE) ;
    this.belongs = belongs ;
    
    //final GroupSprite sprite = new GroupSprite() ;
    //sprite.attach(STRIP_MODEL, 0, 0, -0.05f) ;
    //attachSprite(sprite);
    attachModel(MODEL);
  }
  

  public LandingStrip(Session s) throws Exception {
    super(s) ;
    belongs = (SupplyDepot) s.loadObject() ;
    docking = (Dropship) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(belongs) ;
    s.saveObject(docking) ;
  }
  
  
  
  /**  Owning/pathing modifications-
    */
  public int owningType() {
    return VENUE_OWNS ;
  }
  
  
  public int pathType() {
    return Tile.PATH_BLOCKS ;
  }
  
  
  public boolean privateProperty() {
    return true ;
  }
  
  
  public boolean canPlace() {
    if (! super.canPlace()) return false ;
    if (! Spacing.adjacent(this, belongs)) return false ;
    return true ;
  }
  
  
  protected boolean canTouch(Element e) {
    return (e.owningType() < this.owningType()) || e == belongs ;
  }
  
  
  
  /**  Docking functions-
    */
  public Boardable[] canBoard(Boardable batch[]) {
    final Batch <Boardable> CB = new Batch <Boardable> () ;
    if (mainEntrance() != null) CB.add(mainEntrance()) ;
    if (docking != null) CB.add(docking) ;
    int i = 2 ; for (Mobile m : inside()) if (m instanceof Boardable) {
      CB.add((Boardable) m) ;
    }
    return CB.toArray(Boardable.class) ;
  }
  
  
  public Dropship docking() {
    return docking ;
  }
  
  
  public void setToDock(Dropship ship) {
    docking = ship ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! structure.intact()) return ;
    if (docking != null && ! docking.inWorld()) docking = null ;
  }
  
  
  public Service[] services() { return null ; }
  
  public Background[] careers() { return null ; }
  
  public Behaviour jobFor(Actor actor) { return null ; }
  
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(SupplyDepot.ICON, "landing_strip");
  }
  
  
  public String fullName() {
    return "Landing Strip" ;
  }
  
  
  public String helpInfo() {
    return
      "The Landing Strip provides offworld freighters with a convenient area "+
      "to land and refuel, facilitating sanctioned trade and migration." ;
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_MERCHANT ;
  }
}

















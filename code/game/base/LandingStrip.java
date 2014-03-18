

package code.game.base ;
import code.game.actors.*;
import code.game.building.*;
import code.game.civilian.*;
import code.game.common.*;
import code.game.planet.*;
import code.graphics.common.*;
import code.graphics.cutout.*;
import code.graphics.widgets.*;
import code.user.*;
import code.util.*;





public class LandingStrip extends Venue {
  
  
  
  /**  Constructors, data fields, setup and save/load methods-
    */
  final static String IMG_DIR = "media/Buildings/merchant/" ;
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    IMG_DIR+"landing_strip.png", LandingStrip.class, 4.25f, 0
  );
  
  
  
  final SupplyDepot belongs ;
  private Dropship docking ;
  
  
  public LandingStrip(SupplyDepot belongs) {
    super(4, 1, ENTRANCE_NORTH, belongs.base()) ;
    structure.setupStats(50, 10, 25, 0, Structure.TYPE_FIXTURE) ;
    this.belongs = belongs ;
    
    //final GroupSprite sprite = new GroupSprite() ;
    //sprite.attach(STRIP_MODEL, 0, 0, -0.05f) ;
    //attachSprite(sprite) ;
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
  public Composite portrait(HUD UI) {
    return null;//new Composite(UI, "media/GUI/Buttons/supply_depot_button.gif") ;
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
  
  
  public void writeInformation(Description d, int categoryID, HUD UI) {
    super.writeInformation(d, categoryID, UI) ;
  }
}

















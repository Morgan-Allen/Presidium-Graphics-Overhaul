


package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.graphics.common.*;
import stratos.graphics.solids.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.economic.Economy.*;



public class CargoBarge extends Vehicle implements
  Inventory.Owner
{
  
  /**  Fields, constants, constructors and save/load methods-
    */
  final static String
    FILE_DIR = "media/Vehicles/",
    XML_FILE = "VehicleModels.xml";
  final static ModelAsset BARGE_MODEL = MS3DModel.loadFrom(
    FILE_DIR, "loader_2.ms3d", CargoBarge.class, XML_FILE, "CargoBarge"
  );
  
  
  
  public CargoBarge() {
    super();
    attachSprite(BARGE_MODEL.makeSprite());
  }
  
  
  public CargoBarge(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  public float height() { return 1.0f; }
  public float radius() { return 1.0f; }
  
  
  
  /**  Economic and behavioural functions-
    */
  public Behaviour jobFor(Actor actor) {
    return null;
  }
  
  
  public float homeCrowding(Actor actor) {
    return 1;
  }
  
  
  public float visitCrowding(Actor actor) {
    return 0;
  }
  
  
  public boolean actionBoard(Actor actor, CargoBarge ship) {
    ship.setInside(actor, true);
    return true;
  }
  
  /*
  protected void offloadPassengers() {
    final int size = 2 * (int) Math.ceil(radius());
    final int EC[] = Spacing.entranceCoords(size, size, entranceFace);
    final Box2D site = this.area(null);
    final Tile o = world.tileAt(site.xpos() + 0.5f, site.ypos() + 0.5f);
    final Tile exit = world.tileAt(o.x + EC[0], o.y + EC[1]);
    this.dropPoint = exit;
    
    for (Mobile m : inside()) if (! m.inWorld()) {
      m.enterWorldAt(exit.x, exit.y, world);
    }
    inside.clear();
  }
  //*/
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates);
  }
  
  
  public Traded[] services() { return null; }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Cargo Barge";
  }
  
  
  public Composite portrait(BaseUI UI) {
    return null;
  }
  
  
  public String helpInfo() {
    return
      "Cargo Barges shuttle goods in bulk between the more distant reaches "+
      "of your settlement.\n\n"+
      "  'She kicks like a mule and stinks of raw carbons, but she'll "+
      "getcha from A to B.  Assuming B is downhill.'\n"+
      "  -Tev Marlo, Supply Corps";
  }
}




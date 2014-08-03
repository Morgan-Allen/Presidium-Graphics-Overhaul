


package stratos.game.base;
import stratos.game.common.*;
import stratos.game.building.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



//  TODO:  This needs to be placed spontaneously.


public class Causeway extends Structural {
  
  
  final static String IMG_DIR = "media/Buildings/civilian/";
  final static CutoutModel
    NODE_MODELS[] = CutoutModel.fromImages(
      IMG_DIR, Causeway.class, 2, 0.1f, true,
      "causeway_left.png",
      "causeway_right.png",
      "causeway_hub.png"
    ),
    NODE_MODEL_LEFT   = NODE_MODELS[0],
    NODE_MODEL_RIGHT  = NODE_MODELS[1],
    NODE_MODEL_CENTRE = NODE_MODELS[2],
    MODEL_FLAT_LEFT   = NODE_MODELS[2],
    MODEL_FLAT_RIGHT  = NODE_MODELS[2];
  final public static ImageAsset ICON = ImageAsset.fromImage(
    "media/GUI/Buttons/mag_line_button.gif", Causeway.class
  );
  
  
  final protected static int
    TYPE_SECTION  = 0,
    TYPE_HUB      = 1,
    TYPE_TERMINAL = 2;
  
  
  
  public Causeway(Base base) {
    super(2, 1, base);
    structure.setupStats(
      10,  //integrity
      25,  //armour
      20,  //build cost
      0,   //max upogrades
      Structure.TYPE_FIXTURE
    );
  }
  
  
  public Causeway(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  protected Structural instance(Base base) {
    return new Causeway(base);
  }
  
  
  protected boolean lockToGrid() {
    return true;
  }
  

  protected void configFromAdjacent(boolean[] near, int numNear) {
    final Tile o = origin();
    if (numNear == 2) {
      type = TYPE_SECTION;
      if (near[N] && near[S]) {
        facing = Y_AXIS;
        if (o.y % 8 == 0) {
          type = TYPE_HUB;
          attachModel(MODEL_FLAT_RIGHT);
        }
        else attachModel(NODE_MODEL_RIGHT);
        return;
      }
      if (near[W] && near[E]) {
        facing = X_AXIS;
        if (o.x % 8 == 0) {
          type = TYPE_HUB;
          attachModel(MODEL_FLAT_LEFT );
        }
        else attachModel(NODE_MODEL_LEFT);
        return;
      }
    }
    facing = CORNER;
    attachModel(NODE_MODEL_CENTRE);
  }
  
  
  public int pathType() {
    return Tile.PATH_ROAD;
  }


  protected void updatePaving(boolean inWorld) {
    //
    //  For some reason, switching the order in which these are called causes
    //  some roads to disappear.  TODO:  INVESTIGATE
    if (type != TYPE_SECTION) {
      base().paving.updateJunction(this, origin(), inWorld);
    }
    base().paving.updatePerimeter(this, inWorld);
  }
  
  
  public boolean enterWorldAt(int x, int y, World world) {
    if (! super.enterWorldAt(x, y, world)) return false;
    base.paving.map.maskAsPaved(Spacing.under(area(), world), true);
    return true;
  }
  
  
  public void exitWorld() {
    base.paving.map.maskAsPaved(Spacing.under(area(), world), false);
    super.exitWorld();
  }
  


  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Causeway";
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "causeway");
  }
  
  
  public String helpInfo() {
    return
      "Causeways facilitate long-range transport of goods and personnel, "+
      "along with water, power and life support.";
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_HIDDEN;
  }
}






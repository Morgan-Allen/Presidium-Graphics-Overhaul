


package stratos.game.base;
import stratos.game.common.*;
import stratos.game.building.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;




public class SolarBank extends Segment implements Economy {
  
  
  final static String
    IMG_DIR = "media/Buildings/ecologist/" ;
  final public static ModelAsset
    ARRAY_MODELS[] = CutoutModel.fromImages(
      IMG_DIR, SolarBank.class, 2, 2, false,
      "solar_bank_left.png",
      "solar_bank_right.png",
      "solar_bank_centre.png"
      //"windtrap_right.png",
      //"power_hub_left.png",
      //"power_hub_right.png"
    ),
    MODEL_LEFT       = ARRAY_MODELS[0],
    MODEL_RIGHT      = ARRAY_MODELS[1],
    MODEL_CENTRE     = ARRAY_MODELS[2];
    /*
    MODEL_TRAP_LEFT  = ARRAY_MODELS[2],
    MODEL_TRAP_RIGHT = ARRAY_MODELS[3],
    MODEL_HUB_LEFT   = ARRAY_MODELS[4],
    MODEL_HUB_RIGHT  = ARRAY_MODELS[5];
  //*/
  final static ImageAsset ICON = ImageAsset.fromImage(
    "media/GUI/Buttons/solar_array_button.gif", SolarBank.class
  );
  
  final int
    TYPE_SOLAR = 0,
    TYPE_WIND  = 1,
    TYPE_HUB   = 2 ;
  
  
  
  public SolarBank(Base base) {
    super(2, 2, base) ;
    structure.setupStats(10, 5, 40, 0, Structure.TYPE_FIXTURE) ;
    personnel.setShiftType(SHIFTS_ALWAYS) ;
  }
  
  
  public SolarBank(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  protected Segment instance(Base base) {
    return new SolarBank(base);
  }
  
  
  protected boolean lockToGrid() {
    return false;
  }
  

  protected void configFromAdjacent(boolean[] near, int numNear) {
    final Tile o = origin() ;
    type = TYPE_SOLAR ;
    
    if (numNear > 0 && numNear <= 2) {
      if (near[N] || near[S]) {
        facing = X_AXIS ;
        if (o.y % 8 == 0) {
          type = TYPE_WIND ;
          attachModel(MODEL_CENTRE) ;
        }
        else attachModel(MODEL_LEFT) ;
        return ;
      }
      if (near[W] || near[E]) {
        facing = Y_AXIS ;
        if (o.x % 8 == 0) {
          type = TYPE_WIND ;
          attachModel(MODEL_CENTRE) ;
        }
        else attachModel(MODEL_RIGHT) ;
        return ;
      }
    }
    
    facing = CORNER ;
    attachModel(MODEL_RIGHT) ;
  }
  

  protected List <Segment> installedBetween(Tile start, Tile end) {
    final List <Segment> installed = super.installedBetween(start, end);
    if (installed == null) return installed;
    
    final int hubIndex = installed.size() / 2;
    final SolarBank hub = (SolarBank) installed.atIndex(hubIndex);
    hub.type = TYPE_HUB;
    ModelAsset model = hub.facing == X_AXIS ? MODEL_CENTRE : MODEL_CENTRE;
    hub.attachModel(model);
    
    return installed;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! structure.intact()) return ;
    //
    //  TODO:  Power must be stockpiled by day and released slowly at night.
    //  ...Maybe just a constant output could be assumed, for simplicity's
    //  sake?  Or maybe the hub could detect shortages and release more power
    //  to satisfy demand?
    
    final float dayVal = Planet.dayValue(world) ;
    stocks.bumpItem(POWER, 5 * dayVal / 10f, type == TYPE_HUB ? 100 : 10) ;
    stocks.bumpItem(WATER, 1 * dayVal / 10f, 5 ) ;
  }
  
  
  protected void updatePaving(boolean inWorld) {
    if (type != TYPE_HUB) return ;
    base().paving.updatePerimeter(this, inWorld) ;
    
    final Tile o = origin() ;
    base().paving.updateJunction(this, o, false) ;
    
    final Tile perim[] = Spacing.perimeter(area(), world) ;
    for (int n = 0 ; n < perim.length ; n += 4) {
      final Tile t = perim[n] ;
      if (t != null) base().paving.updateJunction(this, t, inWorld) ;
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Solar Bank" ;
  }


  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "solar_bank");
  }
  
  
  public String helpInfo() {
    return
      "Solar Banks provide clean power and a small amount of water to your "+
      "settlement.";
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_ECOLOGIST ;
  }
}









package stratos.game.base;
import stratos.game.common.*;
import stratos.game.building.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;

import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.building.Economy.*;



public class SolarBank extends Structural {
  
  
  final static String
    IMG_DIR = "media/Buildings/ecologist/";
  final public static ModelAsset
    ARRAY_MODELS[] = CutoutModel.fromImages(
      SolarBank.class, IMG_DIR, 2, 2, false,
      "solar_bank_left.png",
      "solar_bank_right.png",
      "solar_bank_centre.png"
    ),
    MODEL_LEFT       = ARRAY_MODELS[0],
    MODEL_RIGHT      = ARRAY_MODELS[1],
    MODEL_CENTRE     = ARRAY_MODELS[2];
  
  final static ImageAsset ICON = ImageAsset.fromImage(
    SolarBank.class, "media/GUI/Buttons/solar_array_button.gif"
  );
  
  final int
    TYPE_SOLAR = 0,
    TYPE_WIND  = 1,
    TYPE_HUB   = 2;
  
  
  
  public SolarBank(Base base) {
    super(2, 2, base);
    structure.setupStats(10, 5, 40, 0, Structure.TYPE_FIXTURE);
  }
  
  
  public SolarBank(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  //  TODO:  Restore the use of this:
  
  protected void configFromAdjacent(boolean[] near, int numNear) {
    final Tile o = origin();
    type = TYPE_SOLAR;
    
    if (numNear > 0 && numNear <= 2) {
      if (near[N] || near[S]) {
        facing = X_AXIS;
        if (o.y % 8 == 0) {
          type = TYPE_WIND;
          attachModel(MODEL_CENTRE);
        }
        else attachModel(MODEL_LEFT);
        return;
      }
      if (near[W] || near[E]) {
        facing = Y_AXIS;
        if (o.x % 8 == 0) {
          type = TYPE_WIND;
          attachModel(MODEL_CENTRE);
        }
        else attachModel(MODEL_RIGHT);
        return;
      }
    }
    
    facing = CORNER;
    attachModel(MODEL_RIGHT);
  }
  
  /*
  protected List <Structural> installedBetween(Tile start, Tile end) {
    final List <Structural> installed = super.installedBetween(start, end);
    if (installed == null) return installed;
    
    final int hubIndex = installed.size() / 2;
    final SolarBank hub = (SolarBank) installed.atIndex(hubIndex);
    hub.type = TYPE_HUB;
    ModelAsset model = hub.facing == X_AXIS ? MODEL_CENTRE : MODEL_CENTRE;
    hub.attachModel(model);
    
    return installed;
  }
  //*/
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates);
    
    //  TODO:  Vary this based on how much you have stocked of each over the
    //  past 24 hours, sunlight/moisture values, et cetera.
    structure.assignOutputs(
      Item.withAmount(POWER, 2),
      Item.withAmount(OPEN_WATER, 0.5f)
    );
  }
  
  
  protected void updatePaving(boolean inWorld) {
    if (type != TYPE_HUB) return;
    final Paving paving = base().paving;
    paving.updatePerimeter(this, inWorld);
    
    final Tile perim[] = Spacing.perimeter(area(), world);
    for (int n = 0; n < perim.length; n += 4) {
      final Tile t = perim[n];
      if (t != null) paving.updateJunction(this, t, inWorld && ! t.blocked());
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Solar Bank";
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
    return InstallTab.TYPE_ECOLOGIST;
  }
}





/*
protected Structural instance(Base base) {
  return new SolarBank(base);
}


protected boolean lockToGrid() {
  return false;
}
//*/

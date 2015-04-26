


package stratos.game.civic;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;



public class SolarBank extends Venue {
  
  
  final static String
    IMG_DIR = "media/Buildings/ecologist/";
  final public static ModelAsset
    BANK_MODELS[][] = CutoutModel.fromImageGrid(
      SolarBank.class, IMG_DIR+"all_solar_banks.png", 2, 3, 2, 2, false
    ),
    
    MODEL_X_SEGMENT = BANK_MODELS[1][1],
    MODEL_Y_SEGMENT = BANK_MODELS[0][1],
    MODEL_X_HUB     = BANK_MODELS[1][0],
    MODEL_Y_HUB     = BANK_MODELS[0][0],
    MODEL_X_TRAP    = BANK_MODELS[1][2],
    MODEL_Y_TRAP    = BANK_MODELS[0][2];
  
  final static ImageAsset ICON = ImageAsset.fromImage(
    SolarBank.class, "media/GUI/Buttons/solar_array_button.gif"
  );
  
  final static Conversion
    LAND_TO_POWER = new Conversion(
      SolarBank.class, "land_to_power",
      TO, 2, POWER
    ),
    LAND_TO_WATER = new Conversion(
      SolarBank.class, "land_to_water",
      TO, 1, WATER
    );
  
  
  final static Blueprint BLUEPRINT = new Blueprint(
    SolarBank.class, "solar_bank", "Solar Bank",
    2, 2, IS_LINEAR | IS_FIXTURE,
    EcologistStation.BLUEPRINT, Owner.TIER_FACILITY,
    LAND_TO_POWER, LAND_TO_WATER
  );
  
  
  public SolarBank(Base base) {
    super(BLUEPRINT, base);
    structure.setupStats(10, 5, 40, 0, Structure.TYPE_FIXTURE);
  }
  
  
  public SolarBank(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Updates and life-cycle:
    */
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) {
      structure.assignOutputs();
      return;
    }
    
    final float sun = world.terrain().insolationSample(origin());
    structure.assignOutputs(
      Item.withAmount(POWER, sun * 4),
      Item.withAmount(WATER, (0.5f + 1 - sun) / 2)
    );
  }
  
  
  public Background[] careers() {
    return null;
  }
  
  
  public Traded[] services() {
    return null;
  }
  
  
  
  /**  Structure.Basis and placement methods-
    */
  public boolean setupWith(Tile position, Box2D area, Coord... others) {
    if (! super.setupWith(position, area, others)) return false;
    
    if (area.xdim() > area.ydim()) {
      if ((position.x / 2) % 3 == 0) {
        attachModel(MODEL_X_HUB);
      }
      else attachModel(MODEL_X_SEGMENT);
    }
    else {
      if ((position.y / 2) % 3 == 0) {
        attachModel(MODEL_Y_HUB);
      }
      else attachModel(MODEL_Y_SEGMENT);
    }
    return true;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "solar_bank");
  }
  
  
  public SelectionPane configPanel(SelectionPane panel, BaseUI UI) {
    final String status = null;
    return VenuePane.configSimplePanel(this, panel, UI, status);
  }
  
  
  public String helpInfo() {
    return
      "Solar Banks provide clean power and a small amount of water to your "+
      "settlement.";
  }
  
  
  public String objectCategory() {
    return InstallationPane.TYPE_ECOLOGIST;
  }
}




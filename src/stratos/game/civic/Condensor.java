

package stratos.game.civic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;



public class Condensor extends Venue {
  
  
  final static ModelAsset MODEL = CutoutModel.fromImage(
    Condensor.class, "media/Buildings/aesthete/condensor.png", 3, 2
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    Condensor.class, "media/GUI/Buttons/condensor_button.gif"
  );
  
  final static Blueprint BLUEPRINT = new Blueprint(
    Condensor.class, "condensor",
    "Condensor", UIConstants.TYPE_AESTHETIC,
    3, 2, IS_FIXTURE,
    EcologistStation.BLUEPRINT, Owner.TIER_FACILITY
  );
  
  
  
  public Condensor(Base base) {
    super(BLUEPRINT, base);
    structure.setupStats(
      85 ,  // integrity
      1  ,  // armour
      200,  // build cost
      0  ,  // max upgrades
      Structure.TYPE_FIXTURE
    );
    attachModel(MODEL);
  }
  
  
  public Condensor(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Economic functions-
    */
  public Background[] careers() {
    return null;
  }
  
  
  public Traded[] services() {
    return new Traded[] { ATMO, WATER };
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (structure.intact()) {
      stocks.forceDemand(POWER, 4, false);
      stocks.forceDemand(ATMO, 10, true );
      stocks.forceDemand(WATER, 5, true );
      structure.setAmbienceVal(5);
    }
    else {
      stocks.clearDemands();
      structure.setAmbienceVal(0);
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "condensor");
  }
  
  
  public SelectionPane configSelectPane(SelectionPane panel, BaseUI UI) {
    return VenuePane.configSimplePanel(this, panel, UI, null);
  }
  
  
  public String helpInfo() {
    return
      "The Condensor provides water and life support to the surrounding "+
      "settlement.";
  }
}










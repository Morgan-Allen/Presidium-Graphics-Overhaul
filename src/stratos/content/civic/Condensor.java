/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.wild.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;
import static stratos.game.actors.Qualities.*;



public class Condensor extends Venue {
  
  
  final static ModelAsset MODEL = CutoutModel.fromImage(
    Condensor.class, "media/Buildings/aesthete/condensor.png", 2, 1
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    Condensor.class, "media/GUI/Buttons/condensor_button.gif"
  );
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    Condensor.class, "condensor",
    "Condensor", Target.TYPE_WIP, ICON,
    "The Condensor provides "+WATER+" and "+ATMO+" to the surrounding "+
    "settlement.",
    2, 1, Structure.IS_FIXTURE, Owner.TIER_FACILITY, 85, 1,
    ATMO, WATER
  );
  
  final public static Upgrade LEVELS[] = BLUEPRINT.createVenueLevels(
    Upgrade.SINGLE_LEVEL, EcologistStation.LEVELS[0],
    new Object[] { 10, ASSEMBLY, 5, GEOPHYSICS },
    250
  );
  
  
  
  public Condensor(Base base) {
    super(BLUEPRINT, base);
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
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (structure.intact()) {
      final Tile at = world.tileAt(this);
      
      //  TODO:  Move this to the Solar Bank?
      float waterOut = 0;
      waterOut += world.terrain().fertilitySample(at);
      waterOut += world.terrain().habitatSample(at, Habitat.SHALLOWS);
      waterOut += world.terrain().habitatSample(at, Habitat.OCEAN   );
      waterOut *= 2.5f;
      
      stocks.forceDemand(POWER, 4       , false);
      stocks.forceDemand(ATMO , 10      , true );
      stocks.forceDemand(WATER, waterOut, true );
      structure.setAmbienceVal(5);
    }
    else {
      stocks.clearDemands();
      structure.setAmbienceVal(0);
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public SelectionPane configSelectPane(SelectionPane panel, HUD UI) {
    return VenuePane.configSimplePanel(this, panel, UI, null, null);
  }
  
  
  public String helpInfo() {
    String info = super.helpInfo();
    if (stocks.demandFor(WATER) < 5) info =
      "The terrain around this Condensor is very dry, which will limit "+WATER+
      " output.";
    return info;
  }
}










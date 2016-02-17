/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.actors.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;

import static stratos.game.actors.Qualities.*;
import static stratos.game.craft.Economy.*;



public class SolarBank extends Venue {
  
  
  final static String
    IMG_DIR = "media/Buildings/ecologist/";
  final public static ModelAsset
    BANK_MODELS[][] = CutoutModel.fromImageGrid(
      SolarBank.class, IMG_DIR+"all_solar_banks.png", 2, 3, 2, 1, false
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
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    SolarBank.class, "solar_bank",
    "Solar Bank", Target.TYPE_WIP, ICON,
    "Solar Banks provide clean "+POWER+" and a small amount of "+WATER+" to "+
    "your settlement.",
    2, 1, Structure.IS_LINEAR | Structure.IS_FIXTURE,
    Owner.TIER_FACILITY, 10, 5,
    POWER, WATER
  );
  
  final static Conversion
    LAND_TO_POWER = new Conversion(
      BLUEPRINT, "land_to_power_at_banks",
      TO, 2, POWER
    ),
    LAND_TO_WATER = new Conversion(
      BLUEPRINT, "land_to_water_at_banks",
      TO, 1, WATER
    );
  
  final public static Upgrade LEVELS[] = BLUEPRINT.createVenueLevels(
    Upgrade.SINGLE_LEVEL, BotanicalStation.LEVELS[0],
    new Object[] { 10, ASSEMBLY, 0, GEOPHYSICS },
    25
  );
  
  
  public SolarBank(Base base) {
    super(BLUEPRINT, base);
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
    if (! structure.intact()) { stocks.clearDemands(); return; }
    final float sun = world.terrain().insolationSample(origin());
    stocks.forceDemand(POWER, 0, sun * 4);
    stocks.forceDemand(WATER, 0, (0.5f + 1 - sun) / 2);
  }
  
  
  
  /**  Placeable and placement methods-
    */
  public boolean setupWith(Tile position, Box2D area, Coord... others) {
    if (! super.setupWith(position, area, others)) return false;
    
    if (area == null) area = new Box2D(footprint());
    
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
  public SelectionPane configSelectPane(SelectionPane panel, HUD UI) {
    final String status = null;
    return VenuePane.configSimplePanel(this, panel, UI, null, status);
  }
}




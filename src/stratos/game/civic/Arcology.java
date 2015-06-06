/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.civic;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.actors.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;



public class Arcology extends Venue {
  
  
  /**  Data fields, constructors, setup and save/load methods-
    */
  final static String IMG_DIR = "media/Buildings/aesthete/";
  final static ModelAsset
    BEDS_MODELS[][] = CutoutModel.fromImageGrid(
      Arcology.class, IMG_DIR+"all_arcology.png",
      4, 4, 2, 2, false
    ),
    
    MBW = BEDS_MODELS[0][0],
    MBX = BEDS_MODELS[1][0],
    MBE = BEDS_MODELS[2][0],
    MODELS_X_AXIS[] = { MBW, MBX, MBE },  //bottom goes west to east...
    
    MBS = BEDS_MODELS[0][1],
    MBY = BEDS_MODELS[1][1],
    MBN = BEDS_MODELS[2][1],
    MODELS_Y_AXIS[] = { MBS, MBY, MBN };  //top goes south to north...
  
  final static ImageAsset ICON = ImageAsset.fromImage(
    Arcology.class, "media/GUI/Buttons/arcology_button.gif"
  );
  
  final static int
    FULL_GROWTH_INTERVAL = Stage.STANDARD_DAY_LENGTH * 5;
  
  final static Blueprint BLUEPRINT = new Blueprint(
    Arcology.class, "arcology",
    "Arcology", UIConstants.TYPE_AESTHETIC, ICON,
    "Arcology provides both beauty and life support to your settlement, "+
    "but require space and "+WATER+".",
    2, 2, Structure.IS_LINEAR | Structure.IS_FIXTURE,
    EcologistStation.BLUEPRINT, Owner.TIER_FACILITY,
    15, 1, 100, Structure.NO_UPGRADES
  );
  
  
  private float plantsHealth = 0.5f;
  
  
  public Arcology(Base base) {
    super(BLUEPRINT, base);
  }
  
  
  public Arcology(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  
  /**  Placement and situation methods-
    */
  public boolean setupWith(Tile position, Box2D area, Coord... others) {
    if (! super.setupWith(position, area, others)) return false;
    
    //  TODO:  You need to insert a special widget here at every 4th space, and
    //  cap correctly around that...
    
    
    final Object model = PlaceUtils.setupSegment(
      this, position, area, others, MODELS_X_AXIS, MODELS_Y_AXIS
    );
    attachModel((ModelAsset) model);
    return true;
  }
  
  
  
  /**  Behaviour and economic functions-
    */
  //  TODO:  Require seeding by an ecologist for maximum growth!
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    
    if (structure.intact()) {
      float waterNeed = 1f - world.terrain().fertilitySample(origin()) / 2;
      stocks.forceDemand(WATER, waterNeed, false);
      
      final float growth = 1f + stocks.amountOf(WATER) - waterNeed;
      plantsHealth += growth / FULL_GROWTH_INTERVAL;
      plantsHealth = Nums.clamp(plantsHealth, 0, 1);
      
      structure.setAmbienceVal(Ambience.GREAT_AMBIENCE * plantsHealth);
      world.ecology().impingeBiomass(origin(), 5 * plantsHealth, 1);
    }
    else {
      structure.setAmbienceVal(Ambience.NO_AMBIENCE_FX);
    }
  }
  
  
  public Background[] careers() {
    return null;
  }
  
  
  public Traded[] services() {
    return null;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public SelectionPane configSelectPane(SelectionPane panel, BaseUI UI) {
    final int gP = (int) (plantsHealth * 100);
    final String status = "Plant growth: "+gP+"%";
    return VenuePane.configSimplePanel(this, panel, UI, status);
  }
}






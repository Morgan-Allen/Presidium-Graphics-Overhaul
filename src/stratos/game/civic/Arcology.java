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



//  TODO:  The art here needs revision.

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
  
  final static VenueProfile PROFILE = new VenueProfile(
    Arcology.class, "arcology", "Arcology",
    2, 2, IS_LINEAR | IS_FIXTURE,
    EcologistStation.PROFILE, Owner.TIER_FACILITY
  );
  
  
  private int facing = UNUSED;
  private float plantsHealth = 0.5f;
  
  
  public Arcology(Base base) {
    super(PROFILE, base);
    structure.setupStats(15, 1, 100, 0, Structure.TYPE_FIXTURE);
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
    final Object model = Placement.setupSegment(
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
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "arcology");
  }
  
  
  public SelectionPane configPanel(SelectionPane panel, BaseUI UI) {
    final String status = "Plant health: "+I.shorten(plantsHealth, 1);
    return VenuePane.configSimplePanel(this, panel, UI, status);
  }
  
  
  public String helpInfo() {
    return
      "Arcology provides beauty and life support to your settlement, helping "+
      "to improve ambience and minimise squalor.";
  }
  
  
  public String objectCategory() {
    return UIConstants.TYPE_AESTHETIC;
  }
}




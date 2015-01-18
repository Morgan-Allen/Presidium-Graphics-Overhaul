/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.common.*;
import stratos.game.economic.*;
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
      4, 4, 2, 1
    ),
    
    MODEL_BEDS_WEST  = BEDS_MODELS[0][1],
    MODEL_BEDS_RIGHT = BEDS_MODELS[1][1],  //bottom goes south to north...
    MODEL_BEDS_EAST  = BEDS_MODELS[2][1],
    
    MODEL_BEDS_SOUTH = BEDS_MODELS[0][0],
    MODEL_BEDS_LEFT  = BEDS_MODELS[1][0],  //top goes west to east...
    MODEL_BEDS_NORTH = BEDS_MODELS[2][0];
    
    /*
    ART_MODELS[] = {
      BEDS_MODELS[0][2], BEDS_MODELS[1][2], BEDS_MODELS[2][2],
      BEDS_MODELS[0][3], BEDS_MODELS[1][3], BEDS_MODELS[2][3]
    };
  //*/
  final static ImageAsset ICON = ImageAsset.fromImage(
    Arcology.class, "media/GUI/Buttons/arcology_button.gif"
  );
  
  final static int
    FULL_GROWTH_INTERVAL = Stage.STANDARD_DAY_LENGTH * 5;
  
  private int facing = UNUSED;
  private float plantsHealth = 0.5f;
  
  
  public Arcology(Base base) {
    super(2, 2, ENTRANCE_NONE, base);
    structure.setupStats(15, 1, 100, 0, Structure.TYPE_FIXTURE);
    
    //  TODO:  Okay.  Now you just need to make sure that the facing-type
    //  upgrades on placement.
    this.attachModel(MODEL_BEDS_WEST);
  }
  
  
  public Arcology(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  
  /**  Placement and situation methods-
    */
  //  TODO:  Unify this with the equivalent methods in the SolarBank class (and
  //  maybe ShieldWall?)
  
  //  TODO:  You need to have horizontal and vertical placement-coords, and
  //  pick whichever gives you more space.
  
  //  TODO:  Allow tracing on the ground?
  
  final static int
    FACING_X_COORDS[] = { 0, 0,  2, 0,  4, 0,  6, 0 }, OFFS_X[] = {3, 0},
    FACING_Y_COORDS[] = { 0, 0,  0, 2,  0, 4,  0, 6 }, OFFS_Y[] = {0, 3},
    SIDE_LENGTH = 8;
  
  
  private Arcology[] getBankPlacement(Tile point, Base base, int facing) {
    //
    //  Firstly, determine which sector this point lies within, and the corner
    //  tile of that sector.
    if (point == null) return null;
    final Stage world = point.world;
    final List <Arcology> newBank = new List <Arcology> ();
    final int coords[] = facing == X_AXIS ? FACING_X_COORDS : FACING_Y_COORDS;
    final int offs[]   = facing == X_AXIS ? OFFS_X : OFFS_Y;
    
    for (int n = 0; n < FACING_Y_COORDS.length;) {
      final Tile under = world.tileAt(
        point.x + coords[n++] - offs[0],
        point.y + coords[n++] - offs[1]
      );
      if (under == null) return null;
      final Arcology s = new Arcology(base);
      s.facing = facing;
      s.setPosition(under.x, under.y, world);
      //if (! s.canPlace()) return null;
      newBank.add(s);
    }
    return newBank.toArray(Arcology.class);
  }
  
  
  private boolean bankOkay(Venue bank[]) {
    if (bank == null) return false;
    for (Venue b : bank) if (! b.canPlace()) return false;
    return true;
  }
  
  
  public boolean setPosition(float x, float y, Stage world) {
    if (! super.setPosition(x, y, world)) return false;
    if (facing != UNUSED) return true;
    
    Arcology group[] = null;
    if (! bankOkay(group)) group = getBankPlacement(origin(), base, X_AXIS);
    if (! bankOkay(group)) group = getBankPlacement(origin(), base, Y_AXIS);
    if (group == null) return false;
    
    structure.assignGroup(group);
    for (Arcology bank : group) {
      bank.structure.assignGroup(group);
      final boolean
        first = bank == Visit.last(group),
        last  = bank == group[0];
      if (bank.facing == X_AXIS) {
        if      (first) bank.attachModel(MODEL_BEDS_NORTH);
        else if (last ) bank.attachModel(MODEL_BEDS_SOUTH);
        else            bank.attachModel(MODEL_BEDS_LEFT );
      }
      else {
        if      (first) bank.attachModel(MODEL_BEDS_EAST );
        else if (last ) bank.attachModel(MODEL_BEDS_WEST );
        else            bank.attachModel(MODEL_BEDS_RIGHT);
      }
    }
    return true;
  }
  
  
  protected boolean checkPerimeter(Stage world) {
    for (Tile t : Spacing.perimeter(footprint(), world)) {
      if (t == null) continue;
      if (t.owningType() >= this.owningType()) return false;
    }
    return true;
  }
  
  
  
  /**  Behaviour and economic functions-
    */
  //  TODO:  Require seeding from an ecologist station for maximum growth!
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    
    float waterNeed = 1f - world.terrain().fertilitySample(origin()) / 2;
    stocks.forceDemand(WATER, waterNeed, Tier.CONSUMER);
    
    final float growth = 1f + stocks.amountOf(WATER) - waterNeed;
    plantsHealth += growth / FULL_GROWTH_INTERVAL;
    plantsHealth = Nums.clamp(plantsHealth, 0, 1);
    
    structure.setAmbienceVal(10 * plantsHealth);
    world.ecology().impingeBiomass(origin(), 5 * plantsHealth, 1);
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
  
  
  public String fullName() { return "Arcology"; }
  
  
  public SelectionInfoPane configPanel(SelectionInfoPane panel, BaseUI UI) {
    final String status = "Plant health: "+I.shorten(plantsHealth, 1);
    return VenueDescription.configSimplePanel(this, panel, UI, status);
  }
  
  
  public String helpInfo() {
    return
      "Arcology provides beauty and life support to your settlement, helping "+
      "to improve ambience and minimise squalor.";
  }
  
  
  public String objectCategory() {
    return UIConstants.TYPE_AESTHETE;
  }
}


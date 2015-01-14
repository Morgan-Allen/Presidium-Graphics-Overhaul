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
  
  /*
  final static int
    IS_PLACING  = -1,
    FACING_INIT =  0,
    FACING_X    =  1,
    FACING_Y    =  2;
  //*/
  
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
  //  TODO:  You need to have horizontal and vertical placement-coords, and
  //  pick whichever gives you more space.
  
  //  TODO:  Allow tracing on the ground?
  
  final static int
    FACING_Y_COORDS[] = { 0, 0,  0, 2,  0, 4,  0, 6 },
    FACING_X_COORDS[] = { 0, 0,  2, 0,  4, 0,  6, 0 },
    SIDE_LENGTH = 8;
  
  
  private Arcology[] getBankPlacement(
    Tile point, Base base, int facing
  ) {
    //
    //  Firstly, determine which sector this point lies within, and the corner
    //  tile of that sector.
    if (point == null) return null;
    final Stage world = point.world;
    final Tile corner = world.tileAt(
      Nums.round(point.x, 1, false),
      Nums.round(point.y, 1, false)
    );
    //
    //
    final List <Arcology> newBank = new List <Arcology> ();
    final int coords[] = facing == X_AXIS ? FACING_X_COORDS : FACING_Y_COORDS;
    for (int n = 0; n < FACING_Y_COORDS.length;) {
      final Tile under = world.tileAt(
        corner.x + coords[n++],
        corner.y + coords[n++]
      );
      if (under == null) return null;
      final Arcology s = new Arcology(base);
      s.facing = facing;
      s.setPosition(under.x, under.y, world);
      if (! s.canPlace()) return null;
      newBank.add(s);
    }
    return newBank.toArray(Arcology.class);
  }
  
  
  public boolean setPosition(float x, float y, Stage world) {
    if (! super.setPosition(x, y, world)) return false;
    if (facing != UNUSED) return true;
    
    Arcology group[] = null;
    if (group == null) group = getBankPlacement(origin(), base, X_AXIS);
    if (group == null) group = getBankPlacement(origin(), base, Y_AXIS);
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
  
  
  
  /**  Behaviour and economic functions-
    */
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    
    final Tile o = origin();
    boolean near[] = new boolean[8];
    int numNear = 0;
    for (int i : T_INDEX) {
      final Tile t = world.tileAt(o.x + T_X[i] * 2, o.y + T_Y[i] * 2);
      if (t != null && t.onTop() instanceof Arcology) {
        near[i] = true;
        numNear++;
      }
    }
    
    //this.configFromAdjacent(near, numNear);
  }
  
  
  public Background[] careers() {
    return null;
  }
  
  
  public Traded[] services() {
    return null;
  }
  
  
  public void onGrowth(Tile t) {
    /*
    //
    //  Demand water in proportion to dryness of the surrounding terrain.
    //  TODO:  You'll also need input of greens or saplings, in all likelihood.
    float needWater = 1 - (origin().habitat().moisture() / 10f);
    needWater *= needWater;
    stocks.incDemand(WATER, needWater, TIER_CONSUMER, 1);
    stocks.bumpItem(WATER, needWater / -10f, 1);
    final float shortWater = stocks.shortagePenalty(WATER);
    //
    //  Kill off the plants if you don't have enough.  Grow 'em otherwise.
    if (shortWater > 0) {
      plantsHealth -= shortWater * needWater / World.STANDARD_DAY_LENGTH;
    }
    else {
      plantsHealth += 1f / World.STANDARD_DAY_LENGTH;
    }
    plantsHealth = Visit.clamp(plantsHealth, 0, 1);
    //
    //  TODO:  UPDATE SPRITE TO REFLECT THIS.
    //*/
    if (t != origin() || ! structure.intact()) return;
    
    plantsHealth = 0.5f;
    world.ecology().impingeBiomass(
      origin(), 5 * plantsHealth, Stage.GROWTH_INTERVAL
    );
    structure.setAmbienceVal(10 * plantsHealth);
    //base().paving.updatePerimeter(this, inWorld());
  }
  
  //
  //  TODO:  Have samples of various different indigenous or foreign flora,
  //  suited to the local climate.
  /*
  private float numSaplings() {
    float num = 0;
    for (Item i : stocks.matches(SAMPLES)) {
      final Crop crop = (Crop) i.refers;
      num += i.amount;
    }
    return num;
  }
  
  
  private void updateSprite() {
    
  }
  //*/


  /**  Rendering and interface methods-
    */
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "arcology");
  }
  
  
  public String fullName() { return "Arcology"; }
  
  
  public SelectionInfoPane configPanel(SelectionInfoPane panel, BaseUI UI) {
    return VenueDescription.configSimplePanel(this, panel, UI, null);
  }
  
  
  public String helpInfo() {
    return
      "Arcology provides beauty and life support to your settlement, helping "+
      "to improve ambience and minimise squalor.";
  }
  
  
  public String objectCategory() {
    return InstallTab.TYPE_AESTHETE;
  }
}





//  TODO:  Use a simpler system here- either horizontal or vertical
//  alignment.
/*
protected void configFromAdjacent(boolean[] near, int numNear) {

  final Tile o = origin();
  final int varID = (o.world.terrain().varAt(o) + o.x + o.y) % 6;
  int capIndex = -1;
  
  if (numNear == 2) {
    if (near[N] && near[S]) facing = Y_AXIS;
    if (near[W] && near[E]) facing = X_AXIS;
  }
  else if (numNear == 1) {
    if (near[N] || near[S]) {
      facing = Y_AXIS;
      capIndex = near[S] ? 2 : 6;
    }
    if (near[W] || near[E]) {
      facing = X_AXIS;
      capIndex = near[E] ? 6 : 2;
    }
  }
  if (facing == -1) facing = CORNER;
  
  if (facing == X_AXIS) {
    if      (capIndex == 2) attachModel(MODEL_BEDS_EAST );
    else if (capIndex == 6) attachModel(MODEL_BEDS_WEST );
    else attachModel(MODEL_BEDS_RIGHT);
  }
  if (facing == Y_AXIS) {
    if      (capIndex == 2) attachModel(MODEL_BEDS_NORTH);
    else if (capIndex == 6) attachModel(MODEL_BEDS_SOUTH);
    else attachModel(MODEL_BEDS_LEFT );
  }
}
//*/









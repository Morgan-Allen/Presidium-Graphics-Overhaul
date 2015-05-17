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
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.economic.Economy.*;



public class Nursery extends Venue implements TileConstants {
  
  
  /**  Constructors, data fields, setup and save/load methods-
    */
  protected static boolean verbose = false;
  
  final static String IMG_DIR = "media/Buildings/ecologist/";
  final static ImageAsset ICON = ImageAsset.fromImage(
    Nursery.class, "media/GUI/Buttons/nursery_button.gif"
  );
  final static ModelAsset
    NURSERY_MODEL = CutoutModel.fromImage(
      Nursery.class, IMG_DIR+"nursery.png", 2, 2
    );
  
  final public static float
    NUM_DAYS_MATURE    = 5,
    MATURE_DURATION    = Stage.STANDARD_DAY_LENGTH * NUM_DAYS_MATURE,
    GROW_TIMES_PER_DAY = Stage.STANDARD_DAY_LENGTH / Stage.GROWTH_INTERVAL,
    EXTRA_CLAIM_SIZE   = 4,
    
    CEREAL_BONUS = 2.00f,
    HIVE_DIVISOR = 4.00f,
    DRYLAND_MULT = 0.75f,
    WETLAND_MULT = 1.25f;
  
  final public static Conversion
    LAND_TO_CARBS = new Conversion(
      Nursery.class, "land_to_carbs",
      TO, 1, CARBS
    ),
    LAND_TO_GREENS = new Conversion(
      Nursery.class, "land_to_greens",
      TO, 1, GREENS
    );
  
  final static Blueprint BLUEPRINT = new Blueprint(
    Nursery.class, "nursery",
    "Nursery", UIConstants.TYPE_ECOLOGIST,
    2, 2, IS_ZONED,
    EcologistStation.BLUEPRINT, Owner.TIER_FACILITY,
    LAND_TO_CARBS, LAND_TO_GREENS
  );
  
  
  private Box2D areaClaimed = new Box2D();
  private Tile toPlant[] = new Tile[0];
  private float needsTending = 0;
  
  
  public Nursery(Base base) {
    super(BLUEPRINT, base);
    structure.setupStats(
      25,  //integrity
      5,  //armour
      75,  //build cost
      0,  //max upgrades
      Structure.TYPE_FIXTURE
    );
    staff.setShiftType(SHIFTS_BY_DAY);
    attachModel(NURSERY_MODEL);
  }
  
  
  public Nursery(Session s) throws Exception {
    super(s);
    areaClaimed.loadFrom(s.input());
    toPlant      = (Tile[]) s.loadObjectArray(Tile.class);
    needsTending = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    areaClaimed.saveTo(s.output());
    s.saveObjectArray(toPlant     );
    s.saveFloat      (needsTending);
  }
  
  
  public int owningTier() {
    return Owner.TIER_PRIVATE;
  }
  
  
  
  /**  Placement and supply-demand functions-
    */
  public boolean setupWith(Tile position, Box2D area, Coord... others) {
    if (! super.setupWith(position, area, others)) return false;
    //
    //  By default, we claim an area 2 tiles larger than the basic footprint,
    //  but we can also have a larger area assigned (e.g, by a human player or
    //  by an automated placement-search.)
    areaClaimed.setTo(footprint()).expandBy(2);
    if (area != null) areaClaimed.include(area);
    this.facing = areaClaimed.xdim() > areaClaimed.ydim() ?
      FACING_SOUTH : FACING_EAST
    ;
    return true;
  }
  
  
  public boolean canPlace(Account reasons) {
    if (! super.canPlace(reasons)) return false;
    if (areaClaimed.maxSide() > Stage.ZONE_SIZE) {
      return reasons.setFailure("Area is too large!");
    }
    final Stage world = origin().world;
    if (! Placement.perimeterFits(this, areaClaimed, owningTier(), 2, world)) {
      return reasons.setFailure("Might obstruct pathing");
    }
    return true;
  }
  
  
  public Box2D areaClaimed() {
    return areaClaimed;
  }
  
  
  public float ratePlacing(Target point, boolean exact) {
    
    final Stage world = point.world();
    final Presences presences = world.presences;
    final EcologistStation station = (EcologistStation) presences.nearestMatch(
      EcologistStation.class, point, -1
    );
    if (station == null || station.base() != base) return -1;
    final float distance = Spacing.distance(point, station);
    
    float demand = base.demands.globalShortage(Nursery.class, false);
    final Tile under = world.tileAt(point);
    float rating = 0;
    rating += world.terrain().fertilitySample(under);
    rating /= 1 + (distance / Stage.ZONE_SIZE);
    rating *= 10 * demand;
    return rating;
  }
  
  
  protected void checkCropStates() {
    final boolean report = verbose && I.talkAbout == this;
    if (toPlant == null || toPlant.length == 0) {
      if (report) I.say("\nNO CROPS TO CHECK");
      needsTending = 0;
      return;
    }
    
    if (report) I.say("\nCHECKING CROP STATES");
    needsTending = 0;
    for (Tile t : toPlant) {
      final Crop c = plantedAt(t);
      if (c == null || c.needsTending()) needsTending++;
    }
    
    if (report) I.say("NEEDS TENDING: "+needsTending);
    needsTending /= toPlant.length;
  }
  
  
  
  /**  Establishing crop areas-
    */
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    structure.setAmbienceVal(Ambience.MILD_AMBIENCE);
    if (toPlant.length == 0) scanForCropTiles();
    if (numUpdates % 10 == 0) checkCropStates();
  }
  
  
  protected void updatePaving(boolean inWorld) {
    final boolean report = verbose && I.talkAbout == this;
    
    if (report) I.say("\nGETTING PERIMETER TILES FOR AREA: "+areaClaimed);
    final Batch <Tile> around = new Batch <Tile> ();
    for (Tile t : Spacing.perimeter(areaClaimed, world)) if (t != null) {
      around.add(t);
      if (report) I.say("  TILE AT: "+t.x+"|"+t.y);
    }
    if (report) I.say("\nGETTING UNPLANTED TILES FOR AREA");
    for (Tile t : world.tilesIn(areaClaimed, true)) {
      if (! couldPlant(t)) {
        around.add(t);
        if (report) I.say("  TILE AT: "+t.x+"|"+t.y);
      }
    }
    base.transport.updatePerimeter(this, inWorld, around);
  }
  
  
  private void scanForCropTiles() {
    final boolean report = verbose && I.talkAbout == this;
    //
    //  We then grab all plantable tiles in the area claimed and resize the
    //  claim itself to fit neatly around those:
    final Batch <Tile> grabbed = new Batch <Tile> ();
    final Box2D cropped = new Box2D(footprint());
    if (report) {
      I.say("\nORIGINAL AREA CLAIMED: "+areaClaimed);
      I.say("  FOOTPRINT:   "+footprint());
      I.say("  ORIGIN TILE: "+origin());
    }
    
    for (Tile t : world.tilesIn(areaClaimed, true)) {
      if (couldPlant(t)) {
        grabbed.add(t);
        cropped.include(t.x, t.y, 0.5f);
        if (report) I.say("  WILL PLANT AT: "+t);
      }
    }
    areaClaimed.setTo(cropped);
    toPlant = grabbed.toArray(Tile.class);
    if (report) I.say("NEW CROPPED AREA: "+areaClaimed);
  }
  
  
  private int plantType(Tile t) {
    if (! areaClaimed.contains(t.x, t.y)) return -1;
    
    final boolean across = facing == FACING_NORTH || facing == FACING_SOUTH;
    final int s = Nums.round(t.world.size, 6, true);  //  Modulus offset.
    final Tile o = origin();
    
    for (Tile n : t.allAdjacent(null)) {
      if (n == null || (n.onTop() instanceof Crop)) continue;
      if (n.pathType() >= Tile.PATH_HINDERS && n.reserved()) return -1;
    }
    
    if (footprint().contains(t.x, t.y)) return -1;
    if (across) {
      if ((t.x + s - o.x) % 3 == 2) return -1;
      if ((t.x + s - o.x) % 6 == 0) return  2;
    }
    else {
      if ((t.y + s - o.y) % 3 == 2) return -1;
      if ((t.y + s - o.y) % 6 == 0) return  2;
    }
    return 1;
  }
  
  
  public boolean couldPlant(Tile t) {
    if (! t.habitat().pathClear) return false;
    if (t.onTop() instanceof Crop || ! t.reserved()) return plantType(t) > 0;
    return false;
  }
  
  
  public boolean shouldCover(Tile t) {
    return plantType(t) == 2;
  }


  public Tile[] toPlant() {
    return toPlant;
  }
  
  
  public Crop plantedAt(Tile t) {
    if (t == null || ! (t.onTop() instanceof Crop)) return null;
    return (Crop) t.onTop();
  }
  
  
  public float needForTending() {
    return needsTending;
  }
  
  
  
  /**  Economic functions-
    */
  public Traded    [] services() { return null; }
  public Background[] careers () { return null; }
  
  
  
  /**  Rendering and interface methods-
    */
  final static String
    DEFAULT_INFO = 
      "Nurseries secure a high-quality food source from plant crops, but need "+
      "space, hard labour and fertile soils.",
    POOR_SOILS_INFO =
      "The poor soils around this Nursery will hamper growth and yield a "+
      "stingy harvest.",
    WAITING_ON_SEED_INFO =
      "The land around this Nursery will have to be seeded by your "+
      ""+Backgrounds.CULTIVATOR+"s.",
    POOR_HEALTH_INFO =
      "The crops around this Nursery are sickly.  Try to improve seed stock "+
      "at the "+EcologistStation.BLUEPRINT.name+".",
    AWAITING_GROWTH_INFO =
      "The crops around this Nursery have yet to mature.  Allow them a few "+
      "days to bear fruit.";
  
  private String compileHealthReport() {
    final StringBuffer s = new StringBuffer();
    
    final int numTiles = toPlant.length;
    float
      health = 0, growth = 0, fertility = 0,
      numPlant = 0, numCarbs = 0, numGreens = 0;
    
    for (Tile t : toPlant) {
      final Crop c = plantedAt(t);
      fertility += t.habitat().moisture();
      if (c == null) continue;
      
      final float perDay = c.dailyYieldEstimate(t);
      final Traded type = Crop.yieldType(c.species());
      numPlant++;
      health    += c.health();
      growth    += c.growStage();
      if (type == CARBS ) numCarbs  += perDay;
      if (type == GREENS) numGreens += perDay;
    }
    
    if      (fertility < (numTiles * 0.5f)) s.append(POOR_SOILS_INFO     );
    else if (numPlant == 0                ) s.append(WAITING_ON_SEED_INFO);
    else if (health    < (numPlant * 0.5f)) s.append(POOR_HEALTH_INFO    );
    else if (growth    < (numPlant * 0.5f)) s.append(AWAITING_GROWTH_INFO);
    else s.append(DEFAULT_INFO);
    
    if (numCarbs  > 0) {
      s.append("\n  Estimated "+CARBS +" per day: "+I.shorten(numCarbs , 1));
    }
    if (numGreens > 0) {
      s.append("\n  Estimated "+GREENS+" per day: "+I.shorten(numGreens, 1));
    }
    return s.toString();
  }
  
  
  protected float[] goodDisplayOffsets() {
    return new float[] {
      0.0f, 0.0f,
      0.5f, 0.0f,
      1.0f, 0.0f
    };
  }
  
  
  protected Traded[] goodsToShow() {
    return new Traded[] { CARBS, PROTEIN, GREENS };
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "plantation");
  }
  
  
  public SelectionPane configPanel(SelectionPane panel, BaseUI UI) {
    return VenuePane.configSimplePanel(this, panel, UI, null);
  }
  
  
  public String helpInfo() {
    if (inWorld() && structure.intact()) return compileHealthReport();
    else return DEFAULT_INFO;
  }
}


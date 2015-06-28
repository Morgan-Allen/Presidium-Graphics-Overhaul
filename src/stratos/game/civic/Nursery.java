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
      Nursery.class, IMG_DIR+"nursery.png", 2, 1
    );
  
  final static Blueprint BLUEPRINT = new Blueprint(
    Nursery.class, "nursery",
    "Nursery", UIConstants.TYPE_ECOLOGIST, ICON,
    "Nurseries secure a high-quality food source from plant crops, but need "+
    "space, hard labour and fertile soils.",
    2, 1, Structure.IS_ZONED,
    EcologistStation.BLUEPRINT, Owner.TIER_FACILITY,
    25,  //integrity
    5,  //armour
    75,  //build cost
    Structure.NO_UPGRADES
  );
  
  final static int
    MAX_AREA_SIDE = Stage.ZONE_SIZE - 4,
    MIN_AREA_SIDE = BLUEPRINT.size  + 4;
  
  final public static Conversion
    LAND_TO_CARBS = new Conversion(
      BLUEPRINT, "land_to_carbs",
      TO, 1, CARBS
    ),
    LAND_TO_GREENS = new Conversion(
      BLUEPRINT, "land_to_greens",
      TO, 1, GREENS
    );
  
  
  private Box2D areaClaimed = new Box2D();
  private Tile toPlant[] = new Tile[0];
  private float needsTending = 0;
  
  
  public Nursery(Base base) {
    super(BLUEPRINT, base);
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
  final static Siting SITING = new Siting(BLUEPRINT) {
    
    public float ratePointDemand(Base base, Target point, boolean exact) {
      final Stage world = point.world();
      final Tile under = world.tileAt(point);
      
      final Venue station = (Venue) world.presences.nearestMatch(
        EcologistStation.class, point, -1
      );
      if (station == null || station.base() != base) return -1;
      final float distance = Spacing.distance(point, station);
      
      float rating = super.ratePointDemand(base, point, exact);
      rating *= world.terrain().fertilitySample(under) * 2;
      rating /= 1 + (distance / Stage.ZONE_SIZE);
      return rating;
    }
  };
  
  
  public boolean setupWith(Tile position, Box2D area, Coord... others) {
    
    final Tile oldOrigin = origin();
    if (! super.setupWith(position, area, others)) return false;
    //
    //  By default, we claim an area 2 tiles larger than the basic footprint,
    //  but we can also have a larger area assigned (e.g, by a human player or
    //  by an automated placement-search.)
    
    final Stage world = position.world;
    final Box2D
      minArea = new Box2D(footprint()).expandTo(MIN_AREA_SIDE),
      foot    = footprint();
    
    if (area == null) {
      areaClaimed.setTo(minArea);
      areaClaimed.expandTo(MAX_AREA_SIDE);
      
      //  TODO:  Crop with an extra margin to allow clearance!
      areaClaimed.setTo(world.claims.cropNewClaim(this, areaClaimed));
      
      //  TODO:  This can result in areas too large.
      //areaClaimed.expandToUnit(Stage.UNIT_GRID_SIZE);
    }
    else {
      areaClaimed.setTo(area);
    }
    if (! foot.containedBy(areaClaimed)) areaClaimed.setTo(foot);
    //
    //  NOTE:  Facing must be set before crop-tiles are settled on, as this
    //  affects row-orientation!
    setFacing(areaClaimed.xdim() > areaClaimed.ydim() ?
      FACING_SOUTH : FACING_EAST
    );
    if (origin() != oldOrigin) scanForCropTiles();
    return true;
  }
  
  
  public boolean canPlace(Account reasons) {
    if (! super.canPlace(reasons)) return false;
    final Stage world = origin().world;
    
    if (areaClaimed.maxSide() > MAX_AREA_SIDE) {
      return reasons.setFailure("Area is too large!");
    }
    if (areaClaimed.minSide() < MIN_AREA_SIDE) {
      return reasons.setFailure("Area is too small!");
    }
    
    if (! SiteUtils.pathingOkayAround(this, areaClaimed, owningTier(), world)) {
    //if (! SiteUtils.checkAroundClaim(this, areaClaimed, world)) {
      return reasons.setFailure("Might obstruct pathing");
    }
    return true;
  }
  
  
  public Box2D areaClaimed() {
    return areaClaimed;
  }
  
  
  public Tile[] reserved() {
    return toPlant;
  }
  
  
  public boolean preventsClaimBy(Venue other) {
    return true;
  }
  
  
  
  
  /**  Utility methods for handling tile-planting:
    */
  private int plantType(Tile t) {
    if (! areaClaimed.contains(t.x, t.y, 1)) return -1;
    
    final boolean across = facing() == FACING_NORTH || facing() == FACING_SOUTH;
    final int s = 1 + Nums.round(t.world.size, 6, true);  //  Modulus offset.
    final Tile o = origin();
    
    for (Tile n : t.allAdjacent(null)) {
      if (n == null || (n.above() instanceof Crop)) continue;
      if (n.pathType() >= Tile.PATH_HINDERS && n.reserved()) return -1;
    }
    
    if (footprint().contains(t.x, t.y, -1)) return -1;
    if (across) {
      if (t.y == o.y) return -1;
      if ((t.x + s - o.x) % 3 == 2) return -1;
      if ((t.x + s - o.x) % 6 == 0) return  2;
    }
    else {
      if (t.x == o.x) return -1;
      if ((t.y + s - o.y) % 3 == 2) return -1;
      if ((t.y + s - o.y) % 6 == 0) return  2;
    }
    return 1;
  }
  
  
  private void scanForCropTiles() {
    final boolean report = verbose && I.talkAbout == this;
    //
    //  TODO:  Try only doing this once, during initial setup- and avoid the
    //  extra-cropping step.  (Or consider reserving this for the preview-
    //  methods only?)
    //
    //  We then grab all plantable tiles in the area claimed.
    final Batch <Tile> grabbed = new Batch <Tile> ();
    if (report) {
      I.say("\nORIGINAL AREA CLAIMED: "+areaClaimed);
      I.say("  FOOTPRINT:   "+footprint());
      I.say("  ORIGIN TILE: "+origin());
    }
    
    final Stage world = origin().world;
    for (Tile t : world.tilesIn(areaClaimed, true)) {
      if (couldPlant(t)) {
        grabbed.add(t);
        if (report) I.say("  WILL PLANT AT: "+t);
      }
    }
    toPlant = grabbed.toArray(Tile.class);
    if (report) I.say("NEW CROPPED AREA: "+areaClaimed);
  }
  
  
  public boolean couldPlant(Tile t) {
    if (! t.habitat().pathClear) return false;
    if (t.above() instanceof Crop || ! t.reserved()) return plantType(t) > 0;
    return false;
  }
  
  
  public boolean shouldCover(Tile t) {
    return plantType(t) == 2;
  }


  public Tile[] toPlant() {
    return toPlant;
  }
  
  
  public Crop plantedAt(Tile t) {
    if (t == null || ! (t.above() instanceof Crop)) return null;
    return (Crop) t.above();
  }
  
  
  public float needForTending() {
    return needsTending;
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
  
  
  
  /**  Other general update methods-
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

    if (report) I.say("\nGETTING UNPLANTED TILES FOR AREA");
    final Batch <Tile> around = new Batch <Tile> ();
    for (Tile t : world.tilesIn(areaClaimed, true)) {
      if (! couldPlant(t)) {
        around.add(t);
        if (report) I.say("  TILE AT: "+t.x+"|"+t.y);
      }
    }
    base.transport.updatePerimeter(this, inWorld, around);
  }
  
  
  
  /**  Economic functions-
    */
  public Traded    [] services() { return null; }
  public Background[] careers () { return null; }
  
  
  
  /**  Rendering and interface methods-
    */
  final static String
    POOR_SOILS_INFO =
      "The poor soils around this Nursery will hamper growth and yield a "+
      "stingy harvest.",
    WAITING_ON_SEED_INFO =
      "The land around this Nursery will have to be seeded by your "+
      ""+Backgrounds.CULTIVATOR+"s.",
    POOR_HEALTH_INFO =
      "The crops around this Nursery are sickly.  Try to improve seed stock "+
      "at the "+EcologistStation.BLUEPRINT+".",
    AWAITING_GROWTH_INFO =
      "The crops around this Nursery have yet to mature.  Allow them a few "+
      "days to bear fruit.";
  
  private String compileOutputReport() {
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
    else s.append(BLUEPRINT.description);
    
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
  
  
  public SelectionPane configSelectPane(SelectionPane panel, BaseUI UI) {
    return VenuePane.configSimplePanel(this, panel, UI, null);
  }
  
  
  public String helpInfo() {
    if (inWorld() && structure.intact()) return compileOutputReport();
    else return super.helpInfo();
  }
}


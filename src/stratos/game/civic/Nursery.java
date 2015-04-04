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



//  TODO:  Introduce sub-classes of the nursery specifically intended for
//  aquaculture and forestry.

public class Nursery extends Venue implements TileConstants {
  
  
  /**  Constructors, data fields, setup and save/load methods-
    */
  protected static boolean verbose = false;
  
  final static String IMG_DIR = "media/Buildings/ecologist/";
  final static ModelAsset
    NURSERY_MODEL = CutoutModel.fromImage(
      Nursery.class, IMG_DIR+"nursery.png", 2, 2
    );
  
  final public static float
    MATURE_DURATION  = Stage.STANDARD_DAY_LENGTH * 5,
    GROW_INCREMENT   = Stage.GROWTH_INTERVAL / MATURE_DURATION,
    EXTRA_CLAIM_SIZE = 4,
    
    CEREAL_BONUS = 2.00f,
    HIVE_DIVISOR = 4.00f,
    DRYLAND_MULT = 0.75f,
    WETLAND_MULT = 1.25f,
    
    NURSERY_CARBS   = 1,
    NURSERY_GREENS  = 0.5f,
    NURSERY_PROTEIN = 0.5f;
  
  //  TODO:  You need to include some conversions here to allow for supply/
  //         demand evaluation.
  final static VenueProfile PROFILE = new VenueProfile(
    Nursery.class, "nursery", "Nursery",
    2, 2, Venue.Type.TYPE_FIXTURE,
    EcologistStation.PROFILE, Owner.TIER_FACILITY
  );
  
  
  private Box2D areaClaimed = new Box2D();
  private Tile toPlant[] = new Tile[0];
  private float needsTending = 0;
  
  
  public Nursery(Base base) {
    super(PROFILE, base);
    structure.setupStats(
      25,  //integrity
      5,  //armour
      15,  //build cost
      0,  //max upgrades
      Structure.TYPE_FIXTURE
    );
    staff.setShiftType(SHIFTS_BY_DAY);
    this.attachModel(NURSERY_MODEL);
  }
  
  
  public Nursery(Session s) throws Exception {
    super(s);
    areaClaimed.loadFrom(s.input());
    type         = s.loadInt();
    toPlant      = (Tile[]) s.loadObjectArray(Tile.class);
    needsTending = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    areaClaimed.saveTo(s.output());
    s.saveInt        (type        );
    s.saveObjectArray(toPlant     );
    s.saveFloat      (needsTending);
  }
  
  
  
  /**  Placement and supply-demand functions-
    */
  public boolean setPosition(float x, float y, Stage world) {
    final boolean okay = super.setPosition(x, y, world);
    if (okay) areaClaimed.setTo(footprint()).expandBy((int) EXTRA_CLAIM_SIZE);
    return okay;
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
    if (distance > Stage.SECTOR_SIZE) return -1;
    
    float demand = base.demands.globalShortage(Nursery.class);
    final Tile under = world.tileAt(point);
    float rating = 0;
    rating += world.terrain().fertilitySample (under);
    rating += world.terrain().insolationSample(under);
    rating /= 1 + (distance / Stage.SECTOR_SIZE);
    return rating * 10 * demand;
  }
  
  
  private void scanForCropTiles() {
    final boolean report = verbose && I.talkAbout == this;
    
    final Box2D cropArea = new Box2D().setTo(areaClaimed).expandBy(-1);
    final Batch <Tile> grabbed = new Batch <Tile> ();
    if (report) I.say("\nCROP AREA: "+cropArea);
    
    for (Tile t : world.tilesIn(cropArea, true)) {
      if (! couldPlant(t)) continue;
      grabbed.add(t);
      if (report && plantedAt(t) == null) I.say("  ADDING TILE: "+t);
    }
    
    //  TODO:  Grab contiguous areas and put 'covered' crops along one edge.
    toPlant = grabbed.toArray(Tile.class);
  }
  
  
  public boolean couldPlant(Tile t) {
    if (PavingMap.pavingReserved(t, true) || t.reserved()) return false;
    return true;
  }
  
  
  protected void checkCropStates() {
    final boolean report = verbose && I.talkAbout == this;
    
    if (report) I.say("CHECKING CROP STATES");
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
    
    structure.setAmbienceVal(2);
    if (numUpdates % 10 == 0) {
      scanForCropTiles();
      checkCropStates();
    }
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
  private String compileHealthReport() {
    final StringBuffer s = new StringBuffer();
    s.append(
      "Plantations of cropland secure a high-quality food source, but need "+
      "space and constant attention."
    );
    if (inWorld() && structure.intact()) {
      float health = 0, growth = 0, numC = 0;
      for (Tile t : toPlant) {
        final Crop c = plantedAt(t);
        if (c == null) continue;
        numC++;
        health += c.health();
        growth += c.growStage();
      }
      if (numC > 0) {
        final int PH = (int) (health * 100 / numC);
        final int PG = (int) (growth * 100 / (numC * Crop.MAX_GROWTH));
        s.append("\n  Crop growth: "+PG+"%");
        s.append("\n  Crop health: "+PH+"%");
      }
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
    return Composite.withImage(EcologistStation.ICON, "plantation");
  }
  
  
  public SelectionPane configPanel(SelectionPane panel, BaseUI UI) {
    return VenuePane.configSimplePanel(this, panel, UI, null);
  }
  
  
  public String helpInfo() {
    return compileHealthReport();
  }
  
  
  public String objectCategory() {
    return InstallationPane.TYPE_ECOLOGIST;
  }
}


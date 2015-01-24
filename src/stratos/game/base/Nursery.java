/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.base;

import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.game.wild.Species;
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
  private static boolean verbose = false;
  
  final static String IMG_DIR = "media/Buildings/ecologist/";
  final static ModelAsset
    NURSERY_MODEL = CutoutModel.fromImage(
      Nursery.class, IMG_DIR+"nursery.png", 2, 2
    );
  
  final public static float
    MATURE_DURATION = Stage.STANDARD_DAY_LENGTH * 5,
    GROW_INCREMENT  = Stage.GROWTH_INTERVAL / MATURE_DURATION,
    
    EXTRA_CLAIM_SIZE = 4,
    
    MAX_HEALTH_BONUS     = 2.0f,
    INFEST_GROW_PENALTY  = 0.5f,
    POLLUTE_GROW_PENALTY = 0.5f,
    UPGRADE_GROW_BONUS   = 0.25f,
    
    CEREAL_BONUS = 2.00f,
    DRYLAND_MULT = 0.75f,
    WETLAND_MULT = 1.25f,
    
    NURSERY_CARBS   = 1,
    NURSERY_GREENS  = 0.5f,
    NURSERY_PROTEIN = 0.5f;
  
  //  TODO:  You need to include some conversions here to allow for supply/
  //         demand evaluation.
  
  final static VenueProfile PROFILE = new VenueProfile(
    Nursery.class, "nursery",
    2, 2, ENTRANCE_SOUTH
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
    type    = s.loadInt();
    toPlant = (Tile[]) s.loadObjectArray(Tile.class);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    areaClaimed.saveTo(s.output());
    s.saveInt        (type);
    s.saveObjectArray(toPlant);
  }
  

  public int owningTier() {
    return TIER_PRIVATE;
  }
  
  
  
  /**  Placement and supply-demand functions-
    */
  public boolean setPosition(float x, float y, Stage world) {
    final boolean okay = super.setPosition(x, y, world);
    if (okay) areaClaimed.setTo(footprint()).expandBy((int) EXTRA_CLAIM_SIZE);
    return okay;
  }
  
  
  protected Box2D areaClaimed() {
    return areaClaimed;
  }
  
  
  public float ratePlacing(Target point, boolean exact) {
    
    //  TODO:  THESE HAVE TO BE ECOLOGIST STATIONS BELONGING TO THE SAME BASE.
    //  ...It might be worth getting supply-and-demand in order here first.
    
    //  TODO:  Base the demand for this on the tech-level of the parent station
    //  -by default, you only get one nursery.
    
    final Stage world = point.world();
    final Presences presences = world.presences;
    
    final EcologistStation station = (EcologistStation) presences.nearestMatch(
      EcologistStation.class, point, -1
    );
    if (station == null || station.base() != base) return -1;
    
    final float distance = Spacing.distance(point, station);
    if (distance > Stage.SECTOR_SIZE) return -1;
    
    final Nursery nearby = (Nursery) presences.nearestMatch(
      Nursery.class, point, Stage.SECTOR_SIZE
    );
    if (nearby != null & nearby != this) return -1;
    
    final Tile under = world.tileAt(point);
    float rating = 0;
    rating += world.terrain().fertilitySample (under);
    rating += world.terrain().insolationSample(under);
    rating /= 1 + (distance / Stage.SECTOR_SIZE);
    return (rating / 2) * 10;
  }
  
  
  private void scanForCropTiles() {
    final boolean report = verbose && I.talkAbout == this;
    
    final Box2D cropArea = new Box2D().setTo(areaClaimed).expandBy(-1);
    final Batch <Tile> grabbed = new Batch <Tile> ();
    if (report) I.say("\nCROP AREA: "+cropArea);
    
    for (Tile t : world.tilesIn(cropArea, true)) {
      if (PavingMap.pavingReserved(t) || t.reserved()) continue;
      grabbed.add(t);
      if (report && plantedAt(t) == null) I.say("  ADDING TILE: "+t);
    }
    
    //  TODO:  Grab contiguous areas and put 'covered' crops along one edge.
    
    toPlant = grabbed.toArray(Tile.class);
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
      
      final float
        growth = 10 * 1f / MATURE_DURATION,
        decay  = growth / 10;
      for (Item seed : stocks.matches(SAMPLES)) {
        stocks.removeItem(Item.withAmount(seed, decay));
      }
      stocks.bumpItem(CARBS  , growth * NURSERY_CARBS  );
      stocks.bumpItem(GREENS , growth * NURSERY_GREENS );
      stocks.bumpItem(PROTEIN, growth * NURSERY_PROTEIN);
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
  public int numOpenings(Background v) {
    int num = super.numOpenings(v);
    if (v == CULTIVATOR) return num + 2;
    return 0;
  }
  
  
  public Traded[] services() { return new Traded[] {
    CARBS, PROTEIN, GREENS, LCHC
  }; }
  
  
  public Background[] careers() { return new Background[] {
    CULTIVATOR
  }; }
  
  
  public Behaviour jobFor(Actor actor, boolean onShift) {
    final Choice choice = new Choice(actor);
    
    //  If you're really short on food, consider foraging in the surrounds or
    //  farming 24/7.
    final float shortages = (
      stocks.shortagePenalty(CARBS ) +
      stocks.shortagePenalty(GREENS)
    ) / 2f;
    if (shortages > 0) {
      final Farming farming = new Farming(actor, this);
      choice.add(farming);
      
      final Foraging foraging = new Foraging(actor, this);
      foraging.setMotive(Plan.MOTIVE_EMERGENCY, Plan.PARAMOUNT * shortages);
      choice.add(foraging);
    }
    
    //if (choice.size() > 0) return choice.pickMostUrgent();
    if (! staff.onShift(actor)) return choice.pickMostUrgent();
    
    //  Otherwise, consider normal deliveries and routine tending-
    final Delivery d = DeliveryUtils.bestBulkDeliveryFrom(
      this, services(), 2, 10, 5
    );
    choice.add(d);
    choice.add(bestSeedCollection(actor));
    choice.add(new Farming(actor, this));
    
    //  In addition to forestry operations-
    choice.add(Forestry.nextPlanting(actor, this));
    choice.add(Forestry.nextCutting (actor, this));
    
    return choice.pickMostUrgent();
  }
  
  
  private Delivery bestSeedCollection(Actor actor) {
    final Pick <Delivery> pick = new Pick <Delivery> ();
    
    for (Object t : world.presences.sampleFromMap(
      this, world, 3, null, EcologistStation.class
    )) {
      final EcologistStation station = (EcologistStation) t;
      if (! station.allowsEntry(actor)) continue;
      
      final Batch <Item> seedTypes = new Batch <Item> ();
      float rating = 0;
      
      for (Species s : Crop.ALL_VARIETIES) {
        Item seed = Item.withReference(SAMPLES, s);
        seed = station.stocks.bestSample(seed, 1);
        if (seed == null || stocks.amountOf(seed) > 0) continue;
        seedTypes.add(seed);
        rating += seed.quality + 0.5f;
      }
      
      final Delivery seedD = new Delivery(seedTypes, station, this);
      if (Plan.competition(seedD, station, actor) > 0) continue;
      seedD.replace = true;
      pick.compare(seedD, rating);
    }
    
    return pick.result();
  }
  
  
  
  /**  Rendering and interface methods-
    */
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


  public String fullName() {
    return "Nursery";
  }
  
  
  public SelectionInfoPane configPanel(SelectionInfoPane panel, BaseUI UI) {
    return VenueDescription.configSimplePanel(this, panel, UI, null);
  }
  
  
  public String helpInfo() {
    /*
    return
      "Nurseries allow young plants to be cultivated in a secure environment "+
      "prior to outdoor planting, and provide a small but steady food yield "+
      "regardless of outside conditions.";
    //*/
    return
      "Plantations of managed, mixed-culture cropland secure a high-quality "+
      "food source for your base, but require space and constant attention.";
  }
  
  
  public String objectCategory() {
    return InstallTab.TYPE_ECOLOGIST;
  }
}


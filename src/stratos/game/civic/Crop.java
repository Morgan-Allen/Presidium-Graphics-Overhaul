/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.civic;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;
import static stratos.game.wild.Flora.*;



public class Crop extends Element {
  
  
  final static String IMG_DIR = "media/Buildings/ecologist/";
  final static CutoutModel
    COVERING_LEFT = CutoutModel.fromImage(
      Nursery.class, IMG_DIR+"covering_left.png", 1, 1
    ),
    COVERING_RIGHT = CutoutModel.fromImage(
      Nursery.class, IMG_DIR+"covering_right.png", 1, 1
    ),
    CROP_MODELS[][] = CutoutModel.fromImageGrid(
      Nursery.class, IMG_DIR+"all_crops.png",
      4, 4, 0.5f, 0.5f, false
    ),
    GRUB_BOX_MODEL = CutoutModel.fromImage(
      Nursery.class, IMG_DIR+"grub_box.png", 0.5f, 0.5f
    );
  
  
  final public static int
    NOT_PLANTED =  0,
    MIN_GROWTH  =  1,
    MIN_HARVEST =  3,
    MAX_GROWTH  =  4;
  final public static float
    NO_HEALTH  = -1,
    MIN_HEALTH =  0,
    MAX_HEALTH =  2;
  
  final public static String STAGE_NAMES[] = {
    "Unplanted ",
    "Sprouting ",
    "Growing ",
    "Mature ",
    "Ripened "
  };
  final public static String HEALTH_NAMES[] = {
    "Feeble",
    "Poor",
    "Fair",
    "Good",
    "Excellent",
    "Perfect"
  };
  
  
  final public Nursery parent;
  
  private Species species;
  private float growStage, quality;
  private boolean blighted, covered;
  
  
  public Crop(Nursery parent, Species species) {
    super();
    this.parent = parent;
    this.species = species;
    growStage = NOT_PLANTED;
    quality = 1.0f;
  }
  
  
  public Crop(Session s) throws Exception {
    super(s);
    s.cacheInstance(this);
    parent    = (Nursery) s.loadObject();
    species   = (Species) s.loadObject();
    growStage = s.loadFloat();
    quality   = s.loadFloat();
    blighted  = s.loadBool ();
    covered   = s.loadBool ();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(parent   );
    s.saveObject(species  );
    s.saveFloat (growStage);
    s.saveFloat (quality  );
    s.saveBool  (blighted );
    s.saveBool  (covered  );
  }
  
  
  public int pathType() {
    if (covered) return Tile.PATH_BLOCKS;
    return Tile.PATH_HINDERS;
  }
  
  
  public int owningTier() {
    if (parent != null && parent.structure.intact() && parent.inWorld()) {
      return Owner.TIER_PRIVATE;
    }
    return Owner.TIER_TERRAIN;
  }
  
  
  
  /**  Growth calculations-
    */
  final public static Species ALL_VARIETIES[] = {
    ONI_RICE,
    DURWHEAT,
    TUBER_LILY,
    BROADFRUITS,
    HIVE_GRUBS
  };
  
  final static Object CROP_SPECIES[][] = {
    new Object[] { ONI_RICE   , CARBS , CROP_MODELS[0] },
    new Object[] { DURWHEAT   , CARBS , CROP_MODELS[1] },
    new Object[] { TUBER_LILY , GREENS, CROP_MODELS[3] },
    new Object[] { BROADFRUITS, GREENS, CROP_MODELS[2] },
    new Object[] {
      HIVE_GRUBS, PROTEIN,
      new ModelAsset[] { GRUB_BOX_MODEL }
    },
    null,
    null,
    new Object[] { WILD_FLORA, GREENS, null },
  };
  
  
  public static ModelAsset speciesModel(Species s, int growStage) {
    final int varID = Visit.indexOf(s, ALL_VARIETIES);
    final ModelAsset seq[] = (ModelAsset[]) CROP_SPECIES[varID][2];
    return seq[Nums.clamp(growStage, seq.length)];
  }
  
  
  static boolean isHive(Species s) {
    return s == HIVE_GRUBS || s == BLUE_VALVES;
  }
  
  
  static boolean isCereal(Species s) {
    return s == DURWHEAT || s == ONI_RICE;
  }
  
  
  static boolean isDryland(Species s) {
    return s == DURWHEAT || s == BROADFRUITS;
  }
  
  
  static Crop cropAt(Tile t) {
    if (t.onTop() instanceof Crop) {
      return (Crop) t.onTop();
    }
    return null;
  }
  
  
  public static Traded yieldType(Species species) {
    if (species == null) return null;
    final Traded type;
    if (isHive(species)) {
      type = Economy.PROTEIN;
    }
    else if (isCereal(species)) {
      type = Economy.CARBS;
    }
    else type = Economy.GREENS;
    return type;
  }
  
  
  public static float habitatBonus(Tile t, Species s, Item seed) {
    float bonus = 0.0f;
    
    //  First, apply appropriate modifier for microclimate-
    final float moisture = t.habitat().moisture() / 10f;
    final boolean hive = isHive(s);
    
    if (hive) {
      bonus = (1 + moisture) * 0.5f / Nursery.HIVE_DIVISOR;
    }
    else if (isDryland(s)) {
      bonus = Nursery.DRYLAND_MULT * (1 + moisture) / 2f;
    }
    else {
      bonus = moisture * Nursery.WETLAND_MULT;
    }
    
    //  Then, we determine bonus based on crop type-
    if (hive) {
      bonus += 0.5f / Nursery.HIVE_DIVISOR;
    }
    else if (isCereal(s)) {
      bonus *= Nursery.CEREAL_BONUS;
    }
    else {
      bonus += 0;
    }
    
    //  And if a seed is provided, that as well-
    if (seed != null) {
      if (hive) bonus += seed.quality / Nursery.HIVE_DIVISOR;
      else bonus += seed.quality;
      return bonus;
    }
    else return Nums.clamp(bonus, 0, MAX_HEALTH);
  }
  
  
  public void seedWith(Species s, float quality) {
    this.species   = s;
    this.quality   = Nums.clamp(quality, 0, MAX_HEALTH);
    this.growStage = MIN_GROWTH;
    this.covered   = parent.shouldCover(origin());
    updateSprite();
  }
  
  
  public void onGrowth(Tile tile) {
    //
    //  Crops disappear once their parent nursery is salvaged or destroyed, and
    //  can't grow if they're not seeded.
    if (parent == null || ! (parent.inWorld() && parent.couldPlant(tile))) {
      setAsDestroyed();
      return;
    }
    if (growStage == NOT_PLANTED || species == null) return;
    
    final boolean report = Nursery.verbose && I.talkAbout == parent;
    final float
      dailyGrowth = dailyGrowthEstimate(tile, report),
      health      = quality / MAX_HEALTH,
      increment   = dailyGrowth * MAX_GROWTH / Nursery.GROW_TIMES_PER_DAY;
    
    if (Rand.num() < increment * (1 - health)) blighted = true;
    growStage = Nums.clamp(growStage + increment, MIN_GROWTH, MAX_GROWTH);
    //
    //  Update biomass and possibly sprite state-
    world.ecology().impingeBiomass(
      origin(), growStage() / 2f, Stage.GROWTH_INTERVAL
    );
    updateSprite();
  }
  
  
  private float dailyGrowthEstimate(Tile tile, boolean report) {
    if (blighted) return -1f / Nursery.NUM_DAYS_MATURE;
    
    final Stage world = parent.world();
    float
      increment = 1f / Nursery.NUM_DAYS_MATURE,
      health    = quality / MAX_HEALTH,
      growBonus = habitatBonus(tile, species, null),
      pollution = 0 - world.ecology().ambience.valueAt(tile),
      waterNeed = parent.stocks.relativeShortage(WATER);
    
    if (report) I.reportVars("\nEstimating crop growth", "  ",
      "Increment" , increment,
      "Health"    , health   ,
      "Grow bonus", growBonus,
      "Pollution" , pollution,
      "Water need", waterNeed,
      "Grow stage", growStage,
      "Blighted?" , blighted 
    );
    
    increment *= growBonus * (1 + health) / 2;
    if (pollution > 0) increment *= (2 - pollution) / 2;
    if (waterNeed > 0) increment *= (2 - waterNeed) / 2;
    
    return increment;
  }
  
  
  public float dailyYieldEstimate(Tile tile) {
    final float fullAmount = 1;
    return dailyGrowthEstimate(tile, false) * fullAmount;
  }
  
  
  public Item yieldCrop() {
    final Traded type = yieldType(species);
    final float amount = growStage / MAX_GROWTH;
    growStage = NOT_PLANTED;
    quality = NO_HEALTH;
    blighted = false;
    parent.checkCropStates();
    
    updateSprite();
    return Item.withAmount(type, amount);
  }
  
  
  public void disinfest() {
    blighted = false;
  }
  
  
  public boolean needsTending() {
    return
      blighted ||
      growStage == NOT_PLANTED ||
      growStage >= MIN_HARVEST;
  }
  
  
  public boolean blighted() {
    return blighted;
  }
  
  
  public int growStage() {
    return (int) growStage;
  }
  
  
  public float health() {
    return quality / ((blighted ? 2f : 1f) * MAX_HEALTH);
  }
  
  
  public Species species() {
    return species;
  }
  
  
  
  /**  Rendering and interface-
    */
  protected void updateSprite() {
    if (covered) {
      final int f = parent.facing();
      boolean across = f == Venue.FACING_NORTH || f == Venue.FACING_SOUTH;
      if (across) attachModel(COVERING_RIGHT);
      else        attachModel(COVERING_LEFT );
      return;
    }
    final GroupSprite old = (GroupSprite) sprite();
    final int stage = Nums.round(growStage, 1, true);
    final ModelAsset model = speciesModel(species, stage);
    if (old != null && old.atIndex(0).model() == model) return;
    
    final GroupSprite GS = new GroupSprite();
    GS.attach(model, -0.25f, -0.25f, 0);
    GS.attach(model,  0.25f, -0.25f, 0);
    GS.attach(model, -0.25f,  0.25f, 0);
    GS.attach(model,  0.25f,  0.25f, 0);
    attachSprite(GS);
    
    if (old != null) world.ephemera.addGhost(this, 1, old, 2.0f);
  }
  
  
  public String toString() {
    return species.name;
  }
}




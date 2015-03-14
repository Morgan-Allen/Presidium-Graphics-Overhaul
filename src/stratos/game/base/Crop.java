/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.base;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.wild.Species;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;



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
      4, 4, 0.5f, 0.5f
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
  
  
  
  /**  Growth calculations-
    */
  final public static Species ALL_VARIETIES[] = {
    Species.ONI_RICE,
    Species.DURWHEAT,
    Species.TUBER_LILY,
    Species.BROADFRUITS,
    Species.HIVE_GRUBS
  };
  
  final static Object CROP_SPECIES[][] = {
    new Object[] { Species.ONI_RICE   , CARBS , CROP_MODELS[0] },
    new Object[] { Species.DURWHEAT   , CARBS , CROP_MODELS[1] },
    new Object[] { Species.TUBER_LILY , GREENS, CROP_MODELS[3] },
    new Object[] { Species.BROADFRUITS, GREENS, CROP_MODELS[2] },
    new Object[] {
      Species.HIVE_GRUBS, PROTEIN,
      new ModelAsset[] { GRUB_BOX_MODEL }
    },
    null,
    null,
    new Object[] { Species.TIMBER, GREENS, null },
  };
  
  
  public static ModelAsset speciesModel(Species s, int growStage) {
    final int varID = Visit.indexOf(s, ALL_VARIETIES);
    final ModelAsset seq[] = (ModelAsset[]) CROP_SPECIES[varID][2];
    return seq[Nums.clamp(growStage, seq.length)];
  }
  
  
  static boolean isHive(Species s) {
    return s == Species.HIVE_GRUBS || s == Species.BLUE_VALVES;
  }
  
  
  static boolean isCereal(Species s) {
    return s == Species.DURWHEAT || s == Species.ONI_RICE;
  }
  
  
  static boolean isDryland(Species s) {
    return s == Species.DURWHEAT || s == Species.BROADFRUITS;
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
  
  
  public static float habitatBonus(Tile t, Species s) {
    float bonus = 0.0f;
    
    //  First, apply appropriate modifier for microclimate-
    final float moisture = t.habitat().moisture() / 10f;
    if (isDryland(s)) {
      bonus = Nursery.DRYLAND_MULT * (1 + moisture) / 2f;
    }
    else bonus = moisture * Nursery.WETLAND_MULT;
    
    //  Then, we determine bonus based on crop type-
    if (isHive(s)) {
      bonus += 0.5f / Nursery.HIVE_DIVISOR;
    }
    else if (isCereal(s)) {
      bonus *= Nursery.CEREAL_BONUS;
    }
    return Nums.clamp(bonus, 0, Nursery.MAX_HEALTH_BONUS);
  }
  
  
  public void seedWith(Species s, float quality) {
    this.species   = s;
    this.quality   = Nums.clamp(quality, 0, Nursery.MAX_HEALTH_BONUS);
    this.growStage = MIN_GROWTH;
    this.covered   = (origin().x - parent.origin().x) % 4 == 0;
    //  TODO:  Try to smarten up the system for determining coverage.
    updateSprite();
  }
  
  
  public void onGrowth(Tile tile) {
    
    if (parent == null || ! parent.inWorld()) {
      setAsDestroyed();
      return;
    }
    if (growStage == NOT_PLANTED || species == null) return;
    
    //  TODO:  Possibly combine with irrigation effects from water supply or
    //  life support?
    
    final Stage world = parent.world();
    final float pollution = Nums.clamp(
      tile.world.ecology().ambience.valueAt(tile), 0, 1
    );
    
    float increment = 1f;
    increment -= (pollution * Nursery.POLLUTE_GROW_PENALTY);
    if (blighted) increment -= Nursery.INFEST_GROW_PENALTY;
    if (increment > 0) {
      increment *= Planet.dayValue(world) * 2;
      increment *= quality * habitatBonus(tile, species);
    }
    increment *= Rand.num() * 2 * Nursery.GROW_INCREMENT * MAX_GROWTH;
    
    growStage = Nums.clamp(growStage + increment, MIN_GROWTH, MAX_GROWTH);
    checkBlight(pollution);

    //  Update biomass and possibly sprite state-
    world.ecology().impingeBiomass(
      origin(), growStage() / 2f, Stage.GROWTH_INTERVAL
    );
    updateSprite();
  }
  
  
  private void checkBlight(float pollution) {
    if (growStage <= MIN_GROWTH) { blighted = false; return; }
    float blightChance = (pollution + MAX_HEALTH - quality) / MAX_HEALTH;
    
    //  The chance of contracting disease increases if near infected plants of
    //  the same species, and decreases with access to a hive.
    final Tile o = this.origin();
    final Tile t = Spacing.pickRandomTile(this, 4, o.world);
    final Crop c = cropAt(t);
    if (c != null) {
      if (c.species == this.species && c.blighted) blightChance += 1;
      else if (isHive(c.species) && ! isHive(this.species)) blightChance -= 1;
    }
    
    //  Better-established plants can fight off infection more easily, and if
    //  infection-chance is low, spontaneous recovery can occur.
    blightChance *= 2f / (2 + (growStage / MAX_GROWTH));
    float recoverChance = (1f - blightChance) * Nursery.GROW_INCREMENT / 2;
    blightChance *= Nursery.GROW_INCREMENT;
    if (blighted && Rand.num() < recoverChance) blighted = false;
    if (Rand.num() < blightChance && ! blighted) blighted = true;
    if (growStage <= MIN_GROWTH) blighted = false;
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
    return quality / (blighted ? 2f : 1f);
  }
  
  
  public Species species() {
    return species;
  }
  
  
  
  /**  Rendering and interface-
    */
  protected void updateSprite() {
    if (covered) {
      attachModel(COVERING_RIGHT);
      return;
    }
    final GroupSprite old = (GroupSprite) sprite();
    final ModelAsset model = speciesModel(species, (int) growStage);
    if (old != null && old.atIndex(0).model() == model) return;
    
    final GroupSprite GS = new GroupSprite();
    GS.attach(model, -0.25f, -0.25f, 0);
    GS.attach(model,  0.25f, -0.25f, 0);
    GS.attach(model, -0.25f,  0.25f, 0);
    GS.attach(model,  0.25f,  0.25f, 0);
    attachSprite(GS);
  }
  
  
  public String toString() {
    return species.name;
    /*
    final int stage = (int) Nums.clamp(growStage, 0, MAX_GROWTH);
    final String HD;
    if (blighted) HD = " (Infested)";
    else {
      final int HL = Nums.clamp((int) quality, 5);
      HD = " ("+HEALTH_NAMES[HL]+" health)";
    }
    return STAGE_NAMES[stage]+""+species.name+HD;
    //*/
  }
}







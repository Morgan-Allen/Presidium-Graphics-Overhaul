/**  
 *  Written by Morgan Allen.
 *  I intend to slap on some kind of open-source license here in a while, but
 *  for now, feel free to poke around for non-commercial purposes.
 */
package stratos.game.wild;
import static stratos.game.craft.Economy.*;

import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.maps.TerrainGen;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import stratos.game.wild.Species.Type;



/*
 Crops and Flora include:
   Durwheat                     (primary carbs on land)
   Bulrice                      (primary carbs in water)
   Broadfruits                  (secondary greens on land)
   Tuber lily                   (secondary greens in water)
   Ant/termite/bee/worm cells   (tertiary protein on land)
   Fish/mussel/clam farming     (tertiary protein in water)
   
   Vapok Canopy/Broadleaves  (tropical)
   Mixtaob Tree/Glass Cacti  (desert)
   Redwood/Cushion Plants    (tundra)
   Strain XV97/Mycon Bloom   (wastes)
   Lichens/Annuals           (pioneer species)
   Coral Beds/Algal Forest   (rivers/oceans)
   
   Lumen forest (changer) + Rhizome (glaive knight) + Manna tree (collective)
   Albedan ecology:  Carpets + Metastases + Amoeba Clade
//*/

public class Flora extends Element implements TileConstants {
  
  
  /**
   * Field definitions and constructors-
   */
  private static boolean
    initVerbose    = false,
    updatesVerbose = false;
  
  final public static int
    NOT_PLANTED = -1,
    MIN_GROWTH  =  0,
    MIN_HARVEST =  3,
    MAX_GROWTH  =  4;
  final public static float
    NO_HEALTH  = -1,
    MIN_HEALTH =  0,
    MAX_HEALTH =  2;
  
  final public static String STAGE_NAMES[] = {
    "Seedling " ,
    "Sprouting ",
    "Growing "  ,
    "Mature "   ,
    "Ripened "  ,
  };
  
  final static String TREE_STAGE_NAMES[] = {
    "Seedling ",
    "Sapling " ,
    "Mature "  ,
    "Seasoned ",
    "Canopy "  ,
  };
  
  final public static String HEALTH_NAMES[] = {
    "Feeble"   ,
    "Poor"     ,
    "Fair"     ,
    "Good"     ,
    "Excellent",
    "Perfect"  ,
  };
  final public static String DEFAULT_INFO =
    "Flora are the native vegetation of the planet."
  ;
  
  final public static Species
    BASE_SPECIES = new Species(
      Flora.class, "Wild Flora",
      DEFAULT_INFO, null, null,
      Type.FLORA, 0.5f, 0.8f, false,
      10, POLYMER
    ) {},
    BASE_VARIETY[] = { BASE_SPECIES };
  
  final public static float
    NUM_DAYS_MATURE    = 2.5f,
    MATURE_DURATION    = Stage.STANDARD_DAY_LENGTH * NUM_DAYS_MATURE,
    GROW_TIMES_PER_DAY = Stage.STANDARD_DAY_LENGTH / Stage.GROWTH_INTERVAL,
    DIE_CHANCE_PER_DAY = 0.5f / NUM_DAYS_MATURE,
    UPDATE_DIE_CHANCE  = DIE_CHANCE_PER_DAY / GROW_TIMES_PER_DAY,
    
    CEREAL_BONUS = 2.00f,
    HIVE_DIVISOR = 4.00f,
    DRYLAND_MULT = 0.75f,
    WETLAND_MULT = 1.25f,
    WILD_MULD    = 0.50f;
  
  
  private Species species;
  private float growth = 0, quality = 0;
  private boolean blighted;
  
  
  public Flora(Species s) {
    this.species = s;
    this.growth  = NOT_PLANTED;
    this.quality = NO_HEALTH;
  }
  
  
  public Flora(Session s) throws Exception {
    super(s);
    species  = (Species) s.loadObject();
    growth   = s.loadFloat();
    quality  = s.loadFloat();
    blighted = s.loadBool ();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(species );
    s.saveFloat (growth  );
    s.saveFloat (quality );
    s.saveBool  (blighted);
  }
  
  

  
  /**  External population and general query-methods.
    */
  public static void populateFlora(Stage world) {
    final boolean report = initVerbose;
    if (report) I.say("\nPopulating world flora...");
    
    for (Coord c : Visit.grid(0, 0, world.size, world.size, 1)) {
      final Tile t = world.tileAt(c.x, c.y);
      final Flora grows = tryGrowthAt(t, true);
      if (grows == null) continue;
      
      final int stage = grows.growStage();
      grows.refreshIncept(true);
      world.ecology().impingeBiomass(t, stage, Stage.STANDARD_DAY_LENGTH);
    }
  }
  
  
  public static float maxGrowth(Tile t, Species s) {
    if (growthBonus(t, s, null) == -1) return 0;
    if (s.domesticated) return MAX_GROWTH;
    final int
      var  = t.world.terrain().varAt(t),
      mark = var / TerrainGen.MARK_VARS;
    if (mark == 0) return 0;
    if (mark == 1) {
      if (var % TerrainGen.MARK_VARS == 0) return 0.5f;
      return 0;
    }
    float boost = var * 0.5f / TerrainGen.MARK_VARS;
    return t.habitat().moisture() * (MAX_GROWTH + boost) / 10f;
  }
  
  
  public static Flora tryGrowthAt(Tile t, boolean init) {
    if (! hasSpace(t)) return null;
    
    final Habitat soil = t.habitat();
    final Species s = (Species) Rand.pickFrom(soil.floraSpecies);
    
    final Flora grown = new Flora(s);
    grown.seedWith(s, Rand.num());
    grown.enterWorldAt(t.x, t.y, t.world, true);
    
    if (init) {
      float maxGrowth = maxGrowth(t, BASE_SPECIES);
      if (maxGrowth >= 1) maxGrowth -= Rand.num();
      grown.incGrowth(maxGrowth, t.world, false);
    }
    return grown;
  }
  

  public static boolean hasSpace(Tile t) {
    if (t.reserved() || t.pathType() != Tile.PATH_CLEAR) return false;
    if (t.isEntrance() || t.inside().size() > 0) return false;
    return maxGrowth(t, BASE_SPECIES) > 0;
  }
  
  
  public static float growthBonus(Tile t, Species s, Item seed) {
    final Habitat soil = t.habitat();
    if (soil.floraSpecies == null && ! s.domesticated) return -1;
    
    final float moisture = soil.moisture() / 10f;
    float bonus = 0.5f;
    bonus += (s.waterNeed * moisture);
    bonus += (1 - s.waterNeed) * (1 - moisture) / 2;
    
    if (seed != null) bonus *= 1 + (seed.quality * 1f / Item.MAX_QUALITY);
    return Nums.clamp(bonus, 0, MAX_HEALTH);
  }
  
  
  public static Flora foundAt(Target t) {
    if (! (t instanceof Tile)) return null;
    final Element e = ((Tile) t).above();
    return (e instanceof Flora) ? (Flora) e : null;
  }
  
  
  private float dailyGrowthEstimate(Tile tile, boolean report) {
    final Stage world = tile.world;
    float
      increment = 1f / NUM_DAYS_MATURE,
      health    = health(),
      growBonus = growthBonus(tile, species, null),
      yieldMult = species.growRate,
      pollution = 0 - world.ecology().ambience.valueAt(tile);
    
    if (report) I.reportVars("\nEstimating crop growth", "  ",
      "Increment" , increment,
      "Health"    , health   ,
      "Grow bonus", growBonus,
      "Yield mult", yieldMult,
      "Pollution" , pollution,
      "Grow stage", growth,
      "Blighted?" , blighted 
    );
    
    increment *= growBonus * yieldMult * (1 + health) / 2;
    if (pollution > 0) increment *= (2 - pollution) / 2;
    return increment;
  }
  
  
  public float dailyYieldEstimate(Tile tile) {
    final float fullAmount = 1;
    return dailyGrowthEstimate(tile, false) * fullAmount;
  }
  
  
  
  /**  General life-cycle and active growth-updates.
    */
  public boolean canPlace() {
    if (inWorld()) return true;
    return origin() != null && maxGrowth(origin(), species) > 0;
  }
  
  
  public boolean enterWorldAt(int x, int y, Stage world, boolean intact) {
    if (! super.enterWorldAt(x, y, world, intact)) return false;
    world.presences.togglePresence(this, true);
    return true;
  }
  
  
  public void exitWorld() {
    world.presences.togglePresence(this, false);
    super.exitWorld();
  }
  
  
  public void onGrowth(Tile tile) {
    final boolean report = updatesVerbose && I.talkAbout == this;
    final float
      dailyGrowth = dailyGrowthEstimate(tile, report),
      health      = health(),
      increment   = dailyGrowth * MAX_GROWTH / GROW_TIMES_PER_DAY;
    
    if (Rand.num() < increment * (1 - health)) {
      blighted = true;
    }
    else if (Rand.num() < increment * health) {
      blighted = false;
    }
    
    world.ecology().impingeBiomass(
      origin(), growStage() / 2f, Stage.GROWTH_INTERVAL
    );
    
    incGrowth(increment, tile.world, true);
  }
  
  
  public void incGrowth(float inc, Stage world, boolean natural) {
    final float maxGrowth = maxGrowth(origin(), species);
    
    final int oldStage = Nums.clamp((int) growth, MAX_GROWTH);
    growth = Nums.clamp(growth + inc, MIN_GROWTH, maxGrowth);
    final int newStage = Nums.clamp((int) growth, MAX_GROWTH);
    
    if (natural && oldStage >= MIN_GROWTH && ! species.domesticated) {
      float dieChance = UPDATE_DIE_CHANCE * growth / maxGrowth;
      if (Rand.num() < dieChance) { setAsDestroyed(false); return; }
    }
    if (oldStage != newStage) {
      origin().refreshAdjacent();
      updateSprite();
    }
  }
  

  
  /**  Agriculture-related methods-
    */
  public void seedWith(Species s, float quality) {
    //
    //  TODO:  This is a bit of a kluge (has to do with forestry not being
    //  able to efficiently choose a species-range until it arrives at a given
    //  tile.)  Remove once you have a better solution.
    if (s == BASE_SPECIES) {
      final Species var[] = origin().habitat().floraSpecies;
      s = (Species) Rand.pickFrom(var);
    }
    this.species = s;
    this.quality = Nums.clamp(quality, 0, MAX_HEALTH);
    this.growth  = MIN_GROWTH;
    updateSprite();
  }
  
  
  public Species species() {
    return species;
  }
  
  
  public void disinfest() {
    blighted = false;
  }
  
  
  public boolean needsTending() {
    return growth == NOT_PLANTED || blighted() || ripe();
  }
  
  
  public boolean blighted() {
    return blighted;
  }
  
  
  public boolean ripe() {
    return growth >= MIN_HARVEST;
  }
  
  
  public int growStage() {
    return (int) growth;
  }
  
  
  public float health() {
    if (quality < 0) return 0.5f;
    return quality / ((blighted ? 2f : 1f) * MAX_HEALTH);
  }
  
  
  public float height() {
    return growth * 1f / MAX_GROWTH;
  }
  
  
  public int pathType() {
    if (species.domesticated   ) return Tile.PATH_HINDERS;
    if (growth < MIN_GROWTH + 1) return Tile.PATH_HINDERS;
    return Tile.PATH_BLOCKS;
  }
  
  
  public Item[] materials() {
    return BASE_SPECIES.nutrients(growStage());
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected void updateSprite() {
    final Sprite oldSprite = sprite();
    final int stage = growStage();
    final ModelAsset model = modelForStage(stage);
    if (oldSprite != null && model == oldSprite.model()) return;
    
    attachSprite(model.makeSprite());
    if (oldSprite != null) world.ephemera.addGhost(this, 1, oldSprite, 2.0f, 1);
  }
  
  
  protected ModelAsset modelForStage(int newStage) {
    newStage = Nums.clamp(growStage(), species.modelSequence.length);
    return species.modelSequence[newStage];
  }
  
  
  public String fullName() {
    final String name = species.domesticated ? species.name : "Flora";
    return TREE_STAGE_NAMES[growStage()]+name;
  }
  
  
  public Composite portrait(HUD UI) {
    //  TODO:  FILL THIS IN!
    return null;
  }
  
  
  public String helpInfo() {
    //return "Habitat "+origin().habitat();
    return DEFAULT_INFO;
  }
}















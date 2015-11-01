/**  
 *  Written by Morgan Allen.
 *  I intend to slap on some kind of open-source license here in a while, but
 *  for now, feel free to poke around for non-commercial purposes.
 */
package stratos.game.wild;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import stratos.game.wild.Species.Type;
import static stratos.game.economic.Economy.*;



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
    GROWTH_PER_UPDATE  = MATURE_DURATION / (MAX_GROWTH * GROW_TIMES_PER_DAY),
    
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
      
      final float growth = growthBonus(t, grows.species, null);
      float stage = 0;
      for (int n = MAX_GROWTH; n-- > 0;) {
        final float roll = Rand.index(8) / 8f;
        if (roll < growth) stage += roll;
      }
      stage = Nums.clamp(stage * 2, 0.5f, MAX_GROWTH - 0.5f);
      
      if (report) {
        I.say("  Growth chance at "+t+" was: "+growth);
        I.say("  Initialising flora, stage: "+stage);
      }
      
      grows.growth = stage;
      grows.updateSprite();
      grows.refreshIncept(true);
      world.ecology().impingeBiomass(t, stage, Stage.STANDARD_DAY_LENGTH);
    }
  }
  
  
  public static Flora tryGrowthAt(Tile t, boolean init) {
    if (! hasSpace(t)) return null;
    final Habitat soil = t.habitat();
    final Species s = (Species) Rand.pickFrom(soil.floraSpecies);
    
    float growChance = growthBonus(t, s, null);
    if (init) growChance /= 2;
    else growChance *= Stage.GROWTH_INTERVAL / MATURE_DURATION;
    if (Rand.num() >= growChance) return null;
    
    final Flora grown = new Flora(s);
    grown.seedWith(s, Rand.num());
    grown.enterWorldAt(t.x, t.y, t.world, true);
    return grown;
  }
  
  
  public static boolean hasSpace(Tile t) {
    if (t.reserved() || t.pathType() != Tile.PATH_CLEAR) return false;
    if (t.isEntrance() || t.inside().size() > 0) return false;
    if (growthBonus(t, BASE_SPECIES, null) == -1) return false;
    return numNearOkay(t, true);
  }
  
  
  private static boolean numNearOkay(Tile t, boolean checkNeighbours) {
    int numNear = checkNeighbours ? 0 : 1;
    for (Tile n : t.allAdjacent(null)) {
      if (n == null || n.blocked()) numNear++;
      if (n != null && ! n.buildable()) return false;
      if (n != null && checkNeighbours && n.above() instanceof Flora) {
        if (! numNearOkay(n, false)) return false;
      }
    }
    return numNear < 3;
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
    return origin() != null && hasSpace(origin());
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
    
    incGrowth(increment, tile.world);
  }
  
  
  public void incGrowth(float inc, Stage world) {
    final int oldStage = Nums.clamp((int) growth, MAX_GROWTH);
    growth = Nums.clamp(growth + inc, MIN_GROWTH, MAX_GROWTH);
    final int newStage = Nums.clamp((int) growth, MAX_GROWTH);
    
    final float
      moisture = origin().habitat().moisture() / 10f,
      dieChance = (1 - moisture) * inc;
    final int
      minGrowth = (int) ((moisture * moisture * MAX_GROWTH) + 1),
      maxGrowth = MAX_GROWTH + 1;
    
    if (
      (growth <= 0 || growth >= maxGrowth) ||
      (growth > minGrowth && Rand.num() < dieChance)
    ) {
      setAsDestroyed(false);
      return;
    }
    
    if (oldStage != newStage) updateSprite();
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
  
  
  public Item[] materials() {
    return BASE_SPECIES.nutrients(growStage());
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected void updateSprite() {
    final Sprite oldSprite = sprite();
    final ModelAsset model = modelForStage(growStage());
    if (oldSprite != null && model == oldSprite.model()) return;
    
    attachSprite(model.makeSprite());
    if (oldSprite != null) world.ephemera.addGhost(this, 1, oldSprite, 2.0f);
  }
  
  
  protected ModelAsset modelForStage(int newStage) {
    newStage = Nums.clamp(growStage(), species.modelSequence.length);
    return species.modelSequence[newStage];
  }
  
  
  public String fullName() {
    return TREE_STAGE_NAMES[growStage()]+"Flora";
  }
  
  
  public Composite portrait(BaseUI UI) {
    //  TODO:  FILL THIS IN!
    return null;
  }
  
  
  public String helpInfo() {
    //return "Habitat "+origin().habitat();
    return DEFAULT_INFO;
  }
}















/**  
 *  Written by Morgan Allen.
 *  I intend to slap on some kind of open-source license here in a while, but
 *  for now, feel free to poke around for non-commercial purposes.
 */
package stratos.game.wild;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.Composite;
import stratos.user.BaseUI;
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
  
  final public static Species
    ONI_RICE    = new Species(
      Flora.class, "Oni Rice"   , Type.FLORA, 2, CARBS
    ) {},
    DURWHEAT    = new Species(
      Flora.class, "Durwheat"   , Type.FLORA, 2, CARBS
    ) {},
    SABLE_OAT   = new Species(
      Flora.class, "Sable Oat"  , Type.FLORA, 1, CARBS
    ) {},
    
    TUBER_LILY  = new Species(
      Flora.class, "Tuber Lily" , Type.FLORA, 2, GREENS
    ) {},
    BROADFRUITS = new Species(
      Flora.class, "Broadfruits", Type.FLORA, 2, GREENS
    ) {},
    HIBERNUTS   = new Species(
      Flora.class, "Hibernuts"  , Type.FLORA, 1, GREENS
    ) {},
    
    HIVE_GRUBS  = new Species(
      Flora.class, "Hive Grubs" , Type.FLORA, 1, PROTEIN
    ) {},
    BLUE_VALVES = new Species(
      Flora.class, "Blue Valves", Type.FLORA, 1, PROTEIN
    ) {},
    CLAN_BORE   = new Species(
      Flora.class, "Clan Bore"  , Type.FLORA, 1, PROTEIN
    ) {},
    
    GORG_APHID  = new Species(
      Flora.class, "Gorg Aphid" , Type.FLORA, 1, DRY_SPYCE
    ) {},
    
    PIONEERS    = new Species(
      Flora.class, "Pioneers"   , Type.FLORA
    ) {},
    WILD_FLORA  = new Species(
      Flora.class, "Wild Flora" , Type.FLORA, 2, POLYMER
    ) {};
  
  final static String STAGE_NAMES[] = {
    "Seedling",
    "Sapling" ,
    "Mature"  ,
    "Seasoned"
  };
  
  final public static int
    MAX_GROWTH = 4;
  
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
  
  
  final Habitat habitat;
  final int varID;  //  TODO:  Use a Species here, maybe?
  private float growth = 0;
  
  
  private Flora(Habitat h) {
    this.habitat = h;
    this.varID = Rand.index(4);
  }
  
  
  public Flora(Session s) throws Exception {
    super(s);
    habitat = Habitat.ALL_HABITATS[s.loadInt()];
    varID = s.loadInt();
    growth = s.loadFloat();
  }
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(habitat.ID);
    s.saveInt(varID);
    s.saveFloat(growth);
  }
  
  
  
  /**
   * Attempts to seed or grow new flora at the given coordinates.
   */
  public static void populateFlora(Stage world) {
    final boolean report = initVerbose;
    if (report) I.say("\nPopulating world flora...");
    
    for (Coord c : Visit.grid(0, 0, world.size, world.size, 1)) {
      final Tile t = world.tileAt(c.x, c.y);
      if (t.blocked()) continue;
      final float growChance = growChance(t);
      if (report) I.say("  Grow chance at "+t+" is "+growChance);
      
      if (growChance == -1 || Rand.num() > growChance) continue;
      if (! canGrowAt(t)) continue;
      
      final Flora f = new Flora(t.habitat());
      f.enterWorldAt(t.x, t.y, world, true);
      float stage = 0.5f;
      for (int n = MAX_GROWTH; n-- > 0;) {
        if (Rand.num() < growChance * 4) stage++;
      }
      
      stage = Nums.clamp(stage, 0, MAX_GROWTH - 0.5f);
      f.incGrowth(stage, world, true);
      f.setAsEstablished(true);
      world.ecology().impingeBiomass(t, f.growth, Stage.STANDARD_DAY_LENGTH);
      if (report) I.say("  Initialising flora, stage: "+stage);
    }
  }
  
  
  public static float growChance(Tile t) {
    if (t.habitat().floraModels == null) return -1;
    return (t.habitat().moisture() / 10) / 4;
  }
  
  
  public static boolean canGrowAt(Tile t) {
    if (t.reserved() || t.pathType() != Tile.PATH_CLEAR) return false;
    if (t.isEntrance() || t.inside().size() > 0) return false;
    if (growChance(t) == -1) return false;
    return SiteUtils.pathingOkayAround(t, Owner.TIER_TERRAIN);
  }
  
  
  public static Flora tryGrowthAt(Tile t, boolean certain) {
    //  TODO:  UNIFY THIS WITH GROWTH-METHODS FOR CROPS
    
    final float growChance = growChance(t);
    if (growChance == -1) return null;
    
    if (updatesVerbose) I.say("Grow chance: "+growChance);
    
    if (t.above() instanceof Flora) {
      final Flora f = (Flora) t.above();
      if (Rand.num() < (growChance * 4 * GROWTH_PER_UPDATE)) {
        f.incGrowth(0.5f + Rand.num(), t.world, false);
        t.world.ecology().impingeBiomass(t, f.growth, Stage.GROWTH_INTERVAL);
      }
      return f;
    }
    
    if (! canGrowAt(t)) return null;
    if (! certain) {
      if (Rand.num() > growChance) return null;
      if (Rand.num() > GROWTH_PER_UPDATE) return null;
    }
    return newGrowthAt(t);
  }
  
  
  public static Flora newGrowthAt(Tile t) {
    if (updatesVerbose) I.say("Seeding new tree at: "+t);
    final Flora f = new Flora(t.habitat());
    f.enterWorldAt(t.x, t.y, t.world, true);
    f.incGrowth((0.5f + Rand.num()) / 2, t.world, false);
    t.world.ecology().impingeBiomass(t, f.growth, Stage.GROWTH_INTERVAL);
    return f;
  }
  
  
  public void incGrowth(float inc, Stage world, boolean init) {
    final int oldStage = Nums.clamp((int) growth, MAX_GROWTH);
    growth += inc;
    final int newStage = Nums.clamp((int) growth, MAX_GROWTH);
    
    if (! init) {
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
        setAsDestroyed();
        return;
      }
    }
    
    if (oldStage != newStage || sprite() == null) {
      final Sprite oldSprite = sprite();
      final CutoutModel model = habitat.floraModels[varID][newStage];
      attachSprite(model.makeSprite());
      setAsEstablished(false);
      if (oldSprite != null) world.ephemera.addGhost(this, 1, oldSprite, 2.0f);
    }
  }
  
  
  public int growStage() {
    return Nums.clamp((int) growth, MAX_GROWTH);
  }
  
  
  public Item[] materials() {
    return WILD_FLORA.nutrients(growStage());
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
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return STAGE_NAMES[growStage()]+" Flora";
  }
  
  
  public Composite portrait(BaseUI UI) {
    //  TODO:  FILL THIS IN!
    return null;
  }
  
  
  public String helpInfo() {
    return "Flora are the native vegetation of the planet.";
  }
}















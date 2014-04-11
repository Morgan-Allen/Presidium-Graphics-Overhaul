/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.base ;
import stratos.game.common.* ;
import stratos.game.planet.* ;
import stratos.game.building.*;
import stratos.graphics.common.* ;
import stratos.graphics.cutout.* ;
import stratos.util.* ;


//
//  TODO:  Try and simplify this.  Replace with a fixture of some type,
//  ideally?

public class Crop implements Session.Saveable, Target {
  
  
  final static int
    NOT_PLANTED =  0,
    MIN_GROWTH  =  1,
    MIN_HARVEST =  3,
    MAX_GROWTH  =  4;
  final static float
    NO_HEALTH  = -1,
    MIN_HEALTH =  0,
    MAX_HEALTH =  2;
  
  final static String STAGE_NAMES[] = {
    "Unplanted ",
    "Sprouting ",
    "Growing ",
    "Mature ",
    "Ripened "
  } ;
  final static String HEALTH_NAMES[] = {
    "Feeble",
    "Poor",
    "Fair",
    "Good",
    "Excellent",
    "Perfect"
  } ;
  
  
  final Plantation parent;
  final Tile tile;
  
  private Species species;
  private float growStage, quality;
  private boolean blighted;
  
  
  protected Crop(Plantation parent, Species species, Tile t) {
    this.parent = parent ;
    this.species = species ;
    this.tile = t ;
    growStage = NOT_PLANTED ;
    quality = 1.0f ;
  }
  
  
  public Crop(Session s) throws Exception {
    s.cacheInstance(this) ;
    parent = (Plantation) s.loadObject() ;
    tile = (Tile) s.loadTarget() ;
    species = (Species) s.loadObject() ;
    growStage = s.loadFloat() ;
    quality = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(parent) ;
    s.saveTarget(tile) ;
    s.saveObject(species) ;
    s.saveFloat(growStage) ;
    s.saveFloat(quality) ;
  }
  
  
  
  /**  Implementing the Target interface-
    */
  private Object flagged ;
  public boolean inWorld() { return parent.inWorld() ; }
  public boolean destroyed() { return parent.destroyed() ; }
  public World world() { return parent.world() ; }
  public Vec3D position(Vec3D v) { return tile.position(v) ; }
  public float height() { return tile.height() ; }
  public float radius() { return tile.radius() ; }
  public void flagWith(Object f) { this.flagged = f ; }
  public Object flaggedWith() { return flagged ; }
  
  
  
  /**  Growth calculations-
    */
  static boolean isHive(Species s) {
    return s == Species.HIVE_GRUBS || s == Species.BLUE_VALVES ;
  }
  
  
  static boolean isCereal(Species s) {
    return s == Species.DURWHEAT || s == Species.ONI_RICE ;
  }
  
  
  static boolean isDryland(Species s) {
    return s == Species.DURWHEAT || s == Species.BROADFRUITS;
  }
  
  
  static Crop cropAt(Tile t) {
    //  TODO:  try and use tile-ownership, directly, as a fixture.
    if (t.owner() instanceof Plantation) {
      for (Crop c : ((Plantation) t.owner()).planted) {
        if (c != null && c.tile == t) return c;
      }
    }
    return null;
  }
  
  
  protected static Service yieldType(Species species) {
    final Service type;
    if (isHive(species)) {
      type = Economy.PROTEIN;
    }
    else if (isCereal(species)) {
      type = Economy.CARBS;
    }
    else type = Economy.GREENS;
    return type;
  }
  
  
  static float habitatBonus(Tile t, Species s, BotanicalStation parent) {
    final Upgrade PU;
    float bonus = 0.0f;
    
    //  First, apply appropriate modifier for microclimate-
    final float moisture = t.habitat().moisture() / 10f;
    if (isDryland(s)) {
      bonus = Plantation.DRYLAND_MULT * (1 + moisture) / 2f;
    }
    else bonus = moisture * Plantation.WETLAND_MULT;
    
    //  Then, we determine bonus based on crop type-
    if (isHive(s)) {
      bonus += t.world.ecology().biomassRating(t);
      PU = BotanicalStation.INSECTRY_LAB;
    }
    else if (isCereal(s)) {
      bonus *= Plantation.CEREAL_BONUS;
      PU = BotanicalStation.CEREAL_LAB;
    }
    else PU = BotanicalStation.BROADLEAF_LAB;
    
    //  And, if allowed, the modifier for structure upgrades-
    if (parent != null) {
      final int UB = parent.structure.upgradeBonus(PU);
      bonus *= 1 + (UB * Plantation.UPGRADE_GROW_BONUS);
    }
    return Visit.clamp(bonus, 0, Plantation.MAX_HEALTH_BONUS);
  }
  
  
  protected void seedWith(Species s, float quality) {
    this.species = s;
    this.quality = Visit.clamp(quality, 0, Plantation.MAX_HEALTH_BONUS);
    this.growStage = MIN_GROWTH;
  }
  
  
  //  TODO:  Treat each crop individually as a fixture or element.  Use road
  //  network to get water.
  //final float fertility = tile.habitat().moisture() / 10f;
  
  protected void onGrowth(Tile t) {
    if (growStage == NOT_PLANTED) return ;
    final World world = parent.world() ;
    final float pollution = Visit.clamp(
      tile.world.ecology().ambience.valueAt(tile), 0, 1
    );
    
    float increment = 1f;
    increment -= (pollution * Plantation.POLLUTE_GROW_PENALTY);
    if (blighted) increment -= Plantation.INFEST_GROW_PENALTY;
    if (increment > 0) {
      increment *= Planet.dayValue(world) * 2;
      increment *= quality * habitatBonus(tile, species, null);
    }
    increment *= Rand.num() * 2 * Plantation.GROW_INCREMENT * MAX_GROWTH;
    
    growStage = Visit.clamp(growStage + increment, MIN_GROWTH, MAX_GROWTH);
    checkBlight(pollution);
  }
  
  
  private void checkBlight(float pollution) {
    if (growStage <= MIN_GROWTH) { blighted = false; return; }
    float blightChance = (pollution + MAX_HEALTH - quality) / MAX_HEALTH;
    
    //  The chance of contracting disease increases if near infected plants of
    //  the same species, and decreases with access to a hive.
    final Tile t = Spacing.pickRandomTile(this, 4, tile.world);
    final Crop c = cropAt(t);
    if (c != null) {
      if (c.species == this.species && c.blighted) blightChance += 1;
      else if (isHive(c.species) && ! isHive(this.species)) blightChance -= 1;
    }
    
    //  Better-established plants can fight off infection more easily, and if
    //  infection-chance is low, spontaneous recovery can occur.
    blightChance *= 2f / (2 + (growStage / MAX_GROWTH));
    float recoverChance = (1f - blightChance) * Plantation.GROW_INCREMENT / 2;
    blightChance *= Plantation.GROW_INCREMENT;
    if (blighted && Rand.num() < recoverChance) blighted = false;
    if (Rand.num() < blightChance && ! blighted) blighted = true;
    if (growStage <= MIN_GROWTH) blighted = false;
  }
  
  
  protected void disinfest() {
    blighted = false;
  }
  
  
  protected Item yieldCrop() {
    final Service type = yieldType(species);
    final float amount = growStage / MAX_GROWTH;
    growStage = NOT_PLANTED;
    quality = NO_HEALTH;
    blighted = false;
    return Item.withAmount(type, amount);
  }
  
  
  boolean needsTending() {
    return
      blighted ||
      growStage == NOT_PLANTED ||
      growStage >= MIN_HARVEST ;
  }
  
  
  boolean blighted() {
    return blighted;
  }
  
  
  int growStage() {
    return (int) growStage;
  }
  
  
  Species species() {
    return species;
  }
  
  
  
  /**  Rendering and interface-
    */
  //  TODO:  Pass a Description object here instead?
  public String toString() {
    final int stage = (int) Visit.clamp(growStage, 0, MAX_GROWTH) ;
    final String HD ;
    if (blighted) HD = " (Infested)" ;
    else {
      final int HL = Visit.clamp((int) quality, 5) ;
      HD = " ("+HEALTH_NAMES[HL]+" health)" ;
    }
    return STAGE_NAMES[stage]+""+species.name+HD ;
  }
}








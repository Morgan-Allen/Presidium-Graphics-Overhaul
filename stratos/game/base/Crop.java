/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.base ;
import stratos.game.common.* ;
import stratos.game.planet.* ;
import stratos.graphics.common.* ;
import stratos.graphics.cutout.* ;
import stratos.util.* ;


//
//  TODO:  Try and simplify this.  Replace with a fixture of some type,
//  ideally?

public class Crop implements Session.Saveable, Target {
  
  
  final static int
    AS_SEED     = -1,
    NOT_PLANTED =  0,
    MIN_GROWTH  =  1,
    MIN_HARVEST =  3,
    MAX_GROWTH  =  4 ;
  final static String STAGE_NAMES[] = {
    "Seed Stock ",
    "Unplanted ",
    "Sprouting ",
    "Growing ",
    "Mature ",
    "Ripened "
  } ;
  final static String HEALTH_NAMES[] = {
    "Weak",
    "Poor",
    "Fair",
    "Good",
    "Excellent",
    "Perfect"
  } ;
  
  
  final Plantation parent ;
  final Tile tile ;
  
  Species species ;
  float growStage, health ;
  boolean infested ;
  
  
  protected Crop(Plantation parent, Species species, Tile t) {
    this.parent = parent ;
    this.species = species ;
    this.tile = t ;
    growStage = NOT_PLANTED ;
    health = 1.0f ;
  }
  
  
  public Crop(Session s) throws Exception {
    s.cacheInstance(this) ;
    parent = (Plantation) s.loadObject() ;
    tile = (Tile) s.loadTarget() ;
    species = (Species) s.loadObject() ;
    growStage = s.loadFloat() ;
    health = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(parent) ;
    s.saveTarget(tile) ;
    s.saveObject(species) ;
    s.saveFloat(growStage) ;
    s.saveFloat(health) ;
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
  
  
  
  /**  Updates and queries-
    */
  void doGrowth(float fertility, float amount) {
    if (growStage == NOT_PLANTED) return ;
    final World world = parent.world() ;
    //
    //  Increment growth based on terrain fertility and daylight values.
    float growInc = fertility ;
    growInc += parent.belongs.growBonus(tile, species, true) ;
    growInc *= Rand.num() * Planet.dayValue(world) ;
    growInc = Visit.clamp(growInc, 0.2f, 1.2f) ;
    if (infested) growInc /= 5 ;
    growInc *= amount ;
    this.growStage = Visit.clamp(growStage + growInc, MIN_GROWTH, MAX_GROWTH) ;
    //
    //  Increase the chance of becoming infested based on pollution and
    //  proximity to other diseased plants of the same species, but reduce it
    //  based on intrinsic health rating and insect services.
    //final int hive = Plantation.VAR_HIVE_CELLS ;
    final float pollution = 0 - world.ecology().ambience.valueAt(tile) ;
    float infestChance = (((5 - health) / 10) + pollution) / 2f ;
    //
    //  (Hive cells can themselves become infested, but only from other hives.)
    for (Tile t : tile.allAdjacent(null)) {
      if (t == null || ! (t.owner() instanceof Plantation)) continue ;
      final Crop near = ((Plantation) t.owner()).plantedAt(t) ;
      if (near == null) continue ;
      if (near.isHive() && ! this.isHive()) {
        infestChance -= near.growStage / 10f ;
      }
      if (near.species == this.species) {
        infestChance += 0.1f / (near.infested ? 1 : 2) ;
      }
      else if (! this.isHive()) {
        infestChance += 0.1f / (near.infested ? 2 : 4) ;
      }
    }
    if (Rand.num() < (infestChance * amount)) this.infested = true ;
    ///I.sayAbout(parent, "  Grown: "+this) ;
  }
  
  
  boolean isHive() {
    return species == Species.HIVE_GRUBS || species == Species.BLUE_VALVES ;
  }
  
  
  boolean needsTending() {
    return
      infested ||
      growStage == NOT_PLANTED ||
      growStage >= MIN_HARVEST ;
  }
  
  
  
  /**  Rendering and interface-
    */
  public String toString() {
    if (growStage == AS_SEED) return species.name ;
    int stage = (int) Visit.clamp(growStage + 1, 0, MIN_HARVEST + 1) ;
    final String HD ;
    if (infested) {
      HD = " (Infested)" ;
    }
    else {
      final int HL = Visit.clamp((int) health, 5) ;
      HD = " ("+HEALTH_NAMES[HL]+" health)" ;
    }
    return STAGE_NAMES[stage]+""+species.name+HD ;
  }
}








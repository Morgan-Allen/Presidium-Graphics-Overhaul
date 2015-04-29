/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.maps;
import stratos.game.base.BaseDemands;
import stratos.game.common.*;
import stratos.game.wild.*;
import stratos.util.*;



public class Ecology {
  
  
  /**  Data fields, construction and save/load methods-
    */
  final Stage world;
  final int SR, SS;
  
  final RandomScan growthMap;
  final public Ambience ambience;
  
  
  
  public Ecology(final Stage world) {
    this.world = world;
    SR = Stage.PATCH_RESOLUTION;
    SS = world.size / SR;
    growthMap = new RandomScan(world.size) {
      protected void scanAt(int x, int y) { growthAt(world.tileAt(x, y)); }
    };
    ambience = new Ambience(world);
  }
  
  
  public void loadState(Session s) throws Exception {
    growthMap.loadState(s);
    ambience.loadState(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    growthMap.saveState(s);
    ambience.saveState(s);
  }
  
  
  
  /**  Continuous updates-
    */
  public void updateEcology() {
    final int size = world.size;
    final float time = world.currentTime();
    
    float growIndex = (time % Stage.GROWTH_INTERVAL);
    growIndex *= size * size * 1f / Stage.GROWTH_INTERVAL;
    growthMap.scanThroughTo((int) growIndex);
  }
  
  
  private void growthAt(Tile t) {
    Flora.tryGrowthAt(t, false);
    final Element owner = t.onTop();
    if (owner != null) owner.onGrowth(t);
    ambience.updateAt(t);
  }
  
  
  
  /**  Querying sample values-
    */
  private BlurMap biomass = null;
  private BaseDemands abundances;
  
  private boolean checkMapsInit() {
    if (biomass != null) return true;
    abundances = Base.wildlife(world).demands;
    biomass    = abundances.mapForSupply("Biomass");
    return true;
  }
  
  
  public void impingeBiomass(Tile t, float amount, float duration) {
    checkMapsInit();
    abundances.impingeSupply(biomass, amount, duration, t);
  }
  
  
  public float biomassRating(Tile t) {
    checkMapsInit();
    float sample = abundances.supplyAround(t, biomass, Stage.ZONE_SIZE) * 4;
    return sample / (Stage.ZONE_AREA * 4);
  }
  
  
  public float globalBiomass() {
    checkMapsInit();
    return abundances.globalSupply(biomass) * 4;
  }
  
  
  public float forestRating(Tile t) {
    checkMapsInit();
    float fertility = world.terrain().fertilitySample(t);
    float foresting = biomassRating(t) * 2f / Flora.MAX_GROWTH;
    if (fertility <= 0) return 1;
    return Nums.clamp(foresting / fertility, 0, 2);
  }
  
  
  
  /**  Terraforming methods-
    */
  public void pushClimate(Habitat desired, float strength) {
    //  TODO:  This is the next thing to implement.
  }
}













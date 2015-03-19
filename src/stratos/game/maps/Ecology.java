/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.maps;
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
  
  private Base wildlife = null;
  private BaseDemands abundances;
  private BlurMap biomass;
  
  
  public Ecology(final Stage world) {
    this.world = world;
    SR = Stage.PATCH_RESOLUTION;
    SS = world.size / SR;
    growthMap = new RandomScan(world.size) {
      protected void scanAt(int x, int y) { growthAt(world.tileAt(x, y)); }
    };
    ambience = new Ambience(world);
    extractMaps();
  }
  
  
  public void loadState(Session s) throws Exception {
    growthMap.loadState(s);
    ambience.loadState(s);
    //
    //  We ensure that the wildlife base is loaded first, to ensure that the
    //  associated demand maps can be extracted intact (see below.)
    wildlife = (Base) s.loadObject();
    abundances = null;
    extractMaps();
  }
  
  
  public void saveState(Session s) throws Exception {
    growthMap.saveState(s);
    ambience.saveState(s);
    s.saveObject(wildlife);
  }
  
  
  //  TODO:  You can probably dispense with these soon enough...
  
  private void extractMaps() {
    if (abundances != null) return;
    //
    //  I'm moving these init methods here to avoid potential complications
    //  during the save/load process.
    if (wildlife == null) wildlife = Base.wildlife(world);
    abundances = wildlife.demands;
    biomass   = abundances.mapForSupply("Biomass");
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
    Flora.tryGrowthAt(t);
    final Element owner = t.onTop();
    if (owner != null) owner.onGrowth(t);
    ambience.updateAt(t);
  }
  
  
  public void impingeBiomass(Tile t, float amount, float duration) {
    abundances.impingeSupply(biomass, amount, duration, t);
  }
  
  
  
  /**  Querying sample values-
    */
  public float biomassRating(Tile t) {
    return abundances.supplyAround(t, biomass, Stage.SECTOR_SIZE) * 4;
  }
  
  
  public float globalBiomass() {
    return abundances.globalSupply(biomass) * 4;
  }
  
  
  public float forestRating(Tile t) {
    float fertility = world.terrain().fertilitySample(t);
    float foresting = abundances.supplyAround(t, biomass, Stage.SECTOR_SIZE);
    if (fertility <= 0) return 1;
    return Nums.clamp(foresting / fertility, 0, 2);
  }
  
  
  
  /**  Terraforming methods-
    */
  public void pushClimate(Habitat desired, float strength) {
    //  TODO:  This is the next thing to implement.
  }
}













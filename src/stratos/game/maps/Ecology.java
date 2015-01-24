/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.maps;
import stratos.game.common.*;
import stratos.game.wild.*;
import stratos.util.*;



//  TODO:  Use the ratePlacing() methods for individual nests, instead of those
//  extraneous placement methods.

public class Ecology {
  
  
  /**  Data fields, construction and save/load methods-
    */
  final Stage world;
  final int SR, SS;
  final RandomScan growthMap;
  
  final public Ambience ambience;
  final private BaseDemands abundances;
  final private BlurMap
    biomass,
    preyMap,
    hunterMap,
    speciesMaps[];
  
  
  public Ecology(final Stage world) {
    this.world = world;
    SR = Stage.PATCH_RESOLUTION;
    SS = world.size / SR;
    growthMap = new RandomScan(world.size) {
      protected void scanAt(int x, int y) { growthAt(world.tileAt(x, y)); }
    };
    ambience   = new Ambience(world);
    abundances = Base.wildlife(world).demands;
    
    biomass   = abundances.mapForSupply("Biomass");
    preyMap   = abundances.mapForSupply("Prey"   );
    hunterMap = abundances.mapForSupply("Hunters");
    
    final int numS = Species.ANIMAL_SPECIES.length;
    speciesMaps = new BlurMap[numS];
    for (int i = 0; i < numS; i++) {
      speciesMaps[i] = abundances.mapForSupply(Species.ANIMAL_SPECIES[i]);
    }
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
    abundances.updateAllMaps(1);
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
  
  
  public void impingeAbundance(Fauna f, float duration) {
    final Tile t = f.origin();
    final Species s = f.species;
    final float inc = f.health.maxHealth();
    
    if (s.type == Species.Type.BROWSER ) {
      abundances.impingeSupply(preyMap, inc, duration, t);
    }
    if (s.type == Species.Type.PREDATOR) {
      abundances.impingeSupply(hunterMap, inc, duration, t);
    }
    abundances.impingeSupply(speciesMaps[s.ID], inc, duration, t);
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













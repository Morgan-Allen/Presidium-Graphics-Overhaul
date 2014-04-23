

package stratos.game.maps ;
import stratos.game.common.*;
import stratos.game.wild.Fauna;
import stratos.util.*;



//  TODO:  You might be able to get rid of this class entirely, or merge with
//  the WorldTerrain class instead.


public class Ecology {
  
  
  /**  Data fields, construction and save/load methods-
    */
  final static float
    UPDATE_INC = 0.01f ;
  
  private static boolean verbose = false ;
  
  final World world ;
  final int SR, SS ;
  final RandomScan growthMap ;
  final public Ambience ambience ;
  
  //  TODO:  Consider getting rid of this class entirely.
  final public FadingMap
    biomass,
    preyMap, hunterMap,
    abundances[] ;
  final Batch <FadingMap> allMaps = new Batch <FadingMap> () ;
  
  
  
  public Ecology(final World world) {
    this.world = world ;
    SR = World.PATCH_RESOLUTION ;
    SS = world.size / SR ;
    growthMap = new RandomScan(world.size) {
      protected void scanAt(int x, int y) { growthAt(world.tileAt(x, y)) ; }
    } ;
    ambience = new Ambience(world) ;
    
    allMaps.add(biomass    = new FadingMap(world, SS, -1)) ;
    allMaps.add(preyMap    = new FadingMap(world, SS, -1)) ;
    allMaps.add(hunterMap  = new FadingMap(world, SS, -1)) ;
    
    abundances = new FadingMap[Species.ANIMAL_SPECIES.length] ;
    for (int i = 0 ; i < Species.ANIMAL_SPECIES.length ; i++) {
      abundances[i] = new FadingMap(world, SS, -1) ;
      allMaps.add(abundances[i]) ;
    }
  }
  
  
  public void loadState(Session s) throws Exception {
    //I.say("Loading ecology state...") ;
    growthMap.loadState(s) ;
    ambience.loadState(s) ;
    for (FadingMap map : allMaps) map.loadState(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    growthMap.saveState(s) ;
    ambience.saveState(s) ;
    for (FadingMap map : allMaps) map.saveState(s) ;
  }
  
  
  
  /**  Continuous updates-
    */
  public void updateEcology() {
    final int size = world.size ;
    final float time = world.currentTime() ;
    
    float growIndex = (time % World.GROWTH_INTERVAL) ;
    growIndex *= size * size * 1f / World.GROWTH_INTERVAL ;
    growthMap.scanThroughTo((int) growIndex) ;
    for (FadingMap map : allMaps) map.update();
    //
    //  TODO:  Let the player view this on the minimap!
    //squalorMap.presentVals("Squalor", -1, true) ;
  }
  
  
  private void growthAt(Tile t) {
    Flora.tryGrowthAt(t) ;
    final Element owner = t.owner() ;
    if (owner != null) owner.onGrowth(t) ;
    ambience.updateAt(t) ;
    //world.terrain().setSqualor(t, (byte) squalorAmount(t)) ;
  }
  
  
  public void impingeBiomass(Tile t, float amount, float duration) {
    biomass.accumulate(amount, duration, t.x, t.y);
  }
  
  
  public void impingeAbundance(Fauna f, float duration) {
    final Tile t = f.origin() ;
    final Species s = f.species ;
    final float inc = f.health.maxHealth() ;
    
    if (s.type == Species.Type.BROWSER ) {
      preyMap.accumulate(inc, duration, t.x, t.y);
    }
    if (s.type == Species.Type.PREDATOR) {
      hunterMap.accumulate(inc, duration, t.x, t.y);
    }
    abundances[s.ID].accumulate(inc, duration, t.x, t.y);
  }
  
  
  
  /**  Terraforming methods-
    */
  public void pushClimate(Habitat desired, float strength) {
    //  TODO:  This is the next thing to implement.
  }
  
  
  
  /**  Querying sample values-
    */
  /*
  public float biomassAmount(Tile t) {
    return biomass.longTermVal(t) ;
  }
  //*/
  
  
  public float biomassRating(Tile t) {
    return biomass.sampleAt(t.x, t.y);
    //return biomass.longTermVal(t) * 4f / (SR * SR) ;
  }
  
  
  public float globalBiomass() {
    return biomass.overallValue() ;
  }
}










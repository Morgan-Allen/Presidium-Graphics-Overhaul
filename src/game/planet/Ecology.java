

package src.game.planet ;
import src.game.common.* ;
import src.util.* ;



//  TODO:  You might be able to get rid of this class entirely, or merge with
//  the EcologyGen class instead.


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
    
    allMaps.add(biomass    = new FadingMap(world, SS)) ;
    allMaps.add(preyMap    = new FadingMap(world, SS)) ;
    allMaps.add(hunterMap  = new FadingMap(world, SS)) ;
    
    abundances = new FadingMap[Species.ANIMAL_SPECIES.length] ;
    for (int i = 0 ; i < Species.ANIMAL_SPECIES.length ; i++) {
      abundances[i] = new FadingMap(world, SS) ;
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
    
    for (FadingMap map : allMaps) {
      map.performFade() ;
    }
    
    //
    //  TODO:  Let the player view this on the minimap!
    //squalorMap.presentVals("Squalor", -1, true) ;
  }
  
  
  private void growthAt(Tile t) {
    Flora.tryGrowthAt(t.x, t.y, world, false) ;
    final Element owner = t.owner() ;
    if (owner != null) owner.onGrowth(t) ;
    ambience.updateAt(t) ;
    //world.terrain().setSqualor(t, (byte) squalorAmount(t)) ;
  }
  
  
  public void impingeBiomass(Element e, float amount, boolean gradual) {
    biomass.impingeVal(e.origin(), amount, gradual) ;
  }
  
  /*
  public void impingeSqualor(float squalorVal, Fixture f, boolean gradual) {
    squalorMap.impingeVal(f.area(), squalorVal, gradual) ;
  }
  //*/
  
  
  public void impingeAbundance(Fauna f, boolean gradual) {
    final Tile t = f.origin() ;
    final Species s = f.species ;
    final float inc = f.health.maxHealth() ;
    abundances[s.ID].impingeVal(t, inc, gradual) ;
    if (s.type == Species.Type.BROWSER ) preyMap.impingeVal(t, inc, gradual) ;
    if (s.type == Species.Type.PREDATOR) hunterMap.impingeVal(t, inc, gradual) ;
  }
  
  
  
  
  /**  Terraforming methods-
    */
  public void pushClimate(Habitat desired, float strength) {
    //  TODO:  This is the next thing to implement.
  }
  
  
  
  
  
  /**  Querying sample values-
    */
  public float biomassAmount(Tile t) {
    return biomass.longTermVal(t) ;
  }
  
  
  public float biomassRating(Tile t) {
    return biomass.longTermVal(t) * 4f / (SR * SR) ;
  }
  
  
  public float globalBiomass() {
    return biomass.overallSum() / (world.size * world.size) ;
  }
}










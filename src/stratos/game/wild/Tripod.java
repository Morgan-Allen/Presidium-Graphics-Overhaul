


package stratos.game.wild ;
import stratos.game.actors.* ;
import stratos.game.common.* ;
import stratos.game.planet.* ;
import stratos.game.building.* ;
import stratos.graphics.common.* ;
import stratos.graphics.solids.* ;
import stratos.graphics.widgets.* ;
import stratos.user.* ;
import stratos.util.* ;




public class Tripod extends Artilect implements Economy {
  
  
  final String name ;
  
  
  public Tripod(Base base) {
    super(base, Species.SPECIES_ARTILECT) ;
    
    traits.initAtts(30, 10, 5) ;
    health.initStats(
      100, //lifespan
      5.0f,//bulk bonus
      1.0f,//sight range
      0.8f,//move speed,
      ActorHealth.ARTILECT_METABOLISM
    ) ;
    health.setupHealth(0, Rand.avgNums(2), Rand.avgNums(2)) ;
    
    gear.setDamage(20) ;
    gear.setArmour(20) ;
    traits.setLevel(MARKSMANSHIP, 10) ;
    traits.setLevel(HAND_TO_HAND, 10) ;
    gear.equipOutfit(Item.withQuality(ARTILECT_ARMOUR, 0)) ;
    gear.equipDevice(Item.withQuality(INTRINSIC_ENERGY_WEAPON, 0)) ;
    
    attachSprite(MODEL_TRIPOD.makeSprite()) ;
    name = nameWithBase("Tripod ") ;
  }
  
  
  public Tripod(Session s) throws Exception {
    super(s) ;
    name = s.loadString() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveString(name) ;
  }
  
  
  
  /**  Physical properties-
    */
  public float radius() {
    return 0.5f ;
  }
  
  
  public float height() {
    return 2.5f * super.height() ;
  }
  
  
  protected float aboveGroundHeight() {
    return 0.02f ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return name ;
  }
  
  
  public String helpInfo() {
    return
      "Tripods are among the more feared of the artilect guardians wandering "+
      "the landscape.  Even in a decrepit state, they are well-armed and "+
      "will attack organics with scant provocation." ;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return null ;
  }
}





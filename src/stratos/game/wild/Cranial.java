


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



public class Cranial extends Artilect implements Economy {
  
  
  /**  Construction and save/load methods-
    */
  final String name ;
  
  
  public Cranial(Base base) {
    super(base, (Species) null) ;
    
    traits.initAtts(20, 10, 25) ;
    health.initStats(
      1000,//lifespan
      1.5f,//bulk bonus
      1.0f,//sight range
      0.6f,//move speed,
      ActorHealth.ARTILECT_METABOLISM
    ) ;
    health.setupHealth(0, Rand.avgNums(2), Rand.avgNums(2)) ;
    
    gear.setDamage(15) ;
    gear.setArmour(15) ;
    gear.equipDevice(Item.withQuality(INTRINSIC_MELEE_WEAPON, 0)) ;
    traits.setLevel(HAND_TO_HAND, 5) ;
    traits.setLevel(ANATOMY, 10) ;
    traits.setLevel(ASSEMBLY, 15) ;
    
    attachModel(MODEL_CRANIAL) ;
    name = nameWithBase("Cranial ") ;
  }
  
  
  public Cranial(Session s) throws Exception {
    super(s) ;
    name = s.loadString() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveString(name) ;
  }
  
  
  
  /**  Physical properties-
    */
  public float aboveGroundHeight() {
    return 0.5f ;
  }
  
  
  public float radius() {
    return 0.5f ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return name ;
  }
  
  
  public String helpInfo() {
    return
      "Cranials are cunning, quasi-organic machine intelligences with a "+
      "propensity for abducting living creatures for purposes of dissection, "+
      "interrogation and ultimate assimilation." ;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return null ;
  }
}







package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.graphics.solids.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;



public class Drone extends Artilect {
  
  
  /**  Construction and save/load methods-
    */
  final String name;
  
  
  public Drone(Base base) {
    super(base, Species.DRONE);
    
    traits.initAtts(15, 10, 5);
    health.initStats(
      10,  //lifespan
      0.65f,//bulk bonus
      1.00f,//sight range
      1.25f,//move speed,
      ActorHealth.ARTILECT_METABOLISM
    );
    health.setupHealth(0, Rand.avgNums(2), Rand.avgNums(2));

    gear.setBaseDamage(10);
    gear.setBaseArmour(10);
    traits.setLevel(MARKSMANSHIP, 5);
    gear.equipDevice(Item.withQuality(Economy.INTRINSIC_BEAM, 0));
    
    traits.setLevel(CURIOUS, 1);
    
    final ModelAsset model = DRONE_MODELS[Rand.index(3)];
    attachSprite(model.makeSprite());
    name = nameWithBase("Drone ");
  }
  
  
  public Drone(Session s) throws Exception {
    super(s);
    name = s.loadString();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveString(name);
  }
  
  
  
  /**  Physical properties-
    */
  public float aboveGroundHeight() {
    return health.conscious() ? 0.5f : 0;
  }
  
  
  public float radius() {
    return 0.5f;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return name;
  }
  
  
  public String helpInfo() {
    return
      "Defence Drones are simple, disposable automatons capable of limited "+
      "field operations without supervision.";
  }
  
  
  public Composite portrait(BaseUI UI) {
    return null;
  }
}




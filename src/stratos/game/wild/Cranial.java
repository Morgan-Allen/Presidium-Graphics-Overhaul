/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.game.wild.Species.Type;
import stratos.graphics.common.*;
import stratos.graphics.solids.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Devices.*;
import static stratos.game.economic.Outfits.*;
import static stratos.game.actors.Technique.*;



//  TODO:  Add the grapple, silver plague and psy strike abilities.

public class Cranial extends Artilect {
  
  
  /**  Construction and save/load methods-
    */
  final public static ModelAsset
    MODEL_CRANIAL = MS3DModel.loadFrom(
      FILE_DIR, "Cranial.ms3d", Cranial.class,
      XML_FILE, "Cranial"
    );
  
  final public static Species SPECIES = new Species(
    Cranial.class,
    "Cranial",
    "Cranials are cunning, quasi-organic machine intelligences that direct "+
    "the efforts of their lesser brethren.  They appear to have a marked "+
    "propensity for tortuous experiments on living creatures.",
    null,
    null,
    Type.ARTILECT, 1, 1, 1
  ) {
    public Actor sampleFor(Base base) { return new Cranial(base); }
  };
  
  
  final String name;
  
  
  public Cranial(Base base) {
    super(base, SPECIES);
    
    traits.initAtts(10, 20, 30);
    health.initStats(
      1000,//lifespan
      3.5f,//bulk bonus
      1.0f,//sight range
      0.6f,//move speed,
      ActorHealth.ARTILECT_METABOLISM
    );
    health.setupHealth(0, Rand.avgNums(2), Rand.avgNums(2));
    
    gear.setBaseDamage(15);
    gear.setBaseArmour(15);
    gear.equipDevice(Item.withQuality(LIMB_AND_MAW       , 0));
    gear.equipOutfit(Item.withQuality(INTRINSIC_SHIELDING, 0));
    
    traits.setLevel(HAND_TO_HAND, 15 + Rand.index(5) - 2);
    traits.setLevel(ANATOMY     , 10 + Rand.index(5) - 2);
    traits.setLevel(ASSEMBLY    , 20 + Rand.index(5) - 2);
    traits.setLevel(INSCRIPTION , 10 + Rand.index(5) - 2);
    
    traits.setLevel(IMPASSIVE, 1);
    traits.setLevel(CRUEL    , 1);
    
    skills.addTechnique(IMPALE           );
    skills.addTechnique(IMPLANTATION     );
    skills.addTechnique(SHIELD_ABSORPTION);
    skills.addTechnique(SELF_ASSEMBLY    );
    
    attachModel(MODEL_CRANIAL);
    name = nameWithBase("Cranial ");
  }
  
  
  public Cranial(Session s) throws Exception {
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
    return 0;
  }
  
  
  public float height() {
    return 2.0f;
  }
  
  
  public float radius() {
    return 0.5f;
  }
  
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return name;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return null;
  }
  
  
  protected float moveAnimStride() {
    return 2.0f;
  }
}

















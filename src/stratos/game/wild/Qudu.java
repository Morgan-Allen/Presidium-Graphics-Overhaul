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
import stratos.graphics.common.ModelAsset;
import stratos.graphics.cutout.CutoutModel;
import stratos.graphics.solids.MS3DModel;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;



public class Qudu extends Fauna {
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  final public static ModelAsset
    MODEL_NEST_QUUD = CutoutModel.fromImage(
      Qudu.class, LAIR_DIR+"nest_quud.png", 2.5f, 2
    );
  
  final public static Species SPECIES = new Species(
    Qudu.class,
    "Qudu",
    "Qudu are placid, slow-moving, vegetarian browsers that rely on their "+
    "dense, leathery hides and intractable grip on the ground to protect "+
    "themselves from most predators.",
    FILE_DIR+"QuduPortrait.png",
    MS3DModel.loadFrom(
      FILE_DIR, "Qudu.ms3d", Qudu.class,
      XML_FILE, "Qudu"
    ),
    Species.Type.BROWSER,
    1.50f, //bulk
    0.50f, //speed
    0.75f  //sight
  ) {
    final Blueprint BLUEPRINT = NestUtils.constructBlueprint(
      2, 2, this, MODEL_NEST_QUUD
    );
    public Actor sampleFor(Base base) { return init(new Qudu(base)); }
    public Blueprint nestBlueprint() { return BLUEPRINT; }
  };
  
  
  public Qudu(Base base) {
    super(SPECIES, base);
  }
  
  
  public Qudu(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  protected void initStats() {
    traits.initAtts(7, 3, 2);
    health.initStats(
      1,    //lifespan
      species.baseBulk , //bulk bonus
      species.baseSight, //sight range
      species.speedMult, //speed rate
      ActorHealth.ANIMAL_METABOLISM
    );
    gear.setBaseDamage(8);
    gear.setBaseArmour(15);
    
    traits.setLevel(DEFENSIVE, -1);
    traits.setLevel(FEARLESS , -2);
    
    skills.addTechnique(FORTIFY );
    skills.addTechnique(WITHDRAW);
  }
  
  
  public float radius() {
    return 0.33f;
  }
  
  
  public float height() {
    return 1.33f * super.height();
  }
  
  

  /**  Rendering and interface methods-
    */
  protected float moveAnimStride() { return super.moveAnimStride() * 0.8f; }
  protected float spriteScale() { return super.spriteScale() * 0.8f; }
}




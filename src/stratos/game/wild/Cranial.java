/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
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
    return 1.5f;
  }
  
  
  public float radius() {
    return 0.5f;
  }
  
  
  
  /**  Special Techniques-
    */
  //  TODO:  MOVE TO THE ARTILECT CLASS?
  final static Class BASE_CLASS = Artilect.class;
  final static String
    UI_DIR = "media/GUI/Powers/",
    FX_DIR = "media/SFX/";
  
  final static Technique DETONATE = new Technique(
    "Detonate", UI_DIR+"detonate.png",
    "description",
    BASE_CLASS, "detonate",
    MINOR_POWER         ,
    MILD_HARM           ,
    NO_CONCENTRATION    ,
    NO_FATIGUE          ,
    IS_PASSIVE_ALWAYS | IS_NATURAL_ONLY, null, 0,
    Action.FALL, Action.NORMAL
  ) {
    
  };
  
  final static Technique IMPALE = new Technique(
    "Impale", UI_DIR+"artilect_impale.png",
    "description",
    BASE_CLASS, "artilect_impale",
    MEDIUM_POWER        ,
    EXTREME_HARM        ,
    MEDIUM_CONCENTRATION,
    NO_FATIGUE          ,
    IS_FOCUS_TARGETING | IS_NATURAL_ONLY, null, 0,
    Action.STRIKE_BIG, Action.NORMAL
  ) {
    
  };
  
  final static Technique POSITRON_BEAM = new Technique(
    "Positron Beam", UI_DIR+"positron_beam.png",
    "description",
    BASE_CLASS, "positron_beam",
    MAJOR_POWER         ,
    EXTREME_HARM        ,
    MEDIUM_CONCENTRATION,
    NO_FATIGUE          ,
    IS_FOCUS_TARGETING | IS_NATURAL_ONLY, null, 0,
    Action.FIRE, Action.RANGED
  ) {
    
  };
  
  final static Technique SHIELD_ABSORPTION = new Technique(
    "Shield Absorption", UI_DIR+"artilect_shield_absorb.png",
    "description",
    BASE_CLASS, "artilect_shield_absorb",
    MEDIUM_POWER    ,
    NO_HARM         ,
    NO_CONCENTRATION,
    NO_FATIGUE      ,
    IS_PASSIVE_SKILL_FX | IS_NATURAL_ONLY, null, 0,
    STEALTH_AND_COVER
  ) {
    
  };
  
  final static Technique SLOUGH_FLESH = new Technique(
    "Slough Flesh", UI_DIR+"artilect_slough_flesh.png",
    "description",
    BASE_CLASS, "artilect_slough_flesh",
    MAJOR_POWER        ,
    REAL_HARM          ,
    MAJOR_CONCENTRATION,
    NO_FATIGUE         ,
    IS_FOCUS_TARGETING | IS_NATURAL_ONLY, null, 0,
    Action.STRIKE, Action.NORMAL
  ) {
    
  };
  
  final static Technique SELF_ASSEMBLE = new Technique(
    "Self Assemble", UI_DIR+"self_assemble.png",
    "description",
    BASE_CLASS, "self_assemble",
    MAJOR_POWER         ,
    NO_HARM             ,
    MAJOR_CONCENTRATION ,
    NO_FATIGUE          ,
    IS_FOCUS_TARGETING | IS_NATURAL_ONLY, null, 0,
    Action.FIRE, Action.RANGED
  ) {
    
  };
  
  
  
  
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

















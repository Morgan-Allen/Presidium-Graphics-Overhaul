/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.wild;
import static stratos.game.actors.Qualities.HAND_TO_HAND;
import static stratos.game.actors.Qualities.EVASION;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.maps.*;
import stratos.game.plans.Hunting;
import stratos.game.wild.Species.Type;
import stratos.graphics.common.ModelAsset;
import stratos.graphics.cutout.CutoutModel;
import stratos.graphics.solids.MS3DModel;
import stratos.util.*;



public class Hareen extends Fauna {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static ModelAsset
    MODEL_NEST_VAREEN = CutoutModel.fromImage(
      Hareen.class, "vareen_nest_model",
      LAIR_DIR+"nest_vareen.png", 2.5f, 3
    );
  
  final public static Species SPECIES = new Species(
    Hareen.class,
    "Hareen",
    "Hareen are sharp-eyed aerial omnivores, with a twinned pair of wings "+
    "that make them highly maneuverable flyers.  Their diet includes fruit, "+
    "seeds, insects and carrion, but symbiotic algae also supplement their "+
    "needs.",
    FILE_DIR+"VareenPortrait.png",
    MS3DModel.loadFrom(
      FILE_DIR, "Vareen.ms3d", Hareen.class,
      XML_FILE, "Vareen"
    ),
    Type.BROWSER,
    0.75f, //bulk
    2.60f, //speed
    1.00f  //sight
  ) {
    final Blueprint BLUEPRINT = NestUtils.constructBlueprint(
      2, 2, this, MODEL_NEST_VAREEN
    );
    public Actor sampleFor(Base base) { return init(new Hareen(base)); }
    public Blueprint nestBlueprint() { return BLUEPRINT; }
  };
  
  
  final static float DEFAULT_FLY_HEIGHT = 1.25f;
  final static int FLY_PATH_LIMIT = 16;
  
  private float flyHeight = DEFAULT_FLY_HEIGHT;
  private Nest nest = null;
  
  
  
  public Hareen(Base base) {
    super(SPECIES, base);
  }
  
  
  public Hareen(Session s) throws Exception {
    super(s);
    flyHeight = s.loadFloat();
    nest = (Nest) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveFloat(flyHeight);
    s.saveObject(nest);
  }
  
  
  protected void initStats() {
    //
    //  TODO:  PUT ALL THESE ATTRIBUTES IN THE SPECIES FIELDS
    traits.initAtts(10, 20, 3);
    health.initStats(
      5,    //lifespan
      species.baseBulk , //bulk bonus
      species.baseSight, //sight range
      species.speedMult, //speed rate
      ActorHealth.ANIMAL_METABOLISM
    );
    gear.setBaseDamage(4);
    gear.setBaseArmour(2);
    
    traits.setLevel(HAND_TO_HAND     , 5 );
    traits.setLevel(EVASION, 15);
    
    skills.addTechnique(BASK  );
    skills.addTechnique(FLIGHT);
  }
  
  
  public float radius() {
    return 0.5f;
  }
  
  
  
  /**  Behaviour modifications/implementation-
    */
  protected void updateAsMobile() {
    final Target target = this.planFocus(null, true);
    float idealHeight = DEFAULT_FLY_HEIGHT;
    
    if (indoors() || ! health.conscious()) {
      idealHeight = 0;
    }
    else if (
      (target != null && target != this) &&
      (Spacing.distance(this, target) < 2)
    ) {
      idealHeight = target.position(null).z + (target.height() / 2f) - 0.5f;
    }
    
    final float min = flyHeight - 0.1f, max = flyHeight + 0.1f;
    flyHeight = Nums.clamp(Nums.max(0, idealHeight), min, max);
    
    super.updateAsMobile();
  }
  

  public void updateAsScheduled(int numUpdates, boolean instant) {
    if (! indoors()) {
      final float value = Planet.dayValue(world) / Stage.STANDARD_DAY_LENGTH;
      health.takeCalories(value, 1);
    }
    super.updateAsScheduled(numUpdates, instant);
  }
  
  
  protected float aboveGroundHeight() {
    return flyHeight;
  }
  
  
  protected Behaviour nextBrowsing() {
    final Choice c = new Choice(this);
    for (Target e : senses.awareOf()) {
      if (Hunting.validPrey(e, this)) {
        final Actor prey = (Actor) e;
        if (! prey.health.alive()) c.add(Hunting.asFeeding(this, prey));
      }
    }
    final Behaviour p = c.pickMostUrgent();
    if (p != null) return p;
    return super.nextBrowsing();
  }
  


  /**  Rendering and interface methods-
    */
  protected float moveAnimStride() { return 1.0f; }
}





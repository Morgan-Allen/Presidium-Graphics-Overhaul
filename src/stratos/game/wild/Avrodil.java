/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.game.economic.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Technique.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.solids.*;
import stratos.util.*;



public class Avrodil extends Fauna implements Mount {
  
  
  final public static Species SPECIES = new Species(
    Avrodil.class,
    "Avrodil",
    "ENTER AVRODIL DESCRIPTION HERE",
    
    FILE_DIR+"AvrodilPortrait.png",
    MS3DModel.loadFrom(
      FILE_DIR, "Avrodil.ms3d", Avrodil.class,
      XML_FILE, "Avrodil"
    ),
    Species.Type.PREDATOR,
    3.50f, //bulk
    0.35f, //speed
    0.85f  //sight
  ) {
    final ModelAsset NEST_MODEL = CutoutModel.fromImage(
      Avrodil.class, LAIR_DIR+"sporing_body.png", 3.5f, 3
    );
    final Blueprint BLUEPRINT = Nest.constructBlueprint(
      3, 2, this, NEST_MODEL
    );
    public Actor sampleFor(Base base) { return init(new Avrodil(base)); }
    public Blueprint nestBlueprint() { return BLUEPRINT; }
  };
  
  final static float
    DIGEST_DURATION       = Stage.STANDARD_SHIFT_LENGTH,
    DIGEST_REGEN_FRACTION = 0.2f;
  
  
  private Actor digesting;
  

  public Avrodil(Base base) {
    super(SPECIES, base);
  }
  
  
  public Avrodil(Session s) throws Exception {
    super(s);
    this.digesting = (Actor) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(digesting);
  }
  
  
  protected void initStats() {
    traits.initAtts(20, 3, 6);
    health.initStats(
      1,                 //lifespan
      species.baseBulk , //bulk bonus
      species.baseSight, //sight range
      species.speedMult, //speed rate
      ActorHealth.ANIMAL_METABOLISM
    );
    gear.setBaseDamage(12);
    gear.setBaseArmour(4);
    
    traits.setLevel(FEARLESS    , 1);
    traits.setLevel(MARKSMANSHIP, 5  + Rand.index(5) - 3);
    traits.setLevel(HAND_TO_HAND, 15  + Rand.index(5) - 3);
    
    skills.addTechnique(CAMOUFLAGE);
  }
  
  
  protected void addReactions(Target seen, Choice choice) {
  }
  
  
  protected void addChoices(Choice choice) {
    super.addChoices(choice);
  }
  
  
  
  /**  Specialised Techniques for personal use:
    */
  final static String DIR = "media/GUI/Powers/";
  final static Class BASE_CLASS = Avrodil.class;
  
  
  final public static Technique CAMOUFLAGE = new Technique(
    "Camouflage", DIR+"camouflage.png", Action.FALL,
    BASE_CLASS         , "avrodil_camo",
    MINOR_POWER        ,
    NO_HARM            ,
    MINOR_FATIGUE      ,
    MAJOR_CONCENTRATION,
    Technique.TYPE_PASSIVE_EFFECT, null, 0
  ) {
    
    public float bonusFor(Actor using, Skill skill, Target subject) {
      return -1;
    }
    
    
    public void applyEffect(Actor using, boolean success, Target subject) {
      super.applyEffect(using, success, subject);
      final Tile location = using.origin();
      if (location.habitat().floraSpecies == null) return;
      using.traits.setLevel(asCondition, 1);
    }
    
    
    protected void applyAsCondition(Actor affected) {
      super.applyAsCondition(affected);
      
      if (affected.isMoving()) {
        affected.traits.remove(asCondition);
      }
      else {
        final Tile location = affected.origin();
        final float cover = location.world.ecology().forestRating(location);
        affected.traits.incBonus(STEALTH_AND_COVER, 20 * cover);
        affected.traits.setLevel(asCondition, 0.99f);
      }
    }
    
    
    protected void onConditionStart(Actor affected) {
      super.onConditionStart(affected);
      
      final Tile location = affected.origin();
      final Species flora[] = location.habitat().floraSpecies;
      final ModelAsset model = flora[0].modelSequence[2];
      affected.attachDisguise(model.makeSprite());
    }
    
    
    protected void onConditionEnd(Actor affected) {
      super.onConditionEnd(affected);
      affected.detachDisguise();
    }
    
    
    public boolean triggeredBy(
      Actor actor, Plan current, Action action, Skill used
    ) {
      if (current instanceof Retreat) {
        return true;
      }
      if (current instanceof Resting) {
        return true;
      }
      return false;
    }
  };
  
  
  final public static Technique DEVOUR = new Technique(
    "Devour", DIR+"devour.png", Action.STRIKE_BIG,
    BASE_CLASS          , "avrodil_devour",
    MEDIUM_POWER        ,
    EXTREME_HARM        ,
    MEDIUM_FATIGUE      ,
    MEDIUM_CONCENTRATION,
    Technique.TYPE_INDEPENDANT_ACTION, null, 0
  ) {
    
    public float bonusFor(Actor using, Skill skill, Target subject) {
      return -1;
    }
    
    
    public void applyEffect(Actor using, boolean success, Target subject) {
      final Avrodil eats = (Avrodil) using;
      final Actor victim = (Actor) subject;
      
      final float maxBulk = eats.health.baseBulk() / 2;
      final float chance = 1f - (victim.health.baseBulk() / maxBulk);
      if (Rand.num() > chance) success = false;
      
      super.applyEffect(using, success, subject);
      if (success) victim.bindToMount(eats);
    }
    
    
    public boolean triggeredBy(
      Actor actor, Plan current, Action action, Skill used
    ) {
      if (! (action.subject() instanceof Actor)) {
        return false;
      }
      final Avrodil eats = (Avrodil) actor;
      final Actor victim = (Actor) action.subject();
      if (eats.digesting != null) {
        return false;
      }
      if (victim.health.baseBulk() > eats.health.baseBulk() / 2) {
        return false;
      }
      if (current instanceof Combat && used == HAND_TO_HAND) {
        return true;
      }
      return false;
    }
  };
  
  
  
  //  TODO:  Also include Whiplash and Pollen Spray.
  
  
  
  /**  Other assorted vital statistics-
    */
  public float radius() {
    return 0.75f;
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    
    if (digesting != null && ! instant) {
      final float burn = digesting.health.maxHealth() / DIGEST_DURATION;
      digesting.health.takeFatigue(burn);
      health.takeCalories(burn, 1);
      health.liftInjury(burn * DIGEST_REGEN_FRACTION);
      if (digesting.health.isDead()) digesting = null;
    }
  }
  
  
  public void enterStateKO(String animName) {
    super.enterStateKO(animName);
    if (digesting != null) digesting.releaseFromMount();
  }
  

  public boolean setMounted(Actor mounted, boolean is) {
    if (is) this.digesting = mounted;
    else this.digesting = null;
    return true;
  }
  
  
  public Property mountStoresAt() {
    return mind.home();
  }
  
  
  public boolean allowsActivity(Plan activity) {
    return false;
  }
  
  
  public boolean actorVisible(Actor mounted) {
    if (Technique.isDoingAction(this, DEVOUR)) return true;
    return false;
  }
  
  
  public void configureSpriteFrom(
    Actor mounted, Action action, Sprite actorSprite
  ) {
  }
  
  
  public void describeActor(Actor mounted, Description d) {
    d.append("Being digesting by ");
    d.append(this);
  }
  
  
}












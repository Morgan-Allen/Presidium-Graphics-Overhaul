/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.sfx.*;
import stratos.graphics.solids.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Technique.*;



public class Avrodil extends Fauna {
  
  
  final public static Species SPECIES = new Species(
    Avrodil.class,
    "Avrodil",
    "The Avrodil is a species of voracious ambulatory plant life, notorious "+
    "for it's rapid reproduction and aggressive temperament.",
    FILE_DIR+"AvrodilPortrait.png",
    MS3DModel.loadFrom(
      FILE_DIR, "Avrodil.ms3d", Avrodil.class,
      XML_FILE, "Avrodil"
    ),
    Species.Type.VERMIN,
    3.50f, //bulk
    0.35f, //speed
    0.85f  //sight
  ) {
    
    final ModelAsset NEST_MODEL = CutoutModel.fromImage(
      Avrodil.class, "avrodil_nest_model", LAIR_DIR+"sporing_body.png", 2.5f, 2
    );
    final Blueprint BLUEPRINT = NestUtils.constructBlueprint(
      3, 2, this, NEST_MODEL
    );
    
    public Actor sampleFor(Base base) { return init(new Avrodil(base)); }
    public Blueprint nestBlueprint() { return BLUEPRINT; }
    public boolean fixedNesting() { return false; }
  };
  
  

  public Avrodil(Base base) {
    super(SPECIES, base);
  }
  
  
  public Avrodil(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
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
    traits.setLevel(MARKSMANSHIP, 5   + Rand.index(5) - 3);
    traits.setLevel(HAND_TO_HAND, 15  + Rand.index(5) - 3);
    
    skills.addTechnique(CAMOUFLAGE  );
    skills.addTechnique(DEVOUR      );
    skills.addTechnique(WHIPLASH    );
    skills.addTechnique(POLLEN_SPRAY);
  }
  
  
  protected void addReactions(Target seen, Choice choice) {
    super.addReactions(seen, choice);
  }
  
  
  protected void addChoices(Choice choice) {
    super.addChoices(choice);
  }
  
  
  protected Behaviour nextBuildingNest() {
    return null;
  }
  
  
  protected float breedingReadiness(boolean checkNest) {
    float BR = super.breedingReadiness() * 2;
    return BR;
  }
  
  
  
  /**  Specialised Techniques for personal use:
    */
  final static Class BASE_CLASS = Avrodil.class;
  final static String
    UI_DIR = "media/GUI/Powers/",
    FX_DIR = "media/SFX/";
  final static float
    POLLEN_RADIUS      = 2.0f,
    POLLEN_MOTOR_HIT   = 5   ,
    POLLEN_ACID_DAMAGE = 1.0f;
  
  
  final static ShotFX.Model
    WHIPLASH_MODEL = new ShotFX.Model(
      "whiplash_fx", BASE_CLASS,
      FX_DIR+"whiplash_thrown.png",
      -1, 0, 0.3f, 3.0f, false, false
    );
  final static PlaneFX.Model
    CAMO_CASTING_MODEL = PlaneFX.imageModel(
      "camo_cast_fx", BASE_CLASS,
      FX_DIR+"camo_casting.png",
      0.5f, 0, 0.2f, true, false
    ),
    WHIPLASH_BURST_MODEL = PlaneFX.imageModel(
      "whip_burst_fx", BASE_CLASS,
      FX_DIR+"whiplash_burst.png",
      0.5f, 0, 0, false, false
    ),
    POLLEN_BURST_MODEL = PlaneFX.imageModel(
      "pollen_burst_fx", BASE_CLASS,
      FX_DIR+"pollen_burst.png",
      1.0f, 0, 0.75f, false, false
    ),
    POLLEN_HAZE_MODEL = PlaneFX.animatedModel(
      "pollen_haze_fx", BASE_CLASS,
      FX_DIR+"pollen_haze.png",
      2, 2, 4, 1.0f, 0.25f
    );
  
  
  final public static Technique CAMOUFLAGE = new Technique(
    "Camouflage", UI_DIR+"camouflage.png",
    "Allows the Avrodil to enter a vegetative state, both concealing it's "+
    "location and regenerating health.",
    BASE_CLASS, "avrodil_camo",
    MINOR_POWER     ,
    REAL_HELP       ,
    NO_FATIGUE      ,
    NO_CONCENTRATION,
    IS_SELF_TARGETING | IS_NATURAL_ONLY, null, 0,
    Action.FALL, Action.NORMAL
  ) {
    
    public boolean triggersAction(Actor actor, Plan current, Target subject) {
      if (actor.traits.hasTrait(asCondition)) {
        return false;
      }
      if (current instanceof Retreat) {
        return true;
      }
      if (current instanceof Resting) {
        return true;
      }
      return false;
    }
    
    
    private ModelAsset disguiseFor(Actor actor) {
      final Tile location = actor.origin();
      final Species flora[] = location.habitat().floraSpecies;
      if (flora == null) return null;
      return flora[0].modelSequence[2];
    }
    
    
    protected boolean checkActionSuccess(Actor actor, Target subject) {
      if (actor.isMoving()) return false;
      if (disguiseFor(actor) == null) return false;
      return super.checkActionSuccess(actor, subject);
    }


    public void applyEffect(
      Actor using, Target subject, boolean success, boolean passive
    ) {
      if (! success) return;
      super.applyEffect(using, subject, success, passive);
      using.traits.setLevel(asCondition, 1);
    }
    
    
    protected void applyAsCondition(Actor affected) {
      super.applyAsCondition(affected);
      
      if (affected.isMoving() || ! affected.health.alive()) {
        affected.traits.remove(asCondition);
      }
      else {
        final Tile location = affected.origin();
        final float cover = location.world.ecology().forestRating(location);
        affected.traits.incBonus(EVASION, 20 * cover);
        affected.traits.setLevel(asCondition, 0.99f);
        
        float feed = 1f / Stage.STANDARD_DAY_LENGTH;
        feed *= affected.health.maxHealth();
        affected.health.takeCalories(feed / 2, 1);
        affected.health.liftInjury(feed / 2);
      }
    }
    
    
    protected void onConditionStart(Actor affected) {
      super.onConditionStart(affected);
      
      final ModelAsset model = disguiseFor(affected);
      if (model == null) {
        affected.traits.remove(asCondition);
        return;
      }
      affected.attachDisguise(model.makeSprite());
      
      applyAsCondition(affected);
      SenseUtils.breaksPursuit(affected, null);
    }
    
    
    protected void onConditionEnd(Actor affected) {
      super.onConditionEnd(affected);
      affected.detachDisguise();
    }
  };
  
  
  final public static Technique WHIPLASH = new Technique(
    "Whiplash", UI_DIR+"avrodil_whiplash.png",
    "Allows the Avrodil to lash out at distant targets, dealing injury and "+
    "drawing them into closer range.",
    BASE_CLASS, "avrodil_whiplash",
    MINOR_POWER         ,
    REAL_HARM           ,
    MINOR_FATIGUE       ,
    MEDIUM_CONCENTRATION,
    IS_FOCUS_TARGETING | IS_NATURAL_ONLY, null, 0,
    Action.STRIKE, Action.QUICK | Action.RANGED
  ) {
    
    public void applyEffect(
      Actor using, Target subject, boolean success, boolean passive
    ) {
      super.applyEffect(using, subject, success, passive);
      
      ActionFX.applyShotFX(
        WHIPLASH_MODEL, WHIPLASH_BURST_MODEL,
        using, subject, success, 0.5f, using.world()
      );
      if (success) {
        final Actor struck = (Actor) subject;
        Vec3D pos = struck.position(null).add(using.position(null));
        pos.scale(0.5f);
        struck.setHeading(pos, struck.rotation(), false, using.world());
      }
    }
    
    
    protected boolean checkActionSuccess(Actor actor, Target subject) {
      return Combat.performStrike(
        actor, (Actor) subject,
        MARKSMANSHIP, EVASION,
        Combat.OBJECT_DESTROY, actor.currentAction()
      );
    }
    
    
    public boolean triggersAction(
      Actor actor, Plan current, Target subject
    ) {
      if (! (subject instanceof Actor)) {
        return false;
      }
      if (Spacing.distance(actor, subject) < 1) return false;
      return current instanceof Combat;
    }
  };
  
  
  final public static Technique POLLEN_SPRAY = new Technique(
    "Pollen Spray", UI_DIR+"avrodil_pollen_spray.png",
    "Deals poison damage to all nearby creatures.",
    BASE_CLASS, "avrodil_pollen_spray",
    MEDIUM_POWER        ,
    REAL_HARM           ,
    MEDIUM_FATIGUE      ,
    MEDIUM_CONCENTRATION,
    IS_FOCUS_TARGETING | IS_NATURAL_ONLY, null, 0,
    Action.STRIKE_BIG, Action.QUICK
  ) {
    
    public boolean triggersAction(
      Actor actor, Plan current, Target subject
    ) {
      if (! (subject instanceof Actor)) {
        return false;
      }
      if (Spacing.distance(actor, subject) > POLLEN_RADIUS - 0.5f) {
        return false;
      }
      return current instanceof Combat;
    }
    
    
    public void applyEffect(
      Actor using, Target subject, boolean success, boolean passive
    ) {
      super.applyEffect(using, subject, success, passive);
      ActionFX.applyBurstFX(POLLEN_BURST_MODEL, using, 0.5f, 1.5f);
      
      for (Actor hit : PlanUtils.subjectsInRange(using, POLLEN_RADIUS)) {
        if (hit == using || hit.species() == using.species()) continue;
        hit.traits.setLevel(asCondition, 1);
      }
    }
    
    
    protected void applyAsCondition(Actor affected) {
      super.applyAsCondition(affected);
      final float level = affected.traits.traitLevel(asCondition);
      
      affected.traits.incBonus(MOTOR, 0 - POLLEN_MOTOR_HIT * level);
      affected.health.takeInjury(POLLEN_ACID_DAMAGE * level, false);
      
      ActionFX.applyBurstFX(POLLEN_HAZE_MODEL, affected, 1.25f, 1.0f);
    }
  };
  
  
  
  /**  Other assorted vital statistics-
    */
  public float radius() {
    return 0.75f;
  }
  
  
}












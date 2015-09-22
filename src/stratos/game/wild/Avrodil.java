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
import stratos.graphics.sfx.PlaneFX;
import stratos.graphics.sfx.ShotFX;
import stratos.graphics.solids.*;
import stratos.start.Assets;
import stratos.user.BaseUI;
import stratos.user.SelectionPane;
import stratos.util.*;



public class Avrodil extends Fauna implements Captivity {
  
  
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
    return super.breedingReadiness(false) * 2;
  }
  
  
  
  /**  Specialised Techniques for personal use:
    */
  final static String DIR = "media/GUI/Powers/";
  final static Class BASE_CLASS = Avrodil.class;
  final static float
    POLLEN_RADIUS = 2.0f;
  
  
  final static ShotFX.Model
    WHIPLASH_MODEL = new ShotFX.Model(
      "whiplash_fx", BASE_CLASS, "media/SFX/whiplash_thrown.png",
      -1, 0, 0.3f, 3.0f, false, false
    );
  final static PlaneFX.Model
    CAMO_CASTING_MODEL = new PlaneFX.Model(
      "camo_cast_fx", BASE_CLASS, "media/SFX/camo_casting.png",
      0.5f, 0, 0.2f, true, false
    ),
    WHIPLASH_BURST_MODEL = new PlaneFX.Model(
      "whip_burst_fx", BASE_CLASS, "media/SFX/whiplash_burst.png",
      0.5f, 0, 0, false, false
    ),
    POLLEN_BURST_MODEL = new PlaneFX.Model(
      "pollen_burst_fx", BASE_CLASS, "media/SFX/pollen_burst.png",
      1.0f, 0, 0.75f, false, false
    ),
    POLLEN_HAZE_MODEL = new PlaneFX.Model(
      "pollen_haze_fx", BASE_CLASS, "media/SFX/pollen_haze.png",
      2, 2, 4, 1.0f, 0.25f
    );
  
  
  final public static Technique CAMOUFLAGE = new Technique(
    "Camouflage", DIR+"camouflage.png", Action.FALL,
    BASE_CLASS         , "avrodil_camo",
    MINOR_POWER        ,
    REAL_HELP          ,
    MINOR_FATIGUE      ,
    NO_CONCENTRATION   ,
    Technique.TYPE_PASSIVE_EFFECT, null, 0,
    Action.NORMAL
  ) {
    
    public void applyEffect(
      Actor using, boolean success, Target subject, boolean passive
    ) {
      super.applyEffect(using, success, subject, passive);
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
      Actor actor, Plan current, Action action, Skill used, boolean passive
    ) {
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
  };
  
  
  final public static Technique DEVOUR = new Technique(
    "Devour", DIR+"devour.png", Action.STRIKE_BIG,
    BASE_CLASS          , "avrodil_devour",
    MEDIUM_POWER        ,
    EXTREME_HARM        ,
    MEDIUM_FATIGUE      ,
    MEDIUM_CONCENTRATION,
    Technique.TYPE_INDEPENDANT_ACTION, null, 0,
    Action.QUICK
  ) {
    
    public void applyEffect(
      Actor using, boolean success, Target subject, boolean passive
    ) {
      final Avrodil eats = (Avrodil) using;
      final Actor victim = (Actor) subject;
      final boolean report = false;
      
      if (success) {
        final float maxBulk = eats.health.baseBulk() / 2;
        final float chance = 1f - (victim.health.baseBulk() / maxBulk);
        if (Rand.num() > chance) success = false;
        if (report) I.say("\nChance to devour is: "+chance);
      }
      else if (report) I.say("\nDevour attempt failed!");
      
      if (success) {
        super.applyEffect(using, success, subject, passive);
        victim.bindToMount(eats);
        if (report) I.say("  Devour attempt successful!");
      }
    }
    
    
    protected boolean checkActionSuccess(Actor actor, Target subject) {
      return Combat.performStrike(
        actor, (Actor) subject,
        HAND_TO_HAND, HAND_TO_HAND,
        Combat.OBJECT_DESTROY, actor.currentAction()
      );
    }
    
    
    public boolean triggeredBy(
      Actor actor, Plan current, Action action, Skill used, boolean passive
    ) {
      if (passive || action == null || ! (action.subject() instanceof Actor)) {
        return false;
      }
      if (! (current instanceof Combat)) return false;
      
      final Avrodil eats = (Avrodil) actor;
      final Actor victim = (Actor) action.subject();
      if (eats.digesting != null) {
        return false;
      }
      if (victim.health.baseBulk() > eats.health.baseBulk() / 2) {
        return false;
      }
      return true;
    }
  };
  
  
  final public static Technique WHIPLASH = new Technique(
    "Whiplash", DIR+"avrodil_whiplash.png", Action.STRIKE,
    BASE_CLASS          , "avrodil_whiplash",
    MINOR_POWER         ,
    REAL_HARM           ,
    MINOR_FATIGUE       ,
    MEDIUM_CONCENTRATION,
    Technique.TYPE_INDEPENDANT_ACTION, null, 0,
    Action.QUICK | Action.RANGED
  ) {
    
    public void applyEffect(
      Actor using, boolean success, Target subject, boolean passive
    ) {
      super.applyEffect(using, success, subject, passive);
      
      CombatFX.applyShotFX(
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
        MARKSMANSHIP, STEALTH_AND_COVER,
        Combat.OBJECT_DESTROY, actor.currentAction()
      );
    }
    
    
    public boolean triggeredBy(
      Actor actor, Plan current, Action action, Skill used, boolean passive
    ) {
      if (passive || action == null || ! (action.subject() instanceof Actor)) {
        return false;
      }
      if (Spacing.distance(actor, action.subject()) < 1) return false;
      return current instanceof Combat;
    }
  };
  
  
  final public static Technique POLLEN_SPRAY = new Technique(
    "Pollen Spray", DIR+"avrodil_pollen_spray.png", Action.STRIKE_BIG,
    BASE_CLASS          , "avrodil_pollen_spray",
    MEDIUM_POWER        ,
    REAL_HARM           ,
    MEDIUM_FATIGUE      ,
    MEDIUM_CONCENTRATION,
    Technique.TYPE_INDEPENDANT_ACTION, null, 0,
    Action.QUICK
  ) {
    
    public boolean triggeredBy(
      Actor actor, Plan current, Action action, Skill used, boolean passive
    ) {
      if (passive || action == null || ! (action.subject() instanceof Actor)) {
        return false;
      }
      if (Spacing.distance(actor, action.subject()) > POLLEN_RADIUS - 0.5f) {
        return false;
      }
      return current instanceof Combat;
    }
    
    
    public void applyEffect(
      Actor using, boolean success, Target subject, boolean passive
    ) {
      super.applyEffect(using, success, subject, passive);
      CombatFX.applyBurstFX(POLLEN_BURST_MODEL, using, 0.5f, 1.5f);
      
      for (Actor hit : Technique.subjectsInRange(using, POLLEN_RADIUS)) {
        if (hit == using || hit instanceof Avrodil) continue;
        hit.traits.setLevel(asCondition, 1);
      }
    }
    
    
    protected void applyAsCondition(Actor affected) {
      super.applyAsCondition(affected);
      affected.traits.incBonus(MOTOR, -5);
      affected.health.takeInjury(-1f, false);
      
      CombatFX.applyBurstFX(POLLEN_HAZE_MODEL, affected, 1.25f, 1.0f);
    }
  };
  
  
  
  /**  Other assorted vital statistics-
    */
  public float radius() {
    return 0.75f;
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    
    if (digesting != null && ! instant) {
      final float burn = digesting.health.maxHealth() / DIGEST_DURATION;
      digesting.health.setState(ActorHealth.STATE_SUSPEND);
      digesting.health.takeInjury(burn, true);
      health.takeCalories(burn, 1);
      health.liftInjury(burn * DIGEST_REGEN_FRACTION);
      if (digesting.health.isDead()) {
        digesting = null;
      }
    }
  }
  
  
  public void enterStateKO(String animName) {
    super.enterStateKO(animName);
    if (digesting != null) {
      digesting.health.setState(ActorHealth.STATE_ACTIVE);
      digesting.health.setBleeding(true);
      digesting.releaseFromMount();
    }
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
    viewPosition(actorSprite.position);
  }
  
  
  public void describeActor(Actor mounted, Description d) {
    d.append("Being digested by ");
    d.append(this);
  }
  
  
  public SelectionPane configSelectPane(SelectionPane panel, BaseUI UI) {
    final SelectionPane pane = super.configSelectPane(panel, UI);
    
    if (digesting != null) {
      pane.listing().append("\n  Digesting: ");
      pane.listing().append(digesting);
    }
    return pane;
  }
  
  
}












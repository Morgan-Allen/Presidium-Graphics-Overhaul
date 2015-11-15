/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.base.*;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.sfx.PlaneFX;
import stratos.graphics.widgets.*;
import stratos.start.PlayLoop;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Technique.*;



public abstract class Fauna extends Actor implements Mount {
  
  
  /**  Field definitions, constructors, and save/load functionality-
    */
  private static boolean
    verbose = false;
  
  final static String
    FILE_DIR = "media/Actors/fauna/",
    LAIR_DIR = "media/Buildings/lairs and ruins/",
    XML_FILE = "FaunaModels.xml";
  
  final public static int
    DEFAULT_FORAGE_DIST = Stage.ZONE_SIZE / 2,
    PREDATOR_SEPARATION = Stage.ZONE_SIZE * 2,
    MIN_SEPARATION      = 2,
    
    BROWSER_TO_FLORA_RATIO = 75,
    PREDATOR_TO_PREY_RATIO = 3 ,
    DEFAULT_BREED_INTERVAL = Stage.STANDARD_DAY_LENGTH;
  
  final public static float
    PLANT_CONVERSION = 4.0f,
    MEAT_CONVERSION  = 8.0f,
    NEST_INTERVAL    = Stage.STANDARD_DAY_LENGTH;
  
  
  final public Species species;
  private float breedMetre = 0.0f, lastMigrateCheck = -1;
  private Actor riding = null;
  
  
  public Fauna(Species species, Base base) {
    if (species == null) I.complain("NULL SPECIES!");
    this.species = species;
    mind.setVocation(species);
    initStats();
    attachSprite(species.modelSequence[0].makeSprite());
    assignBase(base);
  }
  
  
  public Fauna(Session s) throws Exception {
    super(s);
    species          = (Species) s.loadObject();
    breedMetre       = s.loadFloat();
    lastMigrateCheck = s.loadFloat();
    riding           = (Actor) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(species         );
    s.saveFloat (breedMetre      );
    s.saveFloat (lastMigrateCheck);
    s.saveObject(riding          );
  }
  
  
  public Species species() { return species; }
  protected abstract void initStats();
  
  
  
  /**  Registering abundance within the ecology class-
    */
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    
    final int period = 10;
    if (numUpdates % period == 0 && health.alive()) {
      final float breedRating = breedingReadiness(), breedInc;
      if (breedRating > 0) {
        breedInc = period * breedRating / DEFAULT_BREED_INTERVAL;
      }
      else {
        breedInc = period * -1f / DEFAULT_BREED_INTERVAL;
      }
      breedMetre = Nums.clamp(breedMetre + breedInc, 0, 1);
      
      updateTrophicPresence(period);
    }
  }
  
  
  public boolean enterWorldAt(int x, int y, Stage world, boolean intact) {
    if (! super.enterWorldAt(x, y, world, intact)) return false;
    
    updateTrophicPresence(-1);
    return true;
  }
  
  
  protected void updateTrophicPresence(int period) {
    final BaseDemands BD = base().demands;
    final Tile at = origin();
    BD.impingeSupply(species.trophicKey(), species.metabolism(), period, at);
    BD.impingeSupply(Fauna.class         , species.metabolism(), period, at);
  }
  
  
  protected float breedingReadiness() {
    final float crowding = NestUtils.crowding(this);
    float fertility = (health.agingStage() - 0.5f) * health.caloryLevel();
    return (1 - crowding) * Nums.clamp(fertility, 0, ActorHealth.AGE_MAX);
  }
  
  
  public float breedingCondition() {
    return this.breedMetre;
  }
  


  /**  Shared behavioural methods-
    */
  protected ActorMind initMind() {
    final Fauna fauna = this;
    return new ActorMind(fauna) {
      
      protected Choice createNewBehaviours(Choice choice) {
        if (choice == null) choice = new Choice(actor);
        fauna.addChoices(choice);
        return choice;
      }
      
      public void updateAI(int numUpdates) {
        super.updateAI(numUpdates);
      }
      
      protected void addReactions(Target seen, Choice choice) {
        fauna.addReactions(seen, choice);
      }
      
      protected void putEmergencyResponse(Choice choice) {
        fauna.putEmergencyResponse(choice);
      }
    };
  }
  
  
  protected ActorRelations initRelations() {
    //
    //  We install some default relationships with other animals, etc.-
    return new ActorRelations(this) {
      
      protected float initRelationValue(Accountable object) {
        if (object == actor || object == actor.mind.home()) {
          return 1.0f;
        }
        else if (object instanceof Actor) {
          final Actor other = (Actor) object;
          if (other.species().animal()) {
            final Fauna f = (Fauna) other;
            if (f.species == species) return 0.25f;
            if (f.species.type == Species.Type.BROWSER) return 0;
            if (f.species.predator() && species.preyedOn()) return -0.5f;
          }
          if (other.base() == actor.base()) return 0.5f;
          return -0.25f;
        }
        else return 0;
      }
      
      
      protected float initRelationNovelty(Accountable object) {
        if (object instanceof Fauna || object == actor.base()) return 0;
        else return MAX_NOVELTY;
      }
    };
  }
  
  
  protected void addChoices(Choice choice) {
    for (Target t : senses.awareOf()) addReactions(t, choice);
    if (species.browser () ) choice.add(nextBrowsing());
    if (species.predator() ) choice.add(nextHunting ());
    if (breedMetre >= 0.99f) choice.add(nextBreeding());
    if (senses.haven() != null) choice.add(new Resting(this, senses.haven()));
    
    if (domesticated()) {
      addDomesticBehaviours(choice);
    }
    else {
      choice.add(nextMigration   ());
      choice.add(nextBuildingNest());
    }
    choice.add(new Retreat(this));
  }
  
  
  protected void addReactions(Target seen, Choice choice) {
    if (seen instanceof Actor) choice.add(new Combat(this, (Actor) seen));
  }
  
  
  protected void putEmergencyResponse(Choice choice) {
    choice.add(new Retreat(this));
  }
  
  
  
  /**  Specific, generalised implementations for common behaviour types-
    */
  protected Behaviour nextHunting() {
    final Choice c = new Choice(this);
    for (Target e : senses.awareOf()) {
      if (Hunting.validPrey(e, this)) {
        final Actor prey = (Actor) e;
        c.add(Hunting.asFeeding(this, prey));
      }
    }
    return c.pickMostUrgent();
  }
  
  
  protected Behaviour nextBrowsing() {
    return Gathering.asBrowsing(this, NestUtils.forageRange(species));
  }
  
  
  protected Behaviour nextMigration() {
    return Exploring.nextWandering(this);
  }
  
  
  protected Behaviour nextBuildingNest() {
    final Nest nest = (Nest) this.mind.home();
    if (nest == null) return null;
    final float repair = nest.structure.repairLevel();
    if (repair >= 0.9f) return null;
    return new Repairs(this, nest, HANDICRAFTS, false);
  }
  
  
  /*
  //  TODO:  USE NESTING/FINDHOME FOR THIS
  
  protected Behaviour nextMigration() {
    final boolean report = verbose && I.talkAbout == this;
    Target wandersTo = null;
    String description = null;
    float priority = 0;
    
    final Target home = mind.home();
    Nest newNest = null;
    if (lastMigrateCheck == -1) lastMigrateCheck = world.currentTime();
    
    final float timeSinceCheck = world.currentTime() - lastMigrateCheck;
    final boolean homeless = ! (home instanceof Nest);
    if (report) {
      I.say("\nChecking migration for "+this);
      I.say("  Last check:  "+timeSinceCheck+"/"+NEST_INTERVAL);
      I.say("  Crowding is: "+NestUtils.crowding(this)+", homeless? "+homeless);
    }
    
    if (timeSinceCheck > NEST_INTERVAL || homeless) {
      final boolean crowded = homeless || NestUtils.crowding(this) > 0.5f;
      newNest = crowded ? NestUtils.findNestFor(this) : null;
      lastMigrateCheck = world.currentTime();
    }
    if (newNest != null && newNest != home) {
      wandersTo = newNest;
      description = "Migrating";
      priority = Action.ROUTINE;
    }
    else {
      final Target centre = mind.home() == null ? this : mind.home();
      wandersTo = Spacing.pickRandomTile(
        centre, NestUtils.forageRange(species) / 2, world
      );
      description = "Wandering";
      priority = Action.IDLE * (Planet.dayValue(world) + 1) / 2;
    }
    if (wandersTo == null) return null;
    
    final Action migrates = new Action(
      this, wandersTo,
      this, "actionMigrate",
      Action.LOOK, description
    );
    migrates.setPriority(priority);
    
    if (report) {
      I.say("  Wander point:    "+wandersTo);
      I.say("  Action priority: "+migrates.priorityFor(this));
      I.say("  Description:     "+description);
    }
    
    final Tile around = Spacing.pickFreeTileAround(wandersTo, this);
    if (around == null) return null;
    migrates.setMoveTarget(around);
    return migrates;
  }
  
  
  public boolean actionMigrate(Fauna actor, Target point) {
    if (point instanceof Nest) {
      final Nest nest = (Nest) point;
      
      if (NestUtils.crowding(species, nest, world) >= 1) {
        return false;
      }
      if (! nest.inWorld()) {
        if (! nest.canPlace()) {
          return false;
        }
        nest.assignBase(actor.base());
        nest.enterWorld();
        nest.structure.setState(Structure.STATE_INTACT, 0.01f);
      }
      actor.mind.setHome(nest);
    }
    return true;
  }
  //*/
  
  
  
  //  TODO:  CREATE SPECIAL PLAN FOR THIS AND SHARE WITH HUMANOIDS, ETC?
  
  protected Behaviour nextBreeding() {
    Target shelter = mind.home();
    if (shelter == null) shelter = senses.haven();
    if (shelter == null) return null;
    
    final Action breeds = new Action(
      this, shelter,
      this, "actionBreed",
      Action.FALL, "Breeding"
    );
    return breeds;
  }
  
  
  public boolean actionBreed(Fauna actor, Target at) {
    actor.breedMetre = 0;
    final int maxKids = 1 + (int) Nums.sqrt(10f / health.lifespan());
    
    for (int numKids = 1 + Rand.index(maxKids); numKids-- > 0;) {
      final Fauna young = (Fauna) species.sampleFor(base());
      final Tile e = at.world().tileAt(at);
      
      young.assignBase(this.base());
      young.health.setupHealth(0, 1, 0);
      young.enterWorldAt(e.x, e.y, e.world);
      
      if (I.logEvents()) {
        I.say("Giving birth to new "+actor.species.name+" at: "+at);
      }
      
      if (at instanceof Property) {
        final Property nest = (Property) at;
        young.mind.setHome(nest);
        young.goAboard(nest, world);
      }
    }
    return true;
  }
  
  
  
  /**  Some physical modifications-
    */
  //  TODO:  Move height and radius calculations here...
  
  
  
  
  /**  Special techniques-
    */
  final static Class BASE_CLASS = Fauna.class;
  final static String
    UI_DIR = "media/GUI/Powers/",
    FX_DIR = "media/SFX/";
  
  //  Okay.  I would like to have FX for-
  //    Withdraw.  (reverse-planefx, slams shut)
  //    Maul/Devour.  (penetration-fx.)
  //    Slam.         (circular burst.)
  //    Infection.    (noxious haze.)
  
  final static PlaneFX.Model
    WITHDRAW_FX = PlaneFX.imageModel(
      "fauna_withdraw_fx", BASE_CLASS,
      FX_DIR+"withdraw_imp.png",
      1, 0, -0.33f, true, false
    ),
    MAUL_FX = PlaneFX.imageModel(
      "fauna_maul_fx", BASE_CLASS,
      FX_DIR+"penetrating_shot.png",
      0.5f, 0, 0.15f, true, false
    ),
    SLAM_FX = PlaneFX.imageModel(
      "fauna_slam_fx", BASE_CLASS,
      FX_DIR+"slam_burst.png",
      1, 0.05f, 0.2f, false, false
    ),
    INFECTION_BURST_FX = PlaneFX.imageModel(
      "infection_burst_fx", BASE_CLASS,
      FX_DIR+"infection_burst.png",
      0.5f, 0.15f, 0.33f, true, false
    ),
    INFECTION_HAZE_FX = PlaneFX.animatedModel(
      "infection_haze_fx", BASE_CLASS,
      FX_DIR+"infection_haze.png",
      2, 2, 4, 1.0f, 0.25f
    );
  
  
  final static int
    WITHDRAW_MELEE_BONUS     = 15,
    WITHDRAW_RANGED_BONUS    = 10,
    WITHDRAW_BONUS_DURATION  = Stage.STANDARD_HOUR_LENGTH / 5,
    BASK_CALORIE_PERCENT     = 50,
    BASK_HEALTH_PERCENT      = 20,
    FLIGHT_EVADE_BONUS       =  5,
    FORTIFY_REPAIR           =  2,
    FORTIFY_HP_PERCENT_EXTRA = 50,
    FORTIFY_ARMOUR_EXTRA     =  5,
    SLAM_DAMAGE_MIN          =  2,
    SLAM_DAMAGE_MAX          =  8,
    SLAM_RADIUS              =  2,
    MAUL_DAMAGE_MAX          =  3,
    MAUL_DEBUFF_PENALTY      = -5,
    MAUL_DEBUFF_DURATION     = Stage.STANDARD_HOUR_LENGTH / 5,
    DEFAULT_INFECTION_RADIUS =  1,
    DIGEST_DURATION          = Stage.STANDARD_SHIFT_LENGTH,
    DIGEST_REGEN_PERCENT     = 20;
  
  
  final public static Technique WITHDRAW = new Technique(
    "Withdraw", UI_DIR+"withdraw.png",
    "Allows this creature to retreat into a protective stance when "+
    "threatened, drastically reducing damage from outside attack.",
    BASE_CLASS, "fauna_withdraw",
    MEDIUM_POWER        ,
    MILD_HELP           ,
    NO_FATIGUE          ,
    MINOR_CONCENTRATION ,
    IS_SELF_TARGETING | IS_PASSIVE_SKILL_FX | IS_NATURAL_ONLY, null, 0,
    Action.MOVE_SNEAK, Action.NORMAL
  ) {
    
    public boolean triggersAction(Actor actor, Plan current, Target subject) {
      if (current instanceof Combat) return false;
      if (actor.traits.hasTrait(asCondition)) return false;
      return actor.senses.underAttack();
    }
    
    
    public boolean triggersPassive(
      Actor actor, Plan current, Skill used, Target subject, boolean reactive
    ) {
      //
      //  This is supposed to be used only in a defensive capacity-
      if (used != HAND_TO_HAND && used != STEALTH_AND_COVER) return false;
      if (current instanceof Combat) return false;
      return actor.traits.hasTrait(asCondition);
    }
    
    
    public float passiveBonus(Actor using, Skill skill, Target subject) {
      if (skill == HAND_TO_HAND     ) return WITHDRAW_MELEE_BONUS ;
      if (skill == STEALTH_AND_COVER) return WITHDRAW_RANGED_BONUS;
      return 0;
    }
    
    
    public void applyEffect(
      Actor using, Target subject, boolean success, boolean passive
    ) {
      super.applyEffect(using, subject, success, passive);
      ActionFX.applyBurstFX(WITHDRAW_FX, using, 0.5f, 1.50f, 0.33f, 1);
      ActionFX.applyBurstFX(WITHDRAW_FX, using, 0.5f, 1.25f, 0.66f, 1);
      //
      //  TODO:  You need a Volley class to do this properly.
      SenseUtils.breaksPursuit(using, using.currentAction());
      using.traits.setLevel(asCondition, 1);
    }
    
    
    protected float conditionDuration() {
      return WITHDRAW_BONUS_DURATION;
    }
  };
  
  
  final public static Technique BASK = new Technique(
    "Bask", UI_DIR+"bask.png",
    "Grants extra calories and faster health regeneration while resting "+
    "or outdoors by day.",
    BASE_CLASS, "fauna_bask",
    MINOR_POWER         ,
    MILD_HELP           ,
    NO_FATIGUE          ,
    NO_CONCENTRATION    ,
    IS_PASSIVE_ALWAYS | IS_NATURAL_ONLY, null, 0, null
  ) {
    public void applyEffect(
      Actor using, Target subject, boolean success, boolean passive
    ) {
      super.applyEffect(using, subject, success, passive);
      
      float dayVal = (Planet.dayValue(using.world()) - 0.25f) / 0.75f;
      boolean canBask = using.health.asleep() || ! using.indoors();
      if (canBask && dayVal > 0) {
        
        float foodVal = using.health.maxHealth();
        foodVal *= dayVal * BASK_CALORIE_PERCENT / 100f;
        foodVal *= 2f / ActorHealth.STARVE_INTERVAL;
        using.health.takeCalories(foodVal, 1);
        
        float healthVal = using.health.maxHealth();
        healthVal *= dayVal * BASK_HEALTH_PERCENT / 100f;
        healthVal *= 2f / Stage.STANDARD_DAY_LENGTH;
        using.health.liftInjury(healthVal);
        
        using.traits.setLevel(asCondition, 1);
      }
      else using.traits.remove(asCondition);
    }
  };
  
  
  final public static Technique FLIGHT = new Technique(
    "Flight", UI_DIR+"flight.png",
    "Grants a higher chance of evading enemy attacks when retreating.",
    BASE_CLASS, "fauna_flight",
    MINOR_POWER         ,
    HARM_UNRATED        ,
    NO_FATIGUE          ,
    MINOR_CONCENTRATION ,
    IS_PASSIVE_SKILL_FX | IS_NATURAL_ONLY, null, 0, null
  ) {
    
    public boolean triggersPassive(
      Actor actor, Plan current, Skill used, Target subject, boolean reactive
    ) {
      if (! actor.isMoving()) return false;
      if (! (current instanceof Retreat)) return used == STEALTH_AND_COVER;
      return used == HAND_TO_HAND || used == STEALTH_AND_COVER;
    }
    
    
    public float passiveBonus(Actor using, Skill skill, Target subject) {
      return FLIGHT_EVADE_BONUS;
    }
  };
  
  
  final public static Technique FORTIFY = new Technique(
    "Fortify", UI_DIR+"fortify.png",
    "Allows nests to be constructed more rapidly, and grants the final "+
    "product greater structural integrity.",
    BASE_CLASS, "fauna_fortify",
    MINOR_POWER         ,
    MILD_HELP           ,
    NO_FATIGUE          ,
    MEDIUM_CONCENTRATION,
    IS_PASSIVE_SKILL_FX | IS_NATURAL_ONLY, null, 0, null
  ) {
    
    public boolean triggersPassive(
      Actor actor, Plan current, Skill used, Target subject, boolean reactive
    ) {
      if (! (current instanceof Repairs)) return false;
      if (! (subject instanceof Nest   )) return false;
      return used == HANDICRAFTS;
    }
    
    
    public void applyEffect(
      Actor using, Target subject, boolean success, boolean passive
    ) {
      super.applyEffect(using, subject, success, passive);
      if (! success) return;
      
      final Nest nest = (Nest) subject;
      final Blueprint b = nest.blueprint;
      nest.structure().repairBy(FORTIFY_REPAIR);
      
      nest.structure.adjustStats(
        (int) (b.integrity * (1 + (FORTIFY_HP_PERCENT_EXTRA / 100f))),
        b.armour + FORTIFY_ARMOUR_EXTRA,
        0, 0, 1, b.properties
      );
    }
  };
  
  
  final public static Technique SLAM = new Technique(
    "Slam", UI_DIR+"slam.png",
    "Deals extra damage in melee while stunning nearby opponents.",
    BASE_CLASS, "fauna_slam",
    MAJOR_POWER         ,
    REAL_HARM           ,
    MINOR_FATIGUE       ,
    MEDIUM_CONCENTRATION,
    IS_FOCUS_TARGETING | IS_NATURAL_ONLY, null, 0,
    Action.STRIKE_BIG, Action.QUICK
  ) {
  
    public boolean triggersAction(Actor actor, Plan current, Target subject) {
      if (! (current instanceof Combat)) return false;
      if (! (subject instanceof Actor )) return false;
      return actor.gear.meleeDeviceOnly();
    }
    
    
    protected float effectRadius() {
      return SLAM_RADIUS;
    }
    
    
    protected boolean effectDescriminates() {
      return true;
    }
    
    
    protected void applySelfEffects(Actor using) {
      super.applySelfEffects(using);
      
      final Stage world = using.world();
      final Vec3D point = using.actionFocus().position(null);
      point.add(using.position(null).scale(0.5f)).scale(0.66f);
      final float s = SLAM_RADIUS / 1.2f;
      ActionFX.applyBurstFX(SLAM_FX, point, 1, s * 1.2f, 0.2f, world);
      ActionFX.applyBurstFX(SLAM_FX, point, 1, s * 1.0f, 0.6f, world);
      ActionFX.applyBurstFX(SLAM_FX, point, 1, s * 0.8f, 1.0f, world);
    }
    
    
    public void applyEffect(
      Actor using, Target subject, boolean success, boolean passive
    ) {
      super.applyEffect(using, subject, success, passive);
      final Actor struck = (Actor) subject;
      
      float damage = roll(SLAM_DAMAGE_MIN, SLAM_DAMAGE_MAX);
      struck.health.takeInjury(damage, false);
      struck.forceReflex(Action.FALL, true);
    }
  };
  
  
  final public static Technique MAUL = new Technique(
    "Maul", UI_DIR+"maul.png",
    "Deals extra damage in melee while hindering an opponent's ability to "+
    "strike back.  Can cause bleeding.",
    BASE_CLASS, "fauna_maul",
    MEDIUM_POWER        ,
    REAL_HARM           ,
    MINOR_FATIGUE       ,
    MEDIUM_CONCENTRATION,
    IS_FOCUS_TARGETING | IS_NATURAL_ONLY, null, 0,
    Action.STRIKE_BIG, Action.QUICK
  ) {
    
    public boolean triggersAction(Actor actor, Plan current, Target subject) {
      if (! (current instanceof Combat)) return false;
      if (! (subject instanceof Actor )) return false;
      return actor.gear.meleeDeviceOnly();
    }
    
    
    protected boolean checkActionSuccess(Actor actor, Target subject) {
      if (! Combat.performGeneralStrike(
        actor, subject, Combat.OBJECT_EITHER, actor.currentAction()
      )) return false;
      return super.checkActionSuccess(actor, subject);
    }
    
    
    public void applyEffect(
      Actor using, Target subject, boolean success, boolean passive
    ) {
      super.applyEffect(using, subject, success, passive);
      
      if (success) {
        ActionFX.applyBurstFX(MAUL_FX, subject, 0.5f, 1);
        
        final Actor struck = (Actor) subject;
        struck.health.takeInjury(roll(0, MAUL_DAMAGE_MAX), false);
        struck.traits.incBonus(HAND_TO_HAND, MAUL_DEBUFF_PENALTY);
        struck.traits.incBonus(MARKSMANSHIP, MAUL_DEBUFF_PENALTY);
        
        if (struck.health.organic() && Rand.yes()) {
          struck.health.setBleeding(true);
          struck.traits.setLevel(asCondition, 1);
        }
      }
    }
    
    
    protected float conditionDuration() {
      return MAUL_DEBUFF_DURATION;
    }


    protected void applyAsCondition(Actor affected) {
      super.applyAsCondition(affected);
      affected.traits.incBonus(HAND_TO_HAND, MAUL_DEBUFF_PENALTY / 2f);
      affected.traits.incBonus(MARKSMANSHIP, MAUL_DEBUFF_PENALTY / 2f);
      if (! affected.health.bleeding()) affected.traits.remove(asCondition);
    }
  };
  
  
  final public static Technique INFECTION = new Technique(
    "Infection", UI_DIR+"infection.png",
    "Anyone in contact with or near this creature runs a risk of "+
    "contracting disease.",
    BASE_CLASS, "fauna_infection",
    MINOR_POWER         ,
    MILD_HARM           ,
    NO_FATIGUE          ,
    NO_CONCENTRATION    ,
    IS_PASSIVE_ALWAYS | IS_NATURAL_ONLY, null, 0, null
  ) {
    public void applyEffect(
      Actor using, Target subject, boolean success, boolean passive
    ) {
      super.applyEffect(using, subject, success, passive);
      
      if (true || Rand.index(4) == 0) {
        ActionFX.applyBurstFX(INFECTION_HAZE_FX, using, 1, 1, 0.6f, 1);
      }
      
      float radius = DEFAULT_INFECTION_RADIUS * (using.health.baseBulk() + 1);
      for (Actor a : PlanUtils.subjectsInRange(using, radius)) {
        final int period = Stage.STANDARD_HOUR_LENGTH;
        float infectChance = 0.5f;
        if (a.actionFocus() == using) infectChance *= 2;
        
        if (Condition.checkContagion(
          a, infectChance, period,
          Condition.ILLNESS, Condition.HIREX_PARASITE
        )) {
          ActionFX.applyBurstFX(INFECTION_BURST_FX, a, 0.5f, 1);
        }
      }
    }
  };
  
  
  final public static Technique NIGHT_VISION = new Technique(
    "Night Vision", UI_DIR+"night_vision.png",
    "Grants this creature extended sight range by night, but poorer vision "+
    "by day.",
    BASE_CLASS, "fauna_night_vision",
    MEDIUM_POWER        ,
    HARM_UNRATED        ,
    NO_FATIGUE          ,
    NO_CONCENTRATION    ,
    IS_PASSIVE_ALWAYS | IS_NATURAL_ONLY, null, 0, null
  ) {
    public void applyEffect(
      Actor using, Target subject, boolean success, boolean passive
    ) {
      super.applyEffect(using, subject, success, passive);
      final float dayVal = Planet.dayValue(using.world());
      float bonus = (0.75f - dayVal) * 20;
      using.traits.incBonus(SURVEILLANCE, bonus);
    }
  };
  
  
  
  final static Traded ITEM_DIGESTING = new Traded(
    BASE_CLASS, "Digesting", null, Economy.FORM_SPECIAL, 0,
    "Some unfortunate creature is being digested..."
  ) {
    public void describeFor(Actor owns, Item i, Description d) {
      d.appendAll("Digesting ", i.refers);
    }
  };
  
  
  private boolean hasDevoured(Actor a) {
    return gear.matchFor(Item.withReference(ITEM_DIGESTING, a)) != null;
  }
  
  
  final public static Technique DEVOUR = new Technique(
    "Devour", UI_DIR+"devour.png",
    "Allows this creature to consume and digest a chosen victim.",
    BASE_CLASS, "fauna_devour",
    MEDIUM_POWER        ,
    EXTREME_HARM        ,
    MEDIUM_FATIGUE      ,
    MEDIUM_CONCENTRATION,
    IS_FOCUS_TARGETING | IS_PASSIVE_ALWAYS | IS_NATURAL_ONLY, null, 0,
    Action.STRIKE_BIG, Action.QUICK
  ) {
    
    public boolean triggersAction(
      Actor actor, Plan current, Target subject
    ) {
      if (! (subject instanceof Actor )) return false;
      if (! (current instanceof Combat)) return false;
      if (actor.gear.amountOf(ITEM_DIGESTING) > 0) return false;
      
      final Actor victim = (Actor) subject;
      if (victim.health.baseBulk() > actor.health.baseBulk() / 2) {
        return false;
      }
      return true;
    }
    
    
    protected boolean checkActionSuccess(Actor actor, Target subject) {
      final Actor victim = (Actor) subject;
      
      final float maxBulk = actor.health.baseBulk() / 2;
      final float chance = 1f - (victim.health.baseBulk() / maxBulk);
      if (Rand.num() > chance) return false;
      
      return Combat.performStrike(
        actor, (Actor) subject,
        HAND_TO_HAND, HAND_TO_HAND,
        Combat.OBJECT_DESTROY, actor.currentAction()
      );
    }
    
    
    public void applyEffect(
      Actor using, Target subject, boolean success, boolean passive
    ) {
      if (passive) { updateDigestion(using); return; }
      
      if (success) {
        super.applyEffect(using, subject, success, passive);
        ActionFX.applyBurstFX(MAUL_FX, subject, 0.5f, 1);
        
        final Stage world = using.world();
        final Actor victim = (Actor) subject;
        final Item digestion = Item.withReference(ITEM_DIGESTING, victim);
        world.ephemera.addGhost(victim, 1, victim.sprite(), 1, 1);
        victim.exitToOffworld();
        victim.health.setState(ActorHealth.STATE_RESTING);
        using.gear.addItem(digestion);
        if (using instanceof Mount) victim.bindToMount((Mount) using);
      }
    }
    
    
    private void updateDigestion(Actor using) {
      final Item digestion = using.gear.matches(ITEM_DIGESTING).first();
      if (digestion == null) return;
      
      final Actor victim = (Actor) digestion.refers;
      final float burn = victim.health.maxHealth() / DIGEST_DURATION;
      victim.health.takeInjury(burn, true);
      using.health.takeCalories(burn, 1);
      using.health.liftInjury(burn * DIGEST_REGEN_PERCENT / 100f);
      
      final float amountGone = 1f / DIGEST_DURATION;
      using.gear.removeItem(Item.withAmount(digestion, amountGone));
      
      if (! using.health.alive()) {
        using.gear.removeItem(digestion);
        victim.releaseFromMount();
        victim.enterWorldAt(using.origin(), using.world());
        victim.health.setBleeding(true);
      }
    }
  };
  
  
  
  /**  Implementing mounted and domestic behaviours-
    */
  public void setAsDomesticated(Actor follows) {
    relations.assignMaster(follows);
    assignBase(follows.base());
    mind.setHome(follows.mind.work());
  }
  
  
  public void setAsFeral() {
    if (! domesticated()) return;
    relations.clearMaster();
    assignBase(Base.wildlife(world));
    mind.setHome(null);
  }
  
  
  public boolean domesticated() {
    return relations.master() != null;
  }
  
  
  //
  //  TODO:  Move this (and related behaviours) to MountUtils.
  
  //*
  protected void addDomesticBehaviours(Choice choice) {
    final Actor follows = relations.master();
    if (follows == null) return;
    
    final float loyalty = relations.valueFor(follows);
    if (loyalty <= 0) {
      setAsFeral();
      return;
    }
    
    final Behaviour current = follows.mind.rootBehaviour();
    final float priority = loyalty * Plan.ROUTINE;
    
    if (current == null || current instanceof Resting) {
      choice.add(new Resting(this, follows.aboard()));
    }
    if (current instanceof Combat && senses.awareOf(current.subject())) {
      final Plan c = ((Combat) current).copyFor(this);
      choice.add(c.addMotives(Plan.NO_PROPERTIES, priority * 0.5f));
    }
    if (
      (current instanceof Exploring ) ||
      (current instanceof Hunting   ) ||
      (current instanceof Gathering ) ||
      (current instanceof Retreat   ) ||
      (current instanceof Patrolling) ||
      (current instanceof Combat    )
    ) {
      if (riding == follows) {
        Boarding dest = PathSearch.accessLocation(riding.pathing.target(), this);
        if (dest != null) {
          final BringPerson b = new BringPerson(this, riding, dest);
          b.addMotives(Plan.NO_PROPERTIES, priority * 1.5f);
          choice.add(b);
        }
      }
      else choice.add(Patrolling.protectionFor(this, follows, priority));
    }
  }
  //*/
  
  
  public boolean setMounted(Actor mounted, boolean is) {
    if (mounted == riding) return true;
    if (riding != null) riding.releaseFromMount();
    riding = is ? mounted : null;
    return true;
  }
  
  
  public boolean allowsActivity(Plan activity) {
    return true;
  }
  
  
  public Property mountStoresAt() {
    return mind.home();
  }
  
  
  public boolean actorVisible(Actor mounted) {
    return true;
  }
  
  
  public void configureSpriteFrom(
    Actor mounted, Action a, Sprite sprite, Rendering rendering
  ) {
    if (a != null) a.configSprite(sprite, rendering);
    //
    //  TODO:  YOU NEED TO USE ATTACH-POINTS HERE!
    viewPosition(sprite.position);
    sprite.position.z += height() - (mounted.height() / 2f);
    sprite.rotation = rotation();
    return;
  }
  
  
  public void describeActor(Actor mounted, Description d) {
    if (mounted == riding) {
      d.appendAll("Being carried by ", this);
    }
    else if (hasDevoured(mounted)) {
      d.appendAll("Being digested by ", this);
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected float spriteScale() {
    return (float) Nums.sqrt(health.ageLevel() + 0.5f);
  }
  
  
  public String fullName() {
    return health.agingDesc()+" "+species.name;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(species.portrait, species.name);
  }
  
  
  public SelectionPane configSelectPane(SelectionPane panel, BaseUI UI) {
    final SelectionPane p = ActorDescription.configSimplePanel(this, panel, UI);
    //final int BP = (int) (breedMetre * 100);
    //p.detail().append("\n  Breeding condition: "+BP+"%");
    return p;
  }
}






/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.civic.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;




public class Hunting extends Combat {
  
  /**  Fields, constructors, and save/load methods-
    */
  private static boolean
    evalVerbose  = false,
    stepsVerbose = false;
  
  final public static int
    TYPE_FEEDS   = 0,
    TYPE_HARVEST = 1,
    TYPE_SAMPLE  = 2;
  final static String TYPE_DESC[] = {
    "Feeding", "Harvest", "Sampling"
  };
  final static int
    STAGE_INIT          = 0,
    STAGE_HUNT          = 1,
    STAGE_FEED          = 2,
    STAGE_HARVEST_MEAT  = 3,
    STAGE_RETURN_MEAT   = 4,
    STAGE_SAMPLE_GENE   = 5,
    STAGE_RETURN_SAMPLE = 6,
    STAGE_COMPLETE      = 7;
  
  
  final int type;
  final Actor prey;
  final Owner depot;
  private int stage = STAGE_INIT;
  private float beginTime = -1;
  
  
  
  public static Hunting asFeeding(Actor actor, Actor prey) {
    return new Hunting(actor, prey, TYPE_FEEDS, null);
  }
  
  
  public static Hunting asHarvest(
    Actor actor, Actor prey, Owner depot, boolean hungry
  ) {
    if (depot == null) return null;
    final float hunger = actor.health.hungerLevel() * Plan.PARAMOUNT;
    final Hunting h = new Hunting(actor, prey, TYPE_HARVEST, depot);
    return (Hunting) h.addMotives(Plan.MOTIVE_EMERGENCY, hunger);
  }
  
  
  public static Hunting asSample(Actor actor, Actor prey, Owner depot) {
    if (depot == null) I.complain("NO DEPOT SPECIFIED!");
    return new Hunting(actor, prey, TYPE_SAMPLE, depot);
  }
  
  
  
  private Hunting(Actor actor, Actor prey, int type, Owner depot) {
    super(
      actor, prey, STYLE_EITHER,
      (type == TYPE_SAMPLE) ? OBJECT_SUBDUE : OBJECT_EITHER
    );
    this.prey = prey;
    this.type = type;
    this.depot = depot;
  }
  
  
  public Hunting(Session s) throws Exception {
    super(s);
    prey = (Actor) s.loadObject();
    type = s.loadInt();
    depot = (Owner) s.loadObject();
    stage = s.loadInt();
    beginTime = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(prey);
    s.saveInt(type);
    s.saveObject(depot);
    s.saveInt(stage);
    s.saveFloat(beginTime);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Hunting(other, prey, type, depot);
  }
  
  
  
  /**  Evaluating targets and priority-
    */
  public static boolean validPrey(Target prey, Actor hunts) {
    if (! (prey instanceof Actor)) return false;
    
    final Actor a = (Actor) prey;
    if (a.indoors() || ! a.health.organic()) return false;
    final float hunger = hunts.health.hungerLevel();
    
    if (a.species() == hunts.species()) {
      if (hunger < 1) return false;
    }
    if (! a.species().browser()) {
      if (hunger < 0.5f) return false;
    }
    
    //final float crowding = a.health.alive() ? Nest.crowdingFor(a) : 1;
    //if (conserve && (crowding < 1 - hunger)) return false;
    return true;
  }
  
  
  final Trait
    HARVEST_TRAITS[] = { CRUEL, FEARLESS, ENERGETIC },
    SAMPLE_TRAITS[]  = { CURIOUS, ENERGETIC, NATURALIST };
  
  
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    
    if (prey.destroyed()) return -1;
    final boolean start = ! hasBegun();
    if (start && ! validPrey(prey, actor)) return -1;
    if (! PlanUtils.isArmed(actor)) return -1;
    
    float urgency, harmLevel;
    final Trait baseTraits[];
    
    final float hunger = actor.health.hungerLevel() + (start ? 0 : 0.5f);
    float crowdRating = 1;
    if (prey.health.alive()) crowdRating = Nest.crowdingFor(prey);
    crowdRating = Nums.clamp((crowdRating - 0.5f) * 2, -1, 1);
    
    if (type == TYPE_FEEDS) {
      urgency    = hunger * PARAMOUNT * (1 + Nums.min(crowdRating, hunger));
      harmLevel  = REAL_HARM;
      baseTraits = Combat.BASE_TRAITS;
    }
    else if (type == TYPE_HARVEST) {
      urgency    = Nums.clamp(ROUTINE * crowdRating, CASUAL, URGENT);
      urgency   += hunger * ROUTINE;
      harmLevel  = REAL_HARM;
      baseTraits = HARVEST_TRAITS;
    }
    else {
      urgency    = ROUTINE;
      harmLevel  = NO_HARM;
      baseTraits = SAMPLE_TRAITS;
    }
    
    final float priority = priorityForActorWith(
      actor, prey,
      urgency, urgency / 2,
      harmLevel, MILD_COMPETITION, REAL_FAIL_RISK,
      RANGED_SKILLS, baseTraits, NORMAL_DISTANCE_CHECK,
      false
    );
    
    if (report) {
      I.say("\nHunting type is: "+TYPE_DESC[type]);
      I.say("  Just started:    "+start   +" ("+hashCode()+")");
      I.say("  Base urgency is: "+urgency );
      I.say("  Final priority:  "+priority);
      I.say("  Hunger is:       "+hunger  +" ("+actor.health.hungerLevel()+")");
      I.say("  Crowd-rating is: "+crowdRating);
    }
    return priority;
  }
  
  
  public boolean valid() {
    if (actor == null) return super.valid();
    if (type == TYPE_SAMPLE) {
      if (actor.gear.hasItem(sample())) return true;
    }
    return super.valid();
  }
  
  
  
  /**  Actual implementation-
    */
  protected Behaviour getNextStep() {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) {
      I.say("Getting next hunting step "+type+" "+actor+": "+prey);
      I.say("  Begun? "+hasBegun()+" "+this.hashCode());
    }
    
    if (type == TYPE_FEEDS) {
      if (! prey.inWorld()) return null;
      if (prey.health.conscious()) return super.getNextStep();
      if (report) I.say("  Feeding.");
      final Action feed = new Action(
        actor, prey,
        this, "actionFeed",
        Action.STRIKE, "Feeding"
      );
      return feed;
    }
    
    if (type == TYPE_HARVEST) {
      if (! CombatUtils.isDowned(prey, object)) {
        return super.getNextStep();
      }
      else if (prey.aboard() == depot) {
        if (report) I.say("  Harvesting");
        final Action harvest = new Action(
          actor, prey,
          this, "actionHarvest",
          Action.BUILD, "Harvesting"
        );
        return harvest;
      }
      else {
        return Suspensor.deliveryTask(actor, prey, depot);
      }
    }
    
    if (type == TYPE_SAMPLE) {
      final Item sample = sample();
      if (actor.gear.hasItem(sample)) {
        if (report) I.say("  Returning sample.");
        final Action returns = new Action(
          actor, depot,
          this, "actionReturnSample",
          Action.REACH_DOWN, "Returning sample"
        );
        return returns;
      }
      if (depot.inventory().hasItem(sample)) return null;
      if (! CombatUtils.isDowned(prey, object)) return super.getNextStep();
      else {
        if (report) I.say("  Sampling.");
        final Action samples = new Action(
          actor, prey,
          this, "actionSample",
          Action.BUILD, "Sampling"
        );
        return samples;
      }
    }
    
    return null;
  }
  
  
  public boolean actionFeed(Actor actor, Actor prey) {
    //
    //  Determine just how large a chunk you can take out of the prey-
    final float
      before = prey.health.injuryLevel(),
      damage = actor.gear.attackDamage() * (Rand.num() + 0.5f) / 10;
    prey.health.takeInjury(damage, true);
    float taken = prey.health.injuryLevel() - before;
    taken *= prey.health.maxHealth();
    //
    //  Then dispose of it appropriately-
    if (! prey.health.dying()) prey.health.setState(ActorHealth.STATE_DYING);
    actor.health.takeCalories(taken * Fauna.MEAT_CONVERSION, 1);
    return true;
  }
  
  
  public boolean actionHarvest(Actor actor, Actor prey) {
    //
    //  Firstly, use a series of basic skill checks to see how effectively the
    //  carcass can be butchered:
    float success = 1, mult = 1;
    if (actor.skills.test(XENOZOOLOGY, 10, 2)) success++;
    if (actor.skills.test(DOMESTICS  , 5 , 1)) success++;
    if (actor.skills.test(HARD_LABOUR, 5 , 1)) success++;
    success /= 5;
    //
    //  We provide a bonus to extraction effiency based on upgrades available
    //  at the kommando lodge-
    if (depot instanceof KommandoLodge) {
      final KommandoLodge still = (KommandoLodge) depot;
      float bonus = still.structure.upgradeLevel(KommandoLodge.FLESH_STILL);
      mult += bonus / 2f;
    }
    //
    //  Then we measure the physical damage done, and decrement prey health.
    final float
      before = prey.health.injuryLevel(),
      damage = success * 2;
    if (prey.health.alive()) prey.health.setState(ActorHealth.STATE_DYING);
    prey.health.takeInjury(damage, true);
    //
    //  Based on the damage, we extract meat and spyce from the carcass-
    final float
      taken = Nums.max(0, prey.health.injuryLevel() - before),
      meat  = taken *  mult      * prey.health.maxHealth() / 2 ,
      spyce = taken * (mult - 1) * prey.health.maxHealth() / 20;
    if (taken <= 0) return false;
    //
    //  Store those in the depot, and return-
    if (I.talkAbout == actor) {
      I.say("Injury before/after: "+before+"/"+prey.health.injuryLevel());
      I.say("Meat amount:  "+meat );
      I.say("Spyce amount: "+spyce);
    }
    depot.inventory().bumpItem(PROTEIN, meat );
    depot.inventory().bumpItem(SPYCE_T, spyce);
    return true;
  }
  
  
  public Item sample() {
    if (type != TYPE_SAMPLE) I.complain("Not a sampling hunt!");
    return Item.withReference(GENE_SEED, prey.species());
  }
  
  
  public boolean actionSample(Actor actor, Actor prey) {
    if (! actor.skills.test(XENOZOOLOGY, 10, 10)) return false;
    actor.gear.addItem(sample());
    return true;
  }
  
  
  public boolean actionReturnSample(Actor actor, Owner depot) {
    actor.gear.transfer(sample(), depot);
    return true;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    final boolean dead = ! prey.health.alive();
    
    if (type == TYPE_SAMPLE) {
      final Item sample = sample();
      if (actor.gear.hasItem(sample)) {
        d.append("Returning "+sample+" to ");
        d.append(depot);
      }
      else {
        d.append("Obtaining samples from ");
        d.append(prey);
      }
    }
    
    if (type == TYPE_FEEDS) {
      if (dead) d.append("Scavenging meat from ");
      else d.append("Hunting ");
      d.append(prey);
    }
    
    if (type == TYPE_HARVEST) {
      if (! dead) {
        d.append("Culling ");
        d.append(prey);
      }
      else if (! prey.destroyed()) {
        d.append("Harvesting meat from ");
        d.append(prey);
      }
      else {
        d.append("Returning meat to ");
        d.append(depot);
      }
    }
  }
}



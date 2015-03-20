/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.economic.Inventory.Owner;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;




public class Hunting extends Combat {
  
  /**  Fields, constructors, and save/load methods-
    */
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
  
  private static boolean verbose = false, evalVerbose = false;
  
  
  final int type;
  final Actor prey;
  final Owner depot;
  private int stage = STAGE_INIT;
  private float beginTime = -1;
  
  
  
  //  TODO:  Get rid of this type- just specialise 'harvesting'.
  public static Hunting asFeeding(Actor actor, Actor prey) {
    return new Hunting(actor, prey, TYPE_FEEDS, null);
  }
  
  
  public static Hunting asHarvest(
    Actor actor, Actor prey, Owner depot, boolean hungry
  ) {
    if (depot == null) return null;
    final float hunger = actor.health.hungerLevel() * Plan.PARAMOUNT;
    final Hunting h = new Hunting(actor, prey, TYPE_HARVEST, depot);
    return (Hunting) h.setMotive(Plan.MOTIVE_EMERGENCY, hunger);
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
  public static boolean validPrey(Target prey, Actor hunts, boolean conserve) {
    if (! (prey instanceof Actor)) return false;
    final Actor a = (Actor) prey;
    if (! a.health.organic()) return false;
    final boolean starved = hunts.health.hungerLevel() >= 1;
    
    if (a.species() == hunts.species() || ! (prey instanceof Fauna)) {
      if (! starved) return false;
    }
    if (conserve && (Nest.crowdingFor(a) < 1) && ! starved) return false;
    return true;
  }
  
  
  final Trait
    HARVEST_TRAITS[] = { CRUEL, FEARLESS, ENERGETIC },
    SAMPLE_TRAITS[]  = { CURIOUS, ENERGETIC, NATURALIST };
  
  
  public float priorityFor(Actor actor) {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (! validPrey(prey, actor, false)) return -1;
    if (! PlanUtils.isArmed(actor)) return 0;
    
    float urgency, harmLevel;
    final Trait baseTraits[];
    final float crowding = Nest.crowdingFor(prey);
    final float hunger = actor.health.hungerLevel();
    
    if (type == TYPE_FEEDS) {
      urgency    = hunger * PARAMOUNT * (1 + Nums.min(crowding, hunger));
      harmLevel  = REAL_HARM;
      baseTraits = Combat.BASE_TRAITS;
    }
    else if (type == TYPE_HARVEST) {
      if (crowding < 1) return 0;
      urgency    = Nums.clamp(ROUTINE * crowding, CASUAL, URGENT);
      harmLevel  = REAL_HARM;
      baseTraits = HARVEST_TRAITS;
    }
    else {
      urgency    = ROUTINE;
      harmLevel  = NO_HARM;
      baseTraits = SAMPLE_TRAITS;
    }
    
    final float priority = priorityForActorWith(
      actor, prey, urgency,
      NO_MODIFIER, harmLevel,
      MILD_COMPETITION, REAL_FAIL_RISK,
      RANGED_SKILLS, baseTraits, NORMAL_DISTANCE_CHECK,
      report
    );
    
    if (report) {
      I.say("\nHunting type is: "+TYPE_DESC[type]);
      I.say("  Base urgency is: "+urgency);
      I.say("  Final priority: "+priority);
    }
    return priority;
  }
  
  
  public boolean valid() {
    if (actor == null) return super.valid();
    if (type == TYPE_HARVEST) {
      if (actor.gear.amountOf(PROTEIN) > 0) return true;
    }
    if (type == TYPE_SAMPLE) {
      if (actor.gear.hasItem(sample())) return true;
    }
    return super.valid();
  }
  
  
  
  /**  Actual implementation-
    */
  protected Behaviour getNextStep() {
    final boolean report = verbose && I.talkAbout == actor && hasBegun();
    if (report) I.say("Getting next hunting step "+type+" "+hashCode());
    
    if (type == TYPE_FEEDS) {
      if (! prey.inWorld()) return null;
      if (prey.health.conscious()) return super.getNextStep();
      
      final float full = ActorHealth.MAX_CALORIES * 0.99f;
      if (actor.health.caloryLevel() >= full) return null;
      final Action feed = new Action(
        actor, prey,
        this, "actionFeed",
        Action.STRIKE, "Feeding"
      );
      return feed;
    }
    
    if (type == TYPE_HARVEST) {
      final float carried = actor.gear.amountOf(PROTEIN);
      if (carried == 0 && ! prey.inWorld()) return null;
      if (! CombatUtils.isDowned(prey, object)) return super.getNextStep();
      
      if (carried >= 5 || (prey.destroyed() && carried > 0)) {
        if (depot == null) return null;
        final Action returns = new Action(
          actor, depot,
          this, "actionReturnHarvest",
          Action.REACH_DOWN, "Returning harvest"
        );
        return returns;
      }
      else {
        if (report) I.say("Returning harvest action...");
        final Action harvest = new Action(
          actor, prey,
          this, "actionHarvest",
          Action.BUILD, "Harvesting"
        );
        return harvest;
      }
    }
    
    if (type == TYPE_SAMPLE) {
      final Item sample = sample();
      if (actor.gear.hasItem(sample)) {
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
  
  
  public Item sample() {
    if (type != TYPE_SAMPLE) I.complain("Not a sampling hunt!");
    return Item.withReference(GENE_SEED, prey.species());
  }
  
  
  public boolean actionHarvest(Actor actor, Actor prey) {
    float success = 1;
    if (actor.skills.test(XENOZOOLOGY, 5, 10)) success++;
    if (actor.skills.test(DOMESTICS, 5, 5)) success++;
    if (actor.skills.test(HARD_LABOUR, 5, 5)) success++;
    
    final float
      before = prey.health.injuryLevel(),
      damage = success * 2;
    prey.health.takeInjury(damage, true);
    final float taken = prey.health.injuryLevel() - before;
    
    if (! prey.health.dying()) prey.health.setState(ActorHealth.STATE_DYING);
    actor.gear.bumpItem(PROTEIN, taken * prey.health.maxHealth());
    return true;
  }
  
  
  public boolean actionReturnHarvest(Actor actor, Owner depot) {
    actor.gear.transfer(PROTEIN, depot);
    return true;
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



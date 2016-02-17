/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.content.civic.*;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



public class Hunting extends Plan {
  
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
  private Combat downing = null;
  
  
  
  public static Hunting asFeeding(Actor actor, Actor prey) {
    return new Hunting(actor, prey, TYPE_FEEDS, null);
  }
  
  
  public static Hunting asHarvest(Actor actor, Actor prey, Owner depot) {
    if (depot == null) I.complain("NO DEPOT SPECIFIED!");
    return new Hunting(actor, prey, TYPE_HARVEST, depot);
  }
  
  
  public static Hunting asSample(Actor actor, Actor prey, Owner depot) {
    if (depot == null) I.complain("NO DEPOT SPECIFIED!");
    return new Hunting(actor, prey, TYPE_SAMPLE, depot);
  }
  
  
  
  private Hunting(Actor actor, Actor prey, int type, Owner depot) {
    super(actor, prey, MOTIVE_EMERGENCY, REAL_HARM);
    this.prey  = prey ;
    this.type  = type ;
    this.depot = depot;
  }
  
  
  public Hunting(Session s) throws Exception {
    super(s);
    prey      = (Actor) s.loadObject();
    type      = s.loadInt();
    depot     = (Owner) s.loadObject();
    stage     = s.loadInt();
    beginTime = s.loadFloat();
    downing   = (Combat) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(prey     );
    s.saveInt   (type     );
    s.saveObject(depot    );
    s.saveInt   (stage    );
    s.saveFloat (beginTime);
    s.saveObject(downing  );
  }
  
  
  public Plan copyFor(Actor other) {
    return new Hunting(other, prey, type, depot);
  }
  
  
  
  /**  Evaluating targets and priority-
    */
  public static boolean validPrey(Target prey, Actor hunts) {
    if (! (prey instanceof Actor)) return false;
    
    final Actor a = (Actor) prey;
    if (! a.health.organic()) return false;
    final boolean alive = preyIsLive(prey);
    final float hunger = hunts.health.hungerLevel();
    
    if (a.species() == hunts.species() || ! a.species().animal()) {
      if (hunger < 1) return false;
    }
    if (alive && ! a.species().preyedOn()) {
      if (hunger < 0.5f) return false;
    }
    return true;
  }
  
  
  private static boolean preyIsLive(Target prey) {
    return ! CombatUtils.isDowned(prey, Combat.OBJECT_EITHER);
  }
  
  
  final Trait SAMPLE_TRAITS[]  = { CURIOUS, ENERGETIC, NATURALIST };
  
  
  protected float getPriority() {
    final boolean report = I.talkAbout == actor && evalVerbose;
    if (report) {
      I.say("\nGetting priority for "+actor+" of hunting "+prey);
    }
    
    if (prey.destroyed() || ! prey.inWorld()) return -1;

    final boolean start = ! hasBegun(), alive = preyIsLive(prey);
    if (start && ! validPrey(prey, actor)  ) return -1;
    if (alive && ! PlanUtils.isArmed(actor)) return -1;
    
    float hunger = 0, urgency = 0, crowdRating = 0, priority = 0;
    float harmLevel = Plan.REAL_HARM;
    //
    //  If you're planning to actually kill/eat this creature, then motivation
    //  is based on current hunger (with a bonus if the hunt is already begun
    //  and/or the creature is downed, to ensure the task isn't abandoned
    //  prematurely.)
    //  If the creature is alive, we also try to favour common (i.e, crowded)
    //  species over rare ones.
    if (type == TYPE_FEEDS || type == TYPE_HARVEST) {
      
      hunger = actor.health.hungerLevel();
      if      (! alive) hunger += ActorHealth.MAX_CALORIES - 1;
      else if (start  ) hunger += 0.25f;
      
      urgency += hunger;
      urgency += motiveBonus() / Plan.PARAMOUNT;
      
      crowdRating = NestUtils.localCrowding(prey.species(), prey);
      crowdRating = Nums.clamp((crowdRating - 0.5f) * 2, 0, 1);
      
      if (alive        ) urgency *= crowdRating;
      if (hunger > 0.5f) urgency += (hunger - 0.5f) * 2;
    }
    //
    //  In the case of a sampling run, things are simpler.
    else {
      urgency += motiveBonus() / Plan.PARAMOUNT;
      urgency += PlanUtils.traitAverage(actor, SAMPLE_TRAITS);
      harmLevel = Plan.NO_HARM;
    }
    //
    //  In any case, live prey can fight back, so we adjust our willingness to
    //  undertake the task accordingly.  (Or not.)
    if (alive && urgency > 0) {
      priority = PlanUtils.combatPriority(
        actor, prey, urgency * Plan.PARAMOUNT, 1, true, harmLevel
      );
      setCompetence(PlanUtils.combatWinChance(actor, prey, 1));
    }
    else {
      priority = urgency * Plan.PARAMOUNT;
    }

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
  
  
  public float harmIntended(Target t) {
    if (t == depot) return 0;
    return super.harmIntended(t);
  }
  
  
  
  /**  Actual implementation-
    */
  protected Behaviour getNextStep() {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) {
      I.say("Getting next hunting step ("+type+") "+actor+" vs. "+prey);
    }
    //  TODO:  See if this can be tidied up a bit...
    
    if (type == TYPE_FEEDS) {
      if (preyIsLive(prey)) {
        return downingStep(Combat.OBJECT_EITHER);
      }
      else {
        if (report) I.say("  Feeding.");
        final Action feed = new Action(
          actor, prey,
          this, "actionFeed",
          Action.STRIKE, "Feeding"
        );
        return feed;
      }
    }
    
    if (type == TYPE_HARVEST) {
      if (preyIsLive(prey)) {
        return downingStep(Combat.OBJECT_EITHER);
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
      if (depot.inventory().hasItem(sample)) {
        return null;
      }
      else if (actor.gear.hasItem(sample)) {
        if (report) I.say("  Returning sample.");
        final Action returns = new Action(
          actor, depot,
          this, "actionReturnSample",
          Action.REACH_DOWN, "Returning sample"
        );
        return returns;
      }
      else if (! CombatUtils.isDowned(prey, Combat.OBJECT_SUBDUE)) {
        return downingStep(Combat.OBJECT_SUBDUE);
      }
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
  
  
  private Combat downingStep(int object) {
    if (downing == null) {
      downing = new Combat(actor, prey, Combat.STYLE_EITHER, object);
    }
    downing.setMotivesFrom(this, PARAMOUNT);
    if (! Plan.canFollow(actor, downing, true)) return null;
    return downing;
  }
  
  
  public boolean actionFeed(Actor actor, Actor prey) {
    //
    //  Determine just how large a chunk you can take out of the prey-
    final float
      before = prey.health.injury(),
      damage = actor.gear.totalDamage() * (Rand.num() + 0.5f) / 10;
    if (! prey.health.dying()) prey.health.setState(ActorHealth.STATE_DEAD);
    prey.health.takeInjury(damage, true);
    float taken = prey.health.injury() - before;
    taken /= ActorHealth.DECOMP_FRACTION;
    //
    //  Then dispose of it appropriately-
    actor.health.takeCalories(taken * Fauna.MEAT_CONVERSION, 1);
    return true;
  }
  
  
  public boolean actionHarvest(Actor actor, Actor prey) {
    //
    //  Firstly, use a series of basic skill checks to see how effectively the
    //  carcass can be butchered:
    final Action a = action();
    float success = 1, mult = 0.5f;
    if (actor.skills.test(XENOZOOLOGY, 10, 2, a)) success++;
    if (actor.skills.test(DOMESTICS  , 5 , 1, a)) success++;
    if (actor.skills.test(HARD_LABOUR, 5 , 1, a)) success++;
    success /= 5;
    //
    //  Then we measure the physical damage done, and decrement prey health.
    final float
      before = prey.health.injury(),
      damage = success * 2,
      DF     = ActorHealth.DECOMP_FRACTION;
    if (prey.health.alive()) prey.health.setState(ActorHealth.STATE_DEAD);
    prey.health.takeInjury(damage, true);
    //
    //  Based on the damage, we extract meat and spyce from the carcass-
    final float
      taken = Nums.max(0, prey.health.injury() - before) / DF,
      meat  = taken *  mult      / 1,
      spyce = taken * (mult - 1) / 5;
    //
    //  Store those in the depot, and return-
    if (stepsVerbose && I.talkAbout == actor) {
      I.say("\nHarvesting meat from "+prey);
      I.say("  Maximum health: "+prey.health.maxHealth()+", "+DF);
      I.say("  Injury before/after: "+before+"/"+prey.health.injury());
      I.say("  Harvest mult: "+mult );
      I.say("  Meat amount:  "+meat );
      I.say("  Spyce amount: "+spyce);
    }
    if (meat  > 0) depot.inventory().bumpItem(PROTEIN  , meat );
    if (spyce > 0) depot.inventory().bumpItem(SPYCES, spyce);
    return true;
  }
  
  
  public Item sample() {
    if (type != TYPE_SAMPLE) I.complain("Not a sampling hunt!");
    return Item.withReference(SAMPLES, prey.species());
  }
  
  
  public boolean actionSample(Actor actor, Actor prey) {
    if (! actor.skills.test(XENOZOOLOGY, 10, 10, action())) return false;
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
    final boolean dead = ! preyIsLive(prey);
    
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
      if (dead) d.append("Feeding on ");
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



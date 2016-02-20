/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.content.civic.*;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.craft.Economy.*;



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
  final public static int
    HARVEST_TIME        = Stage.STANDARD_HOUR_LENGTH,
    SPYCE_DIVISOR       = 5,
    PREDATOR_SPYCE_MULT = Fauna.PREDATOR_TO_PREY_RATIO;
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
  
  private int stage         = STAGE_INIT;
  private float beginTime   = -1;
  private Combat downing    = null;
  private float proteinMult = 1;
  private float spyceMult   = 1;
  
  
  
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
    super(actor, prey, MOTIVE_JOB, REAL_HARM);
    this.prey  = prey ;
    this.type  = type ;
    this.depot = depot;
  }
  
  
  public Hunting(Session s) throws Exception {
    super(s);
    prey        = (Actor) s.loadObject();
    type        = s.loadInt();
    depot       = (Owner) s.loadObject();
    stage       = s.loadInt();
    beginTime   = s.loadFloat();
    downing     = (Combat) s.loadObject();
    proteinMult = s.loadFloat();
    spyceMult   = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(prey       );
    s.saveInt   (type       );
    s.saveObject(depot      );
    s.saveInt   (stage      );
    s.saveFloat (beginTime  );
    s.saveObject(downing    );
    s.saveFloat (proteinMult);
    s.saveFloat (spyceMult  );
  }
  
  
  public Plan copyFor(Actor other) {
    return new Hunting(other, prey, type, depot);
  }
  
  
  public void setProductionLevels(float proteinMult, float spyceMult) {
    this.proteinMult = proteinMult;
    this.spyceMult   = spyceMult  ;
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
  
  
  final Trait SAMPLE_TRAITS[]  = { CURIOUS, RUGGED };
  
  
  protected float getPriority() {
    final boolean report = I.talkAbout == actor && evalVerbose;
    if (report) {
      I.say("\nGetting priority for "+actor+" of hunting "+prey);
    }

    if (prey.destroyed() || ! prey.inWorld()) return -1;
    final boolean begun = hasBegun(), alive = preyIsLive(prey);
    if ((! begun) && (! validPrey(prey, actor))) return -1;
    
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
      crowdRating = NestUtils.localCrowding(prey.species(), prey);
      crowdRating = Nums.clamp((crowdRating - 0.5f) * 2, 0, 1);
      
      urgency += hunger = actor.health.hungerLevel();
      if (alive) urgency *= crowdRating;
      else       urgency += ActorHealth.MAX_CALORIES - 1;
      if (hunger > 0.5f) urgency += (hunger - 0.5f) * 2;
    }
    //
    //  In the case of a sampling run, things are simpler.
    else {
      if (alive) return -1;
      urgency += PlanUtils.traitAverage(actor, SAMPLE_TRAITS);
      harmLevel = Plan.NO_HARM;
    }
    //
    //  In any case, live prey can fight back, so we adjust our willingness to
    //  undertake the task accordingly.  (Or not.)
    urgency += motiveBonus() / Plan.PARAMOUNT;
    if (begun) urgency = Nums.max(urgency, 0.5f);
    
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
      I.say("  Has begun:       "+begun+" ("+hashCode()+")");
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
  
  
  public boolean isEmergency() {
    if (isActive() && nextStep instanceof Combat) return true;
    return false;
  }
  
  
  public float harmIntended(Target t) {
    if (t == depot) return 0;
    if (type == TYPE_SAMPLE) return NO_HARM;
    return super.harmIntended(t);
  }
  
  
  public Item sample() {
    if (type != TYPE_SAMPLE) I.complain("Not a sampling hunt!");
    return Item.withReference(SAMPLES, prey.species());
  }
  
  
  
  /**  Actual implementation-
    */
  protected Behaviour getNextStep() {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) {
      I.say("Getting next hunting step ("+type+") "+actor+" vs. "+prey);
    }
    if (prey.destroyed() || ! prey.inWorld()) return null;
    
    if (type == TYPE_FEEDS) {
      if (preyIsLive(prey)) {
        return downingStep(Combat.OBJECT_EITHER);
      }
      else if (prey.destroyed() || ! prey.inWorld()) {
        return null;
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
        return new Bringing(sample, actor, depot);
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
    return new Combat(actor, prey, Combat.STYLE_EITHER, object);
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
    float process = 1f / HARVEST_TIME;
    final Action a = action();
    if (! actor.skills.test(XENOZOOLOGY, 5 , 1, a)) process /= 2;
    if (! actor.skills.test(HARD_LABOUR, 10, 1, a)) process /= 2;
    
    float damage = prey.health.maxHealth() * process;
    damage /= ActorHealth.DECOMP_FRACTION;
    
    float totalMeat = prey.health.maxHealth() * Fauna.MEAT_CONVERSION;
    totalMeat /= ActorHealth.FOOD_TO_CALORIES;
    totalMeat *= proteinMult;
    
    float totalSpyce = totalMeat / SPYCE_DIVISOR;
    if (prey.species().predator()) totalSpyce *= PREDATOR_SPYCE_MULT;
    totalSpyce *= spyceMult;
    
    prey.health.takeInjury(damage, true);
    depot.inventory().bumpItem(PROTEIN, totalMeat  * process);
    depot.inventory().bumpItem(SPYCES , totalSpyce * process);
    return true;
  }
  
  
  public boolean actionSample(Actor actor, Actor prey) {
    if (! actor.skills.test(XENOZOOLOGY, 10, 10, action())) return false;
    actor.gear.addItem(sample());
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



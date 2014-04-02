/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.tactical ;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.game.planet.*;
import stratos.user.*;
import stratos.util.*;




public class Hunting extends Combat implements Economy {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static int
    TYPE_FEEDS   = 0,
    TYPE_HARVEST = 1,
    TYPE_SAMPLE  = 2;
  final static String TYPE_DESC[] = { "Feeding", "Harvest", "Sampling" };
  final static int
    STAGE_INIT          = 0,
    STAGE_HUNT          = 1,
    STAGE_FEED          = 2,
    STAGE_HARVEST_MEAT  = 3,
    STAGE_RETURN_MEAT   = 4,
    STAGE_SAMPLE_GENE   = 5,
    STAGE_RETURN_SAMPLE = 6,
    STAGE_COMPLETE      = 7 ;
  
  private static boolean verbose = false, evalVerbose = false;
  
  
  final int type ;
  final Actor prey ;
  final Employment depot ;
  private int stage = STAGE_INIT ;
  private float beginTime = -1 ;
  
  
  
  public static Hunting asFeeding(Actor actor, Actor prey) {
    return new Hunting(actor, prey, TYPE_FEEDS, null);
  }
  
  
  public static Hunting asHarvest(Actor actor, Actor prey, Employment depot) {
    if (depot == null) return null;
    return new Hunting(actor, prey, TYPE_HARVEST, depot);
  }
  
  
  public static Hunting asSample(Actor actor, Actor prey, Employment depot) {
    if (depot == null) I.complain("NO DEPOT SPECIFIED!");
    return new Hunting(actor, prey, TYPE_SAMPLE, depot);
  }
  
  
  
  private Hunting(Actor actor, Actor prey, int type, Employment depot) {
    super(actor, prey) ;
    this.prey = prey ;
    this.type = type ;
    this.depot = depot ;
  }
  
  
  public Hunting(Session s) throws Exception {
    super(s) ;
    prey = (Actor) s.loadObject() ;
    type = s.loadInt() ;
    depot = (Employment) s.loadObject() ;
    stage = s.loadInt() ;
    beginTime = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(prey) ;
    s.saveInt(type) ;
    s.saveObject(depot) ;
    s.saveInt(stage) ;
    s.saveFloat(beginTime) ;
  }
  
  
  
  /**  Evaluating targets and priority-
    */
  public static boolean validPrey(Element prey, Actor hunts, boolean conserve) {
    if (! (prey instanceof Actor)) return false;
    final Actor a = (Actor) prey;
    if (a.species() == hunts.species() || ! a.health.organic()) return false;
    if (conserve && Nest.crowdingFor(a) < 1) return false;
    return true;
  }
  
  
  final Trait
    HARVEST_TRAITS[] = { NATURALIST, FEARLESS, CRUEL },
    SAMPLE_TRAITS[]  = { CURIOUS, FEARLESS, NATURALIST };
  
  
  public float priorityFor(Actor actor) {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (! validPrey(prey, actor, false)) {
      if (report) I.say(prey+" IS NOT VALID!");
      return -1;
    }
    if (! actor.gear.armed()) return 0;
    
    float urgency, harmLevel;
    final Trait baseTraits[];
    final float crowding = Nest.crowdingFor(prey);
    
    if (type == TYPE_FEEDS) {
      final float hunger = actor.health.hungerLevel();
      urgency = hunger * PARAMOUNT * Math.min(crowding, hunger);
      harmLevel = EXTREME_HARM;
      baseTraits = Combat.BASE_TRAITS;
    }
    else if (type == TYPE_HARVEST) {
      if (crowding < 1) return 0;
      urgency = Visit.clamp(ROUTINE * crowding, ROUTINE, URGENT);
      harmLevel = EXTREME_HARM;
      baseTraits = HARVEST_TRAITS;
    }
    else {
      urgency = ROUTINE;
      harmLevel = NO_HARM;
      baseTraits = SAMPLE_TRAITS;
    }
    
    final float priority = priorityForActorWith(
      actor, prey, urgency,
      harmLevel, MILD_COMPETITION,
      RANGED_SKILLS, baseTraits,
      NO_MODIFIER, NORMAL_DISTANCE_CHECK, REAL_DANGER
    );
    
    if (report) {
      I.say("\nHunting type is: "+TYPE_DESC[type]);
      I.say("  Base urgency is: "+urgency);
      I.say("  Final priority: "+priority);
    }
    return priority;
  }
  
  
  public boolean valid() {
    if (actor == null) return super.valid() ;
    if (type == TYPE_HARVEST) {
      if (actor.gear.amountOf(PROTEIN) > 0) return true;
    }
    if (type == TYPE_SAMPLE) {
      if (actor.gear.hasItem(sample())) return true;
    }
    return super.valid() ;
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
      if (! isDowned(prey)) return super.getNextStep();
      
      if (carried >= 5 || (prey.destroyed() && carried > 0)) {
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
      final boolean sampled = actor.gear.hasItem(sample());
      if (sampled) {
        final Action returns = new Action(
          actor, depot,
          this, "actionReturnSample",
          Action.REACH_DOWN, "Returning sample"
        );
        return returns;
      }
      if (! isDowned(prey)) return super.getNextStep();
      else {
        final Action samples = new Action(
          actor, prey,
          this, "actionSample",
          Action.BUILD, "Sampling"
        );
        return samples;
      }
    }
    
    if (report) I.say("RETURNING NULL ACTION");
    return null;
  }
  
  
  public int motionType(Actor actor) {
    if (isDowned(prey)) return MOTION_ANY;
    else if (prey.mind.awareOf(actor)) return MOTION_FAST;
    else if (actor.mind.awareOf(prey)) return MOTION_SNEAK;
    else return super.motionType(actor);
  }
  
  
  public boolean actionFeed(Actor actor, Actor prey) {
    //  Determine just how large a chunk you can take out of the prey-
    final float
      before = prey.health.injuryLevel(),
      damage = actor.gear.attackDamage() * (Rand.num() + 0.5f) / 10 ;
    prey.health.takeInjury(damage) ;
    float taken = prey.health.injuryLevel() - before ;
    taken *= prey.health.maxHealth() ;
    
    //  Then dispose of it appropriately-
    if (! prey.health.dying()) prey.health.setState(ActorHealth.STATE_DYING);
    actor.health.takeCalories(taken * Fauna.MEAT_CONVERSION, 1);
    return true;
  }
  
  
  private Item sample() {
    return Item.withReference(SAMPLES, prey);
  }
  
  
  public boolean actionHarvest(Actor actor, Actor prey) {
    float success = 1;
    if (actor.traits.test(XENOZOOLOGY, 5, 10)) success++;
    if (actor.traits.test(DOMESTICS, 5, 5)) success++;
    if (actor.traits.test(HARD_LABOUR, 5, 5)) success++;
    
    final float
      before = prey.health.injuryLevel(),
      damage = success * 2;
    prey.health.takeInjury(damage);
    final float taken = prey.health.injuryLevel() - before;
    
    if (! prey.health.dying()) prey.health.setState(ActorHealth.STATE_DYING);
    actor.gear.bumpItem(PROTEIN, taken * prey.health.maxHealth());
    return true;
  }
  
  
  public boolean actionReturnHarvest(Actor actor, Employment depot) {
    actor.gear.transfer(PROTEIN, depot);
    return true;
  }
  
  
  public boolean actionSample(Actor actor, Actor prey) {
    if (! actor.traits.test(XENOZOOLOGY, 10, 10)) return false;
    actor.gear.addItem(sample());
    return true;
  }
  
  
  public boolean actionReturnSample(Actor actor, Employment depot) {
    actor.gear.transfer(sample(), depot);
    return true;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    if (! prey.health.alive()) {
      if (type == TYPE_FEEDS) {
        d.append("Scavenging meat from ") ;
        d.append(prey) ;
      }
      if (type == TYPE_HARVEST) {
        if (! prey.destroyed()) {
          d.append("Harvesting meat from ") ;
          d.append(prey) ;
        }
        else {
          d.append("Returning meat to ") ;
          d.append(depot) ;
        }
      }
    }
    else d.append("Hunting "+prey) ;
  }
}




//
//  TODO:  Get rid of this and use a similar sampling method to that employed
//  by human-minds?
/*
public static Actor nextPreyFor(
  Actor actor, boolean conserve
) {
  if (verbose) I.sayAbout(actor, "FINDING NEXT PREY") ;
  Actor pickedPrey = null ;
  float bestRating = Float.NEGATIVE_INFINITY ;
  //
  //  
  for (Element t : actor.mind.awareOf()) {
    if (! (t instanceof Actor)) continue ;
    final Actor f = (Actor) t ;
    if ((! f.health.organic()) || (! (t instanceof Fauna))) continue ;
    final Species s = (Species) f.species() ;
    if (s == actor.species()) continue ;
    //
    //  
    final float crowding = Nest.crowdingFor(f) ;
    if (conserve && crowding < 1) continue ;
    final float
      danger = (f.health.alive() ? Combat.combatStrength(f, actor) : 0),
      rangePenalty = Plan.rangePenalty(actor, f) ;
    float rating = 1 / (1f + danger + (rangePenalty / Plan.ROUTINE)) ;
    rating *= crowding * Rand.avgNums(2) ;
    //
    //  
    if (rating > bestRating) { pickedPrey = f ; bestRating = rating ; }
  }
  if (verbose && I.talkAbout == actor) {
    if (pickedPrey == null) I.say("NO PREY FOUND FOR "+actor) ;
    else I.say("PREY IS: "+pickedPrey) ;
  }
  return pickedPrey ;
}
//*/




/*
final boolean report = verbose && I.talkAbout == actor && hasBegun();
if (report) {
  I.say("Getting next hunting step...");
}

if (beginTime == -1) beginTime = actor.world().currentTime() ;
final float timeSpent = actor.world().currentTime() - beginTime ;
if (timeSpent > World.STANDARD_DAY_LENGTH / 3) {
  return null ;
}

if (prey.health.conscious()) return super.getNextStep() ;
if (type == TYPE_FEEDS && actor.health.energyLevel() >= 1.5f) {
  if (verbose) I.sayAbout(actor, "Have eaten fill of "+prey) ;
  return null ;
}

if (type == TYPE_SAMPLE) {
  //  TODO:  TEST THIS OUT AND IMPLEMENT
}

if (type == TYPE_HARVEST) {
  final float amountC = actor.gear.amountOf(PROTEIN) ;
  if ((prey.destroyed() && amountC > 0) || amountC >= 5) {
    final Action process = new Action(
      actor, depot,
      this, "actionProcess",
      Action.REACH_DOWN, "Processing "+prey
    );
    if (report) I.say("Next step is processing...");
    return process ;
  }
}

if (! prey.inWorld()) return null;
final Action harvest = new Action(
  actor, prey,
  this, "actionHarvest",
  Action.BUILD, "Harvesting from "+prey
) ;
if (report) I.say("Next step is harvest...");
return harvest ;
//*/
//}


/*
public boolean actionProcess(Actor actor, Employment depot) {
if (type == TYPE_HARVEST) {
  actor.gear.transfer(PROTEIN, depot) ;
  return true;
}

//  TODO:  Restore and test these functions...
/*
if (type == TYPE_PROCESS || type == TYPE_SAMPLE) {
  final Item
    sample = Item.withReference(SAMPLES, prey),
    carried = actor.gear.matchFor(sample) ;
  if (carried != null) actor.gear.transfer(carried, depot) ;
  if (type == TYPE_SAMPLE) return true ;
  
  final Inventory stocks = depot.inventory() ;
  final float remaining = stocks.amountOf(sample) ;
  if (remaining > 0) {
    float success = 1 ;
    if (actor.traits.test(DOMESTICS  , SIMPLE_DC  , 1)) success++ ;
    if (actor.traits.test(XENOZOOLOGY, MODERATE_DC, 1)) success++ ;
    
    final Species species = (Species) prey.species() ;
    float baseAmount = 0.1f, spiceAmount = 0.1f ;
    if (species.type == Species.Type.BROWSER ) spiceAmount  = 0 ;
    if (species.type != Species.Type.PREDATOR) spiceAmount /= 4 ;
    baseAmount = Math.min(baseAmount, remaining) ;
    
    stocks.removeItem(Item.withAmount(sample, baseAmount)) ;
    baseAmount *= success ;
    spiceAmount *= baseAmount * (1 + (success / 2)) ;
    stocks.bumpItem(PROTEIN, baseAmount) ;
    stocks.bumpItem(TRUE_SPICE, spiceAmount) ;
  }
}
//*/
/*
return true ;
}
//*/






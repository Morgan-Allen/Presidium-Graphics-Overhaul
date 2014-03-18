/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.tactical ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.civilian.*;
import src.game.common.* ;
import src.game.planet.* ;
import src.util.* ;
import src.user.* ;




public class Hunting extends Combat implements Economy {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static int
    TYPE_FEEDS   = 0,
    TYPE_HARVEST = 1,
    TYPE_PROCESS = 2,
    TYPE_SAMPLE  = 3 ;
  final static int
    STAGE_INIT          = 0,
    STAGE_HUNT          = 1,
    STAGE_FEED          = 2,
    STAGE_HARVEST_MEAT  = 3,
    STAGE_RETURN_MEAT   = 4,
    STAGE_SAMPLE_GENE   = 5,
    STAGE_RETURN_SAMPLE = 6,
    STAGE_COMPLETE      = 7 ;
  private static boolean verbose = false ;
  
  
  final int type ;
  final Actor prey ;
  final Employment depot ;
  private int stage = STAGE_INIT ;
  private float beginTime = -1 ;
  
  
  
  public static Hunting asFeeding(Actor actor, Actor prey) {
    return new Hunting(actor, prey, TYPE_FEEDS, null) ;
  }
  
  
  public static Hunting asHarvest(Actor actor, Actor prey, Employment depot) {
    if (depot == null) return asFeeding(actor, prey) ;
    return new Hunting(actor, prey, TYPE_HARVEST, depot) ;
  }
  
  
  public static Hunting asProcess(Actor actor, Actor prey, Employment depot) {
    if (depot == null) I.complain("NO DEPOT SPECIFIED!") ;
    return new Hunting(actor, prey, TYPE_PROCESS, depot) ;
  }
  
  
  public static Hunting asSample(Actor actor, Actor prey, Employment depot) {
    if (depot == null) I.complain("NO DEPOT SPECIFIED!") ;
    return new Hunting(actor, prey, TYPE_SAMPLE, depot) ;
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
  public float priorityFor(Actor actor) {
    //
    //  TODO:  Unify this evaluation method with the 'FindPrey' routine below.
    if (prey.species() == actor.species() || ! prey.health.organic()) {
      return -1 ;
    }
    float priority = 0 ;
    if (type == TYPE_FEEDS) {
      float hunger = actor.health.hungerLevel() ;
      if (begun()) hunger = (hunger + 0.5f) / 1.5f ;
      if (hunger < 0) return 0 ;
      priority = hunger * PARAMOUNT ;
      if (verbose) I.sayAbout(actor, "Base feeding priority: "+priority) ;
    }
    if (type == TYPE_HARVEST || type == TYPE_PROCESS) {
      float crowding = Nest.crowdingFor(prey) ;
      if (crowding < 1) return 0 ;
      priority = ROUTINE ;
    }
    else priority = ROUTINE ;
    priority += priorityMod ;
    //
    //  Modify based on relation with subject-
    //if (prey.species() == actor.species()) priority -= ROUTINE ;
    //priority -= actor.mind.relation(prey) * PARAMOUNT ;
    //
    //  Modify based on danger of extraction-
    if (prey.health.conscious()) {
      priority = Combat.combatPriority(
        actor, prey, priority, PARAMOUNT, false
      ) ;
      priority -= Plan.rangePenalty(actor, prey) ;
    }
    
    if (verbose) I.sayAbout(actor, "Hunting "+prey+" priority: "+priority) ;
    return Visit.clamp(priority, 0, PARAMOUNT) ;
  }
  
  
  public boolean valid() {
    if (actor == null) return super.valid() ;
    //
    //  TODO:  Try to make this whole process more elegant.  Establish the
    //  basic harvest-item from the beginning?
    
    if (type == TYPE_HARVEST) {
      if (actor.gear.amountOf(PROTEIN) > 0) return true ;
    }
    if (type == TYPE_PROCESS || type == TYPE_SAMPLE) {
      final Item sample = Item.withReference(SAMPLES, prey) ;
      if (actor.gear.amountOf(sample) > 0) return true ;
    }
    return super.valid() ;
  }
  
  
  //
  //  TODO:  Get rid of this and use a similar sampling method to that employed
  //  by human-minds?
  
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
  
  
  
  /**  Actual implementation-
    */
  public int motionType(Actor actor) {
    //
    //  Close at normal speed until you are near your prey.  Then enter stealth
    //  mode to get closer.  If they spot you, charge.
    if (prey.mind.awareOf(actor)) {
      return MOTION_FAST ;
    }
    else if (actor.mind.awareOf(prey)) {
      return MOTION_SNEAK ;
    }
    else return super.motionType(actor) ;
  }
  
  
  protected Behaviour getNextStep() {
    if (beginTime == -1) beginTime = actor.world().currentTime() ;
    final float timeSpent = actor.world().currentTime() - beginTime ;
    if (timeSpent > World.STANDARD_DAY_LENGTH / 3) {
      return null ;
    }
    
    if (prey.health.conscious()) return super.getNextStep() ;
    /*
    if (type == TYPE_FEEDS && actor.health.energyLevel() >= 1.5f) {
      if (verbose) I.sayAbout(actor, "Have eaten fill of "+prey) ;
      return null ;
    }
    //*/
    
    if (type == TYPE_HARVEST || type == TYPE_PROCESS) {
      final Item carried = (type == TYPE_HARVEST) ?
        Item.withAmount(PROTEIN, 1) :
        Item.withReference(SAMPLES, prey) ;
      final float amountC = actor.gear.amountOf(carried) ;
      if (
        (prey.destroyed() && amountC > 0) ||
        amountC >= 5 ||
        depot.inventory().amountOf(carried) > 0
      ) {
        final Action process = new Action(
          actor, depot,
          this, "actionProcess",
          Action.REACH_DOWN, "Processing "+prey
        ) ;
        return process ;
      }
    }
    
    final Action harvest = new Action(
      actor, prey,
      this, "actionHarvest",
      Action.BUILD, "Harvesting from "+prey
    ) ;
    return harvest ;
  }
  
  
  public boolean actionHarvest(Actor actor, Actor prey) {
    if (type == TYPE_SAMPLE) {
      final Item sample = Item.withReference(SAMPLES, prey) ;
      actor.gear.addItem(sample) ;
      return true ;
    }
    //
    //  Determine just how large a chunk you can take out of the prey-
    final float
      before = prey.health.injuryLevel(),
      damage = actor.gear.attackDamage() * (Rand.num() + 0.5f) / 10 ;
    prey.health.takeInjury(damage) ;
    float taken = prey.health.injuryLevel() - before ;
    taken *= prey.health.maxHealth() ;
    //
    //  Then dispose of it appropriately-
    if (! prey.health.dying()) prey.health.setState(ActorHealth.STATE_DYING) ;
    if (type == TYPE_FEEDS) {
      actor.health.takeSustenance(taken * Fauna.MEAT_CONVERSION, 1) ;
    }
    if (type == TYPE_HARVEST) {
      actor.gear.bumpItem(PROTEIN, taken) ;
    }
    if (type == TYPE_PROCESS) {
      final Item sample = Item.withReference(SAMPLES, prey) ;
      actor.gear.addItem(Item.withAmount(sample, taken)) ;
    }
    return true ;
  }
  
  
  public boolean actionProcess(Actor actor, Employment depot) {
    if (type == TYPE_HARVEST) {
      actor.gear.transfer(PROTEIN, depot) ;
    }
    
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
    return true ;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    if (! prey.health.alive()) {
      if (type == TYPE_FEEDS) {
        d.append("Scavenging meat from ") ;
        d.append(prey) ;
      }
      if (type == TYPE_HARVEST || type == TYPE_PROCESS) {
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







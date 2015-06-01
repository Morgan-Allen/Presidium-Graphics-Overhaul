/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.civic.*;
import stratos.game.maps.*;
import stratos.game.wild.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



//  TODO:  Try merging this with Farming (or extending it.)

public class Forestry extends Plan {

  private static boolean
    evalVerbose  = false,
    stepsVerbose = false;
  
  final public static int
    STAGE_INIT     = -1,
    STAGE_GET_SEED =  0,
    STAGE_PLANTING =  1,
    STAGE_SAMPLING =  2,
    STAGE_STORAGE  =  3,
    STAGE_CUTTING  =  4,
    STAGE_PROCESS  =  5,
    STAGE_DONE     =  6;
  
  final public static float
    FLORA_PROCESS_TIME = Stage.STANDARD_HOUR_LENGTH,
    GROW_STAGE_POLYMER = 5;
  
  
  final Venue depot;
  private int stage = STAGE_INIT;
  private Tile  toPlant = null;
  private Flora toCut   = null;
  
  
  public static Forestry nextSampling(
    Actor actor, Venue nursery, float urgency
  ) {
    final Forestry f = new Forestry(actor, nursery);
    f.configureFor(STAGE_SAMPLING);
    f.addMotives(Plan.MOTIVE_JOB, urgency * ROUTINE);
    return f;
  }
  
  
  public static Forestry nextPlanting(Actor actor, Venue nursery) {
    final Forestry f = new Forestry(actor, nursery);
    f.configureFor(STAGE_GET_SEED);
    return f;
  }
  
  
  public static Forestry nextCutting(Actor actor, Venue nursery) {
    final Forestry f = new Forestry(actor, nursery);
    f.configureFor(STAGE_CUTTING);
    return f;
  }
  
  
  private Forestry(Actor actor, Venue nursery) {
    super(actor, nursery, MOTIVE_JOB, NO_HARM);
    this.depot = nursery;
  }
  
  
  public Forestry(Session s) throws Exception {
    super(s);
    this.depot = (Venue) s.loadObject();
    this.stage = s.loadInt();
    toPlant = (Tile) s.loadTarget();
    toCut = (Flora) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(depot);
    s.saveInt(stage);
    s.saveTarget(toPlant);
    s.saveObject(toCut);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Forestry(other, depot);
  }

  
  
  /**  Behaviour implementation-
    */
  public boolean configureFor(int stage) {
    final Box2D limit = depot.areaClaimed();
    
    if (stage == STAGE_GET_SEED || stage == STAGE_PLANTING) {
      toPlant = findPlantTile(actor, depot, limit);
      if (toPlant == null) { interrupt(INTERRUPT_NO_PREREQ); return false; }
      if (depot.stocks.amountOf(seedMatch()) > 0) {
        this.stage = STAGE_GET_SEED;
      }
      else this.stage = STAGE_PLANTING;
    }
    
    if (stage == STAGE_SAMPLING || stage == STAGE_CUTTING) {
      toCut = findCutting(actor, depot, limit);
      if (toCut == null) { interrupt(INTERRUPT_NO_PREREQ); return false; }
      this.stage = stage;
    }
    
    return false;
  }
  
  
  private boolean configured() {
    if (stage != STAGE_INIT) return true;
    return false;
  }
  
  
  private Item seedMatch() {
    final Item match = Item.withReference(GENE_SEED, Flora.WILD_FLORA);
    return Item.withAmount(match, 0.1f);
  }
  
  
  private Item sampleMatch() {
    final Item match = Item.withReference(SAMPLES, Flora.WILD_FLORA);
    return Item.withAmount(match, 1);
  }
  
  
  //  TODO:  Vary these for the different activity types (sample, harvest or
  //         planting.)
  final static Skill BASE_SKILLS[] = { CULTIVATION, HARD_LABOUR };
  final static Trait BASE_TRAITS[] = { NATURALIST, ENERGETIC };
  

  protected float getPriority() {
    //
    //  Basic variable setup and sanity checks-
    final boolean report = evalVerbose && I.talkAbout == actor && hasBegun();
    if (! configured()) return 0;
    final Target subject = toPlant == null ? toCut : toPlant;
    if (subject == null) return 0;
    final Tile at = toPlant == null ? toCut.origin() : toPlant;
    //
    //  As the abundance of flora increases, harvest becomes more attractive,
    //  and vice-versa for planting as abundance decreases.
    float abundance = actor.world().ecology().forestRating(at);
    int growStage = -1;
    float urgency = 0, shortage = 0;
    if (toCut != null) {
      growStage = toCut.growStage() + 1;
      abundance *= growStage * 2f / Flora.MAX_GROWTH;
    }
    
    if (stage == STAGE_SAMPLING || stage == STAGE_STORAGE) {
      urgency = 0.2f;
    }
    else if (stage == STAGE_GET_SEED || stage == STAGE_PLANTING) {
      urgency = 1 - abundance;
    }
    else if (stage == STAGE_CUTTING || stage == STAGE_PROCESS) {
      shortage = Nums.max(0, depot.stocks.relativeShortage(POLYMER));
      if (stage == STAGE_CUTTING) urgency = abundance + shortage - 1;
      else urgency = 0.5f * (1 + shortage);
    }
    
    final float priority = PlanUtils.jobPlanPriority(
      actor, this, urgency, 1, 3, MILD_HARM, BASE_TRAITS
    );
    if (report) {
      I.say("\nGetting forestry priority for "+actor);
      I.say("  Stage is:     "+stage  );
      I.say("  Planting at:  "+toPlant);
      I.say("  Cutting:      "+toCut+" (stage "+growStage+")");
      I.say("  Base urgency: "+urgency);
      I.say("  Polymer shortage:     "+shortage );
      I.say("  Vegetation abundance: "+abundance);
      I.say("  Final priority:       "+priority );
    }
    return priority;
  }
  
  
  public Behaviour getNextStep() {
    if (! configured()) return null;
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) {
      I.say("\nGetting next forestry step...");
      I.say("  CURRENT STAGE IS: "+stage+" ("+hashCode()+")");
    }
    
    if (stage == STAGE_GET_SEED) {
      if (report) I.say("  Getting seed.");
      final Action collects = new Action(
        actor, depot,
        this, "actionCollectSeed",
        Action.LOOK, "Collecting seed"
      );
      return collects;
    }
    
    if (stage == STAGE_PLANTING) {
      if (report) I.say("  Getting seed.");
      final Action plants = new Action(
        actor, toPlant,
        this, "actionPlant",
        Action.BUILD, "Planting"
      );
      if (Spacing.adjacent(toPlant, actor) && Rand.num() <= 0.8f) {
        plants.setMoveTarget(actor.origin());
      }
      else {
        final Tile to = Spacing.pickFreeTileAround(toPlant, actor);
        if (to == null) return null;
        plants.setMoveTarget(to);
      }
      return plants;
    }
    
    if (stage == STAGE_SAMPLING) {
      if (report) I.say("  Getting sample.");
      final Action sample = new Action(
        actor, toCut,
        this, "actionSampling",
        Action.BUILD, "Sampling"
      );
      sample.setMoveTarget(Spacing.nearestOpenTile(toCut.origin(), actor));
      return sample;
    }
    
    if (stage == STAGE_STORAGE) {
      if (report) I.say("  Returning with samples.");
      final Action returns = new Action(
        actor, depot,
        this, "actionStoreSamples",
        Action.REACH_DOWN, "Store Samples"
      );
      return returns;
    }
    
    if (stage == STAGE_CUTTING) {
      if (report) I.say("  Going to perform cutting.");
      final Action cuts = new Action(
        actor, toCut,
        this, "actionCutting",
        Action.BUILD, "Cutting"
      );
      cuts.setMoveTarget(Spacing.nearestOpenTile(toCut.origin(), actor));
      return cuts;
    }
    
    if (stage == STAGE_PROCESS) {
      if (report) I.say("  Returning to process cuttings.");
      final Action returns = new Action(
        actor, depot,
        this, "actionProcessHarvest",
        Action.REACH_DOWN, "Processing Harvest"
      );
      return returns;
    }
    return null;
  }
  
  
  public boolean actionCollectSeed(Actor actor, Venue depot) {
    final Item match = seedMatch();
    depot.stocks.transfer(match, actor);
    stage = STAGE_PLANTING;
    return true;
  }
  
  
  public boolean actionPlant(Actor actor, Tile beside) {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) I.say("\nPLANTING AT "+toPlant+", from: "+actor.origin());
    
    if (! Flora.canGrowAt(toPlant)) {
      if (report) {
        I.say("  COULD NOT GROW!");
        I.say("  Occupied? "+(toPlant.inside().size() > 0));
        I.say("  Blocked? "+toPlant.blocked());
      }
      toPlant = findPlantTile(actor, depot, depot.areaClaimed());
      if (report) I.say("  New tile: "+toPlant);
      if (toPlant == null) stage = STAGE_STORAGE;
      return false;
    }
    
    float growStage = -0.5f;
    for (Item seed : actor.gear.matches(seedMatch())) {
      growStage += (1 + seed.quality) / 2f;
      break;
    }
    if (actor.skills.test(CULTIVATION, MODERATE_DC, 5f)) growStage += 0.75f;
    if (actor.skills.test(HARD_LABOUR, ROUTINE_DC , 5f)) growStage += 0.75f;
    if (report) I.say("  Grow stage: "+growStage);
    if (growStage <= 0) return false;
    
    final Flora f = Flora.newGrowthAt(toPlant);
    if (f == null) return false;
    f.incGrowth(growStage * (Rand.num() + 1) / 4, toPlant.world, true);
    
    if (report) I.say("  SUCCESS! Grow stage: "+growStage);
    stage = STAGE_DONE;
    return true;
  }
  
  
  public boolean actionCutting(Actor actor, Flora cut) {
    if (! actor.skills.test(HARD_LABOUR, ROUTINE_DC, 1.0f)) return false;
    cut.setAsDestroyed();
    
    //  TODO:  TRY TO USE A STRETCHER-DELIVERY FOR THIS!
    final Item felled = Item.withReference(SAMPLES, cut);
    actor.gear.addItem(felled);
    
    stage = STAGE_PROCESS;
    return true;
  }
  
  
  public boolean actionProcessHarvest(Actor actor, Venue depot) {
    actor.gear.transfer(SAMPLES, depot);
    
    float processRate = 1, bonus = 1;
    bonus += actor.skills.test(CULTIVATION, SIMPLE_DC , 1) ? 1 : 0;
    bonus += actor.skills.test(HARD_LABOUR, ROUTINE_DC, 1) ? 1 : 0;
    bonus *= GROW_STAGE_POLYMER / (3 * FLORA_PROCESS_TIME);
    boolean working = false;
    
    for (Item i : depot.stocks.matches(SAMPLES)) {
      if (! (i.refers instanceof Flora)) continue;
      final int growth = 1 + ((Flora) i.refers).growStage();
      working = true;
      
      processRate /= FLORA_PROCESS_TIME * growth;
      depot.stocks.removeItem(Item.withAmount(i, processRate));
      depot.stocks.bumpItem(POLYMER, bonus);
      break;
    }
    
    if (! working) {
      stage = STAGE_DONE;
      return false;
    }
    else return true;
  }
  
  
  public boolean actionSampling(Actor actor, Flora cut) {
    if (! actor.skills.test(CULTIVATION, SIMPLE_DC, 1.0f)) return false;
    
    final int growStage = cut.growStage();
    actor.gear.bumpItem(GREENS, growStage * Rand.num() / Flora.MAX_GROWTH);
    
    actor.gear.addItem(sampleMatch());
    stage = STAGE_STORAGE;
    return true;
  }
  
  
  public boolean actionStoreSamples(Actor actor, Venue depot) {
    if (stepsVerbose) I.say("RETURNING SAMPLES TO "+depot);
    
    actor.gear.transfer(SAMPLES    , depot);
    actor.gear.transfer(GENE_SEED  , depot);
    actor.gear.transfer(GREENS     , depot);
    
    stage = STAGE_DONE;
    return true;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (stage == STAGE_INIT) {
      d.append("Performing forestry");
    }
    
    if (stage == STAGE_GET_SEED) {
      d.append("Getting timber seed");
    }
    if (stage == STAGE_PLANTING) {
      d.append("Planting timber");
    }
    
    if (stage == STAGE_CUTTING) {
      d.append("Felling timber");
    }
    if (stage == STAGE_PROCESS) {
      d.append("Processing harvest");
    }
    
    if (stage == STAGE_SAMPLING) {
      d.append("Taking vegetation samples");
    }
    if (stage == STAGE_STORAGE) {
      d.append("Returning samples");
    }
  }
  
  
  
  /**  Utility methods for finding suitable plant/harvest targets-
    */
  public static Tile findPlantTile(Actor actor, Venue depot, Box2D limit) {
    final boolean report = evalVerbose && (
      I.talkAbout == actor || I.talkAbout == depot
    );
    if (report) I.say("\nFinding tile to plant near "+depot);
    
    Tile picked = null, tried;
    float bestRating = Float.NEGATIVE_INFINITY;
    
    for (int n = 5; n-- > 0;) {
      tried = Spacing.pickRandomTile(depot, Stage.ZONE_SIZE, actor.world());
      tried = Spacing.nearestOpenTile(tried, actor);
      if (tried == null || ! Flora.canGrowAt(tried)) continue;
      if (limit != null && ! limit.contains(tried.x, tried.y)) continue;
      
      float rating = tried.habitat().moisture() / 10f;
      rating -= Plan.rangePenalty(actor.base(), actor, tried);
      rating -= Plan.dangerPenalty(tried, actor);
      rating -= actor.world().ecology().forestRating(tried);
      
      if (report) I.say("  Rating for "+tried+" is "+rating);
      if (rating > bestRating) { bestRating = rating; picked = tried; }
    }
    
    return picked;
  }
  
  
  public static Flora findCutting(Actor actor, Target from, Box2D limit) {
    final Target near = from == null ? actor : from;
    final Presences p = actor.world().presences;
    
    Flora cuts = null;
    cuts = (Flora) p.randomMatchNear(Flora.class, near, limit);
    if (cuts == null) cuts = (Flora) p.nearestMatch(Flora.class, near, limit);
    if (cuts == null) return null;
    
    if (! cuts.inWorld()) {
      I.say("\nWARNING: FLORA TO CUT NOT IN WORLD: "+cuts);
      return null;
    }
    return cuts;
  }
}







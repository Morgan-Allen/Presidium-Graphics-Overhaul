/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */



package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.PavingMap;
import stratos.game.wild.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



public class Forestry extends Plan {
  
  final public static int
    STAGE_INIT     = -1,
    STAGE_GET_SEED =  0,
    STAGE_PLANTING =  1,
    STAGE_SAMPLING =  2,
    STAGE_CUTTING  =  3,
    STAGE_RETURN   =  4,
    STAGE_DONE     =  5;
  private static boolean
    evalVerbose  = false,
    stepsVerbose = false;
  
  
  final Venue nursery;
  private int stage = STAGE_INIT;
  private Tile toPlant = null;
  private Flora toCut = null;
  
  
  public static Forestry nextSampling(Actor actor, Venue nursery) {
    final Forestry f = new Forestry(actor, nursery);
    f.configureFor(STAGE_SAMPLING);
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
    this.nursery = nursery;
  }
  
  
  public Forestry(Session s) throws Exception {
    super(s);
    this.nursery = (Venue) s.loadObject();
    this.stage = s.loadInt();
    toPlant = (Tile) s.loadTarget();
    toCut = (Flora) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(nursery);
    s.saveInt(stage);
    s.saveTarget(toPlant);
    s.saveObject(toCut);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Forestry(other, nursery);
  }

  
  
  /**  Behaviour implementation-
    */
  public boolean configureFor(int stage) {
    
    if (stage == STAGE_GET_SEED || stage == STAGE_PLANTING) {
      toPlant = findPlantTile(actor, nursery);
      if (toPlant == null) { interrupt(INTERRUPT_NO_PREREQ); return false; }
      if (nursery.stocks.amountOf(seedMatch()) > 0) {
        this.stage = STAGE_GET_SEED;
      }
      else this.stage = STAGE_PLANTING;
    }
    
    if (stage == STAGE_SAMPLING || stage == STAGE_CUTTING) {
      toCut = findCutting(actor, nursery);
      if (toCut == null) { interrupt(INTERRUPT_NO_PREREQ); return false; }
      this.stage = stage;
    }
    
    return false;
  }
  
  
  private boolean configured() {
    if (stage != STAGE_INIT) return true;
    final float abundance = actor.world().ecology().globalBiomass();
    return configureFor(
      Rand.num() < abundance ? STAGE_CUTTING : STAGE_GET_SEED
    );
  }
  
  
  //  TODO:  Vary these for the different activity types (sample, harvest or
  //         planting.)
  final static Skill BASE_SKILLS[] = { CULTIVATION, HARD_LABOUR };
  final static Trait BASE_TRAITS[] = { NATURALIST, ENERGETIC };
  

  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (! configured()) return 0;
    
    final Target subject = toPlant == null ? toCut : toPlant;
    final Tile at = toPlant == null ? toCut.origin() : toPlant;
    if (subject == null) return 0;
    
    //  As the abundance of flora increases, harvest becomes more attractive,
    //  and vice-versa for planting as abundance decreases.
    final float abundance = actor.world().ecology().forestRating(at);
    float bonus = 0;
    
    if (stage == STAGE_SAMPLING) {
      bonus = 1;
    }
    if (stage == STAGE_GET_SEED || stage == STAGE_PLANTING) {
      bonus += 0.5f - abundance;
    }
    else if (stage == STAGE_CUTTING) {
      bonus += abundance - 0.5f;
    }
    else if (stage == STAGE_RETURN) {
      bonus = 0.5f;
    }
    
    //  Otherwise, it's generally a routine activity.
    final float priority = priorityForActorWith(
      actor, subject,
      CASUAL * (1 + bonus), CASUAL * bonus,
      NO_HARM, FULL_COMPETITION, NO_FAIL_RISK,
      BASE_SKILLS, BASE_TRAITS, HEAVY_DISTANCE_CHECK,
      report
    );
    if (report) {
      I.say("\nGetting forestry priority for "+actor);
      I.say("  Is planting?          "+(toPlant != null));
      I.say("  Vegetation abundance: "+abundance);
    }
    return priority;
  }
  
  
  public Behaviour getNextStep() {
    if (! configured()) return null;
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) I.say("\nGetting next forestry step...");
    
    if (stage == STAGE_GET_SEED) {
      if (report) I.say("  Getting seed.");
      final Action collects = new Action(
        actor, nursery,
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
    
    if (stage == STAGE_RETURN) {
      if (report) I.say("  Returning with cuttings.");
      final Action returns = new Action(
        actor, nursery,
        this, "actionReturnHarvest",
        Action.REACH_DOWN, "Returning Harvest"
      );
      return returns;
    }
    return null;
  }
  
  
  private Item seedMatch() {
    return Item.withAmount(Item.withReference(
      GENE_SEED, Species.TIMBER
    ), 0.1f);
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
      toPlant = findPlantTile(actor, nursery);
      if (report) I.say("  New tile: "+toPlant);
      if (toPlant == null) stage = STAGE_RETURN;
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
    if (! actor.skills.test(CULTIVATION, SIMPLE_DC, 1.0f)) return false;
    cut.setAsDestroyed();
    
    final int growStage = cut.growStage();
    actor.gear.bumpItem(GREENS, growStage * Rand.num() / Flora.MAX_GROWTH);
    if (Rand.num() < 0.1f * growStage) actor.gear.bumpItem(SPYCE_N, 1);
    
    stage = STAGE_RETURN;
    return true;
  }
  
  
  public boolean actionSampling(Actor actor, Flora cut) {
    if (! actor.skills.test(CULTIVATION, SIMPLE_DC, 1.0f)) return false;
    
    actor.gear.addItem(Item.withReference(GENE_SEED, cut));
    stage = STAGE_RETURN;
    return true;
  }
  
  
  public boolean actionReturnHarvest(Actor actor, Venue depot) {
    if (stepsVerbose) I.say("RETURNING SAMPLES TO "+depot);
    
    for (Item seed : actor.gear.matches(seedMatch())) {
      actor.gear.transfer(seed, depot);
    }
    actor.gear.transfer(GENE_SEED  , depot);
    actor.gear.transfer(GREENS     , depot);
    actor.gear.transfer(SPYCE_N, depot);
    
    stage = STAGE_DONE;
    return true;
  }
  
  
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
    if (stage == STAGE_SAMPLING) {
      d.append("Taking vegetation samples");
    }
    if (stage == STAGE_RETURN) {
      d.append("Returning harvest");
    }
  }
  
  
  
  /**  Utility methods for finding suitable plant/harvest targets-
    */
  public static Tile findPlantTile(Actor actor, Venue nursery) {
    Tile picked = null, tried;
    float bestRating = Float.NEGATIVE_INFINITY;
    
    for (int n = 10; n-- > 0;) {
      tried = Spacing.pickRandomTile(
        actor, Stage.SECTOR_SIZE / 2, actor.world()
      );
      tried = Spacing.nearestOpenTile(tried, actor);
      if (tried == null || ! Flora.canGrowAt(tried)) continue;
      if (PavingMap.pavingReserved(tried, true)) continue;
      if (actor.world().claims.venueClaims(tried.area(null))) continue;
      if (Spacing.distance(tried, nursery) > Stage.SECTOR_SIZE) continue;
      
      float rating = tried.habitat().moisture() / 10f;
      rating -= Plan.rangePenalty(actor.base(), actor, tried);
      rating -= Plan.dangerPenalty(tried, actor);
      rating -= actor.world().ecology().biomassRating(tried);
      if (rating > bestRating) { bestRating = rating; picked = tried; }
    }
    
    return picked;
  }
  
  
  public static Flora findCutting(Actor actor, Target from) {
    final Target near = from == null ? actor : from;
    final Presences p = actor.world().presences;
    
    Flora cuts = null;
    cuts = (Flora) p.randomMatchNear(
      Flora.class, near, Stage.SECTOR_SIZE / 2
    );
    if (cuts == null) cuts = (Flora) p.nearestMatch(
      Flora.class, near, Stage.SECTOR_SIZE
    );
    if (cuts == null) return null;
    
    if (! cuts.inWorld()) {
      I.say("\nWARNING: FLORA TO CUT NOT IN WORLD: "+cuts);
      return null;
    }
    return cuts;
  }
}







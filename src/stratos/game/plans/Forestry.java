/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */



package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.maps.Flora;
import stratos.game.maps.Species;
import stratos.graphics.common.*;
import stratos.user.*;
import stratos.util.*;

import static stratos.game.actors.Qualities.*;
import static stratos.game.building.Economy.*;



public class Forestry extends Plan {
  
  final public static int
    STAGE_INIT     = -1,
    STAGE_GET_SEED =  0,
    STAGE_PLANTING =  1,
    STAGE_SAMPLING =  2,
    STAGE_RETURN   =  3,
    STAGE_DONE     =  4;
  private static boolean
    evalVerbose   = false,
    eventsVerbose = true;
  
  
  final Venue nursery;
  private int stage = STAGE_INIT;
  private Tile toPlant = null;
  private Flora toCut = null;
  
  
  public Forestry(Actor actor, Venue nursery) {
    super(actor, nursery);
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
    if (stage == STAGE_GET_SEED) {
      toPlant = findPlantTile(actor, nursery);
      if (toPlant == null) { abortBehaviour(); return false; }
      if (nursery.stocks.amountOf(seedMatch()) > 0) {
        this.stage = STAGE_GET_SEED;
      }
      else this.stage = STAGE_PLANTING;
    }
    if (stage == STAGE_SAMPLING) {
      toCut = findCutting(actor);
      if (toCut == null) { abortBehaviour(); return false; }
      this.stage = STAGE_SAMPLING;
    }
    return false;
  }
  
  
  private boolean configured() {
    if (stage != STAGE_INIT) return true;
    final float abundance = actor.world().ecology().globalBiomass();
    return configureFor(
      Rand.num() < abundance ? STAGE_SAMPLING : STAGE_GET_SEED
    );
  }
  
  
  final static Skill BASE_SKILLS[] = { CULTIVATION, HARD_LABOUR };
  final static Trait BASE_TRAITS[] = { NATURALIST, ENERGETIC };
  

  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (! configured()) return 0;
    final Target subject = toPlant == null ? toCut : toPlant;
    if (subject == null) return 0;
    
    final float priority = priorityForActorWith(
      actor, subject, ROUTINE,
      NO_HARM, FULL_COMPETITION,
      BASE_SKILLS, BASE_TRAITS,
      NO_MODIFIER, NORMAL_DISTANCE_CHECK, NO_FAIL_RISK,
      report
    );
    return priority;
  }
  
  
  public Behaviour getNextStep() {
    if (! configured()) return null;
    final boolean report = eventsVerbose && I.talkAbout == actor;
    
    if (stage == STAGE_GET_SEED) {
      final Action collects = new Action(
        actor, nursery,
        this, "actionCollectSeed",
        Action.LOOK, "Collecting seed"
      );
      return collects;
    }
    if (stage == STAGE_PLANTING) {
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
      final Action cuts = new Action(
        actor, toCut,
        this, "actionCutting",
        Action.BUILD, "Cutting"
      );
      cuts.setMoveTarget(Spacing.nearestOpenTile(toCut.origin(), actor));
      return cuts;
    }
    if (stage == STAGE_RETURN) {
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
    final boolean report = eventsVerbose && I.talkAbout == actor;
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
    if (actor.traits.test(CULTIVATION, MODERATE_DC, 5f)) growStage += 0.75f;
    if (actor.traits.test(HARD_LABOUR, ROUTINE_DC , 5f)) growStage += 0.75f;
    if (report) I.say("  Grow stage: "+growStage);
    if (growStage <= 0) return false;
    
    final Flora f = Flora.newGrowthAt(toPlant);
    if (f == null) return false;
    f.incGrowth(growStage * (Rand.num() + 1) / 4, toPlant.world, true);
    
    if (report) I.say("  SUCCESS! Grow stage: "+growStage);
    
    stage = STAGE_RETURN;
    return true;
  }
  
  
  public boolean actionCutting(Actor actor, Flora cut) {
    if (! actor.traits.test(CULTIVATION, SIMPLE_DC, 1.0f)) return false;
    final Item sample = Item.withReference(SAMPLES, cut);
    actor.gear.addItem(sample);
    cut.incGrowth(-0.5f, actor.world(), false);
    stage = STAGE_RETURN;
    return true;
  }
  
  
  public boolean actionReturnHarvest(Actor actor, Venue depot) {
    if (eventsVerbose) I.say("RETURNING SAMPLES TO "+depot);
    
    for (Item sample : actor.gear.matches(SAMPLES)) {
      if (! (sample.refers instanceof Flora)) continue;
      final Flora cut = (Flora) sample.refers;
      int stage = cut.growStage();
      depot.stocks.bumpItem(GREENS, stage * Rand.num() / Flora.MAX_GROWTH);
      depot.stocks.bumpItem(GENE_SEED, 1);
      actor.gear.removeItem(sample);
    }
    
    actor.gear.transfer(GENE_SEED, depot);
    stage = STAGE_DONE;
    return true;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("Performing Forestry");
  }
  
  
  
  /**  Utility methods for finding suitable plant/harvest targets-
    */
  public static Tile findPlantTile(Actor actor, Venue nursery) {
    Tile picked = null, tried;
    float bestRating = Float.NEGATIVE_INFINITY;
    
    for (int n = 10; n-- > 0;) {
      tried = Spacing.pickRandomTile(
        actor, World.SECTOR_SIZE / 2, actor.world()
      );
      tried = Spacing.nearestOpenTile(tried, actor);
      if (tried == null || ! Flora.canGrowAt(tried)) continue;
      if (Spacing.distance(tried, nursery) > World.SECTOR_SIZE) continue;
      
      float rating = tried.habitat().moisture() / 10f;
      rating -= Plan.rangePenalty(tried, actor);
      rating -= Plan.dangerPenalty(tried, actor);
      rating -= actor.world().ecology().biomassRating(tried);
      if (rating > bestRating) { bestRating = rating; picked = tried; }
    }
    
    return picked;
  }
  
  
  public static Flora findCutting(Actor actor) {
    if (Rand.yes()) return (Flora) actor.world().presences.randomMatchNear(
      Flora.class, actor, World.SECTOR_SIZE
    );
    return (Flora) actor.world().presences.nearestMatch(
      Flora.class, actor, World.SECTOR_SIZE
    );
    /*
    Series <Target> sample = actor.world().presences.sampleFromKey(
      actor, actor.world(), 10, null, Flora.class
    );
    float bestRating = Float.NEGATIVE_INFINITY;
    Flora picked = null;
    for (Target t : sample) {
      final Flora f = (Flora) t;
      if (f.growStage() < 2) continue;
      
      float rating = 0 - Spacing.distance(t, actor);
      final Tile o = f.origin();
      rating -= actor.base().dangerMap.sampleAt(o.x, o.y);
      rating += (f.growStage() - 2) * 10;
      if (rating > bestRating) { picked = f; bestRating = rating; }
    }
    return picked;
    //*/
  }
}







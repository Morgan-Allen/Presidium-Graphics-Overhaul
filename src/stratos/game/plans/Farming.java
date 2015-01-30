/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.game.maps.*;
import stratos.game.wild.Species;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



public class Farming extends Plan {
  
  
  private static boolean
    evalVerbose = false,
    stepVerbose = false;
  
  final Nursery nursery;
  
  
  
  public Farming(Actor actor, Nursery plantation) {
    super(actor, plantation, MOTIVE_JOB, NO_HARM);
    this.nursery = plantation;
  }
  
  
  public Farming(Session s) throws Exception {
    super(s);
    nursery = (Nursery) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(nursery);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Farming(other, nursery);
  }
  
  
  
  /**  Behaviour implementation-
    */
  final static Skill BASE_SKILLS[] = { HARD_LABOUR, CULTIVATION };
  final static Trait BASE_TRAITS[] = { ENERGETIC, NATURALIST };
  

  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if ((! hasBegun()) && Plan.competition(this, nursery, actor) > 0) {
      return 0;
    }
    
    final float min = returnHarvestAction(0) == null ? 0 :  ROUTINE;
    final float need = nursery.needForTending();
    
    final float priority = priorityForActorWith(
      actor, nursery, ROUTINE,
      (need - 0.5f) * ROUTINE, MILD_HELP,
      MILD_COMPETITION, NO_FAIL_RISK,
      BASE_SKILLS, BASE_TRAITS, PARTIAL_DISTANCE_CHECK,
      report
    );
    return Nums.max(min, priority);
  }
  
  
  public boolean finished() {
    final boolean f = super.finished();
    //if (f && verbose) I.sayAbout(actor, "FARMING COMPLETE");
    return f;
  }
  
  
  public Behaviour getNextStep() {
    final boolean report = stepVerbose && I.talkAbout == actor;
    
    if (report) {
      I.say("\nGETTING NEXT FARMING STEP");
      I.say("  Need for tending: "+nursery.needForTending());
      I.say("  Sum of harvest:   "+sumHarvest());
    }
    
    //  If you've harvested enough, bring it back to the depot-
    final Action returns = returnHarvestAction(5);
    if (returns != null) return returns;
    
    //  Find the next tile for seeding, tending or harvest.
    final boolean canPlant = nursery.needForTending() > 0 && canPlant();
    float minDist = Float.POSITIVE_INFINITY, dist;
    Tile toPlant = null;
    
    if (canPlant) for (Tile t : nursery.toPlant()) if (! t.reserved()) {
      
      final Crop c = nursery.plantedAt(t);
      if (c == null || c.needsTending()) {
        dist = Spacing.distance(actor, t);
        if (Spacing.edgeAdjacent(t, actor.origin())) dist /= 2;
        if (dist < minDist) { toPlant = t; minDist = dist; }
      }
    }
    
    if (report) {
      I.say("  Tiles claimed: "+nursery.toPlant().length);
      I.say("  TILE TO PLANT: "+toPlant);
    }
    
    //  If you're out of raw seed, and there's any in the nursery, and there's
    //  planting to be done, pick up some seed.
    if (toPlant != null && nextSeedNeeded() != null) {
      final Action pickup = new Action(
        actor, nursery,
        this, "actionCollectSeed",
        Action.REACH_DOWN, "Collecting seed"
      );
      return pickup;
    }
    
    if (toPlant != null) {
      Crop picked = nursery.plantedAt(toPlant);
      if (picked == null) {
        picked = new Crop(nursery, pickSpecies(toPlant));
        picked.setPosition(toPlant.x, toPlant.y, toPlant.world);
      }
      
      final String actionName, anim, desc;
      if (picked.blighted()) {
        actionName = "actionDisinfest";
        anim = Action.BUILD;
        desc = "Weeding "+picked;
      }
      else if (picked.growStage() >= Crop.MIN_HARVEST) {
        actionName = "actionHarvest";
        anim = Action.REACH_DOWN;
        desc = "Harvesting "+picked;
      }
      else {
        actionName = "actionPlant";
        anim = Action.BUILD;
        desc = "Planting "+picked;
      }
      final Action plants = new Action(
        actor, picked,
        this, actionName,
        anim, desc
      );
      plants.setMoveTarget(Spacing.nearestOpenTile(toPlant, actor));
      return plants;
    }
    
    //  If you've harvested anything else, deliver that back to the depot.
    return returnHarvestAction(0);
  }
  
  
  private float sumHarvest() {
    return 
      actor.gear.amountOf(CARBS) +
      actor.gear.amountOf(GREENS) +
      actor.gear.amountOf(PROTEIN);
  }
  
  
  private Action returnHarvestAction(int amountNeeded) {
    final boolean hasSample = actor.gear.amountOf(SAMPLES) > 0;
    final float sumHarvest = sumHarvest();
    
    if (hasSample && sumHarvest == 0 && amountNeeded == 0) {
      final Action returnSeed = new Action(
        actor, nursery,
        this, "actionReturnHarvest",
        Action.REACH_DOWN, "Returning seed"
      );
      return returnSeed;
    }
    
    if (sumHarvest <= amountNeeded && actor.gear.encumbrance() < 1) {
      return null;
    }
    final Action returnAction = new Action(
      actor, nursery,
      this, "actionReturnHarvest",
      Action.REACH_DOWN, "Returning harvest"
    );
    return returnAction;
  }
  
  
  private Item nextSeedNeeded() {
    for (Species s : Crop.ALL_VARIETIES) {
      final Item seed = Item.asMatch(SAMPLES, s);
      if (nursery.stocks.amountOf(seed) == 0) continue;
      if (actor.gear.amountOf(seed) > 0) continue;
      return seed;
    }
    return null;
  }
  
  
  private boolean canPlant() {
    if (! GameSettings.hardCore) return true;
    //
    //  TODO:  Key farming efforts off a particular crop type, so you can check
    //  for the presence of the right seed type.
    return
      nursery.stocks.amountOf(SAMPLES) > 0 ||
      actor.gear.amountOf(SAMPLES) > 0;
  }
  
  
  public boolean actionCollectSeed(Actor actor, Nursery nursery) {
    Item seed = nextSeedNeeded();
    if (seed == null) return false;
    actor.gear.addItem(nursery.stocks.bestSample(seed, 1));
    return true;
  }
  
  
  
  /**  Action Implementations-
    */
  public boolean actionPlant(Actor actor, Crop crop) {
    if (crop.origin().reserved()) return false;
    if (! crop.inWorld()) crop.enterWorld();
    
    //  Initial seed quality has a substantial impact on crop health.
    final Item seed = actor.gear.bestSample(
      Item.asMatch(SAMPLES, crop.species()), 0.1f
    );
    float plantDC = ROUTINE_DC;
    float health;
    
    if (seed != null) {
      health = 0.5f + (seed.quality / 2f);
      actor.gear.removeItem(seed);
    }
    else {
      if (GameSettings.hardCore) return false;
      plantDC += 5;
      health = 0;
    }
    
    //  So does expertise and elbow grease.
    health += actor.skills.test(CULTIVATION, plantDC, 1) ? 1 : 0;
    health += actor.skills.test(HARD_LABOUR, ROUTINE_DC, 1) ? 1 : 0;
    health *= Nursery.MAX_HEALTH_BONUS / 5;
    final Species s = pickSpecies(crop.origin());
    crop.seedWith(s, health);
    return true;
  }
  
  
  private Species pickSpecies(Tile t) {
    final Float chances[] = new Float[5];
    int i = 0;
    for (Species s : Crop.ALL_VARIETIES) {
      final Item seed = actor.gear.matchFor(Item.asMatch(SAMPLES, s));
      final float stocked = 50 + nursery.stocks.amountOf(Crop.yieldType(s));
      
      chances[i  ] = Crop.habitatBonus(t, s) / stocked;
      chances[i  ] *= 1 + (seed == null ? 0 : seed.quality);
      chances[i++] /= Item.MAX_QUALITY;
    }
    return (Species) Rand.pickFrom(Crop.ALL_VARIETIES, chances);
  }
  
  
  public boolean actionDisinfest(Actor actor, Crop crop) {
    final Item seed = actor.gear.bestSample(
      Item.asMatch(SAMPLES, crop.species()), 0.1f
    );
    int success = seed != null ? 2 : 0;
    if (actor.skills.test(CULTIVATION, MODERATE_DC, 1)) success++;
    if (actor.skills.test(HARD_LABOUR, ROUTINE_DC , 1)) success++;
    if (Rand.index(5) <= success) {
      crop.disinfest();
      if (seed != null) actor.gear.removeItem(seed);
    }
    return true;
  }
  
  
  public boolean actionHarvest(Actor actor, Crop crop) {
    final Item harvest = crop.yieldCrop();
    actor.gear.addItem(harvest);
    actionPlant(actor, crop);
    return true;
  }
  
  
  public boolean actionReturnHarvest(Actor actor, Venue depot) {
    actor.gear.transfer(CARBS, depot);
    actor.gear.transfer(GREENS, depot);
    actor.gear.transfer(PROTEIN, depot);
    for (Item seed : actor.gear.matches(SAMPLES)) {
      actor.gear.transfer(seed, depot);
      //actor.gear.removeItem(seed);
    }
    return true;
  }
  
  
  public void describeBehaviour(Description d) {
    if (super.needsSuffix(d, "Farming")) {
      d.append(" around ");
      d.append(nursery);
    }
  }
}







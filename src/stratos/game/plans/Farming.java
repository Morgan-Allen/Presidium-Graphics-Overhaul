/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.civic.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.game.maps.*;
import stratos.game.wild.Species;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;


//
//  TODO:  Key farming efforts off a particular crop type, so you can check
//  for the presence of the right seed type.

public class Farming extends Plan {
  
  
  private static boolean
    evalVerbose  = false,
    stepsVerbose = false;
  
  final static int
    STAGE_INIT = -1,
    STAGE_COLLECT = 0,
    STAGE_PLANT   = 1,
    STAGE_WEEDS   = 2,
    STAGE_HARVEST = 3,
    STAGE_RETURN  = 4;
  
  final Venue seedDepot;
  final Nursery nursery;
  private float diversity = 0, seedImpulse = 0;
  private int stage = STAGE_INIT;
  
  
  public Farming(Actor actor, Venue seedDepot, Nursery plantation) {
    super(actor, plantation, MOTIVE_JOB, NO_HARM);
    this.seedDepot = seedDepot ;
    this.nursery   = plantation;
  }
  
  
  public Farming(Session s) throws Exception {
    super(s);
    seedDepot   = (Venue  ) s.loadObject();
    nursery     = (Nursery) s.loadObject();
    diversity   = s.loadFloat();
    seedImpulse = s.loadFloat();
    stage       = s.loadInt  ();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(seedDepot  );
    s.saveObject(nursery    );
    s.saveFloat (diversity  );
    s.saveFloat (seedImpulse);
    s.saveInt   (stage      );
  }
  
  
  public Plan copyFor(Actor other) {
    return new Farming(other, seedDepot, nursery);
  }
  
  
  
  /**  Behaviour implementation-
    */
  final static Skill BASE_SKILLS[] = { HARD_LABOUR, CULTIVATION };
  final static Trait BASE_TRAITS[] = { ENERGETIC, NATURALIST };
  
  
  private Species pickSpecies(Tile t, boolean report) {
    if (report) I.say("\nPicking crop species...");
    
    final Pick <Species> pick = new Pick <Species> ();
    diversity = 0;
    
    for (Species s : Crop.ALL_VARIETIES) {
      final Item
        match   = Item.asMatch(GENE_SEED, s),
        carried = actor.gear.matchFor(match),
        atDepot = seedDepot.stocks.matchFor(match),
        seed    = carried == null ? atDepot : carried;
      
      if (seed == null) continue;
      else diversity++;
      
      float shortage = seedDepot.stocks.relativeShortage(Crop.yieldType(s));
      if (t == null) { pick.compare(s, shortage * seed.quality); continue; }
      
      final float chance = Crop.habitatBonus(t, s, seed) * (1 + shortage);
      if (report) {
        I.say("  Chance for "+s+" is "+chance);
        I.say("    Seed source: "+seed);
      }
      pick.compare(s, chance * Rand.num());
    }
    if (report) I.say("  Species picked: "+pick.result());
    
    diversity = Nums.clamp(diversity / 4, 0, 1);
    return pick.result();
  }
  
  
  private float sumHarvest() {
    return 
      actor.gear.amountOf(CARBS  ) +
      actor.gear.amountOf(GREENS ) +
      actor.gear.amountOf(PROTEIN);
  }
  
  
  protected float getPriority() {
    final boolean report = I.talkAbout == actor && evalVerbose;
    if ((! hasBegun()) && PlanUtils.competition(this, nursery, actor) > 0) {
      return 0;
    }
    
    if (pickSpecies(null, report) == null) seedImpulse = 0;
    else seedImpulse = nursery.needForTending() + diversity - 1;
    final float min = returnHarvestAction(0) == null ? 0 : ROUTINE;
    
    if (report) {
      I.say("\nEvaluating farming priority for "+this);
      I.say("  Seed impulse: "+seedImpulse);
      I.say("  Motive bonus:     "+motiveBonus());
    }
    
    final float priority = PlanUtils.jobPlanPriority(
      actor, this, seedImpulse, 1, 0, 0, BASE_TRAITS
    );
    return Nums.max(min, priority);
  }
  
  
  public Behaviour getNextStep() {
    final boolean report = I.talkAbout == actor && evalVerbose;
    
    if (report) {
      I.say("\nGETTING NEXT FARMING STEP AT "+nursery.origin());
      I.say("  Need for tending:  "+nursery.needForTending());
      I.say("  Sum of harvest:    "+sumHarvest());
      I.say("  Total encumbrance: "+actor.gear.encumbrance());
    }
    
    //  If you've harvested enough, bring it back to the depot-
    final Action returns = returnHarvestAction(5);
    if (returns != null) {
      if (report) I.say("  Returning harvest now...");
      return returns;
    }
    
    //  Find the next tile for seeding, tending or harvest.
    float minDist = Float.POSITIVE_INFINITY, dist;
    
    Tile toFarm = null;
    if (seedImpulse > 0) for (Tile t : nursery.reserved()) {
      if (! nursery.couldPlant(t)) continue;
      if (report) I.say("  Checking tile: "+t);
      
      final Crop c = nursery.plantedAt(t);
      if (c == null || c.needsTending()) {
        dist = Spacing.distance(actor, t);
        if (Spacing.edgeAdjacent(t, actor.origin())) dist /= 2;
        if (dist < minDist) { toFarm = t; minDist = dist; }
      }
    }
    
    if (report) {
      I.say("  Tiles claimed: "+nursery.reserved().length);
      I.say("  TILE TO PLANT: "+toFarm);
    }
    
    //  If you're out of raw seed, and there's any in the nursery, and there's
    //  planting to be done, pick up some seed.
    if (toFarm != null && nextSeedNeeded() != null) {
      final Action pickup = new Action(
        actor, seedDepot,
        this, "actionCollectSeed",
        Action.REACH_DOWN, "Collecting seed"
      );
      return pickup;
    }
    
    if (toFarm != null) {
      Crop picked = nursery.plantedAt(toFarm);
      final String actionName, anim, desc;
      
      if (picked != null && picked.blighted()) {
        actionName = "actionDisinfest";
        anim = Action.BUILD;
        desc = "Weeding "+picked;
      }
      else if (picked != null && picked.growStage() >= Crop.MIN_HARVEST) {
        actionName = "actionHarvest";
        anim = Action.REACH_DOWN;
        desc = "Harvesting "+picked;
      }
      else {
        picked = new Crop(nursery, pickSpecies(toFarm, report));
        picked.setPosition(toFarm.x, toFarm.y, toFarm.world);
        actionName = "actionPlant";
        anim = Action.BUILD;
        desc = "Planting "+picked;
      }
      
      final Action plants = new Action(
        actor, picked,
        this, actionName,
        anim, desc
      );
      final Tile open = Spacing.nearestOpenTile(toFarm, actor);
      plants.setMoveTarget(open);
      return plants;
    }
    
    //  If you've harvested anything else, deliver that back to the depot.
    return returnHarvestAction(0);
  }
  
  
  private Action returnHarvestAction(int amountNeeded) {
    if (! hasBegun()) return null;
    
    final boolean hasSeed = actor.gear.amountOf(GENE_SEED) > 0;
    final float sumHarvest = sumHarvest();
    
    if (hasSeed && sumHarvest == 0 && amountNeeded == 0) {
      final Action returnSeed = new Action(
        actor, seedDepot,
        this, "actionReturnHarvest",
        Action.REACH_DOWN, "Returning seed"
      );
      return returnSeed;
    }
    
    if (sumHarvest <= amountNeeded && actor.gear.encumbrance() < 1) {
      return null;
    }
    
    final Action returnAction = new Action(
      actor, seedDepot,
      this, "actionReturnHarvest",
      Action.REACH_DOWN, "Returning harvest"
    );
    return returnAction;
  }
  
  
  private Item nextSeedNeeded() {
    for (Species s : Crop.ALL_VARIETIES) {
      final Item seed = Item.asMatch(GENE_SEED, s);
      if (seedDepot.stocks.amountOf(seed) == 0) continue;
      if (actor.gear.amountOf(seed) > 0) continue;
      return seed;
    }
    return null;
  }
  
  

  /**  Action Implementations-
    */
  public boolean actionCollectSeed(Actor actor, Venue seedDepot) {
    actionReturnHarvest(actor, seedDepot);
    while (true) {
      final Item seed = nextSeedNeeded();
      if (seed == null) break;
      actor.gear.addItem(seedDepot.stocks.bestSample(seed, 1));
    }
    return true;
  }
  
  
  public boolean actionPlant(Actor actor, Crop crop) {
    //final boolean report = I.talkAbout == actor && stepsVerbose;
    if (! crop.inWorld()) {
      if (! nursery.couldPlant(crop.origin())) return false;
      crop.enterWorld();
    }
    
    //  Initial seed quality has a substantial impact on crop health.
    final Item seed = actor.gear.bestSample(
      Item.asMatch(GENE_SEED, crop.species()), 0.1f
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
    health *= Crop.MAX_HEALTH / 5;
    crop.seedWith(crop.species(), health);
    return true;
  }
  
  
  public boolean actionDisinfest(Actor actor, Crop crop) {
    int success = 1;
    if (actor.skills.test(CULTIVATION, MODERATE_DC, 1)) success++;
    if (actor.skills.test(HARD_LABOUR, ROUTINE_DC , 1)) success++;
    if (Rand.index(5) <= success) {
      crop.disinfest();
    }
    return true;
  }
  
  
  public boolean actionHarvest(Actor actor, Crop crop) {
    final boolean report = I.talkAbout == actor && stepsVerbose;
    final Item harvest = crop.yieldCrop();
    actor.gear.addItem(harvest);
    
    if (Rand.yes()) {
      final Tile at = crop.origin();
      crop = new Crop(nursery, pickSpecies(at, report));
      crop.setPosition(at.x, at.y, at.world);
    }
    actionPlant(actor, crop);
    return true;
  }
  
  
  public boolean actionReturnHarvest(Actor actor, Venue depot) {
    actor.gear.transfer(CARBS  , depot);
    actor.gear.transfer(GREENS , depot);
    actor.gear.transfer(PROTEIN, depot);
    for (Item seed : actor.gear.matches(GENE_SEED)) {
      actor.gear.removeItem(seed);
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







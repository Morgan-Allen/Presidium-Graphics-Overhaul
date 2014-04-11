/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */



package stratos.game.base ;
import stratos.game.building.* ;
import stratos.game.common.* ;
import stratos.game.actors.* ;
import stratos.game.planet.* ;
import stratos.graphics.common.* ;
import stratos.user.* ;
import stratos.util.* ;



public class Farming extends Plan implements Economy {
  
  
  private static boolean verbose = false ;
  
  final Plantation nursery ;
  
  
  Farming(Actor actor, Plantation plantation) {
    super(actor, plantation) ;
    this.nursery = plantation ;
  }
  
  
  public Farming(Session s) throws Exception {
    super(s) ;
    nursery = (Plantation) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(nursery);
  }
  
  
  
  
  final static Skill BASE_SKILLS[] = { HARD_LABOUR, CULTIVATION };
  final static Trait BASE_TRAITS[] = { SIMPLE, ENERGETIC, NATURALIST };
  
  
  public float priorityFor(Actor actor) {
    final boolean report = verbose && I.talkAbout == actor;
    if ((! hasBegun()) && nursery.belongs.personnel.assignedTo(this) > 0) {
      return 0;
    }
    
    final float min = sumHarvest() > 0 ? ROUTINE : 0;
    final float need = nursery.needForTending();
    if (need <= 0) return min;
    
    return priorityForActorWith(
      actor, nursery, ROUTINE,
      MILD_HELP, MILD_COMPETITION,
      BASE_SKILLS, BASE_TRAITS,
      (need - 0.5f) * ROUTINE, PARTIAL_DISTANCE_CHECK, NO_DANGER,
      report
    );
  }
  
  
  public boolean finished() {
    final boolean f = super.finished() ;
    if (f && verbose) I.sayAbout(actor, "FARMING COMPLETE") ;
    return f ;
  }
  
  
  public Behaviour getNextStep() {
    //
    //  If you've harvested enough, bring it back to the depot-
    Action action = returnHarvestAction(5) ;
    if (action != null) return action ;
    if (nursery.needForTending() == 0 || ! canPlant()) {
      if (verbose) I.sayAbout(actor, "Should return everything...") ;
      return returnHarvestAction(0) ;
    }
    
    //  If you're out of gene seed, and there's any in the nursery, pick up
    //  some more-
    if (nextSeedNeeded() != null) {
      final Action pickup = new Action(
        actor, nursery,
        this, "actionCollectSeed",
        Action.REACH_DOWN, "Collecting seed"
      ) ;
      return pickup ;
    }
    
    //  Find the next tile for seeding, tending or harvest.
    float minDist = Float.POSITIVE_INFINITY, dist ;
    Crop picked = null ;
    for (Plantation p : nursery.strip) for (Crop c : p.planted) {
      if (c != null && c.needsTending()) {
        dist = Spacing.distance(actor, c.tile) ;
        if (Spacing.edgeAdjacent(c.tile, actor.origin())) dist /= 2 ;
        if (dist < minDist) { picked = c ; minDist = dist ; }
      }
    }
    if (picked != null) {
      final String actionName, anim, desc ;
      if (picked.blighted()) {
        actionName = "actionDisinfest" ;
        anim = Action.BUILD ;
        desc = "Disinfesting "+picked ;
      }
      else if (picked.growStage() >= Crop.MIN_HARVEST) {
        actionName = "actionHarvest" ;
        anim = Action.REACH_DOWN ;
        desc = "Harvesting "+picked ;
      }
      else {
        actionName = "actionPlant" ;
        anim = Action.BUILD ;
        desc = "Planting "+picked ;
      }
      final Action plants = new Action(
        actor, picked,
        this, actionName,
        anim, desc
      ) ;
      plants.setMoveTarget(Spacing.nearestOpenTile(picked.tile, actor)) ;
      return plants ;
    }
    return null ;
  }
  
  
  private Species pickSpecies(Tile t, BotanicalStation parent) {
    final Float chances[] = new Float[5] ;
    int i = 0 ;
    for (Species s : Plantation.ALL_VARIETIES) {
      final float stocked = 50 + parent.stocks.amountOf(Crop.yieldType(s)) ;
      chances[i++] = Crop.habitatBonus(t, s, parent) / stocked ;
    }
    return (Species) Rand.pickFrom(Plantation.ALL_VARIETIES, chances) ;
  }
  
  
  private Item nextSeedNeeded() {
    for (Species s : Plantation.ALL_VARIETIES) {
      final Item seed = Item.asMatch(SAMPLES, s) ;
      if (nursery.stocks.amountOf(seed) == 0) continue ;
      if (actor.gear.amountOf(seed) > 0) continue ;
      return seed ;
    }
    return null ;
  }
  
  
  private boolean canPlant() {
    if (! GameSettings.hardCore) return true ;
    //
    //  TODO:  Key farming efforts off a particular crop type, so you can check
    //  for the presence of the right seed type.
    return
      nursery.stocks.amountOf(SAMPLES) > 0 ||
      actor.gear.amountOf(SAMPLES) > 0 ;
  }
  
  
  public boolean actionCollectSeed(Actor actor, Plantation nursery) {
    Item seed = nextSeedNeeded() ;
    if (seed == null) return false ;
    actor.gear.addItem(nursery.stocks.bestSample(seed, 1)) ;
    return true ;
  }
  
  
  public boolean actionPlant(Actor actor, Crop crop) {
    //
    //  Initial seed quality has a substantial impact on crop health.
    final Item seed = actor.gear.bestSample(
      Item.asMatch(SAMPLES, crop.species()), 0.1f
    ) ;
    float plantDC = ROUTINE_DC ;
    float health;
    
    if (seed != null) {
      health = 0.5f + (seed.quality / 2f) ;
      actor.gear.removeItem(seed) ;
    }
    else {
      if (GameSettings.hardCore) return false ;
      plantDC += 5 ;
      health = 0 ;
    }
    //
    //  So does expertise and elbow grease.
    health += actor.traits.test(CULTIVATION, plantDC, 1) ? 1 : 0 ;
    health += actor.traits.test(HARD_LABOUR, ROUTINE_DC, 1) ? 1 : 0 ;
    health *= Plantation.MAX_HEALTH_BONUS / 5;
    final Species s = pickSpecies(crop.tile, nursery.belongs);
    crop.seedWith(s, health);
    //
    //  Update and return-
    crop.parent.refreshCropSprites() ;
    crop.parent.checkCropStates() ;
    return true ;
  }
  
  
  public boolean actionDisinfest(Actor actor, Crop crop) {
    final Item seed = actor.gear.bestSample(
      Item.asMatch(SAMPLES, crop.species()), 0.1f
    ) ;
    int success = seed != null ? 2 : 0 ;
    if (actor.traits.test(CULTIVATION, MODERATE_DC, 1)) success++ ;
    if (actor.traits.test(HARD_LABOUR, ROUTINE_DC , 1)) success++ ;
    if (Rand.index(5) <= success) {
      crop.disinfest();
      if (seed != null) actor.gear.removeItem(seed) ;
    }
    return true ;
  }
  
  
  public boolean actionHarvest(Actor actor, Crop crop) {
    final Item harvest = crop.yieldCrop();
    actor.gear.addItem(harvest);
    actionPlant(actor, crop);
    return true ;
  }
  
  
  private float sumHarvest() {
    return 
      actor.gear.amountOf(CARBS) +
      actor.gear.amountOf(GREENS) +
      actor.gear.amountOf(PROTEIN) +
      (actor.gear.amountOf(SAMPLES) / 4) ;
  }
  
  
  private Action returnHarvestAction(int amountNeeded) {
    final float sumHarvest = sumHarvest() ;
    if (sumHarvest <= amountNeeded && actor.gear.encumbrance() < 1) {
      return null ;
    }
    final Action returnAction = new Action(
      actor, nursery.belongs,
      this, "actionReturnHarvest",
      Action.REACH_DOWN, "Returning harvest"
    ) ;
    return returnAction ;
  }
  
  
  public boolean actionReturnHarvest(Actor actor, Venue depot) {
    actor.gear.transfer(CARBS, depot) ;
    actor.gear.transfer(GREENS, depot) ;
    actor.gear.transfer(PROTEIN, depot) ;
    for (Item seed : actor.gear.matches(SAMPLES)) {
      actor.gear.removeItem(seed) ;
    }
    return true ;
  }
  
  
  public void describeBehaviour(Description d) {
    if (! describedByStep(d)) d.append("Farming") ;
    d.append(" around ") ;
    d.append(nursery) ;
  }
}







/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.planet ;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.planet.*;
import stratos.game.tactical.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



//
//  TODO:  You need to implement defence of home!


public abstract class Fauna extends Actor {
  
  
  
  /**  Field definitions, constructors, and save/load functionality-
    */
  final public static float
    PLANT_CONVERSION = 4.0f,
    MEAT_CONVERSION  = 4.0f ;
  private static boolean verbose = false ;
  
  
  final public Species species ;
  private float breedMetre = 0.0f, lastMigrateCheck = -1 ;
  
  
  public Fauna(Species species) {
    if (species == null) I.complain("NULL SPECIES!") ;
    this.species = species ;
    initStats() ;
    attachSprite(species.model.makeSprite()) ;
  }
  
  
  public Fauna(Session s) throws Exception {
    super(s) ;
    species = Species.ALL_SPECIES[s.loadInt()] ;
    breedMetre = s.loadFloat() ;
    lastMigrateCheck = s.loadFloat() ;
    initStats() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(species.ID) ;
    s.saveFloat(breedMetre) ;
    s.saveFloat(lastMigrateCheck) ;
  }
  
  
  public Species species() { return species ; }
  protected abstract void initStats() ;
  
  
  
  /**  Registering abundance with the ecology class-
    */
  public boolean enterWorldAt(int x, int y, World world) {
    if (! super.enterWorldAt(x, y, world)) return false ;
    world.ecology().impingeAbundance(this, World.STANDARD_DAY_LENGTH) ;
    return true ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (numUpdates % 10 == 0 && health.alive()) {
      world.ecology().impingeAbundance(this, 10);
      float crowding = Nest.crowdingFor(this) ;
      if (crowding == 1) crowding += 0.1f ;
      float fertility = (health.agingStage() - 0.5f) * health.energyLevel() ;
      float breedInc = (1 - crowding) * 10 / Nest.DEFAULT_BREED_INTERVAL ;
      breedInc *= Visit.clamp(fertility, 0, ActorHealth.AGE_MAX) ;
      breedMetre = Visit.clamp(breedMetre + breedInc, 0, 1) ;
    }
  }
  


  /**  Shared behavioural methods-
    */
  protected ActorMind initAI() {
    final Fauna actor = this ;
    
    return new ActorMind(actor) {
      protected Behaviour createBehaviour() {
        final Choice choice = new Choice(actor) ;
        addChoices(choice) ;
        return choice.weightedPick() ;
      }
      
      protected void updateAI(int numUpdates) {
        super.updateAI(numUpdates) ;
      }
      
      protected void addReactions(Element seen, Choice choice) {
        if (seen instanceof Actor) choice.add(nextDefence((Actor) seen)) ;
      }
      
      //
      //  We install some default relationships with other animals-
      public float relationValue(Actor other) {
        if (other instanceof Fauna) {
          final Fauna f = (Fauna) other ;
          if (f.species == species) return 0.25f ;
          if (f.species.type == Species.Type.BROWSER) return 0 ;
          if (f.species.predator()) return -0.5f ;
        }
        return -0.25f ;
      }
    } ;
  }
  
  
  protected Behaviour nextHunting() {
    final Choice c = new Choice(this);
    for (Element e : mind.awareOf()) {
      if (Hunting.validPrey(e, this, false)) {
        final Actor prey = (Actor) e;
        c.add(Hunting.asFeeding(this, prey));
      }
    }
    return c.pickMostUrgent();
  }
  
  
  protected Behaviour nextBrowsing() {
    final float range = Nest.forageRange(species) * 2 ;
    Target centre = mind.home() ;
    if (centre == null) centre = this ;
    
    final Batch <Flora> sampled = new Batch <Flora> () ;
    world.presences.sampleFromKey(centre, world, 5, sampled, Flora.class) ;
    /*
    final PresenceMap map = world.presences.mapFor(Flora.class) ;
    for (int n = 5 ; n-- > 0 ;) {
      final Flora f = (Flora) map.pickRandomAround(this, range) ;
      if (f != null) sampled.include(f) ;
    }
    //*/
    
    Flora picked = null ;
    float bestRating = 0 ;
    for (Flora f : sampled) {
      final float dist = Spacing.distance(this, f) ;
      if (dist > range) continue ;
      float rating = f.growth * Rand.avgNums(2) ;
      rating *= range / (range + dist) ;
      if (rating > bestRating) { picked = f ; bestRating = rating ; }
    }
    if (picked == null) return null ;
    
    float priority = ActorHealth.MAX_CALORIES - (health.energyLevel() + 0.1f) ;
    priority = priority * Action.PARAMOUNT - Plan.rangePenalty(this, picked) ;
    if (priority < 0) return null ;
    
    final Action browse = new Action(
      this, picked,
      this, "actionBrowse",
      Action.STRIKE, "Browsing"
    ) ;
    browse.setMoveTarget(Spacing.nearestOpenTile(picked.origin(), this)) ;
    browse.setPriority(priority) ;
    return browse ;
  }
  
  
  public boolean actionBrowse(Fauna actor, Flora eaten) {
    if (! eaten.inWorld()) return false ;
    
    I.sayAbout(this, "Am browsing at: "+eaten.origin()) ;
    float bite = 0.1f * eaten.growth * 2 * health.maxHealth() / 10 ;
    eaten.incGrowth(0 - bite, actor.world(), false) ;
    actor.health.takeCalories(bite * PLANT_CONVERSION, 1) ;
    return true ;
  }
  
  
  protected Behaviour nextResting() {
    Target restPoint = this.origin() ;
    final Nest nest = (Nest) this.mind.home() ;
    if (nest != null && nest.inWorld() && nest.structure.intact()) {
      restPoint = nest ;
    }
    final Action rest = new Action(
      this, restPoint,
      this, "actionRest",
      Action.FALL, "Resting"
    ) ;
    final float fatigue = health.fatigueLevel() ;
    if (fatigue < 0) return null ;
    final float priority = fatigue * Action.PARAMOUNT ;
    rest.setPriority(priority) ;
    return rest ;
  }
  
  
  public boolean actionRest(Fauna actor, Target point) {
    actor.health.setState(ActorHealth.STATE_RESTING) ;
    final Nest nest = (Nest) actor.mind.home() ;
    if (nest != point) return true ;
    return true ;
  }
  
  
  protected Behaviour nextMigration() {
    Target wandersTo = null ;
    String description = null ;
    float priority = 0 ;
    
    Nest newNest = null ;
    if (lastMigrateCheck == -1) lastMigrateCheck = world.currentTime() ;
    if (world.currentTime() - lastMigrateCheck > World.GROWTH_INTERVAL) {
      final boolean crowded = Nest.crowdingFor(this) > 0.5f ;
      if (verbose) I.sayAbout(this, "Crowded? "+crowded) ;
      newNest = crowded ? Nest.findNestFor(this) : null ;
      lastMigrateCheck = world.currentTime() ;
    }
    
    
    if (newNest != null && newNest != mind.home()) {
      if (verbose) I.sayAbout(this, "Found new nest! "+newNest.origin()) ;
      wandersTo = newNest ;
      description = "Migrating" ;
      priority = Action.ROUTINE ;
    }
    
    else {
      final Target centre = mind.home() == null ? this : mind.home() ;
      wandersTo = Spacing.pickRandomTile(
        centre, Nest.forageRange(species) / 2, world
      ) ;
      description = "Wandering" ;
      priority = Action.IDLE * Planet.dayValue(world) ;
    }
    if (wandersTo == null) return null ;
    
    final Action migrates = new Action(
      this, wandersTo,
      this, "actionMigrate",
      Action.LOOK, description
    ) ;
    migrates.setPriority(priority) ;
    if (! wandersTo.inWorld()) {
      migrates.setMoveTarget(Spacing.pickFreeTileAround(wandersTo, this)) ;
    }
    return migrates ;
  }
  
  
  public boolean actionMigrate(Fauna actor, Target point) {
    if (point instanceof Nest) {
      final Nest nest = (Nest) point ;
      if (Nest.crowdingFor(this) < 0.5f) return false ;
      if (Nest.crowdingFor(nest, species, world) > 0.5f) return false ;
      
      if (! nest.inWorld()) {
        nest.clearSurrounds() ;
        nest.enterWorld() ;
        nest.structure.setState(Structure.STATE_INTACT, 0.01f) ;
      }
      actor.mind.setHome(nest) ;
    }
    return true ;
  }
  
  
  protected Behaviour nextBuildingNest() {
    final Nest nest = (Nest) this.mind.home() ;
    if (nest == null) return null ;
    final float repair = nest.structure.repairLevel() ;
    if (repair >= 1) return null ;
    final Action buildNest = new Action(
      this, nest,
      this, "actionBuildNest",
      Action.STRIKE, "Repairing Nest"
    ) ;
    buildNest.setMoveTarget(Spacing.pickFreeTileAround(nest, this)) ;
    buildNest.setPriority(((1f - repair) * Action.ROUTINE)) ;
    return buildNest ;
  }
  
  
  public boolean actionBuildNest(Fauna actor, Nest nest) {
    if (! nest.inWorld()) nest.enterWorld() ;
    nest.structure.repairBy(nest.structure.maxIntegrity() / 10f) ;
    return true ;
  }
  
  
  protected Behaviour nextBreeding() {
    if (mind.home() == null) return null ;
    final Action breeds = new Action(
      this, mind.home(),
      this, "actionBreed",
      Action.FALL, "Breeding"
    ) ;
    return breeds ;
  }
  
  
  public boolean actionBreed(Fauna actor, Nest nests) {
    actor.breedMetre = 0 ;
    final int maxKids = 1 + (int) Math.sqrt(10f / health.lifespan()) ;
    for (int numKids = 1 + Rand.index(maxKids) ; numKids-- > 0 ;) {
      final Fauna young = species.newSpecimen() ;
      young.assignBase(this.base()) ;
      young.health.setupHealth(0, 1, 0) ;
      young.mind.setHome(nests) ;
      final Tile e = nests.mainEntrance() ;
      young.enterWorldAt(e.x, e.y, e.world) ;
      I.say("Giving birth to new "+actor.species.name+" at: "+nests) ;
    }
    return true ;
  }
  
  
  protected Behaviour nextDefence(Actor near) {
    return new Retreat(this) ;
  }
  
  
  protected void addChoices(Choice choice) {
    if (species.browser()) choice.add(nextBrowsing()) ;
    if (species.predator()) choice.add(nextHunting()) ;
    if (breedMetre >= 0.99f) choice.add(nextBreeding()) ;
    choice.add(nextResting()) ;
    choice.add(nextMigration()) ;
    choice.add(nextBuildingNest()) ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected float spriteScale() {
    return (float) Math.sqrt(health.ageLevel() + 0.5f) ;
  }
  
  
  public String fullName() {
    return health.agingDesc()+" "+species.name ;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(species.portrait, species.name);
  }
  
  
  public InfoPanel configPanel(InfoPanel panel, BaseUI UI) {
    if (panel == null) panel = new InfoPanel(
      UI, this, portrait(UI)
    );
    final Description d = panel.detail();
    
    d.append("Is: ") ;
    describeStatus(d) ;
    
    d.append("\nNests at: ") ;
    if (mind.home() != null) {
      d.append(mind.home()) ;
      final int BP = (int) (breedMetre * 100) ;
      d.append("\n  Breeding condition: "+BP+"%") ;
    }
    else d.append("No nest") ;
    
    d.append("\nCondition: ") ;
    final Batch <String> CD = health.conditionsDesc() ;
    if (CD.size() == 0) d.append("Okay") ;
    else d.appendList("", CD) ;
    
    //d.append("\n\nCombat strength: "+Combat.combatStrength(this, null)) ;
    
    d.append("\n\n") ;
    d.append(species.info, Colour.LIGHT_GREY) ;
    return panel;
  }
}








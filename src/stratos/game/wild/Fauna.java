/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



public abstract class Fauna extends Actor {
  
  
  /**  Field definitions, constructors, and save/load functionality-
    */
  private static boolean
    verbose = false;
  
  final static String
    FILE_DIR = "media/Actors/fauna/",
    LAIR_DIR = "media/Buildings/lairs and ruins/",
    XML_FILE = "FaunaModels.xml";
  
  final public static int
    DEFAULT_FORAGE_DIST = Stage.ZONE_SIZE / 2,
    PREDATOR_SEPARATION = Stage.ZONE_SIZE * 2,
    MIN_SEPARATION      = 2,
    
    BROWSER_TO_FLORA_RATIO   = 50,
    DEFAULT_BROWSER_SPECIES  = 4 ,
    PREDATOR_TO_PREY_RATIO   = 4 ,
    DEFAULT_PREDATOR_SPECIES = 2 ,
    
    DEFAULT_BREED_INTERVAL = Stage.STANDARD_DAY_LENGTH;
  
  final public static float
    PLANT_CONVERSION = 4.0f,
    MEAT_CONVERSION  = 8.0f,
    NEST_INTERVAL    = Stage.STANDARD_DAY_LENGTH;
  
  
  final public Species species;
  private float breedMetre = 0.0f, lastMigrateCheck = -1;
  
  
  public Fauna(Species species, Base base) {
    if (species == null) I.complain("NULL SPECIES!");
    this.species = species;
    mind.setVocation(species);
    initStats();
    attachSprite(species.modelSequence[0].makeSprite());
    assignBase(base);
  }
  
  
  public Fauna(Session s) throws Exception {
    super(s);
    species          = (Species) s.loadObject();
    breedMetre       = s.loadFloat();
    lastMigrateCheck = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(species         );
    s.saveFloat (breedMetre      );
    s.saveFloat (lastMigrateCheck);
  }
  
  
  public Species species() { return species; }
  protected abstract void initStats();
  
  
  
  /**  Registering abundance with the ecology class-
    */
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    
    final int period = 10;
    if (numUpdates % period == 0 && health.alive()) {
      final float breedRating = breedingReadiness(), breedInc;
      if (breedRating > 0) {
        breedInc = period * breedRating / DEFAULT_BREED_INTERVAL;
      }
      else {
        breedInc = period * -1f / DEFAULT_BREED_INTERVAL;
      }
      breedMetre = Nums.clamp(breedMetre + breedInc, 0, 1);
    }
  }
  
  
  protected float breedingReadiness() {
    final float crowding = NestUtils.crowding(this);
    float fertility = (health.agingStage() - 0.5f) * health.caloryLevel();
    return (1 - crowding) * Nums.clamp(fertility, 0, ActorHealth.AGE_MAX);
  }
  


  /**  Shared behavioural methods-
    */
  protected ActorMind initMind() {
    final Fauna fauna = this;
    return new ActorMind(fauna) {
      
      protected Choice createNewBehaviours(Choice choice) {
        if (choice == null) choice = new Choice(actor);
        fauna.addChoices(choice);
        return choice;
      }
      
      public void updateAI(int numUpdates) {
        super.updateAI(numUpdates);
      }
      
      protected void addReactions(Target seen, Choice choice) {
        fauna.addReactions(seen, choice);
      }
      
      protected void putEmergencyResponse(Choice choice) {
        fauna.putEmergencyResponse(choice);
      }
    };
  }
  
  
  protected ActorRelations initRelations() {
    return new ActorRelations(this) {
      //
      //  We install some default relationships with other animals, etc.-
      public float valueFor(Object object) {
        if (object == actor || object == actor.mind.home()) {
          return 1.0f;
        }
        else if (object instanceof Actor) {
          final Actor other = (Actor) object;
          if (other.species().animal()) {
            final Fauna f = (Fauna) other;
            if (f.species == species) return 0.25f;
            if (f.species.type == Species.Type.BROWSER) return 0;
            if (f.species.predator()) return -0.5f;
          }
          if (other.base() == actor.base()) return 0.5f;
          return -0.25f;
        }
        else return 0;
      }
      //
      //  We (unrealistically) assume that animals never learn.
      public float noveltyFor(Object object) {
        if (object instanceof Fauna || object == actor.base()) return 0;
        else return MAX_NOVELTY;
      }
    };
  }
  
  
  protected void addChoices(Choice choice) {
    for (Target t : senses.awareOf()) addReactions(t, choice);
    if (species.browser () ) choice.add(nextBrowsing());
    if (species.predator() ) choice.add(nextHunting ());
    if (breedMetre >= 0.99f) choice.add(nextBreeding());
    if (senses.haven() != null) choice.add(new Resting(this, senses.haven()));
    choice.add(nextMigration   ());
    choice.add(nextBuildingNest());
    choice.add(new Retreat(this));
  }
  
  
  protected void addReactions(Target seen, Choice choice) {
    if (seen instanceof Actor) choice.add(new Combat(this, (Actor) seen));
  }
  
  
  protected void putEmergencyResponse(Choice choice) {
    choice.add(new Retreat(this));
  }
  
  
  
  /**  Specific, generalised implementations for common behaviour types-
    */
  protected Behaviour nextHunting() {
    final Choice c = new Choice(this);
    for (Target e : senses.awareOf()) {
      if (Hunting.validPrey(e, this)) {
        final Actor prey = (Actor) e;
        c.add(Hunting.asFeeding(this, prey));
      }
    }
    return c.pickMostUrgent();
  }
  
  
  protected Behaviour nextBrowsing() {
    return Gathering.asBrowsing(this, NestUtils.forageRange(species));
  }
  
  
  
  //  TODO:  USE NESTING/FINDHOME FOR THIS
  
  protected Behaviour nextMigration() {
    final boolean report = verbose && I.talkAbout == this;
    Target wandersTo = null;
    String description = null;
    float priority = 0;
    
    final Target home = mind.home();
    Nest newNest = null;
    if (lastMigrateCheck == -1) lastMigrateCheck = world.currentTime();
    
    final float timeSinceCheck = world.currentTime() - lastMigrateCheck;
    final boolean homeless = ! (home instanceof Nest);
    if (report) {
      I.say("\nChecking migration for "+this);
      I.say("  Last check:  "+timeSinceCheck+"/"+NEST_INTERVAL);
      I.say("  Crowding is: "+NestUtils.crowding(this)+", homeless? "+homeless);
    }
    
    if (timeSinceCheck > NEST_INTERVAL || homeless) {
      final boolean crowded = homeless || NestUtils.crowding(this) > 0.5f;
      newNest = crowded ? NestUtils.findNestFor(this) : null;
      lastMigrateCheck = world.currentTime();
    }
    if (newNest != null && newNest != home) {
      wandersTo = newNest;
      description = "Migrating";
      priority = Action.ROUTINE;
    }
    else {
      final Target centre = mind.home() == null ? this : mind.home();
      wandersTo = Spacing.pickRandomTile(
        centre, NestUtils.forageRange(species) / 2, world
      );
      description = "Wandering";
      priority = Action.IDLE * (Planet.dayValue(world) + 1) / 2;
    }
    if (wandersTo == null) return null;
    
    final Action migrates = new Action(
      this, wandersTo,
      this, "actionMigrate",
      Action.LOOK, description
    );
    migrates.setPriority(priority);
    
    if (report) {
      I.say("  Wander point:    "+wandersTo);
      I.say("  Action priority: "+migrates.priorityFor(this));
      I.say("  Description:     "+description);
    }
    
    final Tile around = Spacing.pickFreeTileAround(wandersTo, this);
    if (around == null) return null;
    migrates.setMoveTarget(around);
    return migrates;
  }
  
  
  public boolean actionMigrate(Fauna actor, Target point) {
    if (point instanceof Nest) {
      final Nest nest = (Nest) point;
      
      if (NestUtils.crowding(species, nest, world) >= 1) {
        return false;
      }
      if (! nest.inWorld()) {
        if (! nest.canPlace()) {
          return false;
        }
        nest.assignBase(actor.base());
        nest.enterWorld();
        nest.structure.setState(Structure.STATE_INTACT, 0.01f);
      }
      actor.mind.setHome(nest);
    }
    return true;
  }
  
  
  protected Behaviour nextBuildingNest() {
    final Nest nest = (Nest) this.mind.home();
    if (nest == null) return null;
    final float repair = nest.structure.repairLevel();
    if (repair >= 0.99f) return null;
    final Action buildNest = new Action(
      this, nest,
      this, "actionBuildNest",
      Action.STRIKE, "Repairing Nest"
    );
    buildNest.setMoveTarget(Spacing.pickFreeTileAround(nest, this));
    float priority = Action.CASUAL + ((1f - repair) * Action.ROUTINE);
    buildNest.setPriority(priority);
    return buildNest;
  }
  
  
  public boolean actionBuildNest(Fauna actor, Nest nest) {
    if (! nest.inWorld()) nest.enterWorld();
    nest.structure.repairBy(nest.structure.maxIntegrity() / 10f);
    return true;
  }
  
  
  
  //  TODO:  CREATE SPECIAL PLAN FOR THIS AND SHARE WITH HUMANOIDS, ETC?
  
  protected Behaviour nextBreeding() {
    Target shelter = mind.home();
    if (shelter == null) shelter = senses.haven();
    if (shelter == null) return null;
    
    final Action breeds = new Action(
      this, shelter,
      this, "actionBreed",
      Action.FALL, "Breeding"
    );
    return breeds;
  }
  
  
  public boolean actionBreed(Fauna actor, Target at) {
    actor.breedMetre = 0;
    final int maxKids = 1 + (int) Nums.sqrt(10f / health.lifespan());
    
    for (int numKids = 1 + Rand.index(maxKids); numKids-- > 0;) {
      final Fauna young = (Fauna) species.sampleFor(base());
      final Tile e = at.world().tileAt(at);
      
      young.assignBase(this.base());
      young.health.setupHealth(0, 1, 0);
      young.enterWorldAt(e.x, e.y, e.world);
      
      if (I.logEvents()) {
        I.say("Giving birth to new "+actor.species.name+" at: "+at);
      }
      
      if (at instanceof Property) {
        final Property nest = (Property) at;
        young.mind.setHome(nest);
        young.goAboard(nest, world);
      }
    }
    return true;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected float spriteScale() {
    return (float) Nums.sqrt(health.ageLevel() + 0.5f);
  }
  
  
  public String fullName() {
    return health.agingDesc()+" "+species.name;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(species.portrait, species.name);
  }
  
  
  public SelectionPane configSelectPane(SelectionPane panel, BaseUI UI) {
    final SelectionPane p = ActorDescription.configSimplePanel(this, panel, UI);
    final int BP = (int) (breedMetre * 100);
    p.listing().append("\n  Breeding condition: "+BP+"%");
    return p;
  }
}






/*
protected Behaviour nextResting() {
  Target restPoint = this.origin();
  final Nest nest = (Nest) this.mind.home();
  if (nest != null && nest.inWorld() && nest.structure.intact()) {
    restPoint = nest;
  }
  final Action rest = new Action(
    this, restPoint,
    this, "actionRest",
    Action.FALL, "Resting"
  );
  final float fatigue = health.fatigueLevel();
  
  final float priority = Action.IDLE + (fatigue * Action.PARAMOUNT);
  rest.setPriority(priority);
  return rest;
}


public boolean actionRest(Fauna actor, Target point) {
  if (actor.health.fatigue() >= 1) {
    actor.health.setState(ActorHealth.STATE_RESTING);
  }
  return true;
}
//*/


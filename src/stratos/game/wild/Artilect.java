


package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.game.plans.Combat;
import stratos.game.plans.CombatUtils;
import stratos.game.plans.Patrolling;
import stratos.game.plans.Resting;
import stratos.game.plans.Retreat;
import stratos.game.plans.StretcherDelivery;
import stratos.game.tactical.*;
import stratos.game.civilian.*;
import stratos.graphics.common.*;
import stratos.graphics.solids.*;
import stratos.graphics.widgets.HUD;
import stratos.user.*;
import stratos.util.*;




public abstract class Artilect extends Actor {

  
  
  /**  Construction and save/load methods-
    */
  final static float
    FUEL_CELLS_REGEN = World.STANDARD_DAY_LENGTH;
  
  final static String
    FILE_DIR = "media/Actors/artilects/",
    XML_FILE = "ArtilectModels.xml";
  final public static ModelAsset
    MODEL_TRIPOD = MS3DModel.loadFrom(
      FILE_DIR, "Tripod.ms3d", Species.class,
      XML_FILE, "Tripod"
    ),
    MODEL_DEFENCE_DRONE = MS3DModel.loadFrom(
      FILE_DIR, "DefenceDrone.ms3d", Species.class,
      XML_FILE, "Defence Drone"
    ),
    MODEL_RECON_DRONE = MS3DModel.loadFrom(
      FILE_DIR, "ReconDrone.ms3d", Species.class,
      XML_FILE, "Recon Drone"
    ),
    MODEL_BLAST_DRONE = MS3DModel.loadFrom(
      FILE_DIR, "BlastDrone.ms3d", Species.class,
      XML_FILE, "Blast Drone"
    ),
    DRONE_MODELS[] = {
      MODEL_DEFENCE_DRONE, MODEL_RECON_DRONE, MODEL_BLAST_DRONE
    },
    
    MODEL_CRANIAL = MS3DModel.loadFrom(
      FILE_DIR, "Cranial.ms3d", Species.class,
      XML_FILE, "Cranial"
    ),
    MODEL_TESSERACT = MS3DModel.loadFrom(
      FILE_DIR, "Tesseract.ms3d", Species.class,
      XML_FILE, "Tesseract"
    )
 ;
  
  private static boolean verbose = false;
  
  final Species species;
  
  
  
  protected Artilect(Base base, Species s) {
    super();
    this.species = s;
    assignBase(base);
  }
  
  
  public Artilect(Session s) throws Exception {
    super(s);
    this.species = (Species) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(species);
  }
  
  
  public Species species() {
    return species;
  }
  

  protected ActorMind initAI() {
    final Artilect actor = this;
    return new ActorMind(actor) {
      
      protected Choice createNewBehaviours(Choice choice) {
        if (choice == null) choice = new Choice(actor);
        addChoices(choice);
        return choice;
      }
      
      protected void updateAI(int numUpdates) {
        super.updateAI(numUpdates);
      }
      
      protected void addReactions(Target seen, Choice choice) {
        if (seen instanceof Actor) choice.add(nextDefence((Actor) seen));
      }
    };
  }
  
  
  protected Relations initMemories() {
    final Artilect actor = this;
    
    return new Relations(this) {
      public float relationValue(Venue venue) {
        if (venue == actor.mind.home()) return 1;
        return super.relationValue(venue);
      }
      
      public float relationValue(Actor other) {
        if (actor.base() != null && other.base() == actor.base()) return 0.5f;
        if (other.health.artilect()) return 1.0f;
        return -1.0f;
      }
    };
  }
  
  
  protected Behaviour nextDefence(Actor near) {
    if (near == null) return null;
    final Plan defence = new Combat(this, near).setMotive(
      Plan.MOTIVE_EMERGENCY, Plan.ROUTINE
    );
    //I.sayAbout(this, "Have just seen: "+near);
    //I.sayAbout(this, "Defence priority: "+defence.priorityFor(this));
    return defence;
  }
  
  
  //  TODO:  Arrange for occasional scouting trips and active raids.
  //  (Intervals are slow, based on community size.)
  
  protected void addChoices(Choice choice) {
    //
    //  Patrol around your base and see off intruders.
    final boolean report = verbose && I.talkAbout == this;
    ///choice.isVerbose = report;
    final boolean
      isDrone   = this instanceof Drone,
      isTripod  = this instanceof Tripod,
      isCranial = this instanceof Cranial;
    
    //  Perform reconaissance or patrolling.
    //  Retreat and return to base.
    //  (Drone specialties.)
    
    final Employer home = mind.home();
    Element guards = home == null ? this : (Element) home;
    final float distance = Spacing.distance(this, guards) / World.SECTOR_SIZE;
    final float danger = CombatUtils.dangerAtSpot(this, this, null);
    
    final Plan patrol = Patrolling.aroundPerimeter(this, guards, world);
    if (isDrone) {
      final float urgency = Plan.ROUTINE + (distance * Plan.PARAMOUNT);
      patrol.setMotive(Plan.MOTIVE_DUTY, urgency);
      choice.add(patrol);
    }
    else if (isTripod) {
      patrol.setMotive(Plan.MOTIVE_DUTY, Plan.IDLE);
      choice.add(patrol);
    }
    
    if (isDrone && distance > 1) choice.add(new Retreat(this));
    
    //
    //  Launch an assault on a nearby settlement, if numbers are too large.
    //  Capture specimens and bring back to lair.
    //  (Tripod specialties.)
    //  TODO:  Use the BaseAI to decide on declaring raids on other settlements.
    final Plan assault = nextAssault();
    if (assault != null) {
      if (isTripod) assault.setMotive(Plan.MOTIVE_DUTY, Plan.PARAMOUNT);
      choice.add(assault);
    }
    if (
      (isTripod || isCranial) && danger == 0 &&
      home.personnel().numResident(Species.SPECIES_CRANIAL) > 0
    ) {
      //  TODO:  Restore this later, once Cybrid creation is sorted out.
      /*
      for (Target t : senses.awareOf()) if (t instanceof Human) {
        final Human other = (Human) t;
        if (other.health.conscious()) continue;
        if (report) I.say("  POTENTIAL SUBJECT: "+other);
        final Plan recovery = new StretcherDelivery(this, other, home);
        recovery.setMotive(Plan.MOTIVE_DUTY, Plan.URGENT);
        choice.add(recovery);
      }
      //*/
      for (Actor other : home.personnel().residents()) {
        if (other.health.conscious()) continue;
        if (report) I.say("  FALLEN ALLY: "+other);
        final Plan recovery = new StretcherDelivery(this, other, home);
        choice.add(recovery);
      }
    }
    //
    //  Experiment upon/dissect/interrogate/convert any captives.
    //  Perform repairs on another artilect, or refurbish a new model.
    //  (Cranial specialties.)
    if (isCranial && home instanceof Venue) {
      final Venue venue = (Venue) mind.home();
      for (Actor other : venue.personnel.residents()) {
        choice.add(new SpawnArtilect(this, other, venue));
      }
      final Ruins ruins = (Ruins) world.presences.randomMatchNear(
        Ruins.class, this, World.SECTOR_SIZE
      );
      choice.add(nextSpawning(this, ruins));
    }
    
    //
    //  Defend home site or retreat to different site (all).
    //  Respond to obelisk or tesseract presence (all).
    
    for (Target e : senses.awareOf()) if (e instanceof Actor) {
      choice.add(new Combat(this, (Actor) e));
    }
    if (mind.home() != null) {
      final Resting rest = new Resting(this, mind.home());
      if (isDrone) rest.setMotive(Plan.MOTIVE_DUTY, Plan.CASUAL);
      else rest.setMotive(Plan.MOTIVE_DUTY, Plan.ROUTINE);
      choice.add(rest);
    }
  }
  
  
  protected Plan nextAssault() {
    if (! (mind.home() instanceof Venue)) return null;
    final Venue lair = (Venue) mind.home();
    final Batch <Venue> sampled = new Batch <Venue> ();
    world.presences.sampleFromMap(this, world, 10, sampled, Venue.class);
    
    final int SS = World.SECTOR_SIZE;
    Venue toAssault = null;
    float bestRating = 0;
    
    //
    //  TODO:  Base priority on proximity to your lair, along with total
    //  settlement size.
    for (Venue venue : sampled) {
      if (venue.base() == this.base()) continue;
      final float crowding = 1 - venue.base().communitySpirit();
      
      final float dist = Spacing.distance(venue, lair);
      if (dist > Ruins.MIN_RUINS_SPACING) continue;
      
      float rating = SS / (SS + dist);
      rating += 1 - relations.relationValue(venue);
      if (rating > bestRating) { bestRating = rating; toAssault = venue; }
    }
    
    if (toAssault == null) return null;
    final Combat siege = new Combat(this, toAssault);
    
    return siege;
  }
  
  
  protected Plan nextSpawning(Actor actor, Ruins lair) {
    if (lair == null) return null;
    
    int mostSpace = 0;
    Species pick = null;
    for (Species s : Species.ARTILECT_SPECIES) {
      final int space = lair.spaceFor(s);
      if (space > mostSpace) { mostSpace = space; pick = s; }
    }
    if (pick == null) return null;
    
    final Artilect spawned = (Artilect) pick.newSpecimen(lair.base());
    return new SpawnArtilect(actor, spawned, lair);
  }
  
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates);
    final float fuelInc = 1f / FUEL_CELLS_REGEN;
    if (isDoing(Resting.class, null)) gear.addFuelCells(fuelInc);
  }
  
  
  
  
  /**  Rendering and interface methods-
    */
  public SelectionInfoPane configPanel(SelectionInfoPane panel, BaseUI UI) {
    if (panel == null) panel = new SelectionInfoPane(
      UI, this, null//, "STATUS", "SKILLS", "PROFILE"
    );
    final Description d = panel.detail();

    d.append("Is: ");
    super.describeStatus(d);
    
    d.append("\n\nCondition: ");
    final Batch <String> healthDesc = health.conditionsDesc();
    for (String desc : healthDesc) {
      d.append("\n  "+desc);
    }
    final Batch <Condition> conditions = traits.conditions();
    for (Condition c : conditions) {
      d.append("\n  ");
      d.append(traits.levelDesc(c));
    }
    if (healthDesc.size() == 0 && conditions.size() == 0) {
      d.append("\n  Okay");
    }

    d.append("\n\nSkills: ");
    for (Skill skill : traits.skillSet()) {
      final int level = (int) traits.traitLevel(skill);
      d.append("\n  "+skill.name+" "+level+" ");
      d.append(Skill.skillDesc(level), Skill.skillTone(level));
    }
    
    d.append("\n\n");
    d.append(helpInfo());
    return panel;
  }
  
  
  //  TODO:  Get rid of this once species are sorted out.
  protected abstract String helpInfo();
  
  
  protected static String nameWithBase(String base) {
    final StringBuffer nB = new StringBuffer(base);
    for (int n = 4; n-- > 0;) {
      if (Rand.yes()) nB.append((char) ('0' + Rand.index(10)));
      else nB.append((char) ('A'+Rand.index(26)));
    }
    return nB.toString();
  }
}



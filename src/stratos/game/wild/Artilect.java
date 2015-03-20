/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.game.economic.*;
import stratos.graphics.common.*;
import stratos.graphics.solids.*;
import stratos.user.*;
import stratos.util.*;
import stratos.game.politic.Mission;




public abstract class Artilect extends Actor {

  
  
  /**  Construction and save/load methods-
    */
  final static float
    FUEL_CELLS_REGEN = Stage.STANDARD_DAY_LENGTH;
  
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
  

  protected ActorMind initMind() {
    final Artilect artilect = this;
    return new ActorMind(artilect) {
      
      protected Choice createNewBehaviours(Choice choice) {
        if (choice == null) choice = new Choice(actor);
        artilect.addChoices(choice);
        return choice;
      }
      
      public void updateAI(int numUpdates) {
        super.updateAI(numUpdates);
      }
      
      protected void addReactions(Target seen, Choice choice) {
        artilect.addReactions(seen, choice);
      }
      
      protected void putEmergencyResponse(Choice choice) {
        artilect.putEmergencyResponse(choice);
      }
    };
  }
  
  
  protected ActorRelations initRelations() {
    return new ActorRelations(this) {
      
      public float valueFor(Object object) {
        if (object == actor || object == actor.mind.home()) {
          return 1.0f;
        }
        else if (object instanceof Accountable) {
          final Base belongs = ((Accountable) object).base();
          if (belongs == actor.base()) return 1.0f;
          return -1.0f;
        }
        else return 0;
      }
    };
  }
  
  
  protected void addReactions(Target seen, Choice choice) {
    if (seen instanceof Actor) {
      final Combat combat = new Combat(
        this, (Actor) seen, Combat.STYLE_EITHER, Combat.OBJECT_DESTROY
      );
      choice.add(combat);
    }
  }
  
  
  protected void putEmergencyResponse(Choice choice) {
    choice.add(new Retreat(this));
  }
  
  
  //  TODO:  Arrange for occasional scouting trips and active raids.
  //  (Intervals are slow, based on community size.)
  
  //  TODO:  Split this method out to the various subclasses and/or the ruins
  //  itself?
  
  protected void addChoices(Choice choice) {
    final boolean report = verbose && I.talkAbout == this;
    if (report) I.say("\n  Getting next behaviour for "+this);
    //
    //  Ascertain a few basic facts first-
    final boolean
      isDrone   = this instanceof Drone  ,
      isTripod  = this instanceof Tripod ,
      isCranial = this instanceof Cranial;
    final Property home = mind.home();
    Element guards = home == null ? this : (Element) home;
    //final float distance = Spacing.distance(this, guards) / Stage.SECTOR_SIZE;
    //
    //  Security and defence related tasks-
    if (! isCranial) {
      final Plan patrol = Patrolling.aroundPerimeter(this, guards, world);
      choice.add(patrol.setMotive(Plan.MOTIVE_JOB, Plan.IDLE));
      choice.add(JoinMission.attemptFor(this));
    }
    //if (distance > 1)
    choice.add(new Retreat(this));
    //
    //  Defend home site or retreat to different site (all).
    //  Respond to obelisk or tesseract presence (all).
    for (Target e : senses.awareOf()) if (e instanceof Actor) {
      choice.add(new Combat(this, (Actor) e));
    }
    if (home != null && ! home.staff().onShift(this)) {
      final Resting rest = new Resting(this, mind.home());
      if (isDrone) rest.setMotive(Plan.MOTIVE_JOB, Plan.CASUAL);
      else rest.setMotive(Plan.MOTIVE_JOB, Plan.ROUTINE);
      choice.add(rest);
    }
    //
    //  Launch an assault on a nearby settlement, if numbers are too large.
    //  Capture specimens and bring back to lair.
    //  (Tripod specialties.)
    if ((isTripod || isCranial) && home != null) {
      for (Target t : senses.awareOf()) {
        if (t instanceof Human) {
          final Human other = (Human) t;
          if (other.health.conscious()) continue;
          final Plan recovery = new StretcherDelivery(this, other, home);
          choice.add(recovery.setMotive(Plan.MOTIVE_JOB, Plan.CASUAL));
        }
        if (t instanceof Artilect) {
          final Artilect other = (Artilect) t;
          if (other.health.conscious()) continue;
          final Plan recovery = new StretcherDelivery(this, other, home);
          choice.add(recovery.setMotive(Plan.MOTIVE_JOB, Plan.CASUAL));
        }
      }
    }
    //
    //  Experiment upon/dissect/interrogate/convert any captives.
    //  Perform repairs on another artilect, or refurbish a new model.
    //  (Cranial specialties.)
    if (isCranial && home instanceof Venue) {
      final Venue venue = (Venue) mind.home();
      for (Actor other : venue.staff.residents()) {
        choice.add(new SpawnArtilect(this, other, venue));
      }
      final Ruins ruins = (Ruins) world.presences.randomMatchNear(
        Ruins.class, this, Stage.SECTOR_SIZE
      );
      choice.add(nextSpawning(this, ruins));
    }
  }
  
  
  protected Plan nextSpawning(Actor actor, Ruins lair) {
    if (lair == null) return null;
    final Pick <Species> pick = new Pick <Species> ();
    
    for (Species s : Species.ARTILECT_SPECIES) {
      final float rating = 1f - lair.crowdRating(null, s);
      if (rating <= 0) continue;
      pick.compare(s, rating);
    }
    if (pick.result() == null) return null;
    
    final Artilect spawned = (Artilect) pick.result().sampleFor(base());
    return new SpawnArtilect(actor, spawned, lair);
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    final float fuelInc = 1f / FUEL_CELLS_REGEN;
    if (isDoing(Resting.class, null)) gear.incPowerCells(fuelInc);
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public SelectionPane configPanel(SelectionPane panel, BaseUI UI) {
    return ActorDescription.configSimplePanel(this, panel, UI);
  }
  
  
  protected static String nameWithBase(String base) {
    final StringBuffer nB = new StringBuffer(base);
    for (int n = 4; n-- > 0;) {
      if (Rand.yes()) nB.append((char) ('0' + Rand.index(10)));
      else nB.append((char) ('A'+Rand.index(26)));
    }
    return nB.toString();
  }
}



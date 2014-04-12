


package stratos.game.wild ;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.planet.Species;
import stratos.game.tactical.*;
import stratos.graphics.common.*;
import stratos.graphics.solids.*;
import stratos.graphics.widgets.HUD;
import stratos.user.*;
import stratos.util.*;




public abstract class Artilect extends Actor {

  
  
  /**  Construction and save/load methods-
    */
  final static String
    FILE_DIR = "media/Actors/artilects/",
    XML_FILE = "ArtilectModels.xml" ;
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
  
  
  final Species species;
  
  
  
  protected Artilect(Base base, Species s) {
    super() ;
    this.species = s;
    assignBase(base);
  }
  
  
  public Artilect(Session s) throws Exception {
    super(s) ;
    this.species = (Species) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(species);
  }
  
  
  public Species species() {
    return species;
  }
  

  protected ActorMind initAI() {
    final Artilect actor = this ;
    return new ActorMind(actor) {
      
      protected Behaviour createBehaviour() {
        final Choice choice = new Choice(actor) ;
        addChoices(choice) ;
        return choice.pickMostUrgent() ;
      }
      
      protected void updateAI(int numUpdates) {
        super.updateAI(numUpdates) ;
      }
      
      protected void addReactions(Target seen, Choice choice) {
        if (seen instanceof Actor) choice.add(nextDefence((Actor) seen)) ;
      }
      
      public float relationValue(Actor other) {
        if (actor.base() != null && other.base() == actor.base()) return 0.5f ;
        if (other instanceof Artilect) return 1.0f ;
        return -1.0f ;
      }
    } ;
  }
  
  
  protected Behaviour nextDefence(Actor near) {
    if (near == null) return null ;
    final Plan defence = new Combat(this, near).setMotive(
      Plan.MOTIVE_EMERGENCY, Plan.ROUTINE
    );
    //I.sayAbout(this, "Have just seen: "+near) ;
    //I.sayAbout(this, "Defence priority: "+defence.priorityFor(this)) ;
    return defence ;
  }
  
  
  protected void addChoices(Choice choice) {
    
    //I.say("Creating new choices for "+this) ;
    //
    //  Patrol around your base and see off intruders.
    Element guards = mind.home() == null ? this : (Element) mind.home() ;
    final float distance = Spacing.distance(this, guards) / World.SECTOR_SIZE;
    
    final Plan p = Patrolling.aroundPerimeter(this, guards, world).setMotive(
      Plan.MOTIVE_DUTY, Plan.IDLE + (distance * Plan.PARAMOUNT)
    );
    ///I.say("Patrolling priority: "+p.priorityFor(this)) ;
    choice.add(p) ;
    
    for (Target e : senses.awareOf()) if (e instanceof Actor) {
      choice.add(new Combat(this, (Actor) e)) ;
    }
    
    //
    //
    //  Perform reconaissance or patrolling.
    //  Retreat and return to base.
    //  (Drone specialties.)
    
    //
    //  Launch an assault on a nearby settlement, if numbers are too large.
    //  Capture specimens and bring back to lair.
    //  (Tripod specialties.)
    choice.add(nextAssault()) ;
    choice.add(new Retreat(this)) ;
    
    //
    //  Experiment upon/dissect/interrogate/convert any captives.
    //  Perform repairs on another artilect, or refurbish a new model.
    //  (Cranial specialties.)
    
    //
    //  Defend home site or retreat to different site (all).
    //  Respond to obelisk or tesseract presence (all).
  }
  
  
  protected Behaviour nextAssault() {
    if (! (mind.home() instanceof Venue)) return null ;
    final Venue lair = (Venue) mind.home() ;
    final Batch <Venue> sampled = new Batch <Venue> () ;
    world.presences.sampleFromKey(this, world, 10, sampled, Venue.class) ;
    
    final int SS = World.SECTOR_SIZE ;
    Venue toAssault = null ;
    float bestRating = 0 ;
    
    //
    //  TODO:  Base priority on proximity to your lair, along with total
    //  settlement size.
    for (Venue venue : sampled) {
      if (venue.base() == this.base()) continue;
      final float crowding = 1 - venue.base().communitySpirit();
      
      final float dist = Spacing.distance(venue, lair);
      if (dist > Ruins.MIN_RUINS_SPACING) continue;
      
      float rating = SS / (SS + dist) ;
      rating += 1 - mind.relationValue(venue) ;
      if (rating > bestRating) { bestRating = rating ; toAssault = venue ; }
    }
    
    if (toAssault == null) return null ;
    final Combat siege = new Combat(this, toAssault) ;
    
    return siege ;
  }
  
  
  
  
  /**  Rendering and interface methods-
    */
  public InfoPanel configPanel(InfoPanel panel, BaseUI UI) {
    if (panel == null) panel = new InfoPanel(
      UI, this, null//, "STATUS", "SKILLS", "PROFILE"
    );
    final Description d = panel.detail();

    d.append("Is: ") ;
    super.describeStatus(d) ;
    
    d.append("\n\nCondition: ") ;
    final Batch <String> healthDesc = health.conditionsDesc() ;
    for (String desc : healthDesc) {
      d.append("\n  "+desc) ;
    }
    final Batch <Condition> conditions = traits.conditions() ;
    for (Condition c : conditions) {
      d.append("\n  ") ;
      d.append(traits.levelDesc(c)) ;
    }
    if (healthDesc.size() == 0 && conditions.size() == 0) {
      d.append("\n  Okay") ;
    }

    d.append("\n\nSkills: ") ;
    for (Skill skill : traits.skillSet()) {
      final int level = (int) traits.traitLevel(skill) ;
      d.append("\n  "+skill.name+" "+level+" ") ;
      d.append(Skill.skillDesc(level), Skill.skillTone(level)) ;
    }
    
    d.append("\n\n");
    d.append(helpInfo());
    return panel;
  }
  
  //  TODO:  Get rid of this once species are sorted out.
  protected abstract String helpInfo();
  
  
  
  
  protected static String nameWithBase(String base) {
    final StringBuffer nB = new StringBuffer(base) ;
    for (int n = 4 ; n-- > 0 ;) {
      if (Rand.yes()) nB.append((char) ('0' + Rand.index(10))) ;
      else nB.append((char) ('A'+Rand.index(26))) ;
    }
    return nB.toString() ;
  }
}






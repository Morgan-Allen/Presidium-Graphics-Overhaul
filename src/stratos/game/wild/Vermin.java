

package stratos.game.wild;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.actors.*;
import stratos.game.plans.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



//  TODO:  3 ACTIVITIES- HIDING, FINDING HOMES/BIRTHING, AND STEALING.

//  TODO:  ADAPT BROWSING FOR THIS PURPOSE?  Avrodils and Rem Leeches
//  have different feeding techniques.


public abstract class Vermin extends Actor {
  
  
  /**  Data fields, constructors, and save/load methods-
    */
  private static boolean verbose = false;
  
  final static String
    FILE_DIR = "media/Actors/vermin/",
    XML_FILE = "VerminModels.xml";
  
  final Species species;
  
  
  public Vermin(Species s, Base b) {
    super();
    this.species = s;
    mind.setVocation(s);
    assignBase(b);
    initStats();
    attachSprite(species.modelSequence[0].makeSprite());
  }
  

  public Vermin(Session s) throws Exception {
    super(s);
    species = (Species) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(species);
  }
  
  
  public Species species() {
    return species;
  }
  
  
  protected abstract void initStats();
  
  
  
  /**  Behaviour implementation-
    */
  protected ActorMind initMind() {
    final Vermin vermin = this;
    return new ActorMind(this) {
      
      protected Choice createNewBehaviours(Choice choice) {
        if (choice == null) choice = new Choice(actor);
        vermin.addChoices(choice);
        return choice;
      }
      
      protected void addReactions(Target m, Choice choice) {
        vermin.addReactions(m, choice);
      }
      
      protected void putEmergencyResponse(Choice choice) {
        vermin.putEmergencyResponse(choice);
      }
    };
  }
  
  
  protected void addReactions(Target seen, Choice choice) {
    //if (true) return;
    
    if (seen instanceof Actor) {
      final Combat combat = new Combat(
        this, (Actor) seen, Combat.STYLE_EITHER, Combat.OBJECT_EITHER
      );
      choice.add(combat);
      choice.add(Hunting.asFeeding(this, (Actor) seen));
    }
  }
  
  
  protected void putEmergencyResponse(Choice choice) {
    choice.add(new Retreat(this));
  }
  
  
  protected void addChoices(Choice choice) {
    final boolean report = I.talkAbout == this && verbose;
    if (report) I.say("\nCreating choices for "+this);
    
    choice.isVerbose = report;
    for (Target t : senses.awareOf()) addReactions(t, choice);
    
    //
    //  Finding home activities-
    final Exploring e = Exploring.nextWandering(this);
    if (e != null) choice.add(e.addMotives(Plan.MOTIVE_LEISURE, 1));
    
    choice.add(new Retreat(this));
    
    //  TODO:  Use vacated animal nests, ruins, or base-buildings.
    
    //
    //  Hiding activities-
    if (senses.haven() != null) {
      choice.add(new Resting(this, senses.haven()));
    }

    //  TODO:  ADAPT BROWSING FOR THIS PURPOSE?  Avrodils and Rem Leeches
    //  have different feeding techniques.
    //
    //  Stealing activities-
    final Venue home = (Venue) mind.home();
    if (home != null) {
      final Batch <Venue> venues = new Batch <Venue> ();
      world.presences.sampleFromMaps(this, world, 5, venues, Venue.class);
      
      for (Venue venue : venues) if (venue != home) {
        Item toSteal = Looting.pickItemFrom(
          venue, this, Economy.ALL_FOOD_TYPES
        );
        if (toSteal == null) continue;
        Looting l = new Looting(this, venue, Item.withAmount(toSteal, 1), home);
        float hungerBonus = health.hungerLevel() * Plan.PARAMOUNT;
        choice.add(l.addMotives(Plan.MOTIVE_EMERGENCY, hungerBonus));
      }
    }
    choice.add(Gathering.asBrowsing(this, Stage.ZONE_SIZE));
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return species.name;
  }
  
  
  public Composite portrait(HUD UI) {
    return null;
  }
  
  
  public SelectionPane configSelectPane(SelectionPane panel, HUD UI) {
    return ActorDescription.configSimplePanel(this, panel, UI);
  }
}









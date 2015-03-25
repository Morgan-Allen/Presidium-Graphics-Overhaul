

package stratos.game.wild;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.game.plans.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



//  TODO:  3 ACTIVITIES- HIDING, FINDING HOMES/BIRTHING, AND STEALING.

//  TODO:  ADAPT BROWSING FOR THIS PURPOSE?  Avrodils and Rem Leeches
//  have different feeding techniques.


//  Let's try simplicity for now.  It's basically a kind of raid-behaviour-
//  (do I need to write a mission for that, or can I build it into the AI?)

//  ...Actually, a Recovery mission might not be such a bad idea.  Try to
//  add that.  (Plus a Disguise/Spying mission!)


//  *  If offworld, how do you model that specific to each base?
//  (This is desirable for the sake of simplicity and balance-maintanance.)

//  Okay.  So, a given base has 'neighbours' above, below, and on each
//  side, along with the general surrounding territory.

//  *  They have a certain chance to lodge temporarily at a suitable home
//     and reproduce, or else to migrate offworld/to another lodge.
//     Impossible to nail down.

//  They'll consider this once per day by default (same as the spawn interval.)


//  TODO:  Should Vermin count as a type of Fauna?  With some extra behavioural
//         changes?

public abstract class Vermin extends Actor {
  
  
  /**  Data fields, constructors, and save/load methods-
    */
  final static String
    FILE_DIR = "media/Actors/vermin/",
    XML_FILE = "VerminModels.xml";
  
  final Species species;
  
  
  public Vermin(Species s, Base b) {
    super();
    this.species = s;
    assignBase(b);
    initStats();
    attachSprite(species.model.makeSprite());
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
    if (seen instanceof Actor) {
      final Combat combat = new Combat(
        this, (Actor) seen, Combat.STYLE_EITHER, Combat.OBJECT_EITHER
      );
      choice.add(combat);
    }
  }
  
  
  protected void putEmergencyResponse(Choice choice) {
    choice.add(new Retreat(this));
  }
  
  
  protected void addChoices(Choice choice) {
    boolean report = I.talkAbout == this && false;
    if (report) I.say("\nCreating choices for "+this);
    
    choice.isVerbose = report;
    //
    //  Finding home activities-
    choice.add(Exploring.nextWandering(this).addMotives(Plan.MOTIVE_LEISURE, 1));
    
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
      
      for (Venue venue : venues) {
        final Item toSteal = Looting.pickItemFrom(
          venue, this, Economy.ALL_FOOD_TYPES
        );
        choice.add(new Looting(this, venue, toSteal, home));
      }
    }
    choice.add(new Foraging(this, home));
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return species.name;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return null;
  }
  
  
  public SelectionPane configPanel(SelectionPane panel, BaseUI UI) {
    return ActorDescription.configSimplePanel(this, panel, UI);
  }
}









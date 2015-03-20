

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

public class Vermin extends Actor {
  
  
  /**  Data fields, constructors, and save/load methods-
    */
  final Species species;
  
  
  public Vermin(Base b, Species s) {
    super();
    this.species = s;
    assignBase(b);
  }
  

  public Vermin(Session s) throws Exception {
    super(s);
    species = (Species) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(species);
  }
  
  
  
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
    
    //  TODO:  3 ACTIVITIES- HIDING, FINDING HOMES/BIRTHING, AND STEALING.
    
    //  TODO:  ADAPT BROWSING FOR THIS PURPOSE?  Avrodils and Rem Leeches
    //  have different feeding techniques.
    
    
    final Venue home = (Venue) mind.home();
    
    final Batch <Venue> venues = new Batch <Venue> ();
    world.presences.sampleFromMaps(this, world, 5, venues, Venue.class);
    
    for (Venue venue : venues) {
      final Item toSteal = Looting.pickItemFrom(
        venue, this, Economy.ALL_FOOD_TYPES
      );
      choice.add(new Looting(this, venue, toSteal, home));
    }
    
    //  Let's try simplicity for now.  It's basically a kind of raid-behaviour-
    //  (do I need to write a mission for that, or can I build it into the AI?)
    
    //  ...Actually, a Recovery mission might not be such a bad idea.  Try to
    //  add that.  (Plus a Disguise/Spying mission!)
    
    
    //  *  If offworld, how do you model that specific to each base?
    //  (This is desirable for the sake of simplicity and balance-maintanance.)
    
    //  Okay.  So, a given base has 'neighbours' above, below, and on each
    //  side, along with the general surrounding territory.
    
    //  *  They have a certain chance to lodge at a suitable entry-point and
    //     start to reproduce, and to migrate 'offworld' or to another entry-
    //     point if things get crowded.  (So extermination can temporarily
    //     cull their numbers, and failing to exterminate won't mean you're
    //     overrun.)  Not too hard.
  }
  
  
  private Venue findHomePoint() {
    //  TODO:  Only accept service hatches or vacated animal nests.  (Use the
    //  animal behaviour?  Yeah.  You can add stealth to that.)
    return null;
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









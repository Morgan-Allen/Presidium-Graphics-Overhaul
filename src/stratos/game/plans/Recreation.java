


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.civilian.Pledge;
import stratos.game.common.*;
import stratos.game.maps.Planet;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.building.Economy.*;



public class Recreation extends Plan {
  
  
  /**  Data fields, construction and save/load methods-
    */
  private static boolean verbose = false;
  
  
  final static int
    PERFORM_TIME = World.STANDARD_HOUR_LENGTH,
    STAY_TIME    = World.STANDARD_HOUR_LENGTH;
  final public static int
    TYPE_ANY      = -1,
    TYPE_SONG     =  0,
    TYPE_EROTICS  =  1,
    TYPE_MEDIA    =  2,
    TYPE_LARP     =  3,
    TYPE_SPORT    =  4,
    TYPE_MEDITATE =  5;
  final static Trait ENJOYMENT_TRAITS[][] = {
    { },
    { INDULGENT },
    { },
    { CURIOUS, OUTGOING },
    { POSITIVE, DEFENSIVE },
    { IMPASSIVE, STUBBORN },
  };
  final static String RELAX_DESC[] = {
    "Listening to Music",
    "Enjoying a Private Dance",
    "Watching Media",
    "LARPing",
    "Enjoying Sport",
    "Meditating"
  };
  
  final Boarding venue;
  final int type;
  public float cost = 0, enjoyBonus = 1;
  
  
  public Recreation(Actor actor, Boarding venue, int performType) {
    super(actor, (Element) venue);
    this.venue = venue;
    this.type = performType;
  }


  public Recreation(Session s) throws Exception {
    super(s);
    venue = (Venue) s.loadTarget();
    type = s.loadInt();
    cost = s.loadFloat();
    enjoyBonus = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveTarget(venue);
    s.saveInt(type);
    s.saveFloat(cost);
    s.saveFloat(enjoyBonus);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Recreation(other, venue, type);
  }
  
  
  public boolean matchesPlan(Plan p) {
    if (! super.matchesPlan(p)) return false;
    final int pT = ((Recreation) p).type;
    if (pT == TYPE_ANY || this.type == TYPE_ANY) return true;
    return pT == this.type;
  }
  
  
  
  /**  Finding and evaluating targets-
    */
  protected float getPriority() {
    final boolean report = verbose && I.talkAbout == actor;
    
    if (cost > actor.gear.credits() / 2f) return 0;
    float modifier = NO_MODIFIER;
    modifier += Performance.performValueFor(venue, this);
    modifier += IDLE * rateComfort(venue, actor, this) / 10;
    modifier -= Pledge.greedLevel(actor, (int) cost) * ROUTINE;
    final float need = 1f - actor.health.moraleLevel();
    
    final float priority = priorityForActorWith(
      actor, venue, CASUAL * need,
      NO_HARM, NO_COMPETITION,
      NO_SKILLS, ENJOYMENT_TRAITS[type],
      modifier, NORMAL_DISTANCE_CHECK, NO_FAIL_RISK,
      report
    );
    if (report) {
      I.say("  Need for fun: "+need);
      I.say("  Final priority: "+priority);
    }
    return priority;
  }
  
  
  public static float rateComfort(Boarding at, Actor actor, Recreation r) {
    float performValue = Performance.performValueFor(at, r) / 10f;
    if (performValue < 0) return -1;
    //  TODO:  Average with ambienceVal for a Venue's structure?
    float ambience = actor.world().ecology().ambience.valueAt(at) / 2f;
    return performValue + ambience;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    if (priorityFor(actor) <= 0) return null;
    final Action relax = new Action(
      actor, venue,
      this, "actionRelax",
      Action.TALK_LONG, "Relaxing"
    );
    return relax;
  }
  
  
  public boolean actionRelax(Actor actor, Venue venue) {
    //
    //  Make any neccesary initial payment-
    float comfort = rateComfort(venue, actor, this);
    if (cost > 0 && comfort > 0) {
      venue.stocks.incCredits(cost);
      actor.gear.incCredits(0 - cost);
      cost = 0;
    }
    
    final float interval = 1f / World.STANDARD_DAY_LENGTH;
    if (actor.traits.traitLevel(SOMA_HAZE) > 0) {
      comfort++;
    }
    comfort += enjoyBonus;
    //  TODO:  Chat at random with other occupants (using the Dialogue class.)
    //
    //  TODO:  Have morale converge to a particular level based on surroundings,
    //  rather than gaining a continual increase!
    comfort *= interval;
    actor.health.adjustMorale(comfort);
    return true;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    final float performValue = Performance.performValueFor(venue, this);
    if (performValue > 0) d.append(RELAX_DESC[type]);
    else d.append("Relaxing");
    d.append(" at ");
    d.append(venue);
  }
}









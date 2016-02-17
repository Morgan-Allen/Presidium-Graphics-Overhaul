


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.base.Pledge;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.Planet;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



//  TODO:  Consider merging this with Resting.

public class Recreation extends Plan {
  
  
  /**  Data fields, construction and save/load methods-
    */
  private static boolean
    evalVerbose  = false,
    stepsVerbose = false;
  
  
  final static int
    PERFORM_TIME = Stage.STANDARD_HOUR_LENGTH,
    ENJOY_TIME   = Stage.STANDARD_DAY_LENGTH / 3;
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
  
  final Venue venue;
  final int type;
  protected float cost = 0, enjoyBonus = 1;
  
  
  public Recreation(Actor actor, Venue venue, int performType, float cost) {
    super(actor, (Element) venue, MOTIVE_LEISURE, NO_HARM);
    this.venue = venue;
    this.type = performType;
    this.cost = cost;
  }


  public Recreation(Session s) throws Exception {
    super(s);
    venue      = (Venue) s.loadObject();
    type       = s.loadInt();
    cost       = s.loadFloat();
    enjoyBonus = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(venue     );
    s.saveInt   (type      );
    s.saveFloat (cost      );
    s.saveFloat (enjoyBonus);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Recreation(other, venue, type, cost);
  }
  
  
  public boolean matchesPlan(Behaviour p) {
    if (! super.matchesPlan(p)) return false;
    final int pT = ((Recreation) p).type;
    if (pT == TYPE_ANY || this.type == TYPE_ANY) return true;
    return pT == this.type;
  }
  
  
  
  /**  Finding and evaluating targets-
    */
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (report) {
      I.say("\nEvaluating recreation priority for "+actor);
    }
    
    if (cost > actor.gear.allCredits() / 2f) return -1;
    if (cost > 0 && ! venue.openFor(actor)) return -1;
    
    final float
      needFun = 1f - actor.health.moraleLevel(),
      comfort = rateComfort(venue, actor, this),
      cashPen = actor.motives.greedPriority(cost),
      indulge = actor.traits.relativeLevel(INDULGENT) * CASUAL / 2;
    
    float priority = CASUAL + (needFun * (comfort + 1)) + indulge - cashPen;
    if (report) I.reportVars(
      "\nRecreation priority", "  ",
      "needFun", needFun,
      "comfort", comfort,
      "indulge", indulge,
      "cost"   , cost   ,
      "cashPen", cashPen
    );
    return priority;
  }
  
  
  public static float rateComfort(Venue at, Actor actor, Recreation r) {
    float performValue = Performance.performValueFor(at, r) / 10f;
    if (performValue < 0) performValue = 0;
    //
    //  TODO:  Average with ambienceVal for a Venue's structure?
    float ambience = actor.world().ecology().ambience.valueAt(at) / 2f;
    return (performValue + ambience) / 2;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    final Action relax = new Action(
      actor, venue,
      this, "actionRelax",
      Action.TALK_LONG, "Relaxing"
    );
    return relax;
  }
  
  
  public boolean actionRelax(Actor actor, Venue venue) {
    final boolean report = stepsVerbose && (
      I.talkAbout == actor || I.talkAbout == venue
    );
    if (report) {
      I.say("\n"+actor+" relaxing at "+venue);
    }
    //
    //  Make any necessary initial payment-
    float comfort = rateComfort(venue, actor, this);
    if (cost > 0 && comfort > 0) {
      venue.stocks.incCredits(cost);
      actor.gear.incCredits(0 - cost);
      cost = 0;
    }
    
    if (actor.traits.traitLevel(Condition.SOMA_HAZE) > 0) {
      comfort += 0.5f;
    }
    if (Resting.dineFrom(actor, venue)) {
      comfort += 0.5f;
    }
    final Actor chats = (Actor) Rand.pickFrom(venue.staff.visitors());
    if (chats != null) {
      comfort += DialogueUtils.tryChat(actor, chats, 0);
    }
    
    comfort *= enjoyBonus;
    
    if (report) {
      I.say("  Comfort level: "+comfort);
      I.say("  Morale level:  "+actor.health.moraleLevel());
    }
    //
    //  TODO:  Have morale converge to a particular level based on surroundings,
    //  rather than gaining a continual increase...
    comfort *= 1f / ENJOY_TIME;
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









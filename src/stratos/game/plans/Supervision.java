


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.base.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



//  TODO:  Use this to perform domestic chores, cleanup, and child-minding.

public class Supervision extends Plan {
  
  
  /**  Data fields, setup and save/load functions-
    */
  private static boolean
    evalVerbose  = false,
    stepsVerbose = false;
  
  public static enum Type {
    TYPE_OVERSIGHT,
    TYPE_DOMESTIC ,
    TYPE_INVENTORY;
  };
  final static float
    DEFAULT_EVAL_TIME   = Stage.STANDARD_HOUR_LENGTH / 2,
    UNIT_CATALOGUE_TIME = Stage.STANDARD_HOUR_LENGTH / 2;
  
  
  final Venue venue;
  final Type type;
  
  private Session.Saveable worksOn = null;
  private float beginTime = -1;
  
  
  
  public static Supervision oversight(Venue venue, Actor actor) {
    return new Supervision(actor, venue, Type.TYPE_OVERSIGHT);
  }
  
  
  public static Supervision inventory(StockExchange venue, Actor actor) {
    return new Supervision(actor, venue, Type.TYPE_INVENTORY);
  }
  
  
  public static Supervision domestic(Venue venue, Actor actor) {
    return new Supervision(actor, venue, Type.TYPE_DOMESTIC);
  }
  
  
  private Supervision(Actor actor, Venue supervised, Type type) {
    super(actor, supervised, false, NO_HARM);
    this.venue = supervised;
    this.type  = type      ;
  }
  
  
  public Supervision(Session s) throws Exception {
    super(s);
    this.venue     = (Venue) s.loadObject();
    this.type      = (Type) s.loadEnum(Type.values());
    this.beginTime = s.loadFloat();
    this.worksOn   = s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(venue    );
    s.saveEnum  (type     );
    s.saveFloat (beginTime);
    s.saveObject(worksOn  );
  }
  
  
  public Plan copyFor(Actor other) {
    return new Supervision(other, venue, type);
  }
  
  
  
  /**  Evaluating targets and priority-
    */
  final static Trait BASE_TRAITS[] = { RELAXED, IGNORANT, DUTIFUL };

  protected float getPriority() {
    if (! venue.staff.onShift(actor)) return 0;
    
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (report) {
      I.say("\nAssessing priority for supervision of "+venue);
      I.say("  Value of subject: "+actor.relations.valueFor(subject));
    }
    
    return priorityForActorWith(
      actor, venue,
      ROUTINE, NO_MODIFIER,
      NO_HARM, FULL_COMPETITION, NO_FAIL_RISK,
      NO_SKILLS, BASE_TRAITS, PARTIAL_DISTANCE_CHECK,
      report
    );
  }
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) I.say("\nGetting next supervision step: "+actor);
    
    final float time = actor.world().currentTime();
    if (beginTime == -1) beginTime = time;
    final float elapsed = time - beginTime;
    
    if (elapsed > DEFAULT_EVAL_TIME) {
      final Behaviour nextJob = venue.jobFor(actor);
      if (! (nextJob instanceof Supervision)) {
        I.say("  Supervision complete!");
        interrupt(INTERRUPT_CANCEL);
        return null;
      }
      else beginTime = time;
    }
    else this.worksOn = null;
    
    if (type == Type.TYPE_INVENTORY) {
      final StockExchange exchange = (StockExchange) venue;
      for (Item item : venue.stocks.allItems()) {
        if (exchange.catalogueLevel(item.type) >= 1) continue;
        worksOn = item.type;
        break;
      }
      if (worksOn != null) {
        if (report) I.say("  Taking inventory...");
        final Action inventory = new Action(
          actor, exchange,
          this, "actionInventory",
          Action.LOOK, "Arranging inventory"
        );
        return inventory;
      }
    }
    
    if (type == Type.TYPE_DOMESTIC) {
      //  TODO:  Add child-minding, et cetera.
    }

    if (report) I.say("  Doing normal oversight...");
    final Action supervise = new Action(
      actor, venue,
      this, "actionSupervise",
      Action.LOOK, "Supervising"
    );
    return supervise;
  }
  
  
  public boolean actionSupervise(Actor actor, Venue venue) {
    
    //  If you have any items demanded by the venue, put them away-
    for (Item i : actor.gear.allItems()) {
      if (i.refers != null || i.type.form != FORM_MATERIAL) continue;
      if (venue.stocks.demandFor(i.type) > 0) actor.gear.transfer(i, venue);
    }
    return true;
  }
  
  
  public boolean actionInventory(Actor actor, StockExchange venue) {
    float inc = 1.0f;
    inc += actor.skills.test(ACCOUNTING, ROUTINE_DC  , 1) ? 0 : 1;
    inc *= actor.skills.test(ACCOUNTING, DIFFICULT_DC, 1) ? 1 : 2;
    inc /= 2 * UNIT_CATALOGUE_TIME;
    if (venue.adjustCatalogue((Traded) worksOn, inc)) return true;
    return false;
  }
  
  
  
  public void describeBehaviour(Description d) {
    
    if (type == Type.TYPE_DOMESTIC) {
      d.append("Keeping house at ");
      d.append(venue);
      return;
    }
    if (type == Type.TYPE_INVENTORY && worksOn != null) {
      final Traded good = (Traded) worksOn;
      final float CL = ((StockExchange) venue).catalogueLevel(good);
      
      d.append("Arranging inventory (");
      d.append(good);
      d.append(") at ");
      d.append(venue);
      if (CL > 0) d.append(" ("+((int) (CL * 100))+"% done)");
      return;
    }
    
    d.append("Supervising ");
    d.append(venue);
  }
}






package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;




//  TODO:  Just have immigrants arrive on a world looking for work, with the
//  likelihood based on supply/demand, and let them fend for themselves on
//  arrival?

public class FindWork extends Plan {
  
  
  private static boolean
    verbose = false;
  
  private Background position;
  private Property employer;
  private float rating  = 0;
  private int   hireFee = 0;
  
  
  private FindWork(Actor actor, Background position, Property employer) {
    super(actor, actor, true, NO_HARM);
    this.position = position;
    this.employer = employer;
  }
  
  
  public FindWork(Session s) throws Exception {
    super(s);
    position = (Background) s.loadObject();
    employer = (Property  ) s.loadObject();
    rating   = s.loadFloat();
    hireFee  = s.loadInt  ();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(position);
    s.saveObject(employer);
    s.saveFloat (rating  );
    s.saveInt   (hireFee );
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  public Background position() {
    return position;
  }
  
  
  public Property employer() {
    return employer;
  }
  
  
  public int hiringFee() {
    return hireFee;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected float getPriority() {
    final boolean report = verbose && I.talkAbout == actor;
    final float priority = Nums.clamp(URGENT * rating, 0, URGENT);
    if (report) {
      I.say("\nGetting priority for work application: "+actor);
      I.say("  Venue:    "+employer);
      I.say("  Position: "+position);
      I.say("  Rating:   "+rating  );
      I.say("  Hire for: "+hireFee );
      I.say("  Priority: "+priority);
    }
    return priority;
  }
  
  
  protected Behaviour getNextStep() {
    final boolean report = verbose && I.talkAbout == actor;
    if (report) I.say("\nGetting next find-work step for "+actor);
    
    if (employer == actor.mind.work() || ! canApply()) {
      if (report) I.say("  Cannot apply at: "+employer);
      return null;
    }
    
    if (report) I.say("  Applying at "+employer);
    final Action applies = new Action(
      actor, employer,
      this, "actionApplyTo",
      Action.LOOK, "Applying for work"
    );
    return applies;
  }
  
  
  private boolean canApply() {
    return
      position != null && employer.inWorld() &&
      employer.structure().intact() &&
      employer.crowdRating(actor, position) < 1 &&
      ! employer.staff().applications().includes(this);
  }
  
  
  public boolean actionApplyTo(Actor client, Property best) {
    if (! canApply()) return false;
    confirmApplication();
    return true;
  }
  
  
  public boolean matchesPlan(Plan other) {
    if (! super.matchesPlan(other)) return false;
    final FindWork a = (FindWork) other;
    return
      a.actor    == actor    &&
      a.position == position &&
      a.employer == employer;
  }
  
  
  public boolean finished() {
    return false;
  }
  
  
  public void confirmApplication() {
    if (! canApply()) return;
    if (actor.mind.work() == employer) return;
    employer.staff().setApplicant(this, true);
  }
  
  
  public void cancelApplication() {
    if (! canApply()) return;
    employer.staff().setApplicant(this, false);
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    d.append("Applying for work as ");
    d.append(position+" at ");
    d.append(employer);
  }
  
  
  
  /**  Helper methods for finding other employment-
    */
  public static FindWork attemptFor(Actor actor, Background b, Base at) {
    final Stage world = at.world;
    final Tile around = world.tileAt(
      Rand.index(world.size),
      Rand.index(world.size)
    );
    Property pick = (Property) world.presences.randomMatchNear(b, around, -1);
    if (pick == null || pick.base() != at) {
      return null;
    }
    return attemptFor(actor, pick);
  }
  
  
  //  NOTE:  The idea here is those you really only ever instance a single
  //  FindWork plan for a given actor.  This is why it gets 'assigned to do'
  //  automatically, and never actually finishes.
  public static FindWork attemptFor(Actor actor, Property at) {
    final boolean report = verbose && (
      I.talkAbout == actor || I.talkAbout == at
    );
    if (at.careers() == null) return null;
    if (report) I.say("\n"+actor+" checking for career opportunities at "+at);
    
    FindWork main = (FindWork) actor.matchFor(FindWork.class);
    if (main == null) {
      main = new FindWork(actor, null, null);
      actor.mind.assignToDo(main);
    }
    
    final Pick <FindWork> pick = new Pick <FindWork> ();
    if (main.position != null) {
      pick.compare(main, main.rateOpening(main.position, main.employer) * 1.5f);
    }
    
    final Property work = actor.mind.work();
    if (work != null) {
      final FindWork app = new FindWork(actor, actor.vocation(), work);
      pick.compare(app, app.rateOpening(app.position, app.employer) * 1.5f);
    }
    
    for (Background c : at.careers()) {
      final FindWork app = new FindWork(actor, c, at);
      pick.compare(app, main.rateOpening(app.position, app.employer));
    }

    final FindWork app = pick.result();
    if (app != null && app.position != work) {
      main.position = app.position;
      main.employer = app.employer;
      main.rating   = pick.bestRating();
      main.calcHiringFee();
      if (report) {
        I.say("  Most promising: "+main.position);
        I.say("  Venue:          "+main.employer);
        I.say("  Rating:         "+main.rating  );
      }
    }
    return main;
  }
  
  
  public static Background ambitionOf(Actor actor) {
    final FindWork finding = (FindWork) actor.matchFor(FindWork.class);
    if (finding == null || finding.position == null) return null;
    return finding.position;
  }
  
  
  private float rateOpening(Background position, Property at) {
    float rating = Career.ratePromotion(position, actor);
    rating *= actor.relations.valueFor(at);
    //  TODO:  Also impact through wage-rate and area living conditions.
    rating /= 1f + at.staff().applications().size();
    return rating;
  }
  
  
  private int calcHiringFee() {
    int transport = 0, incentive = 0, guildFees = 0;
    
    //  TODO:  Allow the player to set wages in a similar manner to setting
    //  goods' import/export levels.
    if (position.standing < 0) return 0;
    guildFees += Backgrounds.HIRE_COSTS[position.standing];
    
    if (actor.inWorld()) {
      guildFees = 0;
    }
    else {
      //  TODO:  ...This could be much higher, depending on origin point.
      transport += 100;
    }
    if (employer instanceof Venue) {
      final Venue venue = (Venue) employer;
      if (venue.staff.numHired(position) == 0) {
        guildFees /= 2;
        transport /= 2;
      }
    }
    
    //  TODO:  Set up incentive to join the settlement, based on settlement
    //  ratings and legislation.
    
    return this.hireFee = guildFees + transport + incentive;
  }
  
}


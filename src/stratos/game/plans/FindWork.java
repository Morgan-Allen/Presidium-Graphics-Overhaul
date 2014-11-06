


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.building.Economy.*;




//  TODO:  Just have immigrants arrive on a world looking for work, with the
//  likelihood based on supply/demand, and let them fend for themselves on
//  arrival?

public class FindWork extends Plan {
  
  
  private static boolean verbose = false;
  
  private Background position;
  private Employer employer;
  private float rating  = 0;
  private int   hireFee = 0;
  
  
  private FindWork(Actor actor, Background position, Employer employer) {
    super(actor, actor, true);
    this.position = position;
    this.employer = employer;
  }
  
  
  public FindWork(Session s) throws Exception {
    super(s);
    position = (Background) s.loadObject();
    employer = (Employer  ) s.loadObject();
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
  
  
  public Employer employer() {
    return employer;
  }
  
  
  public int hiringFee() {
    return hireFee;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected float getPriority() {
    return Visit.clamp(URGENT * rating, 0, URGENT);
  }
  
  
  protected Behaviour getNextStep() {
    if (employer == actor.mind.work() || ! canApply()) return null;
    
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
      employer.numOpenings(position) > 0;
  }
  
  
  public boolean actionApplyTo(Actor client, Employer best) {
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
    employer.personnel().setApplicant(this, true);
  }
  
  
  public void cancelApplication() {
    if (! canApply()) return;
    employer.personnel().setApplicant(this, false);
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
    Employer pick = (Employer) world.presences.randomMatchNear(b, around, -1);
    if (pick.base() != at) return null;
    return attemptFor(actor, pick);
  }
  
  
  //  NOTE:  The idea here is those you really only ever instance a single
  //  FindWork plan for a given actor.  This is why it gets 'assigned to do'
  //  automatically, and never actually finishes.
  
  public static FindWork attemptFor(Actor actor, Employer at) {
    FindWork find = (FindWork) actor.matchFor(FindWork.class);
    if (find == null) {
      find = new FindWork(actor, null, null);
      actor.mind.assignToDo(find);
    }
    
    final Pick <FindWork> pick = new Pick <FindWork> ();
    if (find.position != null) {
      pick.compare(find, find.rateOpening(find.position, find.employer) * 1.5f);
    }
    
    final Employer work = actor.mind.work();
    if (work != null) {
      final FindWork app = new FindWork(actor, actor.vocation(), work);
      pick.compare(app, app.rateOpening(app.position, app.employer) * 1.5f);
    }
    
    for (Background c : at.careers()) {
      final FindWork app = new FindWork(actor, c, at);
      pick.compare(app, find.rateOpening(app.position, app.employer));
    }
    
    if (pick.result() != null) {
      final FindWork app = pick.result();
      find.position = app.position;
      find.employer = app.employer;
      find.rating   = app.rating  ;
      find.calcHiringFee();
    }
    return find;
  }
  
  
  public static Background ambitionOf(Actor actor) {
    final FindWork finding = (FindWork) actor.matchFor(FindWork.class);
    if (finding == null || finding.position == null) return null;
    return finding.position;
  }
  
  
  private float rateOpening(Background position, Employer at) {
    float rating = Career.ratePromotion(position, actor);
    rating *= actor.relations.valueFor(at);
    //  TODO:  Also impact through wage-rate and area living conditions.
    rating /= 1f + at.personnel().applications().size();
    return rating;
  }
  
  
  private int calcHiringFee() {
    int transport = 0, incentive = 0, guildFees = 0;
    
    //  TODO:  Allow the player to set wages in a similar manner to setting
    //  goods' import/export levels.
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
      if (venue.personnel.numHired(position) == 0) {
        guildFees /= 2;
        transport /= 2;
      }
    }
    
    //  TODO:  Set up incentive to join the settlement, based on settlement
    //  ratings and legislation.
    
    return this.hireFee = guildFees + transport + incentive;
  }
  
}


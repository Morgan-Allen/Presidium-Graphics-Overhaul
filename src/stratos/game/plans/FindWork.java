/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.base.BaseCommerce;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



public class FindWork extends Plan {
  
  
  public static boolean
    rateVerbose  = false,
    extraVerbose = false,
    evalVerbose  = false,
    stepsVerbose = false,
    offworldOnly = false;
  
  final static float
    SWITCH_THRESHOLD = 1.5f;
  
  private Background position;
  private Property employer;
  private float rating  = 0;
  private int   hireFee = 0;
  
  
  private FindWork(Actor actor, Background position, Property employer) {
    super(actor, actor, MOTIVE_PERSONAL, NO_HARM);
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
  
  
  public boolean matchesPlan(Behaviour other) {
    if (! super.matchesPlan(other)) return false;
    final FindWork a = (FindWork) other;
    return
      a.actor    == actor    &&
      a.position == position &&
      a.employer == employer;
  }
  
  
  public boolean finished() {
    return false;
    //return wasHired();
  }
  
  
  public boolean requiresApproval() {
    if (position == null || employer == null) return false;
    else if (position.standing >= Backgrounds.CLASS_AGENT) return true;
    else if (actor.mind.vocation() != position) return true;
    else if (employer.staff().numOpenings(position) <= 0) return true;
    else return false;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor && hasBegun();
    if (position == null || employer == null || wasHired()) return -1;
    rating = rateOpening(this, false, report);
    final float priority = Nums.clamp(ROUTINE * rating, 0, URGENT);
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
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (! canApply()) {
      if (report) I.say("\nCannot apply at: "+employer);
      return null;
    }
    if (report) {
      I.say("\nGetting next find-work step for "+actor);
    }
    
    if (report) I.say("  Applying at "+employer);
    final Action applies = new Action(
      actor, employer,
      this, "actionApplyTo",
      Action.LOOK, "Applying for work"
    );
    return applies;
  }
  
  
  public boolean actionApplyTo(Actor client, Property best) {
    confirmApplication();
    return true;
  }
  
  
  public boolean canOrDidApply() {
    if (canApply()) return true;
    if (employer == null || position == null) return false;
    return employer.staff().hasApplication(this);
  }
  
  
  public boolean canApply() {
    return
      position != null && employer != null &&
      employer.inWorld() && employer.structure().intact() &&
      employer.crowdRating(actor, position) < 1 &&
      ! employer.staff().hasApplication(this);
  }
  
  
  public boolean wasHired() {
    if (position == null || employer == null) return false;
    return actor.mind.vocation() == position && actor.mind.work() == employer;
  }
  
  
  public void confirmApplication() {
    if (wasHired() || ! canApply()) return;
    employer.staff().setApplicant(this, true);
  }
  
  
  public void cancelApplication() {
    if (employer == null || position == null) return;
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
    if (at.careers() == null) return null;
    //
    //  First, determine the extent of debug output to provide-
    final boolean report = rateVerbose && (
      I.talkAbout == actor || I.talkAbout == at
    ) && ! (actor.inWorld() && offworldOnly);
    final boolean repB = report && extraVerbose;
    if (report) I.say("\n"+actor+" checking for career opportunities at "+at);
    //  
    //  Then we allow
    final Pick <FindWork> pick = new Pick <FindWork> (null, 0) {
      public void compare(FindWork f, float rating) {
        super.compare(f, rating);
        if (report) I.say("    Rating: "+rating+" for "+f.position);
      }
    };
    FindWork main = (FindWork) actor.matchFor(FindWork.class, false);
    float rating = 0;
    if (main == null) {
      main = new FindWork(actor, null, null);
      actor.mind.assignToDo(main);
    }
    final Background currentPos = actor.mind.vocation();
    final Property   currentEmp = actor.mind.work    ();
    final Background lastTryPos = main.position;
    final Property   lastTryEmp = main.employer;
    
    if (lastTryPos != null && lastTryEmp != null) {
      rating = rateOpening(main, true, repB);
      pick.compare(main, rating);
    }
    
    if (currentPos != null && currentEmp != null) {
      final FindWork app = new FindWork(actor, currentPos, currentEmp);
      rating = rateOpening(app, false, repB);
      pick.compare(app, rating);
    }
    
    for (Background c : at.careers()) {
      final FindWork app = new FindWork(actor, c, at);
      rating = rateOpening(app, false, repB);
      pick.compare(app, rating);
    }
    
    final FindWork app = pick.result();
    if (app != null) {
      assignAmbition(actor, app.position, app.employer, pick.bestRating());
    }
    if (report) {
      I.say("  Current job:  "+currentPos+" at "+currentEmp);
      I.say("  Last applied: "+lastTryPos+" at "+lastTryEmp);
      I.say("  Is offworld:  "+(! actor.inWorld()));
      I.say("  Best job:     "+main.position+" at "+main.employer);
      I.say("  Best rating:  "+main.rating);
    }
    return main;
  }
  
  
  private static float rateOpening(
    FindWork app, boolean sameTried, boolean report
  ) {
    final Property   at       = app.employer;
    final Background position = app.position;
    final Actor      actor    = app.actor   ;
    final boolean isNew = ! at.staff().isWorker(actor);
    if (isNew && at.crowdRating(actor, position) >= 1) return -1;
    if (position != actor.mind.vocation() && ! actor.inWorld()) return -1;
    
    if (report) I.say("\nRating opening for "+position);
    //
    //  
    float rating = Career.ratePromotion(position, actor, report);
    if (report) I.say("\nBase promotion rating: "+rating);
    if (at.base() != actor.base()) {
      rating *= actor.relations.valueFor(at.base());
      if (report) I.say("  After base relations: "+rating);
    }
    //
    //  The basic idea here is to partially 'mix in' the appeal of money (or
    //  financial desperation) when considering whether to take a job you
    //  otherwise dislike.
    final float baseGreed = actor.motives.greedPriority(
      position.defaultSalary / Backgrounds.NUM_DAYS_PAY
    ) / Plan.ROUTINE;
    final float greedFactor = ((rating + 1) / 2) * baseGreed;
    rating += greedFactor - 1;
    if (report) {
      I.say("  New salary: "+position.defaultSalary);
      I.say("  Old salary: "+actor.mind.vocation().defaultSalary);
      I.say("  Greed is:   "+greedFactor+" (Base "+baseGreed+")");
    }
    //  TODO:  Also impact through area living conditions (or factor that into
    //         hiring costs?)
    final int numApps = at.staff().numApplied(position);
    if (! at.staff().hasApplication(app)) {
      final int MA = (int) BaseCommerce.MAX_APPLICANTS;
      rating -= numApps / MA;
      if (report) I.say("  Total/max applicants: "+numApps+"/"+MA);
    }
    //
    //  We favour applications that will be automatically approved.  Also, we
    //  give preference to the same vocation and/or the same venue and/or the
    //  same application (to maintain stability.)
    //  TODO:  USE STUBBORNNESS-TRAIT HERE
    if (position == actor.mind.vocation() || at == actor.mind.work()) {
      rating *= SWITCH_THRESHOLD;
      if (report) I.say("  Is same as old position or employer");
    }
    if (sameTried) {
      rating *= SWITCH_THRESHOLD;
      if (report) I.say("  Is same as last application");
    }
    if (! app.requiresApproval()) {
      rating *= SWITCH_THRESHOLD;
      if (report) I.say("  Does not require approval");
    }
    if (report) I.say("  Final rating:         "+rating+"\n");
    return rating;
  }
  
  
  //  TODO:  Store this in the ActorMotives class instead.
  public static void assignAmbition(
    Actor actor, Background position, Property at, float rating
  ) {
    FindWork finding = (FindWork) actor.matchFor(FindWork.class, false);
    if (finding == null) actor.mind.assignToDo(
      finding = new FindWork(actor, null, null)
    );
    finding.position = position;
    finding.employer = at;
    finding.rating = rating;
    finding.calcHiringFee();
  }
  
  
  public static Background ambitionOf(Actor actor) {
    final FindWork finding = (FindWork) actor.matchFor(FindWork.class, false);
    if (finding == null || finding.position == null) return null;
    return finding.position;
  }
  
  
  private int calcHiringFee() {
    if (! position.isAgent()) return 0;
    
    int transport = 0, incentive = 0, guildFees = 0;
    
    //  TODO:  Allow the player to set wages in a similar manner to setting
    //  goods' import/export levels.
    guildFees += position.defaultSalary;
    if (guildFees == 0) return 0;
    
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


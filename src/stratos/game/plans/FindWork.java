


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.civilian.Application;
import stratos.game.civilian.Employer;
import stratos.game.common.*;
import stratos.user.*;
import stratos.util.*;

import static stratos.game.actors.Qualities.*;
import static stratos.game.building.Economy.*;



public class FindWork extends Plan {
  
  
  private static boolean verbose = false;
  
  final Application application;
  final float rating;
  
  
  private FindWork(Actor actor, Application newApp, float rating) {
    super(actor, newApp.employer);
    this.application = newApp;
    this.rating = rating;
  }
  
  
  public FindWork(Session s) throws Exception {
    super(s);
    application = (Application) s.loadObject();
    rating = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(application);
    s.saveFloat(rating);
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected float getPriority() {
    return Visit.clamp(URGENT * rating, 0, URGENT);
  }
  
  
  protected Behaviour getNextStep() {
    if (! application.valid()) return null;
    if (actor.mind.application() == application) return null;
    if (application.employer == actor.mind.work()) return null;
    final Action applies = new Action(
      actor, application.employer,
      this, "actionApplyTo",
      Action.LOOK, "Applying for work"
    );
    return applies;
  }
  
  
  public boolean actionApplyTo(Actor client, Employer best) {
    if (! application.valid()) return false;
    client.mind.switchApplication(application);
    return true;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("Applying for work as ");
    d.append(application.position+" at ");
    d.append(application.employer);
  }
  
  
  
  /**  Helper methods for finding other employment-
    */
  //
  //  TODO:  Allow all venues to offer this as a service instead?  That might
  //  be simpler...
  
  public static Application lookForWork(
    Actor actor, Base base, boolean report
  ) {
    final Employer work = actor.mind.work();
    if (report) I.say("\n"+actor+" looking for work!");
    
    if (work instanceof Vehicle) return null;
    //
    //  Set up key comparison variables-
    final World world = base.world;
    float bestRating = 0;
    Application picked = null;
    final Batch <Venue> batch = new Batch <Venue> ();
    
    final int WS = world.size / 2;
    final Target from =
      actor.inWorld() ? actor :
      Spacing.pickRandomTile(world.tileAt(WS, WS), WS * 2, world);
    
    //
    //  Ensure that any new applications outweigh the value of older attempts-
    if (actor.mind.application() != null) {
      final Application oldApp = actor.mind.application();
      bestRating = Math.max(bestRating, rateApplication(oldApp, report) * 1.5f);
      world.presences.sampleFromMaps(
        from, world, 2, batch, oldApp.position
      );
    }
    if (work != null) {
      final Application WA = new Application(actor, actor.vocation(), work);
      bestRating = Math.max(bestRating, rateApplication(WA, report) * 1.5f);
    }
    world.presences.sampleFromMaps(
      from, world, 2, batch, actor.vocation(), base
    );
    if (report) I.say("  Venues sampled: "+batch.size());
    
    //  Assess the attractiveness of applying for jobs at each venue-
    
    //  TODO:  Allow defection to another base, if the job prospects are
    //  sufficiently attractive?
    for (Venue venue : batch) if (venue.base() == actor.base()) {
      final Background careers[] = venue.careers();
      if (careers == null) continue;
      
      for (Background c : careers) if (venue.numOpenings(c) > 0) {
        final Application newApp = new Application(actor, c, venue);
        
        final int signingCost = signingCost(newApp);
        newApp.setHiringFee(signingCost);
        
        final float rating = rateApplication(newApp, report);
        if (rating > bestRating) {
          bestRating = rating;
          picked = newApp;
        }
        
        if (report) I.say("  Rating for "+c+" at "+venue+" is: "+rating);
      }
    }
    return picked;
  }
  
  
  private static float rateApplication(Application app, boolean report) {
    if (! app.valid()) return -1;
    
    final Actor a = app.applies;
    if (! Career.qualifies(a, app.position)) {
      if (report) I.say("  NO QUALIFICATION");
      return -1;
    }
    
    float rating = 2;
    rating *= Career.ratePromotion(app.position, a);
    
    if (a.mind.home() != null) {
      rating /= 1 + Spacing.distance(a.mind.home(), app.employer);
    }
    
    rating *= 5f / (5 + app.employer.personnel().applications().size());
    
    return rating;
  }
  
  
  
  /**  Returns the default hiring fee associated with a given application.
    */
  public static int signingCost(Application app) {
    int transport = 0, incentive = 0, guildFees = 0;
    
    //  TODO:  Allow the player to set wages in a similar manner to setting
    //  goods' import/export levels.
    guildFees += Backgrounds.HIRE_COSTS[app.position.standing];
    
    if (app.applies.inWorld()) {
      guildFees = 0;
    }
    else {
      //  TODO:  ...This could be much higher, depending on origin point.
      transport += 100;
    }
    if (app.employer instanceof Venue) {
      final Venue venue = (Venue) app.employer;
      if (venue.personnel.numHired(app.position) == 0) {
        guildFees /= 2;
        transport /= 2;
      }
    }
    
    
    //  TODO:  Set up incentive to join the settlement, based on settlement
    //  ratings and legislation.
    
    return guildFees + transport + incentive;
  }
  
  
  
  /**  Public helper methods-
    */
  public static FindWork attemptFor(Actor actor) {
    final boolean report = verbose && I.talkAbout == actor;
    final Application newApp = lookForWork(actor, actor.base(), report);
    if (newApp == null || newApp.employer == actor.mind.work()) return null;
    return new FindWork(actor, newApp, rateApplication(newApp, report));
  }
  
  
  public static void fillVacancies(Venue venue, boolean enterWorld) {
    //
    //  We automatically fill any positions available when the venue is
    //  established.  This is done for free, but candidates cannot be screened.
    if (venue.careers() == null) return;
    for (Background v : venue.careers()) {
      final int numOpen = venue.numOpenings(v);
      if (numOpen <= 0) continue;
      
      for (int i = numOpen; i-- > 0;) {
        final Human worker = new Human(v, venue.base());
        worker.mind.setWork(venue);
        
        if (GameSettings.hireFree || enterWorld) {
          worker.enterWorldAt(venue, venue.world());
          worker.goAboard(venue, venue.world());
        }
        else {
          venue.base().commerce.addImmigrant(worker);
        }
      }
    }
  }
}



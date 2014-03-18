


package stratos.game.civilian ;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.user.*;
import stratos.util.*;



public class FindWork extends Plan implements Economy {
  
  
  private static boolean verbose = false ;
  
  
  final Application application ;
  final float rating ;
  
  
  public static FindWork attemptFor(Actor actor) {
    final Application newApp = lookForWork(actor, actor.base()) ;
    if (newApp == null || newApp.employer == actor.mind.work()) return null ;
    final int signingCost = signingCost(newApp) ;
    newApp.setHiringFee(signingCost) ;
    return new FindWork(actor, newApp, rateApplication(newApp)) ;
  }
  
  
  private FindWork(Actor actor, Application newApp, float rating) {
    super(actor) ;
    this.application = newApp ;
    this.rating = rating ;
  }
  
  
  public FindWork(Session s) throws Exception {
    super(s) ;
    application = (Application) s.loadObject() ;
    rating = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(application) ;
    s.saveFloat(rating) ;
  }
  
  

  public float priorityFor(Actor actor) {
    return Visit.clamp(URGENT * rating, 0, URGENT) ;
  }
  
  
  protected Behaviour getNextStep() {
    if (actor.mind.application() == application) return null ;
    if (application.employer == actor.mind.work()) return null ;
    final Action applies = new Action(
      actor, application.employer,
      this, "actionApplyTo",
      Action.LOOK, "Applying for work"
    ) ;
    return applies ;
  }
  
  
  public boolean actionApplyTo(Actor client, Employment best) {
    if (! application.valid()) return false ;
    client.mind.switchApplication(application) ;
    return true ;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("Applying for work as ") ;
    d.append(application.position+" at ") ;
    d.append(application.employer) ;
  }
  
  
  
  /**  Helper methods for finding other employment-
    */
  //
  //  TODO:  Allow all venues to offer this as a service instead?  That might
  //  be simpler...
  
  public static Application lookForWork(Actor actor, Base base) {
    final Employment work = actor.mind.work() ;
    if (work instanceof Vehicle) return null ;
    //
    //  Set up key comparison variables-
    final World world = base.world ;
    float bestRating = 0 ;
    Application picked = null ;
    final Batch <Venue> batch = new Batch <Venue> () ;
    
    final int WS = world.size / 2 ;
    final Target from =
      actor.inWorld() ? actor :
      Spacing.pickRandomTile(world.tileAt(WS, WS), WS * 2, world) ;
    
    //
    //  Ensure that any new applications outweigh the value of older attempts-
    if (actor.mind.application() != null) {
      final Application oldApp = actor.mind.application() ;
      bestRating = Math.max(bestRating, rateApplication(oldApp) * 1.5f) ;
      world.presences.sampleFromKeys(
        from, world, 2, batch, oldApp.position
      ) ;
    }
    if (work != null) {
      final Application WA = new Application(actor, actor.vocation(), work) ;
      bestRating = Math.max(bestRating, rateApplication(WA) * 1.5f) ;
    }
    world.presences.sampleFromKeys(
      from, world, 2, batch, actor.vocation(), base
    ) ;
    
    //
    //  Assess the attractiveness of applying for jobs at each venue-
    for (Venue venue : batch) {
      final Background careers[] = venue.careers() ;
      if (careers == null) continue ;
      for (Background c : careers) if (venue.numOpenings(c) > 0) {
        final Application newApp = new Application(actor, c, venue) ;
        final float rating = rateApplication(newApp) ;
        if (rating > bestRating) {
          bestRating = rating ;
          picked = newApp ;
        }
        
        if (verbose && I.talkAbout == actor) {
          I.say("Rating for "+c+" at "+venue+" is: "+rating) ;
        }
      }
    }
    return picked ;
  }
  
  
  private static float rateApplication(Application app) {
    
    final Actor a = app.applies ;
    if (! Career.qualifies(a, app.position)) return -1 ;
    
    float rating = 2 ;
    rating *= Career.ratePromotion(app.position, a) ;
    
    if (a.mind.home() != null) {
      rating /= 1 + Spacing.distance(a.mind.home(), app.employer) ;
    }
    
    rating *= 5f / (5 + app.employer.personnel().applications().size()) ;
    
    return rating ;
  }
  
  
  public static int signingCost(Application app) {
    
    //
    //  TODO:  Signing cost is based on transport factors, attraction, no.
    //  already employed, etc.  Implement all of that.
    int transport = 0, incentive = 0, guildFees = 0 ;
    guildFees += Background.HIRE_COSTS[app.position.standing] ;
    
    if (! app.applies.inWorld()) {
      //  ...This could potentially be much higher, depending on origin point.
      transport += 100 ;
    }
    else if (app.applies.mind.work() == null) {
      guildFees = 0 ;
    }
    
    if (app.employer instanceof Venue) {
      final Venue venue = (Venue) app.employer ;
      int numEmployed = venue.personnel.numPositions(app.position) ;
      if (numEmployed == 0) {
        guildFees = 0 ;
        transport /= 2 ;
      }
      else if (numEmployed == 1) {
        guildFees /= 2 ;
      }
      else guildFees *= 1 + ((numEmployed - 2) / 2f) ;
    }
    
    return guildFees + transport + incentive ;
  }
  
}




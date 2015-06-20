/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.user.*;
import stratos.util.*;



public class BaseSetup {
  
  /**  Placement of assorted structure types based on internal demand:
    */
  private static boolean
    verbose      = false,
    shortCycle   = false,
    extraVerbose = false;
  
  //  TODO:  You can cull various building-types early by skipping anything for
  //  which there is no profile-centric demand- or only checking the first N
  //  types which have the most demand.
  final static float
    FULL_EVAL_PERIOD  = Stage.STANDARD_DAY_LENGTH / 2,//  Eval-cycle length.
    SHORT_EVAL_PERIOD = Stage.STANDARD_HOUR_LENGTH * 2,
    DEFAULT_PLACE_HP  = 50,
    MAX_PLACE_RATING  = 10;
  final static int
    MAX_SITINGS_IN_PASS   = 5,
    DEFAULT_VENUE_SAMPLES = 5,
    DEFAULT_CHAT_SAMPLES  = 5;
  
  
  final Stage world;
  final Base  base ;
  //
  //  Data structures for conducting time-sliced placement of private venues:
  private Blueprint canPlace[];
  private List <SitingPass> passes = new List();
  
  
  public BaseSetup(Base base, Stage world, Blueprint... canPlace) {
    this.base     = base    ;
    this.world    = world   ;
    this.canPlace = canPlace;
  }
  
  
  public void loadState(Session s) throws Exception {
    canPlace = (Blueprint[]) s.loadObjectArray(Blueprint.class);
    for (int n = s.loadInt(); n-- > 0;) passes.add(SitingPass.loadPass(s));
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObjectArray(canPlace);
    s.saveInt(passes.size());
    for (SitingPass pass : passes) SitingPass.savePass(pass, s);
  }
  
  
  public void setAvailableVenues(Blueprint... canPlace) {
    this.canPlace = canPlace;
  }
  
  
  public Blueprint[] available() {
    return canPlace;
  }
  
  
  
  /**  Time-sliced automation of building-placement methods-
    */
  public Batch <Venue> doFullPlacements(Blueprint... types) {
    final Batch <Venue> placed = new Batch();
    for (Blueprint type : types) {
      final Siting siting = type.siting();
      if (siting == null) continue;
      int demand = Nums.round(siting.rateSettlementDemand(base), 1, true);
      Visit.appendTo(placed, doPlacementsFor(type, demand));
    }
    return placed;
  }
  

  public Batch <Venue> doPlacementsFor(Blueprint type, int demand) {
    final Batch <Venue> placed = new Batch();
    if (type.siting() == null) return placed;
    while (demand-- > 0) {
      final Venue toPlace = type.createVenue(base);
      final SitingPass pass = new SitingPass(base, type.siting(), toPlace);
      doSitingPass(pass, placed, 1);
    }
    return placed;
  }
  

  public Batch <Venue> doPlacementsFor(Venue... toPlace) {
    final Batch <Venue> placed = new Batch();
    for (Venue v : toPlace) {
      if (v.blueprint.siting() == null) continue;
      final SitingPass pass = new SitingPass(base, v.blueprint.siting(), v);
      doSitingPass(pass, placed, 1);
    }
    return placed;
  }
  
  
  private boolean doSitingPass(
    SitingPass pass, Batch <Venue> placed, float fraction
  ) {
    if (fraction >= 1) pass.performFullPass();
    else pass.performPassFraction(fraction);
    
    if (pass.success()) {
      final Tile point = pass.pointPicked();
      I.say("VENUE BEING PLACED: "+pass.placed.hashCode()+" AT "+point);
      
      pass.placed.setupWith(point, null);
      pass.placed.doPlacement(true);
      if (placed != null) placed.add(pass.placed);
      return true;
    }
    return false;
  }
  
  
  public void updatePlacements() {
    
    final float interval = shortCycle ?
      SHORT_EVAL_PERIOD :
      FULL_EVAL_PERIOD  ;
    for (SitingPass pass : passes) {
      final float fraction = 1 / (interval * passes.size());
      doSitingPass(pass, null, fraction);
      if (pass.complete()) passes.remove(pass);
    }
    
    if (passes.empty() && canPlace != null) {
      final List <SitingPass> sorting = new List <SitingPass> () {
        protected float queuePriority(SitingPass pass) { return pass.rating; }
      };
      
      for (Blueprint type : canPlace) if (canSite(type)) {
        final float demand = type.siting().rateSettlementDemand(base);
        if (demand > 0) {
          final Venue toPlace = type.createVenue(base);
          final SitingPass pass = new SitingPass(base, type.siting(), toPlace);
          pass.rating = demand;
          sorting.add(pass);
        }
      }
      
      sorting.queueSort();
      while (passes.size() < MAX_SITINGS_IN_PASS && ! sorting.empty()) {
        passes.add(sorting.removeLast());
      }
    }
  }
  
  
  private boolean canSite(Blueprint type) {
    if (type == null || type.siting() == null) return false;
    
    //  TODO:  You also need to skip over certain venue-types, depending
    //  on just how much autonomy/discretion the AI might have in different
    //  areas of the map.
    
    
    if (type.owningTier > Owner.TIER_PRIVATE) {
      if (base.advice.controlLevel() < BaseAdvice.LEVEL_TOTAL) return false;
    }
    else {
      if (base.advice.controlLevel() < BaseAdvice.LEVEL_ADVISOR) return false;
    }
    return true;
  }
  
  
  
  /**  Establishing base personnel:
    */
  public void fillVacancies(
    Venue venue, boolean enterWorld, Actor... employed
  ) {
    //
    //  We automatically fill any positions available when the venue is
    //  established.  This is done for free, but candidates cannot be screened.
    if (venue.careers() == null) return;
    
    final boolean report = verbose && base == BaseUI.currentPlayed();
    final int MAX_TRIES = 100;  //Safety measure...
    final boolean hasList = employed != null && employed.length > 0;
    int numTries = 0;
    if (report) I.say("\nAttempting to fill vacancies at "+venue);
    
    if (hasList) for (Actor worker : employed) {
      addWorkerTo(venue, worker, enterWorld);
    }
    else for (Background v : venue.careers()) while (true) {
      
      //  True-loops are a recipe for trouble:
      if (++numTries > MAX_TRIES) {
        I.say("\nWARNING: COULD NOT FILL VACANCIES FOR "+venue);
        return;
      }
      
      //  First, check to make sure that space is still available (and that a
      //  new worker can be made available.)
      final Actor worker = v.sampleFor(venue.base());
      if (worker == null) break;
      final float crowding = venue.crowdRating(worker, v);
      if (report) {
        I.say("  Num hired:   "+venue.staff.numHired(v));
        I.say("  Crowding for "+worker+" is "+crowding);
      }
      if (crowding >= 1) break;
      else addWorkerTo(venue, worker, enterWorld);
    }
  }
  
  
  private void addWorkerTo(Venue venue, Actor worker, boolean enterWorld) {
    //
    //  Then set the actor's employment status (and possibly residency as
    //  well.  NOTE:  Work is set first to ensure residency is permitted at
    //  various venues.)
    worker.mind.setWork(venue);
    if (venue.crowdRating(worker, Backgrounds.AS_RESIDENT) < 1) {
      worker.mind.setHome(venue);
    }
    
    //  Finally, ensure the new worker is either in the world, or registered
    //  for migration as soon as possible:
    if (GameSettings.hireFree || enterWorld) {
      worker.enterWorldAt(venue, venue.world());
    }
    else {
      final Stage world = venue.base().world;
      world.offworld.journeys.addLocalImmigrant(worker, base);
    }
  }
  
  
  public void fillVacancies(
    Series <? extends Venue> venues, boolean enterWorld
  ) {
    for (Venue v : venues) fillVacancies(v, enterWorld);
  }
  
  
  
  /**  Establishing relationships, gear, experience and health FX-
    */
  public void establishRelations(Series <? extends Actor> among) {
    for (Actor f : among) for (Actor t : among) {
      if (f == t || f.relations.hasRelation(t)) continue;
      
      float initRelation = 0.25f;
      for (int n = DEFAULT_CHAT_SAMPLES; n-- > 0;) {
        initRelation += DialogueUtils.tryChat(f, t);
      }
      float initNovelty = 1.0f;
      initNovelty -= (Rand.num() - 0.5f) / 2;
      f.relations.setRelation(t, initRelation, initNovelty);
    }
  }
  
  
  public void establishRelationsAt(Venue v) {
    final Batch <Actor> among = new Batch <Actor> ();
    Visit.appendTo(among, v.staff.workers  ());
    Visit.appendTo(among, v.staff.lodgers());
    
    final Stage world = base.world;
    final Series <Target> nearby = world.presences.sampleFromMap(
      v, world, DEFAULT_VENUE_SAMPLES, null, base
    );
    for (Target t : nearby) if (t instanceof Venue) {
      final Venue n = (Venue) t;
      for (Actor a : n.staff.workers  ()) if (Rand.yes()) among.add(a);
      for (Actor a : n.staff.lodgers()) if (Rand.yes()) among.add(a);
      if (among.size() >= ActorRelations.MAX_RELATIONS) break;
    }
    establishRelations(among);
  }
}


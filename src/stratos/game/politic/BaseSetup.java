/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.politic;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.user.BaseUI;
import stratos.util.*;
import stratos.game.plans.*;





public class BaseSetup {
  
  /**  Placement of assorted structure types based on internal demand:
    */
  private static boolean
    verbose      = false,
    extraVerbose = false;
  
  final static float
    FULL_EVAL_PERIOD      = Stage.STANDARD_DAY_LENGTH,  //  Eval-cycle length.
    DEFAULT_PLACE_HP      = 50,
    
    MAX_PLACE_RATING      = 10;
  final static int
    DEFAULT_VENUE_SAMPLES = 5,
    DEFAULT_CHAT_SAMPLES  = 5;
  
  final Base base;
  final Stage world;
  
  //  Data structures for conducting time-sliced placement of private venues:
  protected VenueProfile canPlace[];
  static class Placing {
    Venue        sampled  ;
    StageSection placed   ;
    Tile         exactTile;
    float        rating   ;
  }
  
  final List <Placing> placings = new List <Placing> () {
    protected float queuePriority(Placing r) {
      return r.rating;
    }
  };
  //
  //  State variables for time-sliced placement-evaluation:
  private int   initPlaceCount = -1;  //  Total placements at start of cycle
  private float lastEval       = -1;  //  Last evaluation time within cycle
  private float totalBuildHP =  0;  //  Total HP placed during this cycle
  private Batch <Venue> allPlaced = new Batch <Venue> ();
  
  
  public BaseSetup(Base base, Stage world, VenueProfile... canPlace) {
    this.base     = base    ;
    this.world    = world   ;
    this.canPlace = canPlace;
  }
  
  
  public void loadState(Session s) throws Exception {
    canPlace = (VenueProfile[]) s.loadObjectArray(VenueProfile.class);
    
    final int numP = s.loadInt();
    for (int n = numP ; n-- > 0;) {
      final Placing p = new Placing();
      p.sampled   = (Venue  ) s.loadObject();
      p.placed    = (StageSection) s.loadTarget();
      p.exactTile = (Tile   ) s.loadTarget();
      p.rating    =           s.loadFloat ();
    }
    
    initPlaceCount = s.loadInt  ();
    lastEval       = s.loadFloat();
    totalBuildHP = s.loadFloat();
    s.loadObjects(allPlaced);
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObjectArray(canPlace);
    
    s.saveInt(placings.size());
    for (Placing p : placings) {
      s.saveObject(p.sampled  );
      s.saveTarget(p.placed   );
      s.saveTarget(p.exactTile);
      s.saveFloat (p.rating   );
    }
    s.saveInt  (initPlaceCount);
    s.saveFloat(lastEval      );
    s.saveFloat(totalBuildHP);
    s.saveObjects(allPlaced);
  }
  
  
  public void setAvailableVenues(VenueProfile... canPlace) {
    this.canPlace = canPlace;
  }
  
  
  
  /**  Time-sliced automation of building-placement methods-
    */
  //  TODO:  Permit hints as to preferred placement-location, and an argument
  //  for instant-placement.
  
  public Batch <Venue> doPlacementsFor(VenueProfile type, int count) {
    final Venue toPlace[] = new Venue[count];
    for (int n = count; n-- > 0;) toPlace[n] = type.sampleVenue(base);
    return doPlacementsFor(toPlace);
  }
  
  
  public Batch <Venue> doPlacementsFor(Venue... toPlace) {
    final boolean report = verbose && BaseUI.currentPlayed() == base;
    
    for (Venue placing : toPlace) {
      rankSectionPlacings(new Venue[] {placing}, report);
      attemptPlacements(placings.size(), -1, report);
    }
    
    final Batch <Venue> placed = new Batch <Venue> ();
    Visit.appendTo(placed, allPlaced);
    allPlaced.clear();
    placings.clear();
    for (Venue v : placed) {
      v.structure().setState(Structure.STATE_INTACT, 1);
    }
    return placed;
  }
  
  
  public void updatePlacements() {
    final boolean report = verbose && (BaseUI.currentPlayed() == base);
    //
    //  If the set of placings has been exhausted, then it's time for a new
    //  cycle of evaluations.  Rank potential sites and reset the build-total.
    if (placings.size() == 0) {
      final Venue samples[] = VenueProfile.sampleVenues(
        Venue.VENUE_OWNS, true, canPlace
      );
      rankSectionPlacings(samples, report);
      initPlaceCount = placings.size();
      if (report && initPlaceCount > 0) {
        I.say("\nFinished ranking sections!");
      }
      totalBuildHP = 0;
      allPlaced.clear();
      return;
    }
    //
    //  Calculate the total limit of building-placement for the cycle (this
    //  shouldn't change much, but anyway...)
    //  TODO:  Also base off finances, labour force, and the amount of
    //  structures currently in need of repairs, et cetera.
    float buildLimit = 0;
    buildLimit = Nums.log(10, 1 + base.relations.population());
    buildLimit *= DEFAULT_PLACE_HP;
    //
    //  Then calculate how many sites we can evaluate during this fraction of
    //  the full cycle.
    final float time = world.currentTime();
    if (lastEval == -1) lastEval = time;
    int numIters = 0;
    numIters += initPlaceCount * time     / FULL_EVAL_PERIOD;
    numIters -= initPlaceCount * lastEval / FULL_EVAL_PERIOD;
    if (numIters <= 0) return;
    //
    //  And perform the actual placement attempts-
    else lastEval = time;
    if (report) {
      I.say("\nPlacement iterations: "+numIters+"/"+initPlaceCount);
    }
    attemptPlacements(numIters, buildLimit, report);
  }
  
  
  private void rankSectionPlacings(Venue samples[], boolean report) {
    placings.clear();
    
    for (StageSection section : world.sections.sectionsUnder(world.area())) {
      for (Venue sample : samples) {
        sample.assignBase(base);
        final Placing p = new Placing();
        p.sampled   = sample ;
        p.placed    = section;
        p.exactTile = null   ;
        p.rating    = sample.ratePlacing(section, false);
        //
        //  We add placements regardless of rating, but cull them during the
        //  attempt-phases (see below.)
        placings.add(p);
        if (report) descPlacing("  Ranking placement: ", p);
      }
    }
    
    placings.queueSort();
  }
  
  
  private void attemptPlacements(
    int maxChecked, float buildLimit, boolean report
  ) {
    while (maxChecked-- > 0 && placings.size() > 0) {
      if (buildLimit > 0 && totalBuildHP > buildLimit) continue;
      final Placing best = placings.removeLast();
      if (best.sampled.inWorld() || best.rating <= 0) {
        if (report) I.say("  Placement culled...");
        continue;
      }
      
      if (report) {
        I.say("\nAttempting placement at best site for "+best.sampled);
        I.say("  Rough location: "+best.placed);
        I.say("  Rating was:     "+best.rating);
      }
      if (attemptExactPlacement(best, report)) {
        totalBuildHP += best.sampled.structure.maxIntegrity();
      }
      else if (report) I.say("  No suitable site found.");
    }
  }
  
  
  private boolean attemptExactPlacement(Placing placing, boolean report) {
    final Venue sample = placing.sampled;
    final Pick <Tile> sitePick = new Pick <Tile> ();
    
    for (Tile t : world.tilesIn(placing.placed.area, false)) {
      sample.setPosition(t.x, t.y, world);
      if (! sample.canPlace()) continue;
      final float rating = sample.ratePlacing(t, true);
      sitePick.compare(t, rating);
    }
    
    if (sitePick.empty()) {
      if (report) I.say("  No room for placement!");
      return false;
    }
    if (sitePick.bestRating() <= 0) {
      if (report) I.say("  Best rating negative: "+sitePick.bestRating());
      return false;
    }
    final Tile bestSite = sitePick.result();
    if (report) {
      I.say("\nPlacing "+sample+" at "+bestSite);
      I.say("  Rating: "+sitePick.bestRating());
      I.say("  Total HP placed: "+totalBuildHP);
    }
    sample.setPosition(bestSite.x, bestSite.y, world);
    sample.doPlacement();
    allPlaced.add(sample);
    return true;
  }
  
  
  /**  Establishing base personnel:
    */
  public void fillVacancies(Venue venue, boolean enterWorld) {
    //
    //  We automatically fill any positions available when the venue is
    //  established.  This is done for free, but candidates cannot be screened.
    if (venue.careers() == null) return;
    
    final boolean report = verbose && base == BaseUI.currentPlayed();
    final int MAX_TRIES = 100;  //Safety measure...
    int numTries = 0;
    if (report) I.say("\nAttempting to fill vacancies at "+venue);
    
    for (Background v : venue.careers()) while (true) {
      
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
        worker.goAboard(venue, venue.world());
      }
      else {
        final Stage world = venue.base().world;
        world.offworld.addImmigrant(worker, world);
      }
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
    Visit.appendTo(among, v.staff.residents());
    
    final Stage world = base.world;
    final Series <Target> nearby = world.presences.sampleFromMap(
      v, world, DEFAULT_VENUE_SAMPLES, null, base
    );
    for (Target t : nearby) if (t instanceof Venue) {
      final Venue n = (Venue) t;
      for (Actor a : n.staff.workers  ()) if (Rand.yes()) among.add(a);
      for (Actor a : n.staff.residents()) if (Rand.yes()) among.add(a);
      if (among.size() >= ActorRelations.MAX_RELATIONS) break;
    }
    establishRelations(among);
  }
  
  
  
  /** Feedback and debug methods-
    */
  private void descPlacing(String desc, Placing best) {
    I.say(
      "  "+desc+""+best.sampled+
      " "+best.placed.absX+"/"+best.placed.absY+
      ", rating: "+best.rating
    );
  }
}






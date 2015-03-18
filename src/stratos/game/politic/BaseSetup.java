/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.politic;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.game.plans.*;
import stratos.user.*;
import stratos.util.*;
import stratos.game.economic.Inventory.Owner;



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
    DEFAULT_VENUE_SAMPLES = 5,
    DEFAULT_CHAT_SAMPLES  = 5;
  
  
  final Stage world;
  final Base  base ;
  //
  //  Data structures for conducting time-sliced placement of private venues:
  protected VenueProfile canPlace[];
  static class Siting {
    Venue        sample   ;
    StageSection placed   ;
    Tile         exactTile;
    float        rating   ;
  }
  final Sorting <Siting> sitings = new Sorting <Siting> () {
    public int compare(Siting a, Siting b) {
      if (a.rating > b.rating) return  1;
      if (b.rating > a.rating) return -1;
      return 0;
    }
  };
  //
  //  State variables for time-sliced placement-evaluation:
  private int   initPlaceCount = -1;  //  Total placements at start of cycle
  private float lastEval       = -1;  //  Last evaluation time within cycle
  private float totalBuildHP   =  0;  //  Total HP placed during this cycle
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
      final Siting p = new Siting();
      p.sample    = (Venue       ) s.loadObject();
      p.placed    = (StageSection) s.loadTarget();
      p.exactTile = (Tile        ) s.loadTarget();
      p.rating    =                s.loadFloat ();
      sitings.add(p);
    }
    
    initPlaceCount = s.loadInt  ();
    lastEval       = s.loadFloat();
    totalBuildHP = s.loadFloat();
    s.loadObjects(allPlaced);
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObjectArray(canPlace);
    
    s.saveInt(sitings.size());
    for (Siting p : sitings) {
      s.saveObject(p.sample   );
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
  public void doFullPlacements() {
    //  TODO:  IMPLEMENT THIS
  }
  
  
  public Batch <Venue> doFullPlacements(VenueProfile... types) {
    final Batch <Venue> placed = new Batch <Venue> ();
    
    for (StageSection section : world.sections.sectionsUnder(world.area(), 0)) {
      final Pick <Venue> pick = new Pick <Venue> (null, 0);
      
      for (VenueProfile p : types) {
        final Venue v = p.sampleVenue(base);
        pick.compare(v, v.ratePlacing(section, false));
      }
      if (pick.result() != null) {
        final Venue v = pick.result();
        float x = section.absX + Rand.index(section.size);
        float y = section.absY + Rand.index(section.size);
        v.setPosition(x, y, world);
        if (! v.canPlace()) continue;
        v.enterWorld();
        v.structure.setState(Structure.STATE_INTACT, 1);
        placed.add(v);
      }
    }
    
    return placed;
  }
  
  
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
      attemptPlacements(sitings.size(), -1, report);
    }
    
    final Batch <Venue> placed = new Batch <Venue> ();
    Visit.appendTo(placed, allPlaced);
    allPlaced.clear();
    sitings.clear();
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
    if (sitings.size() == 0) {
      final Venue samples[] = VenueProfile.sampleVenues(
        Owner.TIER_PRIVATE, canPlace
      );
      rankSectionPlacings(samples, report);
      initPlaceCount = sitings.size();
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
    final float evalPeriod = shortCycle ? SHORT_EVAL_PERIOD : FULL_EVAL_PERIOD;
    //
    //  Then calculate how many sites we can evaluate during this fraction of
    //  the full cycle.
    final float time = world.currentTime();
    if (lastEval == -1) lastEval = time;
    int numIters = 0;
    numIters += initPlaceCount * time     / evalPeriod;
    numIters -= initPlaceCount * lastEval / evalPeriod;
    if (numIters <= 0) return;
    //
    //  And perform the actual placement attempts-
    else lastEval = time;
    if (report) {
      I.say("\nPlacement iterations: "+numIters+"/"+initPlaceCount);
      I.say("  Sitings remaining: "+sitings.size());
    }
    attemptPlacements(numIters, buildLimit, report);
  }
  
  
  private void rankSectionPlacings(Venue samples[], boolean report) {
    sitings.clear();
    if (report) {
      I.say("\nAttempting to gather section rankings...");
      I.say("  Time: "+base.world.currentTime());
    }
    
    for (StageSection section : world.sections.sectionsUnder(world.area(), 0)) {
      for (Venue sample : samples) {
        sample.assignBase(base);
        final Siting p = new Siting();
        p.sample    = sample ;
        p.placed    = section;
        p.exactTile = null   ;
        p.rating    = sample.ratePlacing(section, false);
        //
        //  We add placements regardless of rating, but cull them during the
        //  attempt-phases (see below.)
        sitings.add(p);
      }
    }
    
    if (report) for (Siting p : sitings) if (p.rating > 0) {
      descPlacing("  Ranking placement: ", p);
    }
  }
  
  
  private void attemptPlacements(
    int maxChecked, float buildLimit, boolean report
  ) {
    while (maxChecked-- > 0 && sitings.size() > 0) {
      final Siting best  = sitings.removeGreatest();
      final Venue sample = best.sample;
      if (buildLimit > 0 && totalBuildHP > buildLimit) continue;
      if (best.rating <= 0) {
        if (report) {
          I.say("\nCulling placement of "+sample);
          I.say("  Rough location: "+best.placed);
          I.say("  Rating was:     "+best.rating);
        }
        continue;
      }
      if (report) {
        I.say("\nAttempting placement at best site for "+sample);
        I.say("  Rough location: "+best.placed);
        I.say("  Rating was:     "+best.rating);
      }
      if (attemptExactPlacement(best, sample, report)) {
        removeSitings(sample);
      }
      else if (report) I.say("  No suitable site found.");
    }
  }
  
  
  private void removeSitings(Venue sample) {
    final Batch <Siting> remains = new Batch <Siting> ();
    while (sitings.size() > 0) {
      final Siting s = sitings.removeLeast();
      if (s.sample != sample) remains.add(s);
    }
    Visit.appendTo(sitings, remains);
  }
  
  
  private boolean attemptExactPlacement(
    Siting placing, Venue sample, boolean report
  ) {
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
    sample.setPosition(bestSite.x, bestSite.y, world);
    sample.doPlacement();
    
    allPlaced.add(sample);
    totalBuildHP += sample.structure.maxIntegrity();
    
    if (report) {
      I.say("\nPlacing "+sample+" at "+bestSite);
      I.say("  Rating:       "+sitePick.bestRating());
      I.say("  Integrity:    "+sample.structure.maxIntegrity());
      I.say("  Total so far: "+totalBuildHP);
    }
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
  private void descPlacing(String desc, Siting best) {
    I.say(
      "  "+desc+""+best.sample+
      " "+best.placed.absX+"/"+best.placed.absY+
      ", rating: "+best.rating
    );
  }
}






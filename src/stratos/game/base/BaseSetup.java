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
import static stratos.game.economic.Economy.*;



//  TODO:  Unify this with the BaseAdvice class.

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
    FULL_EVAL_PERIOD  = Stage.STANDARD_DAY_LENGTH  / 2, // Eval-cycle length.
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
  final List <SitingPass> sorting = new List <SitingPass> () {
    protected float queuePriority(SitingPass r) {
      return r.rating;
    }
  };
  
  
  public BaseSetup(Base base, Stage world, Blueprint... canPlace) {
    this.base     = base    ;
    this.world    = world   ;
    this.canPlace = canPlace;
  }
  
  
  public void loadState(Session s) throws Exception {
    canPlace = (Blueprint[]) s.loadObjectArray(Blueprint.class);
    for (int n = s.loadInt(); n-- > 0;) sorting.add(SitingPass.loadPass(s));
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObjectArray(canPlace);
    s.saveInt(sorting.size());
    for (SitingPass pass : sorting) SitingPass.savePass(pass, s);
  }
  
  
  public void setAvailableVenues(Blueprint... canPlace) {
    this.canPlace = canPlace;
  }
  
  
  public Blueprint[] available() {
    return canPlace;
  }
  
  
  
  /**  Time-sliced automation of building-placement methods-
    */
  public Batch <Venue> doPlacementsFor(Blueprint type, int total) {
    final Batch <Venue> record = new Batch();
    if (type.siting() == null) return record;
    while (total-- > 0) {
      final SitingPass pass = new SitingPass(base, type.siting());
      pass.placeState = SitingPass.PLACE_INTACT;
      pass.performFullPass();
      if (pass.success()) record.add(pass.placed);
    }
    return record;
  }
  
  
  public Batch <Venue> doPlacementsFor(Venue... toPlace) {
    final Batch <Venue> record = new Batch();
    for (Venue v : toPlace) {
      if (v.blueprint.siting() == null) continue;
      final SitingPass pass = new SitingPass(base, v.blueprint.siting(), v);
      pass.placeState = SitingPass.PLACE_INTACT;
      pass.performFullPass();
      if (pass.success()) record.add(pass.placed);
    }
    return record;
  }
  
  
  public Batch <Venue> doFullPlacements(Blueprint... types) {
    //
    //  Complete any previously-underway sitings first.
    updatePasses(sorting, null, 1, -1, null, verbose);
    sorting.clear();
    //
    //  Then try placing the new types until you run out of space or demand for
    //  them-
    final Batch <Venue> record = new Batch();
    do { updatePasses(sorting, types, 1, -1, record, verbose); }
    while (sorting.size() > 0);
    return record;
  }
  
  
  private void updatePasses(
    List <SitingPass> sorting, Blueprint canPlace[],
    float sumFractions, float buildLimit,
    Batch <Venue> record, boolean report
  ) {
    report &= extraVerbose && (sorting.size() > 0 || canPlace != null);
    
    if (report) {
      I.say("\nUpdating siting passes for "+base);
      I.say("  Pass fraction: "+sumFractions    );
      I.say("  Build limit:   "+buildLimit      );
      I.say("  Place types:   "+I.list(canPlace));
      I.say("  Total sorted:  "+sorting.size()  );
      I.say("  Sorting is: ");
      for (SitingPass p : sorting) {
        I.say("    "+p.placed+": "+p.rating);
      }
    }
    
    final boolean placeAll = buildLimit < 0;
    float sumDemands = 0, sumBuilt = 0;
    
    if (sorting.empty() && canPlace != null) {
      for (Blueprint type : canPlace) {
        if (placeAll || hasSitePermission(type)) {
          if (report) I.say("    Adding new pass for "+type);
          final SitingPass pass = new SitingPass(base, type.siting());
          sorting.add(pass);
        }
        else if (report) I.say("    Cannot auto-site: "+type);
      }
    }
    
    if (report) {
      I.say("\n  "+sorting.size()+" entries in sorting.");
    }
    for (SitingPass s : sorting) {
      float demand = s.siting.rateSettlementDemand(base);
      if (s.rating > 0) demand = Nums.min(demand, s.rating - 1);
      if (report) I.say("    Demand for "+s.placed+" is: "+demand);
      
      if (demand > 0) sumDemands += s.rating = demand;
      else sorting.remove(s);
    }
    
    sorting.queueSort();
    if (! placeAll) {
      while (sorting.size() > MAX_SITINGS_IN_PASS) sorting.removeLast();
    }
    
    for (SitingPass s : sorting) {
      final float fraction = sumFractions * s.rating / sumDemands;
      if (placeAll) s.placeState = SitingPass.PLACE_INTACT;
      s.performPassFraction(fraction);
      if (report) {
        I.say("  Performed pass for "+s.siting.blueprint);
        I.say("    Demand for type:   "+s.rating+"/"+sumDemands);
        I.say("    Pass fraction:     "+fraction  );
        I.say("    Total built:       "+buildLimit);
        I.say("    Complete/success?  "+s.complete()+"/"+s.success());
      }
      
      if (s.complete()) {
        sorting.remove(s);
        if (! s.success()) continue;
        if (record != null) record.add(s.placed);
        
        sumBuilt += s.placed.structure().maxIntegrity();
        if (buildLimit >= 0 && sumBuilt > buildLimit) break;
        
        final float demand = s.rating;
        s = new SitingPass(base, s.siting);
        s.rating = demand;
        sorting.add(s);
      }
    }
  }
  
  
  public void updatePlacements() {
    final boolean report = extraVerbose && BaseUI.currentPlayed() == base;
    
    final float
      interval    = shortCycle ? SHORT_EVAL_PERIOD : FULL_EVAL_PERIOD,
      numBuilding = base.demands.globalDemand(SERVICE_REPAIRS),
      numBuilders = base.demands.globalSupply(SERVICE_REPAIRS),
      buildLimit  = numBuilders - numBuilding;
    
    if (buildLimit < 0) return;
    updatePasses(sorting, canPlace, 1f / interval, buildLimit, null, report);
  }
  
  
  public boolean hasSitePermission(Blueprint type) {
    if (type == null || type.siting() == null) return false;
    if (! base.research.hasTheory(type.baseUpgrade())) return false;
    
    //  TODO:  You might also need to vary this for different regions of the
    //  map, depending on base-ownership or local autonomy.
    
    final int control = base.advice.autonomy();
    if (type.hasProperty(Structure.IS_PUBLIC)) {
      if (control <= BaseAdvice.LEVEL_NO_AUTO) return false;
    }
    else {
      if (control < BaseAdvice.LEVEL_FULL_AUTO) return false;
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
  
  
  public void fillVacancies(
    Series <? extends Venue> venues, boolean enterWorld
  ) {
    for (Venue v : venues) fillVacancies(v, enterWorld);
  }
  
  
  public void addWorkerTo(Venue venue, Actor worker, boolean enterWorld) {
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
    Visit.appendTo(among, v.staff.workers());
    Visit.appendTo(among, v.staff.lodgers());
    
    final Stage world = base.world;
    final Series <Target> nearby = world.presences.sampleFromMap(
      v, world, DEFAULT_VENUE_SAMPLES, null, base
    );
    for (Target t : nearby) if (t instanceof Venue) {
      final Venue n = (Venue) t;
      for (Actor a : n.staff.workers()) if (Rand.yes()) among.add(a);
      for (Actor a : n.staff.lodgers()) if (Rand.yes()) among.add(a);
      if (among.size() >= ActorRelations.MAX_RELATIONS) break;
    }
    establishRelations(among);
  }
}


/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.campaign;

import stratos.start.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.util.*;
import stratos.game.plans.*;

import org.apache.commons.math3.util.FastMath;



public class BaseSetup {
  
  
  /**  Placement of assorted structure types based on internal demand:
    */
  final static float
    FULL_EVAL_PERIOD = Stage.STANDARD_DAY_LENGTH,
    DEFAULT_PLACE_HP = 50;
  
  private static boolean verbose = false;
  
  final Base base;
  final Stage world;
  
  //
  //  Data structures for handling supply-and-demand:
  //private BaseDemands demands = new BaseDemands(this);  //  Setup this.
  
  //
  //  Data structures for conducting time-sliced placement of private venues:
  static class Placing {
    Venue   sampled  ;
    WorldSection placed   ;
    Tile    exactTile;
    float   rating   ;
  }
  
  final List <Placing> placings = new List <Placing> () {
    protected float queuePriority(Placing r) {
      return r.rating;
    }
  };
  
  private int   iterLimit = 0;  //  Matches placings.length after evaluation.
  private float lastEval = -1;  //  Last evaluation time.
  private float placeLimit = 0, amountPlaced = 0;
  
  
  public BaseSetup(Base base, Stage world) {
    this.base  = base ;
    this.world = world;
  }
  
  
  public void loadState(Session s) throws Exception {
    //demands.loadState(s);
    
    final int numP = s.loadInt();
    for (int n = numP ; n-- > 0;) {
      final Placing p = new Placing();
      p.sampled   = (Venue  ) s.loadObject();
      p.placed    = (WorldSection) s.loadTarget();
      p.exactTile = (Tile   ) s.loadTarget();
      p.rating    =           s.loadFloat ();
    }
    iterLimit    = s.loadInt  ();
    lastEval     = s.loadFloat();
    placeLimit   = s.loadFloat();
    amountPlaced = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    //demands.saveState(s);
    
    s.saveInt(placings.size());
    for (Placing p : placings) {
      s.saveObject(p.sampled  );
      s.saveTarget(p.placed   );
      s.saveTarget(p.exactTile);
      s.saveFloat (p.rating   );
    }
    s.saveInt  (iterLimit   );
    s.saveFloat(lastEval    );
    s.saveFloat(placeLimit  );
    s.saveFloat(amountPlaced);
  }
  
  
  
  /**  Time-sliced automation of building-placement methods-
    */
  public void updatePlacements() {
    
    if (placings.size() == 0) {
      if (verbose) I.say("Ranking sections...");
      rankSectionPlacings();
      calcPlaceLimit();
      return;
    }
    
    final float time = world.currentTime();
    if (lastEval == -1) lastEval = time;
    
    int numIters = 0;
    numIters += iterLimit * time     / FULL_EVAL_PERIOD;
    numIters -= iterLimit * lastEval / FULL_EVAL_PERIOD;
    
    if (numIters <= 0) return;
    lastEval = time;
    if (verbose) I.say("Placement iterations: "+numIters+"/"+iterLimit);
    
    while (numIters-- > 0 && placings.size() > 0) {
      final Placing best = placings.removeLast();
      if (amountPlaced < placeLimit) continue;
      if (best.sampled.inWorld() || best.rating <= 0) continue;
      
      if (attemptExactPlacement(best)) {
        amountPlaced += best.sampled.structure.maxIntegrity();
        
        if (verbose) descPlacing("New Placement: ", best);
        if (verbose) I.say("  Placed: "+amountPlaced+" Limit: "+placeLimit);
      }
    }
  }
  
  
  private boolean attemptExactPlacement(Placing placing) {
    final Venue sample = placing.sampled;
    float bestRating = 0;
    Tile  bestTile = null;
    
    //  TODO:  try and improve efficiency here.  (It would help if it were just
    //         a question of fixed areas.)
    
    //  TODO:  Time-slice this as well?  Well, as space becomes exhausted, the
    //  number of viable placements relative to tiles drops.  On the other
    //  hand, more tiles can be immediately discounted.  So in principle, that
    //  evens out.
    
    for (Tile t : world.tilesIn(placing.placed.area, false)) {
      sample.setPosition(t.x, t.y, world);
      if (! sample.canPlace()) continue;
      final float rating = sample.ratePlacing(t);
      if (rating > bestRating) { bestRating = rating; bestTile = t; }
    }
    
    if (bestRating <= 0) return false;
    sample.assignBase(base);
    sample.setPosition(bestTile.x, bestTile.y, world);
    sample.doPlacement();
    return true;
  }
  
  
  private void calcPlaceLimit() {
    placeLimit = (float) FastMath.log(10, 1 + base.relations.population());
    placeLimit *= DEFAULT_PLACE_HP;
    
    //  TODO:  Also base off finances, labour force, and the amount of
    //  structures currently in need of repairs.
    
  }
  
  
  private void rankSectionPlacings() {
    placings.clear();
    amountPlaced = 0;
    final Venue samples[] = VenueProfile.sampleVenues(Venue.VENUE_OWNS, true);
    
    for (WorldSection section : world.sections.sectionsUnder(world.area())) {
      for (Venue sample : samples) {
        final Placing p = new Placing();
        p.sampled   = sample ;
        p.placed    = section;
        p.exactTile = null   ;
        p.rating    = sample.ratePlacing(section);
        if (p.rating > 0) {
          placings.add(p);
          if (verbose) descPlacing("Ranking placement: ", p);
        }
      }
    }
    
    placings.queueSort();
    iterLimit = placings.size();
  }
  
  
  private void descPlacing(String desc, Placing best) {
    I.say(
      "  "+desc+""+best.sampled+
      " "+best.placed.absX+"/"+best.placed.absY+
      ", rating: "+best.rating
    );
  }
  
  
  
  /**  Establishing relationships, gear, experience and health FX-
    */
  public static void establishRelations(Series <? extends Actor>... among) {
    for (Series <? extends Actor> sF : among) for (Actor f : sF) {
      for (Series <? extends Actor> tF : among) for (Actor t : tF) {
        if (f == t || f.relations.hasRelation(t)) continue;
        
        float initRelation = 0;
        for (int n = 10; n-- > 0;) {
          initRelation += DialogueUtils.tryChat(f, t);
        }
        f.relations.setRelation(t, initRelation, Rand.num());
      }
    }
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








/*
//  TODO:  Humans in general might want a method like this, during the setup
//  process.
public static void establishRelations(Venue venue) {
  
  final World world = venue.world();
  final Batch <Actor>
    from = new Batch <Actor> (),
    to = new Batch <Actor> ();
  for (Actor a : venue.personnel.residents()) from.add(a);
  for (Actor a : venue.personnel.workers()) from.add(a);
  
  final Batch <Venue> nearby = new Batch <Venue> ();
  world.presences.sampleFromKey(venue, world, 5, nearby, Venue.class);
  for (Venue v : nearby) {
    for (Actor a : v.personnel.residents()) to.add(a);
    for (Actor a : v.personnel.workers()) to.add(a);
  }
  
  for (Actor f : from) for (Actor t : to) {
    float initRelation = 0;
    for (int n = 10; n-- > 0;) {
      initRelation += Dialogue.tryChat(f, t) * 10;
    }
    f.memories.initRelation(t, initRelation, Rand.num());
  }
}
//*/



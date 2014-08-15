/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.campaign;

import stratos.start.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.util.*;
import stratos.game.plans.*;
import stratos.game.maps.*;
import stratos.game.common.WorldSections.Section;

import java.lang.reflect.*;
import org.apache.commons.math3.util.FastMath;



public class BaseSetup {
  
  
  /**  Placement of assorted structure types based on internal demand:
    */
  final Base base;
  final World world;
  
  //private BaseDemands demands = new BaseDemands(this);  //  Setup this.
  
  
  public BaseSetup(Base base, World world) {
    this.base  = base ;
    this.world = world;
  }
  
  
  public void loadState(Session s) throws Exception {
    //demands.loadState(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    //demands.saveState(s);
  }
  
  
  
  /**  Time-sliced automation of building-placement methods-
    */
  final static float
    FULL_EVAL_PERIOD = World.STANDARD_DAY_LENGTH,
    DEFAULT_PLACE_HP = 50;
  
  static class Placing {
    Venue sampled;
    Section placed;
    Tile exactTile;
    float rating;
  }
  
  final List <Placing> placings = new List <Placing> () {
    protected float queuePriority(Placing r) {
      return r.rating;
    }
  };
  private int initLength = 0;
  private float lastEval = -1;
  private float placeLimit = 0, amountPlaced = 0;
  
  
  public void updatePlacements() {
    if (amountPlaced > placeLimit || placings.size() == 0) {
      rankSectionPlacings();
      return;
    }
    
    final float time = world.currentTime();
    if (lastEval == -1) lastEval = time;
    float placeCounter = 0;
    placeCounter += initLength * time     / FULL_EVAL_PERIOD;
    placeCounter -= initLength * lastEval / FULL_EVAL_PERIOD;
    
    while (placeCounter > 0) {
      final Placing best = placings.removeFirst();
      if (best.sampled.inWorld()) continue;
      if (attemptExactPlacement(best)) {
        amountPlaced += best.sampled.structure.maxIntegrity();
      }
      placeCounter--;
    }
    
    placeLimit = (float) FastMath.log(10, 1 + base.relations.population());
    placeLimit *= DEFAULT_PLACE_HP;
    lastEval = time;
  }
  
  
  private void rankSectionPlacings() {
    placings.clear();
    amountPlaced = 0;
    final Venue samples[] = sampleVenues(Venue.VENUE_OWNS, true);
    
    for (Section section : world.sections.sectionsUnder(world.area())) {
      for (Venue sample : samples) {
        final Placing p = new Placing();
        placings.add(p);
        p.sampled = sample;
        p.placed = section;
        p.exactTile = null;
        p.rating = sample.ratePlacing(section);
      }
    }
    
    placings.queueSort();
    initLength = placings.size();
  }
  
  
  private boolean attemptExactPlacement(Placing placing) {
    final Venue sample = placing.sampled;
    float bestRating = -1;
    Tile  bestTile = null;
    
    //  TODO:  try and improve efficiency here.  (It would help if it were just
    //         a question of fixed areas.)
    
    for (Tile t : world.tilesIn(placing.placed.area, false)) {
      sample.setPosition(t.x, t.y, world);
      if (! sample.canPlace()) continue;
      final float rating = sample.ratePlacing(t);
      if (rating > bestRating) { bestRating = rating; bestTile = t; }
    }
    
    if (bestRating <= 0) return false;
    sample.setPosition(bestTile.x, bestTile.y, world);
    sample.doPlace(bestTile, null);
    return true;
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
  

  
  /**  Compiles a list of all facility types and their behaviour profiles for
    *  ease of iteration, etc.
    */
  private static Class        allFT[] = null;
  private static VenueProfile allFP[] = null;
  
  
  public static Class[] venueTypes() {
    if (allFT != null) return allFT;
    
    final Batch <Class       > allTypes    = new Batch();
    final Batch <VenueProfile> allProfiles = new Batch();
    
    for (Class baseClass : Assets.loadPackage("stratos.game.base")) {
      final Venue sample = VenueProfile.sampleVenue(baseClass);
      if (sample != null) {
        allTypes.add(baseClass);
        allProfiles.add(sample.profile);
      }
    }
    
    allFT = allTypes   .toArray(Class       .class);
    allFP = allProfiles.toArray(VenueProfile.class);
    return allFT;
  }
  
  
  public static VenueProfile[] facilityProfiles() {
    venueTypes();
    return allFP;
  }
  
  
  public static Venue[] sampleVenues(int owningType, boolean privateProperty) {
    final Batch <Venue> typeBatch = new Batch <Venue> ();
    
    for (VenueProfile p : facilityProfiles()) {
      final Venue sample = VenueProfile.sampleVenue(p.baseClass);
      if (sample.owningType() > owningType) continue;
      if (sample.privateProperty() != privateProperty) continue;
      typeBatch.add(sample);
    }
    return typeBatch.toArray(Venue.class);
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



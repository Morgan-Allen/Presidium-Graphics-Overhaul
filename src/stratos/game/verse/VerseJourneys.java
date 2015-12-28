/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.verse;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.economic.*;
import stratos.user.*;
import stratos.util.*;
import stratos.game.plans.Smuggling;
import stratos.content.civic.Dropship;
import static stratos.content.civic.Dropship.*;
import static stratos.game.base.BaseCommerce.*;


//
//  TODO:  It's even possible this data should be written to a separate file
//         from the world itself, and loaded distinctly?
//
//  TODO:  Try to improve time-slicing, here and in Verse/VerseBase.


public class VerseJourneys {
  
  
  /**  Data fields, construction, and save/load methods-
    */
  private static boolean
    verbose = false;
  
  final Verse universe;
  final List <Journey> journeys = new List <Journey> ();
  private int updateCounter = 0;
  
  
  protected VerseJourneys(Verse universe) {
    this.universe = universe;
  }
  
  
  public void loadState(Session s) throws Exception {
    s.loadObjects(journeys);
    updateCounter = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObjects(journeys);
    s.saveInt(updateCounter);
  }
  
  
  
  /**  Exchange methods for interfacing with the world-
    */
  public Journey journeyFor(Mobile mobile) {
    for (Journey j : journeys) {
      if (j.transport == mobile) return j;
      if (j.migrants.includes(mobile)) return j;
    }
    return null;
  }
  
  
  public Series <Journey> journeysBetween(
    Sector orig, Sector dest, Base matchBase, boolean eitherWay
  ) {
    //
    //  We sort the incoming matches in order of arrival.
    final float time = universe.world.currentTime();
    final List <Journey> matches = new List <Journey> () {
      protected float queuePriority(Journey j) {
        return time - j.arriveTime;
      }
    };
    for (Journey j : journeys) {
      final Base base = j.transport == null ? null : j.transport.base();
      if (matchBase != null && base != matchBase) continue;
      if (
        (j.origin      == orig && j.destination == dest) ||
        (j.destination == orig && j.origin      == dest && eitherWay)
      ) matches.queueAdd(j);
    }
    return matches;
  }
  
  
  public Journey nextJourneyBetween(
    Sector orig, Sector dest, Base matchBase, boolean eitherWay
  ) {
    return journeysBetween(orig, dest, matchBase, eitherWay).first();
  }
  
  
  protected void updateJourneys(int numUpdates) {
    
    boolean report = verbose;
    if (report) {
      I.say("\nUpdating journeys for universe...");
      I.say("  Updates/period: "+numUpdates+"/"+UPDATE_INTERVAL);
      I.say("  Total journeys: "+journeys.size());
    }
    
    if ((numUpdates % UPDATE_INTERVAL) != 0 || GameSettings.noShips) return;
    
    //
    //  TODO:  THIS NEEDS TO BE PUT ON A SCHEDULE
    for (Journey j : journeys) {
      j.updateJourney();
      if (j.complete()) journeys.remove(j);
    }
    //
    ///if (numUpdates % 10 == 0) reportOffworldState("\n\nREGULAR CHECKUP");
  }
  
  
  
  /**  Utility methods for handling shipping & transport-
    */
  public Series <Vehicle> allTransports() {
    final Batch <Vehicle> allShips = new Batch();
    for (Journey j : journeys) if (j.transport != null) {
      allShips.add(j.transport);
    }
    return allShips;
  }
  
  
  public boolean dueToArrive(Vehicle trans, Sector destination) {
    final Journey j = journeyFor(trans);
    if (j == null || j.destination != destination) return false;
    final float time = universe.world.currentTime();
    return trans.flightState() == STATE_AWAY && time >= j.arriveTime;
  }
  
  
  public Series <Vehicle> transportsBetween(
    Sector orig, Sector dest, Base matchBase, boolean eitherWay
  ) {
    final Batch <Vehicle> trans = new Batch();
    final Series <Journey> matches = journeysBetween(
      orig, dest, matchBase, eitherWay
    );
    for (Journey j : matches) if (j.transport != null) trans.add(j.transport);
    return trans;
  }
  

  public Vehicle nextTransportBetween(
    Sector orig, Sector dest, Base base, boolean eitherWay
  ) {
    Series <Vehicle> between = transportsBetween(orig, dest, base, eitherWay);
    return between.first();
  }
  
  
  public Sector originFor(Vehicle trans) {
    final Journey j = journeyFor(trans);
    return (j == null) ? null : j.origin;
  }
  
  
  public Sector destinationFor(Vehicle trans) {
    final Journey j = journeyFor(trans);
    return (j == null) ? null : j.destination;
  }


  public Vehicle carries(Mobile mobile) {
    final Journey j = journeyFor(mobile);
    return j == null ? null : j.transport;
  }
  
  
  
  /**  Other utility-setup methods-
    */
  //
  //  TODO:  Direct references to Dropships should not be used!  The base for
  //  the world of origin should be creating the vehicle as needed.
  
  public Vehicle setupTransport(
    Sector from, Sector goes, Base base, boolean recurs
  ) {
    final Vehicle match = nextTransportBetween(from, goes, base, recurs);
    if (match != null) return match;
    
    final Dropship ship = new Dropship(base);
    Journey journey = Journey.configForTrader(ship, from, goes, base.world);
    if (journey == null) return null;
    
    journey.refreshCrewAndCargo();
    journey.beginJourney();
    return ship;
  }
  
  
  public boolean retireTransport(Vehicle trans) {
    final Journey j = journeyFor(trans);
    if (j == null) return false;
    journeys.remove(j);
    return true;
  }
  
  
  public Vehicle setupDefaultShipping(Base base) {
    return setupTransport(
      base.commerce.homeworld(), universe.stageLocation(), base, true
    );
  }
  
  
  public boolean addLocalImmigrant(Actor actor, Base base) {
    //  TODO:  You may have to reconsider this method once offworld base
    //         simulation is more developed...
    final Sector
      local = base.world.offworld.stageLocation(),
      home  = base.commerce.homeworld();
    
    if (local == null || home == null) { I.complain(
      "\nBOTH LOCAL AND HOMEWORLD LOCATIONS MUST BE SET FOR IMMIGRATION!"+
      "\n  (Homeworld: "+home+"  Locale: "+local+")"
    ); return false; }
    
    actor.mind.assignBehaviour(Smuggling.asImmigration(actor, base.world));
    SectorBase sectorBase = universe.baseForSector(home);
    sectorBase.toggleUnit(actor, true);
    return true;
  }
  
  
  public boolean scheduleArrival(Vehicle trans, float delay) {
    final Journey j = journeyFor(trans);
    if (j == null) return false;
    j.arriveTime = universe.world.currentTime() + delay;
    return true;
  }
  
  
  public boolean scheduleLocalDrop(Base base, float delay) {
    final Sector
      orig = base.commerce.homeworld(),
      dest = universe.stageLocation ();
    
    Vehicle trans = nextTransportBetween(orig, dest, base, true);
    if (trans == null) trans = setupTransport(orig, dest, base, true);
    
    final Journey j = journeyFor(trans);
    if (j == null || trans.inWorld()) return false;
    
    j.refreshCrewAndCargo();
    j.pickupOffworldMigrants();
    j.arriveTime  = universe.world.currentTime() + delay;
    return true;
  }
  
  
  
  /**  Interface and debug methods-
    */
  public float arrivalETA(Mobile mobile, Base base) {
    //
    //  Basic sanity checks first.
    if (mobile.inWorld()) return 0;
    final float  time     = universe.world.currentTime();
    final Sector locale   = universe.stageLocation();
    final Sector resides  = universe.currentSector(mobile);
    //
    //  If the actor is currently aboard a dropship, return it's arrival date.
    Journey journey = journeyFor(mobile);
    if (journey != null && journey.destination == locale) {
      if (journey.transport != null && journey.transport.inWorld()) return 0;
      final float ETA = journey.arriveTime - time;
      return ETA < 0 ? 0 : ETA;
    }
    if (journey != null && journey.origin == locale && journey.returns()) {
      return journey.arriveTime + journey.standardTripTime() - time;
    }
    //
    //  Otherwise, try to find the next dropship likely to visit the actor's
    //  current location, and make a reasonable guess about trip times.
    journey = nextJourneyBetween(locale, resides, base, true);
    if (journey != null && journey.origin == locale && journey.returns()) {
      return journey.arriveTime + journey.standardTripTime() - time;
    }
    //
    //  If it's currently heading here, it'll have to head back after picking
    //  up passengers- and if it's already heading in but doesn't have the
    //  actor aboard, a full return trip will be needed (in and out, twice as
    //  long.)
    if (journey != null && journey.returns()) {
      return journey.arriveTime + (journey.standardTripTime() * 2) - time;
    }
    return -1;
  }
  
  
  public void reportOffworldState(String prelude) {
    I.say(prelude);
    for (Journey j : journeys) {
      j.reportJourneyState("\n  ");
    }
    for (SectorBase base : universe.bases) {
      I.say("\n"+base.location+" has the following residents:");
      for (Mobile m : base.allUnits()) {
        I.say("    "+m);
      }
    }
  }
}












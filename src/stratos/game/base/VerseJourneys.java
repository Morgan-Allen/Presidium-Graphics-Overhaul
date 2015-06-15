/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.economic.*;
import stratos.user.BaseUI;
import stratos.util.*;
import stratos.game.plans.Smuggling;
import static stratos.game.base.BaseCommerce.*;
import static stratos.game.economic.Dropship.*;



//
//  TODO:  It's even possible this data should be written to a separate file
//         from the world itself, and loaded distinctly?

public class VerseJourneys {
  
  
  /**  Data fields, construction, and save/load methods-
    */
  private static boolean
    verbose        = false,
    transitVerbose = false,
    updatesVerbose = false,
    extraVerbose   = false;
  
  //  TODO:  Calculate destination-specific travel times and accidents.
  //  TODO:  Try to improve time-slicing, here and in Verse/VerseBase.
  
  final Verse universe;
  final List <Journey> journeys = new List <Journey> ();
  private int updateCounter = 0;
  
  
  protected VerseJourneys(Verse universe) {
    this.universe = universe;
  }
  
  
  public void loadState(Session s) throws Exception {
    for (int n = s.loadInt(); n-- > 0;) {
      final Journey j = new Journey();
      j.vessel      = (Dropship) s.loadObject();
      j.origin      = (VerseLocation) s.loadObject();
      j.destination = (VerseLocation) s.loadObject();
      j.arriveTime  = s.loadFloat();
      j.returns     = s.loadBool();
      journeys.add(j);
    }
    updateCounter = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(journeys.size());
    for (Journey j : journeys) {
      s.saveObject(j.vessel     );
      s.saveObject(j.origin     );
      s.saveObject(j.destination);
      s.saveFloat (j.arriveTime );
      s.saveBool  (j.returns    );
    }
    s.saveInt(updateCounter);
  }
  
  
  
  /**  Query and update/simulation methods-
    */
  public static interface Activity extends Behaviour {
    void onWorldExit();
    void onWorldEntry();
    void whileOffworld();
    boolean doneOffworld();
    VerseLocation origin();
  }
  
  
  public static Activity activityFor(Mobile mobile) {
    if (mobile instanceof Actor) {
      final Behaviour root = ((Actor) mobile).mind.rootBehaviour();
      if (root instanceof Activity) return (Activity) root;
    }
    else if (mobile instanceof Vehicle) {
      return activityFor(((Vehicle) mobile).pilot());
    }
    return null;
  }

  
  static class Journey {
    Dropship vessel;
    VerseLocation origin;
    VerseLocation destination;
    float arriveTime;
    boolean returns;
  }
  
  
  
  /**  Exchange methods for interfacing with the world-
    */
  public Dropship setupShipping(
    VerseLocation origin, VerseLocation destination, Base base, boolean recurs
  ) {
    final Journey journey = new Journey();
    final Dropship ship = new Dropship();
    ship.assignBase(base);
    final float journeyTime = SHIP_JOURNEY_TIME;
    
    journey.vessel      = ship;
    journey.arriveTime  = journeyTime + base.world.currentTime();
    journey.origin      = origin;
    journey.destination = destination;
    journey.returns     = recurs;
    
    journeys.add(journey);
    return ship;
  }
  
  
  public boolean scheduleArrival(Dropship ship, float delay) {
    final Journey j = journeyFor(ship);
    if (j == null) return false;
    j.arriveTime = universe.world.currentTime() + delay;
    return true;
  }
  
  
  public boolean retireShip(Dropship ship) {
    final Journey j = journeyFor(ship);
    if (j == null) return false;
    journeys.remove(j);
    return true;
  }
  
  
  public VerseLocation originFor(Dropship ship) {
    final Journey j = journeyFor(ship);
    return (j == null) ? null : j.origin;
  }
  
  
  public VerseLocation destinationFor(Dropship ship) {
    final Journey j = journeyFor(ship);
    return (j == null) ? null : j.destination;
  }
  
  
  protected Journey journeyFor(Dropship ship) {
    for (Journey j : journeys) if (j.vessel == ship) return j;
    return null;
  }
  
  
  protected void updateJourneys(int numUpdates) {
    if ((numUpdates % UPDATE_INTERVAL) != 0 || GameSettings.noShips) return;
    
    //  TODO:  THIS NEEDS TO BE PUT ON A SCHEDULE
    for (Journey j : journeys) {
      updateShipping(j.vessel, j);
    }
    
    ///if (numUpdates % 10 == 0) reportOffworldState("\n\nREGULAR CHECKUP");
  }
  
  
  
  /**  Utility methods for handling ship descent:
    */
  public Batch <Dropship> allVessels() {
    final Batch <Dropship> vessels = new Batch <Dropship> ();
    for (Journey j : journeys) {
      vessels.add(j.vessel);
    }
    return vessels;
  }
  
  
  public boolean dueToArrive(Dropship ship, VerseLocation destination) {
    final Journey j = journeyFor(ship);
    if (j == null || j.destination != destination) return false;
    final float time = universe.world.currentTime();
    return ship.flightStage() == STAGE_AWAY && time >= j.arriveTime;
  }
  

  private void updateShipping(Dropship ship, Journey journey) {
    //
    //  Basic variable setup-
    final boolean report = verbose && BaseUI.currentPlayed() == ship.base();
    final Stage world     = universe.world;
    final float time      = world.currentTime();
    final int   shipStage = ship.flightStage();
    final BaseCommerce commerce = ship.base().commerce;
    final boolean
      visitWorld = journey.destination == universe.stageLocation(),
      arriving   = shipStage == STAGE_AWAY && time >= journey.arriveTime;
    if (report) {
      reportJourneyState(journey, "\nUpdating shipping for "+ship);
    }
    //
    //  Sanity checks for can't-happen events-
    if (shipStage != STAGE_AWAY && ! ship.inWorld()) {
      if (I.logEvents()) {
        I.say("\nABNORMAL STATE FOR SHIP "+ship);
        I.say("  Stage is: "+shipStage+", not in world");
      }
      retireShip(ship);
      return;
    }
    final float timeGap = time - journey.arriveTime;
    if (timeGap > (SHIP_JOURNEY_TIME * 2) && ! visitWorld) {
      if (I.logEvents()) {
        I.say("\nShip journey took too long: "+ship);
        I.say("  Arrive time:  "+journey.arriveTime);
        I.say("  Current time: "+time);
        I.say("  Time gap:     "+timeGap+"/"+SHIP_JOURNEY_TIME);
      }
      journey.arriveTime = time;
      return;
    }
    //
    //  If the ship has already landed in-world, see if it's time to depart-
    if (shipStage == STAGE_LANDED || shipStage == STAGE_BOARDING) {
      final float sinceDescent = time - journey.arriveTime;
      final boolean allAboard = ShipUtils.allAboard(ship);
      
      if (sinceDescent > SHIP_VISIT_DURATION) {
        if (shipStage == STAGE_LANDED) ship.beginBoarding();
        if (allAboard && shipStage == STAGE_BOARDING) {
          ship.beginAscent();
          
          journey.arriveTime = world.currentTime();
          journey.arriveTime += SHIP_JOURNEY_TIME * (0.5f + Rand.num());
          journey.destination = journey.origin;
          journey.origin = universe.stageLocation();

          if (I.logEvents()) I.say("\n"+ship+" IS TAKING OFF!");
          if (report) reportOffworldState("\n\n"+ship+" IS TAKING OFF");
        }
      }
    }
    //
    //  If the ship is en-route to an offworld destination, see if it's arrived
    //  there yet.
    if (arriving && ! visitWorld) {
      final VerseLocation oldOrigin = journey.origin;
      journey.arriveTime  = world.currentTime();
      journey.arriveTime  += SHIP_JOURNEY_TIME * (0.5f + Rand.num());
      journey.origin      = journey.destination;
      journey.destination = oldOrigin;
      cycleOffworldPassengers(ship, journey);
      if (! journey.returns) journeys.remove(journey);
      
      if (I.logEvents()) I.say("\n"+ship+" REACHED "+journey.origin);
      if (report) reportOffworldState("\n\n"+ship+" REACHED "+journey.origin);
    }
    //
    //  And if the ship is coming back from offworld, see if it's time to
    //  begin landing-
    if (arriving && visitWorld && ShipUtils.findLandingSite(ship, true)) {
      
      ShipUtils.findLandingSite(ship, false);
      commerce.configCargo(ship.cargo, Dropship.MAX_CAPACITY, true);
      refreshCrew(ship, true, Backgrounds.DEFAULT_SHIP_CREW);
      
      ship.beginDescent(world);
      
      if (I.logEvents()) {
        I.say("\n"+ship+" IS LANDING");
        I.say("  Area:  "+ship.landArea ());
        I.say("  Point: "+ship.dropPoint());
      }
      if (report) reportOffworldState("\n\n"+ship+" IS LANDING");
    }
  }
  

  private void refreshCrew(
    Dropship ship, boolean forLanding, Background... positions
  ) {
    //
    //  Update the ship's state of repairs while we're at this...
    if (forLanding) {
      final float repair = Nums.clamp(1.25f - (Rand.num() / 2), 0, 1);
      ship.structure.setState(Structure.STATE_INTACT, repair);
    }
    //
    //  This crew will need to be updated every now and then- in the sense of
    //  changing the roster due to losses or career changes.
    for (Background b : positions) {
      if (ship.staff().numHired(b) < Visit.countInside(b, positions)) {
        final Human staff = new Human(new Career(b), ship.base());
        staff.mind.setWork(ship);
        staff.mind.setHome(ship);
      }
    }
    //
    //  Get rid of fatigue and hunger, modulate mood, et cetera- account for
    //  the effects of time spent offworld.  And make sure they're aboard!
    for (Actor works : ship.crew()) {
      final float MH = works.health.maxHealth();
      works.health.liftFatigue (MH * Rand.num());
      works.health.takeCalories(MH, 0.25f + Rand.num());
      works.health.adjustMorale(Rand.num() / 2f);
      works.mind.clearAgenda();
      ship.setInside(works, true);
    }
  }
  
  
  private void cycleOffworldPassengers(Dropship ship, Journey journey) {
    final VerseBase base = Verse.baseForLocation(journey.origin, universe);
    
    for (Mobile m : ship.inside()) {
      final Activity a = activityFor(m);
      if (a != null) base.toggleExpat(m, true);
    }
    ship.inside().clear();
    
    if (journey.returns) for (Mobile m : base.expats()) {
      final Activity a = activityFor(m);
      if (a != null && a.doneOffworld() && a.origin() == journey.destination) {
        base.toggleExpat(m, false);
        ship.setInside(m, true);
      }
    }
  }
  
  
  
  /**  Utility methods specifically for handling local setup-
    */
  public Dropship setupDefaultShipping(Base base) {
    return setupShipping(
      base.commerce.homeworld(), universe.stageLocation(), base, true
    );
  }
  
  
  public boolean scheduleLocalDrop(Dropship ship, float delay) {
    final Journey j = journeyFor(ship);
    if (j == null || ship.inWorld()) return false;
    j.origin      = ship.base().commerce.homeworld();
    j.destination = universe.stageLocation();
    j.arriveTime  = universe.world.currentTime() + delay;
    cycleOffworldPassengers(ship, j);
    return true;
  }
  
  
  public boolean scheduleLocalDrop(Base base, float delay) {
    final VerseLocation
      orig = base.commerce.homeworld(),
      dest = universe.stageLocation();
    
    Dropship ship = nextShipBetween(orig, dest, base, true);
    if (ship == null) ship = setupShipping(orig, dest, base, true);
    return scheduleLocalDrop(ship, delay);
  }
  

  public Dropship carries(Mobile mobile) {
    for (Journey j : journeys) {
      if (j.vessel == mobile) return j.vessel;
      if (j.vessel.inside().includes(mobile)) return j.vessel;
    }
    return null;
  }
  
  
  public Dropship[] shipsBetween(
    VerseLocation orig, VerseLocation dest, Base base, boolean eitherWay
  ) {
    //
    //  We sort the incoming matches in order of arrival.
    final float time = universe.world.currentTime();
    final List <Journey> matches = new List <Journey> () {
      protected float queuePriority(Journey j) {
        return time - j.arriveTime;
      }
    };
    for (Journey j : journeys) if (j.vessel.base() == base && (
      (j.origin      == orig && j.destination == dest) ||
      (j.destination == orig && j.origin      == dest && eitherWay)
    )) {
      matches.queueAdd(j);
    }
    //
    //  And return in a more compact format-
    final Dropship between[] = new Dropship[matches.size()];
    int i = 0; for (Journey j : matches) between[i++] = j.vessel;
    return between;
  }
  

  public Dropship nextShipBetween(
    VerseLocation orig, VerseLocation dest, Base base, boolean eitherWay
  ) {
    final Dropship between[] = shipsBetween(orig, dest, base, eitherWay);
    if (between.length == 0) return null;
    else return between[0];
  }
  
  
  public boolean addLocalImmigrant(Actor actor, Base base) {
    final VerseLocation
      local = base.world.offworld.stageLocation(),
      home  = base.commerce.homeworld();
    
    if (local == null || home == null) { I.complain(
      "\nBOTH LOCAL AND HOMEWORLD LOCATIONS MUST BE SET FOR IMMIGRATION!"+
      "\n  (Homeworld: "+home+"  Locale: "+local+")"
    ); return false; }
    
    final Stage world = base.world;
    Dropship ship = null;
    if (ship == null) ship = nextShipBetween(home, local, base, true);
    if (ship == null) ship = setupShipping  (home, local, base, true);
    if (ship == null) {
      I.complain("\nCOULD NOT SET UP SHIPPING BETWEEN "+home+" AND "+local+"!");
      return false;
    }
    
    actor.mind.assignBehaviour(new Smuggling(actor, ship, world, false));
    Verse.baseForLocation(home, universe).toggleExpat(actor, true);
    return true;
  }
  
  
  public void handleEmmigrants(Dropship ship) {
    for (Mobile migrant : ship.inside()) {
      final Activity a = activityFor(migrant);
      if (migrant.inWorld()) migrant.exitToOffworld();
      if (a != null) a.onWorldExit();
    }
  }
  
  
  
  /**  Interface and debug methods-
    */
  public float arrivalETA(Mobile mobile, Base base) {
    //
    //  Basic sanity checks first.
    if (mobile.inWorld()) return 0;
    final float time = universe.world.currentTime();
    final VerseLocation locale = universe.stageLocation();
    //
    //  If the actor is currently aboard a dropship, return it's arrival date.
    Dropship carries = carries(mobile);
    Journey  journey = journeyFor(carries);
    if (carries != null && journey.destination == locale) {
      final float ETA = journey.arriveTime - time;
      if (ETA < 0 || carries.inWorld()) return 0;
      else return ETA;
    }
    //
    //  Otherwise, try to find the next dropship likely to visit the actor's
    //  current location, and make a reasonable guess about trip times.
    //
    //  If it's currently heading out, it'll have to head back after picking up
    //  passengers- and if it's already heading in but doesn't have the actor
    //  aboard, a full return trip will be needed (in and out, twice as long.)
    final VerseLocation resides = Verse.currentLocation(mobile, universe);
    final float tripTime = SHIP_VISIT_DURATION + SHIP_JOURNEY_TIME;
    carries = nextShipBetween(locale, resides, base, false);
    journey = journeyFor(carries);
    if (carries != null) {
      return journey.arriveTime + tripTime - time;
    }
    carries = nextShipBetween(resides, locale, base, false);
    journey = journeyFor(carries);
    if (carries != null) {
      return journey.arriveTime + (tripTime * 2) - time;
    }
    return -1;
  }
  
  
  public void reportJourneyState(Journey journey, String prelude) {
    final Dropship ship = journey.vessel;
    final float time = universe.world.currentTime();
    I.say(prelude);
    I.say("  Current time:   "+time+" (arrives at "+journey.arriveTime+")");
    I.say("  Current stage:  "+ship.flightStage()+" in world: "+ship.inWorld());
    I.say("  Origin:         "+journey.origin);
    I.say("  Destination:    "+journey.destination);
    I.say("  Structure okay: "+ship.structure().intact());
    I.say("  Should arrive?  "+(time >= journey.arriveTime));
    I.say("  Passengers:");
    for (Mobile m : ship.inside()) {
      I.say("    "+m);
    }
  }
  
  
  public void reportOffworldState(String prelude) {
    I.say(prelude);
    for (Journey j : journeys) {
      reportJourneyState(j, "\n"+j.vessel+" is travelling between...");
    }
    for (VerseBase base : universe.bases) {
      I.say("\n"+base.location+" has the following residents:");
      for (Mobile m : base.expats()) {
        I.say("    "+m);
      }
    }
  }
}












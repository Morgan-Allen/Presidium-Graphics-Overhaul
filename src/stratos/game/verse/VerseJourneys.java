/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.verse;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.base.BaseCommerce;
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
      j.transport   = (Vehicle) s.loadObject();
      j.migrants    = (Batch) s.loadObjects(j.migrants);
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
      s.saveObject (j.transport  );
      s.saveObjects(j.migrants   );
      s.saveObject (j.origin     );
      s.saveObject (j.destination);
      s.saveFloat  (j.arriveTime );
      s.saveBool   (j.returns    );
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
  
  
  public static VerseLocation originFor(Mobile mobile) {
    final Journey j = mobile.base().world.offworld.journeys.journeyFor(mobile);
    return j == null ? null : j.origin;
  }
  
  
  public static VerseLocation destinationFor(Mobile mobile) {
    final Journey j = mobile.base().world.offworld.journeys.journeyFor(mobile);
    return j == null ? null : j.destination;
  }
  
  
  
  /**  Exchange methods for interfacing with the world-
    */
  //
  //  TODO:  Adapt this to both land and space transport routes.
  public Dropship setupTransport(
    VerseLocation origin, VerseLocation destination, Base base, boolean recurs
  ) {
    final Dropship ship = new Dropship();
    final Journey journey = setupJourney(origin, destination, recurs);
    journey.transport = ship;
    ship.assignBase(base);
    cycleOffworldPassengers(journey);
    return ship;
  }
  
  
  protected Journey setupJourney(
    VerseLocation origin, VerseLocation destination, boolean recurs
  ) {
    final Journey journey     = new Journey();
    final Stage   world       = universe.world;
    final float   journeyTime = SHIP_JOURNEY_TIME;
    
    journey.arriveTime  = journeyTime + world.currentTime();
    journey.origin      = origin;
    journey.destination = destination;
    journey.returns     = recurs;
    
    journeys.add(journey);
    return journey;
  }
  
  
  public boolean scheduleArrival(Vehicle trans, float delay) {
    final Journey j = journeyFor(trans);
    if (j == null) return false;
    j.arriveTime = universe.world.currentTime() + delay;
    return true;
  }
  
  
  public boolean retireTransport(Vehicle trans) {
    final Journey j = journeyFor(trans);
    if (j == null) return false;
    journeys.remove(j);
    return true;
  }
  
  
  public VerseLocation originFor(Vehicle trans) {
    final Journey j = journeyFor(trans);
    return (j == null) ? null : j.origin;
  }
  
  
  public VerseLocation destinationFor(Vehicle trans) {
    final Journey j = journeyFor(trans);
    return (j == null) ? null : j.destination;
  }
  
  
  protected void updateJourneys(int numUpdates) {
    if ((numUpdates % UPDATE_INTERVAL) != 0 || GameSettings.noShips) return;
    
    //  TODO:  THIS NEEDS TO BE PUT ON A SCHEDULE
    for (Journey j : journeys) updateJourney(j);
    //
    ///if (numUpdates % 10 == 0) reportOffworldState("\n\nREGULAR CHECKUP");
  }
  
  
  
  /**  Utility methods for handling ship descent:
    */
  public Batch <Dropship> allDropships() {
    final Batch <Dropship> allShips = new Batch <Dropship> ();
    for (Journey j : journeys) if (j.transport instanceof Dropship) {
      allShips.add((Dropship) j.transport);
    }
    return allShips;
  }
  
  
  public boolean dueToArrive(Vehicle trans, VerseLocation destination) {
    final Journey j = journeyFor(trans);
    if (j == null || j.destination != destination) return false;
    final float time = universe.world.currentTime();
    return trans.flightStage() == STAGE_AWAY && time >= j.arriveTime;
  }
  

  private void updateJourney(Journey journey) {
    //
    //  Basic variable setup-
    final Stage        world     = universe.world;
    final float        time      = world.currentTime();
    final Vehicle      trans     = journey.transport;
    final boolean      hasTrans  = trans != null;
    final int          shipStage = hasTrans ? trans.flightStage() : STAGE_AWAY;
    
    final boolean
      visitWorld = journey.destination == universe.stageLocation(),
      arriving   = shipStage == STAGE_AWAY && time >= journey.arriveTime,
      canEnter   = trans == null || ShipUtils.findLandingSite(trans, true);
    
    final Base    playBase = BaseUI.currentPlayed();
    final boolean report   = verbose && hasTrans && playBase == trans.base();
    final String  label    = ""+journey.transport;
    if (report) {
      reportJourneyState(journey, "\nUpdating journey for "+label);
    }
    //
    //  Sanity checks for can't-happen events-
    if (hasTrans && shipStage != STAGE_AWAY && ! trans.inWorld()) {
      if (I.logEvents()) {
        I.say("\nABNORMAL STATE FOR TRANSPORT- "+trans);
        I.say("  Stage is: "+shipStage+", not in world");
      }
      retireTransport(trans);
      return;
    }
    final float timeGap = time - journey.arriveTime;
    if (timeGap > (SHIP_JOURNEY_TIME * 2) && ! visitWorld) {
      if (I.logEvents()) {
        I.say("\nJourney took too long for "+label);
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
      final boolean allAboard = ShipUtils.allAboard(trans);
      
      if (sinceDescent > SHIP_VISIT_DURATION) {
        if (shipStage == STAGE_LANDED) trans.beginBoarding();
        if (allAboard && shipStage == STAGE_BOARDING) {
          trans.beginTakeoff();
          
          journey.arriveTime = world.currentTime();
          journey.arriveTime += SHIP_JOURNEY_TIME * (0.5f + Rand.num());
          journey.destination = journey.origin;
          journey.origin = universe.stageLocation();

          if (I.logEvents()) I.say("\n"+trans+" IS TAKING OFF!");
          if (report) reportOffworldState("\n\n"+trans+" IS TAKING OFF");
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
      cycleOffworldPassengers(journey);
      if (! journey.returns) journeys.remove(journey);
      
      if (I.logEvents()) I.say("\n"+label+" REACHED "+journey.origin);
      if (report) reportOffworldState("\n\n"+label+" REACHED "+journey.origin);
    }
    //
    //  And if the ship is coming back from offworld, see if it's time to
    //  begin landing-
    if (arriving && visitWorld && canEnter) {
      if (hasTrans) {
        ShipUtils.findLandingSite(trans, false);
        trans.base().advice.sendArrivalMessage(trans, journey.origin);
      }
      
      for (Mobile m : journey.migrants) trans.setInside(m, true);
      journey.migrants.clear();
      trans.beginLanding(world);
      
      if (I.logEvents() && hasTrans) {
        I.say("\n"+trans+" IS LANDING");
        I.say("  Area:  "+trans.landArea ());
        I.say("  Point: "+trans.dropPoint());
      }
      if (report) reportOffworldState("\n\n"+label+" IS LANDING");
    }
  }
  
  
  private void cycleOffworldPassengers(Journey journey) {
    final VerseBase base = Verse.baseForLocation(journey.origin, universe);
    final Vehicle transport = journey.transport;
    
    if (transport != null) {
      refreshCrewAndCargo(transport, true, Backgrounds.DEFAULT_SHIP_CREW);
    }
    
    for (Mobile m : journey.migrants) {
      final Activity a = activityFor(m);
      if (a != null) base.toggleExpat(m, true);
    }
    journey.migrants.clear();
    
    if (journey.returns) for (Mobile m : base.expats()) {
      final Activity a = activityFor(m);
      if (a != null && a.doneOffworld() && a.origin() == journey.destination) {
        base.toggleExpat(m, false);
        journey.migrants.add(m);
      }
    }
  }
  
  
  private void refreshCrewAndCargo(
    Vehicle transport, boolean forLanding, Background... positions
  ) {
    //
    //  Configure the ship's cargo capacity-
    final BaseCommerce commerce = transport.base().commerce;
    commerce.configCargo(transport.cargo, Dropship.MAX_CAPACITY, true);
    //
    //  Update the ship's state of repairs while we're at this...
    if (forLanding) {
      final float repair = Nums.clamp(1.25f - (Rand.num() / 2), 0, 1);
      transport.structure.setState(Structure.STATE_INTACT, repair);
    }
    //
    //  This crew will need to be updated every now and then- in the sense of
    //  changing the roster due to losses or career changes.
    for (Background b : positions) {
      if (transport.staff().numHired(b) < Visit.countInside(b, positions)) {
        final Human staff = new Human(new Career(b), transport.base());
        staff.mind.setWork(transport);
        staff.mind.setHome(transport);
      }
    }
    //
    //  Get rid of fatigue and hunger, modulate mood, et cetera- account for
    //  the effects of time spent offworld.  And make sure they're aboard!
    for (Actor works : transport.crew()) {
      final float MH = works.health.maxHealth();
      works.health.liftFatigue (MH * Rand.num());
      works.health.takeCalories(MH, 0.25f + Rand.num());
      works.health.adjustMorale(Rand.num() / 2f);
      works.mind.clearAgenda();
      transport.setInside(works, true);
    }
  }
  
  
  
  /**  Utility methods specifically for handling local setup-
    */
  public Dropship setupDefaultShipping(Base base) {
    return setupTransport(
      base.commerce.homeworld(), universe.stageLocation(), base, true
    );
  }
  
  
  public boolean scheduleLocalDrop(Base base, float delay) {
    final VerseLocation
      orig = base.commerce.homeworld(),
      dest = universe.stageLocation();
    
    Vehicle trans = nextTransportBetween(orig, dest, base, true);
    if (trans == null) trans = setupTransport(orig, dest, base, true);
    
    final Journey j = journeyFor(trans);
    if (j == null || trans.inWorld()) return false;
    j.arriveTime  = universe.world.currentTime() + delay;
    cycleOffworldPassengers(j);
    return true;
  }
  
  
  public Journey journeyFor(Mobile mobile) {
    for (Journey j : journeys) {
      if (j.transport == mobile) return j;
      if (j.migrants.includes(mobile)) return j;
    }
    return null;
  }
  
  
  public Journey journeyFor(Vehicle trans) {
    for (Journey j : journeys) if (j.transport == trans) return j;
    return null;
  }
  

  public Vehicle carries(Mobile mobile) {
    final Journey j = journeyFor(mobile);
    return j == null ? null : j.transport;
  }
  
  
  public Series <Journey> journeysBetween(
    VerseLocation orig, VerseLocation dest, Base matchBase, boolean eitherWay
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
    VerseLocation orig, VerseLocation dest, Base matchBase, boolean eitherWay
  ) {
    return journeysBetween(orig, dest, matchBase, eitherWay).first();
  }
  
  
  public Vehicle[] transportsBetween(
    VerseLocation orig, VerseLocation dest, Base matchBase, boolean eitherWay
  ) {
    final Batch <Vehicle> trans = new Batch();
    final Series <Journey> matches = journeysBetween(
      orig, dest, matchBase, eitherWay
    );
    for (Journey j : matches) if (j.transport != null) trans.add(j.transport);
    return trans.toArray(Vehicle.class);
  }
  

  public Vehicle nextTransportBetween(
    VerseLocation orig, VerseLocation dest, Base base, boolean eitherWay
  ) {
    final Vehicle between[] = transportsBetween(orig, dest, base, eitherWay);
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
    Vehicle trans = null;
    if (trans == null) trans = nextTransportBetween(home, local, base, true);
    if (trans == null) trans = setupTransport      (home, local, base, true);
    if (trans == null) {
      I.complain("\nCOULD NOT SET UP SHIPPING BETWEEN "+home+" AND "+local+"!");
      return false;
    }
    
    actor.mind.assignBehaviour(new Smuggling(actor, trans, world, false));
    Verse.baseForLocation(home, universe).toggleExpat(actor, true);
    return true;
  }
  
  
  public void handleEmmigrants(VerseLocation goes, Vehicle transport) {
    handleEmmigrants(universe.stageLocation(), goes, transport);
  }
  
  
  public void handleEmmigrants(VerseLocation goes, Mobile... migrants) {
    handleEmmigrants(universe.stageLocation(), goes, null, migrants);
  }
  
  
  protected void handleEmmigrants(
    VerseLocation from, VerseLocation goes,
    Vehicle transport, Mobile... migrants
  ) {
    Journey j = journeyFor(transport);
    if (j == null) j = setupJourney(from, goes, false);
    
    if (transport != null) for (Mobile migrant : transport.inside()) {
      j.migrants.add(migrant);
    }
    for (Mobile migrant : migrants) if (migrant != null) {
      j.migrants.add(migrant);
    }
    for (Mobile migrant : j.migrants) {
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
    final float tripTime = SHIP_VISIT_DURATION + SHIP_JOURNEY_TIME;
    final VerseLocation locale  = universe.stageLocation();
    final VerseLocation resides = Verse.currentLocation(mobile, universe);
    //
    //  If the actor is currently aboard a dropship, return it's arrival date.
    Journey journey = journeyFor(mobile);
    if (journey != null && journey.destination == locale) {
      if (journey.transport != null && journey.transport.inWorld()) return 0;
      final float ETA = journey.arriveTime - time;
      return ETA < 0 ? 0 : ETA;
    }
    if (journey != null && journey.origin == locale && journey.returns) {
      return journey.arriveTime + tripTime - time;
    }
    //
    //  Otherwise, try to find the next dropship likely to visit the actor's
    //  current location, and make a reasonable guess about trip times.
    journey = this.nextJourneyBetween(locale, resides, base, true);
    if (journey != null && journey.origin == locale && journey.returns) {
      return journey.arriveTime + tripTime - time;
    }
    //
    //  If it's currently heading here, it'll have to head back after picking
    //  up passengers- and if it's already heading in but doesn't have the
    //  actor aboard, a full return trip will be needed (in and out, twice as
    //  long.)
    if (journey != null && journey.returns) {
      return journey.arriveTime + (tripTime * 2) - time;
    }
    return -1;
  }
  
  
  public void reportJourneyState(Journey journey, String prelude) {
    final Vehicle trans = journey.transport;
    final float time = universe.world.currentTime();
    final int stage = trans.flightStage();
    I.say(prelude);
    I.say("  Current time:   "+time+" (arrives at "+journey.arriveTime+")");
    I.say("  Current stage:  "+stage+" in world: "+trans.inWorld());
    I.say("  Origin:         "+journey.origin);
    I.say("  Destination:    "+journey.destination);
    I.say("  Structure okay: "+trans.structure().intact());
    I.say("  Should arrive?  "+(time >= journey.arriveTime));
    I.say("  Passengers:");
    for (Mobile m : trans.inside()) {
      I.say("    "+m);
    }
  }
  
  
  public void reportOffworldState(String prelude) {
    I.say(prelude);
    for (Journey j : journeys) {
      reportJourneyState(j, "\n"+j.transport+" is travelling between...");
    }
    for (VerseBase base : universe.bases) {
      I.say("\n"+base.location+" has the following residents:");
      for (Mobile m : base.expats()) {
        I.say("    "+m);
      }
    }
  }
}












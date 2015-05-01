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
    verbose        = true ,
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
      j.vessel      = (Vehicle) s.loadObject();
      j.origin      = (VerseLocation) s.loadObject();
      j.destination = (VerseLocation) s.loadObject();
      j.arriveTime  = s.loadFloat();
      journeys.add(j);
    }
    updateCounter = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(journeys.size());
    for (Journey j : journeys) {
      s.saveObject     (j.vessel     );
      s.saveObject     (j.origin     );
      s.saveObject     (j.destination);
      s.saveFloat      (j.arriveTime );
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
    Vehicle vessel;
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
  
  
  protected Journey journeyFor(Dropship ship) {
    for (Journey j : journeys) if (j.vessel == ship) return j;
    return null;
  }
  
  
  protected void updateJourneys() {
    //if ((numUpdates % UPDATE_INTERVAL) != 0) return;
    
    //  TODO:  THIS NEEDS TO BE PUT ON A SCHEDULE
    for (Journey j : journeys) {
      
      if (j.vessel instanceof Dropship) updateShipping((Dropship) j.vessel, j);
    }
  }
  
  
  
  /**  Utility methods for handling ship descent:
    */
  public Batch <Dropship> allVessels() {
    final Batch <Dropship> vessels = new Batch <Dropship> ();
    for (Journey j : journeys) if (j.vessel instanceof Dropship) {
      vessels.add((Dropship) j.vessel);
    }
    return vessels;
  }
  

  private void updateShipping(Dropship ship, Journey journey) {
    final boolean report = verbose && BaseUI.currentPlayed() == ship.base();
    
    final Stage world     = universe.world;
    final float time      = world.currentTime();
    final int   shipStage = ship.flightStage();
    final BaseCommerce commerce = ship.base().commerce;
    
    final boolean
      visitWorld = journey.destination == universe.stageLocation(),
      arriving   = shipStage == STAGE_AWAY && time >= journey.arriveTime,
      canLand    = ShipUtils.findLandingSite(ship, ship.base());
    //
    //  If the ship has already landed, see if it's time to depart-
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
    }
    //
    //  And if the ship is coming back from offworld, see if it's time to
    //  return-
    if (arriving && visitWorld && canLand) {
      if (report) I.say("\nSENDING DROPSHIP TO "+ship.landArea());
      
      commerce.configCargo(ship.cargo, Dropship.MAX_CAPACITY, true);
      refreshCrew(ship, true, Backgrounds.DEFAULT_SHIP_CREW);
      
      for (Actor c : ship.crew()) ship.setInside(c, true);
      ship.beginDescent(world);
    }
  }
  

  private void refreshCrew(
    Dropship ship, boolean forLanding, Background... positions
  ) {
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
    //  the effects of time spent offworld.
    for (Actor works : ship.crew()) {
      final float MH = works.health.maxHealth();
      works.health.liftFatigue (MH * Rand.num());
      works.health.takeCalories(MH, 0.25f + Rand.num());
      works.health.adjustMorale(Rand.num() / 2f);
      works.mind.clearAgenda();
    }
    //
    //  Update the ship's state of repairs while we're at it-
    if (forLanding) {
      final float repair = Nums.clamp(1.25f - (Rand.num() / 2), 0, 1);
      ship.structure.setState(Structure.STATE_INTACT, repair);
    }
  }
  
  
  private void cycleOffworldPassengers(Dropship ship, Journey journey) {
    final VerseBase base = Verse.baseForLocation(journey.origin, universe);
    
    for (Mobile m : ship.inside()) {
      final Activity a = activityFor(m);
      if (a != null) base.toggleResident(m, true);
    }
    ship.inside().clear();
    
    if (journey.returns) for (Mobile m : base.residents()) {
      final Activity a = activityFor(m);
      if (a != null && a.doneOffworld() && a.origin() == journey.destination) {
        base.toggleResident(m, false);
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
    if (j == null) return false;
    j.origin      = ship.base().commerce.homeworld();
    j.destination = universe.stageLocation();
    j.arriveTime  = universe.world.currentTime() + delay;
    cycleOffworldPassengers(ship, j);
    return true;
  }
  
  
  public boolean scheduleLocalDrop(Base base, float delay) {
    final Dropship ship = setupDefaultShipping(base);
    return scheduleLocalDrop(ship, delay);
  }
  

  public Dropship carries(Actor aboard) {
    for (Journey j : journeys) if (j.vessel instanceof Dropship) {
      if (j.vessel.inside().includes(aboard)) return (Dropship) j.vessel;
    }
    return null;
  }
  
  
  public float arrivalETA(Actor hired, Base base) {
    //
    //  Basic sanity checks first.
    if (hired.inWorld()) return 0;
    final float time = universe.world.currentTime();
    final VerseLocation locale = universe.stageLocation();
    //
    //  If the actor is currently aboard a dropship, return it's arrival date.
    Dropship carries = carries(hired);
    Journey  journey = journeyFor(carries);
    if (carries != null && journey.destination == locale) {
      final float ETA = journey.arriveTime - time;
      if (ETA < 0 || carries.inWorld()) return 0;
      else return ETA;
    }
    //
    //  Otherwise, try to find the next dropship likely to visit the actor's
    //  current location, and make a reasonable guess about trip times.
    final VerseLocation resides = Verse.currentLocation(hired, universe);
    carries = nextShipBetween(resides, locale, base, true);
    journey = journeyFor(carries);
    if (carries == null) return -1;
    final float tripTime = SHIP_VISIT_DURATION + SHIP_JOURNEY_TIME;
    //
    //  If it's currently heading out, it'll have to head back after picking up
    //  passengers- and if it's already heading in but doesn't have the actor
    //  aboard, a full return trip will be needed (in and out, twice as long.)
    float returnTime = journey.arriveTime - time;
    if (journey.destination == resides) returnTime += tripTime * 1;
    else                                returnTime += tripTime * 2;
    return returnTime;
  }
  
  
  public Dropship nextShipBetween(
    VerseLocation origin, VerseLocation destination,
    Base base, boolean eitherWay
  ) {
    if (eitherWay) {
      Dropship returns = nextShipBetween(destination, origin, base, false);
      if (returns != null) return returns;
    }
    
    final Pick <Journey> pick = new Pick <Journey> ();
    final float time = base.world.currentTime();
    
    for (Journey j : journeys) {
      if (j.vessel.base() != base || ! (j.vessel instanceof Dropship)) continue;
      if (j.origin != origin || j.destination != destination) continue;
      pick.compare(j, j.arriveTime - time);
    }
    if (pick.empty()) return null;
    return (Dropship) pick.result().vessel;
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
    if (ship == null) ship = nextShipBetween(home , local, base, true);
    if (ship != null) {
      actor.mind.assignBehaviour(new Smuggling(actor, ship, world, false));
      Verse.baseForLocation(home, universe).toggleResident(actor, true);
      return true;
    }
    
    I.complain("\nNO SUITABLE SHIP FOUND BETWEEN "+home+" AND "+local+"!");
    return false;
  }
}







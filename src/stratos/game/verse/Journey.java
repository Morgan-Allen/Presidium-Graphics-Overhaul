/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.verse;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.base.BaseCommerce.*;
import static stratos.game.economic.Vehicle.*;



public class Journey implements Session.Saveable {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  private static boolean
    verbose = true ;
  
  final public static int
    IS_TRADING   = 1 << 0,
    IS_MISSION   = 1 << 1,
    IS_ESCAPE    = 1 << 2,
    IS_SINGLE    = 1 << 3,
    IS_RETURN    = 1 << 4,
    IS_RECURRING = 1 << 5;
  final static int
    STAGE_INIT     = -1,
    STAGE_OUTWARD  =  0,
    STAGE_ARRIVED  =  1,
    STAGE_RETURN   =  2,
    STAGE_RETURNED =  3,
    STAGE_COMPLETE =  4;
  
  final Verse verse;
  final int properties;
  final Base client;
  
  Sector   origin;
  Sector   destination;
  Boarding worldTarget;
  float    departTime = -1;
  float    arriveTime = -1;
  int      tripStage  = STAGE_INIT;
  
  boolean begun = false;
  Vehicle transport;
  Boarding transitPoint;
  Batch <Mobile> migrants = new Batch();
  
  
  
  protected Journey(Verse verse, int properties, Base client) {
    this.verse      = verse;
    this.properties = properties;
    this.client     = client;
  }
  
  
  public Journey(Session s) throws Exception {
    s.cacheInstance(this);
    verse        = s.world().offworld;
    properties   = s.loadInt();
    client       = (Base) s.loadObject();
    
    origin       = (Sector) s.loadObject();
    destination  = (Sector) s.loadObject();
    worldTarget  = (Boarding) s.loadObject();
    departTime   = s.loadFloat();
    arriveTime   = s.loadFloat();
    tripStage    = s.loadInt();
    
    begun        = s.loadBool();
    transport    = (Vehicle ) s.loadObject();
    transitPoint = (Boarding) s.loadObject();
    s.loadObjects(migrants);
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt    (properties );
    s.saveObject (client     );
    
    s.saveObject (origin     );
    s.saveObject (destination);
    s.saveObject (worldTarget);
    s.saveFloat  (departTime );
    s.saveFloat  (arriveTime );
    s.saveInt    (tripStage  );
    
    s.saveBool   (begun       );
    s.saveObject (transport   );
    s.saveObject (transitPoint);
    s.saveObjects(migrants    );
  }
  
  
  
  /**  Other general access and query methods-
    */
  public Vehicle transport() {
    return transport;
  }
  
  
  public Boarding transitPoint() {
    return transitPoint;
  }
  
  
  public Boarding migrantTransitPoint() {
    if (transport != null) {
      if (transport.landed()) return transport;
      else return null;
    }
    else return transitPoint;
  }
  
  
  public boolean hasProperty(int p) {
    return (properties & p) == p;
  }
  
  
  public boolean forTrading() {
    return hasProperty(IS_TRADING);
  }
  
  
  public boolean forMission() {
    return hasProperty(IS_MISSION);
  }
  
  
  public boolean forEscape() {
    return hasProperty(IS_ESCAPE);
  }
  
  
  public boolean returns() {
    return hasProperty(IS_RETURN) || hasProperty(IS_RECURRING);
  }
  
  
  public boolean hasBegun() {
    return begun;
  }
  
  
  public boolean complete() {
    return tripStage == STAGE_COMPLETE;
  }
  
  
  public boolean hasArrived() {
    return tripStage == STAGE_ARRIVED;
  }
  
  
  
  /**  Factory methods for exteral access-
    */
  public static Journey configForTrader(
    Vehicle trader, Sector from, Sector goes, Stage world
  ) {
    final Verse verse = world.offworld;
    Journey journey = verse.journeys.journeyFor(trader);
    final int props = IS_TRADING | IS_RECURRING;
    if (journey == null) journey = new Journey(verse, props, trader.base());
    
    journey.origin       = from;
    journey.destination  = goes;
    journey.worldTarget  = null;
    journey.transport    = trader;
    journey.transitPoint = trader;

    if (! journey.checkTransitPoint()) return null;
    return journey;
  }
  
  
  public static Journey configForMission(Mission mission) {
    
    //  TODO:  You need proper handling for missions *between* or *from*
    //  offworld sectors as well as missions *to* offworld sectors...
    
    final Base base = mission.base();
    final Property HQ = base.HQ();
    if (HQ == null || ! mission.isOffworld()) return null;
    
    final Verse verse = mission.base().world.offworld;
    final Object subject = mission.subject();
    
    Sector from = verse.stageLocation(), goes = null;
    if (subject instanceof Sector) goes = (Sector) subject;
    if (subject instanceof Mobile) goes = verse.currentSector((Mobile) subject);
    if (goes == null || goes == from) return null;
    
    final int props = IS_MISSION | IS_RETURN;
    final Journey journey = new Journey(verse, props, base);
    journey.origin      = from;
    journey.destination = goes;
    journey.worldTarget = HQ  ;
    journey.transport   = EntryPoints.findTransport(HQ, goes, base);
    
    if (! journey.checkTransitPoint()) return null;
    return journey;
  }
  
  
  public static Journey configAsEscape(
    Boarding from, Sector goes, Stage world, Mobile flees
  ) {
    final Verse verse = world.offworld;
    final int props = IS_ESCAPE | IS_SINGLE;
    final Journey journey = new Journey(verse, props, flees.base());
    
    journey.origin       = world.localSector();
    journey.destination  = goes;
    journey.transport    = null;
    journey.transitPoint = from;

    if (! journey.checkTransitPoint()) return null;
    return journey;
  }
  
  
  private boolean checkTransitPoint() {
    final boolean leaveWorld = origin      == verse.stageLocation();
    final boolean visitWorld = destination == verse.stageLocation();
    final boolean airborne = transport != null && transport.motionFlyer();
    //
    //  If you can't fly, you can't visit distant sectors:
    if ((! airborne) && (! origin.borders(destination))) {
      return false;
    }
    //
    //  Any transport that visits the world needs to have a suitable landing
    //  site:
    if (visitWorld && transport != null) {
      EntryPoints.findLandingSite(transport, this, true);
      if (transport.dropPoint() == null) return false;
    }
    //
    //  Finally, all ground journeys either visiting or leaving the world must
    //  have an entry point along the border:
    if ((leaveWorld || visitWorld) && (! airborne)) {
      final Sector offworld = visitWorld ? origin : destination;
      transitPoint = EntryPoints.findBorderPoint(
        worldTarget, offworld, (Tile) transitPoint, client
      );
      if (transitPoint == null) return false;
    }
    return true;
  }
  
  
  
  /**  Public interface to allow off-world behaviours to be updated and
    *  resolved:
    */
  public static interface Purpose extends Behaviour {
    void onWorldExit();
    void onWorldEntry();
    void whileOffworld();
    boolean doneOffworld();
    
    //  TODO:  You'll need more fine-grained control over which expats will
    //         sign up for a Journey- or which transports will accept them back.
    Sector origin();
  }
  
  
  public static Purpose activityFor(Mobile mobile) {
    if (mobile instanceof Actor) {
      final Behaviour root = ((Actor) mobile).mind.rootBehaviour();
      if (root instanceof Journey.Purpose) return (Journey.Purpose) root;
    }
    else if (mobile instanceof Vehicle) {
      return activityFor(((Vehicle) mobile).pilot());
    }
    return null;
  }
  
  
  public void addMigrant(Mobile m) {
    migrants.include(m);
  }
  
  
  public boolean hasMigrant(Mobile m) {
    return migrants.includes(m);
  }
  
  
  public Series <Mobile> migrants() {
    return migrants;
  }
  
  
  public float standardTripTime() {
    return origin.standardTripTime(destination, Sector.SEP_STELLAR);
  }

  
  
  /**  Regular update and life-cycle methods:
    */
  public void beginJourney(Mobile... extraMigrants) {
    if (begun) return;
    begun = true;
    
    boolean report = verbose;
    if (report) I.say("Beginning journey: "+this);
    
    final int maxSep = Sector.SEP_STELLAR;
    final float tripTime = origin.standardTripTime(destination, maxSep);
    
    departTime = verse.world.currentTime();
    arriveTime = departTime + (tripTime * (1 + Rand.num()) / 2f);
    
    if (transport != null) for (Mobile migrant : transport.inside()) {
      migrants.add(migrant);
    }
    for (Mobile migrant : extraMigrants) if (migrant != null) {
      migrants.add(migrant);
    }
    for (Mobile migrant : migrants) {
      final Journey.Purpose a = Journey.activityFor(migrant);
      if (migrant.inWorld()) migrant.exitToOffworld();
      if (a != null) a.onWorldExit();
    }
    
    verse.journeys.journeys.include(this);
  }
  
  
  protected void updateJourney() {
    final boolean report = verbose;
    final String  label  = report ? ""+labelObject() : "";
    if (report) {
      reportJourneyState("\nUpdating journey for "+label);
    }
    //
    //  Basic variable setup-
    final Stage   world      = verse.world;
    final float   time       = world.currentTime();
    final boolean hasTrans   = transport != null;
    final int     shipStage  = hasTrans ? transport.flightState() : STATE_AWAY;
    final boolean visitWorld = destination == verse.stageLocation();
    final boolean arriving   = shipStage == STATE_AWAY && time >= arriveTime;
    
    if (tripStage == STAGE_INIT) tripStage = STAGE_OUTWARD;
    if (arriving) onArrival(visitWorld);
    if (checkForDeparture(visitWorld)) beginReturnTrip();
  }
  
  
  protected boolean checkForDeparture(boolean visitWorld) {
    //
    //  NOTE:  This method is only called for automatically-recurring trips by,
    //  e.g, trade freighters.  Other trips will either be complete upon
    //  arrival or will be prompted to recall by an external control block.
    if (! hasProperty(IS_RECURRING)) return false;
    
    final float   time       = verse.world.currentTime();
    final boolean stayTimeUp = time - arriveTime > SHIP_VISIT_DURATION;
    
    if (visitWorld) {
      if (transport != null && transport.landed() && stayTimeUp) {
        final boolean boarding = transport.flightState() == STATE_BOARDING;
        if (! boarding) transport.beginBoarding();
        final boolean allAboard = PilotUtils.allAboard(transport);
        
        if (allAboard && boarding) {
          transport.beginTakeoff();
          return true;
        }
      }
    }
    else {
      if (tripStage == STAGE_ARRIVED) return true;
    }
    return false;
  }
  
  
  protected void onArrival(boolean visitWorld) {
    if      (tripStage == STAGE_OUTWARD) tripStage = STAGE_ARRIVED ;
    else if (tripStage == STAGE_RETURN ) tripStage = STAGE_RETURNED;
    else {
      I.complain("\nCANNOT ARRIVE DURING TRIP STAGE: "+tripStage);
      return;
    }
    //
    //  If you're arriving in-world, then you either tell the transport to
    //  begin landing (and it'll handle the mushy details) or you eject all
    //  your migrants at the transit-point:
    if (visitWorld && checkTransitPoint()) {
      if (transport != null) {
        for (Mobile m : migrants) transport.setInside(m, true);
        migrants.clear();
        transport.beginLanding(verse.world);
        transport.base().advice.sendArrivalMessage(transport, origin);
      }
      else {
        for (Mobile m : migrants) {
          m.enterWorldAt(transitPoint, verse.world);
        }
        migrants.clear();
      }
    }
    //
    //  In principle, all you do offworld is dump off your passengers.
    //  Then, once you're due to return, anybody with business back at your
    //  point of origin (and the same purpose) can board your vessel and return
    //  home.
    else {
      final SectorBase base = verse.baseForSector(origin);
      if (transport != null) {
        refreshCrewAndCargo(true, transport.careers());
      }
      for (Mobile m : migrants) {
        final Purpose a = activityFor(m);
        if (a != null) base.toggleExpat(m, true);
      }
      migrants.clear();
    }
  }
  
  
  public void beginReturnTrip() {
    if (tripStage != STAGE_ARRIVED) {
      I.complain("\nCANNOT BEGIN RETURN TRIP UNLESS ARRIVED!");
      return;
    }
    
    final Sector oldOrigin = origin;
    arriveTime  = verse.world.currentTime();
    arriveTime  += SHIP_JOURNEY_TIME * (0.5f + Rand.num());
    origin      = destination;
    destination = oldOrigin;
    
    if (hasProperty(IS_SINGLE)) {
      tripStage = STAGE_COMPLETE;
    }
    else if (hasProperty(IS_RETURN)) {
      if (tripStage == STAGE_RETURNED) tripStage = STAGE_COMPLETE;
      else tripStage = STAGE_RETURN;
    }
    else {
      tripStage = STAGE_OUTWARD;
    }
    
    final SectorBase base = verse.baseForSector(origin);
    final boolean offworld = origin != verse.stageLocation();
    
    if (offworld && returns()) for (Mobile m : base.expats()) {
      final Purpose a = activityFor(m);
      
      //  TODO:  If the journey relies on transport, make sure it allows the
      //  expat aboard (i.e, doesn't belong to a hostile base.)
      
      if (a != null && a.doneOffworld() && a.origin() == destination) {
        base.toggleExpat(m, false);
        migrants.add(m);
      }
    }
  }
  
  
  public void endJourney() {
    this.tripStage = STAGE_COMPLETE;
    if (transport != null) transport.assignJourney(null);
  }
  
  
  protected void refreshCrewAndCargo(
    boolean forLanding, Background... positions
  ) {
    //
    //  Configure the ship's cargo capacity-
    final BaseCommerce commerce = transport.base().commerce;
    commerce.configCargo(transport.cargo, transport.spaceCapacity(), true);
    //
    //  Update the ship's state of repairs while we're at this...
    if (forLanding) {
      final float repair = Nums.clamp(1.25f - (Rand.num() / 2), 0, 1);
      transport.structure.setState(Structure.STATE_INTACT, repair);
    }
    //
    //  This crew will need to be updated every now and then- in the sense of
    //  changing the roster due to losses or career changes.
    
    //  TODO:  Ideally, offworld base-simulation should handle this?
    final Base home = transport.base();
    for (Background b : positions) {
      if (transport.staff().numOpenings(b) > 0) {
        final Human staff = new Human(b, home);
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
  
  
  
  /**  Rendering, debug and interface methods-
    */
  public void reportJourneyState(String prelude) {
    final float time = verse.world.currentTime();
    I.say(prelude);
    I.say("  Current time:   "+time+" (arrives at "+arriveTime+")");
    I.say("  Origin:         "+origin);
    I.say("  Destination:    "+destination);
    I.say("  Should arrive?  "+(time >= arriveTime));
    I.say("  Migrants are:");
    for (Mobile m : migrants) {
      I.say("    "+m);
    }
    if (transport != null) {
      final int stage = transport.flightState();
      I.say("  Transport is:   "+transport);
      I.say("  Flight stage:   "+stage+" in world: "+transport.inWorld());
      I.say("  Structure okay: "+transport.structure().intact());
      I.say("  Passengers:     "+I.list(transport.inside().toArray()));
    }
  }
  
  
  private Object labelObject() {
    for (Mobile m : migrants) if (m instanceof Actor) {
      final Mission job = ((Actor) m).mind.mission();
      if (job != null && job.journey() == this) return job;
    }
    if (transport != null) return transport;
    if (migrants.size() >= 1) return migrants.first();
    return null;
  }
  
  
  public String toString() {
    return "Journey for "+labelObject();
  }
}












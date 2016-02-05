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
import stratos.user.BaseUI;
import stratos.util.*;



public class Journey implements Session.Saveable {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  private static boolean
    verbose = true;
  
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
    STAGE_COMPLETE =  3;
  final public static int
    TRADE_STAY_DURATION = Stage.STANDARD_HOUR_LENGTH * 6,
    RAID_STAY_DURATION  = Stage.STANDARD_HOUR_LENGTH * 4,
    DIPLO_STAY_DURATION = Stage.STANDARD_DAY_LENGTH  * 2;
  
  final Verse verse;
  final int properties;
  final Base client;
  
  Sector   origin;
  Sector   destination;
  Boarding worldTarget;
  float    departTime = -1;
  float    arriveTime = -1;
  int      tripStage  = STAGE_INIT;
  
  boolean  begun = false;
  float    maxStayTime = -1;
  Vehicle  transport;
  Boarding transitPoint;
  Batch <Mobile> migrants = new Batch();
  
  
  
  protected Journey(Verse verse, int properties, Base client) {
    this.verse      = verse;
    this.properties = properties;
    this.client     = client;
  }
  
  
  public Journey(Session s) throws Exception {
    s.cacheInstance(this);
    verse        = (Verse) s.loadObject();
    properties   = s.loadInt();
    client       = (Base) s.loadObject();
    
    origin       = (Sector) s.loadObject();
    destination  = (Sector) s.loadObject();
    worldTarget  = (Boarding) s.loadObject();
    departTime   = s.loadFloat();
    arriveTime   = s.loadFloat();
    tripStage    = s.loadInt();
    
    begun        = s.loadBool();
    maxStayTime  = s.loadFloat();
    transport    = (Vehicle ) s.loadObject();
    transitPoint = (Boarding) s.loadObject();
    s.loadObjects(migrants);
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject (verse      );
    s.saveInt    (properties );
    s.saveObject (client     );
    
    s.saveObject (origin     );
    s.saveObject (destination);
    s.saveObject (worldTarget);
    s.saveFloat  (departTime );
    s.saveFloat  (arriveTime );
    s.saveInt    (tripStage  );
    
    s.saveBool   (begun       );
    s.saveFloat  (maxStayTime );
    s.saveObject (transport   );
    s.saveObject (transitPoint);
    s.saveObjects(migrants    );
  }
  
  
  
  /**  Other general access and query methods-
    */
  public Sector origin() {
    return origin;
  }
  
  
  public Sector destination() {
    return destination;
  }
  
  
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
  
  
  public boolean didReturn() {
    return returns() && tripStage == STAGE_COMPLETE;
  }
  
  
  public boolean didArrive() {
    return tripStage >= STAGE_ARRIVED;
  }
  
  
  public float departTime() {
    return departTime;
  }
  
  
  public float arriveTime() {
    return arriveTime;
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
    journey.maxStayTime  = TRADE_STAY_DURATION;
    
    if (! journey.checkTransitPoint()) return null;
    return journey;
  }
  
  
  public static Journey configForVisit(
    Sector from, Stage world,
    Boarding entryOrTransport, Base base, float maxStayTime
  ) {
    final Verse verse = world.offworld;
    final int props = IS_MISSION | IS_RETURN;
    final Journey journey = new Journey(verse, props, base);
    
    journey.origin       = from;
    journey.destination  = world.localSector();
    journey.transitPoint = entryOrTransport;
    journey.maxStayTime  = maxStayTime;
    
    if (entryOrTransport instanceof Vehicle) {
      journey.transport = (Vehicle) entryOrTransport;
    }
    if (! journey.checkTransitPoint()) return null;
    return journey;
  }
  
  
  public static Journey configForMission(Mission mission, boolean returns) {
    final Base   base   = mission.base();
    final Verse  verse  = base.world.offworld;
    final Sector locale = base.world.localSector();
    final Sector goes   = verse.currentSector(mission.subject());
    if (goes == null || goes == locale) return null;
    
    final Boarding pathTo = base.HQ();
    final int props;
    if (returns) props = IS_MISSION | IS_RETURN;
    else         props = IS_MISSION | IS_SINGLE;
    
    final Journey journey = new Journey(verse, props, base);
    journey.origin      = locale;
    journey.destination = goes;
    journey.worldTarget = pathTo;
    journey.transport   = EntryPoints.findTransport(pathTo, goes, base);
    
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
    final boolean airborne   = transport != null && transport.motionFlyer();
    //
    //  If you can't fly, you can't visit distant sectors:
    if ((! airborne) && (! origin.borders(destination))) {
      if (transitPoint instanceof EntryPoints.Portal) {
        return ((EntryPoints.Portal) transitPoint).leadsTo() == destination;
      }
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
    Sector origin();
    boolean acceptsTransport(Vehicle t, Journey j);
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
  
  
  public float calcTripTime() {
    final int maxSep = Sector.SEP_STELLAR;
    float multiple = 1;
    
    final Sector home = client.visits.homeworld();
    if (origin == home || destination == home) multiple = 0.25f;
    
    return origin.standardTripTime(destination, maxSep) * multiple;
  }

  
  
  /**  Regular update and life-cycle methods:
    */
  public void beginJourney(Mobile... extraMigrants) {
    if (complete()) return;
    begun = true;
    
    boolean report = verbose;
    if (report) I.say("Beginning journey: "+this);
    
    final float tripTime = calcTripTime();
    
    departTime = verse.stage().currentTime();
    arriveTime = departTime + (tripTime * (0.5f + Rand.num()));
    
    pickupOnstageMigrants(extraMigrants);
    pickupOffworldMigrants();
    if (transport != null) transport.assignJourney(this);
    verse.journeys.journeys.include(this);
    
    //
    //  Finally, send a message to the targeted base.
    final Base host = worldTarget == null ? null : worldTarget.base();
    if (host != null && hasProperty(IS_MISSION)) {
      host.advice.sendMissionVisitMessage(this, false);
    }
  }
  
  
  public void setArrivalTime(float time) {
    this.arriveTime = time;
  }
  
  
  protected void updateJourney() {
    final boolean report = verbose;
    final String  label  = report ? ""+labelObject() : "";
    if (report) {
      reportJourneyState("\nUpdating journey for: "+label);
    }
    //
    //  Basic variable setup-
    final Stage   world      = verse.stage();
    final float   time       = world.currentTime();
    final boolean visitWorld = destination == verse.stageLocation();
    if (tripStage == STAGE_INIT) tripStage = STAGE_OUTWARD;
    if (time >= arriveTime) {
      onArrival(visitWorld);
      if (checkForDeparture()) beginReturnTrip();
    }
  }
  
  
  protected boolean checkForDeparture() {
    if (maxStayTime == -1 || complete()) return false;
    
    final float timeStayed = verse.stage().currentTime() - arriveTime;
    if (timeStayed > maxStayTime) return true;
    
    //  TODO:  You'll need to perform a check here to ensure that everyone is
    //  ready to depart first...
    return false;
  }
  
  
  protected void onArrival(boolean visitWorld) {
    if      (hasProperty(IS_SINGLE)    ) tripStage = STAGE_COMPLETE;
    else if (tripStage <= STAGE_OUTWARD) tripStage = STAGE_ARRIVED ;
    else if (tripStage == STAGE_RETURN ) tripStage = STAGE_COMPLETE;
    else return;
    //
    //  If you're arriving in-world, then you either tell the transport to
    //  begin landing (and it'll handle the mushy details) or you eject all
    //  your migrants at the transit-point:
    if (visitWorld && checkTransitPoint()) {
      if (transport != null) {
        for (Mobile m : migrants) transport.setInside(m, true);
        migrants.clear();
        transport.beginLanding(verse.stage());
        transport.base().advice.sendArrivalMessage(transport, origin);
      }
      else {
        for (Mobile m : migrants) {
          m.enterWorldAt(transitPoint, verse.stage());
        }
        migrants.clear();
      }
      
      final Base host = worldTarget == null ? null : worldTarget.base();
      if (host != null && hasProperty(IS_MISSION)) {
        host.advice.sendMissionVisitMessage(this, true);
      }
    }
    //
    //  In principle, all you do offworld is dump off your passengers.
    //  Then, once you're due to return, anybody with business back at your
    //  point of origin (and the same purpose) can board your vessel and return
    //  home.
    if (! visitWorld) {
      final SectorBase base = verse.baseForSector(destination);
      refreshCrewAndCargo();
      for (Mobile m : migrants) {
        final Purpose a = activityFor(m);
        if (a != null) base.toggleUnit(m, true);
      }
      migrants.clear();
    }
  }
  
  
  public void beginReturnTrip() {
    if (tripStage >= STAGE_RETURN || hasProperty(IS_SINGLE)) return;
    if (hasProperty(IS_RETURN)) tripStage = STAGE_RETURN;
    else tripStage = STAGE_OUTWARD;
    
    final Sector oldOrigin = origin;
    arriveTime  =  verse.stage().currentTime();
    arriveTime  += calcTripTime() * (0.5f + Rand.num());
    origin      =  destination;
    destination =  oldOrigin;
    
    pickupOffworldMigrants();
  }
  
  
  public void endJourney() {
    this.tripStage = STAGE_COMPLETE;
    if (transport != null) transport.assignJourney(null);
  }
  
  
  
  /**  Dealing with common conditions at the start and end of a journey:
    */
  protected void pickupOnstageMigrants(Mobile... extraMigrants) {
    if (transport != null) for (Mobile migrant : transport.inside()) {
      migrants.add(migrant);
    }
    for (Mobile migrant : extraMigrants) if (migrant != null) {
      migrants.add(migrant);
    }
    for (Mobile migrant : migrants) if (migrant.inWorld()) {
      migrant.exitToOffworld();
      final Journey.Purpose a = Journey.activityFor(migrant);
      if (a != null) a.onWorldExit();
    }
  }
  
  
  //
  //  TODO:  Ideally, either offworld base-simulation or the transport in
  //  question should handle this.
  
  protected void pickupOffworldMigrants() {
    final SectorBase base = verse.baseForSector(origin);
    final boolean offworld = origin != verse.stageLocation();
    
    if (offworld && returns()) for (Mobile m : base.allUnits()) {
      final Purpose a = activityFor(m);
      if (a == null || ! a.doneOffworld()) continue;
      
      if (a.origin() == destination && a.acceptsTransport(transport, this)) {
        base.toggleUnit(m, false);
        migrants.add(m);
      }
    }
  }
  
  
  protected void refreshCrewAndCargo() {
    if (transport == null) return;
    //
    //  Configure the ship's cargo capacity-
    final BaseVisits commerce = transport.base().visits;
    commerce.configCargo(transport.cargo, transport.spaceCapacity(), true);
    //
    //  Update the ship's state of repairs while we're at this...
    final float repair = Nums.clamp(1.25f - (Rand.num() / 2), 0, 1);
    transport.structure.setState(Structure.STATE_INTACT, repair);
    //
    //  This crew will need to be updated every now and then- in the sense of
    //  changing the roster due to losses or career changes.
    final Base home = transport.base();
    for (Background b : transport.careers()) {
      while (transport.staff().numOpenings(b) > 0) {
        final Actor staff = b.sampleFor(home);
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
    final float time = verse.stage().currentTime();
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
  
  
  public Mission mission() {
    for (Mobile m : migrants) if (m instanceof Actor) {
      final Mission job = ((Actor) m).mind.mission();
      if (job != null && job.journey() == this) return job;
    }
    return null;
  }
  
  
  private Object labelObject() {
    Mission job = mission();
    if (job != null) return job;
    if (transport != null) return transport;
    if (migrants.size() >= 1) return migrants.first();
    return null;
  }
  
  
  public String toString() {
    return "Journey for "+labelObject()+" #"+hashCode();
  }
}












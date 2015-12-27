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
    verbose = false;
  
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
  
  Sector origin;
  Sector destination;
  float  arriveTime = -1;
  int    tripStage = STAGE_INIT;
  
  boolean begun = false;
  Vehicle transport;
  Boarding transitPoint;
  Batch <Mobile> migrants = new Batch();
  
  
  
  protected Journey(Verse verse, int properties) {
    this.verse = verse;
    this.properties = properties;
  }
  
  
  public Journey(Session s) throws Exception {
    s.cacheInstance(this);
    verse        = s.world().offworld;
    properties   = s.loadInt();
    
    origin       = (Sector) s.loadObject();
    destination  = (Sector) s.loadObject();
    arriveTime   = s.loadFloat();
    tripStage    = s.loadInt();
    
    begun        = s.loadBool();
    transport    = (Vehicle ) s.loadObject();
    transitPoint = (Boarding) s.loadObject();
    migrants     = (Batch   ) s.loadObjects(migrants);
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt    (properties );
    
    s.saveObject (origin     );
    s.saveObject (destination);
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
  
  
  public boolean hasProperty(int p) {
    return (properties & p) == p;
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
    if (journey == null) journey = new Journey(verse, props);
    
    journey.origin       = from;
    journey.destination  = goes;
    journey.arriveTime   = -1;
    journey.transport    = trader;
    journey.transitPoint = trader;
    
    return journey;
  }
  
  
  public static Journey configForMission(Mission mission) {
    
    //  TODO:  You need proper handling for missions *between* offworld sectors
    //  as well as missions *from* offworld sectors...
    
    final Base base = mission.base();
    final Property HQ = base.HQ();
    if (HQ == null || ! mission.isOffworld()) return null;
    
    final Verse verse = mission.base().world.offworld;
    final Object subject = mission.subject();
    
    Sector from = verse.localSector(), goes = null;
    if (subject instanceof Sector) goes = (Sector) subject;
    if (subject instanceof Mobile) goes = verse.currentSector((Mobile) subject);
    if (goes == null || goes == from) return null;
    
    final int props = IS_MISSION | IS_RETURN;
    final Journey journey = new Journey(verse, props);
    journey.origin      = from;
    journey.destination = goes;
    journey.arriveTime  = -1;
    
    if (from.borders(goes)) {
      journey.transport = null;
      journey.transitPoint = EntryPoints.findBorderPoint(HQ, goes, base);
    }
    else {
      journey.transport = EntryPoints.findTransport(HQ, goes, base);
      journey.transitPoint = journey.transport;
    }
    
    if (journey.transitPoint == null) return null;
    return journey;
  }
  
  
  public static Journey configAsEscape(
    Boarding from, Sector goes, Stage world
  ) {
    final Verse verse = world.offworld;
    final int props = IS_ESCAPE | IS_SINGLE;
    final Journey journey = new Journey(verse, props);

    journey.origin       = world.localSector();
    journey.destination  = goes;
    journey.arriveTime   = -1;
    journey.transport    = null;
    journey.transitPoint = from;
    
    return journey;
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

  
  
  /**  Regular update and life-cycle methods:
    */
  protected void updateJourney() {
    //
    //  Basic variable setup-
    final Stage   world     = verse.world;
    final float   time      = world.currentTime();
    final Vehicle trans     = transport;
    final boolean hasTrans  = trans != null;
    final int     shipStage = hasTrans ? trans.flightState() : STATE_AWAY;
    
    final boolean
      visitWorld = destination == verse.stageLocation(),
      arriving   = shipStage == STATE_AWAY && time >= arriveTime,
      canEnter   = trans == null || EntryPoints.findLandingSite(trans, true);
    
    final Base    playBase = BaseUI.currentPlayed();
    final boolean report   = verbose && hasTrans && playBase == trans.base();
    final String  label    = ""+transport;
    if (report) {
      reportJourneyState("\nUpdating journey for "+label);
    }
    
    
    //
    //  Sanity checks for can't-happen events-
    if (hasTrans && shipStage != STATE_AWAY && ! trans.inWorld()) {
      if (I.logEvents()) {
        I.say("\nABNORMAL STATE FOR TRANSPORT- "+trans);
        I.say("  Stage is: "+shipStage+", not in world");
      }
      verse.journeys.retireTransport(trans);
      return;
    }
    final float timeGap = time - arriveTime;
    if (timeGap > (SHIP_JOURNEY_TIME * 2) && ! visitWorld) {
      if (I.logEvents()) {
        I.say("\nJourney took too long for "+label);
        I.say("  Arrive time:  "+arriveTime);
        I.say("  Current time: "+time);
        I.say("  Time gap:     "+timeGap+"/"+SHIP_JOURNEY_TIME);
      }
      arriveTime = time;
      return;
    }
    

    
    //
    //  If the ship is arriving from offworld, see if it's time to begin
    //  landing-
    if (arriving && visitWorld && canEnter) {
      if (hasTrans) {
        EntryPoints.findLandingSite(trans, false);
        trans.base().advice.sendArrivalMessage(trans, origin);
      }
      
      for (Mobile m : migrants) trans.setInside(m, true);
      migrants.clear();
      trans.beginLanding(world);
      
      if (I.logEvents() && hasTrans) {
        I.say("\n"+trans+" IS LANDING");
        I.say("  Area:  "+trans.landArea ());
        I.say("  Point: "+trans.dropPoint());
      }
    }
    
    
    //
    //  If the ship has already landed in-world, see if it's time to depart-
    if (tripStage == STAGE_INIT) tripStage = STAGE_OUTWARD;
    boolean dueToLeave = false;
    
    if (shipStage == STATE_LANDED || shipStage == STATE_BOARDING) {
      final float sinceDescent = time - arriveTime;
      final boolean allAboard = PilotUtils.allAboard(trans);
      onArrival(false);
      
      if (sinceDescent > SHIP_VISIT_DURATION) {
        if (shipStage == STATE_LANDED) trans.beginBoarding();
        
        //  TODO:  IN THE CASE OF NON-RETURN TRIPS, YOU SHOULD NOT BE DOING
        //  THIS.  FIX.
        
        if (allAboard && shipStage == STATE_BOARDING) {
          trans.beginTakeoff();
          dueToLeave = true;
          
          if (I.logEvents()) I.say("\n"+trans+" IS TAKING OFF!");
        }
      }
    }
    
    //
    //  If the ship is en-route to an offworld destination, see if it's arrived
    //  there yet.
    if (arriving && ! visitWorld) {
      onArrival(true);
      dueToLeave = true;
      if (I.logEvents()) I.say("\n"+label+" REACHED "+origin);
    }
    
    if (dueToLeave && hasProperty(IS_RECURRING)) beginReturnTrip();
  }
  
  
  protected void onArrival(boolean offWorld) {
    if (hasProperty(IS_RETURN) && tripStage == STAGE_RETURN) {
      tripStage = STAGE_RETURNED;
    }
    else tripStage = STAGE_ARRIVED;
    
    if (offWorld) {
      
      //  TODO:  Ideally, base-simulation should be handling this...
      
      final SectorBase base = verse.baseForSector(origin);
      if (transport != null) {
        refreshCrewAndCargo(true, transport.careers());
      }
      for (Mobile m : migrants) {
        final Purpose a = activityFor(m);
        if (a != null) base.toggleExpat(m, true);
      }
      migrants.clear();
      
      if (returns()) for (Mobile m : base.expats()) {
        final Purpose a = activityFor(m);
        
        //  TODO:  If the journey relies on transport, make sure it allows the
        //  expat aboard (i.e, doesn't belong to a hostile base.)
        
        if (a != null && a.doneOffworld() && a.origin() == destination) {
          base.toggleExpat(m, false);
          migrants.add(m);
        }
      }
    }
  }
  
  
  public void beginReturnTrip() {
    if (tripStage != STAGE_ARRIVED) {
      I.complain("\nCANNOT BEGIN RETURN TRIP BEFORE ARRIVAL!");
      return;
    }
    
    final Sector oldOrigin = origin;
    arriveTime  = verse.world.currentTime();
    arriveTime  += SHIP_JOURNEY_TIME * (0.5f + Rand.num());
    origin      = destination;
    destination = oldOrigin;
    
    if (hasProperty(IS_SINGLE)) tripStage = STAGE_COMPLETE;
    if (hasProperty(IS_RETURN)) {
      if (tripStage == STAGE_RETURNED) tripStage = STAGE_COMPLETE;
      else tripStage = STAGE_RETURN;
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
    
    //
    //  TODO:  Ideally, offworld base-simulation should handle this...
    
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
  
}










/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.craft;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.verse.*;
import stratos.graphics.common.*;
import stratos.graphics.sfx.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;




public abstract class Vehicle extends Mobile implements
  Boarding, Owner, Property, Selectable, Placeable
{
  
  /**  Fields, constants, constructors and save/load methods-
    */
  private static boolean
    verbose = false;
  
  final public static int
    STATE_LANDING  = 0,
    STATE_LANDED   = 1,
    STATE_BOARDING = 2,
    STATE_TAKEOFF  = 3,
    STATE_AWAY     = 4;
  final public static float
    INIT_DIST  = 10.0f,
    INIT_HIGH  = 10.0f,
    TOP_SPEED  =  5.0f,
    NO_LANDING =  -100;
  
  final public Stocks cargo = new Stocks(this);
  final public Structure structure = new Structure(this);
  final public Staff staff = new Staff(this);
  
  private Journey journey;
  final List <Mobile> inside = new List <Mobile> ();
  private Actor pilot;
  private Venue hangar;
  private float pilotBonus;
  
  private Vec3D aimPos = new Vec3D(0, 0, NO_LANDING);
  private float stateInceptTime = 0;
  private int   state = STATE_AWAY;
  
  protected float entranceFace = Venue.FACE_NONE;
  protected Boarding dropPoint;
  
  final TalkFX chat = ActorAssets.TALK_MODEL.makeSprite();
  
  
  public Vehicle() {
    super();
    this.state = STATE_AWAY;
    structure.setState(Structure.STATE_INTACT, 1);
  }
  

  public Vehicle(Session s) throws Exception {
    super(s);
    cargo    .loadState(s);
    structure.loadState(s);
    staff    .loadState(s);
    s.loadObjects(inside);
    
    journey      = (Journey ) s.loadObject();
    pilot        = (Actor   ) s.loadObject();
    hangar       = (Venue   ) s.loadObject();
    dropPoint    = (Boarding) s.loadObject();
    entranceFace = s.loadFloat();
    
    aimPos.loadFrom(s.input());
    stateInceptTime = s.loadFloat();
    state           = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    cargo    .saveState(s);
    structure.saveState(s);
    staff    .saveState(s);
    s.saveObjects(inside);
    
    s.saveObject(journey     );
    s.saveObject(pilot       );
    s.saveObject(hangar      );
    s.saveObject(dropPoint   );
    s.saveFloat (entranceFace);
    
    aimPos.saveTo(s.output());
    s.saveFloat(stateInceptTime);
    s.saveInt  (state          );
  }
  
  
  public Staff staff() { return staff; }
  public Structure structure() { return structure; }
  
  
  
  /**  Pilot and hangar configuration-
    */
  public boolean canPilot(Actor actor) {
    if (pilot != null && actor != null && actor != pilot) {
      return false;
    }
    return true;
  }
  
  
  public boolean setPilot(Actor actor) {
    if (! canPilot(actor)) {
      //I.complain("CANNOT SET AS PILOT");
      return false;
    }
    this.pilot = actor;
    return true;
  }
  
  
  public Actor pilot() {
    return pilot;
  }
  
  
  public void setHangar(Venue hangar) {
    this.hangar = hangar;
    assignBase(hangar.base());
  }
  
  
  public Venue hangar() {
    return hangar;
  }
  
  
  public boolean abandoned() {
    return pilot == null && aboard() != hangar;
  }
  
  
  public Journey journey() {
    return journey;
  }
  
  
  public void assignJourney(Journey j) {
    this.journey = j;
  }
  
  
  
  /**  Dealing with items & inventory-
    */
  public Inventory inventory() {
    return cargo;
  }
  

  public int owningTier() {
    return TIER_PRIVATE;
  }
  
  
  public float priceFor(Traded service, boolean sold) {
    return service.defaultPrice();
  }
  

  public int spaceCapacity() {
    return structure.maxIntegrity();
  }
  
  
  public void afterTransaction(Item item, float amount) {
  }
  
  
  
  /**  Dealing with structural and placement requirements-
    */
  public Index <Upgrade> allUpgrades() {
    return null;
  }
  
  
  public void onCompletion() {
  }
  
  
  public void setAsDestroyed(boolean salvaged) {
    super.setAsDestroyed(salvaged);
  }
  
  
  public void doPlacement(boolean intact) {
    intact |= GameSettings.buildFree || structure.intact();
    final Tile at = origin();
    enterWorldAt(at.x, at.y, at.world, intact);
  }
  
  
  public boolean setupWith(Tile position, Box2D area, Coord... others) {
    return setPosition(position.x, position.y, position.world);
  }
  
  
  public boolean canPlace(Account reasons) {
    return reasons.setSuccess();
  }
  
  
  public void previewPlacement(boolean canPlace, Rendering rendering) {
  }
  
  
  
  /**  Assigning jobs to crew members-
    */
  public void addTasks(Choice choice, Actor forActor, Background background) {
  }
  
  
  public float crowdRating(Actor forActor, Background b) {
    return 1;
  }
  
  
  public int numPositions(Background b) {
    return 0;
  }
  
  
  public Background[] careers() {
    return new Background[0];
  }
  
  
  public boolean isManned() {
    for (Actor a : crew()) if (a.aboard() == this) return true;
    return false;
  }
  
  
  public boolean openFor(Actor actor) {
    return Staff.doesBelong(actor, this);
  }
  
  
  public List <Actor> crew() {
    return staff.workers();
  }
  
  
  public boolean actionDrive(Actor actor, Vehicle driven) {
    return true;
  }
  

  
  
  
  /**  Handling the business of ascent and landing-
    */
  public void assignLandPoint(Vec3D aimPos, Boarding dropPoint) {
    if (aimPos == null) this.aimPos.set(0, 0, NO_LANDING);
    else this.aimPos.setTo(aimPos);
    this.dropPoint = dropPoint;
  }
  
  
  public Vec3D aiming() {
    if (aimPos == null) return null;
    return new Vec3D(aimPos);
  }
  
  
  public Box2D landArea() {
    if (aimPos.z == NO_LANDING) return null;
    final int size = (int) Nums.ceil(radius());
    final Box2D area = new Box2D().set(aimPos.x, aimPos.y, 0, 0);
    area.expandBy(size + 1);
    return area;
  }
  
  
  public Box2D area(Box2D put) {
    if (put == null) put = new Box2D();
    final int size = (int) Nums.ceil(radius());
    return put.set(position.x, position.y, 0, 0).expandBy(size + 1);
  }
  
  
  
  public void beginLanding(Stage world) {
    final Tile entry = Spacing.pickRandomTile(
      world.tileAt(aimPos.x, aimPos.y), INIT_DIST, world
    );
    enterWorldAt(entry.x, entry.y, world, true);
    nextPosition.set(entry.x, entry.y, INIT_HIGH);
    nextRotation = 0;
    setHeading(nextPosition, nextRotation, true, world);
    entranceFace = Venue.FACE_EAST;
    
    stateInceptTime = world.currentTime();
    state           = STATE_LANDING;
    canBoard        = null;
  }
  
  
  public boolean enterWorldAt(int x, int y, Stage world, boolean intact) {
    if (! super.enterWorldAt(x, y, world, intact)) return false;
    if (landed()) completeLanding();
    return true;
  }
  
  
  private void completeLanding() {
    nextPosition.setTo(position.setTo(aimPos));
    dropPoint = PilotUtils.performLanding(this, world, entranceFace);
    
    stateInceptTime = world.currentTime();
    state           = STATE_LANDED;
    canBoard        = null;
    PilotUtils.offloadPassengers(this, true);
  }
  
  
  public void beginBoarding() {
    if (state != STATE_LANDED) I.complain("Cannot board until landed!");
    state = STATE_BOARDING;
  }
  
  
  public void exitWorld() {
    if (landed()) {
      I.say("\n"+this+" EXITING WORLD UNDER ABNORMAL CIRCUMSTANCES");
      I.reportStackTrace();
      beginTakeoff();
    }
    PilotUtils.completeTakeoff(world, this);
    cargo.onWorldExit();
    super.exitWorld();
  }
  
  
  public void beginTakeoff() {
    if (state == STATE_LANDED) PilotUtils.offloadPassengers(this, false);
    
    final Tile exits = Spacing.pickRandomTile(origin(), INIT_DIST, world);
    final Vec3D exitPoint = new Vec3D(exits.x, exits.y, INIT_HIGH);
    PilotUtils.performTakeoff(world, this, exitPoint);
    cargo.clearDemands();
    
    stateInceptTime = world.currentTime();
    state           = STATE_TAKEOFF;
    canBoard        = null;
  }
  
  
  private void completeTakeoff() {
    exitWorld();
    stateInceptTime = world.currentTime();
    state           = STATE_AWAY;
    canBoard        = null;
  }
  
  
  public boolean landed() {
    return state == STATE_LANDED || state == STATE_BOARDING;
  }
  
  
  public boolean boarding() {
    return state == STATE_BOARDING;
  }
  
  
  public int flightState() {
    return state;
  }
  
  
  public Boarding mainEntrance() {
    if (landed()) return dropPoint;
    else return null;
  }
  
  
  
  /**  Handling pathing-
    */
  protected Pathing initPathing() {
    return new Pathing(this);
  }
  
  
  protected void updateAsMobile() {
    super.updateAsMobile();
    
    final boolean report = verbose && (
      I.talkAbout == this || (pilot != null && I.talkAbout == pilot)
    );
    
    if (pilot != null) updatePiloting();
    else pathing.updateTarget(pathing.target());
    final Boarding step = pathing.nextStep();
    if (report) {
      I.say("\nUpdating vehicle (pilot "+pilot+")");
      I.say("  Path target:  "+pathing.target());
      I.say("  Next step is: "+step);
    }
    ///else world.schedule.scheduleNow(this);
    updateVehicleMotion(step);
  }
  
  
  protected void updatePiloting() {
    if (pilot.aboard() != this) {
      pathing.updateTarget(null);
      return;
    }
    final Target focus = pilot.actionFocus();
    if (focus != null) pathing.updateTarget(focus);
  }
  
  
  protected void pathingAbort() {
    if (pilot != null) {
      final Action action = pilot.currentAction();
      if (action != null) action.interrupt(Plan.INTERRUPT_LOSE_PATH);
    }
    pathing.updateTarget(null);
  }
  
  
  protected boolean collides() {
    return false;
  }
  
  
  protected void updateVehicleMotion(Boarding step) {
    //
    //  Check to see if ascent or descent are complete-
    final float height = position.z / INIT_HIGH;
    if (state == STATE_TAKEOFF && height >= 1) {
      completeTakeoff();
    }
    if (state == STATE_LANDING) {
      //
      //  If obstructions appear during the descent, restart the flight-path.
      //  If you touchdown, register as such.
      if (! EntryPoints.checkLandingArea(this, world, journey, landArea())) {
        beginTakeoff();
      }
      else if (height <= 0) {
        completeLanding();
      }
    }
    //
    //  Different forms of motion may be in order.
    final boolean canMove = structure.intact() && step != null;
    final int motion = motionType();
    
    if ((motion == MOTION_HOVER || motion == MOTION_WALKS) && canMove) {
      final float baseSpeed = baseMoveRate();
      float moveRate = baseSpeed;
      if (origin().pathType() == Tile.PATH_ROAD) moveRate *= 1.5f;
      //  TODO:  RESTORE THIS?
      //if (origin().owner() instanceof Causeway) moveRate *= 1.5f;
      moveRate *= (pilotBonus + 1) / 2;
      pathing.headTowards(step, moveRate, 5 * baseSpeed, true);
    }
    
    if (motion == MOTION_FLYER && inWorld() && ! landed()) {
      PilotUtils.adjustFlight(this, aimPos, 0, height, TOP_SPEED);
    }
  }
  
  
  public boolean isMoving() {
    return pathing.nextStep() != null;
  }
  
  
  protected float baseMoveRate() {
    return 1.0f;
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    
    structure.updateStructure(numUpdates);
    if (! structure.intact()) return;
    if (landed() && ! instant) {
      cargo.updateStockDemands(1, services());
      staff.updateStaff(numUpdates);
    }
    if (! pathing.checkPathingOkay()) pathing.refreshFullPath();
    
    //  TODO:  Allow vehicles to act as Mounts.
    
    if (pilot != null && pilot.aboard() == this) {
      pilotBonus = 1;
      final Action a = pilot.currentAction();
      if (! pilot.skills.test(PILOTING, SIMPLE_DC, 0.5f, a)) pilotBonus /= 1.5f;
      if (pilot.skills.test(PILOTING, MODERATE_DC, 0.5f, a)) pilotBonus *= 1.5f;
    }
    else {
      pilotBonus = 0.5f;
      pilot = null;
    }
  }
  
  
  
  
  /**  Handling passengers and cargo-
    */
  private Boarding canBoard[] = null;
  
  
  protected void onTileChange(Tile oldTile, Tile newTile) {
    super.onTileChange(oldTile, newTile);
    canBoard = null;
  }
  
  
  public void setInside(Mobile m, boolean is) {
    if (is) {
      inside.include(m);
    }
    else {
      inside.remove(m);
    }
    canBoard = null;
  }
  
  
  public List <Mobile> inside() {
    return inside;
  }
  
  
  public Boarding[] canBoard() {
    if (canBoard != null) return canBoard;

    if (landed()) {
      final Boarding batch[] = new Boarding[2];
      batch[0] = dropPoint;
      if (aboard() != null) batch[1] = aboard;
      return canBoard = batch;
    }
    else {
      return canBoard = new Boarding[0];
    }
  }
  
  
  public boolean isEntrance(Boarding b) {
    if (b == aboard()) return true;
    return dropPoint == b;
  }
  
  
  public boolean allowsEntry(Accountable m) {
    if (! structure.intact()) return false;
    if (m.base() == this.base()) return true;
    if (Faction.isFactionEnemy(this, m)) return false;
    return true;
  }
  
  
  public int boardableType() {
    return Boarding.BOARDABLE_VEHICLE;
  }
  
  
  public Box2D footprint() {
    return area(null);
  }
  
  
  public Boarding dropPoint() {
    return dropPoint;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public SelectionPane configSelectPane(SelectionPane panel, HUD UI) {
    if (panel == null) panel = new SelectionPane(
      UI, this, portrait(UI), true
    );
    final Description d = panel.detail(), l = panel.listing();
    describeStatus(d, null);
    d.append("\n\n");
    
    final float repair = structure.repairLevel();
    int maxInt = structure.maxIntegrity();
    int condition = (int) (repair * maxInt);
    d.append("  Condition: "+condition+"/"+maxInt);
    
    d.append("\n\n");
    d.append(helpInfo(), Colour.LITE_GREY);
    
    if (crew().size() > 0) l.appendList("\n\nCrew: "      , crew()          );
    if (inside.size() > 0) l.appendList("\n\nPassengers: ", inside          );
    if (! cargo.empty()  ) l.appendList("\n\nCargo: "     , cargo.allItems());
    return panel;
  }
  
  
  public SelectionOptions configSelectOptions(SelectionOptions info, HUD UI) {
    return SelectionOptions.configOptions(this, info, UI);
  }
  
  
  public TalkFX chat() {
    return chat;
  }
  
  
  protected float fogFor(Base base) {
    if (base == this.base()) return (1 + super.fogFor(base)) / 2f;
    return super.fogFor(base);
  }
  
  
  public void renderFor(Rendering rendering, Base base) {
    super.renderFor(rendering, base);
    if (chat.numPhrases() > 0) {
      chat.position.setTo(sprite().position);
      chat.position.z += height();
      chat.readyFor(rendering);
    }
  }
  

  public void renderSelection(Rendering rendering, boolean hovered) {
    if (indoors() || ! inWorld()) return;
    
    final Vec3D viewPos = viewPosition(null);
    viewPos.z = Nums.max(viewPos.z, 0);
    
    Selection.renderSimpleCircle(
      this, viewPos, rendering,
      hovered ? Colour.transparency(0.5f) : Colour.WHITE
    );
  }
  
  
  public String objectCategory() {
    return Target.TYPE_VEHICLE;
  }
  
  
  public void describeStatus(Description d, Object client) {
    
    final int state = flightState();
    if      (state == STATE_LANDING) d.append("Descending to drop point");
    else if (state == STATE_TAKEOFF) d.append("Taking off");
    else if (state == STATE_AWAY   ) d.append("Offworld");
    
    else if (pilot != null && pilot.mind.rootBehaviour() != null) {
      pilot.mind.rootBehaviour().describeBehaviour(d);
    }
    else if (pathing.target() != null) {
      if (pathing.target() == aboard()) d.append("Aboard ");
      else d.append("Heading for ");
      d.append(pathing.target());
    }
    else {
      d.append("Idling");
    }
  }
}







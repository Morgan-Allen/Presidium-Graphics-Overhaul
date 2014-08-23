/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.building;
import stratos.game.actors.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.sfx.TalkFX;
import stratos.user.*;
import stratos.util.*;




public abstract class Vehicle extends Mobile implements
  Boarding, Inventory.Owner, Employer,
  Selectable, Installation
{
  
  
  /**  Fields, constants, constructors and save/load methods-
    */
  protected Base base;
  final public Stocks cargo = new Stocks(this);
  final public Structure structure = new Structure(this);
  final Personnel personnel = new Personnel(this);
  
  final protected List <Mobile> inside = new List <Mobile> ();
  private Actor pilot;
  private Venue hangar;
  private float pilotBonus;
  
  protected float entranceFace = Venue.ENTRANCE_NONE;
  protected Boarding dropPoint;
  
  final TalkFX chat = new TalkFX();
  
  
  public Vehicle() {
    super();
    structure.setState(Structure.STATE_INTACT, 1);
  }

  public Vehicle(Session s) throws Exception {
    super(s);
    cargo.loadState(s);
    structure.loadState(s);
    personnel.loadState(s);
    s.loadObjects(inside);
    dropPoint = (Boarding) s.loadTarget();
    entranceFace = s.loadFloat();
    base = (Base) s.loadObject();
    pilot = (Actor) s.loadObject();
    hangar = (Venue) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    cargo.saveState(s);
    structure.saveState(s);
    personnel.saveState(s);
    s.saveObjects(inside);
    s.saveTarget(dropPoint);
    s.saveFloat(entranceFace);
    s.saveObject(base);
    s.saveObject(pilot);
    s.saveObject(hangar);
  }
  
  
  public void assignBase(Base base) { this.base = base; }
  public Base base() { return base; }
  public Personnel personnel() { return personnel; }
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
  
  
  
  /**  Dealing with items, inventory and structural requirements-
    */
  public Inventory inventory() {
    return cargo;
  }
  
  
  public float priceFor(TradeType service) {
    return service.basePrice;
  }
  
  
  public int spaceFor(TradeType good) {
    return structure.maxIntegrity();//- cargo.spaceUsed();
  }
  
  
  public void afterTransaction(Item item, float amount) {
  }
  
  
  public TalkFX chat() {
    return chat;
  }
  

  public Index <Upgrade> allUpgrades() {
    return null;
  }
  
  
  public void onCompletion() {
  }
  
  
  public void onDestruction() {
  }
  
  
  
  /**  Vehicles are generally commissioned as an accompaniment to venues by
    *  venues themselves, so these methods aren't much used.
    */
  public int buildCost() { return structure.buildCost(); }
  public String buildCategory() { return UIConstants.TYPE_HIDDEN; }

  public boolean pointsOkay(Tile from, Tile to) { return false; }
  public void doPlace(Tile from, Tile to) {}
  public void preview(
    boolean canPlace, Rendering rendering, Tile from, Tile to
  ) {}
  
  
  
  /**  TODO:  Include code here for assessing suitable landing sites?
    */

  /**  Assigning jobs to crew members-
    */
  public Behaviour jobFor(Actor actor) {
    return null;
  }
  
  
  public void addServices(Choice actor, Actor forActor) {}
  public Background[] careers() { return null; }
  
  
  public void setWorker(Actor actor, boolean is) {
    personnel.setWorker(actor, is);
  }

  
  public void setApplicant(Application app, boolean is) {
    //I.complain("NOT IMPLEMENTED YET!");
    personnel.setApplicant(app, is);
  }
  
  
  public int numOpenings(Background b) {
    return 0;
  }
  
  
  public List <Actor> crew() {
    return personnel.workers();
  }
  
  
  public boolean privateProperty() {
    return true;
  }
  
  
  public boolean actionDrive(Actor actor, Vehicle driven) {
    return true;
  }
  
  
  
  /**  Handling pathing-
    */
  protected Pathing initPathing() {
    return new Pathing(this);
  }
  
  
  protected void updateAsMobile() {
    super.updateAsMobile();
    if (pilot != null) updatePiloting();
    else pathing.updateTarget(pathing.target());
    final Boarding step = pathing.nextStep();
    
    if (pathing.checkPathingOkay() && step != null) {
      float moveRate = baseMoveRate();
      if (origin().pathType() == Tile.PATH_ROAD) moveRate *= 1.5f;
      //  TODO:  RESTORE THIS
      //if (origin().owner() instanceof Causeway) moveRate *= 1.5f;
      moveRate *= (pilotBonus + 1) / 2;
      pathing.headTowards(step, moveRate, true);
    }
    else world.schedule.scheduleNow(this);
  }
  
  
  protected float baseMoveRate() {
    return 1.0f;
  }
  
  
  protected void updatePiloting() {
    if (pilot.aboard() != this) {
      pathing.updateTarget(null);
      return;
    }
    if (pilot.currentAction() == null) return;
    pathing.updateTarget(pilot.currentAction().subject());
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates);
    structure.updateStructure(numUpdates);
    cargo.updateStocks(numUpdates, services());
    //
    //  TODO:  Restore this once building/salvage of vehicles is complete.
    ///if (! structure.intact()) return;
    //  TODO:  Create a specialised 'Travel' plan to handle piloting in general!
    /*
    
    if (pilot != null && pilot.aboard() == this) {
      pilotBonus = 1;
      if (! pilot.skills.test(PILOTING, SIMPLE_DC, 0.5f)) pilotBonus /= 1.5f;
      if (pilot.skills.test(PILOTING, MODERATE_DC, 0.5f)) pilotBonus *= 1.5f;
    }
    else {
      pilotBonus = 0.5f;
      pilot = null;
    }
    if (! pathing.checkPathingOkay()) pathing.refreshFullPath();
    if (hangar != null && hangar.destroyed()) {
      //  TODO:  REGISTER FOR SALVAGE
      setAsDestroyed();
    }
    /*/
  }
  
  /*
  public boolean blocksMotion(Boardable b) {
    if (super.blocksMotion(b)) return true;
    if (b instanceof Tile && b != aboard()) {
      final Tile t = (Tile) b;
      if (Spacing.distance(t, origin()) > MobilePathing.MAX_PATH_SCAN) {
        return false; 
      }
      if (t.inside().size() > 0) return true;
    }
    return false;
  }
  //*/
  
  
  
  
  
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
    
    final Boarding batch[] = new Boarding[2];
    batch[0] = dropPoint;
    if (aboard() != null) batch[1] = aboard;
    
    return canBoard = batch;
  }
  
  
  public boolean isEntrance(Boarding b) {
    return dropPoint == b;
  }
  
  
  public boolean allowsEntry(Mobile m) {
    return m.base() == base();
  }
  
  
  public int boardableType() {
    return Boarding.BOARDABLE_VEHICLE;
  }
  
  
  public Box2D area(Box2D put) {
    if (put == null) put = new Box2D();
    final Vec3D p = position;
    final float r = radius();
    put.set(p.x - r, p.y - r, r * 2, r * 2);
    return put;
  }
  
  
  public boolean landed() {
    return true;
  }
  
  
  public Boarding dropPoint() {
    return dropPoint;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public SelectionInfoPane configPanel(SelectionInfoPane panel, BaseUI UI) {
    if (panel == null) panel = new SelectionInfoPane(UI, this, portrait(UI));
    
    final Description d = panel.detail();
    describeStatus(d);
    if (crew().size() > 0) d.appendList("\n\nCrew: ", crew());
    if (inside.size() > 0) d.appendList("\n\nPassengers: ", inside);
    if (! cargo.empty()) d.appendList("\n\nCargo: ", cargo.allItems());
    d.append("\n\n"); d.append(helpInfo(), Colour.LIGHT_GREY);
    return panel;
  }
  
  
  public TargetOptions configInfo(TargetOptions info, BaseUI UI) {
    if (info == null) info = new TargetOptions(UI, this);
    return info;
  }
  
  
  protected float fogFor(Base base) {
    if (base == this.base) return (1 + super.fogFor(base)) / 2f;
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
    Selection.renderPlane(
      rendering, viewPosition(null), radius() + 0.5f,
      hovered ? Colour.transparency(0.5f) : Colour.WHITE,
      Selection.SELECT_CIRCLE
    );
  }
  
  
  public Target selectionLocksOn() {
    return this;
  }
  

  public String toString() {
    return fullName();
  }
  
  
  public void whenTextClicked() {
    BaseUI.current().selection.pushSelection(this, false);
  }
  
  
  public void describeStatus(Description d) {
    if (pilot != null && pilot.mind.rootBehaviour() != null) {
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




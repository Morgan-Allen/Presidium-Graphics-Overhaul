/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.economic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.sfx.TalkFX;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;




public abstract class Vehicle extends Mobile implements
  Boarding, Owner, Property,
  Selectable, Structure.Basis
{
  
  /**  Fields, constants, constructors and save/load methods-
    */
  private static boolean
    verbose = false;
  
  protected Base base;
  final public Stocks cargo = new Stocks(this);
  final public Structure structure = new Structure(this);
  final public Staff staff = new Staff(this);
  
  final protected List <Mobile> inside = new List <Mobile> ();
  private Actor pilot;
  private Venue hangar;
  private float pilotBonus;
  
  protected float entranceFace = Venue.FACING_NONE;
  protected Boarding dropPoint;
  
  final TalkFX chat = new TalkFX();
  
  
  public Vehicle() {
    super();
    structure.setState(Structure.STATE_INTACT, 1);
  }

  public Vehicle(Session s) throws Exception {
    super(s);
    cargo    .loadState(s);
    structure.loadState(s);
    staff    .loadState(s);
    s.loadObjects(inside);
    dropPoint    = (Boarding) s.loadTarget();
    entranceFace = s.loadFloat();
    base         = (Base ) s.loadObject();
    pilot        = (Actor) s.loadObject();
    hangar       = (Venue) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    cargo    .saveState(s);
    structure.saveState(s);
    staff    .saveState(s);
    s.saveObjects(inside);
    s.saveTarget(dropPoint   );
    s.saveFloat (entranceFace);
    s.saveObject(base        );
    s.saveObject(pilot       );
    s.saveObject(hangar      );
  }
  
  
  public void assignBase(Base base) { this.base = base; }
  public Base base() { return base; }
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
  
  
  
  /**  Dealing with items, inventory and structural requirements-
    */
  public Inventory inventory() {
    return cargo;
  }
  

  public int owningTier() {
    return TIER_PRIVATE;
  }
  
  
  public float priceFor(Traded service) {
    return service.basePrice();
  }
  
  
  public int spaceFor(Traded good) {
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
  
  
  public void doPlacement() {
    enterWorld();
  }
  
  
  
  /**  Assigning jobs to crew members-
    */
  public void addTasks(Choice choice, Actor forActor, Background background) {
  }
  
  
  public float crowdRating(Actor forActor, Background b) {
    return 1;
  }
  
  
  public Background[] careers() { return null; }
  
  
  public boolean isManned() {
    for (Actor a : crew()) if (a.aboard() == this) return true;
    return false;
  }
  
  
  public boolean openFor(Actor actor) {
    return staff.doesBelong(actor);
  }
  
  
  public List <Actor> crew() {
    return staff.workers();
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
    if (! structure.intact()) return;
    
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
    
    if (step != null) {
      final float baseSpeed = baseMoveRate();
      float moveRate = baseSpeed;
      if (origin().pathType() == Tile.PATH_ROAD) moveRate *= 1.5f;
      //  TODO:  RESTORE THIS
      //if (origin().owner() instanceof Causeway) moveRate *= 1.5f;
      moveRate *= (pilotBonus + 1) / 2;
      pathing.headTowards(step, moveRate, 5 * baseSpeed, true);
    }
    else world.schedule.scheduleNow(this);
  }
  
  
  protected void updatePiloting() {
    if (pilot.aboard() != this) {
      pathing.updateTarget(null);
      return;
    }
    final Target focus = pilot.actionFocus();
    if (focus != null) pathing.updateTarget(focus);
  }
  
  
  public boolean isMoving() {
    return pathing.nextStep() != null;
  }
  
  
  protected float baseMoveRate() {
    return 1.0f;
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    structure.updateStructure(numUpdates);
    if (! structure.intact()) return;
    cargo.updateOrders();
    if (! pathing.checkPathingOkay()) pathing.refreshFullPath();
    
    //  TODO:  Allow vehicles to act as Mounts.
    
    if (pilot != null && pilot.aboard() == this) {
      pilotBonus = 1;
      if (! pilot.skills.test(PILOTING, SIMPLE_DC, 0.5f)) pilotBonus /= 1.5f;
      if (pilot.skills.test(PILOTING, MODERATE_DC, 0.5f)) pilotBonus *= 1.5f;
    }
    else {
      pilotBonus = 0.5f;
      pilot = null;
    }
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
    if (b == aboard()) return true;
    return dropPoint == b;
  }
  
  
  public boolean allowsEntry(Mobile m) {
    if (! structure.intact()) return false;
    if (m.base() == this.base) return true;
    return base.relations.relationWith(m.base()) > 0;
  }
  
  
  public int boardableType() {
    return Boarding.BOARDABLE_VEHICLE;
  }
  
  
  public Box2D footprint() {
    return area(null);
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
  
  
  public Boarding mainEntrance() {
    return dropPoint;
  }
  
  
  public Boarding dropPoint() {
    return dropPoint;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public SelectionPane configPanel(SelectionPane panel, BaseUI UI) {
    if (panel == null) panel = new SelectionPane(
      UI, this, portrait(UI), true
    );
    final Description d = panel.detail(), l = panel.listing();
    describeStatus(d);
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
    
    final Vec3D viewPos = viewPosition(null);
    viewPos.z = Nums.max(viewPos.z, 0);
    
    Selection.renderSimpleCircle(
      this, viewPos, rendering,
      hovered ? Colour.transparency(0.5f) : Colour.WHITE
    );
  }
  
  
  public Target selectionLocksOn() {
    return this;
  }
  

  public String toString() {
    return fullName();
  }
  
  
  public String objectCategory() {
    return UIConstants.TYPE_HIDDEN;
  }
  
  
  public void whenClicked() {
    BaseUI.current().selection.pushSelection(this);
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




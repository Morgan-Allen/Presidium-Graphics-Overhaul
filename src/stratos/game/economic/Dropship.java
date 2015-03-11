/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.economic;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.plans.*;
import stratos.game.politic.*;
import stratos.graphics.common.*;
import stratos.graphics.sfx.PlaneFX;
import stratos.graphics.solids.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;



/**  Trade ships come to deposit and collect personnel and cargo.
  */
//
//  NOTE:  This class has been prone to bugs where sprite position appears to
//  'jitter' when passing over blocked tiles below, due to the mobile class
//  attempting to 'correct' position after each update.  This class must treat
//  all tiles as passable to compensate.

public class Dropship extends Vehicle implements Inventory.Owner {
  
  
  /**  Fields, constants, constructors and save/load methods-
    */
  private static boolean
    verbose = false;
  
  final static String SHIP_NAMES[] = {
    "The Space Marine",
    "The Solar Wind",
    "The Blue Nebula",
    "The Dejah Thoris",
    "The Leia Organa",
    "The Princess Irulan",
    "The Aeon Falcon",
    "The Business End",
    "The Tranquillity",
    "The Arrow of Orion",
    "The Polaris",
    "The Water Bearer",
    "The Bottle of Klein",
    "The Occam Razor",
    "The Event Horizon",
    "The Lacrimosa",
    "The HMS Magellanic",
    "The Daedalus IV",
    "The Firebrat",
    "The Wing and Prayer",
    "The Prima Noctis",
    "The Nova Dodger",
    "The Apollo",
    "The HMS Halcyon",
    "The Zen Perigee"
  };
  
  final static String
    FILE_DIR = "media/Vehicles/",
    XML_FILE = "VehicleModels.xml";
  final static ModelAsset
    FREIGHTER_MODEL = MS3DModel.loadFrom(
      FILE_DIR, "dropship.ms3d", Dropship.class,
      XML_FILE, "Dropship"
    );
  
  final public static int
    STAGE_DESCENT  = 0,
    STAGE_LANDED   = 1,
    STAGE_BOARDING = 2,
    STAGE_ASCENT   = 3,
    STAGE_AWAY     = 4;
  final public static int
    MAX_CAPACITY   = 100,
    MAX_PASSENGERS = 5,
    MAX_CREW       = 5;
  final public static float
    INIT_DIST  = 10.0f,
    INIT_HIGH  = 10.0f,
    TOP_SPEED  =  5.0f,
    NO_LANDING =  -100;
  
  
  private Vec3D aimPos = new Vec3D(0, 0, NO_LANDING);
  private float stageInceptTime = 0;
  private int   stage = STAGE_AWAY;
  private int   nameID = -1;
  
  
  
  public Dropship() {
    super();
    attachSprite(FREIGHTER_MODEL.makeSprite());
    this.stage = STAGE_AWAY;
    this.nameID = Rand.index(SHIP_NAMES.length);
  }
  
  
  public Dropship(Session s) throws Exception {
    super(s);
    aimPos.loadFrom(s.input());
    stageInceptTime = s.loadFloat();
    stage = s.loadInt();
    nameID = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    aimPos.saveTo(s.output());
    s.saveFloat(stageInceptTime);
    s.saveInt(stage);
    s.saveInt(nameID);
  }
  
  
  //public int owningType() { return Element.VENUE_OWNS; }
  public int pathType() { return Tile.PATH_BLOCKS; }
  public float height() { return 1.5f; }
  public float radius() { return 1.5f; }
  
  
  
  /**  Economic and behavioural functions-
    */
  public void addTasks(Choice choice, Actor actor, Background b) {
    if (b == Backgrounds.AS_RESIDENT || b == Backgrounds.AS_VISITOR) return;
    
    final boolean report = verbose && (
      I.talkAbout == actor || I.talkAbout == this
    );
    if (report) {
      I.say("\nGetting next dropship job for "+actor);
      ///I.reportStackTrace();
    }
    
    if (actor.isDoing(Delivery.class, null)) {
      if (report) I.say("  Is doing delivery!  Won't interrupt...");
      return;
    }
    
    if (stage >= STAGE_BOARDING) {
      final Smuggling boarding = new Smuggling(actor, null, this, new Item[0]);
      if (staff.assignedTo(Delivery.class) == 0) {
        boarding.setMotive(Plan.MOTIVE_EMERGENCY, Plan.PARAMOUNT);
      }
      choice.add(boarding);
      return;
    }
    
    final Choice jobs = new Choice(actor);
    final Traded lacks[] = cargo.shortageTypes();
    final Traded goods[] = cargo.surplusTypes ();
    jobs.isVerbose = report;
    
    final Batch <Venue> depots = DeliveryUtils.nearbyDepots(
      this, world, SERVICE_COMMERCE
    );
    jobs.add(DeliveryUtils.bestBulkDeliveryFrom (this, goods, 2, 10, depots));
    jobs.add(DeliveryUtils.bestBulkCollectionFor(this, lacks, 2, 10, depots));
    if (! jobs.empty()) { choice.add(jobs.pickMostUrgent()); return; }
    
    if (dropPoint instanceof Venue) {
      final Venue hangar = (Venue) dropPoint;
      final Traded t[] = hangar.services();
      jobs.add(DeliveryUtils.bestBulkDeliveryFrom (hangar, t, 1, 10, 2));
      jobs.add(DeliveryUtils.bestBulkCollectionFor(hangar, t, 1, 10, 2));
    }
    else {
      if (report) {
        I.say("  Goods lacked:  "+lacks.length);
        for (Traded t : lacks) I.say("    "+t);
        I.say("  Goods surplus: "+goods.length);
        for (Traded t : goods) I.say("    "+t);
      }
      jobs.add(DeliveryUtils.bestBulkDeliveryFrom (this, goods, 1, 10, 2));
      jobs.add(DeliveryUtils.bestBulkCollectionFor(this, lacks, 1, 10, 2));
    }
    choice.add(jobs.pickMostUrgent());
  }
  
  
  public float crowdRating(Actor actor, Background background) {
    if (background == Backgrounds.AS_VISITOR) {
      float crowding = 0;
      for (Mobile m : inside()) {
        if (m instanceof Actor) {
          if (((Actor) m).mind.work() == this) continue;
        }
        crowding++;
      }
      crowding /= MAX_PASSENGERS;
      return crowding;
    }
    else if (background == Backgrounds.AS_RESIDENT) {
      if (! staff().isWorker(actor)) return 1;
      return staff().residents().size() * 1f / MAX_CREW;
    }
    else return 0;
  }
  
  
  public Traded[] services() { return ALL_MATERIALS; }
  
  
  public int spaceFor(Traded good) {
    return MAX_CAPACITY;
  }
  
  
  public float priceFor(Traded service) {
    //  TODO:  REDUCE PRICES CHARGED IF YOU'RE DOCKED AT AN AIRFIELD WITH
    //  PROPER FUEL STOCKS.
    
    final BaseCommerce c = base.commerce;
    if (c.localSurplus(service) > 0) return c.exportPrice(service);
    if (c.localShortage(service) > 0) return c.importPrice(service);
    return service.basePrice();
  }
  
  
  
  /**  Handling the business of ascent and landing-
    */
  protected void assignLandPoint(Vec3D aimPos, Boarding dropPoint) {
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
  
  
  public void beginDescent(Stage world) {
    final Tile entry = Spacing.pickRandomTile(
      world.tileAt(aimPos.x, aimPos.y), INIT_DIST, world
    );
    enterWorldAt(entry.x, entry.y, world);
    nextPosition.set(entry.x, entry.y, INIT_HIGH);
    nextRotation = 0;
    setHeading(nextPosition, nextRotation, true, world);
    entranceFace = Venue.FACING_EAST;
    stage = STAGE_DESCENT;
    stageInceptTime = world.currentTime();
  }
  
  
  public boolean enterWorldAt(int x, int y, Stage world) {
    if (! super.enterWorldAt(x, y, world)) return false;
    if (landed()) completeDescent();
    return true;
  }
  
  
  private void completeDescent() {
    nextPosition.setTo(position.setTo(aimPos));
    dropPoint = ShipUtils.performLanding(this, world, entranceFace);
    ShipUtils.offloadPassengers(this, true);
    stageInceptTime = world.currentTime();
    stage = STAGE_LANDED;
  }
  
  
  public void beginBoarding() {
    if (stage != STAGE_LANDED) I.complain("Cannot board until landed!");
    stage = STAGE_BOARDING;
  }
  
  
  public void exitWorld() {
    if (landed()) beginAscent();
    super.exitWorld();
  }
  
  
  public void beginAscent() {
    if (stage == STAGE_LANDED) {
      ShipUtils.offloadPassengers(this, false);
    }
    ShipUtils.performTakeoff(world, this);
    stage = STAGE_ASCENT;
    stageInceptTime = world.currentTime();
  }
  
  
  private void completeAscent() {
    world.offworld.beginJourney(this, 5);//Offworld.TRAVEL_DURATION);
    exitWorld();
    stage = STAGE_AWAY;
    stageInceptTime = world.currentTime();
    aimPos.set(0, 0, NO_LANDING);
  }
  
  
  public boolean landed() {
    return stage == STAGE_LANDED || stage == STAGE_BOARDING;
  }
  
  
  public int flightStage() {
    return stage;
  }
  
  
  protected void updateAsMobile() {
    super.updateAsMobile();
    //
    //  Check to see if ascent or descent are complete-
    final float height = position.z / INIT_HIGH;
    if (stage == STAGE_ASCENT && height >= 1) {
      completeAscent();
    }
    if (stage == STAGE_DESCENT) {
      //
      //  If obstructions appear during the descent, restart the flight-path.
      //  If you touchdown, register as such.
      if (! ShipUtils.checkLandingArea(this, world, landArea())) {
        beginAscent();
      }
      else if (height <= 0) {
        completeDescent();
      }
    }
    //
    //  Otherwise, adjust motion-
    if (inWorld() && ! landed()) {
      ShipUtils.adjustFlight(this, aimPos, 0, height);
    }
  }
  

  public Boarding[] canBoard() {
    if (landed()) return super.canBoard();
    else return new Boarding[0];
  }
  
  
  public Boarding mainEntrance() {
    if (landed()) return super.mainEntrance();
    else return null;
  }
  
  
  //  TODO:  Path-finding will need to be more generally addressed here...
  public void pathingAbort() {}
  protected boolean collides() { return false; }
  
  
  
  /**  Rendering and interface methods-
    */
  public void renderFor(Rendering rendering, Base base) {
    final Sprite s = this.sprite();
    final float fadeProgress = fadeWithHeight();
    s.colour = Colour.transparency(fadeProgress);
    
    final float animProgress = Nums.clamp(fadeProgress - 1, 0, 1);
    s.setAnimation("descend", Nums.clamp(animProgress, 0, 1), true);
    super.renderFor(rendering, base);
  }
  
  
  protected float fadeWithHeight() {
    final float height = viewPosition(null).z / INIT_HIGH;
    return (1 - height) * 2;
  }
  

  public void renderSelection(Rendering rendering, boolean hovered) {
    if (indoors() || ! inWorld()) return;
    float alpha = Nums.clamp(fadeWithHeight(), 0, 1);
    alpha *= (hovered ? 0.5f : 1);
    Selection.renderSimpleCircle(
      this, viewPosition(null), rendering, Colour.transparency(alpha)
    );
  }
  
  
  protected PlaneFX createShadow(Sprite rendered) {
    final PlaneFX shadow = super.createShadow(rendered);
    shadow.colour = Colour.transparency(fadeWithHeight());
    return shadow;
  }
  
  
  public String fullName() {
    if (nameID == -1) return "Dropship";
    return SHIP_NAMES[nameID];
  }
  
  
  public Composite portrait(BaseUI UI) {
    return null;
  }
  
  
  public String helpInfo() {
    return
      "Dropships ferry initial colonists and startup supplies to your "+
      "fledgling settlement, courtesy of your homeworld's generosity.";
  }
  
  
  public void describeStatus(Description d) {
    if (stage == STAGE_DESCENT) d.append("Descending to drop point");
    else if (stage == STAGE_ASCENT) d.append("Taking off");
    else if (stage == STAGE_AWAY) d.append("Offworld");
    else super.describeStatus(d);
  }
}








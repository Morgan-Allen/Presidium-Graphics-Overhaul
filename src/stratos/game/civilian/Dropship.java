


package stratos.game.civilian;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.building.*;
import stratos.game.campaign.Commerce;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.solids.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;

//import static stratos.game.actors.Qualities.*;
//import static stratos.game.actors.Backgrounds.*;
import static stratos.game.building.Economy.*;



/**  Trade ships come to deposit and collect personnel and cargo.
  */
//
//NOTE:  This class has been prone to bugs where sprite position appears to
//'jitter' when passing over blocked tiles below, due to the mobile class
//attempting to 'correct' position after each update.  This class must treat
//all tiles as passable to compensate.


//  TODO:  Dropships should have their supply/demand levels calibrated in
//  advance whenever cargo is loaded.



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
    "The Century Kestrel",
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
    "The Terminal Drift",
    "The Halcyon",
    "The Zen Reciprocal"
  };
  
  final static String
    FILE_DIR = "media/Vehicles/",
    XML_FILE = "VehicleModels.xml";
  final static ModelAsset
    FREIGHTER_MODEL = MS3DModel.loadFrom(
      FILE_DIR, "dropship.ms3d", Species.class,
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
    INIT_DIST = 10.0f,
    INIT_HIGH = 10.0f,
    TOP_SPEED =  5.0f;
  
  
  private Vec3D aimPos = new Vec3D();
  private float stageInceptTime = 0;
  private int stage = STAGE_AWAY;
  private int nameID = -1;
  
  
  
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
  
  
  public int owningType() { return Element.VENUE_OWNS; }
  public int pathType() { return Tile.PATH_BLOCKS; }
  public float height() { return 1.5f; }
  public float radius() { return 1.5f; }
  
  
  
  /**  Economic and behavioural functions-
    */
  public float visitCrowding(Actor actor) {
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
  
  
  public float homeCrowding(Actor actor) {
    return personnel().residents().size() * 1f / MAX_CREW;
  }
  
  
  public Traded[] services() { return ALL_MATERIALS; }
  
  
  public Behaviour jobFor(Actor actor) {
    final boolean report = verbose && (
      I.talkAbout == actor || I.talkAbout == this
    );
    if (actor.isDoing(Delivery.class, null)) return null;
    
    if (report) I.say("\nGetting next dropship job for "+actor);
    
    if (stage >= STAGE_BOARDING) {
      final Action boardAction = new Action(
        actor, this,
        this, "actionBoard",
        Action.STAND, "boarding "+this
      );
      boardAction.setPriority(
        stage == STAGE_BOARDING ? Action.PARAMOUNT : 100
      );
      if (report) I.say("Boarding priority: "+boardAction.priorityFor(actor));
      return boardAction;
    }
    
    final Batch <Venue> depots = DeliveryUtils.nearbyDepots(
      this, world, FRSD.class, StockExchange.class
    );
    final Commerce c = this.base.commerce;
    final Choice choice = new Choice(actor);
    
    choice.add(DeliveryUtils.bestExportDelivery(this, depots, 10));
    choice.add(DeliveryUtils.bestImportDelivery(this, depots, 10));
    
    final Traded lacks[] = c.globalShortages();
    choice.add(DeliveryUtils.bestBulkCollectionFor(this, lacks, 1, 10, 2));
    
    final Traded goods[] = c.globalSurpluses();
    choice.add(DeliveryUtils.bestBulkDeliveryFrom (this, goods, 1, 10, 2));
    
    final Plan pick = (Plan) choice.pickMostUrgent();
    
    I.say("Plan picked is: "+pick);
    return pick;
    //return choice.pickMostUrgent();
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates);
    if (stage != STAGE_LANDED) return;
    
    /*
    final int period = (int) scheduledInterval();
    for (Traded good : ALL_MATERIALS) {
      cargo.incDemand(good, 0, Stocks.TIER_TRADER, period, this);
    }
    //*/
    
    //  TODO:  Supply/demand here needs to be based on supply/demand from
    //  trading partners.
    //*
    final Commerce commerce = base.commerce;
    final Tally <Traded> surpluses = new Tally <Traded> ();
    float sumS = 0;
    
    for (Traded good : ALL_MATERIALS) {
      final float surplus = commerce.localSurplus(good);
      if (surplus > 0) {
        sumS += surplus;
        surpluses.add(surplus, good);
      }
      else if (commerce.localShortage(good) > 0) {
        cargo.forceDemand(good, 0, Stocks.TIER_PRODUCER);
      }
      else {
        cargo.forceDemand(good, 0, Stocks.TIER_TRADER);
      }
    }
    
    for (Traded good : surpluses.keys()) {
      final float wanted = MAX_CAPACITY * surpluses.valueFor(good) / sumS;
      cargo.forceDemand(good, wanted, Stocks.TIER_CONSUMER);
    }
    //*/
  }
  
  
  public boolean actionBoard(Actor actor, Dropship ship) {
    ship.setInside(actor, true);
    return true;
  }
  
  
  public void beginBoarding() {
    if (stage != STAGE_LANDED) I.complain("Cannot board until landed!");
    stage = STAGE_BOARDING;
  }
  
  
  public boolean allAboard() {
    for (Actor c : crew()) {
      if (c.aboard() != this) return false;
      if (c.focusFor(null) != this) return false;
    }
    return true;
  }
  
  
  protected void offloadPassengers() {
    final Vec3D p = dropPoint.position(null);
    
    for (Mobile m : inside()) {
      
      if (m instanceof Actor) {
        final Actor a = (Actor) m;
        final boolean belongs =
          personnel().workers().includes(a) ||
          personnel().residents().includes(a);
        
        if (! belongs) {
          m.setPosition(p.x, p.y, world);
          m.goAboard(dropPoint, world);
        }
      }
      
      if (! m.inWorld()) {
        m.setPosition(p.x, p.y, world);
        m.enterWorld();
        m.goAboard(dropPoint, world);
      }
    }
    inside.clear();
  }
  
  
  public int spaceFor(Traded good) {
    return MAX_CAPACITY;
  }
  
  
  public float priceFor(Traded service) {
    final Commerce c = base.commerce;
    if (c.localSurplus(service) > 0) return c.exportPrice(service);
    if (c.localShortage(service) > 0) return c.importPrice(service);
    return service.basePrice();
  }
  


  /**  Handling the business of ascent and landing-
    */
  public void beginDescent(Stage world) {
    final Tile entry = Spacing.pickRandomTile(
      world.tileAt(aimPos.x, aimPos.y), INIT_DIST, world
    );
    enterWorldAt(entry.x, entry.y, world);
    nextPosition.set(entry.x, entry.y, INIT_HIGH);
    nextRotation = 0;
    setHeading(nextPosition, nextRotation, true, world);
    entranceFace = Venue.ENTRANCE_SOUTH;
    stage = STAGE_DESCENT;
    stageInceptTime = world.currentTime();
  }
  
  
  private void performLanding(Stage world, Box2D site) {
    if (dropPoint instanceof Venue) {
    }
    else {
      //  Claim any tiles underneath as owned, and evacuate any occupants-
      site = new Box2D().setTo(site).expandBy(-1);
      for (Tile t : world.tilesIn(site, false)) {
        if (t.onTop() != null) t.onTop().setAsDestroyed();
        t.setOnTop(this);
      }
      for (Tile t : world.tilesIn(site, false)) {
        for (Mobile m : t.inside()) if (m != this) {
          final Tile e = Spacing.nearestOpenTile(m.origin(), m);
          m.setPosition(e.x, e.y, world);
        }
      }
      final int size = 2 * (int) Math.ceil(radius());
      final int EC[] = Spacing.entranceCoords(size, size, entranceFace);
      final Tile o = world.tileAt(site.xpos() + 0.5f, site.ypos() + 0.5f);
      final Tile exit = world.tileAt(o.x + EC[0], o.y + EC[1]);
      
      //  And just make sure the exit is clear-
      if (exit.onTop() != null) exit.onTop().setAsDestroyed();
      this.dropPoint = exit;
    }
    
    //  Offload cargo and passengers-
    offloadPassengers();
  }
  
  
  public void beginAscent() {
    if (stage == STAGE_LANDED) offloadPassengers();
    
    ///I.say("BEGINNING ASCENT");
    //  TODO:  Restore docking at a launch hangar!
    /*
    if (dropPoint instanceof LaunchHangar) {
      ((LaunchHangar) dropPoint).setToDock(null);
    }
    //*/
    //else
    if (landed()) {
      final Box2D site = new Box2D().setTo(landArea()).expandBy(-1);
      for (Tile t : world.tilesIn(site, false)) t.setOnTop(null);
    }
    
    final Tile exits = Spacing.pickRandomTile(origin(), INIT_DIST, world);
    aimPos.set(exits.x, exits.y, INIT_HIGH);
    this.dropPoint = null;
    stage = STAGE_ASCENT;
    stageInceptTime = world.currentTime();
  }
  
  
  public void completeDescent() {
    nextPosition.setTo(position.setTo(aimPos));
    rotation = nextRotation = 0;
    performLanding(world, landArea());
    offloadPassengers();
    stageInceptTime = world.currentTime();
    stage = STAGE_LANDED;
  }
  
  
  public void resetAwayTime() {
    if (stage != STAGE_AWAY) return;
    stageInceptTime = 0 - Commerce.SUPPLY_INTERVAL;
  }
  
  
  public boolean landed() {
    return stage == STAGE_LANDED || stage == STAGE_BOARDING;
  }
  
  
  public float timeLanded() {
    if (stage == STAGE_AWAY || stage == STAGE_DESCENT) return - 1;
    return world.currentTime() - stageInceptTime;
  }
  
  
  public float timeAway(Stage world) {
    return world.currentTime() - stageInceptTime;
  }
  
  
  public int flightStage() {
    return stage;
  }
  
  
  protected void updateAsMobile() {
    super.updateAsMobile();
    //
    //  If obstructions appear during the descent, restart the flight-path-
    if (stage == STAGE_DESCENT) {
      if (! checkLandingArea(world, landArea())) {
        beginAscent();
      }
    }
    //
    //  Check to see if ascent or descent are complete-
    final float height = position.z / INIT_HIGH;
    if (stage == STAGE_ASCENT && height >= 1) {
      for (Mobile m : inside()) m.exitWorld();
      exitWorld();
      stage = STAGE_AWAY;
      stageInceptTime = world.currentTime();
    }
    if (stage == STAGE_DESCENT && height <= 0) {
      performLanding(world, landArea());
      stage = STAGE_LANDED;
      stageInceptTime = world.currentTime();
    }
    //
    //  Otherwise, adjust motion-
    if (inWorld() && ! landed()) adjustFlight(height);
  }
  
  
  private void adjustFlight(float height) {
    //
    //  Firstly, determine what your current position is relative to the aim
    //  point-
    final Vec3D disp = aimPos.sub(position, null);
    final Vec2D heading = new Vec2D().setTo(disp).scale(-1);
    //
    //  Calculate rate of lateral speed and descent-
    final float UPS = 1f / Stage.UPDATES_PER_SECOND;
    final float speed = TOP_SPEED * height * UPS;
    float ascent = TOP_SPEED * UPS / 4;
    ascent = Math.min(ascent, Math.abs(position.z - aimPos.z));
    if (stage == STAGE_DESCENT) ascent *= -1;
    //
    //  Then head toward the aim point-
    if (disp.length() > speed) disp.scale(speed / disp.length());
    disp.z = 0;
    nextPosition.setTo(position).add(disp);
    nextPosition.z = position.z + ascent;
    //
    //  And adjust rotation-
    float angle = (heading.toAngle() * height) + (90 * (1 - height));
    final float
      angleDif = Vec2D.degreeDif(angle, rotation),
      absDif   = Math.abs(angleDif), maxRotate = 90 * UPS;
    if (height < 0.5f && absDif > maxRotate) {
      angle = rotation + (maxRotate * (angleDif > 0 ? 1 : -1));
      angle = (angle + 360) % 360;
    }
    nextRotation = angle;
  }
  

  public Boarding[] canBoard() {
    if (landed()) return super.canBoard();
    else return new Boarding[0];
  }
  
  
  public void pathingAbort() {}
  
  //  TODO:  Path-finding will need to be more generally addressed here...
  protected boolean collides() { return false; }
  //public boolean blockedBy(Boardable t) { return false; }
  
  
  /**  Code for finding a suitable landing site-
    */
  public Box2D landArea() {
    final int size = (int) Math.ceil(radius());
    final Box2D area = new Box2D().set(aimPos.x, aimPos.y, 0, 0);
    area.expandBy(size + 1);
    return area;
  }
  
  
  protected boolean checkLandingArea(Stage world, Box2D area) {
    if (dropPoint instanceof Venue) {
      return dropPoint.inWorld();
    }
    else for (Tile t : world.tilesIn(area, false)) {
      if (t == null) return false;
      if (t.onTop() == this) continue;
      if (PavingMap.pavingReserved(t)) return false;
      if (t.owningType() > Element.ELEMENT_OWNS) return false;
    }
    return true;
  }
  
  
  public boolean findLandingSite(final Base base) {
    this.assignBase(base);
    final Stage world = base.world;
    //LaunchHangar landing = null;
    //float bestRating = Float.NEGATIVE_INFINITY;
    
    //  TODO:  Land at the launch hangar instead.
    /*
    for (Object o : world.presences.matchesNear(FRSD.class, this, -1)) {
      final FRSD depot = (FRSD) o;
      final LaunchHangar strip = depot.launchHangar();
      if (strip == null || ! depot.structure.intact()) continue;
      if (strip.docking() != null || ! strip.structure.intact()) continue;
      float rating = 0; for (Service good : ALL_MATERIALS) {
        rating += depot.exportDemand(good);
        rating += depot.importShortage(good);
      }
      rating /= 2 * ALL_MATERIALS.length;
      if (rating > bestRating) { landing = strip; bestRating = rating; }
    }
    
    if (landing != null) {
      landing.position(aimPos);
      dropPoint = landing;
      landing.setToDock(this);
      I.say("Landing at depot: "+dropPoint);
      return true;
    }
    //*/
    
    final Tile midTile = world.tileAt(world.size / 2, world.size / 2);
    final Presences p = world.presences;
    Target nearest = null;
    
    //  TODO:  There needs to be a more elegant solution here...
    nearest = p.randomMatchNear(FRSD.class, midTile, -1);
    if (findLandingSite(nearest, base)) return true;
    nearest = p.randomMatchNear(base, midTile, -1);
    if (findLandingSite(nearest, base)) return true;
    nearest = p.nearestMatch(base, midTile, -1);
    if (findLandingSite(nearest, base)) return true;
    return false;
  }

  
  private boolean findLandingSite(
    Target from, final Base base
  ) {
    if (from == null) return false;
    final Tile init = Spacing.nearestOpenTile(world.tileAt(from), from);
    if (init == null) return false;
    //
    //  Then, spread out to try and find a decent landing site-
    final Box2D area = landArea();
    final int maxDist = Stage.SECTOR_SIZE * 2;
    final TileSpread spread = new TileSpread(init) {
      protected boolean canAccess(Tile t) {
        if (Spacing.distance(t, init) > maxDist) return false;
        return ! t.blocked();
      }
      protected boolean canPlaceAt(Tile t) {
        area.xpos(t.x - 0.5f);
        area.ypos(t.y - 0.5f);
        return checkLandingArea(base.world, area);
      }
    };
    spread.doSearch();
    if (spread.success()) {
      aimPos.set(
        area.xpos() + (area.xdim() / 2f),
        area.ypos() + (area.ydim() / 2f),
        0
      );
      aimPos.z = base.world.terrain().trueHeight(aimPos.x, aimPos.y);
      dropPoint = null;
      I.say("Landing at point: "+aimPos);
      return true;
    }
    return false;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void renderFor(Rendering rendering, Base base) {
    final Sprite s = this.sprite();
    final float height = this.viewPosition(null).z / INIT_HIGH;
    
    final float fadeProgress = height < 0.5f ? 1 : ((1 - height) * 2);
    s.colour = Colour.transparency(fadeProgress);
    
    final float animProgress = height > 0.5f ? 0 : ((0.5f - height) * 2);
    s.setAnimation("descend", Visit.clamp(animProgress, 0, 1), true);
    
    super.renderFor(rendering, base);
  }
  
  
  /*
  public void renderSelection(Rendering rendering, boolean hovered) {
    if (indoors() || ! inWorld()) return;
    float fadeout = sprite().colour.a;
    Selection.renderPlane(
      rendering, viewPosition(null), radius() + 0.5f,
      Colour.transparency(fadeout * (hovered ? 0.5f : 1)),
      Selection.SELECT_CIRCLE
    );
  }
  //*/
  
  
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








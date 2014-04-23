


package stratos.game.civilian ;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.building.*;
import stratos.game.campaign.Commerce;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.CutoutModel;
import stratos.graphics.solids.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



/**  Trade ships come to deposit and collect personnel and cargo.
  */
//
//NOTE:  This class has been prone to bugs where sprite position appears to
//'jitter' when passing over blocked tiles below, due to the mobile class
//attempting to 'correct' position after each update.  This class must treat
//all tiles as passable to compensate.



public class Dropship extends Vehicle implements
  Inventory.Owner, Economy
{
  
  
  
  /**  Fields, constants, constructors and save/load methods-
    */
  final static String SHIP_NAMES[] = {
    "The Lusty Mariner",
    "The Solar Wind",
    "The Blue Nebula",
    "The Dejah Thoris",
    "The Royal Organa",
    "The Princess Irulan",
    "The Century Hawk",
    "The Business End",
    "The Tranquillity",
    "The Arrow of Orion",
    "The Polaris",
    "The Water Bearer",
    "The Bottle of Klein",
    "The Occam Razor",
    "The Black Horizon",
    "The Lacrimosa",
    "The HMS Magellanic",
    "The Daedalus IV",
    "The Firebrat",
    "The Wing and Prayer",
    "The Prima Noctis",
  } ;
  
  
  final static String
    FILE_DIR = "media/Vehicles/",
    XML_FILE = "VehicleModels.xml" ;
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
    STAGE_AWAY     = 4 ;
  final public static int
    MAX_CAPACITY   = 100,
    MAX_PASSENGERS = 5,
    MAX_CREW       = 5;
  final public static float
    INIT_DIST = 10.0f,
    INIT_HIGH = 10.0f,
    TOP_SPEED =  5.0f ;
  
  
  private Vec3D aimPos = new Vec3D() ;
  private float stageInceptTime = 0 ;
  private int stage = STAGE_AWAY ;
  private int nameID = -1 ;
  
  
  
  public Dropship() {
    super() ;
    attachSprite(FREIGHTER_MODEL.makeSprite()) ;
    this.stage = STAGE_AWAY ;
    this.nameID = Rand.index(SHIP_NAMES.length) ;
  }
  
  
  public Dropship(Session s) throws Exception {
    super(s) ;
    aimPos.loadFrom(s.input()) ;
    stageInceptTime = s.loadFloat() ;
    stage = s.loadInt() ;
    nameID = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    aimPos.saveTo(s.output()) ;
    s.saveFloat(stageInceptTime) ;
    s.saveInt(stage) ;
    s.saveInt(nameID) ;
  }
  
  
  public int owningType() { return Element.VENUE_OWNS ; }
  public int pathType() { return Tile.PATH_BLOCKS ; }
  public float height() { return 1.5f ; }
  public float radius() { return 1.5f ; }
  
  
  
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
  
  
  public Behaviour jobFor(Actor actor) {
    if (actor.isDoing(Delivery.class, null)) return null ;
    
    if (stage >= STAGE_BOARDING) {
      final Action boardAction = new Action(
        actor, this,
        this, "actionBoard",
        Action.STAND, "boarding "+this
      ) ;
      boardAction.setPriority(
        stage == STAGE_BOARDING ? Action.PARAMOUNT : 100
      ) ;
      I.sayAbout(actor, "Boarding priority: "+boardAction.priorityFor(actor)) ;
      return boardAction ;
    }
    
    final Batch <Venue> depots = nearbyDepots() ;
    final Delivery d = Deliveries.nextImportDelivery(
      actor, this, ALL_COMMODITIES, depots, 10, world
    ) ;
    if (d != null) return d ;
    
    final Delivery c = Deliveries.nextExportCollection(
      actor, this, ALL_COMMODITIES, depots, 10, world
    ) ;
    if (c != null) return c ;
    return null ;
  }
  
  
  private Batch <Venue> nearbyDepots() {
    final Batch <Venue> depots = new Batch <Venue> () ;
    for (Object o : world.presences.matchesNear(SupplyDepot.class, this, -1)) {
      if (o == this) continue ;
      depots.add((Venue) o) ;
    }
    return depots ;
  }
  
  
  public boolean actionBoard(Actor actor, Dropship ship) {
    ship.setInside(actor, true) ;
    return true ;
  }
  
  
  public void beginBoarding() {
    if (stage != STAGE_LANDED) I.complain("Cannot board until landed!") ;
    stage = STAGE_BOARDING ;
  }
  
  
  public boolean allAboard() {
    for (Actor c : crew()) {
      if (c.aboard() != this) return false ;
      if (c.focusFor(null) != this) return false ;
    }
    return true ;
  }
  
  
  protected void offloadPassengers() {
    final Vec3D p = dropPoint.position(null) ;
    
    for (Mobile m : inside()) if (! m.inWorld()) {
      m.setPosition(p.x, p.y, world) ;
      m.enterWorld() ;
      m.goAboard(dropPoint, world) ;
    }
    inside.clear() ;
  }
  
  
  public int spaceFor(Service good) {
    return MAX_CAPACITY ;
  }
  
  
  
  /**  Handling the business of ascent and landing-
    */
  public void beginDescent(World world) {
    I.sayAbout(this, "BEGINNING DESCENT") ;
    final Tile entry = Spacing.pickRandomTile(
      world.tileAt(aimPos.x, aimPos.y), INIT_DIST, world
    ) ;
    enterWorldAt(entry.x, entry.y, world) ;
    nextPosition.set(entry.x, entry.y, INIT_HIGH) ;
    nextRotation = 0 ;
    setHeading(nextPosition, nextRotation, true, world) ;
    entranceFace = Venue.ENTRANCE_EAST ;
    stage = STAGE_DESCENT ;
    stageInceptTime = world.currentTime() ;
  }
  
  
  private void performLanding(World world, Box2D site) {
    if (! (dropPoint instanceof Venue)) {
      //
      //  Clear any detritus around the perimeter, Claim tiles in the middle as
      //  owned, and evacuate any occupants-
      for (Tile t : world.tilesIn(site, false)) {
        if (t.owner() != null) t.owner().setAsDestroyed() ;
      }
      site = new Box2D().setTo(site).expandBy(-1) ;
      for (Tile t : world.tilesIn(site, false)) t.setOwner(this) ;
      for (Tile t : world.tilesIn(site, false)) {
        for (Mobile m : t.inside()) if (m != this) {
          final Tile e = Spacing.nearestOpenTile(m.origin(), m) ;
          m.setPosition(e.x, e.y, world) ;
        }
      }
      final int size = 2 * (int) Math.ceil(radius()) ;
      final int EC[] = Spacing.entranceCoords(size, size, entranceFace) ;
      final Tile o = world.tileAt(site.xpos() + 0.5f, site.ypos() + 0.5f) ;
      final Tile exit = world.tileAt(o.x + EC[0], o.y + EC[1]) ;
      this.dropPoint = exit ;
    }
    //
    //  Offload cargo and passengers-
    offloadPassengers() ;
  }
  
  
  public void beginAscent() {
    ///I.say("BEGINNING ASCENT") ;
    if (dropPoint instanceof LandingStrip) {
      ((LandingStrip) dropPoint).setToDock(null) ;
    }
    else if (landed()) {
      final Box2D site = new Box2D().setTo(landArea()).expandBy(-1) ;
      for (Tile t : world.tilesIn(site, false)) t.setOwner(null) ;
    }
    final Tile exits = Spacing.pickRandomTile(origin(), INIT_DIST, world) ;
    aimPos.set(exits.x, exits.y, INIT_HIGH) ;
    this.dropPoint = null ;
    stage = STAGE_ASCENT ;
    stageInceptTime = world.currentTime() ;
  }
  
  
  public void completeDescent() {
    nextPosition.setTo(position.setTo(aimPos)) ;
    rotation = nextRotation = 0 ;
    performLanding(world, landArea()) ;
    offloadPassengers() ;
    stageInceptTime = world.currentTime() ;
    stage = STAGE_LANDED ;
  }
  
  
  public boolean landed() {
    return stage == STAGE_LANDED || stage == STAGE_BOARDING ;
  }
  
  
  public float timeLanded() {
    if (stage == STAGE_AWAY || stage == STAGE_DESCENT) return - 1 ;
    return world.currentTime() - stageInceptTime ;
  }
  
  
  public float timeAway(World world) {
    return world.currentTime() - stageInceptTime ;
  }
  
  
  public int flightStage() {
    return stage ;
  }
  
  
  protected void updateAsMobile() {
    super.updateAsMobile() ;
    final float height = position.z / INIT_HIGH ;
    //
    //  Check to see if ascent or descent are complete-
    if (stage == STAGE_ASCENT && height >= 1) {
      for (Mobile m : inside()) m.exitWorld() ;
      exitWorld() ;
      stage = STAGE_AWAY ;
      stageInceptTime = world.currentTime() ;
    }
    if (stage == STAGE_DESCENT && height <= 0) {
      performLanding(world, landArea()) ;
      stage = STAGE_LANDED ;
      stageInceptTime = world.currentTime() ;
    }
    //
    //  Otherwise, adjust motion-
    if (inWorld() && ! landed()) adjustFlight(height) ;
  }
  
  
  private void adjustFlight(float height) {
    //
    //  Firstly, determine what your current position is relative to the aim
    //  point-
    final Vec3D disp = aimPos.sub(position, null) ;
    final Vec2D heading = new Vec2D().setTo(disp).scale(-1) ;
    //
    //  Calculate rate of lateral speed and descent-
    final float UPS = 1f / World.UPDATES_PER_SECOND ;
    final float speed = TOP_SPEED * height * UPS ;
    float ascent = TOP_SPEED * UPS / 4 ;
    ascent = Math.min(ascent, Math.abs(position.z - aimPos.z)) ;
    if (stage == STAGE_DESCENT) ascent *= -1 ;
    //
    //  Then head toward the aim point-
    if (disp.length() > speed) disp.scale(speed / disp.length()) ;
    disp.z = 0 ;
    nextPosition.setTo(position).add(disp) ;
    nextPosition.z = position.z + ascent ;
    //
    //  And adjust rotation-
    float angle = (heading.toAngle() * height) + (90 * (1 - height)) ;
    final float
      angleDif = Vec2D.degreeDif(angle, rotation),
      absDif   = Math.abs(angleDif), maxRotate = 90 * UPS ;
    if (height < 0.5f && absDif > maxRotate) {
      angle = rotation + (maxRotate * (angleDif > 0 ? 1 : -1)) ;
      angle = (angle + 360) % 360 ;
    }
    nextRotation = angle ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (stage == STAGE_DESCENT) {
      if (! checkLandingArea(world, landArea())) {
        beginAscent() ;
      }
    }
  }
  

  public Boardable[] canBoard(Boardable batch[]) {
    if (landed()) return super.canBoard(batch) ;
    else return new Boardable[0] ;
  }
  
  
  public void pathingAbort() {}
  
  //  TODO:  Path-finding will need to be more generally addressed here...
  protected boolean collides() { return false; }
  //public boolean blockedBy(Boardable t) { return false ; }
  
  
  /**  Code for finding a suitable landing site-
    */
  private Box2D landArea() {
    final int size = (int) Math.ceil(radius()) ;
    final Box2D area = new Box2D().set(aimPos.x, aimPos.y, 0, 0) ;
    area.expandBy(size + 1) ;
    return area ;
  }
  
  
  protected boolean checkLandingArea(World world, Box2D area) {
    if (dropPoint instanceof Venue) {
      return dropPoint.inWorld() ;
    }
    else for (Tile t : world.tilesIn(area, false)) {
      if (t == null) return false ;
      if (t.owner() == this) continue ;
      if (t.owningType() > Element.ELEMENT_OWNS) return false ;
    }
    return true ;
  }
  
  
  public boolean findLandingSite(final Base base) {
    this.assignBase(base) ;
    final World world = base.world ;
    LandingStrip landing = null ;
    float bestRating = Float.NEGATIVE_INFINITY ;
    
    for (Object o : world.presences.matchesNear(SupplyDepot.class, this, -1)) {
      final SupplyDepot depot = (SupplyDepot) o ;
      final LandingStrip strip = depot.landingStrip() ;
      if (strip == null || ! depot.structure.intact()) continue ;
      if (strip.docking() != null || ! strip.structure.intact()) continue ;
      float rating = 0 ; for (Service good : ALL_COMMODITIES) {
        rating += depot.exportDemand(good) ;
        rating += depot.importShortage(good) ;
      }
      rating /= 2 * ALL_COMMODITIES.length ;
      if (rating > bestRating) { landing = strip ; bestRating = rating ; }
    }
    
    if (landing != null) {
      landing.position(aimPos) ;
      dropPoint = landing ;
      landing.setToDock(this) ;
      I.say("Landing at depot: "+dropPoint) ;
      return true ;
    }
    
    final Tile midTile = world.tileAt(world.size / 2, world.size / 2) ;
    final Target nearest = world.presences.randomMatchNear(base, midTile, -1) ;
    if (nearest == null) return false ;
    
    final Tile init = Spacing.nearestOpenTile(world.tileAt(nearest), midTile) ;
    if (init == null) return false ;
    return findLandingSite(init, base) ;
  }

  
  private boolean findLandingSite(
    final Tile init, final Base base
  ) {
    //
    //  Then, spread out to try and find a decent landing site-
    final Box2D area = landArea() ;
    final int maxDist = World.SECTOR_SIZE * 2 ;
    final TileSpread spread = new TileSpread(init) {
      protected boolean canAccess(Tile t) {
        if (Spacing.distance(t, init) > maxDist) return false ;
        return ! t.blocked() ;
      }
      protected boolean canPlaceAt(Tile t) {
        area.xpos(t.x - 0.5f) ;
        area.ypos(t.y - 0.5f) ;
        return checkLandingArea(base.world, area) ;
      }
    } ;
    spread.doSearch() ;
    if (spread.success()) {
      aimPos.set(
        area.xpos() + (area.xdim() / 2f),
        area.ypos() + (area.ydim() / 2f),
        0
      ) ;
      aimPos.z = base.world.terrain().trueHeight(aimPos.x, aimPos.y) ;
      dropPoint = null ;
      I.say("Landing at point: "+aimPos) ;
      return true ;
    }
    return false ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void renderFor(Rendering rendering, Base base) {
    final Sprite s = this.sprite() ;
    final float height = this.viewPosition(null).z / INIT_HIGH ;
    
    final float fadeProgress = height < 0.5f ? 1 : ((1 - height) * 2) ;
    s.colour = Colour.transparency(fadeProgress) ;
    
    final float animProgress = height > 0.5f ? 0 : ((0.5f - height) * 2) ;
    s.setAnimation("descend", Visit.clamp(animProgress, 0, 1), true) ;
    
    super.renderFor(rendering, base) ;
  }
  

  public void renderSelection(Rendering rendering, boolean hovered) {
    if (indoors() || ! inWorld()) return ;
    float fadeout = sprite().colour.a ;
    Selection.renderPlane(
      rendering, viewPosition(null), radius() + 0.5f,
      Colour.transparency(fadeout * (hovered ? 0.5f : 1)),
      Selection.SELECT_CIRCLE
    ) ;
  }
  
  
  public String fullName() {
    if (nameID == -1) return "Dropship" ;
    return SHIP_NAMES[nameID] ;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return null ;
  }
  
  
  public String helpInfo() {
    return
      "Dropships ferry initial colonists and startup supplies to your "+
      "fledgling settlement, courtesy of your homeworld's generosity." ;
  }
  
  
  public void describeStatus(Description d) {
    if (stage == STAGE_DESCENT) d.append("Descending to drop point") ;
    else if (stage == STAGE_ASCENT) d.append("Taking off") ;
    else if (stage == STAGE_AWAY) d.append("Offworld") ;
    else super.describeStatus(d) ;
  }
}








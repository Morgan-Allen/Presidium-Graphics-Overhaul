/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.economic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.sfx.*;
import stratos.graphics.terrain.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.base.HoldingUpgrades.OCCUPANCIES;
import static stratos.game.economic.Economy.*;



public abstract class Venue extends Structural implements
  Boarding, Inventory.Owner, Property
{
  
  
  /**  Field definitions, constants, constructors, and save/load methods.
    */
  final public static int
    ENTRANCE_NONE  = -1,
    ENTRANCE_NORTH =  N / 2,
    ENTRANCE_EAST  =  E / 2,
    ENTRANCE_SOUTH =  S / 2,
    ENTRANCE_WEST  =  W / 2,
    NUM_SIDES      =  4;
  
  final public static int
    
    PRIMARY_SHIFT      = 1,
    SECONDARY_SHIFT    = 2,
    OFF_DUTY           = 3,
    
    SHIFTS_ALWAYS      = 0,
    SHIFTS_BY_HOURS    = 1,   //different 8-hour periods off.
    SHIFTS_BY_DAY      = 2,   //every second or third day off.
    SHIFTS_BY_24_HOUR  = 3,   //on for an entire day at a time.
    SHIFTS_BY_CALENDAR = 4;   //weekends and holidays off.  NOT DONE YET
  
  
  final public VenueProfile profile;
  final public Staff staff = new Staff(this);
  final public Stocks stocks = new Stocks(this);
  
  protected int entranceFace;
  protected Tile entrance;
  private List <Mobile> inside = new List <Mobile> ();
  
  
  final public TalkFX chat = new TalkFX();
  
  
  
  public Venue(int size, int high, int entranceFace, Base base) {
    super(size, high, base);
    //this.profile = profile;
    this.base = base;
    this.entranceFace = entranceFace;
    
    //  TODO:  Create and cache the profile here, using pre-existing data.
    //         ...Also, consider creating a dedicated constructor for this
    //         purpose?
    //  TODO:  Add more properties once you get the chance...
    final Class PK = this.getClass();
    VenueProfile profile = VenueProfile.profileFor(PK);
    if (profile == null) profile = new VenueProfile(PK, PK);
    this.profile = profile;
  }
  
  
  public Venue(Session s) throws Exception {
    super(s);
    
    profile = (VenueProfile) s.loadObject();
    staff.loadState(s);
    stocks.loadState(s);
    
    entranceFace = s.loadInt();
    entrance = (Tile) s.loadTarget();
    s.loadObjects(inside);
    
    buildSprite = (BuildingSprite) sprite();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    
    s.saveObject(profile);
    staff.saveState(s);
    stocks.saveState(s);
    
    s.saveInt(entranceFace);
    s.saveTarget(entrance);
    s.saveObjects(inside);
  }
  
  
  public Index <Upgrade> allUpgrades() { return null; }
  public Structure structure() { return structure; }
  public Staff staff() { return staff; }
  public Base base() { return base; }
  
  
  public void assignBase(Base base) {
    if (! inWorld()) { this.base = base; return; }
    world.presences.togglePresence(this, false);
    this.base = base;
    world.presences.togglePresence(this, true);
  }
  
  
  
  /**  Dealing with items and inventory-
    */
  public Inventory inventory() {
    return stocks;
  }
  
  
  public float priceFor(Traded service) {
    return stocks.priceFor(service);
  }
  
  
  public int spaceFor(Traded good) {
    return structure.maxIntegrity();
  }
  
  
  public void afterTransaction(Item item, float amount) {
  }
  
  
  public TalkFX chat() {
    return chat;
  }
  
  
  
  /**  Structure.Basis and positioning-
    */
  public boolean canPlace() {
    if (origin() == null) return false;
    final Stage world = origin().world;
    //
    //  Make sure we don't displace any more important object, or occupy their
    //  entrances.  In addition, the entrance must be clear.
    for (Tile t : world.tilesIn(footprint(), false)) {
      if (t == null || t.reserved()) return false;
    }
    for (Venue c : world.claims.venuesConflicting(areaClaimed(), this)) {
      if (c.owningTier() >= this.owningTier()) return false;
    }
    if (! checkPerimeter(world)) return false;
    final Tile e = mainEntrance();
    if (e != null && e.reserved()) return false;
    return true;
  }
  
  
  //  TODO:  You'll need to allow a 2x2 tile clearance for structures in a
  //  different patch (to allow for wider roads.)  Move these out to utility
  //  methods in the placement class.
  
  protected boolean checkPerimeter(Stage world) {
    //
    //  Don't abut on anything of higher priority-
    /*
    for (Tile n : Spacing.perimeter(footprint(), world)) {
      if (n == null || ! n.habitat().pathClear) return false;
      final Element top = n.onTop();
      //if (top != null && ! canTouch(top)) return false;
    }
    //*/
    //
    //  And make sure we don't create isolated areas of unreachable tiles-
    if (! Spacing.perimeterFits(this)) return false;
    return true;
  }
  
  
  public boolean setPosition(float x, float y, Stage world) {
    if (! super.setPosition(x, y, world)) return false;
    final Tile o = origin();
    if (entranceFace == ENTRANCE_NONE) {
      entrance = null;
    }
    else {
      final int off[] = Spacing.entranceCoords(size, size, entranceFace);
      entrance = world.tileAt(o.x + off[0], o.y + off[1]);
    }
    return true;
  }
  
  
  //  TODO:  Reserve the salvage-orders strictly for the doPlacement method?
  
  public boolean enterWorldAt(int x, int y, Stage world) {
    if (! super.enterWorldAt(x, y, world)) return false;
    if (base == null) I.complain("VENUES MUST HAVE A BASED ASSIGNED! "+this);
    
    for (Venue c : world.claims.venuesConflicting(areaClaimed(), this)) {
      c.structure.beginSalvage();
    }
    
    world.presences.togglePresence(this, true);
    world.claims.assertNewClaim(this, areaClaimed());
    stocks.onWorldEntry();
    staff.onCommission();
    impingeSupply(true);
    return true;
  }
  
  
  public void exitWorld() {
    stocks.onWorldExit();
    staff.onDecommission();
    world.presences.togglePresence(this, false);
    world.claims.removeClaim(this);
    super.exitWorld();
  }
  
  
  public float scheduledInterval() {
    return 1;
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    if (destroyed()) {
      I.say(this+" IS DESTROYED! SHOULD NOT BE ON SCHEDULE!");
      this.setAsDestroyed();
    }
    structure.updateStructure(numUpdates);
    
    if (instant) return;
    final boolean rare = numUpdates % 10 == 0;
    
    if (! structure.needsSalvage()) {
      if (rare) updatePaving(true);
      staff.updateStaff(numUpdates);
      impingeSupply(false);
    }
    if (structure.intact()) {
      final int needHome = staff.workforce();
      base.demands.impingeDemand(SERVICE_HOUSING, needHome, 1, this);
      stocks.updateOrders();
      if (rare) stocks.updateDemands(10);
    }
  }
  
  
  protected void updatePaving(boolean inWorld) {
    super.updatePaving(inWorld);
    base.transport.updateJunction(this, mainEntrance(), inWorld);
  }
  
  
  protected Box2D areaClaimed() {
    return footprint();
  }
  
  
  public boolean preventsClaimBy(Venue other) {
    
    return other.owningTier() <= this.owningTier();
  }
  
  
  
  /**  Implementing the Boardable interface-
    */
  private Boarding canBoard[] = null;
  
  
  public void setInside(Mobile m, boolean is) {
    if (is) {
      inside.include(m);
    }
    else {
      inside.remove(m);
    }
    if (m instanceof Boarding) canBoard = null;
  }
  
  
  public List <Mobile> inside() {
    return inside;
  }
  
  
  public Tile mainEntrance() {
    return entrance;
  }
  
  
  public Box2D area(Box2D put) {
    if (put == null) put = new Box2D();
    return put.setTo(this.footprint());
  }
  
  
  public Boarding[] canBoard() {
    if (canBoard != null) return canBoard;
    
    final int minSize = 1 + inside.size();
    final Boarding batch[] = new Boarding[minSize];
    batch[0] = entrance;
    int i = 1; for (Mobile m : inside) if (m instanceof Boarding) {
      batch[i++] = (Boarding) m;
    }
    
    return canBoard = batch;
  }
  
  
  public boolean isEntrance(Boarding t) {
    return entrance == t;
  }
  
  
  public boolean allowsEntry(Mobile m) {
    if (! structure.intact()) return false;
    if (m.base() == this.base) return true;
    return base.relations.relationWith(m.base()) > 0;
  }
  
  
  public int boardableType() {
    return Boarding.BOARDABLE_VENUE;
  }
  
  
  
  /**  Recruiting staff and assigning manufacturing tasks-
    */
  public boolean isManned() {
    for (Actor a : staff.workers) {
      if (a.health.conscious() && a.aboard() == this) return true;
    }
    return false;
  }
  
  
  public float crowdRating(Actor forActor, Background background) {
    if (background == Backgrounds.AS_RESIDENT) {
      return 1;
    }
    else if (background == Backgrounds.AS_VISITOR) {
      float crowding = 0;
      for (Mobile m : inside()) if (! staff.doesBelong(m)) crowding++;
      crowding /= ((size * 2) + 1);
      return crowding;
    }
    else {
      final int openings = numOpenings(background);
      if (openings <= 0) return 1;
      final int hired = staff.numHired(background);
      return hired * 1f / openings;
    }
  }
  
  
  protected int numOpenings(Background b) {
    return structure.upgradeBonus(b);
  }
  
  
  public void addTasks(Choice choice, Actor actor, Background background) {
    if (! structure.intact()) return;
    else if (background == Backgrounds.AS_RESIDENT) {
      return;
    }
    else if (background == Backgrounds.AS_VISITOR) {
      addServices(choice, actor);
    }
    else {
      final boolean onShift = staff.onShift(actor);
      choice.add(jobFor(actor, onShift));
    }
  }
  
  
  protected void addServices(Choice choice, Actor forActor) {}
  protected Behaviour jobFor(Actor actor, boolean onShift) { return null; }
  
  
  
  //  TODO:  Make these abstract?
  public float ratePlacing(Target point, boolean exact) {
    return 1;
  }
  
  
  protected void impingeSupply(boolean onEntry) {
    final int period = onEntry ? -1 : 1;
    base.demands.impingeSupply(getClass(), 1, period, this);
  }
  
  
  public int owningTier() {
    return TIER_PUBLIC;
  }
  
  
  
  /**  Interface methods-
    */
  public SelectionInfoPane configPanel(SelectionInfoPane panel, BaseUI UI) {
    return VenueDescription.configStandardPanel(this, panel, UI, false);
  }
  
  
  protected boolean showLights() {
    return isManned();
  }
  
  
  protected void toggleStatusFor(Traded need, ModelAsset model) {
    if (! structure.intact()) buildSprite.toggleFX(need.model, false);
    buildSprite.toggleFX(model, stocks.shortagePenalty(need) > 0);
  }
  
  
  protected void toggleStatusDisplay() {
    final boolean showBurn = structure.burning();
    buildSprite.toggleFX(BuildingSprite.BLAST_MODEL, showBurn);
    toggleStatusFor(ATMO, BuildingSprite.LIFE_SUPPORT_MODEL);
    toggleStatusFor(POWER       , BuildingSprite.POWER_MODEL);
    toggleStatusFor(WATER  , BuildingSprite.WATER_MODEL);
  }
  
  
  protected void renderChat(Rendering rendering, Base base) {
    if (! structure.intact()) return;
    if (chat.numPhrases() > 0) {
      chat.position.setTo(sprite().position);
      chat.position.z += height();
      chat.readyFor(rendering);
    }
  }
  
  
  private boolean canShow(Traded type) {
    if (type.form == FORM_PROVISION) return false;
    if (type.picPath == Traded.DEFAULT_PIC_PATH) return false;
    return true;
  }

  
  final protected static float STANDARD_GOOD_SPRITE_OFFSETS[] = {
    0, 0,
    1, 0,
    0, 1,
    2, 0,
    0, 2,
    3, 0,
    0, 3
  };
  
  
  protected float[] goodDisplayOffsets() {
    return STANDARD_GOOD_SPRITE_OFFSETS;
  }
  
  
  protected Traded[] goodsToShow() {
    return services();
  }
  
  
  protected float goodDisplayAmount(Traded good) {
    if (! structure.intact()) return 0;
    return stocks.amountOf(good);
  }
  
  
  protected void updateItemSprites() {
    final Traded services[] = goodsToShow();
    final float offsets[] = goodDisplayOffsets();
    if (services == null) return;
    
    final boolean hide = ! structure.intact();
    final float
      initY = (size / 2f) - BuildingSprite.ITEM_SIZE,
      initX = BuildingSprite.ITEM_SIZE - (size / 2f);
    
    int index = -1;
    for (Traded s : services) if (canShow(s)) index += 2;
    if (index < 0) return;
    index = Nums.clamp(index, offsets.length);
    
    for (int SI = services.length; SI-- > 0;) {
      final Traded s = services[SI];
      if (! canShow(s)) continue;
      if (index < 0) break;
      final float y = offsets[index--], x = offsets[index--];
      if (y >= size || size <= -x) continue;
      buildSprite.updateItemDisplay(
        s.model,
        hide ? 0 : goodDisplayAmount(s),
        initX + x,
        initY - y
      );
    }
  }
  
  
  public void renderFor(Rendering rendering, Base base) {
    super.renderFor(rendering, base);
    toggleStatusDisplay();
    updateItemSprites();
    renderChat(rendering, base);
  }
}





/*
protected int numOpenings(Background v) {
  return structure.upgradeBonus(v) - staff.numHired(v);
}
//*/
/*
public float visitCrowding(Actor actor) {
  float crowding = 0;
  for (Mobile m : inside()) {
    if (m instanceof Actor) {
      if (((Actor) m).mind.work() == this) continue;
    }
    crowding++;
  }
  crowding /= ((size * 2) + 1);
  return crowding;
}


public float homeCrowding(Actor actor) {
  return 1;
}
//*/


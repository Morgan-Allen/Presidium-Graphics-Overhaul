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
import stratos.user.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;



public abstract class Venue extends Structural implements
  Boarding, Owner, Property, Placeable
{
  
  /**  Field definitions, constants, constructors, and save/load methods.
    */
  final public static int
    PRIMARY_SHIFT      = 1,
    SECONDARY_SHIFT    = 2,
    OFF_DUTY           = 0,
    
    SHIFTS_ALWAYS      = 0,
    SHIFTS_BY_HOURS    = 1,   //different 8-hour periods off.
    SHIFTS_BY_DAY      = 2,   //every second or third day off.
    SHIFTS_BY_24_HOUR  = 3,   //on for an entire day at a time.
    SHIFTS_BY_CALENDAR = 4;   //weekends and holidays off.  NOT DONE YET
  
  final public static int
    //  These are OR'd together in the VenueProfile to state properties-
    IS_NORMAL  = 0 ,
    IS_FIXTURE = 1 ,
    IS_LINEAR  = 2 ,
    IS_ZONED   = 4 ,
    IS_GRIDDED = 8 ,
    IS_UNIQUE  = 16,
    IS_WILD    = 32;
  
  final public static Blueprint NO_REQUIREMENTS[] = new Blueprint[0];
  
  
  final public Blueprint blueprint;
  final public Staff staff = new Staff(this);
  final public Stocks stocks = new Stocks(this);
  
  protected Tile entrance;
  private List <Mobile> inside = new List <Mobile> ();
  
  final public TalkFX chat = new TalkFX();
  private int nameID = -2;
  
  
  
  protected Venue(Blueprint blueprint, Base base) {
    super(blueprint.size, blueprint.high, base);
    this.base      = base     ;
    this.blueprint = blueprint;
  }
  
  
  public Venue(Session s) throws Exception {
    super(s);
    
    blueprint = (Blueprint) s.loadObject();
    staff .loadState(s);
    stocks.loadState(s);
    
    entrance = (Tile) s.loadTarget();
    s.loadObjects(inside);
    
    buildSprite = (BuildingSprite) sprite();
    nameID = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    
    s.saveObject(blueprint);
    staff .saveState(s);
    stocks.saveState(s);
    
    s.saveTarget(entrance);
    s.saveObjects(inside);
    
    s.saveInt(nameID);
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
  
  
  public float priceFor(Traded good, boolean sold) {
    return good.basePrice();
  }
  
  
  public int spaceFor(Traded good) {
    return structure.maxIntegrity();
  }
  
  
  public void afterTransaction(Item item, float amount) {
  }
  
  
  
  /**  Structure.Basis and positioning-
    */
  public boolean setupWith(Tile position, Box2D area, Coord... others) {
    if (position == null) return false;
    return setPosition(position.x, position.y, position.world);
  }
  
  
  public boolean setPosition(float x, float y, Stage world) {
    if (! super.setPosition(x, y, world)) return false;
    final Tile o = origin();
    if (blueprint.isFixture()) {
      //
      //  Fixture-venues don't normally have entrances, but we make an
      //  exception for tiling-venues.
      entrance = (pathType() <= Tile.PATH_CLEAR) ? origin() : null;
    }
    else {
      //
      //  If your current facing is viable, stick with that.
      if (facing == FACING_INIT) facing = FACING_EAST;
      final int off[] = Placement.entranceCoords(size, size, facing);
      entrance = world.tileAt(o.x + off[0], o.y + off[1]);
      if (! Placement.isViableEntrance(this, entrance)) entrance = null;
    }
    if (! entranceOkay()) return false;
    return true;
  }
  
  
  public boolean canPlace(Account reasons) {
    if (origin() == null) return reasons.setFailure("Over the edge!");
    final Stage world = origin().world;
    final boolean solid = pathType() >= Tile.PATH_HINDERS;
    //
    //  Make sure we don't displace any more important object, or occupy their
    //  entrances.  In addition, the entrance must be clear.
    if (! entranceOkay()) {
      return reasons.setFailure("No room for entrance");
    }
    for (Tile t : world.tilesIn(footprint(), false)) {
      if (t == null) return reasons.setFailure("Over the edge!");
      if (t.reserved()) {
        if (reasons == Account.NONE) return false;
        return reasons.setFailure("Area reserved by "+t.onTop());
      }
      if (! canBuildOn(t)) {
        if (reasons == Account.NONE) return false;
        return reasons.setFailure(t.habitat()+" is not buildable");
      }
      if (t.isEntrance() && solid) {
        if (reasons == Account.NONE) return false;
        return reasons.setFailure("Is entrance for "+t.entranceFor());
      }
    }
    for (Venue c : world.claims.venuesConflicting(areaClaimed(), this)) {
      if (reasons == Account.NONE) return false;
      return reasons.setFailure("Too close to "+c);
    }
    if (solid && ! checkPerimeter(world)) {
      return reasons.setFailure("Might obstruct pathing");
    }
    return reasons.setSuccess();
  }
  
  
  public boolean canPlace() {
    return canPlace(Account.NONE);
  }
  
  
  protected boolean entranceOkay() {
    if (blueprint.isFixture()) return true;
    if (entrance == null || ! Placement.isViableEntrance(this, entrance)) {
      return false;
    }
    return true;
  }
  
  
  protected boolean canBuildOn(Tile t) {
    return t.habitat().pathClear;
  }
  
  
  protected boolean checkPerimeter(Stage world) {
    return Placement.perimeterFits(this, world);
  }
  
  
  public boolean enterWorldAt(int x, int y, Stage world) {
    if (! super.enterWorldAt(x, y, world)) return false;
    if (base == null) I.complain("VENUES MUST HAVE A BASE ASSIGNED! "+this);
    
    //  TODO:  Extend the above to non-venue fixtures as well (instead of the
    //  procedure below.)
    for (Tile t : Spacing.perimeter(footprint(), world)) if (t != null) {
      final Element fringes = t.onTop();
      if (fringes == null || fringes.owningTier() >= TIER_PRIVATE) continue;
      else fringes.setAsDestroyed();
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
      stocks.updateOrders();
      if (rare) {
        stocks.updateDemands(10);
        int needHome = 0;
        for (Actor a : staff.workers()) if (a.mind.home() != this) needHome++;
        base.demands.impingeDemand(SERVICE_HOUSING, needHome, 10, this);
      }
    }
  }
  
  
  protected void updatePaving(boolean inWorld) {
    super.updatePaving(inWorld);
    if (pathType() <= Tile.PATH_CLEAR) {
      byte road = inWorld ? StageTerrain.ROAD_LIGHT : StageTerrain.ROAD_NONE;
      for (Tile t : world.tilesIn(footprint(), false)) {
        world.terrain().setRoadType(t, road);
      }
    }
    else {
      base.transport.updateJunction(this, mainEntrance(), inWorld);
    }
  }
  
  
  public Box2D areaClaimed() {
    return footprint();
  }
  
  
  public boolean preventsClaimBy(Venue other) {
    return true;
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
    //  TODO:  WORK THIS OUT
    return false;
    //return base.relations.relationWith(m.base()) >= 0;
  }
  
  
  public int boardableType() {
    return Boarding.BOARDABLE_VENUE;
  }
  
  
  
  /**  Recruiting staff and assigning manufacturing tasks-
    */
  public boolean openFor(Actor actor) {
    if (! structure.intact()) return false;
    if (actor != null && Staff.doesBelong(actor, this)) return true;
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
      for (Mobile m : inside()) if (! Staff.doesBelong(m, this)) crowding++;
      crowding /= ((size * 2) + 1);
      return crowding;
    }
    else {
      final int openings = numOpenings(background);
      if (openings <= 0) return 1;
      final int hired = staff.numHired(background);
      return hired * 1f / (hired + openings);
    }
  }
  
  
  protected int numOpenings(Background b) {
    return structure.upgradeBonus(b) - staff.numHired(b);
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

  //  TODO:  Make these abstract?
  //
  //  By default, these do nothing.
  protected void addServices(Choice choice, Actor client) {}
  protected Behaviour jobFor(Actor actor, boolean onShift) { return null; }
  
  
  public float ratePlacing(Target point, boolean exact) {
    return 0;
  }
  
  
  protected void impingeSupply(boolean onEntry) {
    final int period = onEntry ? -1 : 1;
    base.demands.impingeSupply(getClass(), 1, period, this);
  }
  
  
  public int owningTier() {
    return blueprint.owningTier;
  }
  
  
  
  /**  Interface methods-
    */
  public SelectionPane configSelectPane(SelectionPane panel, BaseUI UI) {
    return VenuePane.configStandardPanel(this, panel, UI, null);
  }
  
  
  public String fullName() {
    if (blueprint.isFixture()) return blueprint.name;
    if (blueprint.isUnique ()) return "The "+blueprint.name;
    
    if (nameID == -2 && inWorld()) {
      nameID = base.nextVenueID(getClass());
    }
    if (nameID < 0) return blueprint.name;
    String suffix = ""+nameID;
    return blueprint.name+" "+suffix;
  }
  
  
  public String objectCategory() {
    return blueprint.category;
  }
  
  
  protected boolean showLights() {
    if (blueprint.isFixture()) return true;
    return staff.visitors().size() > 0;
  }
  
  
  protected void toggleStatusFor(Traded need, ModelAsset model) {
    if (! structure.intact()) buildSprite.toggleFX(need.model, false);
    final boolean needs = stocks.relativeShortage(need) > 0.5f;
    buildSprite.toggleFX(model, needs);
  }
  
  
  protected void toggleStatusDisplay() {
    final boolean showBurn = structure.burning();
    buildSprite.toggleFX(BuildingSprite.BLAST_MODEL, showBurn);
    toggleStatusFor(ATMO , BuildingSprite.LIFE_SUPPORT_MODEL);
    toggleStatusFor(POWER, BuildingSprite.POWER_MODEL);
    toggleStatusFor(WATER, BuildingSprite.WATER_MODEL);
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
        s.model, hide ? 0 : goodDisplayAmount(s),
        initX + x, initY - y, 0
      );
    }
  }
  
  
  public TalkFX chat() {
    return chat;
  }
  
  
  public void renderFor(Rendering rendering, Base base) {
    super.renderFor(rendering, base);
    toggleStatusDisplay();
    updateItemSprites();
    renderChat(rendering, base);
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    if (destroyed() || origin() == null) return;
    if (pathType() <= Tile.PATH_CLEAR || blueprint.isGrouped()) return;
    super.renderSelection(rendering, hovered);
  }
}










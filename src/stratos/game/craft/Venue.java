/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.craft;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.game.wild.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.sfx.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.craft.Economy.*;




public abstract class Venue extends Fixture implements
  Property, TileConstants, Schedule.Updates
{
  
  /**  Field definitions, constants, constructors, and save/load methods.
    */
  final public static int
    FACE_INIT   = -2,
    FACE_NONE   = -1,
    FACE_NORTH  =  N / 2,
    FACE_EAST   =  E / 2,
    FACE_SOUTH  =  S / 2,
    FACE_WEST   =  W / 2,
    ALL_FACES[] = { FACE_SOUTH, FACE_EAST, FACE_NORTH, FACE_WEST },
    NUM_FACES   =  ALL_FACES.length;
  
  final public static int
    PRIMARY_SHIFT      =  2,
    SECONDARY_SHIFT    =  1,
    OFF_DUTY           =  0,
    NOT_HIRED          = -1,
    
    SHIFTS_ALWAYS      =  0,
    SHIFTS_BY_HOURS    =  1,   //different 8-hour periods off.
    SHIFTS_BY_DAY      =  2,   //every second or third day off.
    SHIFTS_BY_24_HOUR  =  3,   //on for an entire day at a time.
    SHIFTS_BY_CALENDAR =  4;   //weekends and holidays off.  NOT DONE YET
  
  final public static Blueprint NO_REQUIREMENTS[] = new Blueprint[0];
  
  protected Base base;
  private Venue parent;
  
  final public Blueprint blueprint;
  final public Structure structure = new Structure(this);
  final public Staff     staff     = new Staff    (this);
  final public Stocks    stocks    = new Stocks   (this);
  
  protected Tile entrance;
  private List <Mobile> inside = new List <Mobile> ();
  private int facing = FACE_INIT;
  
  protected BuildingSprite buildSprite;
  final public TalkFX chat = ActorAssets.TALK_MODEL.makeSprite();
  private int nameID = -2;
  
  
  
  protected Venue(Blueprint blueprint, Base base) {
    super(blueprint.size, blueprint.high);
    structure.setupStats(blueprint);
    this.base      = base     ;
    this.blueprint = blueprint;
  }
  
  
  public Venue(Session s) throws Exception {
    super(s);
    base   = (Base ) s.loadObject();
    parent = (Venue) s.loadObject();
    
    blueprint = (Blueprint) s.loadObject();
    structure.loadState(s);
    staff    .loadState(s);
    stocks   .loadState(s);
    
    entrance = (Tile) s.loadObject();
    s.loadObjects(inside);
    this.facing = s.loadInt();
    
    buildSprite = (BuildingSprite) sprite();
    nameID = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(base  );
    s.saveObject(parent);
    
    s.saveObject(blueprint);
    structure.saveState(s);
    staff    .saveState(s);
    stocks   .saveState(s);
    
    s.saveObject (entrance);
    s.saveObjects(inside  );
    s.saveInt    (facing  );
    
    s.saveInt(nameID);
  }
  
  
  public Structure structure() { return structure; }
  public Staff staff() { return staff; }
  public Base base() { return base; }
  public Venue parent() { return parent; }
  
  
  public void assignBase(Base base) {
    if (! inWorld()) { this.base = base; return; }
    world.presences.togglePresence(this, false);
    this.base = base;
    world.presences.togglePresence(this, true);
  }
  
  
  public void assignParent(Venue parent) {
    this.parent = parent;
  }
  
  
  
  /**  Setup and positioning-
    */
  public boolean setupWith(Tile position, Box2D area, Coord... others) {
    if (position == null) return false;
    return setPosition(position.x, position.y, position.world);
  }
  
  
  public boolean setPosition(float x, float y, Stage world) {
    final Tile lastPos = origin();
    if (! super.setPosition(x, y, world)) return false;
    final Tile o = origin();
    final boolean moved = o != lastPos;
    //
    //  If position has been changed (or has been initially assigned) then
    //  we can take the liberty of choosing our ideal entrance-
    if (moved) {
      if (blueprint.isZoned()) setFacing(facing);
      else setFacing(SiteUtils.pickBestEntranceFace(this));
    }
    if (facing == FACE_INIT) setFacing(FACE_EAST);
    return true;
  }
  
  
  public void setFacing(int facing) {
    this.facing   = facing % NUM_FACES;
    this.entrance = pickEntrance(facing);
    this.canBoard = null;
  }
  
  
  public int facing() {
    return facing;
  }
  
  
  protected Tile pickEntrance(int facing) {
    final Tile o = origin();
    if (o == null || blueprint.isFixture()) {
      return null;
    }
    else {
      final int off[] = SiteUtils.entranceCoords(size, size, facing);
      return o.world.tileAt(o.x + off[0], o.y + off[1]);
    }
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
    if (! SiteUtils.checkAreaClear(footprint(), world, this, reasons, null)) {
      return false;
    }
    //
    //  We also check against any claims make by other structures, and try to
    //  avoid creating un-reachable areas with a closed-off perimeter.
    for (Venue c : world.claims.venuesConflicting(areaClaimed(), this)) {
      //
      //  TODO:  You need to return a full list of conflicting venues- claims,
      //  footprint, and perimeter- and subject them to a similar check...
      if (SiteUtils.trumpsSiting(this, c)) continue;
      if (reasons == Account.NONE) return false;
      return reasons.setFailure("Too close to "+c);
    }
    if (solid && ! checkPerimeter(world)) {
      return reasons.setFailure("Might obstruct pathing");
    }
    //
    //  All going well, return success.
    return reasons.setSuccess();
  }
  
  
  public boolean canPlace() {
    return canPlace(Account.NONE);
  }
  
  
  //  TODO:  Add these methods to a dedicated Siting class, I think...
  
  protected boolean entranceOkay() {
    if (blueprint.isFixture()) return true;
    if (! SiteUtils.isViableEntrance(this, entrance)) return false;
    return true;
  }
  
  
  public boolean canBuildOn(Tile t) {
    return t.habitat().pathClear;
  }
  
  
  protected boolean checkPerimeter(Stage world) {
    return SiteUtils.pathingOkayAround(this, world);
  }
  
  
  public Box2D areaClaimed() {
    return footprint();
  }
  
  
  public Tile[] reserved() {
    return new Tile[0];
  }
  
  
  public boolean preventsClaimBy(Venue other) {
    return blueprint.isZoned() || other.base() != base;
  }
  
  
  
  /**  Actual placement and life-cycle:
    */
  public void doPlacement(boolean intact) {
    intact |= GameSettings.buildFree || structure.intact();
    final Tile at = origin();
    final Stage world = at.world;
    
    if (intact) {
      final Box2D around = new Box2D(footprint()).expandBy(1);
      for (Tile t : world.tilesIn(around, true)) t.clearUnlessOwned(intact);
      enterWorldAt(at.x, at.y, world, true);
      structure.setState(Structure.STATE_INTACT, 1);
      onCompletion();
    }
    else {
      structure.setState(Structure.STATE_INSTALL, 0);
      for (Tile t : world.tilesIn(footprint(), false)) {
        t.setReserves(this, true);
      }
      enterWorldAt(at.x, at.y, world, false);
    }
    
    if (sprite() != null) {
      sprite().colour = null;
      sprite().passType = Sprite.PASS_NORMAL;
    }
  }
  
  
  public boolean enterWorldAt(int x, int y, Stage world, boolean intact) {
    if (! super.enterWorldAt(x, y, world, intact)) return false;
    world.schedule.scheduleForUpdates(this);
    
    if (base == null) I.complain("VENUES MUST HAVE A BASE ASSIGNED! "+this);
    
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
    
    if (base != null) updatePaving(false);
    world.schedule.unschedule(this);
    
    super.exitWorld();
  }
  
  
  public void onCompletion() {
    //
    //  TODO:  THIS IS USED BY THE DROPSHIP CLASS AS WELL- FACTOR THAT OUT!
    //
    //  As a final step, we take anything mobile within our footprint area and
    //  kick it outside:
    final Stage world = origin().world;
    Tile exit = mainEntrance();
    if (exit == null) {
      final Tile perim[] = Spacing.perimeter(footprint(), world);
      for (Tile p : perim) if (p != null && ! p.blocked()) { exit = p; break; }
    }
    if (exit == null) exit = Spacing.nearestOpenTile(this, this, world);
    if (exit == null) I.complain("No exit point from "+this);
    
    for (Tile t : world.tilesIn(footprint(), false)) {
      t.clearUnlessOwned();
      t.setAbove(this, true);
      for (Mobile m : t.inside()) m.setPosition(exit.x, exit.y, world);
    }
    updatePaving(true);
    for (Tile t : Spacing.perimeter(footprint(), world)) if (t != null) {
      t.clearUnlessOwned();
      RoadsRepair.updatePavingAround(t, base);
    }
    
    //  TODO:  RESTORE THIS!
    //  world.ephemera.addGhost(this, size, buildSprite.scaffolding(), 2.0f);
    refreshIncept(world, false);
  }
  
  
  public void setAsDestroyed(boolean salvaged) {
    if (! salvaged) Wreckage.reduceToSlag(footprint(), world);
    buildSprite.clearFX();
    super.setAsDestroyed(salvaged);
  }
  
  
  
  /**  Regular updates-
    */
  public float scheduledInterval() {
    return 1;
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    structure.updateStructure(numUpdates);
    
    if (instant) return;
    final boolean rare = numUpdates % 10 == 0;
    
    if (! structure.needsSalvage()) {
      if (rare) updatePaving(true);
      staff.updateStaff(numUpdates);
      impingeSupply(false);
    }
  }
  
  
  protected void updatePaving(boolean inWorld) {
    if (pathType() <= Tile.PATH_CLEAR) {
      final Tile under[] = Spacing.under(footprint(), world);
      base.transport.updatePerimeter(this, inWorld, under);
    }
    else {
      base.transport.updatePerimeter(this, inWorld);
    }
    base.transport.updateJunction(this, mainEntrance(), inWorld);
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
  
  
  public boolean allowsEntry(Accountable m) {
    if (! structure.intact()) return false;
    if (m.base() == base) return true;
    if (m instanceof Owner) {
      final Item pass = Item.withReference(ITEM_PASSCODE, base.faction());
      if (((Owner) m).inventory().hasItem(pass)) return true;
    }
    return false;
  }
  
  
  public int boardableType() {
    return Boarding.BOARDABLE_VENUE;
  }
  
  
  public boolean indoors() {
    return true;
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
      final int positions = numPositions(background);
      if (positions <= 0) return 1;
      final int hired = staff.numHired(background);
      return hired * 1f / positions;
    }
  }
  
  
  protected int numPositions(Background b) {
    return structure.upgradeLevel(b);
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
      choice.add(jobFor(actor));
    }
  }
  
  
  //  TODO:  Make these abstract?
  //
  //  By default, these do nothing.
  protected void addServices(Choice choice, Actor client) {}
  protected Behaviour jobFor(Actor actor) { return null; }
  
  
  protected void impingeSupply(boolean onEntry) {
    final int period = onEntry ? -1 : 1;
    base.demands.impingeSupply(getClass(), 1, period, this);
  }
  
  
  public int owningTier() {
    return blueprint.owningTier;
  }
  
  
  public Item[] materials() {
    return super.materials();
    //  TODO:  IMPLEMENT THIS!
    //return structure.materials();
  }
  
  
  public Inventory inventory() {
    return stocks;
  }
  
  
  public float priceFor(Traded good, boolean sold) {
    return good.defaultPrice();
  }
  
  
  public int spaceCapacity() {
    return structure.maxIntegrity() / 2;
  }
  
  
  public void afterTransaction(Item item, float amount) {
  }

  
  public Traded[] services() {
    return blueprint.tradeServices();
  }
  
  
  public Background[] careers() {
    return blueprint.careerServices();
  }
  
  
  
  /**  Interface methods-
    */
  public SelectionPane configSelectPane(SelectionPane panel, HUD UI) {
    return VenuePane.configStandardPanel(this, panel, UI, null);
  }
  
  
  public SelectionOptions configSelectOptions(SelectionOptions info, HUD UI) {
    return SelectionOptions.configOptions(this, info, UI);
  }
  
  
  public String fullName() {
    if (blueprint.isFixture()) return blueprint.name;
    if (base.isPrimal()      ) return blueprint.name;
    if (blueprint.isUnique ()) return "The "+blueprint.name;
    
    if (nameID == -2 && inWorld()) {
      nameID = base.nextVenueID(blueprint);
    }
    if (nameID < 0) return blueprint.name;
    
    final String suffix = ""+nameID;
    return blueprint.name+" "+suffix;
  }
  

  public String helpInfo() {
    return blueprint.description;
  }
  
  
  public Composite portrait(HUD UI) {
    if (blueprint.icon == null) return null;
    return Composite.withImage(blueprint.icon, blueprint.keyID);
  }
  
  
  public String objectCategory() {
    return blueprint.category;
  }
  
  
  public Constant infoSubject() {
    return blueprint;
  }
  
  
  protected boolean showLights() {
    if (blueprint.isFixture()) return true;
    return staff.visitors().size() > 0;
  }
  
  
  protected void toggleStatusFor(Traded need, ModelAsset model) {
    if (! structure.intact()) buildSprite.toggleFX(need.model, false);
    final boolean needs = stocks.relativeShortage(need, false) > 0.5f;
    buildSprite.toggleFX(model, needs);
  }
  
  
  protected void toggleStatusDisplay() {
    final boolean showBurn = structure.burning();
    buildSprite.toggleFX(VenueAssets.BLAST_MODEL, showBurn);
    toggleStatusFor(ATMO , VenueAssets.ATMO_MODEL );
    toggleStatusFor(POWER, VenueAssets.POWER_MODEL);
    toggleStatusFor(WATER, VenueAssets.WATER_MODEL);
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
  
  
  protected boolean showHoverStockIcons() {
    return false;
  }
  
  
  protected void updateItemSprites() {
    final Traded services[] = goodsToShow();
    final float  offsets [] = goodDisplayOffsets();
    if (services == null || offsets == null) return;
    
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
  
  
  public BuildingSprite buildSprite() {
    return buildSprite;
  }
  
  
  public void renderFor(Rendering rendering, Base base) {
    //
    //  (Note- see flagSpriteForChange in the Repairs class.)
    position(buildSprite.position);
    
    //String animState = BuildingSprite.STATE_FOUNDING;
    //buildSprite.setAnimation(animState, structure.repairLevel(), false);
    //*
    buildSprite.updateCondition(
      structure.repairLevel(),
      structure.intact     (),
      structure.burning    ()
    );
    
    if (buildSprite.flagChange) {
      final Tile o = origin();
      final boolean map[][] = new boolean[size][size];
      for (Tile t : world.tilesIn(footprint(), false)) {
        map[t.x - o.x][t.y - o.y] = t.above() == this;
      }
      buildSprite.toggleFoundation(map);
    }
    //*/
    
    buildSprite.passType = Sprite.PASS_NORMAL;
    super.renderFor(rendering, base);
    
    renderHealthbars(rendering, base);
    toggleStatusDisplay();
    updateItemSprites();
    renderChat(rendering, base);
  }
  

  public void attachSprite(Sprite sprite) {
    if (sprite == null) {
      buildSprite = null;
      super.attachSprite(null);
    }
    else {
      //  TODO:  Problem.  BuildingSprites need a dedicated model now- and not
      //  all the art will accomodate that.  Shoot.
      
      buildSprite = BuildingSprite.fromBase(sprite, size, high);
      super.attachSprite(buildSprite);
    }
  }
  
  
  protected void renderHealthbars(Rendering rendering, Base base) {
    final boolean focused = BaseUI.isSelectedOrHovered(this);
    final boolean alarm =
      structure.intact() && (base == base() || focused) &&
      (structure.burning() || structure.repairLevel() < 0.25f);
    if ((! focused) && (! alarm)) return;
    
    float zoff = high;
    //
    //  First of all, we show the structure's current healthbar, and possibly
    //  a sub-bar to show upgrade progress.
    final int NU = structure.numOptionalUpgrades();
    final Healthbar healthbar = new Healthbar();
    healthbar.hurtLevel = 1 - structure.repairLevel();
    healthbar.size = (radius() * 50);
    healthbar.size *= 1 + Structure.UPGRADE_HP_BONUSES[NU];
    healthbar.matchTo(buildSprite);
    healthbar.position.z += (zoff += 0.6f);
    healthbar.readyFor(rendering);
    
    healthbar.colour = base().colour();
    healthbar.alarm = alarm;
    
    if (structure.needsUpgrade()) {
      final Upgrade inProg = structure.upgradeInProgress();
      Healthbar progBar = new Healthbar();
      progBar.hurtLevel = 1 - structure.upgradeProgress(inProg);
      progBar.tireLevel = 0;
      progBar.size = healthbar.size;
      progBar.position.setTo(healthbar.position);
      progBar.yoff = 0 - Healthbar.BAR_HEIGHT;
      
      final Colour c = new Colour(healthbar.colour);
      c.blend(Colour.WHITE, 0.5f);
      progBar.colour = c;
      progBar.flash  = c;
      progBar.readyFor(rendering);
    }
    //
    //  We also show a visual indication of all the goods present at this
    //  venue...
    if (renderStockHoverIcons(healthbar, rendering)) {
      //zoff += 0.4f;
    }
    //
    //  And then the name-label:
    final Label label = ActorAssets.LABEL_MODEL.makeSprite();
    label.matchTo(buildSprite);
    label.position.z += (zoff += 0.1f);
    label.phrase = this.fullName();
    label.readyFor(rendering);
    label.fontScale = 1.0f;
  }
  
  
  protected boolean renderStockHoverIcons(
    Healthbar healthbar, Rendering rendering
  ) {
    if (! showHoverStockIcons()) return false;
    
    //  TODO:  MOVE THIS OUT TO A DEDICATED WIDGET-CLASS!
    final Batch <CutoutModel> itemModels = new Batch();
    final Batch <CutoutModel> tickModels = new Batch();
    for (Traded t : VenuePane.ITEM_LIST_ORDER) {
      final float
        amount      = stocks.amountOf   (t),
        consumption = stocks.consumption(t),
        production  = stocks.production (t),
        totalDemand = consumption + production;
      if (totalDemand == 0 && amount == 0) continue;
      if (t.form == Economy.FORM_PROVISION && totalDemand <= amount) continue;
      
      itemModels.add(t.model);
      if      (amount < consumption / 2) tickModels.add(Traded.SHORTAGE_MODEL);
      else if (amount < totalDemand / 2) tickModels.add(Traded.QUESTION_MODEL);
      else                               tickModels.add(Traded.OKAY_MODEL    );
    }
    if (itemModels.size() > 0) {
      CutoutSprite.renderAbove(
        healthbar.position, 0, 0.1f - 0.5f, -1, rendering,
        0.25f, 0.67f, itemModels
      );
      CutoutSprite.renderAbove(
        healthbar.position, 0.1f, 0.2f - 0.5f, -1.1f, rendering,
        0.25f, 0.33f, tickModels
      );
      return true;
    }
    return false;
  }
  
  
  public void previewPlacement(boolean canPlace, Rendering rendering) {
    final Sprite sprite = this.buildSprite;
    if (sprite == null) return;
    this.viewPosition(sprite.position);
    
    renderClaim(rendering, false, canPlace ? Colour.WHITE : Colour.SOFT_RED);
    
    if (canPlace) {
      sprite.colour = new Colour(0, 1, 0, 0.5f);
      sprite.passType = Sprite.PASS_PREVIEW;
      sprite.readyFor(rendering);
    }
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    renderSelection(rendering, hovered, Colour.WHITE);
  }
  
  
  protected void renderSelection(
    Rendering rendering, boolean hovered, Colour tinge
  ) {
    if (destroyed() || origin() == null) return;
    if (pathType() <= Tile.PATH_CLEAR || blueprint.isLinear()) return;
    final Colour temp = new Colour();
    
    final String key = origin()+"_print_"+this;
    temp.set(Colour.transparency(hovered ? 0.5f : 1)).multiply(tinge);
    BaseUI.current().selection.renderTileOverlay(
      rendering, origin().world, temp,
      Selection.SELECT_OVERLAY, false,
      key, true, this
    );
    
    renderClaim(rendering, hovered, tinge);
  }
  
  
  protected void renderClaim(
    Rendering rendering, boolean hovered, Colour tinge
  ) {
    final String keyRes = origin()+"_reserve_print_"+this;
    final Colour temp = new Colour();
    float opacity = (hovered ? 0.25f : 0.375f) * (inWorld() ? 1 : 2);
    temp.set(Colour.transparency(opacity)).multiply(tinge);
    
    if (inWorld()) {
      /*
      BaseUI.current().selection.renderTileOverlay(
        rendering, origin().world, temp,
        Selection.SELECT_OVERLAY, false,
        keyRes, true, areaClaimed()
      );
      //*/
    }
    else {
      final Batch <Object> under = new Batch <Object> ();
      for (Tile t : reserved()) {
        if (t != null) under.add(t);
      }
      under.add(footprint());
      if (mainEntrance() != null) under.add(mainEntrance());
      
      BaseUI.current().selection.renderTileOverlay(
        rendering, origin().world, temp,
        Habitat.RESERVE_TEXTURE, true,
        "install_preview", false, under.toArray()
      );
    }
  }
}












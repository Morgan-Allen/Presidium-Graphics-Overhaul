/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.building ;
import stratos.game.actors.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.sfx.*;
import stratos.graphics.terrain.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;




public abstract class Venue extends Fixture implements
  Schedule.Updates, Boardable, Installation,
  Inventory.Owner, Employer,
  Selectable, TileConstants, Economy
{
  
  
  /**  Field definitions, constants, constructors, and save/load methods.
    */
  final protected static String
    CAT_STATUS   = "STATUS",
    CAT_STAFF    = "STAFF",
    CAT_STOCK    = "STOCK",
    CAT_UPGRADES = "UPGRADES";
  final public static int
    ENTRANCE_NONE  = -1,
    ENTRANCE_NORTH =  N / 2,
    ENTRANCE_EAST  =  E / 2,
    ENTRANCE_SOUTH =  S / 2,
    ENTRANCE_WEST  =  W / 2,
    NUM_SIDES      =  4 ;
  final public static int
    
    PRIMARY_SHIFT      = 1,
    SECONDARY_SHIFT    = 2,
    OFF_DUTY           = 3,
    
    SHIFTS_ALWAYS      = 0,
    SHIFTS_BY_HOURS    = 1,   //different 8-hour periods off.
    SHIFTS_BY_DAY      = 2,   //every second or third day off.
    SHIFTS_BY_CALENDAR = 3 ;  //weekends and holidays off.  NOT DONE YET
  
  
  BuildingSprite buildSprite ;
  final Healthbar healthbar = new Healthbar();
  final Label label = new Label();
  final public TalkFX chat = new TalkFX();
  
  protected int entranceFace ;
  protected Tile entrance ;
  
  private Base base ;
  private List <Mobile> inside = new List <Mobile> () ;
  
  final public Personnel   personnel = new Personnel(this)   ;
  final public VenueStocks stocks    = new VenueStocks(this) ;
  final public Structure   structure = new Structure(this)   ;
  
  
  
  public Venue(int size, int high, int entranceFace, Base base) {
    super(size, high) ;
    this.base = base ;
    this.entranceFace = entranceFace ;
  }
  
  
  public Venue(Session s) throws Exception {
    super(s) ;
    buildSprite = (BuildingSprite) sprite() ;

    entranceFace = s.loadInt() ;
    entrance = (Tile) s.loadTarget() ;
    base = (Base) s.loadObject() ;
    s.loadObjects(inside) ;
    
    personnel.loadState(s) ;
    stocks.loadState(s) ;
    structure.loadState(s) ;
    
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(entranceFace) ;
    s.saveTarget(entrance) ;
    s.saveObject(base) ;
    s.saveObjects(inside) ;
    
    personnel.saveState(s) ;
    stocks.saveState(s) ;
    structure.saveState(s) ;
  }
  
  
  public Index <Upgrade> allUpgrades() { return null ; }
  public Structure structure() { return structure ; }
  public Personnel personnel() { return personnel ; }
  
  public int owningType() { return VENUE_OWNS ; }
  public Base base() { return base ; }
  
  protected BuildingSprite buildSprite() { return buildSprite ; }
  
  
  public void assignBase(Base base) {
    if (! inWorld()) { this.base = base; return; }
    world.presences.togglePresence(this, false) ;
    this.base = base ;
    world.presences.togglePresence(this, true) ;
  }
  
  
  
  /**  Dealing with items and inventory-
    */
  public Inventory inventory() {
    return stocks ;
  }
  
  
  public float priceFor(Service service) {
    return stocks.priceFor(service) ;
  }
  
  
  public int spaceFor(Service good) {
    return structure.maxIntegrity() ;
  }
  
  
  public void afterTransaction(Item item, float amount) {
  }
  
  
  
  /**  Installation and positioning-
    */
  public boolean canPlace() {
    if (origin() == null) return false ;
    final World world = origin().world ;
    
    //  Make sure we don't displace any more important object, or occupy their
    //  entrances.  In addition, the entrance must be clear.
    final int OT = owningType() ;
    for (Tile t : world.tilesIn(area(), false)) {
      if (t == null || t.owningType() >= OT) return false ;
      for (Element e : Spacing.entranceFor(t)) {
        if (e.owningType() >= OT) return false ;
      }
    }
    
    if (! checkPerimeter(world)) return false;
    final Tile e = mainEntrance() ;
    if (e != null && e.owningType() >= OT) return false ;
    return true ;
  }
  
  
  protected boolean checkPerimeter(World world) {
    //  Don't abut on anything of higher priority-
    for (Tile n : Spacing.perimeter(area(), world)) {
      if (n == null || (n.owner() != null && ! canTouch(n.owner()))) {
        return false ;
      }
    }
    //  And make sure we don't create isolated areas of unreachable tiles-
    if (! Spacing.perimeterFits(this)) return false ;
    return true;
  }
  
  
  public boolean setPosition(float x, float y, World world) {
    if (! super.setPosition(x, y, world)) return false ;
    final Tile o = origin() ;
    if (entranceFace == ENTRANCE_NONE) {
      entrance = null ;
    }
    else {
      final int off[] = Spacing.entranceCoords(size, size, entranceFace) ;
      entrance = world.tileAt(o.x + off[0], o.y + off[1]) ;
    }
    return true ;
  }
  
  
  public boolean enterWorldAt(int x, int y, World world) {
    if (! super.enterWorldAt(x, y, world)) return false ;

    world.presences.togglePresence(this, true);
    world.schedule.scheduleForUpdates(this);
    stocks.onWorldEntry();
    personnel.onCommission();
    return true ;
  }
  
  
  public void exitWorld() {
    stocks.onWorldExit();
    personnel.onDecommission();
    world.presences.togglePresence(this, false);
    if (base != null) updatePaving(false);
    world.schedule.unschedule(this);
    
    super.exitWorld() ;
  }
  
  
  public void setAsEstablished(boolean isDone) {
    super.setAsEstablished(isDone) ;
  }
  
  
  public void onCompletion() {
    world.ephemera.addGhost(this, size, buildSprite.scaffolding(), 2.0f) ;
    setAsEstablished(false) ;
  }
  
  
  public void onDecommission() {
    world.ephemera.addGhost(this, size, buildSprite.baseSprite(), 2.0f) ;
    setAsEstablished(false) ;
  }
  
  
  public void onDestruction() {
    Wreckage.reduceToSlag(area(), world) ;
  }
  
  
  public void setAsDestroyed() {
    buildSprite().clearFX() ;
    super.setAsDestroyed() ;
  }
  
  
  
  /**  Implementing the Boardable interface-
    */
  public void setInside(Mobile m, boolean is) {
    if (is) {
      inside.include(m) ;
    }
    else {
      inside.remove(m) ;
    }
  }
  
  
  public List <Mobile> inside() {
    return inside ;
  }
  
  
  public Tile mainEntrance() {
    return entrance ;
  }
  
  
  public Box2D area(Box2D put) {
    if (put == null) put = new Box2D() ;
    final Tile o = origin() ;
    put.set(o.x - 0.5f, o.y - 0.5f, size, size) ;
    return put ;
  }
  
  
  public Boardable[] canBoard(Boardable batch[]) {
    final int minSize = 1 + inside.size() ;
    if (batch == null || batch.length < minSize) {
      batch = new Boardable[minSize] ;
    }
    else for (int i = batch.length ; i-- > 1 ;) batch[i] = null ;
    batch[0] = entrance ;
    int i = 1 ; for (Mobile m : inside) if (m instanceof Boardable) {
      batch[i++] = (Boardable) m ;
    }
    return batch ;
  }
  
  
  public boolean isEntrance(Boardable t) {
    return entrance == t ;
  }
  
  
  public boolean allowsEntry(Mobile m) {
    return m.base() == base ;
  }
  
  
  public int boardableType() {
    return Boardable.BOARDABLE_VENUE ;
  }
  
  
  
  /**  Updates and life cycle-
    */
  public float scheduledInterval() {
    return 1 ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    if (destroyed()) {
      I.say(this+" IS DESTROYED! SHOULD NOT BE ON SCHEDULE!") ;
      this.setAsDestroyed() ;
    }
    structure.updateStructure(numUpdates) ;
    if (! structure.needsSalvage()) {
      if (base != null && numUpdates % 10 == 0) updatePaving(true) ;
      personnel.updatePersonnel(numUpdates) ;
    }
    if (structure.intact()) {
      stocks.updateStocks(numUpdates) ;
    }
  }
  
  
  protected void updatePaving(boolean inWorld) {
    if (inWorld) {
      base.paving.updateJunction(this, mainEntrance(), true) ;
      base.paving.updatePerimeter(this, true) ;
    }
    else {
      base.paving.updatePerimeter(this, false) ;
      base.paving.updateJunction(this, mainEntrance(), false) ;
    }
  }
  
  
  
  /**  Recruiting staff and assigning manufacturing tasks-
    */
  public int numOpenings(Background v) {
    return structure.upgradeBonus(v) - personnel.numHired(v);
  }
  
  
  public boolean isManned() {
    for (Actor a : personnel.workers) {
      if (a.health.conscious() && a.aboard() == this) return true ;
    }
    return false ;
  }
  
  
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
  
  
  public abstract Background[] careers() ;
  public abstract Service[] services() ;  //TODO:  Rename to Goods?
  public void addServices(Choice choice, Actor forActor) {}
  
  
  public boolean privateProperty() {
    return false ;
  }
  
  
  
  /**  Installation interface-
    */
  public int buildCost() {
    return structure.buildCost() ;
  }
  
  
  public void placeFromOrigin() {
    final Tile t = origin();
    final int HS = this.size / 2;
    doPlace(t.world.tileAt(t.x + HS, t.y + HS), null);
  }
  
  //  TODO:  This built-in offset is introducing unnecesary complications.  Try
  //  to move it back to the InstallTab/InstallTask code
  
  public boolean pointsOkay(Tile from, Tile to) {
    //  You have to check for visibility too.  Have a Base argument?
    if (from == null) return false ;
    final int HS = this.size / 2;
    final Tile t = from.world.tileAt(from.x - HS, from.y - HS) ;
    if (t == null) return false;
    setPosition(t.x, t.y, t.world) ;
    return canPlace() ;
  }
  
  
  public void doPlace(Tile from, Tile to) {
    if (sprite() != null) sprite().colour = null ;
    pointsOkay(from, to);
    clearSurrounds() ;
    enterWorld() ;
    
    if (GameSettings.buildFree) {
      structure.setState(Structure.STATE_INTACT , 1) ;
      //I.say("Now placing: "+this+" in intact state") ;
    }
    else {
      structure.setState(Structure.STATE_INSTALL, 0) ;
      //I.say("Now placing: "+this+" in install phase") ;
    }
  }
  
  
  public void preview(
    boolean canPlace, Rendering rendering, Tile from, Tile to
  ) {
    if (from == null) return ;
    pointsOkay(from, to);
    
    if (canPlace) BaseUI.current().selection.renderTileOverlay(
      rendering, from.world, canPlace ? Colour.GREEN : Colour.RED,
      Selection.SELECT_OVERLAY, false, this, this
    );
    
    final Sprite sprite = this.buildSprite;
    if (sprite == null) return ;
    this.viewPosition(sprite.position);
    sprite.colour = canPlace ? Colour.GREEN : Colour.RED ;
    sprite.passType = Sprite.PASS_PREVIEW;
    sprite.readyFor(rendering);
  }
  
  
  
  /**  Interface methods-
    */
  public String toString() {
    return fullName() ;
  }
  

  public TargetInfo configInfo(TargetInfo info, BaseUI UI) {
    if (info == null) info = new TargetInfo(UI, this);
    return info;
  }
  
  
  public InfoPanel configPanel(InfoPanel panel, BaseUI UI) {
    return VenueDescription.configStandardPanel(this, panel, UI);
  }

  
  public void whenTextClicked() {
    BaseUI.current().selection.pushSelection(this, false);
  }
  
  
  public Target selectionLocksOn() { return this; }
  
  
  
  /**  Rendering methods-
    */
  public void attachSprite(Sprite sprite) {
    if (sprite == null) super.attachSprite(null);
    else {
      buildSprite = BuildingSprite.fromBase(sprite, size, high);
      super.attachSprite(buildSprite) ;
    }
  }
  
  
  protected float fogFor(Base base) {
    if (base == this.base) return (1 + super.fogFor(base)) / 2f ;
    return super.fogFor(base) ;
  }
  
  
  protected boolean showLights() {
    return isManned() ;
  }
  
  
  protected void renderHealthbars(Rendering rendering, Base base) {
    final boolean focused = BaseUI.isSelectedOrHovered(this);
    final boolean alarm =
      structure.intact() && (base == base() || focused) &&
      (structure.burning() || structure.repairLevel() < 0.25f);
    if ((! focused) && (! alarm)) return;
    
    final int NU = structure.numUpgrades();
    healthbar.level = structure.repairLevel();
    healthbar.size = (radius() * 50);
    healthbar.size *= 1 + Structure.UPGRADE_HP_BONUSES[NU];
    healthbar.matchTo(buildSprite);
    healthbar.position.z += height() + 0.1f;
    healthbar.readyFor(rendering);
    
    if (base() == null) healthbar.colour = Colour.LIGHT_GREY;
    else healthbar.colour = base().colour;
    healthbar.alarm = alarm;
    
    label.matchTo(buildSprite);
    label.position.z += height() - 0.25f;
    label.phrase = this.fullName();
    label.readyFor(rendering);
    label.fontScale = 1.0f;
    
    if (structure.needsUpgrade()) {
      Healthbar progBar = new Healthbar();
      progBar.level = structure.upgradeProgress();
      progBar.size = healthbar.size;
      progBar.position.setTo(healthbar.position);
      progBar.yoff = Healthbar.BAR_HEIGHT;
      
      final Colour c = new Colour(healthbar.colour);
      c.set(
        (1 + c.r) / 2,
        (1 + c.g) / 2,
        (1 + c.b) / 2,
        1
      );
      progBar.colour = c;
      progBar.warn = healthbar.colour;
      progBar.readyFor(rendering);
    }
  }
  
  
  protected void toggleStatusFor(Service need, ModelAsset model) {
    if (! structure.intact()) buildSprite.toggleFX(need.model, false);
    buildSprite.toggleFX(model, stocks.shortagePenalty(need) > 0);
  }
  
  
  protected void toggleStatusDisplay() {
    final boolean showBurn = structure.burning();
    buildSprite.toggleFX(BuildingSprite.BLAST_MODEL, showBurn);
    toggleStatusFor(LIFE_SUPPORT, BuildingSprite.LIFE_SUPPORT_MODEL);
    toggleStatusFor(POWER       , BuildingSprite.POWER_MODEL);
    toggleStatusFor(WATER       , BuildingSprite.WATER_MODEL);
  }
  
  
  protected void renderChat(Rendering rendering, Base base) {
    if (! structure.intact()) return ;
    if (chat.numPhrases() > 0) {
      chat.position.setTo(sprite().position) ;
      chat.position.z += height() ;
      chat.readyFor(rendering);
    }
  }
  
  
  
  private boolean canShow(Service type) {
    if (type.form == FORM_PROVISION) return false ;
    if (type.picPath == Service.DEFAULT_PIC_PATH) return false ;
    return true ;
  }

  
  final protected static float STANDARD_GOOD_SPRITE_OFFSETS[] = {
    0, 0,
    1, 0,
    0, 1,
    2, 0,
    0, 2,
    3, 0,
    0, 3
  } ;
  
  
  protected float[] goodDisplayOffsets() {
    return STANDARD_GOOD_SPRITE_OFFSETS ;
  }
  
  
  protected Service[] goodsToShow() {
    return services() ;
  }
  
  
  protected float goodDisplayAmount(Service good) {
    if (! structure.intact()) return 0 ;
    return stocks.amountOf(good) ;
  }
  
  
  protected void updateItemSprites() {
    final Service services[] = goodsToShow() ;
    final float offsets[] = goodDisplayOffsets() ;
    if (services == null) return ;
    
    final boolean hide = ! structure.intact() ;
    final float
      initY = (size / 2f) - BuildingSprite.ITEM_SIZE,
      initX = BuildingSprite.ITEM_SIZE - (size / 2f) ;
    
    int index = -1 ;
    for (Service s : services) if (canShow(s)) index += 2 ;
    if (index < 0) return ;
    index = Visit.clamp(index, offsets.length) ;
    
    for (int SI = services.length; SI-- > 0;) {
      final Service s = services[SI];
      if (! canShow(s)) continue;
      if (index < 0) break ;
      final float y = offsets[index--], x = offsets[index--] ;
      if (y >= size || size <= -x) continue ;
      buildSprite.updateItemDisplay(
        s.model,
        hide ? 0 : goodDisplayAmount(s),
        initX + x,
        initY - y
      ) ;
    }
  }
  
  
  public void renderFor(Rendering rendering, Base base) {
    position(buildSprite.position) ;
    super.renderFor(rendering, base) ;
    buildSprite.updateCondition(
      structure.repairLevel(),
      structure.intact(),
      structure.burning()
    ) ;
    buildSprite.passType = Sprite.PASS_NORMAL;
    toggleStatusDisplay() ;
    updateItemSprites() ;
    renderHealthbars(rendering, base) ;
    renderChat(rendering, base) ;
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    if (destroyed() || ! inWorld()) return ;
    
    BaseUI.current().selection.renderTileOverlay(
      rendering, world,
      hovered ? Colour.transparency(0.5f) : Colour.WHITE,
      Selection.SELECT_OVERLAY, true, this, this
    );
  }
}





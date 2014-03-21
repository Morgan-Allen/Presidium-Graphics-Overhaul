/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.building ;
import stratos.game.actors.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.game.planet.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.sfx.*;
import stratos.graphics.terrain.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;




public abstract class Venue extends Fixture implements
  Schedule.Updates, Boardable, Installation,
  Inventory.Owner, Employment,
  Selectable, TileConstants, Economy
{
  
  
  /**  Field definitions, constants, constructors, and save/load methods.
    */
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
    //
    //  Make sure we don't displace any more important object, or occupy their
    //  entrances.  In addition, the entrance must be clear.
    final int OT = owningType() ;
    for (Tile t : world.tilesIn(area(), false)) {
      if (t == null || t.owningType() >= OT) return false ;
      for (Element e : Spacing.entranceFor(t)) {
        if (e.owningType() >= OT) return false ;
      }
    }
    //
    //  Don't abut on anything of higher priority-
    for (Tile n : Spacing.perimeter(area(), world)) {
      if (n == null || (n.owner() != null && ! canTouch(n.owner()))) {
        return false ;
      }
    }
    //
    //  And make sure we don't create isolated areas of unreachable tiles-
    if (! Spacing.perimeterFits(this)) return false ;
    final Tile e = mainEntrance() ;
    if (e != null && e.owningType() >= OT) return false ;
    return true ;
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

    world.presences.togglePresence(this, true ) ;
    world.schedule.scheduleForUpdates(this) ;
    personnel.onCommission() ;
    return true ;
  }
  
  
  public void exitWorld() {
    personnel.onDecommission() ;
    world.presences.togglePresence(this, false) ;
    if (base != null) updatePaving(false) ;
    world.schedule.unschedule(this) ;
    
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
  
  public abstract Background[] careers() ;
  public abstract Service[] services() ;  //TODO:  Rename to Goods?
  public void addServices(Choice choice, Actor forActor) {}
  
  
  //  public int occupancy() ;
  //  public int comfortLevel() ;
  //  public boolean allowsResidence(Actor a) ;
  
  
  public boolean privateProperty() {
    return false ;
  }
  
  
  
  /**  Installation interface-
    */
  public int buildCost() {
    return structure.buildCost() ;
  }
  
  
  public boolean pointsOkay(Tile from, Tile to) {
    //  You have to check for visibility too.  Have a Base argument?
    if (from == null) return false ;
    final Tile t = from ;
    setPosition(t.x, t.y, t.world) ;
    return canPlace() ;
  }
  
  
  public void doPlace(Tile from, Tile to) {
    if (sprite() != null) sprite().colour = null ;
    final Tile t = from ;
    setPosition(t.x, t.y, t.world) ;
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
    final Tile t = from ;
    final World world = t.world ;
    setPosition(t.x, t.y, t.world) ;
    
    //  TODO:  RESTORE THIS
    /*
    final TerrainMesh overlay = world.terrain().createOverlay(
      world, surrounds(), false, Texture.WHITE_TEX
    ) ;
    overlay.colour = canPlace ? Colour.GREEN : Colour.RED ;
    rendering.addClient(overlay) ;
    //*/
    
    if (sprite() == null) return ;
    this.viewPosition(sprite().position);
    sprite().colour = canPlace ? Colour.GREEN : Colour.RED ;
    sprite().readyFor(rendering);
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
    if (panel == null) panel = new InfoPanel(
      UI, this, 0, "STATUS", "STAFF", "STOCK", "UPGRADES"
    );
    final int categoryID = panel.categoryID();
    final Description d = panel.detail();
    if (categoryID == 0) describeCondition(d, UI) ;
    if (categoryID == 1) describePersonnel(d, UI) ;
    if (categoryID == 2) describeStocks(d, UI) ;
    if (categoryID == 3) describeUpgrades(d, UI) ;
    return panel;
  }
  
  
  private void describeCondition(Description d, BaseUI UI) {
    
    d.append("Condition and Repair:") ;
    d.append("\n  Integrity: ") ;
    d.append(structure.repair()+" / "+structure.maxIntegrity()) ;
    
    //
    //  If there's an upgrade in progress, list it here.
    final String CUD = structure.currentUpgradeDesc() ;
    if (CUD != null) d.append("\n  "+CUD) ;
    d.append("\n  Materials Needed: "+"None") ;
    d.append("\n  Untaxed Credits: "+(int) stocks.credits()) ;
    
    final float squalor = 0 - world.ecology().ambience.valueAt(this) ;
    if (squalor > 0) {
      final String SN = " ("+I.shorten(squalor, 1)+")" ;
      d.append("\n  "+Ambience.squalorDesc(squalor)+" Squalor"+SN) ;
    }
    else {
      final String AN = " ("+I.shorten(0 - squalor, 1)+")" ;
      d.append("\n  "+Ambience.squalorDesc(squalor)+" Ambience"+AN) ;
    }
    
    final float danger = base().dangerMap.longTermVal(world.tileAt(this)) ;
    if (danger > 0) {
      final String DN = " ("+I.shorten(danger, 1)+")" ;
      d.append("\n  "+Ambience.dangerDesc(danger)+" Hazards"+DN) ;
    }
    else {
      final String SN = " ("+I.shorten(0 - danger, 1)+")" ;
      d.append("\n  "+Ambience.dangerDesc(danger)+" Safety"+SN) ;
    }
    
    d.append("\n\n") ;
    
    d.append(helpInfo(), Colour.LIGHT_GREY) ;
  }
  
  
  protected void describeStocks(Description d, BaseUI UI) {
    d.append("Stocks and Orders:") ;
    boolean empty = true ;
    
    final Sorting <Item> listing = new Sorting <Item> () {
      public int compare(Item a, Item b) {
        if (a.equals(b)) return 0 ;
        if (a.type.typeID > b.type.typeID) return  1 ;
        if (a.type.typeID < b.type.typeID) return -1 ;
        if (a.refers != null && b.refers != null) {
          if (a.refers.hashCode() > b.refers.hashCode()) return  1 ;
          if (a.refers.hashCode() < b.refers.hashCode()) return -1 ;
        }
        if (a.quality > b.quality) return  1 ;
        if (a.quality < b.quality) return -1 ;
        return 0 ;
      }
    } ;
    for (Item item : stocks.allItems()) listing.add(item) ;
    for (Service type : ALL_ITEM_TYPES) {
      if (stocks.demandFor(type) > 0 && stocks.amountOf(type) == 0) {
        listing.add(Item.withAmount(type, 0)) ;
      }
    }
    if (listing.size() > 0) empty = false ;
    for (Item item : listing) describeStocks(item, d) ;
    
    for (Manufacture m : stocks.specialOrders()) {
      d.append("\n  ") ; m.describeBehaviour(d) ; empty = false ;
    }
    if (empty) d.append("\n  No stocks or orders.") ;
  }
  
  
  protected boolean describeStocks(Item item, Description d) {
    final float needed ;
    final Service type = item.type ;
    if (this instanceof Service.Trade) {
      final Service.Trade trade = (Service.Trade) this ;
      needed = Math.max(Math.max(
        trade.exportDemand(type),
        trade.importDemand(type)
      ), stocks.demandFor(type)) ;
    }
    else needed = stocks.demandFor(type) ;
    final float amount = stocks.amountOf(type) ;
    if (needed == 0 && amount == 0) return false ;
    
    final String nS = I.shorten(needed, 1) ;
    d.append("\n  ") ;
    item.describeTo(d) ;
    
    //if (Visit.arrayIncludes(services(), type) && item.refers == null) {
      final int price = (int) Math.ceil(priceFor(type)) ;
      d.append(" /"+nS+" (Price "+price+")") ;
    //}
    return true ;
  }
  
  
  
  private void describePersonnel(Description d, BaseUI UI) {
    final Background c[] = careers();
    if (c != null && c.length > 0) {
      d.append("\nCareers and Openings:");
      for (Background v : c) {
        final int
          hired = personnel.numHired(v),
          total = hired + numOpenings(v);
        d.append("\n  "+hired+"/"+total+" "+v.name);
      }
      d.append("\n");
    }
    
    d.append("\nPersonnel:");
    for (final Application a : personnel.applications) {
      final Actor p = a.applies;
      d.append("\n") ;
      ((Text) d).insert(p.portrait(UI).texture(), 40);
      d.append(p);
      d.append(p.inWorld() ? " (" : " (Offworld ");
      d.append(p.vocation().name+")\n  ");
      
      d.append(new Description.Link("Hire for "+a.hiringFee()+" cred") {
        public void whenClicked() { personnel.confirmApplication(a); }
      }) ;
    }
    
    final Batch <Mobile> considered = new Batch <Mobile> () ;
    for (Actor m : personnel.residents()) considered.include(m) ;
    for (Actor m : personnel.workers()) considered.include(m) ;
    for (Mobile m : inside) considered.include(m) ;
    
    for (Mobile m : considered) {
      d.append("\n  ") ;
      if (d instanceof Text && m instanceof Human) {
        final Composite p = ((Human) m).portrait(UI);
        ((Text) d).insert(p.texture(), 40);
      }
      d.append(m) ;
      if (m instanceof Actor) {
        d.append("\n  ") ;
        d.append(descDuty((Actor) m)) ;
      }
      d.append("\n  ") ;
      m.describeStatus(d) ;
    }
  }
  
  
  private String descDuty(Actor a) {
    final Background v = a.vocation() ;
    final String VN = v == null ? a.species().toString() : v.nameFor(a) ;
    if (a.mind.home() == this) return "(Resident "+VN+")" ;
    if (a.mind.work() != this) return "(Visiting "+VN+")" ;
    final String duty = personnel.onShift(a) ? "On-Duty" : "Off-Duty" ;
    return "("+duty+" "+VN+")" ;
  }
  
  
  private static Upgrade lastCU ;  //last clicked...
  
  private void describeUpgrades(Description d, BaseUI UI) {
    final Base played = BaseUI.current().played();
    
    if (played == base && ! privateProperty()) {
      d.append("Orders: ") ;
      final Venue v = this ;
      if (structure.needsSalvage()) {
        d.append(new Description.Link("\n  Cancel Salvage") {
          public void whenClicked() {
            final float condition = structure.repairLevel() ;
            structure.setState(Structure.STATE_INTACT, condition) ;
            world.ephemera.addGhost(v, size, buildSprite.scaffolding(), 2.0f) ;
          }
        }) ;
      }
      else {
        d.append(new Description.Link("\n  Begin Salvage") {
          public void whenClicked() {
            final float condition = structure.repairLevel() ;
            structure.setState(Structure.STATE_SALVAGE, condition) ;
            world.ephemera.addGhost(v, size, buildSprite.baseSprite(), 2.0f) ;
          }
        }) ;
      }
      d.append("\n\n") ;
    }
    
    final int numU = structure.numUpgrades(), maxU = structure.maxUpgrades() ;
    if (maxU > 0) {
      final Batch <String> DU = structure.descOngoingUpgrades() ;
      d.append("Upgrade slots ("+numU+"/"+maxU+")") ;
      for (String s : DU) d.append("\n  "+s) ;
      d.append("\n\nUpgrades available: ") ;
      
      final Index <Upgrade> upgrades = allUpgrades() ;
      if (upgrades != null && upgrades.members().length > 0) {
        for (final Upgrade upgrade : upgrades) {
          d.append("\n  ") ;
          d.append(new Description.Link(upgrade.name) {
            public void whenClicked() { lastCU = upgrade ; }
          }) ;
          d.append(" (x"+structure.upgradeLevel(upgrade)+")") ;
        }
        if (lastCU != null) {
          d.append("\n\n") ;
          d.append(lastCU.description) ;
          d.append("\n  Cost: "+lastCU.buildCost+"   ") ;
          if (lastCU.required != null) {
            d.append("\n  Requires: "+lastCU.required.name) ;
          }
          if (structure.upgradePossible(lastCU)) {
            d.append(new Description.Link("\n\n  BEGIN UPGRADE") {
              public void whenClicked() {
                structure.beginUpgrade(lastCU, false) ;
              }
            }) ;
          }
        }
      }
      else d.append("\n  No upgrades.") ;
    }
  }

  
  public void whenClicked() {
    lastCU = null ;
    BaseUI.current().selection.pushSelection(this, false) ;
  }
  
  
  public InfoPanel createPanel(BaseUI UI) {
    return new InfoPanel(UI, this, 0) ;
  }
  
  
  public Target selectionLocksOn() { return this ; }
  
  
  
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
    //if (! structure.intact()) return;
    healthbar.level = structure.repairLevel();
    
    final BaseUI UI = BaseUI.current();
    if (
      UI.selection.selected() != this &&
      UI.selection.hovered()  != this &&
      healthbar.level > 0.5f
    ) return;
    
    final int NU = structure.numUpgrades();
    healthbar.size = (radius() * 50);
    healthbar.size *= 1 + Structure.UPGRADE_HP_BONUSES[NU];
    healthbar.matchTo(buildSprite);
    healthbar.position.z += height() + 0.1f;
    healthbar.readyFor(rendering);
    
    label.matchTo(buildSprite);
    label.position.z += height() - 0.25f;
    label.phrase = this.fullName();
    label.readyFor(rendering);
    label.fontScale = 1.0f;
    
    if (base() == null) healthbar.full = Colour.LIGHT_GREY ;
    else healthbar.full = base().colour ;
    
    if (structure.needsUpgrade()) {
      Healthbar progBar = new Healthbar() ;
      progBar.level = structure.upgradeProgress() ;
      progBar.size = healthbar.size ;
      progBar.position.setTo(healthbar.position) ;
      progBar.yoff = Healthbar.BAR_HEIGHT ;
      progBar.full = Colour.GREY ;
      progBar.empty = Colour.GREY ;
      progBar.colour = Colour.WHITE ; //paler version of main bar colour?
      progBar.readyFor(rendering);
    }
  }
  
  
  protected void toggleStatusFor(Service need, ModelAsset model) {
    if (! structure.intact()) buildSprite.toggleFX(need.model, false);
    final float
      shortage = stocks.shortageOf(need),
      demand = stocks.demandFor(need);
    buildSprite.toggleFX(model, shortage > (demand / 2) + 0.5f);
  }
  
  
  protected void toggleStatusDisplay() {
    final boolean burns = structure.burning();
    buildSprite.toggleFX(BuildingSprite.BLAST_MODEL, burns);
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
    /*
     0,  0,
     1,  0,
     0, -1,
     2,  0,
     0, -2,
     3,  0,
     0, -3,
     //*/
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
      initY = (size / 2f) - 0.5f,
      initX = 0.5f - (size / 2f) ;
    
    int index = -1 ;
    for (Service s : services) if (canShow(s)) index += 2 ;
    if (index < 0) return ;
    index = Visit.clamp(index, offsets.length) ;
    
    for (Service s : services) if (canShow(s)) {
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
    toggleStatusDisplay() ;
    updateItemSprites() ;
    renderHealthbars(rendering, base) ;
    renderChat(rendering, base) ;
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    if (destroyed() || ! inWorld()) return ;
    
    BaseUI.current().selection.renderTileOverlay(
      rendering, this, world,
      hovered ? Colour.transparency(0.5f) : Colour.WHITE,
      Selection.SELECT_OVERLAY
    );
  }
}





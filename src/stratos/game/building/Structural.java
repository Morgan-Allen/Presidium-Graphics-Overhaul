/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.building;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.sfx.*;
import stratos.user.*;
import stratos.util.*;
import stratos.game.wild.Wreckage;  //  TODO:  Move to same package


//  TODO:  Merge this back with Venue?


public abstract class Structural extends Fixture implements
  Installation, TileConstants, Selectable, Schedule.Updates
{
  
  
  /**  Setup and save/load methods-
    */
  //private static boolean verbose = false;
  
  final public Structure structure = new Structure(this);
  protected Base base;
  protected int facing = -1, type = -1;  //  TODO:  Dispense with
  
  protected BuildingSprite buildSprite;
  
  
  public Structural(int size, int high, Base base) {
    super(size, high);
    this.base = base;
  }
  
  
  public Structural(Session s) throws Exception {
    super(s);
    structure.loadState(s);
    this.base = (Base) s.loadObject();
    this.facing = s.loadInt();
    this.type = s.loadInt();
    
    this.buildSprite = (BuildingSprite) sprite();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    structure.saveState(s);
    s.saveObject(base);
    s.saveInt(facing);
    s.saveInt(type);
  }
  
  
  
  /**  Installation methods-
    */
  public int buildCost() { return structure.buildCost(); }
  public Base base() { return base; }
  public Index <Upgrade> allUpgrades() { return null; }
  public Structure structure() { return structure; }
  
  
  public void onCompletion() {
    world.ephemera.addGhost(this, size, buildSprite.scaffolding(), 2.0f);
    setAsEstablished(false);
  }
  
  
  public void onDestruction() {
    Wreckage.reduceToSlag(footprint(), world);
  }
  
  
  public void setAsDestroyed() {
    buildSprite.clearFX();
    super.setAsDestroyed();
  }
  

  public boolean enterWorldAt(int x, int y, Stage world) {
    if (! super.enterWorldAt(x, y, world)) return false;
    //world.presences.togglePresence(this, true);
    world.schedule.scheduleForUpdates(this);
    return true;
  }
  
  
  public void exitWorld() {
    //world.presences.togglePresence(this, false);
    if (base != null) updatePaving(false);
    world.schedule.unschedule(this);
    super.exitWorld();
  }
  
  
  public float scheduledInterval() {
    return 5;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    structure.updateStructure((int) (numUpdates * scheduledInterval()));
    if (base != null) updatePaving(inWorld());
  }
  
  
  protected void updatePaving(boolean inWorld) {
    base.paveRoutes.updatePerimeter(this, inWorld);
  }
  
  
  public void doPlacement() {
    clearSurrounds();
    enterWorld();
    
    if (GameSettings.buildFree) structure.setState(Structure.STATE_INTACT, 1);
    else structure.setState(Structure.STATE_INSTALL, 0);
    
    if (sprite() != null) {
      sprite().colour = null;
      sprite().passType = Sprite.PASS_NORMAL;
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void previewPlacement(boolean canPlace, Rendering rendering) {
    //final Tile at = origin();
    /*
    if (canPlace) BaseUI.current().selection.renderTileOverlay(
      rendering, at.world, canPlace ? Colour.GREEN : Colour.RED,
      Selection.SELECT_OVERLAY, false, this, this
    );
    //*/
    //if (canPlace) renderSelection(rendering, true);
    
    final Sprite sprite = this.buildSprite;
    if (sprite == null) return;
    this.viewPosition(sprite.position);
    sprite.colour = canPlace ? Colour.GREEN : Colour.RED;
    sprite.passType = Sprite.PASS_PREVIEW;
    sprite.readyFor(rendering);
  }
  
  
  public void attachSprite(Sprite sprite) {
    if (sprite == null) super.attachSprite(null);
    else {
      buildSprite = BuildingSprite.fromBase(sprite, size, high);
      super.attachSprite(buildSprite);
    }
  }
  
  
  protected float fogFor(Base base) {
    if (base == this.base) return (1 + super.fogFor(base)) / 2f;
    return super.fogFor(base);
  }
  
  
  public void renderFor(Rendering rendering, Base base) {
    position(buildSprite.position);
    buildSprite.updateCondition(
      structure.repairLevel(),
      structure.intact(),
      structure.burning()
    );
    buildSprite.passType = Sprite.PASS_NORMAL;
    super.renderFor(rendering, base);
    renderHealthbars(rendering, base);
  }
  
  
  protected void renderHealthbars(Rendering rendering, Base base) {
    final boolean focused = BaseUI.isSelectedOrHovered(this);
    final boolean alarm =
      structure.intact() && (base == base() || focused) &&
      (structure.burning() || structure.repairLevel() < 0.25f);
    if ((! focused) && (! alarm)) return;
    
    final int NU = structure.numUpgrades();
    final Healthbar healthbar = new Healthbar();
    healthbar.level = structure.repairLevel();
    healthbar.size = (radius() * 50);
    healthbar.size *= 1 + Structure.UPGRADE_HP_BONUSES[NU];
    healthbar.matchTo(buildSprite);
    healthbar.position.z += height() + 0.1f;
    healthbar.readyFor(rendering);
    
    if (base() == null) healthbar.colour = Colour.LIGHT_GREY;
    else healthbar.colour = base().colour;
    healthbar.alarm = alarm;
    
    final Label label = new Label();
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
  
  
  public String toString() {
    return fullName();
  }
  
  
  public void whenTextClicked() {
    BaseUI.current().selection.pushSelection(this, false);
  }
  
  
  public Target selectionLocksOn() {
    return this;
  }
  
  
  public SelectionInfoPane configPanel(SelectionInfoPane panel, BaseUI UI) {
    return VenueDescription.configSimplePanel(this, panel, UI, "");
  }
  
  
  public TargetOptions configInfo(TargetOptions info, BaseUI UI) {
    if (info == null) info = new TargetOptions(UI, this);
    return info;
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    if (destroyed() || ! inWorld()) return;
    
    final Installation group[];
    if (structure.group() == null) group = new Installation[] {this};
    else group = structure.group();
    
    BaseUI.current().selection.renderTileOverlay(
      rendering, world,
      hovered ? Colour.transparency(0.5f) : Colour.WHITE,
      Selection.SELECT_OVERLAY, true, this, (Object[]) group
    );
  }
}










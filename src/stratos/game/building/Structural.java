


package stratos.game.building;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.sfx.Healthbar;
import stratos.graphics.sfx.Label;
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
    Wreckage.reduceToSlag(area(), world);
  }
  
  
  public void setAsDestroyed() {
    buildSprite.clearFX();
    super.setAsDestroyed();
  }
  

  public boolean enterWorldAt(int x, int y, World world) {
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
    base.paving.updatePerimeter(this, inWorld);
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
    BaseUI.current().selection.renderTileOverlay(
      rendering, world,
      hovered ? Colour.transparency(0.5f) : Colour.WHITE,
      Selection.SELECT_OVERLAY, true, this, this
    );
  }
}







/**  Helper methods for placement-
  */
/*
protected abstract boolean lockToGrid();
protected abstract Structural instance(Base base);
protected abstract void configFromAdjacent(boolean near[], int numNear);

protected List <Structural> installedBetween(Tile start, Tile end) {
  
  //  Basic variables setup and sanity checks-
  if (start == null) return null;
  if (end   == null) end = start;

  final int unit = this.size;
  final World world = start.world;
  int stepX = unit, stepY = unit;
  
  if (lockToGrid()) {
    start = world.tileAt((start.x / unit) * unit, (start.y / unit) * unit);
  }

  //  Choose the best of 2 straight lines leading outward from the origin.
  final Tile
    goesVert  = world.tileAt(end.x, start.y),
    goesHoriz = world.tileAt(start.x, end.y);
  if (Spacing.distance(end, goesVert) < Spacing.distance(end, goesHoriz)) {
    end = goesVert;
    stepY = 0;
    stepX *= (end.x > start.x) ? 1 : -1;
  }
  else {
    end = goesHoriz;
    stepX = 0;
    stepY *= (end.y > start.y) ? 1 : -1;
  }
  final int maxDist = Spacing.maxAxisDist(start, end);
  
  //  Initialise segments at regular intervals along this line.
  final List <Structural> installed = new List <Structural> ();
  int initX = start.x, initY = start.y;
  while (true) {
    final Tile t = world.tileAt(initX, initY);
    if (t == null || Spacing.maxAxisDist(start, t) > maxDist) break;
    final Structural v = instance(base());
    if (v == null) continue;
    v.setPosition(t.x, t.y, world);
    installed.add(v);
    initX += stepX;
    initY += stepY;
  }
  
  //  Then determine their facing/appearance, and return.
  for (Structural s : installed) s.refreshFromNear(installed);
  return installed;
}


private void refreshFromNear(List <Structural> prior) {
  final Tile o = origin();
  if (o == null) return;
  final World world = o.world;
  
  if (prior != null) for (Structural s : prior) s.origin().flagWith(this);
  
  final int unit = this.size;
  final boolean near[] = new boolean[8];
  int numNear = 0;
  
  for (int i : N_ADJACENT) {
    final Tile n = world.tileAt(o.x + (N_X[i] * unit), o.y + (N_Y[i] * unit));
    if (n == null) continue;
    boolean isNear = false;
    if (n.onTop() instanceof Structural) {
      final Structural s = (Structural) n.onTop();
      if (s.origin() == n && s.getClass() == this.getClass()) isNear = true;
    }
    if (n.flaggedWith() == this) isNear = true;
    if (isNear) { numNear++; near[i] = true; }
  }
  
  if (prior != null) for (Structural s : prior) s.origin().flagWith(null);
  configFromAdjacent(near, numNear);
}



/**  Placement interface-
  */
/*
public void placeFromOrigin() {
  final Tile t = origin();
  if (t == null) I.complain("NO ORIGIN!");
  setPosition(t.x, t.y, t.world);
  singlePlacing(null);
}


private List <Structural> toInstall = null;


public void singlePlacing(List <Structural> prior) {
  if (sprite() != null) sprite().colour = null;
  clearSurrounds();
  enterWorld();
  
  if (GameSettings.buildFree) structure.setState(Structure.STATE_INTACT, 1);
  else structure.setState(Structure.STATE_INSTALL, 0);
  if (prior == null) return;
  
  final Tile o = origin();
  final World world = o.world;
  for (int i : N_ADJACENT) {
    final Tile n = world.tileAt(o.x + (N_X[i] * size), o.y + (N_Y[i] * size));
    if (n == null) continue;
    if (n.onTop() != null && n.onTop().getClass() == this.getClass()) {
      final Structural s = (Structural) n.onTop();
      if (prior != null && prior.includes(s)) continue;
      s.refreshFromNear(prior);
    }
  }
}


public void singlePreview(boolean canPlace, Rendering rendering) {
  final Tile at = origin();

  if (canPlace) BaseUI.current().selection.renderTileOverlay(
    rendering, at.world, canPlace ? Colour.GREEN : Colour.RED,
    Selection.SELECT_OVERLAY, false, this, this
  );
  
  final Sprite sprite = this.buildSprite;
  if (sprite == null) return;
  this.viewPosition(sprite.position);
  sprite.colour = canPlace ? Colour.GREEN : Colour.RED;
  sprite.passType = Sprite.PASS_PREVIEW;
  sprite.readyFor(rendering);
}


public boolean singlePointOkay() {
  final Tile at = origin();
  setPosition(at.x, at.y, at.world);
  return canPlace();
}


public boolean canPlace() {
  if (super.canPlace()) return true;
  ///I.say("Couldn't place normally!");
  final Tile o = origin();
  if (o == null || o.onTop() == null) return false;
  if (o.onTop().getClass() == this.getClass()) return true;
  return false;
}


public boolean pointsOkay(Tile from, Tile to) {
  toInstall = installedBetween(from, to);
  ///I.say("TO INSTALL IS: "+toInstall+", between "+from+" and "+to);
  if (toInstall == null) return false;
  for (Structural s : toInstall) {
    if (! s.singlePointOkay()) return false;
  }
  ///I.say("INSTALL OKAY...");
  return true;
}


public void doPlace(Tile from, Tile to) {
  if (toInstall == null) return;
  for (Structural v : toInstall) v.singlePlacing(toInstall);
}


public void preview(
  boolean canPlace, Rendering rendering, Tile from, Tile to
) {
  if (toInstall == null) return;
  for (Structural v : toInstall) v.singlePreview(canPlace, rendering);
}
//*/
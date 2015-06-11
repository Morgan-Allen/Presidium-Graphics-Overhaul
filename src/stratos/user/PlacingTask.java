/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.user;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.KeyInput;
import stratos.util.*;
import com.badlogic.gdx.Input.Keys;



public class PlacingTask implements UITask {
  
  final static int
    MODE_POINT = 0,
    MODE_LINE  = 1,
    MODE_AREA  = 2;
  
  final BaseUI UI;
  final Blueprint placeType;
  final int mode;
  final boolean gridLock;
  
  private Tile begins;
  private Tile endsAt;
  private boolean dragDone = false;
  private Table <Integer, Venue> placeItems = new Table <Integer, Venue> ();
  
  
  PlacingTask(BaseUI UI, Blueprint placeType) {
    this.UI = UI;
    this.placeType = placeType;
    
    if      (placeType.hasProperty(Structure.IS_ZONED )) mode = MODE_AREA;
    else if (placeType.hasProperty(Structure.IS_LINEAR)) mode = MODE_LINE;
    else mode = MODE_POINT;
    gridLock = placeType.hasProperty(Structure.IS_GRIDDED);
  }
  
  
  public void doTask() {
    Tile picked = UI.selection.pickedTile();
    boolean tryPlacement = false;
    
    if (gridLock && picked != null) {
      final int baseSize = placeType.size, hS = baseSize / 2;
      picked = picked.world.tileAt(
        Nums.round(picked.x, baseSize, false) + hS,
        Nums.round(picked.y, baseSize, false) + hS
      );
    }
    
    if (picked != null) {
      if (mode == MODE_POINT) {
        begins = endsAt = picked;
        if (UI.mouseDown()) tryPlacement = true;
      }
      else if (UI.mouseDown()) {
        if (begins == null) begins = picked;
        endsAt = picked;
        dragDone = true;
      }
      else {
        if (dragDone) tryPlacement = true;
        else begins = endsAt = picked;
      }
    }
    
    if (KeyInput.wasTyped(Keys.ENTER)) tryPlacement = true;
    
    if (begins != null && endsAt != null) {
      setupAreaClaim(tryPlacement);
    }
  }
  
  
  private void setupAreaClaim(boolean tryPlacement) {
    final boolean report = false;
    if (report) {
      I.say("\nGetting area claim...");
      I.say("  Start/end points: "+begins+"/"+endsAt);
      I.say("  Place mode: "+mode);
    }
    //
    //  Set up some initial variables-
    final int baseSize = placeType.size;
    final float hS = (baseSize / 2) + 0.5f;
    Box2D area = null;
    final Batch <Coord> placePoints = new Batch <Coord> ();
    //
    //  If there's only one point to consider, just add that.
    if (mode == MODE_POINT || begins == endsAt) {
      placePoints.add(new Coord(begins.x, begins.y));
    }
    //
    //  In the case of line-placement, we create a sequence of place-points
    //  along either the X or Y axis (whichever is stretched furthest.)
    else if (mode == MODE_LINE) {
      int difX = endsAt.x - begins.x, difY = endsAt.y - begins.y;
      boolean lateral = Nums.abs(difX) > Nums.abs(difY);
      int sign  = (lateral ? difX : difY) > 0 ? 1 : -1;
      int limit = Nums.abs(lateral ? difX : difY);
      int x = begins.x, y = begins.y;
      
      for (int i = 0; i <= limit; i += baseSize) {
        placePoints.add(new Coord(x, y));
        if (lateral) x += baseSize * sign;
        else         y += baseSize * sign;
      }
    }
    //
    //  In the case of an area-placement, just grab a rectangle from one corner
    //  to another (but no smaller than the venue size), and place the venue
    //  itself at the centre.
    else if (mode == MODE_AREA) {
      area = new Box2D(begins.x - 0.5f, begins.y - 0.5f, 0, 0);
      area.include(endsAt.x, endsAt.y, 0.5f);
      if (area.xdim() < baseSize) area.xdim(baseSize);
      if (area.ydim() < baseSize) area.ydim(baseSize);
      final Vec2D c = area.centre();
      placePoints.add(new Coord((int) c.x, (int) c.y));
    }
    //
    //  If an area hasn't been specified already, construct one to envelope
    //  all the place-points generated.
    if (area == null) for (Coord c : placePoints) {
      final Box2D foot = new Box2D(c.x - hS, c.y - hS, baseSize, baseSize);
      if (area == null) area = new Box2D(foot);
      else area.include(foot);
    }
    //
    //  Check to see if placement is possible, render the preview, and if
    //  confirmed, initiate construction.
    boolean canPlace = checkPlacingOkay(area, placePoints);
    renderPlacement(area, placePoints, canPlace);
    if (tryPlacement && canPlace) performPlacement(area, placePoints);
  }
  
  
  private Venue placingAt(Coord c, Box2D area, Batch <Coord> placePoints) {
    final Base base = UI.played();
    final Coord points[] = placePoints.toArray(Coord.class);
    final int index = Visit.indexOf(c, points);
    
    Venue p = placeItems.get(index);
    if (p == null) {
      p = placeType.createVenue(base);
      placeItems.put(index, p);
    }
    
    final float hS = (placeType.size / 2) + 0.5f;
    p.setupWith(base.world.tileAt(c.x - hS, c.y - hS), area, points);
    return p;
  }
  
  
  private boolean checkPlacingOkay(Box2D area, Batch <Coord> placePoints) {
    final Account reasons = new Account();    
    boolean canPlace = true;
    
    for (Coord c : placePoints) {
      final Venue p = placingAt(c, area, placePoints);
      if (p == null) { canPlace = false; break; }
      if (KeyInput.wasTyped('e')) p.setFacing(p.facing() + 1);
      if (! p.canPlace(reasons)) { canPlace = false; break; }
    }
    
    final String
      POINT_MESSAGE = "(Enter to place, Esc to cancel, E to change entrance)",
      LINE_MESSAGE  = "(Drag to place line, Esc to cancel, Enter to place)"  ,
      AREA_MESSAGE  = "(Drag to select area, Esc to cancel, Enter to place)" ,
      FAIL_MESSAGE  = "(ILLEGAL PLACEMENT- REASON NOT LOGGED INTERNALLY)";
    String message = null;
    switch (mode) {
      case MODE_POINT : message = POINT_MESSAGE; break;
      case MODE_LINE  : message = LINE_MESSAGE ; break;
      case MODE_AREA  : message = AREA_MESSAGE ; break;
    }
    
    final String failMessage = reasons.failReasons().first();
    if (! canPlace) message = failMessage == null ? FAIL_MESSAGE : failMessage;
    BaseUI.setPopupMessage(message);
    return canPlace;
  }
  
  
  private void performPlacement(Box2D area, Batch <Coord> placePoints) {
    final Batch <Venue> placed = new Batch <Venue> ();
    
    if (I.logEvents()) {
      I.say("\nPLACING "+placeType.name+" IN AREA: "+area);
      I.say("  Placement points are:");
      for (Coord c : placePoints) I.say("    "+c);
    }
    
    for (Coord c : placePoints) {
      final Venue p = placingAt(c, area, placePoints);
      p.doPlacement();
      placed.add(p);
      if (I.logEvents()) I.say("  Facing: "+p.facing());
    }
    UI.endCurrentTask();
  }
  
  
  public void cancelTask() {
    UI.endCurrentTask();
  }
  
  
  
  /**  Rendering/preview and debug methods-
    */
  final static ImageAsset
    FOOTPRINT_TEX = ImageAsset.fromImage(
      PlacingTask.class, "media/GUI/blank_back.png"
    );
  
  private void renderPlacement(
    Box2D area, Batch <Coord> placePoints, boolean canPlace
  ) {
    //
    //  Base venue sprites off their current and projected neighbours!
    final Batch <Object> under = new Batch <Object> ();
    under.add(area);
    
    for (Coord c : placePoints) {
      final Venue p = placingAt(c, area, placePoints);
      if (p != null && p.origin() != null) {
        p.previewPlacement(canPlace, UI.rendering);
        if (p.mainEntrance() != null) under.add(p.mainEntrance());
      }
    }
    
    UI.selection.renderTileOverlay(
      UI.rendering, UI.played().world,
      canPlace ? Colour.SOFT_GREEN : Colour.SOFT_RED,
      FOOTPRINT_TEX, "install_preview", false, under.toArray()
    );
  }
  
  
  public ImageAsset cursorImage() {
    return null;
  }
  
  
  public static boolean isBeingPlaced(Target e) {
    final PlacingTask task = currentPlacement();
    if (task != null) for (Venue v : task.placeItems.values()) {
      if (e == v) return true;
    }
    return false;
  }
  
  
  public static boolean isBeingPlaced(Blueprint b) {
    final PlacingTask task = currentPlacement();
    if (task != null) for (Venue v : task.placeItems.values()) {
      if (v.blueprint == b) return true;
    }
    return false;
  }
  
  
  private static PlacingTask currentPlacement() {
    final BaseUI UI = BaseUI.current();
    if (UI == null || ! (UI.currentTask() instanceof PlacingTask)) return null;
    return (PlacingTask) UI.currentTask();
  }
}










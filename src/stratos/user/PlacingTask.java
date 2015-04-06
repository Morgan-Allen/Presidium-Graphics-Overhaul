

package stratos.user;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.graphics.common.*;
import stratos.util.*;



public class PlacingTask implements UITask {
  
  
  public static enum Mode {
    MODE_POINT, MODE_LINE, MODE_AREA
  };
  
  final BaseUI UI;
  final VenueProfile placeType;
  final Mode placeMode;
  
  private Tile begins;
  private Tile endsAt;
  private boolean dragDone = false;
  private Table <Tile, Venue> placeItems = new Table <Tile, Venue> ();
  
  
  PlacingTask(BaseUI UI, VenueProfile placeType, Mode mode) {
    this.UI = UI;
    this.placeType = placeType;
    this.placeMode = mode;
  }
  
  
  public void doTask() {
    Tile picked = UI.selection.pickedTile();
    boolean tryPlacement = false;
    
    if (picked != null) {
      if (placeMode == Mode.MODE_POINT) {
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
    
    if (begins != null && endsAt != null) {
      setupAreaClaim(tryPlacement);
    }
  }
  
  
  private void setupAreaClaim(boolean tryPlacement) {
    //
    //  Set up some initial variables-
    final int baseSize = placeType.size;
    final float hS = (baseSize / 2) + 0.5f;
    Box2D area = null;
    final Batch <Coord> placePoints = new Batch <Coord> ();
    //
    //  If there's only one point to consider, just add that.
    if (placeMode == Mode.MODE_POINT || begins == endsAt) {
      placePoints.add(new Coord(begins.x, begins.y));
    }
    //
    //  In the case of line-placement, we create a sequence of place-points
    //  along either the X or Y axis (whichever is stretched furthest.)
    else if (placeMode == Mode.MODE_LINE) {
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
    else if (placeMode == Mode.MODE_AREA) {
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
  
  
  private Venue placingAt(Coord c) {
    final Base base = UI.played();
    final Tile t = base.world.tileAt(c.x, c.y);
    Venue p = placeItems.get(t);
    if (p == null) {
      final float hS = (placeType.size / 2) + 0.5f;
      p = placeType.sampleVenue(base);
      p.setPosition(c.x - hS, c.y - hS, base.world);
      placeItems.put(t, p);
    }
    return p;
  }
  
  
  private boolean checkPlacingOkay(Box2D area, Batch <Coord> placePoints) {
    //
    //  TODO:  OBTAIN AN ACCOUNT FOR WHY PLACEMENT ISN'T POSSIBLE, AND DISPLAY
    //         THAT.
    boolean canPlace = true;
    for (Coord c : placePoints) {
      final Venue p = placingAt(c);
      if (p == null || ! p.canPlace()) canPlace = false;
    }
    return canPlace;
  }
  
  
  private void performPlacement(Box2D area, Batch <Coord> placePoints) {
    for (Coord c : placePoints) {
      final Venue p = placingAt(c);
      p.doPlacement();
    }
    UI.endCurrentTask();
  }
  
  
  public void cancelTask() {
    UI.endCurrentTask();
  }
  
  
  
  /**  Rendering/preview methods-
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
    
    for (Coord c : placePoints) {
      final Venue p = placingAt(c);
      if (p != null && p.origin() != null) {
        p.previewPlacement(canPlace, UI.rendering);
      }
    }
    
    UI.selection.renderTileOverlay(
      UI.rendering, UI.played().world,
      canPlace ? Colour.SOFT_GREEN : Colour.SOFT_RED,
      FOOTPRINT_TEX, "install_preview", false, area
    );
  }
  
  
  public ImageAsset cursorImage() {
    return null;
  }
  
}









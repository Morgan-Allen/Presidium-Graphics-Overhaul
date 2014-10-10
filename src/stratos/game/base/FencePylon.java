


package stratos.game.base;
import stratos.game.common.*;
import stratos.game.actors.Actor;
import stratos.game.actors.Background;
import stratos.game.actors.Behaviour;
import stratos.game.building.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



public class FencePylon extends Venue implements Selectable {
  
  
  /**  Construction and save/load methods-
    */
  //  TODO:  Refresh the art used here!
  final public static ModelAsset
    MODEL = CutoutModel.fromImage(
      FencePylon.class, "media/Buildings/ecologist/sensor_array.png", 0.75f, 3
    );
  
  final static ImageAsset
    ICON = ImageAsset.fromImage(
      FencePylon.class, "media/GUI/Buttons/shield_wall_button.gif"
    );
  
  
  public FencePylon(Base base) {
    super(2, 3, ENTRANCE_NONE, base);
    attachModel(MODEL);
  }


  public FencePylon(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Registration, life cycle and economic functions-
    */
  public Behaviour jobFor(Actor actor) {
    return null;
  }
  
  
  public Background[] careers() {
    return null;
  }
  
  
  public TradeType[] services() {
    return null;
  }
  
  
  
  /**  Placement assistance/utility methods (needs to fit within certain
    *  strips at the boundary between sectors.)
    */
  private ShieldWall[] barrierForBorderNear(Tile point, Base base) {
    //
    //  Firstly, determine which sector this point lies within, and the corner
    //  tile of that sector.
    if (point == null) return null;
    final World world = point.world;
    final int SS = world.sections.resolution;
    final Tile corner = world.tileAt(SS * (point.x / SS), SS * (point.y / SS));
    //
    //  Then get the perimeter tiles for the sector in question:
    final Box2D around = new Box2D().set(corner.x, corner.y, SS - 1, SS - 1);
    final Tile perim[] = Spacing.perimeter(around, world);
    final int sideLength = perim.length / 4;
    //
    //  Find which side of the sector the given point is closest to (measured
    //  from their midpoints.)
    float minDist = Float.POSITIVE_INFINITY;
    int sideID = -1;
    for (int n = 0; n < 4; n++) {
      final Tile mid = perim[(int) ((n + 0.5f) * sideLength)];
      if (mid == null) continue;
      final float dist = Spacing.distance(point, mid);
      if (dist < minDist) { minDist = dist; sideID = n; }
    }
    if (sideID == -1) return null;
    //
    //  Then take every second tile on that side, and generate a segment of
    //  shield wall to go with it.
    final Batch <ShieldWall> barrier = new Batch <ShieldWall> ();
    final int
      minIndex = sideID * sideLength,
      maxIndex = minIndex + sideLength;
    for (int n = minIndex; n < maxIndex; n += 2) {
      final Tile under = perim[n];
      final ShieldWall segment = new ShieldWall(base);
      segment.setPosition(under.x, under.y, world);
      barrier.add(segment);
    }
    return barrier.toArray(ShieldWall.class);
  }
  
  
  //  TODO:  Save/load this bit.
  private ShieldWall barrier[] = null;
  
  
  public boolean setPosition(float x, float y, World world) {
    if (! super.setPosition(x, y, world)) return false;
    barrier = barrierForBorderNear(origin(), base);
    if (barrier == null) return false;
    for (ShieldWall segment : barrier) {
      segment.setupSpritesFrom(barrier);
    }
    return true;
  }
  
  
  public boolean canPlace() {
    if (! super.canPlace() || barrier == null) return false;
    for (ShieldWall segment : barrier) {
      if (! segment.canPlace()) return false;
    }
    return true;
  }
  
  
  public void doPlacement() {
    super.doPlacement();
    for (ShieldWall segment : barrier) {
      segment.doPlacement();
    }
  }
  
  
  public void previewPlacement(boolean canPlace, Rendering rendering) {
    super.previewPlacement(canPlace, rendering);
    if (barrier != null) for (ShieldWall segment : barrier) {
      segment.previewPlacement(canPlace, rendering);
    }
  }
  
  /*
  public void renderSelection(Rendering rendering, boolean hovered) {
    if (destroyed() || ! inWorld()) return;
    BaseUI.current().selection.renderTileOverlay(
      rendering, world,
      hovered ? Colour.transparency(0.5f) : Colour.WHITE,
      Selection.SELECT_OVERLAY, true, this, this
    );
  }
  //*/
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Fence Pylon";
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_MILITANT;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "fence_pylon");
  }


  public String helpInfo() {
    return
      "Fence Pylons generate the protective shields neccesary to reinforce "+
      "your perimeter defences.";
  }
  
  /*
  public void renderSelection(Rendering rendering, boolean hovered) {
    if (destroyed() || ! inWorld()) return;
    Selection.renderPlane(
      rendering, position(null), (xdim() / 2f) + 0.5f,
      Colour.transparency(hovered ?  0.25f : 0.5f),
      Selection.SELECT_CIRCLE
    );
  }
  //*/
}







/*
protected static FencePylon locateNewPost(KommandoLodge parent) {
  final World world = parent.world();
  final int numAttempts = 5, bonus = parent.structure.upgradeLevel(
    KommandoLodge.SENSOR_PERIMETER
  );
  
  float range = World.SECTOR_SIZE / 2f, spacing = 10;
  range *= 1 + bonus;
  final FencePylon post = new FencePylon(parent, 2 * (1 + bonus));
  
  Tile picked = null;
  float bestRating = 0;
  for (int n = numAttempts; n-- > 0;) {
    
    final Tile t = Spacing.pickRandomTile(parent, range, world);
    if (t.blocked()) continue;
    
    final FencePylon nearest = (FencePylon) world.presences.nearestMatch(
      FencePylon.class, t, spacing
    );
    if (nearest != null) continue;
    
    post.setPosition(t.x, t.y, world);
    if (! Spacing.perimeterFits(post)) continue;
    
    float rating = 1;
    final Venue nearV = (Venue) world.presences.nearestMatch(
      Venue.class, t, -1
    );
    if (nearV != null) rating += Spacing.distance(t, nearV);
    if (rating > bestRating) { picked = t; bestRating = rating; }
  }
  
  if (picked == null) return null;
  post.setPosition(picked.x, picked.y, world);
  return post;
}


public boolean enterWorldAt(int x, int y, World world) {
  if (! super.enterWorldAt(x, y, world)) return false;
  world.presences.togglePresence(this, origin(), true, FencePylon.class);
  return true;
}


public void exitWorld() {
  world.presences.togglePresence(this, origin(), false, FencePylon.class);
  super.exitWorld();
}


public int owningType() {
  return Element.ELEMENT_OWNS;
}


public int pathType() {
  return Tile.PATH_BLOCKS;
}


public void onGrowth(Tile t) {
  float inc = World.GROWTH_INTERVAL * 1f / World.STANDARD_DAY_LENGTH;
  batteryLife -= Rand.num() * inc;
  if (batteryLife <= 0) setAsDestroyed();
  else parent.base().intelMap.liftFogAround(this, 10);
}
//*/


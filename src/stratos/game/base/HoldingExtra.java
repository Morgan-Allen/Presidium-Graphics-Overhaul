

package stratos.game.base;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.util.*;




/**  'Extras' for a venue serve a couple of functions- primarily decorative,
  *  but also as a mild comfort bonus for the occupants of the holding.
  */
public class HoldingExtra extends Fixture implements TileConstants {
  
  
  final static ModelAsset EXTRA_MODELS[][] = CutoutModel.fromImageGrid(
      Holding.class, Holding.IMG_DIR+"housing_props.png",
      3, 3, 1.03f, 1
    );
  private static boolean verbose = false;
  
  
  final Holding parent;
  private int level = -1;
  
  
  protected HoldingExtra(Holding parent, int level) {
    super(1, 1);
    this.parent = parent;
    updateLevel(level);
  }
  
  
  public HoldingExtra(Session s) throws Exception {
    super(s);
    parent = (Holding) s.loadObject();
    level = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(parent);
    s.saveInt(level);
  }
  
  
  public int owningType() {
    return FIXTURE_OWNS;
  }
  
  
  public int pathType() {
    return Tile.PATH_BLOCKS;
  }
  
  
  public void exitWorld() {
    super.exitWorld();
    parent.base().paveRoutes.updatePerimeter(this, false);
  }
  
  
  protected void updateLevel(int level) {
    if (this.level == level) return;
    final Sprite s = sprite();
    if (s != null) world.ephemera.addGhost(this, 1, s, 2.0f);
    attachModel(EXTRA_MODELS[level][Rand.index(3)]);
    this.level = level;
  }
  
  
  public boolean canPlace() {
    if (! super.canPlace()) return false;
    //if (! Spacing.perimeterFits(this)) return false;
    final Tile o = origin();
    if (Spacing.isEntrance(o)) return false;
    for (Tile n : o.allAdjacent(null)) {
      if (n == null) return false;
      final Element e = n.onTop();
      if (e == null) continue;
      if (e instanceof Holding) continue;
      if (e instanceof HoldingExtra) continue;
      return false;
    }
    return true;
  }
  
  
  public String fullName() {
    return "Accessory";
  }
  
  
  public String toString() {
    return fullName();
  }
  
  
  public String helpInfo() {
    return
      "Your subjects will establish small accessories for their holdings, "+
      "given some time and space.";
  }
  
  
  
  /**  Performing updates to placement and paving-
    */
  protected static void removeExtras(
    Holding holding, List <HoldingExtra> extras
  ) {
    for (HoldingExtra extra : extras) extra.setAsDestroyed();
  }
  
  
  protected static void updateExtras(
    Holding holding, List <HoldingExtra> extras, int numUpdates
  ) {
    if (numUpdates % 10 != 0 || ! holding.structure.intact()) return;
    //if (verbose) I.sayAbout(holding, "Updating extras for: "+holding);
    
    final int level = 1 + (int) Nums.floor(holding.upgradeLevel() / 3f);
    addExtras(holding, extras, level);
    //
    //  Clear out any extras that were demolished, and update the level of any
    //  still standing-
    for (HoldingExtra extra : extras) {
      if (extra.destroyed()) extras.remove(extra);
      else {
        holding.base().paveRoutes.updatePerimeter(extra, true);
        extra.updateLevel(level);
      }
    }
  }
  
  
  private static void addExtras(
    Holding holding, List <HoldingExtra> extras, int level
  ) {
    final int sideSize = holding.size;
    final Stage world = holding.world();
    
    if (holding.upgradeLevel() == HoldingUpgrades.LEVEL_TENT) {
      if (extras.size() > 0) removeExtras(holding, extras);
      return;
    }
    if (extras.size() >= sideSize) return;
    //
    //  If you don't have enough extras yet, see if there's space for some on
    //  one side of the holding-
    final Tile perim[] = Spacing.perimeter(holding.footprint(), world);
    final int n = Rand.index(4) * (sideSize + 1);
    boolean sideClear = true;
    final Batch <Tile> claimed = new Batch <Tile> ();
    final HoldingExtra fits = new HoldingExtra(holding, level);
    
    for (int i = n + sideSize; i-- > n;) {
      final Tile t = perim[i];
      if (t == null) continue;
      fits.setPosition(t.x, t.y, world);
      if (fits.canPlace()) claimed.add(t);
      else sideClear = false;
    }
    
    if (sideClear) for (Tile t : claimed) {
      final HoldingExtra extra = new HoldingExtra(holding, level);
      extra.enterWorldAt(t, world);
      extras.add(extra);
    }
    if (extras.size() >= sideSize) return;
    //
    //  Finally, if you have space left, try a purely random placement:
    final Tile tried = Spacing.pickRandomTile(holding, 3, world);
    if (tried == null || tried.onTop() != null) return;
    for (Tile t : tried.allAdjacent(null)) {
      if (t == null || t.onTop() != null) return;
    }
    fits.enterWorldAt(tried, world);
    extras.add(fits);
  }
}










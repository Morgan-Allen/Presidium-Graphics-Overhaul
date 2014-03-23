

package stratos.game.base;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.util.*;



//  TODO:  This is intended to be a dumping ground for the waste and slag and
//  ejecta from mining operations.  It takes up space, pollutes, can't be built
//  over, and decays slowly, if ever.


//  TODO:  Extend Venue instead, and make private property, so you can get a
//         proper handle on the interface.  Also, make 3x3.
//  TODO:  This HAS to be a venue.  Substantial bugs otherwise.


public class Tailing extends Fixture {
  
  
  
  /**  Data, constructors, and save/load methods-
    */
  final public static float FILL_CAPACITY = 100;
  
  private float fillLevel = 0;
  
  
  public Tailing() {
    super(3, 1);
  }
  
  
  public Tailing(Session s) throws Exception {
    super(s);
    this.fillLevel = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveFloat(fillLevel);
  }
  
  
  
  /**  Placement and initialisation-
    */
  static Tailing siteTailing(final ExcavationSite site) {
    final World world = site.world() ;
    final Tile init = Spacing.pickRandomTile(site.origin(), 4, world) ;
    final Tailing tailing = new Tailing();
    
    final TileSpread spread = new TileSpread(init) {
      protected boolean canAccess(Tile t) {
        if (t.owner() == site) return true ;
        if (t.owningType() >= Element.FIXTURE_OWNS) return false ;
        return true ;
      }
      
      protected boolean canPlaceAt(Tile t) {
        tailing.setPosition(t.x, t.y, world);
        if (tailing.canPlace()) return true;
        return false;
      }
    } ;
    spread.doSearch() ;
    if (spread.success()) {
      //I.say("Total tiles searched: "+spread.allSearched(Tile.class).length) ;
      //tailing.enterWorld();
      return tailing;
    }
    return null ;
  }
  
  
  public boolean enterWorldAt(int x, int y, World world) {
    if (! super.enterWorldAt(x, y, world)) return false;
    attachSprite(updateSprite(null));
    return true;
  }
  
  
  
  /**  Status mutators/accessors-
    */
  public int owningType() {
    if (! inWorld()) return FIXTURE_OWNS;
    return TERRAIN_OWNS;
  }
  
  
  public int pathType() {
    return Tile.PATH_BLOCKS;
  }
  
  
  protected void incFill(float oreAmount) {
    if (oreAmount < 0) I.complain("Can't subtract from tailing.");
    final float inc = oreAmount / FILL_CAPACITY;
    fillLevel = Visit.clamp(fillLevel + inc, 0, 1);
    updateSprite((GroupSprite) sprite());
  }
  
  
  protected float fillLevel() {
    return fillLevel;
  }
  
  
  
  /**  Rendering and interface-
    */
  final static int
    NUM_MOLDS = 9,
    MOLD_COORDS[] = {
      0, 0, 0, 1, 0, 2,
      1, 0, 1, 1, 1, 2,
      2, 0, 2, 1, 2, 2
    };
  
  private GroupSprite updateSprite(GroupSprite sprite) {
    final boolean init = sprite == null;
    if (init) sprite = new GroupSprite();
    final float xo = (size - 1) / -2f, yo = (size - 1) / -2f;
    final Tile o = origin();
    
    final int NML = Smelter.NUM_MOLD_LEVELS;
    final int fillStage = (int) (fillLevel * NUM_MOLDS * NML);
    
    for (int n = NUM_MOLDS; n-- > 0;) {
      final float
        xoff = xo + MOLD_COORDS[n * 2],
        yoff = yo + MOLD_COORDS[(n * 2) + 1];
      final Tile t = o.world.tileAt(o.x + xoff, o.y + yoff);
      final int var = t == null ? 1 : (o.world.worldTerrain().varAt(t) % 2);
      
      final int modelStage = Visit.clamp(fillStage - (n * NML), NML);
      final ModelAsset model = Smelter.SLAG_HEAP_MODELS[var][modelStage];
      
      if (init) sprite.attach(model, xoff, yoff, 0);
      else {
        final CutoutSprite old = (CutoutSprite) sprite.atIndex(n);
        if (old != null && old.model() == model) continue;
        final Sprite ghost = old.model().makeSprite();
        ghost.position.setTo(old.position);
        world().ephemera.addGhost(null, 1, ghost, 2.0f);
        old.setModel((CutoutModel) model);
      }
    }
    return sprite;
  }
}












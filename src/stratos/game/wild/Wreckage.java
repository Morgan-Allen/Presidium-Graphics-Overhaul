


package stratos.game.wild;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.util.*;




public class Wreckage extends Fixture {
  
  
  /**  Construction and save/load routines-
    */
  final public static ModelAsset
    SLAG_MODELS[][] = CutoutModel.fromImageGrid(
      Wreckage.class,
      "media/Buildings/lairs and ruins/all_wreckage.png",
      3, 3, 1, 1, false
    );
  
  final boolean permanent;
  private float spriteSize;
  
  
  private Wreckage(boolean permanent, int size) {
    super(size, size / 2);
    this.permanent = permanent;
    final int tier = size > 1 ? Rand.index(2) : Rand.index(3);
    final ModelAsset model = SLAG_MODELS[Rand.index(3)][tier];
    attachSprite(model.makeSprite());
    spriteSize = (size + Rand.num() + 0.5f) / 2f;
  }
  
  
  public Wreckage(Session s) throws Exception {
    super(s);
    permanent = s.loadBool();
    spriteSize = s.loadFloat();
  }
  

  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveBool(permanent);
    s.saveFloat(spriteSize);
  }
  
  
  public int pathType() {
    return Tile.PATH_HINDERS;
  }


  
  /**  Physical properties, placement and behaviour-
    */
  public static void reduceToSlag(Box2D area, Stage world) {
    final int maxSize = (int) Nums.max(1, area.xdim() / 2);
    
    for (Tile t : world.tilesIn(area, true)) {
      if (t.above() != null) continue;
      int size = 2 + Rand.index(maxSize);
      
      while (size-- > 1) {
        final Wreckage heap = new Wreckage(false, size);
        heap.setPosition(t.x, t.y, world);
        if (! heap.footprint().containedBy(area)) continue;
        if (! heap.canPlace()) continue;
        heap.enterWorldAt(t.x, t.y, world);
        break;
      }
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void renderFor(Rendering rendering, Base base) {
    super.renderFor(rendering, base);
    sprite().position.z -= 0.25f;
    sprite().scale = spriteSize;
  }
}




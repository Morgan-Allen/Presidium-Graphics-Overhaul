/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.wild;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.Composite;
import stratos.graphics.widgets.HUD;
import stratos.util.*;




public class Wreckage extends Fixture {
  
  
  /**  Construction and save/load routines-
    */
  final public static ModelAsset
    SLAG_MODELS[][] = CutoutModel.fromImageGrid(
      Wreckage.class, "wreckage_models",
      "media/Buildings/lairs and ruins/all_wreckage.png",
      3, 3, 1, 1, false
    );
  
  final boolean permanent;
  private float spriteSize;
  
  
  private Wreckage(boolean permanent, int size, int tier) {
    super(size, size / 2);
    this.permanent = permanent;
    if (tier < 0) tier = size > 1 ? Rand.index(2) : Rand.index(3);
    final ModelAsset model = SLAG_MODELS[Rand.index(3)][tier];
    attachSprite(model.makeSprite());
    spriteSize = size + ((Rand.num() - 0.5f) / 2f);
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
  
  
  public float radius() {
    return spriteSize;
  }


  
  /**  Physical properties, placement and behaviour-
    */
  public static void plantCraterAround(Target point, float radius) {
    final Vec3D pos = point.position(null);
    radius += point.radius();
    final Tile origin = point.world().tileAt(pos.x - radius, pos.y - radius);
    final int size = Nums.round(radius * 2, 1, true);
    
    final Wreckage crater = new Wreckage(false, size, 0);
    crater.setPosition(origin.x, origin.y, point.world());
    if (! crater.canPlace()) {
      reduceToSlag(crater.area(null), point.world());
      return;
    }
    crater.enterWorld();
  }
  
  
  public static void reduceToSlag(Box2D area, Stage world) {
    final int maxSize = (int) Nums.clamp(area.xdim() / 2, 1, 2);
    
    for (Tile t : world.tilesIn(area, true)) {
      if (t.above() != null) continue;
      
      for (int size = 1 + Rand.index(maxSize); size > 0; size--) {
        final Wreckage heap = new Wreckage(false, size, -1);
        heap.setPosition(t.x, t.y, world);
        if (! heap.footprint().containedBy(area)) continue;
        if (! heap.canPlace()) continue;
        heap.enterWorldAt(t.x, t.y, world, true);
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
  
  
  public String fullName() {
    return "Wreckage";
  }
  
  
  public Composite portrait(HUD UI) {
    //  TODO:  FILL THIS IN!
    return null;
  }
  
  
  public String helpInfo() {
    return
      "The remains of destroyed structures can linger on the landscape for a "+
      "time...";
  }
}






package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



public class Tailing extends Venue {
  
  
  
  /**  Data, constructors, and save/load methods-
    */
  final public static float FILL_CAPACITY = 100;
  
  final Tailing strip[];
  private float fillLevel = 0;
  
  
  public Tailing(Base base, Tailing strip[]) {
    super(2, 2, ENTRANCE_NONE, base);
    structure.setupStats(200, 10, 0, 0, Structure.TYPE_FIXTURE);
    this.strip = strip;
  }


  public Tailing(Session s) throws Exception {
    super(s);
    this.strip = (Tailing[]) s.loadTargetArray(Tailing.class);
    this.fillLevel = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveTargetArray(strip);
    s.saveFloat(fillLevel);
  }
  
  
  
  
  /**  Placement and initialisation-
    */
  public boolean enterWorldAt(int x, int y, World world) {
    if (! super.enterWorldAt(x, y, world)) return false;
    attachSprite(updateSprite(null));
    return true;
  }
  
  
  protected void incFill(float oreAmount) {
    if (oreAmount < 0) I.complain("Can't subtract from tailing.");
    final float inc = oreAmount / FILL_CAPACITY;
    fillLevel = Visit.clamp(fillLevel + inc, 0, 1);
    updateSprite((GroupSprite) buildSprite().baseSprite());
  }
  
  
  public int owningType() {
    if (! inWorld()) return FIXTURE_OWNS;
    return TERRAIN_OWNS;
  }
  
  
  public int pathType() {
    return Tile.PATH_BLOCKS;
  }
  
  
  public boolean privateProperty() {
    return true;
  }
  
  
  protected float fillLevel() {
    return fillLevel;
  }
  
  
  
  /**  Economic functions-
    */
  public String buildCategory() {
    return UIConstants.TYPE_HIDDEN;
  }
  
  public Background[] careers() { return null; }
  public Service[] services() { return null; }
  public Behaviour jobFor(Actor actor) { return null; }
  
  
  
  /**  Rendering and interface-
    */
  final static int
    NUM_MOLDS = 4,
    MOLD_COORDS[] = {
      1, 0, 1, 1, //0, 2,
      0, 0, 0, 1, //1, 2,
      //2, 0, 2, 1, 2, 2
    };
  private Sprite updateSprite(Sprite oldSprite) {
    if (this == strip[0]) {
      if (oldSprite == null) return Smelter.TAILING_SHAFT_MODEL.makeSprite();
      else return oldSprite;
    }
    
    if (true) {
      if (oldSprite == null) return Smelter.TAILING_ANNEX_MODEL.makeSprite();
      else return oldSprite;
    }
    

    final boolean init = oldSprite == null;
    final GroupSprite sprite = init ?
      new GroupSprite() : (GroupSprite) oldSprite
    ;
      
    final float xo = (size - 1) / -2f, yo = (size - 1) / -2f;
    final Tile o = origin();
    final int NML = Smelter.NUM_MOLD_LEVELS;
    final int fillStage = (int) (fillLevel * NUM_MOLDS * NML);
    
    for (int n = NUM_MOLDS; n-- > 0;) {
      final float
        xoff = xo + MOLD_COORDS[n * 2],
        yoff = yo + MOLD_COORDS[(n * 2) + 1];
      final Tile t = o.world.tileAt(o.x + xoff, o.y + yoff);
      final int var = t == null ? 1 : (o.world.terrain().varAt(t) % 3);
      
      final int modelStage = Visit.clamp(fillStage - (n * NML), NML);
      final ModelAsset model = var == 2 ?
        Smelter.TAILING_SLAB_MODEL :
        Smelter.TAILING_MOLD_MODELS[var][modelStage];
      
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
  
  
  public String fullName() {
    return "Mine Tailings";
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ExcavationSite.ICON, "tailing");
  }
  
  
  public String helpInfo() {
    return "A dumping ground for waste and ejecta from mining operations.";
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    BaseUI.current().selection.renderTileOverlay(
      rendering, world,
      hovered ? Colour.transparency(0.5f) : Colour.WHITE,
      Selection.SELECT_OVERLAY, true, strip[0], strip
    );
  }
}







/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.base;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.game.plans.Mining;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;

import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.building.Economy.*;



public class Smelter extends Venue {
  
  
  final static String IMG_DIR = "media/Buildings/artificer/";
  final static ModelAsset
    DRILLING_MODELS[] = CutoutModel.fromImages(
      Smelter.class, IMG_DIR, 3, 2, false,
      "mantle_drill_1.png",
      "mantle_drill_2.gif",
      "open_shaft.png"
    ),
    OPENING_SHAFT_MODEL = CutoutModel.fromImage(
      Smelter.class, IMG_DIR+"sunk_shaft.gif", 2, 1
    ),
    ALL_MOLD_MODELS[][] = CutoutModel.fromImageGrid(
      Smelter.class, IMG_DIR+"all_molds.png",
      4, 5, 1, 1
    ),
    SMOKE_STACK_MODEL = CutoutModel.fromImage(
      Smelter.class, IMG_DIR+"smoke_stack.png", 1, 1
    ),
    TAILING_SLAB_MODEL = CutoutModel.fromImage(
      Smelter.class, IMG_DIR+"slab.png", 1, 1
    ),
    SMELTER_MOLD_MODELS[][] = {
      ALL_MOLD_MODELS[1],
      ALL_MOLD_MODELS[0]
    },
    TAILING_MOLD_MODELS[][] = {
      ALL_MOLD_MODELS[2],
      ALL_MOLD_MODELS[3]
    };
  
  final public static int SMELT_AMOUNT = 10;
  
  
  //final public ExcavationSite parent;
  protected ExcavationSite parent;
  protected TradeType output;
  private int oldProgress = 0;
  
  
  
  public Smelter(Base base) {
    super(4, 2, ENTRANCE_WEST, base);
    structure.setupStats(75, 6, 150, 0, Structure.TYPE_FIXTURE);
    
    this.assignTo(null, ORES);
    //this.parent = parent;
  }
  
  
  public Smelter(Session s) throws Exception {
    super(s);
    parent = (ExcavationSite) s.loadObject();
    output = (TradeType) s.loadObject();
    oldProgress = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(parent);
    s.saveObject(output);
    s.saveInt(oldProgress);
  }
  
  
  public Smelter assignTo(ExcavationSite belongs, TradeType output) {
    this.parent = belongs;
    this.output = output ;
    updateSprite(0);
    return this;
  }
  
  
  public ExcavationSite belongs() {
    return parent;
  }
  
  
  public int owningType() {
    return FIXTURE_OWNS;
  }
  
  
  public boolean privateProperty() {
    return false;
  }
  
  
  
  /**  Behaviour implementation.
    */
  public boolean enterWorldAt(int x, int y, World world) {
    if (! super.enterWorldAt(x, y, world)) return false;
    return true;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates);
    if (! structure.intact()) return;
    //
    //  Vary pollution based on structural upgrades-
    //final Structure s = parent.structure;
    int pollution = 6;
    //pollution -= s.upgradeLevel(ExcavationSite.SAFETY_PROTOCOL);
    //pollution += s.upgradeLevel(ExcavationSite.MANTLE_DRILLING) * 2;
    if (! isWorking()) pollution /= 2;
    structure.setAmbienceVal(0 - pollution);
  }
  

  public TradeType[] services() {
    return null;
  }
  
  
  public Background[] careers() {
    return null;
  }
  

  public Behaviour jobFor(Actor actor) {
    return null;
  }
  

  private boolean isWorking() {
    for (Actor a : personnel.visitors()) {
      if (a.isDoingAction("actionSmelt", null)) return true;
    }
    return false;
  }
  
  
  
  /**  Finding new sites-
    */
  public boolean canPlace() {
    if (! super.canPlace()) return false;
    for (Tile t : Spacing.perimeter(footprint(), origin().world)) if (t != null) {
      if (t.owningType() >= this.owningType()) return false;
    }
    return true;
  }
  
  
  static Smelter siteSmelter(
    final ExcavationSite site, final TradeType mined
  ) {
    final World world = site.world();
    final Tile init = Spacing.pickRandomTile(site.origin(), 4, world);
    final Smelter smelter = new Smelter(site.base()).assignTo(site, mined);
    
    final TileSpread spread = new TileSpread(init) {
      protected boolean canAccess(Tile t) {
        if (t.onTop() == site) return true;
        if (t.owningType() >= Element.FIXTURE_OWNS) return false;
        return true;
      }
      
      protected boolean canPlaceAt(Tile t) {
        smelter.setPosition(t.x, t.y, world);
        return smelter.canPlace();
      }
    };
    spread.doSearch();
    if (spread.success()) {
      smelter.doPlacement();
      return smelter;
    }
    return null;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  private int spriteVariant() {
    return Visit.indexOf(output, Mining.MINED_TYPES);
  }
  
  //  TODO:  This looks right, but I need some way to fill in the gap along
  //  the edge.
  final private static int
    MOLD_COORDS[] = {
      0, 3,
      1, 3,
      2, 3,
      3, 3,
      3, 2,
      3, 1,
      3, 0
    },
    NUM_MOLDS = MOLD_COORDS.length / 2,
    NUM_MOLD_LEVELS = 5;
  
  
  private void updateSprite(int progress) {
    //  TODO:  Consider a simple teardown-and-replace scheme instead?
    
    final boolean inWorld = inWorld() && sprite() != null;
    //
    //  Otherwise, put together a group sprite-
    final float xo = (size - 1) / -2f, yo = (size - 1) / -2f;
    final GroupSprite s;
    if (inWorld) {
      s = (GroupSprite) buildSprite.baseSprite();
    }
    else {
      s = new GroupSprite();
      s.attach(DRILLING_MODELS[0], -0.5f, -0.5f, 0);
      attachSprite(s);
    }
    //
    //  And attach mold sprites at the right intervals-
    final int fillThresh = progress / NUM_MOLD_LEVELS;
    for (int i = 0, c = 0; i < NUM_MOLDS; i++) {
      int moldLevel = 0;
      if (i < fillThresh) moldLevel = NUM_MOLD_LEVELS - 1;
      else if (i < fillThresh + 1) moldLevel = progress % NUM_MOLD_LEVELS;
      
      ModelAsset model = SMELTER_MOLD_MODELS[spriteVariant()][moldLevel];
      if (i == 0) model = SMOKE_STACK_MODEL;
      
      if (inWorld) {
        final CutoutSprite old = (CutoutSprite) s.atIndex(i + 1);
        if (old != null && old.model() == model) continue;
        final Sprite ghost = old.model().makeSprite();
        ghost.position.setTo(old.position);
        world().ephemera.addGhost(null, 1, ghost, 2.0f);
        old.setModel((CutoutModel) model);
      }
      else s.attach(model,
        MOLD_COORDS[c++] + xo,
        MOLD_COORDS[c++] + yo,
      0);
    }
  }
  
  
  public void renderFor(Rendering rendering, Base base) {
    int progress = NUM_MOLDS * NUM_MOLD_LEVELS;
    progress *= stocks.amountOf(output) / SMELT_AMOUNT;
    if (progress != oldProgress) updateSprite(progress);
    oldProgress = progress;
    super.renderFor(rendering, base);
  }
  
  
  protected TradeType[] goodsToShow() {
    return new TradeType[] { output };
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ExcavationSite.ICON, "smelter");
  }
  
  
  public String fullName() {
    return output.name+" Smelter";
  }
  
  
  public String helpInfo() {
    return
      output.name+" Smelters extract larger quantities of "+output.name+
      " from subterranean mineral deposits.";
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_HIDDEN;
  }
}






/*

package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.maps.Species;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



public class Tailing extends Structural {
  
  
  /**  Data, constructors, and save/load methods-
  final public static float FILL_CAPACITY = 100;
  
  final Tailing strip[];
  private float fillLevel = 0;
  
  
  public Tailing(Base base, Tailing strip[]) {
    super(2, 2, ENTRANCE_NONE, base);
    structure.setupStats(
      10,  //integrity
      10,  //armour
      20,  //build cost
      0,  //max upgrades
      Structure.TYPE_FIXTURE
    );
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
  public boolean enterWorldAt(int x, int y, World world) {
    if (! super.enterWorldAt(x, y, world)) return false;
    attachSprite(updateSprite(null));
    return true;
  }
  
  
  public void incFill(float oreAmount) {
    if (oreAmount < 0) I.complain("Can't subtract from tailing.");
    final float inc = oreAmount / FILL_CAPACITY;
    fillLevel = Visit.clamp(fillLevel + inc, 0, 1);
    updateSprite((GroupSprite) buildSprite.baseSprite());
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
  
  
  protected void updatePaving(boolean inWorld) {
    if (this == strip[0]) super.updatePaving(inWorld);
  }
  
  
  
  /**  Economic functions-
  public String buildCategory() {
    return UIConstants.TYPE_HIDDEN;
  }
  
  public Background[] careers() { return null; }
  public Service[] services() { return null; }
  public Behaviour jobFor(Actor actor) { return null; }
  
  
  
  /**  Rendering and interface-
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

    final boolean init = oldSprite == null;
    final GroupSprite sprite = init ?
      new GroupSprite() : (GroupSprite) oldSprite
   ;
    if (init) sprite.attach(Smelter.TAILING_ANNEX_MODEL, 0, 0, 0);
      
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
        final CutoutSprite old = (CutoutSprite) sprite.atIndex(n + 1);
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
  
  
  public SelectionInfoPane configPanel(SelectionInfoPane panel, BaseUI UI) {
    final StringBuffer d = new StringBuffer();
    d.append("% filled: "+((int) (fillLevel * 100)));
    panel = VenueDescription.configSimplePanel(this, panel, UI, d.toString());
    return panel;
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


//*/






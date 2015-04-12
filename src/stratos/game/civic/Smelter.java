/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.civic;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.game.actors.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.economic.Economy.*;



public class Smelter extends Venue {
  
  
  final static String IMG_DIR = "media/Buildings/artificer/";
  final public static ModelAsset
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
      4, 5, 1, 1, true
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
  
  final static VenueProfile PROFILE = new VenueProfile(
    Smelter.class, "smelter", "Smelter",
    4, 2, IS_NORMAL,
    ExcavationSite.PROFILE, Owner.TIER_FACILITY
  );
  
  protected ExcavationSite parent;
  protected Traded output;
  private int oldProgress = 0;
  
  
  
  public Smelter(Base base) {
    super(PROFILE, base);
    structure.setupStats(75, 6, 150, 0, Structure.TYPE_FIXTURE);
    this.assignTo(null, ORES);
  }
  
  
  public Smelter(Session s) throws Exception {
    super(s);
    parent = (ExcavationSite) s.loadObject();
    output = (Traded) s.loadObject();
    oldProgress = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(parent);
    s.saveObject(output);
    s.saveInt(oldProgress);
  }
  
  
  public Smelter assignTo(ExcavationSite belongs, Traded output) {
    this.parent = belongs;
    this.output = output ;
    updateSprite(0);
    return this;
  }
  
  
  public ExcavationSite belongs() {
    return parent;
  }
  
  
  
  /**  Behaviour implementation.
    */
  public boolean enterWorldAt(int x, int y, Stage world) {
    if (! super.enterWorldAt(x, y, world)) return false;
    return true;
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
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
  

  public Traded[] services() {
    return null;
  }
  
  
  public Background[] careers() {
    return null;
  }
  

  public Behaviour jobFor(Actor actor, boolean onShift) {
    return null;
  }
  

  private boolean isWorking() {
    for (Actor a : staff.visitors()) {
      if (a.isDoingAction("actionSmelt", null)) return true;
    }
    return false;
  }
  
  
  
  /**  Finding new sites-
    */
  public boolean canPlace() {
    if (! super.canPlace()) return false;
    for (Tile t : Spacing.perimeter(footprint(), origin().world)) {
      if (t != null && t.reserved()) return false;
    }
    return true;
  }
  
  
  public float ratePlacing(Target point, boolean exact) {
    //  TODO:  Not being used for the moment...
    return -1;
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
  
  
  protected Traded[] goodsToShow() {
    return new Traded[] { output };
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
  
  
  public String objectCategory() {
    return InstallationPane.TYPE_HIDDEN;
  }
}





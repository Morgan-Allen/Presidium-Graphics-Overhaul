/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.base ;
import stratos.game.common.* ;
import stratos.game.maps.*;
import stratos.game.actors.* ;
import stratos.game.building.* ;
import stratos.graphics.common.* ;
import stratos.graphics.cutout.* ;
import stratos.graphics.widgets.* ;
import stratos.user.* ;
import stratos.util.* ;


//  TODO:  I'm going to completely cut this out for the moment.  More of a
//  complication than I need right now.




public class Smelter extends Venue implements Economy {
  
  
  final static String IMG_DIR = "media/Buildings/artificer/" ;
  final static ModelAsset
    DRILLING_MODELS[] = CutoutModel.fromImages(
      IMG_DIR, Smelter.class, 2, 2, false,
      "metals_smelter.gif",
      "isotopes_smelter.gif",
      "sunk_shaft.gif"
    ),
    TAILING_SHAFT_MODEL = DRILLING_MODELS[2],
    TAILING_ANNEX_MODEL = CutoutModel.fromSplatImage(
      Smelter.class, IMG_DIR+"tailing_annex.png", 2
    ),
    TAILING_SLAB_MODEL = CutoutModel.fromImage(
      Smelter.class, IMG_DIR+"slab.png", 1, 1
    ),
    SMELTER_STACK_MODEL = CutoutModel.fromImage(
      Smelter.class, IMG_DIR+"drain_cover.png", 1, 1
    ),
    ALL_MOLD_MODELS[][] = CutoutModel.fromImageGrid(
      Smelter.class, IMG_DIR+"all_molds.png",
      4, 5, 1, 1
    ),
    SMELTER_MOLD_MODELS[][] = {
      ALL_MOLD_MODELS[1],
      ALL_MOLD_MODELS[0]
    },
    TAILING_MOLD_MODELS[][] = {
      ALL_MOLD_MODELS[2],
      ALL_MOLD_MODELS[3]
    };
  
  final static int
    MOLD_COORDS[] = {
      0, 0,
      0, 1,
      0, 2,
      1, 2,
      2, 2
    },
    NUM_MOLDS = MOLD_COORDS.length / 2,
    NUM_MOLD_LEVELS = 5 ;
  
  final static int SMELT_AMOUNT = 10;
  
  
  final ExcavationSite parent ;
  final Service output ;
  //final Smelter2 strip[] ;
  
  private int oldProgress = 0 ;
  
  
  
  public Smelter(
    ExcavationSite parent, Service mined
  ) {
    super(3, 2, ENTRANCE_WEST, parent.base());
    structure.setupStats(75, 6, 150, 0, Structure.TYPE_FIXTURE);
    this.parent = parent ;
    this.output = mined ;
    //this.strip = strip ;
    //if (variant == 0) updateSprite() ;
    //else attachModel(SHAFT_MODELS[variant - 1]) ;
  }
  
  
  public Smelter(Session s) throws Exception {
    super(s) ;
    parent = (ExcavationSite) s.loadObject() ;
    output = (Service) s.loadObject() ;
    //strip = (Smelter2[]) s.loadTargetArray(Smelter2.class) ;
    oldProgress = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(parent) ;
    s.saveObject(output) ;
    //s.saveTargetArray(strip) ;
    s.saveInt(oldProgress) ;
  }
  
  
  public int owningType() {
    return FIXTURE_OWNS ;
  }
  
  
  public boolean privateProperty() {
    return true ;
  }
  
  
  
  /**  Behaviour implementation.
    */
  public boolean enterWorldAt(int x, int y, World world) {
    if (! super.enterWorldAt(x, y, world)) return false;
    updateSprite(0);
    return true;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! structure.intact()) return ;
    //
    //  Vary pollution based on structural upgrades-
    final Structure s = parent.structure ;
    int pollution = 6 ;
    pollution -= s.upgradeLevel(ExcavationSite.SAFETY_PROTOCOL) ;
    pollution += s.upgradeLevel(ExcavationSite.MANTLE_DRILLING) * 2 ;
    if (! isWorking()) pollution /= 2 ;
    structure.setAmbienceVal(0 - pollution) ;
  }
  

  public Service[] services() {
    return null ;
  }
  
  
  public Background[] careers() {
    return null ;
  }
  

  public Behaviour jobFor(Actor actor) {
    return null ;
  }
  

  private boolean isWorking() {
    for (Actor a : personnel.visitors()) {
      if (a.isDoingAction("actionSmelt", null)) return true ;
    }
    return false ;
  }
  
  
  
  /**  Finding new sites-
    */
  public boolean canPlace() {
    if (! super.canPlace()) return false ;
    for (Tile t : Spacing.perimeter(area(), origin().world)) if (t != null) {
      if (t.owningType() >= this.owningType()) return false ;
    }
    return true ;
  }
  
  
  static Smelter siteSmelter(
    final ExcavationSite site, final Service mined
  ) {
    final World world = site.world() ;
    final Tile init = Spacing.pickRandomTile(site.origin(), 4, world) ;
    final Smelter smelter = new Smelter(site, mined);
    
    final TileSpread spread = new TileSpread(init) {
      protected boolean canAccess(Tile t) {
        if (t.owner() == site) return true ;
        if (t.owningType() >= Element.FIXTURE_OWNS) return false ;
        return true ;
      }
      
      protected boolean canPlaceAt(Tile t) {
        smelter.setPosition(t.x, t.y, world);
        return smelter.canPlace();
      }
    } ;
    spread.doSearch() ;
    if (spread.success()) {
      smelter.placeFromOrigin();
      return smelter ;
    }
    return null ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  private int spriteVariant() {
    return Visit.indexOf(output, Mining.MINED_TYPES) ;
  }
  
  
  private void updateSprite(int progress) {
    final boolean inWorld = inWorld() && sprite() != null;
    //
    //  Otherwise, put together a group sprite-
    final float xo = (size - 1) / -2f, yo = (size - 1) / -2f ;
    final GroupSprite s ;
    if (inWorld) {
      s = (GroupSprite) buildSprite().baseSprite() ;
    }
    else {
      s = new GroupSprite() ;
      s.attach(DRILLING_MODELS[spriteVariant()], 1.5f + xo, 0.5f + yo, 0) ;
      attachSprite(s) ;
    }
    //
    //  And attach mold sprites at the right intervals-
    final int fillThresh = progress / NUM_MOLD_LEVELS;
    for (int i = 0, c = 0 ; i < NUM_MOLDS ; i++) {
      int moldLevel = 0;
      if (i < fillThresh) moldLevel = NUM_MOLD_LEVELS - 1;
      else if (i < fillThresh + 1) moldLevel = progress % NUM_MOLD_LEVELS;
      ModelAsset model = SMELTER_MOLD_MODELS[spriteVariant()][moldLevel];
      if (i == NUM_MOLDS - 4) model = SMELTER_STACK_MODEL;
      
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
      0) ;
    }
  }
  
  
  public void renderFor(Rendering rendering, Base base) {
    int progress = NUM_MOLDS * NUM_MOLD_LEVELS ;
    progress *= stocks.amountOf(output) / SMELT_AMOUNT ;
    if (progress != oldProgress) updateSprite(progress) ;
    oldProgress = progress ;
    super.renderFor(rendering, base) ;
  }
  
  
  protected Service[] goodsToShow() {
    return new Service[] { output } ;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ExcavationSite.ICON, "smelter");
  }
  
  
  public String fullName() {
    return output.name+" Smelter" ;
  }
  
  
  public String helpInfo() {
    return
      output.name+" Smelters extract larger quantities of "+output.name+
      " from subterranean mineral deposits." ;
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_ARTIFICER ;
  }
}



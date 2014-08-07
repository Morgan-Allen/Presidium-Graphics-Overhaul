

package stratos.game.base;
//import javax.net.ssl.SSLEngineResult.Status;

import stratos.game.common.*;
import stratos.game.maps.*;
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



//  TODO:  Just use this as the Nursery.  Same way that the Tailing can just
//  be used as an entry/exit point for excavations.


public class Plantation extends Venue implements TileConstants {
  
  
  /**  Constructors, data fields, setup and save/load methods-
    */
  final static String IMG_DIR = "media/Buildings/ecologist/";
  final static ModelAsset
    NURSERY_MODEL = CutoutModel.fromImage(
      Plantation.class, IMG_DIR+"nursery.png", 2, 2
    );
  
  final public static int
    TYPE_NURSERY = 0;
  final public static float
    MATURE_DURATION = World.STANDARD_DAY_LENGTH * 5,
    GROW_INCREMENT  = World.GROWTH_INTERVAL / MATURE_DURATION,
    
    MAX_HEALTH_BONUS     = 2.0f,
    INFEST_GROW_PENALTY  = 0.5f,
    POLLUTE_GROW_PENALTY = 0.5f,
    UPGRADE_GROW_BONUS   = 0.25f,
    
    CEREAL_BONUS = 2.00f,
    DRYLAND_MULT = 0.75f,
    WETLAND_MULT = 1.25f,
    
    NURSERY_CARBS   = 1,
    NURSERY_GREENS  = 0.5f,
    NURSERY_PROTEIN = 0.5f;
  final static Crop[]
    NO_CROPS = new Crop[0];
  
  
  final public EcologistStation belongs;
  
  final public int type;
  private Crop planted[] = null;
  private float needsTending = 0;
  
  
  public Plantation(
    EcologistStation belongs, int type//, int facing//, Plantation strip[]
  ) {
    super(2, 2, ENTRANCE_SOUTH, belongs.base());
    this.attachModel(NURSERY_MODEL);
    
    final boolean IN = type == TYPE_NURSERY;
    structure.setupStats(
      IN ? 25 : 5,  //integrity
      5,  //armour
      IN ? 15 : 2,  //build cost
      0,  //max upgrades
      Structure.TYPE_FIXTURE
    );
    this.belongs = belongs;
    this.type = type;
  }
  

  public Plantation(Session s) throws Exception {
    super(s);
    belongs = (EcologistStation) s.loadObject();
    type = s.loadInt();
    planted = (Crop[]) s.loadObjectArray(Crop.class);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(belongs);
    s.saveInt(type);
    s.saveObjectArray(planted);
  }
  
  
  public int owningType() {
    return FIXTURE_OWNS;
  }
  
  
  public boolean privateProperty() {
    return true;
  }
  
  
  public int pathType() {
    return Tile.PATH_BLOCKS;
    //if (type == TYPE_NURSERY) return Tile.PATH_BLOCKS;
    //if (type == TYPE_COVERED) return Tile.PATH_BLOCKS;
    //return Tile.PATH_HINDERS;
  }
  
  
  protected void updatePaving(boolean inWorld) {
    if (type == TYPE_NURSERY) super.updatePaving(inWorld);
  }
  
  
  
  /**  Establishing crop areas-
    */
  /*
  final static int
    STRIP_DIRS[]  = { N, E, S, W },
    CROPS_POS[] = { 0, 0, 0, 1, 1, 0, 1, 1 };
  
  
  public boolean enterWorldAt(int x, int y, World world) {
    if (! super.enterWorldAt(x, y, world)) return false;
    if (type == TYPE_NURSERY) {
      attachModel(NURSERY_MODEL);
    }
    else {
      for (int c = 0, i = 0; c < 4; c++) {
        final Tile t = world.tileAt(x + CROPS_POS[i++], y + CROPS_POS[i++]);
        ///I.say("Creating new bed, species: "+Species.CROP_SPECIES[0]);
        planted[c] = new Crop(
          this, Species.CROP_SPECIES[0]
        );
        planted[c].setPosition(t.x, t.y, world);
      }
      //refreshCropSprites();
    }
    return true;
  }
  //*/
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates);
    if (! structure.intact()) return;

    if (! belongs.structure.intact()) {
      structure.setState(Structure.STATE_SALVAGE, -1);
      return;
    }
    structure.setAmbienceVal(2);
    
    if (type == TYPE_NURSERY && numUpdates % 10 == 0) {
      final float
        growth = 10 * 1f / MATURE_DURATION,
        decay = growth / 10;
      for (Item seed : stocks.matches(SAMPLES)) {
        stocks.removeItem(Item.withAmount(seed, decay));
      }
      stocks.bumpItem(CARBS  , growth * NURSERY_CARBS  );
      stocks.bumpItem(GREENS , growth * NURSERY_GREENS );
      stocks.bumpItem(PROTEIN, growth * NURSERY_PROTEIN);
    }
  }
  
  
  public void onGrowth(Tile t) {
    if (! structure.intact()) return;
    
    //  TODO:  Just update growth for any plants inside the nursery.
    //
    //  Here, we average fertility over the plantation as a whole-
    
    //  Then apply growth to each crop-
    /*
    boolean anyChange = false;
    for (Crop c : planted) if (c != null && c.origin() == t) {
      final int oldGrowth = (int) c.growStage();
      c.onGrowth(t);
      final int newGrowth = (int) c.growStage();
      if (oldGrowth != newGrowth) anyChange = true;
      
    }
    //*/
    checkCropStates();
    //if (anyChange) refreshCropSprites();
  }
  
  
  protected void checkCropStates() {
    needsTending = 0;
    for (Crop c : planted) if (c != null && c.needsTending()) needsTending++;
    needsTending /= planted.length;
  }
  
  
  public Crop[] planted() {
    if (planted == null) return NO_CROPS;
    return planted;
  }
  
  
  public Crop plantedAt(Tile t) {
    for (Crop c : planted) if (c != null && c.origin() == t) return c;
    return null;
  }
  
  
  public float needForTending() {
    return needsTending;
  }
  
  
  
  //  TODO:  Consider changing these to employ 1 or 2 cultivators each.
  public TradeType[] services() { return null; }
  
  public Background[] careers() { return null; }
  
  public Behaviour jobFor(Actor actor) { return null; }
  
  
  
  /**  Finding space.
    */
  protected boolean canTouch(Element e) {
    return e.owningType() < this.owningType();
  }
  
  
  //  TODO:  I want a more cohesive method for deciding on the placement of
  //  spontaneous structures.  ...I think, on balance, that the growth method
  //  would not be the worst of systems.  Get supply and demand, and find the
  //  best unclaimed area.
  
  
  public static Plantation placeAllotmentFor(EcologistStation parent) {
    
    
    return null;
  }
  
  
  public static float rateAllotment(Plantation alloted, World world) {
    return 0;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(EcologistStation.ICON, "plantation");
  }
  
  
  public String fullName() {
    return type == TYPE_NURSERY ? "Nursery" : "Plantation";
  }
  
  
  public String helpInfo() {
    if (type == TYPE_NURSERY) return
      "Nurseries allow young plants to be cultivated in a secure environment "+
      "prior to outdoor planting, and provide a small but steady food yield "+
      "regardless of outside conditions.";
    return
      "Plantations of managed, mixed-culture cropland secure a high-quality "+
      "food source for your base, but require space and constant attention.";
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_ECOLOGIST;
  }
  
  
  public SelectionInfoPane configPanel(SelectionInfoPane panel, BaseUI UI) {
    final StringBuffer d = new StringBuffer();
    if (type == TYPE_NURSERY) {
      d.append("\n");
      boolean any = false;
      for (Item seed : stocks.matches(SAMPLES)) {
        final Species s = (Species) seed.refers;
        d.append("\n  Seed for "+s+" (");
        d.append(Crop.HEALTH_NAMES[(int) seed.quality]+" quality)");
        any = true;
      }
      if (! any) d.append("\nNo seed stock.");
    }
    else {
      d.append("\n");
      for (Crop c : planted) {
        d.append("\n  "+c);
      }
    }
    panel = VenueDescription.configSimplePanel(this, panel, UI, d.toString());
    return panel;
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    BaseUI.current().selection.renderTileOverlay(
      rendering, world,
      hovered ? Colour.transparency(0.5f) : Colour.WHITE,
      Selection.SELECT_OVERLAY, true, this
    );
  }
}





//  TODO:  Outsource these to the Placement class.
/*
static Plantation[] placeAllotment(
  final EcologistStation parent, final int minSize, boolean covered
) {
  final World world = parent.world();
  Plantation strip[] = new Plantation[4];
  
  for (int n = 4; n-- > 0;) {
    final Plantation p = strip[n] = new Plantation(
      parent, n == 0 ? TYPE_NURSERY : (covered ? TYPE_COVERED : TYPE_BED),
      0, strip
    );
  }
  
  if (Placement.findClearanceFor(strip, parent, world)) {
    final int dir = Placement.directionOf(strip);
    for (int n = 4; n-- > 0;) {
      final Tile o = strip[n].origin();
      final Plantation p = strip[n] = new Plantation(
        parent, n == 0 ? TYPE_NURSERY : (covered ? TYPE_COVERED : TYPE_BED),
        dir, strip
      );
      p.setPosition(o.x, o.y, world);
      p.placeFromOrigin();
    }
    return strip;
  }
  return null;
}


//  TODO:  INCORPORATE THIS INTO THE PLACEMENT ALGORITHM
static float rateArea(Plantation allots[], World world) {
  //
  //  Favour fertile, unpaved areas close to the parent botanical station but
  //  farther from other structures-
  float
    fertility = 0, num = 0,
    minDist = World.SECTOR_SIZE, parentDist = 0;
  for (Plantation p : allots) {
    parentDist += Spacing.distance(p, p.belongs);
    Target close = world.presences.nearestMatch(Venue.class, p, minDist);
    if (
      close != null && close != p.belongs &&
      ! (close instanceof Plantation)
    ) {
      minDist = Spacing.distance(p, close);
    }
    for (Tile t : world.tilesIn(p.area(), false)) {
      fertility += t.habitat().moisture();
      if (t.pathType() == Tile.PATH_ROAD) fertility /= 2;
      num++;
    }
  }
  float rating = fertility / num;
  rating *= 1 - (parentDist / (allots.length * World.SECTOR_SIZE));
  rating *= minDist / World.SECTOR_SIZE;
  return rating;
}
//*/

/*
private static boolean tryPlacementAt(
  Tile t, BotanicalStation parent, Plantation allots[],
  int dir, boolean covered
) {
  for (int i = 0; i < allots.length; i++) try {
    final Plantation p = allots[i] = new Plantation(
      parent, i == 0 ? TYPE_NURSERY : (covered ? TYPE_COVERED : TYPE_BED),
      dir, allots
    );
    p.setPosition(
      t.x + (N_X[dir] * 2 * i),
      t.y + (N_Y[dir] * 2 * i),
      t.world
    );
    if (! p.canPlace()) return false;
  } catch (Exception e) { return false; }
  return true;
}
//*/


/*
Plantation bestSite[] = null;
float bestRating = 0;

for (int m = 10; m-- > 0;) {
  final Tile t = Spacing.pickRandomTile(parent, 12, world);
  if (t == null) continue;
  final int off = Rand.index(4);
  for (int n = 4; n-- > 0;) {
    final Plantation allots[] = new Plantation[minSize];
    final int i = (n + off) % 4;
    if (tryPlacementAt(t, parent, allots, STRIP_DIRS[i], covered)) {
      final float rating = rateArea(allots, world);
      if (rating > bestRating) { bestSite = allots; bestRating = rating; }
    }
  }
}
if (bestSite != null) {
  for (Plantation p : bestSite) p.placeFromOrigin();
  return bestSite;
}
return null;
//*/



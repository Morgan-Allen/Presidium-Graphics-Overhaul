


package stratos.game.civic;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import stratos.game.base.BaseTransport;
import static stratos.game.economic.Economy.*;



public class SolarBank extends Venue {
  
  
  final static String
    IMG_DIR = "media/Buildings/ecologist/";
  final public static ModelAsset
    BANK_MODELS[][] = CutoutModel.fromImageGrid(
      SolarBank.class, IMG_DIR+"all_solar_banks.png", 2, 3, 2, 2, false
    ),
    
    MODEL_X_SEGMENT = BANK_MODELS[1][1],
    MODEL_Y_SEGMENT = BANK_MODELS[0][1],
    MODEL_X_HUB     = BANK_MODELS[1][0],
    MODEL_Y_HUB     = BANK_MODELS[0][0],
    MODEL_X_TRAP    = BANK_MODELS[1][2],
    MODEL_Y_TRAP    = BANK_MODELS[0][2];
  
  final static ImageAsset ICON = ImageAsset.fromImage(
    SolarBank.class, "media/GUI/Buttons/solar_array_button.gif"
  );
  
  
  final static VenueProfile PROFILE = new VenueProfile(
    SolarBank.class, "solar_bank", "Solar Bank",
    2, 2, IS_LINEAR | IS_FIXTURE,
    EcologistStation.PROFILE, Owner.TIER_FACILITY
  );
  
  
  public SolarBank(Base base) {
    super(PROFILE, base);
    structure.setupStats(10, 5, 40, 0, Structure.TYPE_FIXTURE);
  }
  
  
  public SolarBank(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Updates and life-cycle:
    */
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) {
      structure.assignOutputs();
      return;
    }
    
    final float sun = world.terrain().insolationSample(origin());
    structure.assignOutputs(
      Item.withAmount(POWER, sun * 4),
      Item.withAmount(WATER, (0.5f + 1 - sun) / 2)
    );
  }
  
  
  public Background[] careers() {
    return null;
  }
  
  
  public Traded[] services() {
    return null;
  }
  
  
  
  /**  Structure.Basis and placement methods-
    */
  public boolean setupWith(Tile position, Box2D area, Coord... others) {
    if (! super.setupWith(position, area, others)) return false;
    
    if (area.xdim() > area.ydim()) {
      if ((position.x / 2) % 3 == 0) {
        attachModel(MODEL_X_HUB);
      }
      else attachModel(MODEL_X_SEGMENT);
    }
    else {
      if ((position.y / 2) % 3 == 0) {
        attachModel(MODEL_Y_HUB);
      }
      else attachModel(MODEL_Y_SEGMENT);
    }
    return true;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "solar_bank");
  }
  
  
  public SelectionPane configPanel(SelectionPane panel, BaseUI UI) {
    final String status = null;
    return VenuePane.configSimplePanel(this, panel, UI, status);
  }
  
  
  public String helpInfo() {
    return
      "Solar Banks provide clean power and a small amount of water to your "+
      "settlement.";
  }
  
  
  public String objectCategory() {
    return InstallationPane.TYPE_ECOLOGIST;
  }
}



//  TODO:  Unify this with the equivalent methods in the Arcology class (and
//  maybe ShieldWall?)

/*
final static int
  FACING_X_COORDS[] = { 0, 0,  2, 0,  4, 0,  6, 0 }, OFFS_X[] = {3, 0},
  FACING_Y_COORDS[] = { 0, 0,  0, 2,  0, 4,  0, 6 }, OFFS_Y[] = {0, 3},
  SIDE_LENGTH = 8;

private SolarBank[] getBankPlacement(Tile point, Base base, int facing) {
  if (point == null) return null;
  final Stage world = point.world;
  final List <SolarBank> newBank = new List <SolarBank> ();
  final int coords[] = facing == X_AXIS ? FACING_X_COORDS : FACING_Y_COORDS;
  final int offs[]   = facing == X_AXIS ? OFFS_X : OFFS_Y;
  
  for (int n = 0; n < coords.length;) {
    final Tile under = world.tileAt(
      point.x + coords[n++] - offs[0],
      point.y + coords[n++] - offs[1]
    );
    if (under == null) return null;
    final SolarBank s = new SolarBank(base);
    s.type = TYPE_INIT;
    s.setPosition(under.x, under.y, world);
    //if (! s.canPlace()) return null;
    newBank.add(s);
  }
  return newBank.toArray(SolarBank.class);
}


private boolean bankOkay(Venue bank[]) {
  if (bank == null) return false;
  for (Venue b : bank) if (! b.canPlace()) return false;
  return true;
}


public boolean setPosition(float x, float y, Stage world) {
  if (! super.setPosition(x, y, world)) return false;
  if (type != TYPE_PLACING) return true;
  
  SolarBank group[] = null;
  if (! bankOkay(group)) group = getBankPlacement(origin(), base, X_AXIS);
  if (! bankOkay(group)) group = getBankPlacement(origin(), base, Y_AXIS);
  if (group == null) return false;
  structure.assignGroup(group);
  
  for (SolarBank bank : group) {
    bank.structure.assignGroup(group);
    if (bank == group[group.length / 2]) bank.type = TYPE_HUB;
    else bank.type = TYPE_SOLAR_RIGHT;
    bank.attachModel(TYPE_MODELS[bank.type]);
  }
  return true;
}
//*/

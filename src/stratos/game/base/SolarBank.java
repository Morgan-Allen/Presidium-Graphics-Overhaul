


package stratos.game.base;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.Background;
import stratos.game.actors.Behaviour;
//import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;



public class SolarBank extends Venue {
  
  
  final static String
    IMG_DIR = "media/Buildings/ecologist/";
  final public static ModelAsset
    ARRAY_MODELS[] = CutoutModel.fromImages(
      SolarBank.class, IMG_DIR, 2, 2, false,
      "solar_bank_left.png",
      "solar_bank_right.png",
      "solar_bank_centre.png"
    ),
    MODEL_LEFT       = ARRAY_MODELS[0],
    MODEL_RIGHT      = ARRAY_MODELS[1],
    MODEL_CENTRE     = ARRAY_MODELS[2];
  
  final static ImageAsset ICON = ImageAsset.fromImage(
    SolarBank.class, "media/GUI/Buttons/solar_array_button.gif"
  );
  
  final static int
    TYPE_PLACING     = -1,
    TYPE_INIT        =  0,
    TYPE_SOLAR_LEFT  =  1,
    TYPE_SOLAR_RIGHT =  2,
    TYPE_HUB         =  3;
  
  final static ModelAsset TYPE_MODELS[] = {
    MODEL_CENTRE,
    MODEL_LEFT,
    MODEL_RIGHT,
    MODEL_CENTRE
  };
  
  final static VenueProfile PROFILE = new VenueProfile(
    SolarBank.class, "solar_bank", "Solar Bank",
    2, 2, true, EcologistStation.PROFILE
  );
  
  
  public SolarBank(Base base) {
    super(PROFILE, base);
    this.type = TYPE_PLACING;
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
  
  
  protected void updatePaving(boolean inWorld) {
    final BaseTransport paving = base().transport;
    
    if (type == TYPE_HUB) {
      final Tile perim[] = Spacing.perimeter(footprint(), world);
      for (int n = 0; n < perim.length; n += 4) {
        final Tile t = perim[n];
        if (t != null) paving.updateJunction(this, t, inWorld && ! t.blocked());
      }
    }
    paving.updatePerimeter(this, inWorld);
  }
  
  
  public Behaviour jobFor(Actor actor, boolean onShift) {
    return null;
  }
  
  
  public Background[] careers() {
    return null;
  }
  
  
  public Traded[] services() {
    return null;
  }
  
  
  
  /**  Structure.Basis and placement methods-
    */
  //  TODO:  Unify this with the equivalent methods in the Arcology class (and
  //  maybe ShieldWall?)
  
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
  
  
  protected boolean checkPerimeter(Stage world) {
    for (Tile t : Spacing.perimeter(footprint(), world)) {
      if (t == null) continue;
      if (t.reserved()) return false;
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




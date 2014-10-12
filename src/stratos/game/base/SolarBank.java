


package stratos.game.base;
import stratos.game.common.*;
import stratos.game.actors.Actor;
import stratos.game.actors.Background;
import stratos.game.actors.Behaviour;
import stratos.game.building.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.building.Economy.*;



//  TODO:  Use the same tricks here that you did for the shield wall.


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
  
  
  public SolarBank(Base base) {
    super(2, 2, ENTRANCE_NONE, base);
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
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates);
    
    //  TODO:  Vary this based on how much you have stocked of each over the
    //  past 24 hours, sunlight/moisture values, et cetera.
    structure.assignOutputs(
      Item.withAmount(POWER, 2),
      Item.withAmount(OPEN_WATER, 0.5f)
    );
  }
  
  
  protected void updatePaving(boolean inWorld) {
    if (type != TYPE_HUB) return;
    final Paving paving = base().paving;
    paving.updatePerimeter(this, inWorld);
    
    final Tile perim[] = Spacing.perimeter(area(), world);
    for (int n = 0; n < perim.length; n += 4) {
      final Tile t = perim[n];
      if (t != null) paving.updateJunction(this, t, inWorld && ! t.blocked());
    }
  }
  
  
  public Behaviour jobFor(Actor actor) {
    return null;
  }
  
  
  public Background[] careers() {
    return null;
  }
  
  
  public TradeType[] services() {
    return null;
  }
  
  
  
  /**  Installation and placement methods-
    */
  final static int
    PLACE_COORDS[] = {
      0, -3,  0, -1,  0, 1,  0, 3
    },
    SIDE_LENGTH = 8;
  
  private SolarBank[] getBankPlacement(Tile point, Base base) {
    //
    //  Firstly, determine which sector this point lies within, and the corner
    //  tile of that sector.
    if (point == null) return null;
    final World world = point.world;
    final Tile corner = world.tileAt(
      Visit.round(point.x, 1, false),
      Visit.round(point.y, 1, false)
    );
    //
    //
    final List <SolarBank> newBank = new List <SolarBank> ();
    for (int n = 0; n < PLACE_COORDS.length;) {
      final Tile under = world.tileAt(
        corner.x + PLACE_COORDS[n++],
        corner.y + PLACE_COORDS[n++]
      );
      if (under == null || under.onTop() instanceof SolarBank) continue;
      final SolarBank s = new SolarBank(base);
      s.type = TYPE_INIT;
      s.setPosition(under.x, under.y, world);
      newBank.add(s);
    }
    return newBank.toArray(SolarBank.class);
  }
  
  
  public boolean setPosition(float x, float y, World world) {
    if (! super.setPosition(x, y, world)) return false;
    if (type != TYPE_PLACING) return true;
    
    final SolarBank group[] = getBankPlacement(origin(), base);
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
  
  
  public boolean canPlace() {
    if (type != TYPE_PLACING) return super.canPlace();
    
    if (structure.group() == null) return false;
    for (SolarBank bank : (SolarBank[]) structure.group()) {
      if (! bank.canPlace()) return false;
    }
    return true;
  }
  
  
  protected boolean checkPerimeter(World world) {
    //  TODO:  This might require some modification later.  Ideally, you want
    //  to give solar banks at least one tile of space.
    return true;
  }
  
  
  public void doPlacement() {
    if (type != TYPE_PLACING) {
      super.doPlacement();
      return;
    }
    for (SolarBank bank : (SolarBank[]) structure.group()) {
      bank.doPlacement();
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void previewPlacement(boolean canPlace, Rendering rendering) {
    if (type != TYPE_PLACING) {
      super.previewPlacement(canPlace, rendering);
      return;
    }
    final SolarBank group[] = (SolarBank[]) structure.group();
    if (group != null) for (SolarBank bank : group) {
      bank.previewPlacement(canPlace, rendering);
    }
  }
  
  
  public String fullName() {
    return "Solar Bank";
  }


  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "solar_bank");
  }
  
  
  public String helpInfo() {
    return
      "Solar Banks provide clean power and a small amount of water to your "+
      "settlement.";
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_ECOLOGIST;
  }
}


/*
//  TODO:  Restore the use of this:

protected void configFromAdjacent(boolean[] near, int numNear) {
  final Tile o = origin();
  type = TYPE_SOLAR;
  
  if (numNear > 0 && numNear <= 2) {
    if (near[N] || near[S]) {
      facing = X_AXIS;
      if (o.y % 8 == 0) {
        type = TYPE_WIND;
        attachModel(MODEL_CENTRE);
      }
      else attachModel(MODEL_LEFT);
      return;
    }
    if (near[W] || near[E]) {
      facing = Y_AXIS;
      if (o.x % 8 == 0) {
        type = TYPE_WIND;
        attachModel(MODEL_CENTRE);
      }
      else attachModel(MODEL_RIGHT);
      return;
    }
  }
  
  facing = CORNER;
  attachModel(MODEL_RIGHT);
}
//*/



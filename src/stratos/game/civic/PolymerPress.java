/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.civic;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.game.actors.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;
import static stratos.game.actors.Backgrounds.*;



public class PolymerPress extends Venue implements TileConstants {
  
  
  /**  Constructors, data fields, setup and save/load methods-
    */
  protected static boolean verbose = false;
  
  final static String IMG_DIR = "media/Buildings/ecologist/";
  final static ImageAsset ICON = ImageAsset.fromImage(
    PolymerPress.class, "media/GUI/Buttons/polymer_press_button.gif"
  );
  final static ModelAsset
    MODEL = CutoutModel.fromImage(
      PolymerPress.class, IMG_DIR+"polymer_press.png", 3, 1
    );
  
  final public static Conversion
    FLORA_TO_POLYMER = new Conversion(
      PolymerPress.class, "flora_to_polymer",
      TO, 1, POLYMER
    ),
    CARBS_TO_POLYMER = new Conversion(
      Nursery.class, "carbs_to_polymer",
      1, CARBS, TO, 1, POLYMER
    );
  
  final static Blueprint BLUEPRINT = new Blueprint(
    PolymerPress.class, "polymer_press",
    "Polymer Press", UIConstants.TYPE_HIDDEN,
    3, 1, IS_NORMAL,
    NO_REQUIREMENTS, Owner.TIER_FACILITY,
    FLORA_TO_POLYMER//, CARBS_TO_POLYMER
  );
  
  
  private Box2D areaClaimed = new Box2D();
  
  
  public PolymerPress(Base base) {
    super(BLUEPRINT, base);
    structure.setupStats(
      25,  //integrity
      5,  //armour
      75,  //build cost
      0,  //max upgrades
      Structure.TYPE_FIXTURE
    );
    staff.setShiftType(SHIFTS_BY_DAY);
    attachModel(MODEL);
  }
  
  
  public PolymerPress(Session s) throws Exception {
    super(s);
    areaClaimed.loadFrom(s.input());
    type         = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    areaClaimed.saveTo(s.output());
    s.saveInt        (type        );
  }
  
  
  
  /**  Placement and supply-demand functions-
    */
  public boolean setupWith(Tile position, Box2D area, Coord... others) {
    if (! super.setupWith(position, area, others)) return false;
    //
    //  By default, we claim an area 2 tiles larger than the basic footprint,
    //  but we can also have a larger area assigned (e.g, by a human player or
    //  by an automated placement-search.)
    areaClaimed.setTo(footprint()).expandBy(2);
    if (area != null) areaClaimed.include(area);
    this.facing = areaClaimed.xdim() > areaClaimed.ydim() ?
      FACING_SOUTH : FACING_EAST
    ;
    return true;
  }
  
  
  public boolean canPlace(Account reasons) {
    if (! super.canPlace(reasons)) return false;
    if (areaClaimed.maxSide() > Stage.ZONE_SIZE) {
      return reasons.asFailure("Area is too large!");
    }
    final Stage world = origin().world;
    if (! Placement.perimeterFits(this, areaClaimed, owningTier(), 2, world)) {
      return reasons.asFailure("Might obstruct pathing");
    }
    return true;
  }
  
  
  public Box2D areaClaimed() {
    return areaClaimed;
  }
  
  
  
  /**  Economic functions-
    */
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    structure.setAmbienceVal(2);
    stocks.incDemand(POLYMER, 1, 1, true);
  }
  
  public Background[] careers () { return new Background[] { CULTIVATOR }; }
  
  
  protected int numOpenings(Background b) {
    final int nO = super.numOpenings(b);
    if (b == CULTIVATOR) return nO + 2;
    return 0;
  }
  
  
  protected Behaviour jobFor(Actor actor, boolean onShift) {
    if (staff.shiftFor(actor) == OFF_DUTY) return null;
    final Choice choice = new Choice(actor);
    
    final Bringing d = BringUtils.bestBulkDeliveryFrom(
      this, services(), 1, 5, 5
    );
    choice.add(d);
    
    Venue source = (EcologistStation) world.presences.nearestMatch(
      EcologistStation.class, this, Stage.ZONE_SIZE
    );
    if (source == null) source = this;
    choice.add(Forestry.nextPlanting(actor, source));
    
    choice.add(Forestry.nextCutting (actor, this));
    return choice.weightedPick();
  }
  
  public Traded[] services() { return new Traded[] { POLYMER }; }
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "polymer_press");
  }
  
  
  public SelectionPane configPanel(SelectionPane panel, BaseUI UI) {
    return VenuePane.configSimplePanel(this, panel, UI, null);
  }
  
  
  public String helpInfo() {
    return
      "The Polymer Press converts local Flora into long-chain hydrocarbons "+
      "for use in plastics production.";
  }
}








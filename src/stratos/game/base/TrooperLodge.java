

package stratos.game.base;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.plans.Patrolling;
import stratos.game.tactical.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;

import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.building.Economy.*;



public class TrooperLodge extends Venue {
  
  
  /**  Fields, constants, and save/load methods-
    */
  final static ModelAsset MODEL = CutoutModel.fromImage(
    TrooperLodge.class, "media/Buildings/military/house_garrison.png", 4.25f, 3
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    TrooperLodge.class, "media/GUI/Buttons/garrison_button.gif"
  );
  
  
  private DrillYard drillYard;
  
  
  public TrooperLodge(Base base) {
    super(4, 3, ENTRANCE_SOUTH, base);
    structure.setupStats(
      500, 20, 250,
      Structure.SMALL_MAX_UPGRADES, Structure.TYPE_FIXTURE
    );
    personnel.setShiftType(SHIFTS_BY_HOURS);
    attachSprite(MODEL.makeSprite());
  }
  
  
  public TrooperLodge(Session s) throws Exception {
    super(s);
    drillYard = (DrillYard) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(drillYard);
  }
  
  
  
  /**  Upgrades, economic functions and actor behaviour-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    TrooperLodge.class, "garrison_upgrades"
  );
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES; }
  final public static Upgrade
    MELEE_TRAINING = new Upgrade(
      "Melee Training",
      "Prepares your soldiers for the rigours of close combat.",
      150, null, 3, null, ALL_UPGRADES
    ),
    MARKSMAN_TRAINING = new Upgrade(
      "Marksman Training",
      "Prepares your soldiers for ranged marksmanship.",
      150, null, 3, null, ALL_UPGRADES
    ),
    ENDURANCE_TRAINING = new Upgrade(
      "Endurance Training",
      "Prepares your soldiers for guerilla warfare and wilderness survival.",
      200, null, 3, null, ALL_UPGRADES
    ),
    AID_TRAINING = new Upgrade(
      "Peacekeeper Training",
      "Educates your soldiers about the use of minimal force, local "+
      "contacts, and proper treatment of prisoners.",
      200, null, 3, null, ALL_UPGRADES
    ),
    VOLUNTEER_STATION = new Upgrade(
      "Volunteer Station",
      "Dedicated in defence of their homes, a volunteer militia provides the "+
      "mainstay of your domestic forces.",
      100,
      Backgrounds.VOLUNTEER, 2, null, ALL_UPGRADES
    ),
    VETERAN_STATION = new Upgrade(
      "Veteran Station",
      "Seasoned professional soldiers, veterans provide the backbone of your "+
      "officer corps and command structure.",
      150,
      Backgrounds.VETERAN, 1, VOLUNTEER_STATION, ALL_UPGRADES
    );
  
  public Background[] careers() {
    return new Background[] { Backgrounds.VOLUNTEER, Backgrounds.VETERAN };
  }
  
  
  public int numOpenings(Background v) {
    int num = super.numOpenings(v);
    if (v == Backgrounds.VOLUNTEER) return num + 2;
    if (v == Backgrounds.VETERAN  ) return num + 1;
    return 0;
  }
  
  
  public TradeType[] services() {
    return new TradeType[] {};
  }
  
  
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null;
    return Patrolling.nextGuardPatrol(actor, this, Plan.ROUTINE);
  }
  
  
  public boolean enterWorldAt(int x, int y, World world) {
    if (! super.enterWorldAt(x, y, world)) return false;
    updateDrillYard();
    return true;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates);
    updateDrillYard();
    if (! structure.intact()) return;
  }
  
  //
  //  TODO:  Have the drill yard be visible during placement previews?  Ideally,
  //  yeah.
  
  //
  //  TODO:  For that to work, you'll need to override the previewAt(),
  //  setPosition(), and doPlace() methods.
  
  protected void updateDrillYard() {
    if (drillYard == null || drillYard.destroyed()) {
      final DrillYard newYard = new DrillYard(base).assignTo(this);
      final Tile o = origin();
      final int S = this.size;
      
      for (int n : TileConstants.N_ADJACENT) {
        n = (n + 2) % 8;
        newYard.setPosition(o.x + (N_X[n] * S), o.y + (N_Y[n] * S), world);
        if (newYard.canPlace()) {
          newYard.doPlacement();
          drillYard = newYard;
          break;
        }
      }
    }
  }
  
  
  //  TODO:  This needs to be generalised more robustly.
  /*
  public void onDecommision() {
    super.onDecommission();
    if (drillYard != null) {
      drillYard.structure.setState(Structure.STATE_SALVAGE, -1);
    }
  }
  //*/
  
  
  
  /**  Rendering and interface methods-
    */
  public void renderSelection(Rendering rendering, boolean hovered) {
    BaseUI.current().selection.renderTileOverlay(
      rendering, world,
      hovered ? Colour.transparency(0.5f) : Colour.WHITE,
      Selection.SELECT_OVERLAY, true, this, this, drillYard
    );
  }
  
  
  public SelectionInfoPane configPanel(SelectionInfoPane panel, BaseUI UI) {
    final String CAT_DRILLS = "DRILLS";
    final VenueDescription d = new VenueDescription(
      this, CAT_STATUS, CAT_STAFF, CAT_DRILLS, CAT_UPGRADES
    ) {
      protected void describeCategory(Description d, BaseUI UI, String catID) {
        if (catID.equals(CAT_DRILLS)) {
          if (drillYard == null) d.append("No drill yard!");
          else drillYard.describeDrills(d);
        }
        else super.describeCategory(d, UI, catID);
      }
    };
    return d.configPanel(panel, UI);
  }
  
  
  public String fullName() {
    return "Trooper Lodge";
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "garrison");
  }
  
  
  public String helpInfo() {
    return
      "The Trooper Lodge allows you to recruit the sturdy, disciplined and "+
      "heavily-equipped Trooper into the rank and file of your armed forces.";
  }
  
  
  public String buildCategory() {
    return UIConstants.TYPE_MILITANT;
  }
}








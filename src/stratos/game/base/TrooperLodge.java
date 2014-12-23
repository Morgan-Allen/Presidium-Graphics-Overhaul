

package stratos.game.base;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.economic.Economy.*;



public class TrooperLodge extends Venue {
  
  
  /**  Fields, constants, and save/load methods-
    */
  final static ModelAsset MODEL = CutoutModel.fromImage(
    TrooperLodge.class, "media/Buildings/military/house_garrison.png", 4.25f, 3
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    TrooperLodge.class, "media/GUI/Buttons/garrison_button.gif"
  );
  
  
  public TrooperLodge(Base base) {
    super(4, 3, ENTRANCE_SOUTH, base);
    structure.setupStats(
      500, 20, 250,
      Structure.SMALL_MAX_UPGRADES, Structure.TYPE_FIXTURE
    );
    staff.setShiftType(Venue.SHIFTS_BY_24_HOUR);
    attachSprite(MODEL.makeSprite());
  }
  
  
  public TrooperLodge(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Upgrades, economic functions and actor behaviour-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
  );
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES; }
  final public static Upgrade
    MELEE_TRAINING = new Upgrade(
      "Melee Training",
      "Prepares your soldiers for the rigours of close combat.",
      150, null, 3, null,
      TrooperLodge.class, ALL_UPGRADES
    ),
    MARKSMAN_TRAINING = new Upgrade(
      "Marksman Training",
      "Prepares your soldiers for ranged marksmanship.",
      150, null, 3, null,
      TrooperLodge.class, ALL_UPGRADES
    ),
    ENDURANCE_TRAINING = new Upgrade(
      "Endurance Training",
      "Prepares your soldiers for guerilla warfare and wilderness survival.",
      200, null, 3, null,
      TrooperLodge.class, ALL_UPGRADES
    ),
    PEACEKEEPER_TRAINING = new Upgrade(
      "Peacekeeper Training",
      "Prepares your soldiers to use minimal force, build local contacts,"+
      "and ensure fair treatment of prisoners.",
      200, null, 3, null,
      TrooperLodge.class, ALL_UPGRADES
    ),
    VOLUNTEER_STATION = new Upgrade(
      "Volunteer Station",
      VOLUNTEER.info,
      200,
      Backgrounds.VOLUNTEER, 2, null,
      TrooperLodge.class, ALL_UPGRADES
    ),
    TROOPER_STATION = new Upgrade(
      "Trooper Station",
      TROOPER.info,
      450,
      Backgrounds.TROOPER, 1, VOLUNTEER_STATION,
      TrooperLodge.class, ALL_UPGRADES
    );
  
  public Background[] careers() {
    return new Background[] { Backgrounds.VOLUNTEER, Backgrounds.TROOPER };
  }
  
  
  public int numOpenings(Background v) {
    int num = super.numOpenings(v);
    if (v == Backgrounds.VOLUNTEER) return num + 2;
    if (v == Backgrounds.TROOPER  ) return num + 1;
    return 0;
  }
  
  
  public Traded[] services() {
    return new Traded[] {};
  }
  
  
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! staff.onShift(actor))) return null;
    
    //  TODO:  INCLUDE DRILLING!
    
    //  TODO:  Try to optimise this?
    final ShieldWall wall = (ShieldWall) world.presences.randomMatchNear(
      ShieldWall.class, this, Stage.SECTOR_SIZE
    );
    if (wall != null && wall.base() == base) {
      final int compass = TileConstants.T_ADJACENT[Rand.index(4)];
      final Patrolling sentry = Patrolling.sentryDuty(actor, wall, compass);
      return sentry;
    }
    //
    //  Otherwise, fall back on regular patrols.  (Try to do this in groups?)
    return Patrolling.nextGuardPatrol(actor, this, Plan.ROUTINE);
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
  }
  
  
  
  /**  Rendering and interface methods-
    */
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
  
  
  public String objectCategory() {
    return UIConstants.TYPE_MILITANT;
  }
}








/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.civic;
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
  
  final static VenueProfile PROFILE = new VenueProfile(
    TrooperLodge.class, "trooper_lodge", "Trooper Lodge",
    4, 3, IS_NORMAL,
    Bastion.PROFILE, Owner.TIER_FACILITY
  );
  
  
  public TrooperLodge(Base base) {
    super(PROFILE, base);
    structure.setupStats(
      500, 20, 250,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_FIXTURE
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
      "Drills your soldiers for the rigours of close combat and tight "+
      "formations.",
      150, Upgrade.THREE_LEVELS, null, 3,
      null, TrooperLodge.class, ALL_UPGRADES
    ),
    MARKSMAN_TRAINING = new Upgrade(
      "Marksman Training",
      "Drills your soldiers to improve ranged marksmanship.",
      150, Upgrade.THREE_LEVELS, null, 3,
      null, TrooperLodge.class, ALL_UPGRADES
    ),
    FIELD_MEDICINE = new Upgrade(
      "Field Medicine",
      "Drills your soldiers in first aid techniques and use of combat stims.",
      200, Upgrade.THREE_LEVELS, null, 3,
      null, TrooperLodge.class, ALL_UPGRADES
    ),
    FIELD_REPAIRS = new Upgrade(
      "Field Repairs",
      "Drills your soldiers in maintaining equipment and aiding in "+
      "construction projects.",
      200, Upgrade.THREE_LEVELS, null, 3,
      null, TrooperLodge.class, ALL_UPGRADES
    ),
    VOLUNTEER_STATION = new Upgrade(
      "Volunteer Station",
      VOLUNTEER.info,
      200,
      Upgrade.THREE_LEVELS, Backgrounds.VOLUNTEER, 1,
      null, TrooperLodge.class, ALL_UPGRADES
    ),
    TROOPER_STATION = new Upgrade(
      "Trooper Station",
      TROOPER.info,
      450,
      Upgrade.THREE_LEVELS, Backgrounds.TROOPER, 1,
      VOLUNTEER_STATION, TrooperLodge.class, ALL_UPGRADES
    );

  final static Upgrade TRAIN_UPGRADES[] = {
    MELEE_TRAINING,
    MARKSMAN_TRAINING,
    FIELD_MEDICINE,
    FIELD_REPAIRS
  };
  final static Skill TRAIN_SKILLS[][] = {
    { HAND_TO_HAND, FORMATION_COMBAT },
    { MARKSMANSHIP, SURVEILLANCE },
    { ANATOMY, PHARMACY },
    { ASSEMBLY, HARD_LABOUR }
  };
  
  
  public Behaviour jobFor(Actor actor, boolean onShift) {
    final Choice choice = new Choice(actor);
    final boolean offShift = staff.shiftFor(actor) == SECONDARY_SHIFT;
    //
    //  We allow for drilling in various skills during a soldier's secondary
    //  shift-
    if (offShift) for (int i = 4; i-- > 0;) {
      final Upgrade TU = TRAIN_UPGRADES[i];
      final float trainLevel = structure.upgradeLevel(TU);
      if (trainLevel == 0) continue;
      final Skill TS[] = TRAIN_SKILLS[i];
      final Training s = Training.asDrill(actor, this, TS, trainLevel * 5);
      s.addMotives(Plan.MOTIVE_JOB, (trainLevel + 1) * Plan.CASUAL / 2);
      choice.add(s);
    }
    //
    //  If there are shield walls built nearby, we try to patrol along their
    //  perimeter.
    //  TODO:  Move this to the factory method
    if (onShift) {
      final ShieldWall wall = (ShieldWall) world.presences.randomMatchNear(
        ShieldWall.class, this, Stage.SECTOR_SIZE
      );
      if (wall != null && wall.base() == base) {
        final int compass = TileConstants.T_ADJACENT[Rand.index(4)];
        final Patrolling sentry = Patrolling.sentryDuty(actor, wall, compass);
        choice.add(sentry);
      }
      //
      //  Otherwise, fall back on regular patrols.
      //  TODO:  Try to patrol in groups?
      else {
        choice.add(Patrolling.nextGuardPatrol(actor, this, Plan.ROUTINE));
      }
    }
    return choice.weightedPick();
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
  }
  
  
  
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
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "garrison");
  }
  
  
  public String helpInfo() {
    return
      "The Trooper Lodge allows you to recruit the sturdy, disciplined and "+
      "heavily-equipped Trooper into the rank and file of your armed forces.";
  }
  
  
  public String objectCategory() {
    return UIConstants.TYPE_SECURITY;
  }
  
  
  //  TODO:  Add this to the various missions instead?  So that you can include
  //  yourself, or agents of your household?
  
  /*
  public SelectionInfoPane configPanel(SelectionInfoPane panel, BaseUI UI) {
    //  TODO:  Add the Call-of-duty order here.
    //return VenueDescription.configStandardPanel(this, panel, UI, false);
  }
  //*/
}














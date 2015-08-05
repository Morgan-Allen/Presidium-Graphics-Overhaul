/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.game.plans.*;
import stratos.game.maps.*;
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
    TrooperLodge.class, "media/Buildings/military/trooper_lodge.png", 4, 2
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    TrooperLodge.class, "media/GUI/Buttons/trooper_lodge_button.gif"
  );
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    TrooperLodge.class, "trooper_lodge",
    "Trooper Lodge", UIConstants.TYPE_SECURITY, ICON,
    "The Trooper Lodge allows you to recruit the sturdy, disciplined and "+
    "heavily-equipped Trooper into the rank and file of your armed forces.",
    4, 2, Structure.IS_NORMAL,
    Owner.TIER_FACILITY, 500,
    20, 250, Structure.NORMAL_MAX_UPGRADES
  );
  
  
  public TrooperLodge(Base base) {
    super(BLUEPRINT, base);
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
  final static Siting SITING = new Siting(BLUEPRINT);
  
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
  );
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES; }
  final public static Upgrade
    MELEE_TRAINING = new Upgrade(
      "Melee Training",
      "Drills your soldiers for the rigours of close combat and tight "+
      "formations.",
      150, Upgrade.THREE_LEVELS, null, 3,
      null, BLUEPRINT
    ),
    MARKSMAN_TRAINING = new Upgrade(
      "Marksman Training",
      "Drills your soldiers to improve ranged marksmanship.",
      150, Upgrade.THREE_LEVELS, null, 3,
      null, BLUEPRINT
    ),
    FIELD_MEDICINE = new Upgrade(
      "Field Medicine",
      "Drills your soldiers in first aid techniques and use of combat stims.",
      200, Upgrade.THREE_LEVELS, null, 3,
      null, BLUEPRINT
    ),
    FIELD_REPAIRS = new Upgrade(
      "Field Repairs",
      "Drills your soldiers in maintaining equipment and aiding in "+
      "construction projects.",
      200, Upgrade.THREE_LEVELS, null, 3,
      null, BLUEPRINT
    ),
    VOLUNTEER_POST = new Upgrade(
      "Volunteer Post",
      VOLUNTEER.info,
      200,
      Upgrade.THREE_LEVELS, Backgrounds.VOLUNTEER, 1,
      null, BLUEPRINT
    ),
    TROOPER_OFFICE = new Upgrade(
      "Trooper Office",
      TROOPER.info,
      450,
      Upgrade.THREE_LEVELS, Backgrounds.TROOPER, 1,
      VOLUNTEER_POST, BLUEPRINT
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
  
  
  public Behaviour jobFor(Actor actor) {
    if (staff.offDuty(actor)) return null;
    final Choice choice = new Choice(actor);
    //
    //  If there are shield walls built nearby, we try to patrol along their
    //  perimeter.
    if (staff.onShift(actor)) {
      //
      //  TODO:  Move this to the factory methods for Patrolling.
      final ShieldWall wall = (ShieldWall) world.presences.randomMatchNear(
        ShieldWall.class, this, Stage.ZONE_SIZE
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
    //
    //  We allow for drilling in various skills during a soldier's secondary
    //  shift-
    else for (int i = 4; i-- > 0;) {
      final Upgrade TU = TRAIN_UPGRADES[i];
      final float trainLevel = structure.upgradeLevel(TU);
      if (trainLevel == 0) continue;
      final Skill TS[] = TRAIN_SKILLS[i];
      final Training s = Training.asDrill(actor, this, TS, trainLevel * 5);
      s.addMotives(Plan.MOTIVE_JOB, (trainLevel + 1) * Plan.CASUAL / 2);
      choice.add(s);
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
    if (v == Backgrounds.TROOPER  ) return num + 2;
    return 0;
  }
  
  
  public Traded[] services() {
    return new Traded[] {};
  }
}














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
import static stratos.content.abilities.TrooperTechniques.*;



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
    "Trooper Lodge", Target.TYPE_SECURITY, ICON,
    "The Trooper Lodge allows you to recruit the sturdy, disciplined and "+
    "heavily-equipped Trooper into the rank and file of your armed forces.",
    4, 2, Structure.IS_NORMAL, Owner.TIER_FACILITY, 500, 20,
    VOLUNTEER, TROOPER
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
    LEVELS[] = BLUEPRINT.createVenueLevels(
      Upgrade.TWO_LEVELS, null,
      new Object[] { 10, BATTLE_TACTICS, 0, HAND_TO_HAND, 0, MARKSMANSHIP },
      650, 800//, 1000
    ),
    MELEE_TRAINING = new Upgrade(
      "Melee Training",
      "Drills your soldiers for the rigours of close combat and tight "+
      "formation.",
      150, Upgrade.THREE_LEVELS, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null,
      5, BATTLE_TACTICS, 10, HAND_TO_HAND
    ),
    MARKSMAN_TRAINING = new Upgrade(
      "Marksman Training",
      "Drills your soldiers to improve ranged marksmanship and observation.",
      150, Upgrade.THREE_LEVELS, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null,
      5, BATTLE_TACTICS, 10, MARKSMANSHIP
    ),
    POWER_ARMOUR_UPGRADE = new Upgrade(
      "Power Armour",
      "Allows your soldiers to enter the field in mechanised combat suits.",
      600, Upgrade.SINGLE_LEVEL, LEVELS[1], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null,
      10, BATTLE_TACTICS, 15, HAND_TO_HAND
    ),
    FRAG_LAUNCHER_UPGRADE = new Upgrade(
      "Frag Launcher",
      "Allows your soldiers to field rocket-propelled grenades in the field.",
      600, Upgrade.SINGLE_LEVEL, LEVELS[1], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null,
      10, BATTLE_TACTICS, 15, MARKSMANSHIP
    ),
    FIELD_MEDICINE = new Upgrade(
      "Field Medicine",
      "Drills your soldiers in first aid techniques and use of combat stims.",
      200, Upgrade.SINGLE_LEVEL, LEVELS[1], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null,
      5, BATTLE_TACTICS, 5, ANATOMY
    ),
    FIELD_REPAIRS = new Upgrade(
      "Field Repairs",
      "Drills your soldiers in maintaining equipment and aiding in "+
      "construction projects.",
      200, Upgrade.SINGLE_LEVEL, LEVELS[1], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null,
      5, BATTLE_TACTICS, 5, ASSEMBLY
    );
  
  final static Upgrade TRAIN_UPGRADES[] = {
    MELEE_TRAINING,
    MARKSMAN_TRAINING,
    FIELD_MEDICINE,
    FIELD_REPAIRS
  };
  final static Skill TRAIN_SKILLS[][] = {
    { HAND_TO_HAND, FORMATION_COMBAT },
    { MARKSMANSHIP, SURVEILLANCE     },
    { ANATOMY     , PHARMACY         },
    { ASSEMBLY    , HARD_LABOUR      }
  };
  
  
  public Behaviour jobFor(Actor actor) {
    if (staff.offDuty(actor)) return null;
    final Choice choice = new Choice(actor);
    //
    //  If there are shield walls built nearby, we try to patrol along their
    //  perimeter.
    if (staff.onShift(actor)) {
      choice.isVerbose = true;
      Patrolling.addFormalPatrols(actor, this, choice);
    }
    //
    //  We allow for drilling in various skills during a soldier's secondary
    //  shift, and acquiring upgrade-based techniques.
    else for (int i = 4; i-- > 0;) {
      final Upgrade TU = TRAIN_UPGRADES[i];
      final float trainLevel = structure.upgradeLevel(TU);
      if (trainLevel == 0) continue;
      final Skill TS[] = TRAIN_SKILLS[i];
      final Studying s = Studying.asDrill(actor, this, TS, trainLevel * 5);
      s.addMotives(Plan.MOTIVE_JOB, (trainLevel + 1) * Plan.CASUAL / 2);
      choice.add(s);
    }
    choice.add(Studying.asTechniqueTraining(actor, this, 0, canLearn()));
    return choice.weightedPick();
  }
  
  
  private Technique[] canLearn() {
    Batch <Technique> can = new Batch();
    if (structure.hasUpgrade(FRAG_LAUNCHER_UPGRADE)) can.add(FRAG_LAUNCHER   );
    if (structure.hasUpgrade(POWER_ARMOUR_UPGRADE )) can.add(POWER_ARMOUR_USE);
    return can.toArray(Technique.class);
  }
  
  
  public int numPositions(Background v) {
    final int numP = super.numPositions(v);
    final int level = structure.mainUpgradeLevel();
    if (v == Backgrounds.TROOPER) return 1 + level;
    return numP;
  }
}














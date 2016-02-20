/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.actors.*;
import stratos.game.plans.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.craft.Economy.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.content.abilities.TrooperTechniques.*;



public class TrooperLodge extends Venue implements Conscription {
  
  
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
    SERVICE_SECURITY, VOLUNTEER, TROOPER
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
  
  
  
  /**  Support for mobilisations & conscription-
    */
  public boolean canConscript(Mobile unit, boolean checkDowntime) {
    if (checkDowntime && unit instanceof Actor) {
      final Profile p = base().profiles.profileFor((Actor) unit);
      if (p.downtimeDays() > 0) return false;
    }
    return Staff.doesBelong(unit, this);
  }
  
  
  public float payMultiple(Actor conscript) {
    float multiple = 2;
    multiple *= 1 + (structure.upgradeLevel(CALL_OF_DUTY) * 0.25f);
    return multiple;
  }
  
  
  public float motiveBonus(Actor conscript) {
    float bonus = Plan.ROUTINE;
    bonus += structure.upgradeLevel(CALL_OF_DUTY) * Plan.CASUAL / 2;
    if (structure.hasUpgrade(ESPRIT_DE_CORPS)) bonus += Plan.CASUAL;
    return bonus;
  }
  
  
  public void beginConscription(Mobile unit, Mission mission) {
    final Actor s = (Actor) unit;
    s.mind.assignMission(mission);
    mission.setSpecialRewardFor(s, Pledge.militaryDutyPledge(s));
    mission.setApprovalFor(s, true);
  }
  
  
  public void beginDowntime(Actor conscript) {
    final Profile p = base().profiles.profileFor(conscript);
    float downDays = 2;
    downDays -= structure.upgradeLevel(ESPRIT_DE_CORPS) * 0.5f;
    downDays -= structure.upgradeLevel(CALL_OF_DUTY   ) * 0.5f;
    p.incDowntimeDays(downDays);
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
    SPARRING_GYM = new Upgrade(
      "Sparring Gym",
      "Drills both your soldiers and citizens for the rigours of close "+
      "combat and physical endurance.",
      250, Upgrade.THREE_LEVELS, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null,
      5, BATTLE_TACTICS, 10, HAND_TO_HAND
    ),
    FIRING_RANGE = new Upgrade(
      "Firing Range",
      "Drills both your soldiers and citizens to improve ranged marksmanship "+
      "and observation.",
      250, Upgrade.THREE_LEVELS, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null,
      5, BATTLE_TACTICS, 10, MARKSMANSHIP
    ),
    BARRACKS_SPACE = new Upgrade(
      "Barracks Space",
      "Increases barracks space by 1 for "+TROOPER+"s.",
      400, Upgrade.SINGLE_LEVEL, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, SPARRING_GYM,
      5, BATTLE_TACTICS, 5, HARD_LABOUR
    ),
    CALL_OF_DUTY = new Upgrade(
      "Call of Duty",
      "Increases soldiers' salaries by 25%, but increases the priority given "+
      "to military operations and reduces downtime.",
      400, Upgrade.TWO_LEVELS, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, SPARRING_GYM,
      5, BATTLE_TACTICS, 5, HARD_LABOUR
    ),
    POWER_ARMOUR_UPGRADE = new Upgrade(
      "Power Armour",
      "Allows your soldiers to enter the field in mechanised combat suits.",
      600, Upgrade.SINGLE_LEVEL,
      new Upgrade[] { LEVELS[1], SPARRING_GYM }, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null,
      10, BATTLE_TACTICS, 15, HAND_TO_HAND
    ),
    FRAG_LAUNCHER_UPGRADE = new Upgrade(
      "Frag Launchers",
      "Allows your soldiers to field explosive rockets in battle.",
      600, Upgrade.SINGLE_LEVEL,
      new Upgrade[] { LEVELS[1], FIRING_RANGE }, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null,
      10, BATTLE_TACTICS, 15, MARKSMANSHIP
    ),
    SUPPORT_TRAINING = new Upgrade(
      "Support Training",
      "Drills your soldiers and citizens in the essentials of maintenance "+
      "and field medicine.",
      300, Upgrade.THREE_LEVELS, BARRACKS_SPACE, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null,
      10, BATTLE_TACTICS, 5, ANATOMY, 5, ASSEMBLY
    ),
    ESPRIT_DE_CORPS  = new Upgrade(
      "Esprit de Corps",
      "Increases barracks space by 1 while raising priority of military "+
      "operations and further reducing downtime.",
      500, Upgrade.SINGLE_LEVEL,
      new Upgrade[] { CALL_OF_DUTY, SUPPORT_TRAINING }, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null,
      10, BATTLE_TACTICS, 10, COMMAND
    )
  ;
  
  final static Upgrade TRAIN_UPGRADES[] = {
    SPARRING_GYM,
    FIRING_RANGE,
    SUPPORT_TRAINING
  };
  final static Skill TRAIN_SKILLS[][] = {
    { HAND_TO_HAND, COMMAND        },
    { MARKSMANSHIP, SURVEILLANCE   },
    { ANATOMY     , ASSEMBLY       }
  };
  final static float TRAIN_BONUSES[] = {
    10, 5, 0
  };
  
  
  public Behaviour jobFor(Actor actor) {
    if (staff.offDuty(actor)) return null;
    final Choice choice = new Choice(actor);
    final Profile p = base().profiles.profileFor(actor);
    //
    //  Patrol during normal hours, drill during secondary shift, or do nothing
    //  at all during downtime post-mission:
    if (p.downtimeDays() > 0) {
      return null;
    }
    else if (staff.onShift(actor)) {
      Patrolling.addFormalPatrols(actor, this, choice);
    }
    else {
      addDrilling(choice, actor, true);
    }
    return choice.weightedPick();
  }
  
  
  protected void addServices(Choice choice, Actor client) {
    if (Staff.doesBelong(client, this)) return;
    addDrilling(choice, client, false);
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    
    for (Actor a : staff.workers()) {
      if (a.mind.mission() != null) continue;
      final Profile p = base().profiles.profileFor(a);
      final float downInc = -1f / Stage.STANDARD_DAY_LENGTH;
      if (p.downtimeDays() > 0) p.incDowntimeDays(downInc);
    }
    
    if (structure.hasUpgrade(FRAG_LAUNCHER_UPGRADE)) {
      stocks.bumpItem(FRAG_LAUNCHER_AMMO, 2, 2);
    }
    if (structure.hasUpgrade(POWER_ARMOUR_UPGRADE)) {
      stocks.bumpItem(Outfits.POWER_ARMOUR, 1, 1);
    }
  }
  

  private void addDrilling(Choice choice, Actor actor, boolean job) {
    for (int i = TRAIN_UPGRADES.length; i-- > 0;) {
      final Upgrade TU = TRAIN_UPGRADES[i];
      final float upLevel = structure.upgradeLevel(TU);
      if (upLevel == 0) continue;
      
      final Skill TS[] = TRAIN_SKILLS[i];
      float trainLevel = (TRAIN_BONUSES[i] + (upLevel * 5)) * (job ? 1 : 0.5f);
      final Studying s = Studying.asDrill(actor, this, TS, trainLevel);
      
      if (job) s.addMotives(Plan.MOTIVE_JOB, (upLevel + 1) * Plan.CASUAL / 2);
      choice.add(s);
    }
    if (job) {
      Studying s = Studying.asTechniqueTraining(actor, this, 0, canLearn());
      s.addMotives(Plan.MOTIVE_JOB, Plan.CASUAL);
      choice.add(s);
    }
  }
  
  
  private Technique[] canLearn() {
    Batch <Technique> can = new Batch();
    can.add(FENDING_BLOW);
    can.add(SUPPRESSION );
    if (structure.hasUpgrade(FRAG_LAUNCHER_UPGRADE)) can.add(FRAG_LAUNCHER   );
    if (structure.hasUpgrade(POWER_ARMOUR_UPGRADE )) can.add(POWER_ARMOUR_USE);
    return can.toArray(Technique.class);
  }
  
  
  public int numPositions(Background v) {
    final int numP = super.numPositions(v);
    final int level = structure.mainUpgradeLevel();
    int bonus = 0;
    if (structure.hasUpgrade(BARRACKS_SPACE )) bonus++;
    if (structure.hasUpgrade(ESPRIT_DE_CORPS)) bonus++;
    if (v == Backgrounds.TROOPER) return level + bonus;
    if (v == Backgrounds.VOLUNTEER) return 1 + level;
    return numP;
  }
}














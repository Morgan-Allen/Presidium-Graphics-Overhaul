/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.game.plans.*;
import stratos.game.maps.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
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
  
  
  
  /**  Support for mobilisations-
    */
  public boolean isDeployed() {
    return false;
  }
  
  
  protected void addDeploymentBehaviours(Actor actor, Choice choice) {
    
  }
  
  
  public float deploymentTime() {
    float time = Stage.STANDARD_DAY_LENGTH;
    if (structure.hasUpgrade(CALL_OF_DUTY)) time += Stage.STANDARD_DAY_LENGTH;
    if (structure.hasUpgrade(ESPRIT_DE_CORPS)) time *= 1.5f;
    return time;
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
      200, Upgrade.SINGLE_LEVEL, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, SPARRING_GYM,
      5, BATTLE_TACTICS, 5, DOMESTICS
    ),
    
    //*
    //  TODO:  Increases probability of volunteering for missions?  How do I
    //  handle this, exactly?
    CALL_OF_DUTY = new Upgrade(
      "Call of Duty",
      "Doubles soldiers' salaries, but extends duration of Call to Arms by "+
      "24 hours.",
      300, Upgrade.SINGLE_LEVEL, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, SPARRING_GYM,
      5, BATTLE_TACTICS, 5, DOMESTICS
    ),
    //*/
    
    POWER_ARMOUR_UPGRADE = new Upgrade(
      "Power Armour",
      "Allows your soldiers to enter the field in mechanised combat suits.",
      600, Upgrade.SINGLE_LEVEL,
      new Upgrade[] { LEVELS[1], SPARRING_GYM }, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null,
      10, BATTLE_TACTICS, 15, HAND_TO_HAND
    ),
    FRAG_LAUNCHER_UPGRADE = new Upgrade(
      "Frag Launcher",
      "Allows your soldiers to field explosive rockets in battle.",
      600, Upgrade.SINGLE_LEVEL,
      new Upgrade[] { LEVELS[1], FIRING_RANGE }, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null,
      10, BATTLE_TACTICS, 15, MARKSMANSHIP
    ),
    
    SUPPORT_TRAINING = new Upgrade(
      "Support Training",
      "Drills your soldiers in the essentials of maintenance and field "+
      "medicine.",
      300, Upgrade.THREE_LEVELS, BARRACKS_SPACE, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null,
      10, BATTLE_TACTICS, 5, ANATOMY, 5, ASSEMBLY
    ),
    
    //  TODO:  Increases barracks space and effectiveness of call to arms.
    ESPRIT_DE_CORPS  = new Upgrade(
      "Esprit de Corps",
      "Increases barracks space by 1 and extends duration of Call to Arms "+
      "by 50%.",
      500, Upgrade.SINGLE_LEVEL,
      new Upgrade[] { BARRACKS_SPACE, SUPPORT_TRAINING }, BLUEPRINT,
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
    { HAND_TO_HAND, FORMATION_COMBAT },
    { MARKSMANSHIP, SURVEILLANCE     },
    { ANATOMY     , ASSEMBLY         }
  };
  final static float TRAIN_BONUSES[] = {
    10, 10, 0
  };
  
  
  public Behaviour jobFor(Actor actor) {
    if (staff.offDuty(actor)) return null;
    final Choice choice = new Choice(actor);
    //
    //  Patrol during normal hours, drill during secondary shift.
    if (isDeployed()) {
      addDeploymentBehaviours(actor, choice);
    }
    else if (staff.onShift(actor)) {
      Patrolling.addFormalPatrols(actor, this, choice);
    }
    else {
      addDrilling(choice, actor, true);
      choice.add(Studying.asTechniqueTraining(actor, this, 0, canLearn()));
    }
    return choice.weightedPick();
  }
  
  
  protected void addServices(Choice choice, Actor client) {
    if (Staff.doesBelong(client, this)) return;
    addDrilling(choice, client, false);
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    
    if (structure.hasUpgrade(CALL_OF_DUTY)) {
      for (Actor soldier : staff.workers()) {
        final Profile p = base.profiles.profileFor(soldier);
        final float salary = p.salary() * (2 - 1);
        p.incWagesDue(salary / Stage.STANDARD_DAY_LENGTH);
      }
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
  }


  private Technique[] canLearn() {
    Batch <Technique> can = new Batch();
    can.add(ELECTROCUTE     );
    can.add(SHIELD_HARMONICS);
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














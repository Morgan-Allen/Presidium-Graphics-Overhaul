/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.game.actors.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.util.I;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.economic.Economy.*;
import static stratos.content.abilities.EngineerTechniques.*;



public class EngineerStation extends Venue {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final static ImageAsset ICON = ImageAsset.fromImage(
    EngineerStation.class, "media/GUI/Buttons/artificer_button.gif"
  );
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    EngineerStation.class, "media/Buildings/artificer/artificer.png", 4, 1
  );
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    EngineerStation.class, "engineer_station",
    "Engineer Station", Target.TYPE_ENGINEER, ICON,
    "The Engineer Station manufactures "+PARTS+", "+CIRCUITRY+", devices and "+
    "armour for your citizens.",
    4, 1, Structure.IS_NORMAL, Owner.TIER_FACILITY, 200, 5,
    PLASTICS, PARTS, CIRCUITRY, SERVICE_ARMAMENT, TECHNICIAN, ARTIFICER
  );
  
  
  public EngineerStation(Base base) {
    super(BLUEPRINT, base);
    staff.setShiftType(SHIFTS_BY_DAY);
    this.attachSprite(MODEL.makeSprite());
  }
  
  
  public EngineerStation(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Economic functions, upgrades and employee behaviour-
    */
  final public static Upgrade
    LEVELS[] = BLUEPRINT.createVenueLevels(
      Upgrade.THREE_LEVELS, null,
      new Object[] { 10, ASSEMBLY, 0, CHEMISTRY, 0, FIELD_THEORY },
      500,
      650,
      800
    ),
    ASSEMBLY_LINE = new Upgrade(
      "Assembly Line",
      "Allows standardised "+PARTS+" to be manufactured 33% faster.  Slightly "+
      "increases pollution.",
      300,
      Upgrade.THREE_LEVELS, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, PARTS,
      15, ASSEMBLY
    ),
    MOLDING_PRESS = new Upgrade(
      "Molding Press",
      "Speeds the production of common "+PLASTICS+" and lighter outfits "+
      "by 100%.",
      500,
      Upgrade.SINGLE_LEVEL, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, PLASTICS,
      10, ASSEMBLY, 0, CHEMISTRY
    ),
    WEAPONS_WORKSHOP = new Upgrade(
      "Weapons Workshop",
      "Raises the production quality of standard weaponry.",
      400,
      Upgrade.THREE_LEVELS, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null,
      10, ASSEMBLY, 5, MARKSMANSHIP
    ),
    ARMOUR_FOUNDRY = new Upgrade(
      "Armour Foundry",
      "Allows the production quality of standard armours.",
      400,
      Upgrade.THREE_LEVELS, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null,
      15, ASSEMBLY, 5, CHEMISTRY
    ),
    FIELD_MODULATION = new Upgrade(
      "Field Modulation",
      "Slightly reduces power consumption and allows production of "+
      SHIELD_MODULATOR_ITEM+"s, a useful defensive item.",
      550, Upgrade.SINGLE_LEVEL, LEVELS[1], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, POWER,
      5, ASSEMBLY, 5, FIELD_THEORY
    ),
    BEAM_WEAPONS = new Upgrade(
      "Beam Weapons",
      "Allows high-energy plasma to be reliably generated and controlled, "+
      "permitting upgrades to powerful ranged weapons.",
      400,
      Upgrade.THREE_LEVELS, FIELD_MODULATION, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null,
      10, ASSEMBLY, 15, FIELD_THEORY
    ),
    MICRO_ASSEMBLY = new Upgrade(
      "Micro Assembly",
      "Allows customised "+CIRCUITRY+" to be produced 50% faster, and "+
      "assists production of non-combat devices.",
      200,
      Upgrade.TWO_LEVELS, new Upgrade[] { ASSEMBLY_LINE, LEVELS[1] }, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, CIRCUITRY,
      20, ASSEMBLY, 10, FIELD_THEORY
    ),
    ROBOTIC_ARMATURE = new Upgrade(
      "Robotic Armature",
      "Allows your engineers to equip formidable "+Outfits.POWER_LIFTER+"s,"+
      "and provides a bonus to manufacture of heavier armours.",
      550,
      Upgrade.SINGLE_LEVEL, MICRO_ASSEMBLY, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null,
      20, ASSEMBLY, 5, HAND_TO_HAND
    );
  
  final public static Conversion
    POLYMER_TO_PLASTICS = new Conversion(
      BLUEPRINT, "polymer_to_plastics",
      1, POLYMER, TO, 2, PLASTICS,
      ROUTINE_DC, CHEMISTRY, SIMPLE_DC, ASSEMBLY
    ),
    METALS_TO_PARTS = new Conversion(
      BLUEPRINT, "metals_to_parts",
      1, METALS, TO, 2, PARTS,
      MODERATE_DC, ASSEMBLY, SIMPLE_DC, CHEMISTRY
    ),
    PARTS_TO_CIRCUITRY = new Conversion(
      BLUEPRINT, "parts_to_circuitry",
      1, PARTS, TO, 2, CIRCUITRY,
      MODERATE_DC, INSCRIPTION, STRENUOUS_DC, ASSEMBLY
    );
  
  
  public int numPositions(Background v) {
    final int level = structure.mainUpgradeLevel();
    if (v == TECHNICIAN) return level + 1;
    if (v == ARTIFICER ) return level;
    return 0;
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    
    stocks.incDemand(PLASTICS, 5, 1, true);
    stocks.incDemand(PARTS   , 5, 1, true);
    stocks.translateRawDemands(POLYMER_TO_PLASTICS, 1);
    stocks.translateRawDemands(METALS_TO_PARTS    , 1);
    stocks.translateRawDemands(PARTS_TO_CIRCUITRY , 1);
    
    float pollution = 5, powerNeed = 5;
    powerNeed *= (3 + structure.numOptionalUpgrades()) / 6;
    pollution *= 2f / (2 + structure.upgradeLevel(MOLDING_PRESS));
    pollution *= (5f + structure.upgradeLevel(ASSEMBLY_LINE)) / 5;
    stocks.forceDemand(POWER, powerNeed, false);
    structure.setAmbienceVal(0 - pollution);
  }
  
  
  public Behaviour jobFor(Actor actor) {
    if (staff.offDuty(actor)) return null;
    final Choice choice = new Choice(actor);
    //
    //  Consider contributing toward local repairs-
    choice.add(Repairs.getNextRepairFor(actor, true, 0.1f));
    final Plan building = (Plan) choice.pickMostUrgent(Plan.ROUTINE);
    if (actor.mind.vocation() == TECHNICIAN && building != null) {
      return building;
    }
    //
    //  Consider special commissions for weapons and armour-
    for (Item ordered : stocks.specialOrders()) {
      final Manufacture mO = new Manufacture(actor, this, ordered);
      final Upgrade forType[] = upgradeFor(ordered.type);
      choice.add(mO.setBonusFrom(this, true, forType));
    }
    final Plan crafting = (Plan) choice.pickMostUrgent(Plan.ROUTINE);
    if (actor.mind.vocation() == ARTIFICER && crafting != null) {
      return crafting;
    }
    //
    //  Consider the production of general bulk commodities-
    final Manufacture mL = stocks.nextManufacture(actor, POLYMER_TO_PLASTICS);
    if (mL != null) {
      choice.add(mL.setBonusFrom(this, false, MOLDING_PRESS));
    }
    final Manufacture mP = stocks.nextManufacture(actor, METALS_TO_PARTS);
    if (mP != null) {
      choice.add(mP.setBonusFrom(this, false, ASSEMBLY_LINE));
    }
    final Manufacture mI = stocks.nextManufacture(actor, PARTS_TO_CIRCUITRY);
    if (mI != null) {
      choice.add(mI.setBonusFrom(this, false, MICRO_ASSEMBLY));
    }
    //
    //  Consider research for new upgrades and structures-
    choice.add(Studying.asTechniqueTraining(
      actor, this, 0, ENGINEER_TECHNIQUES
    ));
    //
    //  And return whatever suits the actor best-
    return choice.weightedPick();
  }
  
  
  public void addServices(Choice choice, Actor client) {
    
    if (structure.hasUpgrade(FIELD_MODULATION)) {
      final Item gets = GearPurchase.nextGearToPurchase(
        client, this, SHIELD_MODULATOR_ITEM
      );
      choice.add(GearPurchase.nextCommission(client, this, gets));
    }
    
    final Item gets = GearPurchase.nextGearToPurchase(client, this);
    if (gets != null) {
      final Upgrade limit[] = upgradeFor(gets.type);
      choice.add(GearPurchase.nextCommission(client, this, gets, limit));
    }
    choice.add(BringUtils.nextPersonalPurchase(client, this));
  }
  
  
  final static Upgrade[]
    BASIC_OUTFIT_UPS = { MOLDING_PRESS },
    BASIC_DEVICE_UPS = { ASSEMBLY_LINE, MICRO_ASSEMBLY },
    BASIC_WEAPON_UPS = { WEAPONS_WORKSHOP },
    BEAM_WEAPON_UPS  = { WEAPONS_WORKSHOP, BEAM_WEAPONS },
    BASIC_ARMOUR_UPS = { ARMOUR_FOUNDRY },
    ROBOT_ARMOUR_UPS = { ARMOUR_FOUNDRY, ROBOTIC_ARMATURE };
  
  
  private Upgrade[] upgradeFor(Traded made) {
    if (made == Outfits.OVERALLS) {
      return BASIC_OUTFIT_UPS;
    }
    else if (made instanceof DeviceType) {
      final DeviceType DT = (DeviceType) made;
      if (DT.hasProperty(Devices.ENERGY )) return BEAM_WEAPON_UPS ;
      if (DT.hasProperty(Devices.KINETIC)) return BASIC_WEAPON_UPS;
    }
    else if (made == Outfits.POWER_ARMOUR || made == Outfits.POWER_LIFTER) {
      return ROBOT_ARMOUR_UPS;
    }
    else if (made instanceof OutfitType) {
      return BASIC_ARMOUR_UPS;
    }
    return BASIC_DEVICE_UPS;
  }
  


  /**  Rendering and interface methods-
    */
  protected Traded[] goodsToShow() {
    return new Traded[] { METALS, PARTS, CIRCUITRY };
  }
  
  
  public String helpInfo() {
    return Manufacture.statusMessageFor(
      super.helpInfo(), this, METALS_TO_PARTS, ASSEMBLY_LINE
    );
  }
}













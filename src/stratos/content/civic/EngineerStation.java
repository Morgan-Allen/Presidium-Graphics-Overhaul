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
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.economic.Economy.*;



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
      "Allows standardised "+PARTS+" to be manufactured 50% faster.  Slightly "+
      "increases pollution.",
      300,
      Upgrade.TWO_LEVELS, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, PARTS,
      15, ASSEMBLY
    ),
    MOLDING_PRESS = new Upgrade(
      "Molding Press",
      "Speeds the production of common "+PLASTICS+" and lighter outfits "+
      "by 50%.  Slightly reduces pollution.",
      250, Upgrade.TWO_LEVELS, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, PLASTICS,
      10, ASSEMBLY, 0, CHEMISTRY
    ),
    COMPOSITE_ARMORS = new Upgrade(
      "Composite Armors",
      "Improves the production of heavy armors along with common melee "+
      "weapons.",
      400,
      Upgrade.THREE_LEVELS, MOLDING_PRESS, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null,
      15, ASSEMBLY, 5, CHEMISTRY
    ),
    
    PARTICLE_PHYSICS = new Upgrade(
      "Particle Physics",
      "Improves "+POWER+" production by 5 and allows upgrades to common "+
      "industrial lasers and power cells.",
      350, Upgrade.TWO_LEVELS, LEVELS[0], BLUEPRINT,
      Upgrade.Type.TECH_MODULE, POWER,
      5, ASSEMBLY, 5, FIELD_THEORY
    ),
    
    //  TODO:  Replace with Plasma Flux, as a general pre-req for energy-
    //  techs, like the generator?
    
    PLASMA_WEAPONS = new Upgrade(
      "Plasma Weapons",
      "Allows high-flux energy pulses to be generated and controlled, "+
      "allowing upgrades to powerful ranged weapons.",
      400,
      Upgrade.THREE_LEVELS, PARTICLE_PHYSICS, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, null,
      10, ASSEMBLY, 15, FIELD_THEORY
    ),
    
    //  KINETIC_WEAPONS = new Upgrade(),
    //  Power Cells.  Kinetic Ammo.  Robotics?
    
    //  TODO:  INCLUDE THIS, AND REQUIRE PLASMA WEAPONS
    /*
    T_NULL_ARMBAND = new Upgrade(
      "T-NULL Armband",
      "",
      400,
      Upgrade.SINGLE_LEVEL
    ),
    //*/
    MICRO_ASSEMBLY = new Upgrade(
      "Micro Assembly",
      "Allows customised "+CIRCUITRY+" to be produced 50% faster.  Provides "+
      "a mild bonus to personal commissions.",
      150,
      Upgrade.TWO_LEVELS, new Upgrade[] { ASSEMBLY_LINE, LEVELS[2] }, BLUEPRINT,
      Upgrade.Type.TECH_MODULE, CIRCUITRY,
      20, ASSEMBLY, 10, FIELD_THEORY
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
    powerNeed *= (3 + structure.numUpgrades()) / 6;
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
      final Upgrade forType = upgradeFor(ordered.type);
      choice.add(mO.setBonusFrom(this, true, forType));
    }
    final Plan crafting = (Plan) choice.pickMostUrgent(Plan.ROUTINE);
    if (actor.mind.vocation() == ARTIFICER && crafting != null) {
      return crafting;
    }
    //
    //  Consider research for new upgrades and structures-
    ///choice.add(Studying.asResearch(actor, this, Target.TYPE_ENGINEER));
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
    //  And return whatever suits the actor best-
    return choice.weightedPick();
  }
  
  
  public void addServices(Choice choice, Actor client) {
    final Item
      baseDevice = GearPurchase.nextDeviceToPurchase(client, this),
      baseOutfit = GearPurchase.nextOutfitToPurchase(client, this);
    
    if (baseDevice != null) {
      final Upgrade limit = upgradeFor(baseDevice.type);
      choice.add(GearPurchase.nextCommission(client, this, baseDevice, limit));
    }
    if (baseOutfit != null) {
      final Upgrade limit = upgradeFor(baseOutfit.type);
      choice.add(GearPurchase.nextCommission(client, this, baseOutfit, limit));
    }
    choice.add(BringUtils.nextHomePurchase(client, this));
  }
  
  
  private Upgrade upgradeFor(Traded made) {
    if (made == Outfits.OVERALLS) {
      return MOLDING_PRESS;
    }
    else if (made instanceof DeviceType) {
      final DeviceType DT = (DeviceType) made;
      if (DT.hasProperty(Devices.ENERGY )) return PLASMA_WEAPONS  ;
      if (DT.hasProperty(Devices.KINETIC)) return COMPOSITE_ARMORS;
    }
    else if (made instanceof OutfitType) {
      return COMPOSITE_ARMORS;
    }
    return MICRO_ASSEMBLY;
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













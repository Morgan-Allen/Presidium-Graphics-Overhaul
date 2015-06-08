/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.civic;
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
    "Engineer Station", UIConstants.TYPE_ENGINEER, ICON,
    "The Engineer Station manufactures "+PARTS+", "+CIRCUITRY+", devices and "+
    "armour for your citizens.",
    4, 1, Structure.IS_NORMAL,
    NO_REQUIREMENTS, Owner.TIER_FACILITY,
    200, 5, 350, Structure.NORMAL_MAX_UPGRADES
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
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
  );
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES; }
  final public static Upgrade
    ASSEMBLY_LINE = new Upgrade(
      "Assembly Line",
      "Allows standardised "+PARTS+" to be manufactured 33% faster.  Slightly "+
      "increases pollution.",
      200,
      Upgrade.THREE_LEVELS, PARTS, 2,
      null, BLUEPRINT
    ),
    MOLDING_PRESS = new Upgrade(
      "Molding Press",
      "Speeds the production of common "+PLASTICS+" and working outfits "+
      "by 100%.  Slightly reduces pollution.",
      150, Upgrade.SINGLE_LEVEL, PLASTICS, 1,
      null, BLUEPRINT
    ),
    TECHNICIAN_POST = new Upgrade(
      "Technician Post",
      TECHNICIAN.info,
      50,
      Upgrade.TWO_LEVELS, TECHNICIAN, 1,
      null, BLUEPRINT
    ),
    COMPOSITE_MATERIALS = new Upgrade(
      "Composite Materials",
      "Improves the production of heavy armours along with most melee "+
      "weapons.",
      200,
      Upgrade.THREE_LEVELS, null, 2,
      null, BLUEPRINT
    ),
    PLASMA_WEAPONS = new Upgrade(
      "Plasma Weapons",
      "Allows high-flux energy pulses to be generated and controlled, "+
      "allowing upgrades to most ranged armaments.",
      300,
      Upgrade.THREE_LEVELS, null, 2,
      null, BLUEPRINT
    ),
    //  TODO:  INCLUDE THIS
    /*
    T_NULL_ARMBAND = new Upgrade(
      "T-NULL Armband",
      "",
      400,
      Upgrade.SINGLE_LEVEL
    ),
    //*/
    ARTIFICER_OFFICE = new Upgrade(
      "Artificer Office",
      ARTIFICER.info,
      150,
      Upgrade.SINGLE_LEVEL, ARTIFICER, 1,
      TECHNICIAN_POST, BLUEPRINT
    ),
    MICRO_ASSEMBLY = new Upgrade(
      "Micro Assembly",
      "Allows customised "+CIRCUITRY+" to be produced 33% faster.  Provides "+
      "a mild bonus to personal commissions.",
      150,
      Upgrade.THREE_LEVELS, PLASTICS, 1,
      new Upgrade[] { ASSEMBLY_LINE, ARTIFICER_OFFICE }, BLUEPRINT
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
  
  
  public Traded[] services() {
    return new Traded[] { PLASTICS, PARTS, CIRCUITRY, SERVICE_ARMAMENT };
  }
  
  
  public Background[] careers() {
    return new Background[] { TECHNICIAN, ARTIFICER };
  }
  
  
  public int numOpenings(Background v) {
    int num = super.numOpenings(v);
    if (v == TECHNICIAN) return num + 2;
    if (v == ARTIFICER ) return num + 2;
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
  
  
  public Behaviour jobFor(Actor actor, boolean onShift) {
    if (! onShift) return null;
    final Choice choice = new Choice(actor);
    //
    //  Consider contributing toward local repairs-
    choice.add(Repairs.getNextRepairFor(actor, false));
    if (actor.mind.vocation() == TECHNICIAN && ! choice.empty()) {
      return choice.pickMostUrgent();
    }
    //
    //  Consider special commissions for weapons and armour-
    for (Item ordered : stocks.specialOrders()) {
      final Traded made = ordered.type;
      final Manufacture mO = new Manufacture(actor, this, ordered);
      Upgrade forType = MICRO_ASSEMBLY;
      
      if (made == Outfits.OVERALLS) {
        mO.setBonusFrom(this, false, MOLDING_PRESS);
      }
      else if (made instanceof DeviceType) {
        final DeviceType DT = (DeviceType) made;
        if (DT.hasProperty(Devices.KINETIC)) forType = COMPOSITE_MATERIALS;
        if (DT.hasProperty(Devices.ENERGY )) forType = PLASMA_WEAPONS;
      }
      else if (made instanceof OutfitType) {
        //final OutfitType OT = (OutfitType) made;
        //  TODO:  Add a bonus here from T-Null Arm-band.
        forType = COMPOSITE_MATERIALS;
      }
      choice.add(mO.setBonusFrom(this, true, forType));
    }
    if (actor.mind.vocation() == ARTIFICER && ! choice.empty()) {
      return choice.pickMostUrgent();
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
    //  And return whatever suits the actor best-
    return choice.weightedPick();
  }
  
  
  public void addServices(Choice choice, Actor client) {
    //  TODO:  Disallow commisions for certain gear if you don't have the
    //         right upgrades.
    final DeviceType DT = client.gear.deviceType();
    final OutfitType OT = client.gear.outfitType();
    
    if (DT != null && DT.materials().producesAt(this)) {
      Commission.addCommissions(client, this, choice, DT);
    }
    if (OT != null && OT.materials().producesAt(this)) {
      Commission.addCommissions(client, this, choice, OT);
    }
    choice.add(BringUtils.nextHomePurchase(client, this));
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













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
    EngineerStation.class, "media/Buildings/artificer/artificer.png", 4, 2
  );
  
  final public static Conversion
    METALS_TO_PARTS = new Conversion(
      EngineerStation.class, "metals_to_parts",
      1, METALS, TO, 2, PARTS,
      MODERATE_DC, ASSEMBLY, SIMPLE_DC, CHEMISTRY
    ),
    PARTS_TO_CIRCUITRY = new Conversion(
      Archives.class, "parts_to_circuitry",
      1, PARTS, TO, 2, CIRCUITRY,
      MODERATE_DC, INSCRIPTION, STRENUOUS_DC, ASSEMBLY
    )
  ;
  
  final static Blueprint BLUEPRINT = new Blueprint(
    EngineerStation.class, "engineer_station",
    "Engineer Station", UIConstants.TYPE_ENGINEER,
    4, 2, IS_NORMAL,
    NO_REQUIREMENTS, Owner.TIER_FACILITY, METALS_TO_PARTS
  );
  
  
  public EngineerStation(Base base) {
    super(BLUEPRINT, base);
    structure.setupStats(
      200, 5, 350,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    );
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
      "Allows standardised "+PARTS+" to be manufactured quickly, but "+
      "slightly increases pollution.",
      200,
      Upgrade.THREE_LEVELS, PARTS, 2,
      null, EngineerStation.class
    ),
    MOLDING_PRESS = null,  //  TODO:  INCLUDE THIS
    TECHNICIAN_STATION = new Upgrade(
      "Technician Station",
      Backgrounds.TECHNICIAN.info,
      50,
      Upgrade.TWO_LEVELS, Backgrounds.TECHNICIAN, 1,
      null, EngineerStation.class
    ),
    COMPOSITE_ARMOUR = new Upgrade(
      "Composite Armour",
      "Improves the production of heavy armours along with most melee "+
      "weapons and industrial tools.",
      200,
      Upgrade.THREE_LEVELS, null, 2,
      null, EngineerStation.class
    ),
    PLASMA_WEAPONS = new Upgrade(
      "Plasma Weapons",
      "Allows high-flux energy pulses to be generated and controlled, "+
      "allowing upgrades to most ranged armaments.",
      300,
      Upgrade.THREE_LEVELS, null, 2,
      null, EngineerStation.class
    ),
    T_NULL_ARMBAND = null, //  TODO:  INCLUDE THIS
    ARTIFICER_STATION = new Upgrade(
      "Artificer Station",
      Backgrounds.ARTIFICER.info,
      150,
      Upgrade.SINGLE_LEVEL, Backgrounds.ARTIFICER, 1,
      TECHNICIAN_STATION, EngineerStation.class
    ),
    PROGRAM_TERMINAL = new Upgrade(
      "Program Terminal",
      "Allows for the precise yet highly customised assembly of "+CIRCUITRY+" "+
      "and other personal commissions.",
      150,
      Upgrade.THREE_LEVELS, PLASTICS, 1,
      new Upgrade[] { ASSEMBLY_LINE, ARTIFICER_STATION }, EngineerStation.class
    );
  ;
  
  
  public Traded[] services() {
    return new Traded[] { PARTS, CIRCUITRY, SERVICE_ARMAMENT };
  }
  
  
  public Background[] careers() {
    return new Background[] { Backgrounds.TECHNICIAN, Backgrounds.ARTIFICER };
  }
  
  
  public int numOpenings(Background v) {
    int num = super.numOpenings(v);
    if (v == Backgrounds.TECHNICIAN) return num + 2;
    if (v == Backgrounds.ARTIFICER ) return num + 1;
    return 0;
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    stocks.incDemand(PARTS, 5, 1, true);
    stocks.translateRawDemands(METALS_TO_PARTS, 1);
    
    float pollution = 5, powerNeed = 5;
    powerNeed *= (3 + structure.numUpgrades()) / 3;
    pollution *= 2f / (2 + structure.upgradeLevel(MOLDING_PRESS));
    pollution *= (5f + structure.upgradeLevel(ASSEMBLY_LINE)) / 5;
    stocks.forceDemand(POWER, powerNeed, false);
    structure.setAmbienceVal(0 - pollution);
  }
  
  
  public Behaviour jobFor(Actor actor, boolean onShift) {
    if (! onShift) return null;
    final Choice choice = new Choice(actor);
    
    //  Consider special commissions for weapons and armour-
    for (Item ordered : stocks.specialOrders()) {
      final Traded made = ordered.type;
      final Manufacture mO = new Manufacture(actor, this, ordered);
      
      if (made instanceof DeviceType) {
        final DeviceType DT = (DeviceType) made;
        Upgrade forType = PROGRAM_TERMINAL;
        if (DT.hasProperty(KINETIC)) forType = COMPOSITE_ARMOUR;
        if (DT.hasProperty(ENERGY )) forType = PLASMA_WEAPONS;
        mO.setBonusFrom(this, true, forType);
      }
      else if (made instanceof OutfitType) {
        final OutfitType OT = (OutfitType) made;
        if (OT.shieldBonus > OT.defence) {
          //  TODO:  Add a bonus here from T-Null Arm-band.
          mO.setBonusFrom(this, true, COMPOSITE_ARMOUR);
        }
        else {
          mO.setBonusFrom(this, true, COMPOSITE_ARMOUR);
        }
      }
      else mO.setBonusFrom(this, true, PROGRAM_TERMINAL);
      
      choice.add(mO);
    }
    if (! choice.empty()) return choice.pickMostUrgent();
    
    //  Consider the production of general bulk commodities-
    final Manufacture mP = stocks.nextManufacture(actor, METALS_TO_PARTS);
    if (mP != null) {
      choice.add(mP.setBonusFrom(this, false, ASSEMBLY_LINE));
    }
    final Manufacture mI = stocks.nextManufacture(actor, PARTS_TO_CIRCUITRY);
    if (mI != null) {
      choice.add(mI.setBonusFrom(this, false, ASSEMBLY_LINE, PROGRAM_TERMINAL));
    }
    
    //  Finally, consider contributing toward local repairs-
    choice.add(Repairs.getNextRepairFor(actor, false));
    
    //  And return whatever suits the actor best-
    return choice.weightedPick();
  }
  
  
  public void addServices(Choice choice, Actor client) {
    //  TODO:  Disallow commisions for certain gear if you don't have the
    //         right upgrades.
    final DeviceType DT = client.gear.deviceType();
    final OutfitType OT = client.gear.outfitType();
    final Class ownType = this.getClass();
    
    if (DT != null && DT.materials().facility == ownType) {
      Commission.addCommissions(client, this, choice, DT);
    }
    if (OT != null && OT.materials().facility == ownType) {
      Commission.addCommissions(client, this, choice, OT);
    }
    choice.add(BringUtils.nextHomePurchase(client, this));
  }
  


  /**  Rendering and interface methods-
    */
  protected Traded[] goodsToShow() {
    return new Traded[] { METALS, PARTS, CIRCUITRY };
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "foundry");
  }
  
  
  public String helpInfo() {
    return Manufacture.statusMessageFor(
      "Engineers manufacture parts, devices and armour for your citizens.",
      this, METALS_TO_PARTS, ASSEMBLY_LINE
    );
  }
}













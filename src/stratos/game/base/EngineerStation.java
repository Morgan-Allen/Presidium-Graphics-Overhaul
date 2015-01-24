/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.base;
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
    EngineerStation.class, "media/Buildings/artificer/artificer.png", 3, 2
  );
  
  final public static Conversion
    METALS_TO_PARTS = new Conversion(
      EngineerStation.class, "metals_to_parts",
      1, ORES, TO, 2, PARTS,
      MODERATE_DC, ASSEMBLY, SIMPLE_DC, CHEMISTRY
    )
  ;
  
  final static VenueProfile PROFILE = new VenueProfile(
    EngineerStation.class, "engineer_station",
    3, 2, ENTRANCE_WEST,
    METALS_TO_PARTS
  );
  
  
  public EngineerStation(Base base) {
    super(PROFILE, base);
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
      "Allows standardised parts and miniaturised circuitry to be "+
      "manufactured quickly and in greater abundance.",
      200,
      PARTS, 2, null,
      EngineerStation.class, ALL_UPGRADES
    ),
    MATTER_PRESS = new Upgrade(
      "Matter Press",
      "Allows raw materials to be recycled and sculpted to fit new purposes, "+
      "thereby reducing waste and speeding production of custom parts.",
      150,
      PLASTICS, 1, null,
      EngineerStation.class, ALL_UPGRADES
    ),
    TECHNICIAN_STATION = new Upgrade(
      "Technician Station",
      "Technicians are trained to operate and perform routine maintenance on "+
      "common machinery, but lack the theoretical grounding needed for "+
      "fundamental design or customisation.",
      50,
      Backgrounds.TECHNICIAN, 1, null,
      EngineerStation.class, ALL_UPGRADES
    ),
    COMPOSITE_MATERIALS = new Upgrade(
      "Composite Materials",
      "Enhances the production of lightweight and flexible armours, as well "+
      "as most melee weaponry.",
      200,
      null, 2, MATTER_PRESS,
      EngineerStation.class, ALL_UPGRADES
    ),
    ENERGY_CONTAINMENT = new Upgrade(
      "Energy Containment",
      "Allows high-flux plasmas to be generated and controlled, permitting "+
      "refinements to heavy armours and most ranged weaponry.",
      300,
      null, 2, TECHNICIAN_STATION,
      EngineerStation.class, ALL_UPGRADES
    ),
    ARTIFICER_STATION = new Upgrade(
      "Artificer Station",
      "Artificers are highly-skilled as physicists and engineers, and can "+
      "tackle the most taxing commissions reliant on dangerous or arcane "+
      "technologies.",
      150,
      Backgrounds.ARTIFICER, 1, TECHNICIAN_STATION,
      EngineerStation.class, ALL_UPGRADES
    )
  ;
  
  
  public Traded[] services() {
    return new Traded[] { PARTS, SERVICE_ARMAMENT };
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
    stocks.incDemand(PARTS, 1, Tier.PRODUCER, 1);
    stocks.translateDemands(METALS_TO_PARTS, 1);
    
    float pollution = 5, powerNeed = 5;
    powerNeed *= (3 + structure.numUpgrades()) / 3;
    pollution *= 2f / (2 + structure.upgradeLevel(MATTER_PRESS));
    stocks.forceDemand(POWER, powerNeed, Tier.CONSUMER);
    structure.setAmbienceVal(0 - pollution);
  }
  
  
  public Behaviour jobFor(Actor actor, boolean onShift) {
    if (! onShift) return null;
    //  Consider contributing toward local repairs-
    final Choice choice = new Choice(actor);
    
    //  Consider special commissions for weapons and armour-
    for (Manufacture o : stocks.specialOrders()) {
      final Traded made = o.made().type;
      
      if (made instanceof DeviceType) {
        final DeviceType DT = (DeviceType) made;
        Upgrade forType = MATTER_PRESS;
        if (DT.hasProperty(KINETIC)) forType = COMPOSITE_MATERIALS;
        if (DT.hasProperty(ENERGY )) forType = ENERGY_CONTAINMENT   ;
        o.setBonusFrom(this, true, MATTER_PRESS, forType);
      }
      else if (made instanceof OutfitType) {
        o.setBonusFrom(this, true,
          MATTER_PRESS, COMPOSITE_MATERIALS, ENERGY_CONTAINMENT
        );
      }
      else o.setBonusFrom(this, true, MATTER_PRESS);
      
      choice.add(o);
    }
    
    //  Finally, consider the production of general bulk commodities-
    final Manufacture mP = stocks.nextManufacture(actor, METALS_TO_PARTS);
    if (mP != null) {
      choice.add(mP.setBonusFrom(this, false, ASSEMBLY_LINE));
    }
    
    choice.add(Repairs.getNextRepairFor(actor, false));
    
    //  And return whatever suits the actor best-
    choice.isVerbose = I.talkAbout == this;
    return choice.weightedPick();
  }
  
  
  public void addServices(Choice choice, Actor forActor) {
    Commission.addCommissions(forActor, this, choice, services());
  }
  


  /**  Rendering and interface methods-
    */
  protected Traded[] goodsToShow() {
    return new Traded[] { ORES, PARTS };
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "foundry");
  }


  public String fullName() {
    return "Engineer Station";
  }
  
  
  public String helpInfo() {
    return
      "Your Engineers manufacture parts, devices and armour for your citizens.";
  }
  
  
  public String objectCategory() {
    return InstallTab.TYPE_ARTIFICER;
  }
}








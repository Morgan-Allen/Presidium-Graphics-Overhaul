/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.base ;
import stratos.game.civilian.Commission;
import stratos.game.common.* ;
import stratos.game.actors.* ;
import stratos.game.building.* ;
import stratos.graphics.common.* ;
import stratos.graphics.cutout.* ;
import stratos.graphics.widgets.* ;
import stratos.user.* ;
import stratos.util.* ;



public class Foundry extends Venue implements Economy {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final static ImageAsset ICON = ImageAsset.fromImage(
    "media/GUI/Buttons/artificer_button.gif", Foundry.class
  );
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    Foundry.class, "media/Buildings/artificer/artificer.png", 4, 2
  );
  
  
  public Foundry(Base base) {
    super(4, 2, ENTRANCE_WEST, base) ;
    structure.setupStats(
      200, 5, 350,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    ) ;
    personnel.setShiftType(SHIFTS_BY_DAY) ;
    this.attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public Foundry(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Economic functions, upgrades and employee behaviour-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    Foundry.class, "foundry_upgrades"
  ) ;
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
  final public static Upgrade
    ASSEMBLY_LINE = new Upgrade(
      "Assembly Line",
      "Allows standardised parts and miniaturised circuitry to manufactured "+
      "quickly and in greater abundance.",
      200,
      PARTS, 2, null, ALL_UPGRADES
    ),
    MOLDING_PRESS = new Upgrade(
      "Molding Press",
      "Allows materials to be recycled and sculpted to fit new purposes, "+
      "reducing waste and pollution, and speeding production of custom parts.",
      150,
      PLASTICS, 1, null, ALL_UPGRADES
    ),
    TECHNICIAN_STATION = new Upgrade(
      "Technician Station",
      "Technicians are trained to operate and perform routine maintenance on "+
      "common machinery, but lack the theoretical grounding needed for "+
      "fundamental design or customisation.",
      50,
      Background.TECHNICIAN, 1, null, ALL_UPGRADES
    ),
    COMPOSITE_MATERIALS = new Upgrade(
      "Composite Materials",
      "Enhances the production of lightweight and flexible armours, as well "+
      "as close-range melee weaponry.",
      200,
      null, 2, MOLDING_PRESS, ALL_UPGRADES
    ),
    FLUX_CONTAINMENT = new Upgrade(
      "Flux Containment",
      "Allows high-energy plasmas to be generated and controlled, permitting "+
      "refinements to shield technology and ranged energy weapons.",
      250,
      null, 2, TECHNICIAN_STATION, ALL_UPGRADES
    ),
    ARTIFICER_STATION = new Upgrade(
      "Artificer Station",
      "Artificers are highly-skilled as physicists and engineers, and can "+
      "tackle the most taxing commissions reliant on dangerous or arcane "+
      "technologies.",
      150,
      Background.ARTIFICER, 1, TECHNICIAN_STATION, ALL_UPGRADES
    ) ;
  
  
  public Service[] services() {
    return new Service[] { PARTS, CIRCUITRY, SERVICE_ARMAMENT } ;
  }
  
  
  public Background[] careers() {
    return new Background[] { Background.TECHNICIAN, Background.ARTIFICER } ;
  }
  
  
  public int numOpenings(Background v) {
    int num = super.numOpenings(v) ;
    if (v == Background.TECHNICIAN) return num + 1 ;
    if (v == Background.ARTIFICER ) return num + 1 ;
    return 0 ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! structure.intact()) return ;
    stocks.translateDemands(1, PARTS_TO_CIRCUITRY) ;
    stocks.incDemand(PARTS, 10, VenueStocks.TIER_PRODUCER, 1) ;
    stocks.translateDemands(1, METALS_TO_PARTS) ;
    ///I.say("Demand for metal is: "+stocks.demandFor(METAL_ORE)) ;
    //
    //  Output squalor and demand power-
    float pollution = 5, powerNeed = 5 ;
    if (! isManned()) {
      pollution /= 2 ;
      powerNeed /= 2 ;
    }
    powerNeed *= (3 + structure.numUpgrades()) / 3 ;
    pollution *= 2f / (2 + structure.upgradeLevel(MOLDING_PRESS)) ;
    stocks.forceDemand(POWER, powerNeed, VenueStocks.TIER_CONSUMER) ;
    stocks.removeItem(Item.withAmount(POWER, 0.1f * powerNeed)) ;
    structure.setAmbienceVal(0 - pollution) ;
  }
  
  
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null ;
    final float powerCut = stocks.shortagePenalty(POWER) * 10 ;
    
    //  Consider special commissions for weapons and armour-
    final Manufacture o = stocks.nextSpecialOrder(actor) ;
    if (o != null && personnel.assignedTo(o) < 1) {
      o.checkBonus = structure.upgradeLevel(MOLDING_PRESS) + 5 ;
      final int CMB = structure.upgradeLevel(COMPOSITE_MATERIALS) + 2 ;
      final int FCB = structure.upgradeBonus(FLUX_CONTAINMENT) + 2 ;
      final Service made = o.made().type ;
      
      if (made instanceof DeviceType) {
        final DeviceType DT = (DeviceType) made ;
        if (DT.hasProperty(PHYSICAL)) o.checkBonus += CMB ;
        if (DT.hasProperty(ENERGY)) o.checkBonus += FCB ;
      }
      
      if (made instanceof OutfitType) {
        final OutfitType OT = (OutfitType) made ;
        o.checkBonus += OT.shieldBonus * FCB / 10f ;
        o.checkBonus += OT.defence * CMB / 10f ;
      }
      o.checkBonus -= powerCut ;
      o.priorityMod = Plan.ROUTINE ;
      return o ;
      //choice.add(o) ;
    }
    
    //  Consider contributing toward local repairs-
    final Choice choice = new Choice(actor) ;
    choice.add(Repairs.getNextRepairFor(actor, Plan.CASUAL)) ;
    
    //  Finally, consider the production of general bulk commodities-
    final int PB = 1 + structure.upgradeLevel(ASSEMBLY_LINE) ;
    final Manufacture mP = stocks.nextManufacture(actor, METALS_TO_PARTS) ;
    if (mP != null) {
      mP.checkBonus = (PB * 5) / 2 ;
      mP.checkBonus -= powerCut ;
      choice.add(mP) ;
    }
    final Manufacture mC = stocks.nextManufacture(actor, PARTS_TO_CIRCUITRY) ;
    if (mC != null) {
      mC.checkBonus = (PB * 5) / 2 ;
      mC.checkBonus -= powerCut ;
      choice.add(mC) ;
    }
    
    //  And return whatever suits the actor best-
    return choice.weightedPick() ;
  }
  
  
  public void addServices(Choice choice, Actor forActor) {
    Commission.addCommissions(forActor, this, choice) ;
  }
  


  /**  Rendering and interface methods-
    */
  protected Service[] goodsToShow() {
    return new Service[] { METALS, CIRCUITRY, PARTS } ;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "foundry");
  }


  public String fullName() {
    return "Foundry" ;
  }
  
  
  public String helpInfo() {
    return
      "The Foundry manufactures parts, inscriptions, devices and armour "+
      "for your citizens.";
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_ARTIFICER ;
  }
}








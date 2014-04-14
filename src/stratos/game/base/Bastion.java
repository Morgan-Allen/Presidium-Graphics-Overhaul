/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.base ;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.game.tactical.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



public class Bastion extends Venue implements Economy {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    Bastion.class, "media/Buildings/military/bastion.png", 7, 4
  );
  final public static ImageAsset ICON = ImageAsset.fromImage(
    "media/GUI/Buttons/bastion_button.gif", Bastion.class
  );
  
  
  public Bastion(Base base) {
    super(7, 4, ENTRANCE_EAST, base) ;
    structure.setupStats(
      650, 15, 1000,
      Structure.BIG_MAX_UPGRADES, Structure.TYPE_FIXTURE
    ) ;
    personnel.setShiftType(SHIFTS_BY_HOURS) ;
    attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public Bastion(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  //  TODO:  Restore this later.  You shouldn't have more than one Bastion at
  //  your disposal.
  /*
  public boolean privateProperty() {
    return true ;
  }
  //*/
  
  
  /**  Upgrades, economic functions and behaviour implementation-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    Bastion.class, "bastion_upgrades"
  ) ;
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
  final public static Upgrade
    LOGISTIC_SUPPORT = new Upgrade(
      "Logistic Support",
      "Provides more openings for your Reservists and Auditors, thereby "+
      "aiding construction efforts and revenue flow.",
      200,
      null, 1, null, ALL_UPGRADES
    ),
    SECURITY_MEASURES = new Upgrade(
      "Security Measures",
      "Increases patrols of Veterans in and around your settlement and "+
      "augments your Bastion's output of power and life support.",
      300,
      null, 1, null, ALL_UPGRADES
    ),
    NOBLE_QUARTERS = new Upgrade(
      "Noble Quarters",
      "Increases the space available to your family, advisors, honour guard "+
      "and honoured guests.",
      400,
      null, 1, null, ALL_UPGRADES
    ),
    GUEST_QUARTERS = new Upgrade(
      "Guest Quarters",
      "Makes more space for prisoners and hostages, and creates openings for "+
      "Stewards in your employ.",
      250,
      null, 1, null, ALL_UPGRADES
    ),
    BLAST_SHIELDS = new Upgrade(
      "Blast Shields",
      "Increases the structural integrity of the Bastion, particularly vital "+
      "in the event of atomic attack.",
      450,
      null, 1, null, ALL_UPGRADES
    ),
    SEAT_OF_POWER = new Upgrade(
      "Seat of Power",
      "Augments the strength and range of your psyonic powers and capacity "+
      "to function without sleep or rest.",
      500,
      null, 1, null, ALL_UPGRADES
    ) ;

  
  
  public int numOpenings(Background b) {
    final int nO = super.numOpenings(b) ;
    if (b == Background.VETERAN) {
      return nO + 2 + structure.upgradeLevel(SECURITY_MEASURES) ;
    }
    if (b == Background.TECH_RESERVE) {
      return nO + 2 + structure.upgradeLevel(LOGISTIC_SUPPORT) ;
    }
    if (b == Background.AUDITOR) {
      return nO + ((1 + structure.upgradeLevel(LOGISTIC_SUPPORT)) / 2) ;
    }
    if (b == Background.STEWARD) {
      return nO + ((1 + structure.upgradeLevel(GUEST_QUARTERS)) / 2) ;
    }
    //
    //  TODO:  Return the amount of space open to hostages and guests as well.
    return 0 ;
  }
  
  
  public Behaviour jobFor(Actor actor) {
    if (! structure.intact()) return null;
    if (actor == base().ruler()) {
      final Supervision s = new Supervision(actor, this);
      s.setMotive(Plan.MOTIVE_DUTY, Plan.ROUTINE);
      return s;
    }
    final Background v = actor.vocation() ;
    if (v == Background.STEWARD || v == Background.FIRST_CONSORT) {
      return new Supervision(actor, this) ;
    }
    
    if (! personnel.onShift(actor)) return null ;
    
    if (v == Background.VETERAN || v == Background.WAR_MASTER) {
      return Patrolling.aroundPerimeter(actor, this, world) ;
    }
    if (v == Background.TECH_RESERVE) {
      return Repairs.getNextRepairFor(actor, Plan.CASUAL) ;
    }
    if (v == Background.AUDITOR || v == Background.MINISTER_FOR_ACCOUNTS) {
      final Venue toAudit = Audit.nextToAuditFor(actor) ;
      return toAudit == null ? null : new Audit(actor, toAudit) ;
    }
    return new Supervision(actor, this) ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! structure.intact()) return ;
    //
    //  Provide power and life support-
    final float condition = (structure.repairLevel() + 1f) / 2 ;
    final int SB = structure.upgradeLevel(SECURITY_MEASURES) ;
    int powerLimit = 20 + (SB * 10), lifeSLimit = 10 + (SB * 5) ;
    powerLimit *= condition ;
    lifeSLimit *= condition ;
    if (stocks.amountOf(POWER) < powerLimit) {
      stocks.addItem(Item.withAmount(POWER, 1)) ;
    }
    if (stocks.amountOf(LIFE_SUPPORT) < lifeSLimit) {
      stocks.addItem(Item.withAmount(LIFE_SUPPORT, 1)) ;
    }
    //
    //  Demand provisions-
    final int foodNeed = personnel.residents().size() + 5 ;
    stocks.forceDemand(CARBS  , foodNeed * 1.5f, VenueStocks.TIER_CONSUMER) ;
    stocks.forceDemand(PROTEIN, foodNeed * 1.0f, VenueStocks.TIER_CONSUMER) ;
    stocks.forceDemand(GREENS , foodNeed * 1.0f, VenueStocks.TIER_CONSUMER) ;
    stocks.forceDemand(TRUE_SPICE  , foodNeed * 0.5f, VenueStocks.TIER_CONSUMER) ;
    //
    //  Modify maximum integrity based on upgrades-
    final int BB = structure.upgradeLevel(BLAST_SHIELDS) ;
    structure.updateStats(650 + 250 * BB, 15 + 5 * BB, 0) ;
    
    int ambience = structure.numUpgrades() / 4 ;
    if (ambience == 3) ambience = 10 ;
    if (ambience == 2) ambience = 5 ;
    if (ambience == 1) ambience = 2 ;
    ambience += structure.upgradeLevel(GUEST_QUARTERS) ;
    ambience += structure.upgradeLevel(NOBLE_QUARTERS) ;
    structure.setAmbienceVal(ambience) ;
  }
  
  
  public Background[] careers() {
    return new Background[] {
      Background.TECH_RESERVE, Background.VETERAN,
      Background.AUDITOR, Background.STEWARD
      //Background.HONOUR_GUARD, Background.CONSORT
      //Background.HEIR, Background.ADVISOR
    };
  }
  
  
  public Service[] services() {
    return new Service[] {
      SERVICE_ADMIN, SERVICE_REFUGE, POWER, LIFE_SUPPORT
    };
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "bastion");
  }
  
  
  public String fullName() { return "Bastion" ; }
  
  
  public String helpInfo() {
    return
      "The Bastion is your seat of command for the settlement as a "+
      "whole, houses your family, advisors and bodyguards, and provides "+
      "basic logistic support." ;
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_MILITANT ;
  }
}



/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.civilian.*;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;


//
//  TODO:  Rename this to the Press Office, and move auditor functions to the
//  Enforcer Bloc.



public class AuditOffice extends Venue implements Economy {
  

  final public static ModelAsset MODEL = CutoutModel.fromImage(
    "media/Buildings/merchant/audit_office.png", AuditOffice.class, 2.75f, 2
  );
  
  final public static String
    KEY_RELIEF     = "relief_level",
    KEY_LOWER_TAX  = "lower_tax_level",
    KEY_MIDDLE_TAX = "middle_tax_level",
    KEY_UPPER_TAX  = "upper_tax_level",
    KEY_ESTATE_TAX = "estate_tax_level" ;
  final public static int
    SETTING_NONE   = 0,
    SETTING_LOW    = 1,
    SETTING_MEDIUM = 2,
    SETTING_HIGH   = 3 ;
  final static String
    DESC_SUPPORT[] = {
      "No Relief",
      "Minimal  (2 credits/day, savings +100)",
      "Standard (5 credits/day, savings +250)",
      "Generous (8 credits/day, savings +400)"
    },
    DESC_TAX[] = {
      "No Taxes",
      "Light (20%, 1000 credits exemption)",
      "Moderate (50%, 500 credits exemption)",
      "Heavy (80%, 250 credits exemption)"
    },
    DESC_ESTATE_TAX[] = {
      "No Estate Tax",
      "Light (Gentry pay 1000/annum)",
      "Moderate (Gentry pay 2500/annum)",
      "Heavy (Gentry pay 5000/annum)"
    } ;
  final public static int
    
    DEFAULT_RELIEF     = SETTING_LOW,
    DEFAULT_LOWER_TAX  = SETTING_LOW,
    DEFAULT_MIDDLE_TAX = SETTING_MEDIUM,
    DEFAULT_UPPER_TAX  = SETTING_MEDIUM,
    DEFAULT_ESTATE_TAX = SETTING_HIGH,
    
    RELIEF_AMOUNTS[]  = {  0, 2   , 5   , 8    },
    RELIEF_SAVING[]   = {  0, 100 , 250 , 400  },
    TAX_PERCENTS[]    = {  0, 20  , 50  , 80   },
    TAX_EXEMPTED[] = { -1, 1000, 500 , 250  },
    ESTATE_TAX_FEES[] = {  0, 1000, 2500, 5000 },
    
    MAX_INFLATION = 10,
    WELFARE_CLAIMS[] = { 0, 1, 2, 3 } ;
  
  
  
  public AuditOffice(Base base) {
    super(3, 2, ENTRANCE_EAST, base) ;
    structure.setupStats(
      100, 2, 200,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    ) ;
    personnel.setShiftType(SHIFTS_BY_DAY) ;
    this.attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public AuditOffice(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  
  
  /**  Economic functions, upgrades and behaviour implementation-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    AuditOffice.class, "audit_office_upgrades"
  ) ;
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
  final public static Upgrade
    PRESS_OFFICE = new Upgrade(
      "Press Office",
      "Assists in the production of pressfeed and brings Advertisers into "+
      "your employ, helping to gather information and fortify base morale.",
      150, null, 1, null, ALL_UPGRADES
    ),
    FILE_SYSTEM = new Upgrade(
      "File System",
      "Allows tax documents, support claims and criminal prosecutions to be "+
      "processed quickly and expands the number of Auditors in your employ.",
      300, null, 1, null, ALL_UPGRADES
    ),
    RELIEF_AUDIT = new Upgrade(
      "Relief Audit",
      "Allows local homeless or unemployed to apply for basic income, while "+
      "sourcing a portion of support funding from offworld.",
      150, null, 1, FILE_SYSTEM, ALL_UPGRADES
    ),
    CURRENCY_ADJUSTMENT = new Upgrade(
      "Currency Injection",
      "Permits the office to modify total credits circulation to reflect "+
      "the settlement's property values.",
      200, null, 1, PRESS_OFFICE, ALL_UPGRADES
    )
  ;
  
  
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null ;
    final Choice choice = new Choice(actor) ;
    
    final int linkMod = (int) (5 * (1 - stocks.shortagePenalty(DATALINKS))) ;
    
    if (actor.vocation() == Background.AUDITOR) {
      final Venue toAudit = Audit.nextToAuditFor(actor) ;
      if (toAudit != null) {
        final Audit a = new Audit(actor, toAudit) ;
        a.checkBonus = ((structure.upgradeLevel(FILE_SYSTEM) - 1) * 5) / 2 ;
        a.checkBonus += linkMod ;
        choice.add(a) ;
      }
      //
      //  TODO:  The Auditor needs to sift data and approve stories for
      //  publication.  Use local information only.
    }
    
    if (actor.vocation() == Background.ADVERTISER) {
      Batch <Venue> clients = new Batch <Venue> () ;
      world.presences.sampleFromKey(this, world, 5, clients, Holding.class) ;
      
      final Delivery c = Deliveries.nextCollectionFor(
        actor, this, new Service[] { PLASTICS }, 5, null, world
      ) ;
      choice.add(c) ;
      
      final Delivery d = Deliveries.nextDeliveryFor(
        actor, this, services(), clients, 5, world
      ) ;
      if (d != null) d.shouldPay = null ;
      //
      //  TODO:  Modify this a bit, so that the advertiser needn't go back and
      //  forth all the time?
      choice.add(d) ;
      
      final Manufacture mP = stocks.nextManufacture(
        actor, PLASTICS_TO_PRESSFEED
      ) ;
      if (mP != null) {
        mP.checkBonus = ((structure.upgradeLevel(PRESS_OFFICE) - 1) * 5) / 2 ;
        mP.checkBonus += linkMod ;
        choice.add(mP) ;
      }
    }
    
    return choice.weightedPick() ;
  }
  
  
  public void addServices(Choice choice, Actor forActor) {
    choice.add(new Payday(forActor, this)) ;
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v) ;
    if (v == Background.AUDITOR) {
      return nO + 1 + (structure.upgradeLevel(FILE_SYSTEM) / 2) ;
    }
    if (v == Background.ADVERTISER) {
      return nO + 1 + (structure.upgradeLevel(PRESS_OFFICE) / 2) ;
    }
    return 0 ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! structure.intact()) return ;
    
    float needPower = 2, numWork = personnel.workers().size() ;
    if (! isManned()) needPower /= 2 ;
    stocks.forceDemand(DATALINKS, numWork / 2, VenueStocks.TIER_CONSUMER) ;
    stocks.forceDemand(POWER, needPower, VenueStocks.TIER_CONSUMER) ;
    stocks.bumpItem(POWER, needPower * -0.1f) ;
    
    printCredits() ;
    stocks.translateDemands(1, PLASTICS_TO_PRESSFEED) ;
    structure.setAmbienceVal(2) ;
  }
  
  
  public float assessRelief(Actor assessed, boolean deduct) {
    int claimRate = structure.upgradeLevel(RELIEF_AUDIT) ;
    if (claimRate == 0) return 0 ;
    
    final Profile p = base().profiles.profileFor(assessed) ;
    final float
      relief   = base().profiles.querySetting(KEY_RELIEF),
      interval = p.daysSinceWageEval(world),
      bonus    = 1 + ((claimRate - 1) / 2f),
      payment  = interval * relief * bonus,
      claimed  = interval * WELFARE_CLAIMS[(int) relief] * bonus ;
    
    if (deduct) {
      p.incPaymentDue(payment) ;
      stocks.incCredits(claimed - payment) ;
    }
    return payment ;
  }
  
  
  private void printCredits() {
    int printRate = structure.upgradeLevel(CURRENCY_ADJUSTMENT) ;
    if (printRate == 0) return ;
    
    final float
      propertyValues = base().propertyValues(),
      circulation = base().creditCirculation() ;
    
    printRate *= MAX_INFLATION ;
    float adjust = propertyValues - circulation ;
    adjust = Visit.clamp(adjust, -printRate, printRate) ;
    
    final float needPlastic = printRate / 10f ;
    stocks.incDemand(PLASTICS, needPlastic, VenueStocks.TIER_CONSUMER, 1) ;
    
    adjust /= World.STANDARD_DAY_LENGTH ;
    
    if (adjust > 0) {
      if (stocks.amountOf(PLASTICS) >= adjust / 200f) {
        stocks.bumpItem(PLASTICS, adjust / 200f) ;
        stocks.incCredits(adjust) ;
      }
    }
    else stocks.incCredits(adjust * 0.5f) ;
  }
  
  
  public Background[] careers() {
    return new Background[] { Background.AUDITOR, Background.ADVERTISER } ;
  }
  
  
  public Service[] services() {
    return new Service[] { PRESSFEED, SERVICE_ADMIN } ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void writeInformation(Description d, int categoryID, HUD UI) {
    super.writeInformation(d, categoryID, UI) ;
    if (categoryID != 3 || ! structure.intact()) return ;
    
    d.append("\n\nSupport and Taxation:") ;
    describeSetting(
      d, KEY_RELIEF, DEFAULT_RELIEF,
      "Basic Income", DESC_SUPPORT,
      "Relief level determines the basic income guaranteed to all citizens ("+
      "including the homeless or unemployed) and increases the savings "+
      "threshold for tax exemption."
    ) ;
    describeSetting(
      d, KEY_LOWER_TAX, DEFAULT_LOWER_TAX,
      "Lower Class Tax Level", DESC_TAX,
      "Determines the proportion of income paid in tax by lower-class "+
      "citizens, and lowers the savings threshold for tax exemption."
    ) ;
    describeSetting(
      d, KEY_MIDDLE_TAX, DEFAULT_MIDDLE_TAX,
      "Middle Class Tax Level", DESC_TAX,
      "Determines the proportion of income paid in tax by middle-class "+
      "citizens, and lowers the savings threshold for tax exemption."
    ) ;
    describeSetting(
      d, KEY_UPPER_TAX, DEFAULT_UPPER_TAX,
      "Upper Class Tax Level", DESC_TAX,
      "Determines the proportion of income paid in tax by upper-class "+
      "citizens, and lowers the savings threshold for tax exemption."
    ) ;
    /*
    describeSetting(
      d, KEY_ESTATE_TAX, DEFAULT_ESTATE_TAX,
      "Estate Tax Fees", DESC_ESTATE_TAX,
      "Determines the payment made by landed gentry toward maintenance of "+
      "their sprawling estates."
    ) ;
    if (lastHelpInfo != null) {
      d.append("\n") ;
      d.append(lastHelpInfo) ;
    }
    //*/
  }
  
  private static String lastHelpInfo = null ;
  
  private void describeSetting(
    Description d,
    final String key, int defaultValue,
    String keyLabel, final String valLabels[],
    final String helpInfo
  ) {
    final BaseProfiles BP = base().profiles ;
    final int level = BP.querySetting(key, defaultValue) ;
    
    d.append("\n  "+keyLabel+"\n    ") ;
    d.append(new Description.Link(valLabels[level]) {
      public void whenClicked() {
        final int newLevel = (level + 1) % valLabels.length ;
        BP.assertSetting(key, newLevel) ;
        lastHelpInfo = helpInfo ;
      }
    }) ;
  }
  
  
  public String fullName() {
    return "Audit Office" ;
  }


  public Composite portrait(HUD UI) {
    return null;//new Composite(UI, "media/GUI/Buttons/audit_office_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "The Audit Office regulates financial data and press releases "+
      "pertinent to your settlement's welfare, collecting tax, dispensing "+
      "relief funds and disseminating propaganda." ;
  }
  

  public String buildCategory() {
    return UIConstants.TYPE_MERCHANT ;
  }
}






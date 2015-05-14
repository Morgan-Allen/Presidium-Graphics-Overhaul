/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.civic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.base.BaseCommerce.*;
import static stratos.game.economic.Economy.*;



//  Rations vending.  Repairs.  Medkits.
//  Advertising (morale boost and added purchase-attraction)
//  Security vault.  Currency exchange.



public class StockExchange extends Venue {
  
  
  /**  Data fields, constructors and save/load functionality-
    */
  private static boolean
    verbose = false;
  
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    StockExchange.class, "media/Buildings/merchant/stock_exchange.png", 4, 1
  );
  final public static ImageAsset ICON = ImageAsset.fromImage(
    StockExchange.class, "media/GUI/Buttons/stock_exchange_button.gif"
  );
  
  final static Blueprint BLUEPRINT = new Blueprint(
    StockExchange.class, "stock_exchange",
    "Stock Exchange", UIConstants.TYPE_COMMERCE,
    5, 1, IS_NORMAL,
    NO_REQUIREMENTS, Owner.TIER_TRADER
  );
  
  private float catalogueSums[] = new float[ALL_STOCKED.length];
  
  
  public StockExchange(Base base) {
    super(BLUEPRINT, base);
    staff.setShiftType(SHIFTS_BY_DAY);
    structure.setupStats(
      150, 3, 250, Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    );
    attachSprite(MODEL.makeSprite());
  }
  
  
  public StockExchange(Session s) throws Exception {
    super(s);
    for (int i = ALL_STOCKED.length; i-- > 0;) {
      catalogueSums[i] = s.loadFloat();
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    for (int i = ALL_STOCKED.length; i-- > 0;) {
      s.saveFloat(catalogueSums[i]);
    }
  }
  
  
  
  /**  Supplementary setup methods-
    */
  public boolean adjustCatalogue(Traded good, float inc) {
    final int index = Visit.indexOf(good, ALL_STOCKED);
    if (index == -1) return false;
    float level = catalogueSums[index];
    level = Nums.clamp(level + inc, 0, stocks.amountOf(good));
    catalogueSums[index] = level;
    return true;
  }
  
  
  public float catalogueLevel(Traded good) {
    final int index = Visit.indexOf(good, ALL_STOCKED);
    if (index == -1) return 0;
    return catalogueSums[index] / stocks.amountOf(good);
  }
  
  
  public void afterTransaction(Item item, float amount) {
    super.afterTransaction(item, amount);
    
    if (amount >= 0) {
      stocks.incCredits(super.priceFor(item.type, false) / 2);
      return;
    }
    final float
      basePrice    = super.priceFor(item.type, true),
      upgradeLevel = upgradeLevelFor(item.type),
      cashBonus    = basePrice * BASE_SALE_MARGIN * (upgradeLevel + 1),
      catalogued   = catalogueLevel(item.type);
    
    final float
      paidOn    = Nums.min(catalogued, 0 - amount),
      remainder = (0 - amount) - paidOn;
    adjustCatalogue(item.type, paidOn);
    stocks.incCredits(cashBonus * paidOn);
    stocks.incCredits(cashBonus * remainder / 2);
  }
  
  
  public float priceFor(Traded good, boolean sold) {
    if (sold) return super.priceFor(good, sold) / 2;
    else return super.priceFor(good, sold);
  }
  
  
  public int spaceFor(Traded good) {
    final float upgradeLevel = upgradeLevelFor(good);
    if (upgradeLevel == -1) return 0;
    if (upgradeLevel ==  0) return 0;
    return 5 + (int) (upgradeLevel * 15);
  }
  
  
  
  /**  Upgrades, behaviour and economic functions-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> ();
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES; }
  
  final public static Upgrade
    
    //  TODO:  COOK UP RATIONS AS A 4TH FOOD TYPE
    RATIONS_VENDING = new Upgrade(
      "Rations Vending",
      "Increases space available to carbs and protein and augments "+
      "profits from their sale.",
      150, Upgrade.THREE_LEVELS, null, 1,
      null, StockExchange.class
    ),
    
    //  TODO:  PERMIT BASIC REPAIRS/RECHARGE OF ARMOUR/DEVICES
    HARDWARE_STORE = new Upgrade(
      "Hardware Store",
      "Increases space available to parts and plastics, and augments profits "+
      "from their sale.",
      150, Upgrade.THREE_LEVELS, null, 1,
      null, StockExchange.class
    ),
    
    //  TODO:  PROVIDE STANDARD MEDKITS FOR USE
    MEDICAL_EXCHANGE = new Upgrade(
      "Medical Exchange",
      "Increases space available to greens and medicine, and augments "+
      "profits from their sale.",
      250, Upgrade.THREE_LEVELS, null, 1,
      null, StockExchange.class
    ),
    
    VIRTUAL_CURRENCY = new Upgrade(
      "Virtual Currency",
      "Makes small periodic adjustments to revenue and outlays in response "+
      "to large-scale investment patterns, magnifying both profit and loss.",
      400, Upgrade.THREE_LEVELS, null, 1,
      null, StockExchange.class
    ),
    
    ADVERTISEMENT = new Upgrade(
      "Advertisement",
      "Increases the likelihood of shoppers' visits and enhances morale when "+
      "doing so.",
      300, Upgrade.THREE_LEVELS, null, 1,
      null, StockExchange.class
    );
  
  final public static Traded
    ALL_STOCKED[] = {
      CARBS, PROTEIN, GREENS,
      MEDICINE, PLASTICS, PARTS
    },
    ALL_SERVICES[] = (Traded[]) Visit.compose(Traded.class,
      ALL_STOCKED, new Traded[] { SERVICE_COMMERCE }
    );
  
  
  private float upgradeLevelFor(Traded type) {
    Upgrade upgrade = null;
    if (type == CARBS  || type == PROTEIN ) upgrade = RATIONS_VENDING ;
    if (type == GREENS || type == MEDICINE) upgrade = MEDICAL_EXCHANGE;
    if (type == PARTS  || type == PLASTICS) upgrade = HARDWARE_STORE  ;
    return upgrade == null ? -1 : (structure.upgradeLevel(upgrade) / 3f);
  }
  
  
  public int numOpenings(Background p) {
    final int nO = super.numOpenings(p);
    if (p == Backgrounds.STOCK_VENDOR) return nO + 3;
    return 0;
  }
  
  
  public Behaviour jobFor(Actor actor, boolean onShift) {
    if (! onShift) return null;
    final Choice choice = new Choice(actor);
    //
    //  ...You basically don't want the stock vendor wandering too far, because
    //  the venue has to be manned in order for citizens to come shopping.  So
    //  stick with jobs that happen within the venue.
    if (PlanUtils.competition(Supervision.class, this, actor) > 0) {
      choice.add(BringUtils.bestBulkDeliveryFrom(
        this, services(), 2, 10, 5
      ));
      choice.add(BringUtils.bestBulkCollectionFor(
        this, services(), 2, 10, 5
      ));
    }
    //
    //  TODO:  Have the Supervision class delegate the precise behaviour you
    //  conduct back to the venue.
    choice.add(Supervision.inventory(this, actor));
    return choice.weightedPick();
  }
  
  
  protected void addServices(Choice choice, Actor actor) {
    final Property home = actor.mind.home();
    if (home instanceof Venue) {
      final Bringing d = BringUtils.fillBulkOrder(
        this, home, ((Venue) home).stocks.demanded(), 1, 5
      );
      if (d != null) {
        final float advertBonus = structure.upgradeLevel(ADVERTISEMENT) / 3f;
        d.setWithPayment(actor);
        d.addMotives(Plan.MOTIVE_LEISURE, Plan.ROUTINE * advertBonus);
        choice.add(d);
      }
    }
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    
    final boolean report = verbose && I.talkAbout == this;
    structure.setAmbienceVal(Ambience.MILD_AMBIENCE);
    
    if (report) I.say("\nUpdating stock exchange...");
    for (Traded type : ALL_MATERIALS) {
      final int typeSpace = spaceFor(type);
      if (typeSpace == 0) continue;
      //
      //  We base our desired stock levels partly off installed upgrades, and
      //  partly off local supply/demand levels.
      final float
        realDemand = base.demands.demandFractionFor(this, type, 1) * 2,
        realSupply = base.demands.supplyFractionFor(this, type, 1) * 2,
        shortage   = (realDemand / (realSupply + realDemand)) - 0.5f,
        idealStock = (realDemand + realSupply + typeSpace) / 3;
      final boolean exports = shortage < 0;
      stocks.incDemand(type, Nums.min(typeSpace, idealStock), 1, exports);
      if (report) {
        I.say("\nReal supply/demand for "+type+": "+realSupply+"/"+realDemand);
        I.say("  Global supply:   "+base.demands.globalSupply(type));
        I.say("  Global demand:   "+base.demands.globalDemand(type));
        I.say("  Supply sampling: "+base.demands.supplySampling(type));
        I.say("  Demand sampling: "+base.demands.demandSampling(type));
        I.say("  Space available: "+typeSpace);
        I.say("  Ideal stock is:  "+idealStock);
      }
    }
    //
    //  In essence, we accumulate interest on any debts or losses accrued
    //  before taxation kicks in!
    float interest = structure.upgradeLevel(VIRTUAL_CURRENCY) * 50 / 3f;
    interest /= 100 * Stage.STANDARD_DAY_LENGTH;
    stocks.incCredits(stocks.allCredits() * interest);
  }
  
  
  public Background[] careers() {
    return new Background[] { Backgrounds.STOCK_VENDOR };
  }
  
  
  public Traded[] services() {
    return ALL_SERVICES;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  //  TODO:  Try merging these lists into a single array?
  final static Traded DISPLAYED_GOODS[] = {
    CARBS   , PROTEIN , GREENS  ,
    MEDICINE, PARTS   , PLASTICS,
  };
  //  TODO:  Include the full range of items:  Foods, Drugs, Wares, Spyce.
  final static float GOOD_DISPLAY_OFFSETS[] = {
    0, 0.5f,
    0, 1.5f,
    0, 2.5f,
    0.5f, 0,
    1.5f, 0,
    2.5f, 0,
  };
  
  
  protected float[] goodDisplayOffsets() {
    return GOOD_DISPLAY_OFFSETS;
  }
  
  
  protected Traded[] goodsToShow() {
    return DISPLAYED_GOODS;
  }
  
  /*
  //
  //  TODO:  You have to show items in the back as well, behind a sprite
  //  overlay for the facade of the structure.
  protected float goodDisplayAmount(Traded good) {
    final float CL = catalogueLevel(good);
    return Nums.min(super.goodDisplayAmount(good), 25 * CL);
  }
  //*/
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "stock_exchange");
  }
  
  
  public String helpInfo() {
    if (inWorld() && staff.manning() == 0) {
      return
        "The Stock Exchange cannot provide goods to local homeowners unless "+
        "someone is there to man the stalls!";
    }
    else return
      "The Stock Exchange generates higher profits from the sale of finished "+
      "goods to local homes and businesses.";
  }
}






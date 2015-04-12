/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.civic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.economic.Economy.*;



//  TODO:  Consider getting rid of local deliveries here- it may be more of a
//  nuisance than it's worth.


//  Rations vending.  Repairs.  Medkits.
//  Advertising (morale boost and added purchase-attraction)
//  Security vault.  Currency exchange.



public class StockExchange extends Venue {
  
  
  /**  Data fields, constructors and save/load functionality-
    */
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    StockExchange.class, "media/Buildings/merchant/stock_exchange.png", 3.7f, 1
  );
  final public static ImageAsset ICON = ImageAsset.fromImage(
    StockExchange.class, "media/GUI/Buttons/stock_exchange_button.gif"
  );
  
  final static Traded
    ALL_STOCKED[] = (Traded[]) Visit.compose(Traded.class,
      ALL_FOOD_TYPES, ALL_DRUG_TYPES, ALL_WARES_TYPES, ALL_SPYCE_TYPES
    ),
    ALL_SERVICES[] = (Traded[]) Visit.compose(Traded.class,
      ALL_STOCKED, new Traded[] { SERVICE_COMMERCE }
    );
  
  final static VenueProfile PROFILE = new VenueProfile(
    StockExchange.class, "stock_exchange", "Stock Exchange",
    4, 1, IS_NORMAL,
    SupplyDepot.PROFILE, Owner.TIER_FACILITY
  );
  
  private float catalogueSums[] = new float[ALL_STOCKED.length];
  
  
  public StockExchange(Base base) {
    super(PROFILE, base);
    staff.setShiftType(SHIFTS_BY_DAY);
    structure.setupStats(
      150, 3, 250,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
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
  private float upgradeLevelFor(Traded type) {
    final int category = Economy.categoryFor(type);
    Upgrade upgrade = null;
    switch (category) {
      case (CATEGORY_FOOD ) : upgrade = RATIONS_VENDING   ; break;
      case (CATEGORY_DRUG ) : upgrade = MEDICAL_EXCHANGE; break;
      case (CATEGORY_WARES) : upgrade = HARDWARE_STORE  ; break;
      case (CATEGORY_SPYCE) : upgrade = SPYCE_EMPORIUM  ; break;
    }
    return upgrade == null ? -1 : (structure.upgradeLevel(upgrade) / 3f);
  }
  
  
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
    final float level = catalogueLevel(item.type);
    if (level <= 0 || amount >= 0) return;
    
    adjustCatalogue(item.type, 0 - Nums.abs(amount));
    final float basePrice    = super.priceFor(item.type);
    final float upgradeLevel = upgradeLevelFor(item.type);
    stocks.incCredits(basePrice * DEFAULT_SALES_MARGIN * upgradeLevel);
  }
  
  
  public float priceFor(Traded service) {
    final float basePrice    = super.priceFor(service);
    final float upgradeLevel = upgradeLevelFor(service);
    return basePrice * (1f - (DEFAULT_SALES_MARGIN * upgradeLevel / 2f));
  }
  
  
  public int spaceFor(Traded good) {
    //  TODO:  Include bonuses for structure-level-upgrades, and bulk storage.
    final float upgradeLevel = upgradeLevelFor(good);
    if (upgradeLevel <= 0) return 0;
    return 10 + (int) (upgradeLevel * 15);
  }
  
  
  
  /**  Upgrades, behaviour and economic functions-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> ();
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES; }
  
  final public static Upgrade
    
    //  TODO:  COOK UP RATIONS AS A 4TH FOOD TYPE
    RATIONS_VENDING = new Upgrade(
      "Rations Vending",
      "Increases space available to carbs, greens and protein and augments "+
      "profits from their sale.",
      150, Upgrade.THREE_LEVELS, null, 1,
      null, StockExchange.class, ALL_UPGRADES
    ),
    
    //  TODO:  PERMIT BASIC REPAIRS/RECHARGE OF ARMOUR/DEVICES
    HARDWARE_STORE = new Upgrade(
      "Hardware Store",
      "Increases space available to parts, plastics and datalinks, and "+
      "augments profits from their sale.",
      150, Upgrade.THREE_LEVELS, null, 1,
      null, StockExchange.class, ALL_UPGRADES
    ),
    
    //  TODO:  PROVIDE STANDARD MEDKITS FOR USE
    MEDICAL_EXCHANGE = new Upgrade(
      "Medical Exchange",
      "Increases space available to reagents, soma and medicine, and augments"+
      "profits from their sale.",
      250, Upgrade.THREE_LEVELS, null, 1,
      null, StockExchange.class, ALL_UPGRADES
    ),
    
    VAULT_SECURITY = new Upgrade(
      "Vault Security",
      "Expands inventory space for all goods and provides a measure of "+
      "security against theft.",
      200, Upgrade.THREE_LEVELS, null, 1,
      null, StockExchange.class, ALL_UPGRADES
    ),
    
    /*
    INFORMATION_TRADE = new Upgrade(
    ),
    //*/
    //  TODO:  Dispose of this for now.
    //*
    SPYCE_EMPORIUM = new Upgrade(
      "Spyce Emporium",
      "Permits trading in Natrizoral, Tinerazine, and Halebdynum- trace "+
      "compounds vital to complex chemistry.",
      300, Upgrade.THREE_LEVELS, null, 1,
      RATIONS_VENDING, StockExchange.class, ALL_UPGRADES
    ),
    //*/
    
    VIRTUAL_CURRENCY = new Upgrade(
      "Virtual Currency",
      "Makes small periodic adjustments to revenue and outlays in response "+
      "to large-scale investment patterns, magnifying both profits and losses.",
      400, Upgrade.THREE_LEVELS, null, 1,
      VAULT_SECURITY, StockExchange.class, ALL_UPGRADES
    ),
    
    PATENT_RESURGIN = new Upgrade(
      "Patent: Resurgin",
      "",
      350, Upgrade.THREE_LEVELS, null, 1,
      MEDICAL_EXCHANGE, StockExchange.class, ALL_UPGRADES
    ),
    
    PATENT_QI_ANSIBLE = new Upgrade(
      "Patent: QI Ansible",
      "",
      550, Upgrade.THREE_LEVELS, null, 1,
      VAULT_SECURITY, StockExchange.class, ALL_UPGRADES
    ),
    
    PATENT_EGO_SERUM = new Upgrade(
      "Patent: Ego Serum",
      "",
      750, Upgrade.THREE_LEVELS, null, 1,
      SPYCE_EMPORIUM, StockExchange.class, ALL_UPGRADES
    );
  
  
  public int numOpenings(Background p) {
    final int nO = super.numOpenings(p);
    if (p == Backgrounds.STOCK_VENDOR) return nO + 3;
    return 0;
  }
  
  
  public Behaviour jobFor(Actor actor, boolean onShift) {
    if (! onShift) return null;
    final Choice choice = new Choice(actor);
    
    //  ...You basically don't want the stock vendor wandering too far, because
    //  the venue has to be manned in order for citizens to come shopping.  So
    //  stick with jobs that happen within the venue.
    
    //  TODO:  Consider patent-manufacture activities!
    if (Plan.competition(Supervision.class, this, actor) > 0) {
      choice.add(DeliveryUtils.bestBulkDeliveryFrom(
        this, services(), 2, 10, 5
      ));
      choice.add(DeliveryUtils.bestBulkCollectionFor(
        this, services(), 2, 10, 5
      ));
    }
    
    choice.add(Supervision.inventory(this, actor));
    return choice.weightedPick();
  }
  
  
  protected void addServices(Choice choice, Actor actor) {
    final Property home = actor.mind.home();
    if (home instanceof Venue) {
      final Delivery d = DeliveryUtils.fillBulkOrder(
        this, home, ((Venue) home).stocks.demanded(), 1, 5
      );
      if (d != null) {
        //  TODO:  BASE THIS OFF A KEY UPGRADE!
        final float advertBonus = 0.5f;
        d.setWithPayment(actor, true);
        d.addMotives(Plan.MOTIVE_LEISURE, Plan.CASUAL * advertBonus);
        choice.add(d.setWithPayment(actor, true));
      }
    }
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    
    structure.setAmbienceVal(2.0f);
    
    for (Traded type : ALL_MATERIALS) {
      final int room = spaceFor(type);
      stocks.incDemand(type, room / 2f, 1, false);
    }
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
    CARBS   , PROTEIN , GREENS   ,
    REAGENTS, SOMA    , MEDICINE ,
    PARTS   , PLASTICS, DATALINKS,
    SPYCE_T , SPYCE_H , SPYCE_N  ,
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
  
  //
  //  TODO:  You have to show items in the back as well, behind a sprite
  //  overlay for the facade of the structure.
  protected float goodDisplayAmount(Traded good) {
    final float CL = catalogueLevel(good);
    return Nums.min(super.goodDisplayAmount(good), 25 * CL);
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "stock_exchange");
  }


  public String helpInfo() {
    if (inWorld() && ! isManned()) {
      return
        "The Stock Exchange cannot provide goods to local homeowners unless "+
        "someone is there to man the stalls!";
    }
    else return
      "The Stock Exchange facilitates small-scale purchases within "+
      "residential neighbourhoods, and bulk transactions between local "+
      "merchants.";
  }


  public String objectCategory() {
    return UIConstants.TYPE_COMMERCE;
  }
}






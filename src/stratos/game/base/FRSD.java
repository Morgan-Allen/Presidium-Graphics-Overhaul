/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.building.Economy.*;



public class FRSD extends Venue {
  
  
  /**  Other data fields, constructors and save/load methods-
    */
  final public static ModelAsset MODEL_UNDER = CutoutModel.fromSplatImage(
    FRSD.class, "media/Buildings/merchant/depot_under.gif", 4.0f
  );
  final public static ModelAsset MODEL_CORE = CutoutModel.fromImage(
    FRSD.class, "media/Buildings/merchant/depot_core.png", 3, 2
  );
  final public static ImageAsset ICON = ImageAsset.fromImage(
    FRSD.class, "media/GUI/Buttons/supply_depot_button.gif"
  );
  /*
  final static FacilityProfile PROFILE = new FacilityProfile(
    EcologistStation.class, Structure.TYPE_VENUE,
    3, 200, 2, 0,
    new TradeType[] {},
    new Background[] { FAB_WORKER },
    WASTE_TO_PLASTICS,
    SERVICE_REPAIRS,
    SERVICE_TRADE
  );
  //*/
  
  
  public FRSD(Base base) {
    super(4, 2, ENTRANCE_WEST, base);
    structure.setupStats(
      100,  //integrity
      2,    //armour
      200,  //build cost
      Structure.NORMAL_MAX_UPGRADES,
      Structure.TYPE_VENUE
    );
    personnel.setShiftType(SHIFTS_BY_HOURS);
    
    final GroupSprite sprite = new GroupSprite();
    sprite.attach(MODEL_UNDER, 0   ,  0   , -0.05f);
    sprite.attach(MODEL_CORE , 0.1f, -0.1f,  0    );
    sprite.setSortMode(GroupSprite.SORT_BY_ADDITION);
    attachSprite(sprite);
  }
  
  
  public FRSD(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Upgrades, economic functions and behaviour implementation-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    FRSD.class, "FRSD_upgrades"
  );
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES; }
  final public static Upgrade
    POLYMER_FAB = new Upgrade(
      "Polymer Fab",
      "Permits faster fabrication of basic clothing and plastics.",
      150, null, 1, null, ALL_UPGRADES
    ),
    FAB_WORKER_STATION = new Upgrade(
      "Fab Worker Station",
      "Fab Workers see to the basic construction needs of your settlement, "+
      "along with manufacturing of essential supplies.",
      100, Backgrounds.FAB_WORKER, 1, null, ALL_UPGRADES
    ),
    RATIONS_EXCHANGE = new Upgrade(
      "Rations Exchange",
      "Increases space available for trading in carbs and protein.  "+
      "Provides a small bonus to plastics fabrication.",
      100, null, 1, null, ALL_UPGRADES
    ),
    HARDWARE_EXCHANGE = new Upgrade(
      "Hardware Exchange",
      "Increases space available for trading in ores, parts and fuel.",
      200, null, 1, POLYMER_FAB, ALL_UPGRADES
    );
  
  //  ...That, plus storage options for raw materials.
  
  
  
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null;
    final Choice choice = new Choice(actor);
    
    final Plan r = Repairs.getNextRepairFor(actor, false);
    choice.add(r);
    
    final Delivery d = DeliveryUtils.bestBulkDeliveryFrom(
      this, services(), 2, 10, 5
    );
    if (d != null && personnel.assignedTo(d) < 1) choice.add(d);
    
    final Delivery c = DeliveryUtils.bestBulkCollectionFor(
      this, services(), 2, 10, 5
    );
    if (c != null && personnel.assignedTo(c) < 1) choice.add(c);
    
    final Manufacture m = stocks.nextManufacture(actor, WASTE_TO_PLASTICS);
    choice.add(m);
    
    final Manufacture o = stocks.nextSpecialOrder(actor);
    choice.add(o);
    
    if (choice.empty()) choice.add(new Supervision(actor, this));
    
    return choice.weightedPick();
  }
  
  
  public void addServices(Choice choice, Actor forActor) {
    Commission.addCommissions(forActor, this, choice, OVERALLS);
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates);
    if (! structure.intact()) return;
    
    final float
      plasticLevel = (structure.upgradeLevel(POLYMER_FAB      ) + 1) * 5,
      rationLevel  = (structure.upgradeLevel(RATIONS_EXCHANGE ) + 1) * 5,
      scrapLevel   = (structure.upgradeLevel(HARDWARE_EXCHANGE) + 1) * 5;
    
    stocks.forceDemand(PLASTICS , plasticLevel  , Stocks.TIER_PRODUCER);
    stocks.forceDemand(CARBS    , rationLevel   , Stocks.TIER_TRADER  );
    stocks.forceDemand(PROTEIN  , rationLevel   , Stocks.TIER_TRADER  );
    stocks.forceDemand(ORES     , scrapLevel    , Stocks.TIER_TRADER  );
    stocks.forceDemand(FUEL_RODS, scrapLevel    , Stocks.TIER_TRADER  );
    stocks.forceDemand(PARTS    , scrapLevel / 2, Stocks.TIER_TRADER  );
  }
  
  
  public Background[] careers() {
    return new Background[] { Backgrounds.FAB_WORKER };
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v);
    if (v == Backgrounds.FAB_WORKER) return nO + 3;
    return 0;
  }
  
  
  public TradeType[] services() {
    return new TradeType[] {
      CARBS, PROTEIN, PLASTICS, OVERALLS,
      ORES, FUEL_RODS, PARTS,
      SERVICE_COMMERCE, SERVICE_REPAIRS
    };
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected float[] goodDisplayOffsets() {
    return new float[] { 0.0f, 3.0f };
  }
  
  
  protected TradeType[] goodsToShow() {
    //  TODO:  Have different colours of crate for each category.
    return new TradeType[] { SAMPLES };
  }
  
  
  protected float goodDisplayAmount(TradeType good) {
    float amount = 0;
    for (Item i : stocks.allItems()) amount += i.amount;
    return amount;
  }
  
  
  public String fullName() {
    return "F.R.S.D";
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "FRSD");
  }
  
  
  public String helpInfo() {
    return
      "The Fabrication, Recycling and Supply Depot (or FRSD) provides basic "+
      "construction materials and rations storage suitable to the needs of "+
      "miners, cultivators and other frontier colonists.";
  }
  
  
  public String buildCategory() {
    return UIConstants.TYPE_MERCHANT;
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    BaseUI.current().selection.renderTileOverlay(
      rendering, world,
      hovered ? Colour.transparency(0.5f) : Colour.WHITE,
      Selection.SELECT_OVERLAY, true, this, this
    );
  }
}




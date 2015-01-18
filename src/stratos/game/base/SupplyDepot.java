/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.CHEMISTRY;
import static stratos.game.actors.Qualities.SIMPLE_DC;
import static stratos.game.economic.Economy.*;


//  TODO:  You'll need to introduce explicit upgrades here again.

//  Trades in raw materials and a few finished goods.
//  (Carbs.  Protein.  Plastics.  Parts.)  Reagents.  
//  Ores.  Topes.  Carbons.  Spyce N.  Spyce T.  Spyce H.





public class SupplyDepot extends Venue {
  
  
  /**  Other data fields, constructors and save/load methods-
    */
  final public static ModelAsset MODEL_UNDER = CutoutModel.fromSplatImage(
    SupplyDepot.class, "media/Buildings/merchant/depot_under.gif", 4.0f
  );
  final public static ModelAsset MODEL_CORE = CutoutModel.fromImage(
    SupplyDepot.class, "media/Buildings/merchant/depot_core.png", 3, 2
  );
  final public static ImageAsset ICON = ImageAsset.fromImage(
    SupplyDepot.class, "media/GUI/Buttons/supply_depot_button.gif"
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
  
  //  TODO:  Specialise in all raw materials and only a few finished goods.
  final static Traded
    /*
    ALL_TRADE_TYPES[] = {
      CARBS, PROTEIN, GREENS, LCHC,
      ORES, TOPES, PARTS, PLASTICS
    },
    //*/
    ALL_TRADE_TYPES[] = Economy.ALL_MATERIALS,
    ALL_SERVICES[] = (Traded[]) Visit.compose(Traded.class,
      ALL_TRADE_TYPES, new Traded[] { SERVICE_COMMERCE }
    );
  
  //private CargoBarge cargoBarge;
  private List <CargoBarge> barges = new List <CargoBarge> ();
  
  
  public SupplyDepot(Base base) {
    super(4, 2, ENTRANCE_WEST, base);
    structure.setupStats(
      100,  //integrity
      2  ,  //armour
      200,  //build cost
      Structure.NORMAL_MAX_UPGRADES,
      Structure.TYPE_VENUE
    );
    staff.setShiftType(SHIFTS_BY_HOURS);
    
    final GroupSprite sprite = new GroupSprite();
    sprite.attach(MODEL_UNDER, 0   ,  0   , -0.05f);
    sprite.attach(MODEL_CORE , 0.1f, -0.1f,  0    );
    sprite.setSortMode(GroupSprite.SORT_BY_ADDITION);
    attachSprite(sprite);
  }
  
  
  public SupplyDepot(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Upgrades, economic functions and behaviour implementation-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> ();

  final public static Upgrade
    LCHC_RENDERING  = new Upgrade(
      "LCHC Rendering",
      "Converts organic waste to "+LCHC.name+" along with a slight amount of "+
      "power.",
      200, null, 1, null,
      SupplyDepot.class, ALL_UPGRADES
    ),
    HARDWARE_STORE  = new Upgrade(
      "Hardware Store",
      "Allows civilian purchases of parts and plastics, and increases storage "+
      "space.",
      150, null, 1, null,
      SupplyDepot.class, ALL_UPGRADES
    ),
    RATIONS_VENDING = new Upgrade(
      "Rations Vending",
      "Allows civilian purchases of carbs and protein, and increases storage "+
      "space.",
      100, null, 1, null,
      SupplyDepot.class, ALL_UPGRADES
    ),
    EXPORT_TRADE = new Upgrade(
      "Export Trade",
      "Prepares and fuels cargo convoys to visit distant settlements. "+
      "<NOT IMPLEMENTED YET>",
      250, null, 1, LCHC_RENDERING,
      SupplyDepot.class, ALL_UPGRADES
    );
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES; }
  
  final public static Conversion
    WASTE_TO_LCHC = new Conversion(
      SupplyDepot.class, "waste_to_lchc",
      TO, 1, LCHC,
      SIMPLE_DC, CHEMISTRY
    );
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    
    float bonusC = structure.upgradeLevel(LCHC_RENDERING) * 5;
    stocks.incDemand(LCHC, bonusC, Tier.ANY, 1);
    
    float bonusRV = structure.upgradeLevel(RATIONS_VENDING) * 5;
    stocks.incDemand(CARBS   , bonusRV, Tier.ANY, 1);
    stocks.incDemand(PROTEIN , bonusRV, Tier.ANY, 1);
    
    float bonusHS = structure.upgradeLevel(HARDWARE_STORE) * 5;
    stocks.incDemand(PARTS   , bonusHS, Tier.ANY, 1);
    stocks.incDemand(PLASTICS, bonusHS, Tier.ANY, 1);
    
    int maxBarges = structure.upgradeLevel(EXPORT_TRADE) * 5;
    //  TODO:  You need to send those barges off to different settlements!
    //  TODO:  Consider taking this over entirely from the Stock Exchange.
    
    for (Traded t : ALL_TRADE_TYPES) stocks.incDemand(t, 0, Tier.TRADER, 1);
  }
  
  
  protected Behaviour jobFor(Actor actor, boolean onShift) {
    if (! onShift) return null;
    
    final Choice choice = new Choice(actor);
    choice.add(Repairs.getNextRepairFor(actor, true));
    
    final Manufacture CM = stocks.nextManufacture(actor, WASTE_TO_LCHC);
    if (CM != null) choice.add(CM.setBonusFrom(this, true, LCHC_RENDERING));
    
    final Delivery d = DeliveryUtils.bestBulkDeliveryFrom(
      this, services(), 2, 10, 5
    );
    if (d != null && staff.assignedTo(d) < 1) choice.add(d);
    
    final Delivery c = DeliveryUtils.bestBulkCollectionFor(
      this, services(), 2, 10, 5
    );
    if (c != null && staff.assignedTo(c) < 1) choice.add(c);
    
    if (choice.empty()) choice.add(Supervision.oversight(this, actor));
    return choice.weightedPick();
  }
  
  
  protected void addServices(Choice choice, Actor actor) {
    if (! (actor.mind.home() instanceof Holding)) return;
    
    final Batch <Traded> toBuy = new Batch <Traded> ();
    if (structure.hasUpgrade(RATIONS_VENDING)) {
      toBuy.add(CARBS  );
      toBuy.add(PROTEIN);
    }
    if (structure.hasUpgrade(HARDWARE_STORE)) {
      toBuy.add(PARTS   );
      toBuy.add(PLASTICS);
    }
    if (toBuy.empty()) return;
    
    final Delivery d = DeliveryUtils.fillBulkOrder(
      this, actor.mind.home(), toBuy.toArray(Traded.class), 1, 5
    );
    if (d != null) choice.add(d.setWithPayment(actor, true));
  }
  
  
  public Background[] careers() {
    return new Background[] { Backgrounds.SUPPLY_CORPS };
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v);
    if (v == Backgrounds.SUPPLY_CORPS) return nO + 3;
    return 0;
  }
  
  
  public Traded[] services() {
    return ALL_SERVICES;
  }
  
  
  public void updatePaving(boolean inWorld) {
    super.updatePaving(inWorld);
    
    final Tile at = mainEntrance();
    if (inWorld) {
      final Batch <Tile> e = new Batch <Tile> ();
      final float range = Stage.SECTOR_SIZE * 2;
      final Presences p = world.presences;
      
      for (Object o : p.matchesNear(SERVICE_COMMERCE, this, range)) {
        final Venue v = (Venue) o;
        e.add(v.mainEntrance());
      }
      base.transport.updateJunction(this, at, e, true);
    }
    else base.transport.updateJunction(this, at, null, false);
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected float[] goodDisplayOffsets() {
    return new float[] { 0.0f, 3.0f };
  }
  
  
  protected Traded[] goodsToShow() {
    //  TODO:  Have different colours of crate for each category.
    return new Traded[] { SAMPLES };
  }
  
  
  protected float goodDisplayAmount(Traded good) {
    float amount = 0;
    for (Item i : stocks.allItems()) amount += i.amount;
    return amount;
  }
  
  
  public String fullName() {
    return "Supply Depot";
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "FRSD");
  }
  
  
  public String helpInfo() {
    return
      "The Supply Depot provides basic construction materials and rations "+
      "storage suitable to the needs of frontier colonists or heavy industry.";
  }
  
  
  public String objectCategory() {
    return UIConstants.TYPE_MERCHANT;
  }
}




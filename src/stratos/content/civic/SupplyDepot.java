/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import stratos.game.actors.*;
import stratos.game.base.BaseDemands;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.craft.Economy.*;



//  TODO:  Consider adding one or two upgrades, to allow for scrapping, granary
//  functions, construction materials etc.?

//  Polymer still.  Extra barges.
//  Hardware outlet.  Granary depot.


public class SupplyDepot extends Venue {
  
  /**  Other data fields, constructors and save/load methods-
    */
  final public static ModelAsset MODEL_UNDER = CutoutModel.fromSplatImage(
    SupplyDepot.class, "model_supply_depot_under",
    "media/Buildings/merchant/depot_under.gif", 4
  );
  final public static ModelAsset MODEL_CORE = CutoutModel.fromImage(
    SupplyDepot.class, "model_supply_depot_core",
    "media/Buildings/merchant/depot_core.png", 4, 1
  );
  final public static ImageAsset ICON = ImageAsset.fromImage(
    SupplyDepot.class, "model_supply_depot_icon",
    "media/GUI/Buttons/supply_depot_button.gif"
  );

  final public static Traded
    ALL_STOCKED[] = {
      POLYMER, PLASTICS,
      METALS, FUEL_RODS, PARTS, CARBS, PROTEIN, GREENS, REAGENTS, SPYCES
    },
    HOME_PURCHASE_TYPES[] = {
      PLASTICS, PARTS, CARBS, PROTEIN
    };
  final static float
    VIEW_OFFSETS[] = {
      0, 0,  0.5f, 0,  1.0f, 0,  1.5f, 0,  2.0f, 0,
      0, 1,  0.5f, 1,  1.0f, 1,  1.5f, 1,  2.0f, 1,
    };
  
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    SupplyDepot.class, "supply_depot",
    "Supply Depot", Target.TYPE_COMMERCE, ICON,
    "The Supply Depot provides storage for raw materials, along with a "+
    "steady supply of "+POLYMER+" and "+PLASTICS+".",
    4, 1, Structure.IS_NORMAL,
    Owner.TIER_TRADER, 100, 2,
    Visit.compose(Object.class, ALL_STOCKED, new Object[] {
      SERVICE_COMMERCE, Backgrounds.SUPPLY_CORPS
    })
  );
  
  final public static Upgrade
    LEVELS[] = BLUEPRINT.createVenueLevels(
      Upgrade.SINGLE_LEVEL, null,
      new Object[] { 5, CHEMISTRY, 5, HARD_LABOUR },
      400
    );

  final public static Conversion
    FLORA_TO_POLYMER = new Conversion(
      BLUEPRINT, "flora_to_polymer_at_depot",
      10, HARD_LABOUR, TO, 1, POLYMER
    ),
    NIL_TO_POLYMER = new Conversion(
      BLUEPRINT, "nil_to_polymer_at_depot",
      5, HARD_LABOUR, 5, CHEMISTRY, TO, 1, POLYMER
    ),
    POLYMER_TO_PLASTICS = new Conversion(
      BLUEPRINT, "polymer_to_plastics_at_depot",
      1, POLYMER, TO, 2, PLASTICS,
      ROUTINE_DC, CHEMISTRY, SIMPLE_DC, ASSEMBLY
    );
  
  final static float
    BASE_POLYMER_PER_DAY = 5;
  
  
  private List <CargoBarge> barges = new List <CargoBarge> ();
  
  
  public SupplyDepot(Base base) {
    super(BLUEPRINT, base);
    staff.setShiftType(SHIFTS_BY_HOURS);
    
    final GroupSprite sprite = new GroupSprite();
    sprite.attach(MODEL_UNDER, 0, 0, 0);
    sprite.attach(MODEL_CORE , 0, 0, 0);
    sprite.setSortMode(GroupSprite.SORT_BY_ADDITION);
    attachSprite(sprite);
    
    for (Traded t : ALL_STOCKED) {
      stocks.forceDemand(t, 0, 5);
    }
    for (Traded t : HOME_PURCHASE_TYPES) {
      stocks.forceDemand(t, 5, 0);
    }
    stocks.forceDemand(PLASTICS, 0, 10);
    stocks.forceDemand(POLYMER , 0, 10);
  }
  
  
  public SupplyDepot(Session s) throws Exception {
    super(s);
    s.loadObjects(barges);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObjects(barges);
  }
  
  
  
  /**  Upgrades, economic functions and behaviour implementation-
    */
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    //
    //  Update all stock demands-
    structure.setAmbienceVal(Ambience.MILD_SQUALOR);
    stocks.updateStockDemands(1, ALL_STOCKED);
    
    float recyc = Nums.clamp(1f - base.demands.primaryShortage(ATMO), 0, 1);
    recyc = BASE_POLYMER_PER_DAY * (recyc + 1) / 2;
    if (stocks.relativeShortage(POLYMER, true) > 0) {
      stocks.bumpItem(POLYMER, recyc / Stage.STANDARD_DAY_LENGTH);
    }
    Manufacture.updateProductionEstimates(this, POLYMER_TO_PLASTICS);
    stocks.incDailyDemand(POLYMER, 0, recyc);
    
    //
    //  TODO:  You need to send those barges off to different settlements!
    for (CargoBarge b : barges) if (b.destroyed()) barges.remove(b);
    if (barges.size() == 0) {
      final CargoBarge b = new CargoBarge();
      b.setHangar(this);
      b.assignBase(base);
      b.enterWorldAt(this, world);
      b.structure.setState(Structure.STATE_INSTALL, 0);
      barges.add(b);
    }
  }
  
  
  public void exitWorld() {
    super.exitWorld();
    for (CargoBarge b : barges) b.exitWorld();
  }
  
  
  private boolean bargeReady(CargoBarge b) {
    return b != null && b.inWorld() && b.structure.goodCondition();
  }
  
  
  public float priceFor(Traded good, boolean sold) {
    if (sold) return super.priceFor(good, sold) * BaseDemands.BASE_SALE_MARGIN;
    else      return super.priceFor(good, sold);
  }
  
  
  protected void addServices(Choice choice, Actor client) {
    choice.add(BringUtils.nextPersonalPurchase(
      client, this, HOME_PURCHASE_TYPES
    ));
    
    if (client.gear.outfitType() == Outfits.OVERALLS) {
      final Item gets = GearPurchase.nextGearToPurchase(client, this);
      if (gets != null) {
        choice.add(GearPurchase.nextCommission(client, this, gets));
      }
    }
  }
  
  
  protected Behaviour jobFor(Actor actor) {
    if (staff.offDuty(actor)) return null;
    final Choice choice = new Choice(actor);
    //
    //  Consider basic manufacturing tasks-
    if (staff.onShift(actor)) {
      final Manufacture m = stocks.nextManufacture(actor, POLYMER_TO_PLASTICS);
      if (m != null) {
        choice.add(m.setSpeedBonus(2.0f));
      }
      for (Item ordered : stocks.specialOrders()) {
        final Manufacture mO = new Manufacture(actor, this, ordered);
        choice.add(mO.setSpeedBonus(2.0f));
      }
      choice.add(Gathering.asForestCutting(actor, this));
    }
    //
    //  See if there's a bulk delivery to be made, or if the cargo barge is in
    //  need of repair.
    final Traded services[] = ALL_MATERIALS;
    final CargoBarge cargoBarge = barges.first();
    if (bargeReady(cargoBarge)) {
      final Batch <Venue> depots = BringUtils.nearbyDepots(
        this, world, SERVICE_COMMERCE
      );
      final Bringing bD = BringUtils.bestBulkDeliveryFrom(
        this, services, 5, 50, depots, true
      );
      if (checkCargoJobOkay(bD, cargoBarge)) choice.add(bD);
      final Bringing bC = BringUtils.bestBulkCollectionFor(
        this, services, 5, 50, depots, true
      );
      if (checkCargoJobOkay(bC, cargoBarge)) choice.add(bC);
    }
    else for (CargoBarge b : barges) {
      if (Repairs.needForRepair(b) > 0) choice.add(new Repairs(actor, b, true));
      else if (b.abandoned()) choice.add(new Bringing(b, this));
    }
    //
    //  Otherwise, consider local deliveries.
    final Bringing d = BringUtils.bestBulkDeliveryFrom(
      this, services(), 2, 10, 5, true
    );
    if (d != null && staff.assignedTo(d) < 1) choice.add(d);
    
    final Bringing c = BringUtils.bestBulkCollectionFor(
      this, services(), 2, 10, 5, true
    );
    if (c != null && staff.assignedTo(c) < 1) choice.add(c);
    
    if (! choice.empty()) return choice.weightedPick();
    //
    //  If none of that needs doing, consider local repairs or supervision.
    choice.add(Supervision.oversight(this, actor));
    choice.add(Repairs.getNextRepairFor(actor, true, 0.1f));
    return choice.weightedPick();
  }
  
  
  private boolean checkCargoJobOkay(Bringing job, CargoBarge driven) {
    if (job == null || driven.pilot() != null) return false;
    if (job.totalBulk() < 10) return false;
    
    final float dist = Spacing.distance(job.origin, job.destination);
    if (dist < Stage.ZONE_SIZE) return false;
    
    job.addMotives(Plan.MOTIVE_JOB, Plan.CASUAL);
    job.driven = driven;
    return true;
  }
  
  
  public int numPositions(Background v) {
    if (v == Backgrounds.SUPPLY_CORPS) return 3;
    return 0;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected Traded[] goodsToShow() {
    return ALL_STOCKED;
  }
  
  
  protected float[] goodDisplayOffsets() {
    return VIEW_OFFSETS;
  }
  

  public SelectionPane configSelectPane(SelectionPane panel, HUD UI) {
    return VenuePane.configStandardPanel(this, panel, UI, ALL_STOCKED);
  }
}












/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Backgrounds.STOCK_VENDOR;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



//  TODO:  Consider adding one or two upgrades, to allow for scrapping, granary
//  functions, construction materials etc.?

//  Polymer still.  Extra barges.
//  Hardware outlet.  Granary depot.


public class SupplyDepot extends Venue {
  
  /**  Other data fields, constructors and save/load methods-
    */
  final public static ModelAsset MODEL_UNDER = CutoutModel.fromSplatImage(
    SupplyDepot.class, "media/Buildings/merchant/depot_under.gif", 4
  );
  final public static ModelAsset MODEL_CORE = CutoutModel.fromImage(
    SupplyDepot.class, "media/Buildings/merchant/depot_core.png", 4, 1
  );
  final public static ImageAsset ICON = ImageAsset.fromImage(
    SupplyDepot.class, "media/GUI/Buttons/supply_depot_button.gif"
  );

  final public static Traded
    ALL_STOCKED[] = {
      POLYMER, METALS, FUEL_RODS, PLASTICS, PARTS,
      CARBS, PROTEIN, GREENS, REAGENTS, SPYCES
    },
    HOME_PURCHASE_TYPES[] = {
      PLASTICS, PARTS, CARBS, PROTEIN
    };
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    SupplyDepot.class, "supply_depot",
    "Supply Depot", Target.TYPE_COMMERCE, ICON,
    "The Supply Depot allows for bulk storage and transport of raw materials "+
    "used in manufacturing.",
    4, 1, Structure.IS_NORMAL,
    Owner.TIER_TRADER, 100, 2,
    Visit.compose(Object.class, ALL_STOCKED, new Object[] {
      SERVICE_COMMERCE, Backgrounds.SUPPLY_CORPS
    })
  );
  
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
      stocks.forceDemand(t, 5, true);
    }
    for (Traded t : HOME_PURCHASE_TYPES) {
      stocks.forceDemand(t, 0, false);
    }
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
  final public static Upgrade
    LEVELS[] = BLUEPRINT.createVenueLevels(
      Upgrade.SINGLE_LEVEL, null,
      new Object[] { 5, ACCOUNTING, 5, HARD_LABOUR },
      400
    );
  
  final public static Conversion
    NIL_TO_POLYMER = new Conversion(
      BLUEPRINT, "nil_to_polymer",
      TO, 1, POLYMER,
      SIMPLE_DC, CHEMISTRY
    );
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    //
    //  Update all stock demands-
    structure.setAmbienceVal(Ambience.MILD_SQUALOR);
    for (Traded type : ALL_STOCKED) {
      final float stockBonus = 1 + upgradeLevelFor(type);
      stocks.updateTradeDemand(type, stockBonus, 1);
    }
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
  
  
  private float upgradeLevelFor(Traded type) {
    //  TODO:  Fill this in?
    return 0;
  }
  
  
  public int spaceFor(Traded t) {
    //  TODO:  Return a limit based on existing total good stocks!
    return 20;
  }
  
  
  private boolean bargeReady(CargoBarge b) {
    return b != null && b.inWorld() && b.structure.goodCondition();
  }
  
  
  protected void addServices(Choice choice, Actor client) {
    choice.add(BringUtils.nextPersonalPurchase(
      client, this, HOME_PURCHASE_TYPES
    ));
  }


  protected Behaviour jobFor(Actor actor) {
    if (staff.offDuty(actor)) return null;
    final Choice choice = new Choice(actor);
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
    if (staff.onShift(actor)) {
      choice.add(Repairs.getNextRepairFor(actor, true, 0.1f));
    }
    choice.add(Supervision.oversight(this, actor));
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
  
  
  final static float OFFSETS[] = {
    0, 2,  0, 3,  1, 2,  1, 3,
    0, 2.5f,  0, 3.5f,  1, 2.5f,  1, 3.5f,
  };
  
  
  protected float[] goodDisplayOffsets() {
    return OFFSETS;
  }
  

  public SelectionPane configSelectPane(SelectionPane panel, BaseUI UI) {
    return VenuePane.configStandardPanel(
      this, panel, UI,
      ALL_STOCKED,
      VenuePane.CAT_ORDERS, VenuePane.CAT_STOCK, VenuePane.CAT_STAFFING
    );
  }
}




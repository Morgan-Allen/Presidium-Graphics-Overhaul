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
import static stratos.game.economic.Economy.*;



public class SupplyDepot extends Venue {
  
  /**  Other data fields, constructors and save/load methods-
    */
  final public static ModelAsset MODEL_UNDER = CutoutModel.fromSplatImage(
    SupplyDepot.class, "media/Buildings/merchant/depot_under.gif", 3.5f
  );
  final public static ModelAsset MODEL_CORE = CutoutModel.fromImage(
    SupplyDepot.class, "media/Buildings/merchant/depot_core.png", 2.75f, 1
  );
  final public static ImageAsset ICON = ImageAsset.fromImage(
    SupplyDepot.class, "media/GUI/Buttons/supply_depot_button.gif"
  );
  
  final static Traded
    ALL_TRADE_TYPES[] = {
      CARBS, PROTEIN, POLYMER, METALS,
      PLASTICS, PARTS, GREENS, MEDICINE,
      SOMA, REAGENTS, FUEL_RODS, CIRCUITRY
    },
    DEFAULT_TRADE_TYPES[] = {
      CARBS, PROTEIN, GREENS, MEDICINE,
      POLYMER, METALS, PLASTICS, PARTS
    },
    DEFAULT_IMPORT_TYPES[] = {
      CARBS, PROTEIN, POLYMER, METALS
    },
    ALL_SERVICES[] = (Traded[]) Visit.compose(Traded.class,
      ALL_MATERIALS, new Traded[] { SERVICE_COMMERCE }
    );
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    SupplyDepot.class, "supply_depot",
    "Supply Depot", UIConstants.TYPE_COMMERCE, ICON,
    "The Supply Depot allows for fine control over imports or exports to "+
    "distant settlements.",
    3, 1, Structure.IS_NORMAL,
    NO_REQUIREMENTS, Owner.TIER_DEPOT,
    100,  //integrity
    2  ,  //armour
    200,  //build cost
    Structure.NORMAL_MAX_UPGRADES
  );
  
  private List <CargoBarge> barges = new List <CargoBarge> ();
  
  
  public SupplyDepot(Base base) {
    super(BLUEPRINT, base);
    staff.setShiftType(SHIFTS_BY_HOURS);
    
    final GroupSprite sprite = new GroupSprite();
    sprite.attach(MODEL_UNDER, 0   ,  0   , -0.05f);
    sprite.attach(MODEL_CORE , 0.1f, -0.1f,  0    );
    sprite.setSortMode(GroupSprite.SORT_BY_ADDITION);
    attachSprite(sprite);
    
    for (Traded t : DEFAULT_TRADE_TYPES ) stocks.forceDemand(t, 10, true );
    for (Traded t : DEFAULT_IMPORT_TYPES) stocks.forceDemand(t, 5 , false);
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
  final public static Conversion
    WASTE_TO_LCHC = new Conversion(
      SupplyDepot.class, "waste_to_lchc",
      TO, 1, POLYMER,
      SIMPLE_DC, CHEMISTRY
    );
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    
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
  
  
  public float priceFor(Traded good, boolean sold) {
    final boolean exports = stocks.producer(good);
    if (   exports  &&    sold ) return base.commerce.exportPrice(good);
    if ((! exports) && (! sold)) return base.commerce.importPrice(good);
    return super.priceFor(good, sold);
  }
  
  
  public int spaceFor(Traded t) {
    //  TODO:  Return a limit based on existing total good stocks!
    return 20;
  }
  
  
  private boolean bargeReady(CargoBarge b) {
    return b != null && b.inWorld() && b.structure.goodCondition();
  }
  
  
  protected Behaviour jobFor(Actor actor, boolean onShift) {
    //
    //  During your secondary shift, consider supervising the venue-
    final boolean offShift = staff.shiftFor(actor) == SECONDARY_SHIFT;
    final Choice choice = new Choice(actor);
    //
    //  During the primary shift, you can also perform repairs or localised
    //  deliveries-
    if (onShift || offShift) {
      for (CargoBarge b : barges) {
        if (Repairs.needForRepair(b) > 0) choice.add(new Repairs(actor, b));
        else if (b.abandoned()) choice.add(new Bringing(b, this));
      }
      choice.add(Repairs.getNextRepairFor(actor, true));
      
      final Bringing d = BringUtils.bestBulkDeliveryFrom(
        this, services(), 2, 10, 5
      );
      if (d != null && staff.assignedTo(d) < 1) choice.add(d);
      
      final Bringing c = BringUtils.bestBulkCollectionFor(
        this, services(), 2, 10, 5
      );
      if (c != null && staff.assignedTo(c) < 1) choice.add(c);
      
      return choice.weightedPick();
    }
    if (onShift && choice.empty()) {
      choice.add(Supervision.oversight(this, actor));
    }
    //
    //  See if there's a bulk delivery to be made-
    final Traded services[] = ALL_MATERIALS;
    final CargoBarge cargoBarge = barges.first();
    if (bargeReady(cargoBarge)) {
      final Batch <Venue> depots = BringUtils.nearbyDepots(
        this, world, SERVICE_COMMERCE
      );
      final Bringing bD = BringUtils.bestBulkDeliveryFrom(
        this, services, 5, 50, depots
      );
      if (checkCargoJobOkay(bD, cargoBarge)) choice.add(bD);
      final Bringing bC = BringUtils.bestBulkCollectionFor(
        this, services, 5, 50, depots
      );
      if (checkCargoJobOkay(bC, cargoBarge)) choice.add(bC);
      if (! choice.empty()) return choice.pickMostUrgent();
    }
    return null;
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
  
  
  
  /**  Rendering and interface methods-
    */
  protected Traded[] goodsToShow() {
    return DEFAULT_TRADE_TYPES;
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
      ALL_TRADE_TYPES, VenuePane.CAT_STOCK, VenuePane.CAT_STAFFING
    );
  }
}





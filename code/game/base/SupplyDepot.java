/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package code.game.base ;
import code.game.actors.*;
import code.game.building.*;
import code.game.common.*;
import code.graphics.common.*;
import code.graphics.cutout.*;
import code.graphics.widgets.*;
import code.user.*;
import code.util.*;



public class SupplyDepot extends Venue implements
  Economy, Service.Trade
{
  
  /**  Other data fields, constructors and save/load methods-
    */
  final public static ModelAsset MODEL_UNDER = CutoutModel.fromImage(
    "media/Buildings/merchant/depot_under.gif", SupplyDepot.class, 4.25f, 0
  );
  final public static ModelAsset MODEL_CORE = CutoutModel.fromImage(
    "media/Buildings/merchant/depot_core.png", SupplyDepot.class, 3, 2
  );
  final public static ImageAsset ICON = ImageAsset.fromImage(
    "media/GUI/Buttons/supply_depot_button.gif", SupplyDepot.class
  );
  
  
  final static int
    KEY_RATIONS     = 0,
    KEY_MINERALS    = 1,
    KEY_MEDICAL     = 2,
    KEY_BUILDING    = 3,
    KEY_SPECIALTIES = 4,
    NUM_PREFS = 5 ;
  final static String PREF_TITLES[] = {
    "Basic rations",
    "Mineral wealth",
    "Medical supplies",
    "Building materials",
    "Specialty items"
  } ;
  final static int PREF_LEVELS[] = {
    10, 25, 50, 0, -10, -25, -50
  }, NUM_LEVELS = 7 ;
  final static String LEVEL_TITLES[] = {
    "Light Exports", "Medium Exports", "Heavy Exports",
    "Not Trading",
    "Light Imports", "Medium Imports", "Heavy Imports",
  } ;
  final private static Colour PREF_COLOURS[] = {
    //  TODO:  Blend the colours a bit more.
    Colour.RED, Colour.RED, Colour.RED,
    Colour.LIGHT_GREY,
    Colour.GREEN, Colour.GREEN, Colour.GREEN,
  } ;
  
  
  //
  // TODO:  Allow import levels for each of these to be specified manually.
  
  final public static Table SERVICE_KEY = Table.make(
    
    CARBS     , KEY_RATIONS ,
    PROTEIN   , KEY_RATIONS ,
    GREENS    , KEY_RATIONS ,
    SOMA      , KEY_RATIONS ,
    
    METALS , KEY_MINERALS,
    FUEL_RODS, KEY_MINERALS,
    
    STIM_KITS , KEY_MEDICAL ,
    TRUE_SPICE     , KEY_MEDICAL ,
    MEDICINE  , KEY_MEDICAL ,
    GENE_SEED , KEY_MEDICAL ,
    
    PARTS     , KEY_BUILDING,
    PLASTICS  , KEY_BUILDING,
    CIRCUITRY , KEY_BUILDING,
    PRESSFEED , KEY_BUILDING,
    DATALINKS , KEY_BUILDING,
    FIXTURES     , KEY_BUILDING 
  ) ;
  
  
  final private int exportLevels[] = {
    -10, -10, 10, 10, 0
  } ;
  LandingStrip landingStrip = null ;
  
  
  
  public SupplyDepot(Base base) {
    super(4, 2, ENTRANCE_WEST, base) ;
    
    structure.setupStats(100, 2, 200, 0, Structure.TYPE_VENUE) ;
    personnel.setShiftType(SHIFTS_ALWAYS) ;
    
    attachModel(MODEL_UNDER);
    //final GroupSprite sprite = new GroupSprite() ;
    //sprite.attach(MODEL_UNDER, 0, 0, -0.05f) ;
    //sprite.attach(MODEL_CORE, -0.25f, 0.25f, 0) ;
    //attachSprite(sprite) ;
  }
  
  
  public SupplyDepot(Session s) throws Exception {
    super(s) ;
    for (int n = NUM_PREFS ; n-- > 0 ;) exportLevels[n] = s.loadInt() ;
    landingStrip = (LandingStrip) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    for (int n = NUM_PREFS ; n-- > 0 ;) s.saveInt(exportLevels[n]) ;
    s.saveObject(landingStrip) ;
  }
  
  
  private int exportLevel(Service service) {
    final Integer key = (Integer) SERVICE_KEY.get(service) ;
    if (key == null) return 0 ;
    return exportLevels[key] ;
  }
  
  
  public float priceFor(Service service) {
    final int level = exportLevel(service) ;
    if (level == 0) return service.basePrice ;
    else if (level < 0) return base().commerce.importPrice(service) ;
    else return base().commerce.exportPrice(service) ;
  }
  
  
  public LandingStrip landingStrip() {
    return landingStrip ;
  }
  
  
  
  /**  Upgrades, economic functions and behaviour implementation-
    */
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null ;
    final Choice choice = new Choice(actor) ;
    
    final Delivery d = Deliveries.nextDeliveryFor(
      actor, this, services(), 10, world
    ) ;
    if (d != null && personnel.assignedTo(d) < 1) {
      //d.nextStepFor(actor) ;
      //I.sayAbout(this, "next delivery: "+d) ;
      choice.add(d) ;
    }
    
    final Delivery c = Deliveries.nextCollectionFor(
      actor, this, services(), 10, null, world
    ) ;
    if (c != null && personnel.assignedTo(c) < 1) choice.add(c) ;
    
    choice.add(new Supervision(actor, this)) ;
    return choice.weightedPick() ;
  }
  
  
  public boolean enterWorldAt(int x, int y, World world) {
    if (! super.enterWorldAt(x, y, world)) return false ;
    updateLandingStrip() ;
    return true ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    updateLandingStrip() ;
    if (! structure.intact()) return ;
    
    final Batch <Venue> depots = Deliveries.nearbyDepots(this, world) ;
    for (Service type : ALL_COMMODITIES) {
      final int level = exportLevel(type) ;
      if (level > 0) stocks.forceDemand(
        type, level, VenueStocks.TIER_CONSUMER
      ) ;
      if (level < 0) stocks.forceDemand(
        type, 0 - level, VenueStocks.TIER_PRODUCER
      ) ;
      stocks.diffuseDemand(type, depots) ;
      stocks.diffuseDemand(type) ;
    }
  }
  
  
  public float importDemand(Service type) {
    if (type.form != FORM_COMMODITY) return 0 ;
    final int level = exportLevel(type) ;
    if (level > 0) return 0 ;
    final float demand = stocks.demandFor(type) ;
    return Math.min(0 - level, demand * level / -10f) ;
  }
  
  
  public float exportDemand(Service type) {
    if (type.form != FORM_COMMODITY) return 0 ;
    final int level = exportLevel(type) ;
    if (level < 0) return 0 ;
    final float price = base().commerce.exportPrice(type) ;
    if (price < type.basePrice) return level ;
    else return level + (10 * ((price / type.basePrice) - 1)) ;
  }
  
  
  public float importShortage(Service type) {
    return Math.max(0, importDemand(type) - stocks.amountOf(type)) ;
  }
  
  
  public float exportSurplus(Service type) {
    if (exportDemand(type) == 0) return 0 ;
    return stocks.amountOf(type) ;
  }
  
  
  public Background[] careers() {
    return new Background[] { Background.SUPPLY_CORPS } ;
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v) ;
    if (v == Background.SUPPLY_CORPS) return nO + 2 ;
    return 0 ;
  }
  
  
  public Service[] services() {
    return ALL_COMMODITIES ;
  }
  
  
  
  /**  Setting up the landing strip-
    */
  //
  //  TODO:  Have the landing strip be visible during placement previews?
  //  TODO:  Also, you need to guarantee that the attempted position isn't off
  //  the border of the map!
  protected void updateLandingStrip() {
    if (landingStrip == null || landingStrip.destroyed()) {
      final LandingStrip newStrip = new LandingStrip(this) ;
      final Tile o = origin() ;
      final int S = this.size ;

      for (int n : TileConstants.N_ADJACENT) {
        n = (n + 2) % 8 ;
        newStrip.setPosition(o.x + (N_X[n] * S), o.y + (N_Y[n] * S), world) ;
        if (newStrip.canPlace()) {
          newStrip.doPlace(newStrip.origin(), null) ;
          landingStrip = newStrip ;
          break ;
        }
      }
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected float[] goodDisplayOffsets() {
    return new float[] { -3.0f, 0.0f } ;
  }
  
  
  protected Service[] goodsToShow() {
    //  TODO:  Have different colours of crate for each category.
    return new Service[] { SAMPLES } ;
  }
  
  
  protected float goodDisplayAmount(Service good) {
    float amount = 0 ;
    for (Item i : stocks.allItems()) amount += i.amount ;
    return amount ;
  }
  
  
  public String[] infoCategories() {
    return new String[] { "STATUS", "STAFF", "STOCK", "ORDERS" } ;
  }
  

  public void writeInformation(Description d, int categoryID, HUD UI) {
    if (categoryID == 3) {
      d.append("Trade Quotas (Click to change)\n") ;
      for (int n = 0 ; n < NUM_PREFS ; n++) {
        descPref(n, PREF_TITLES[n], d) ;
      }
    }
    else super.writeInformation(d, categoryID, UI) ;
  }
  
  
  private void descPref(final int index, String title, Description d) {
    int level = exportLevels[index] ;
    d.append("\n"+title) ;
    for (int n = 0 ; n < NUM_LEVELS ; n++) {
      if (level == PREF_LEVELS[n]) {
        final int setting = n ;
        d.append("\n  ") ;
        d.append(new Description.Link(LEVEL_TITLES[n]) {
          public void whenClicked() {
            exportLevels[index] = PREF_LEVELS[(setting + 1) % NUM_LEVELS] ;
          }
        }, PREF_COLOURS[setting]) ;
      }
    }
  }
  
  
  public String fullName() {
    return "Supply Depot" ;
  }
  
  
  public Composite portrait(HUD UI) {
    final Composite cached = Composite.fromCache("supply_depot");
    if (cached != null) return cached;
    return Composite.withImage(ICON, "supply_depot");
  }
  
  
  public String helpInfo() {
    return
      "The Supply Depot mediates long-distance trade, both between remote "+
      "outposts of your own colony and offworld commercial partners." ;
  }
  
  
  public String buildCategory() {
    return UIConstants.TYPE_MERCHANT ;
  }
}








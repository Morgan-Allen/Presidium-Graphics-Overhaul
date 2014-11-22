/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
//import static stratos.game.actors.Qualities.*;
//import static stratos.game.actors.Backgrounds.*;
import static stratos.game.building.Economy.*;



//  TODO:  Add the landing strip back here.

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

  final static Traded ALL_TRADE_TYPES[] = {
    CARBS, PROTEIN, GREENS, LCHC,
    ORES, TOPES, PARTS, PLASTICS
  };
  
  final static int
    TRADE_IMPORT = -1,
    TRADE_EXPORT =  1,
    TRADE_AUTO   =  0,
    MIN_TRADE    = 5 ,
    MAX_TRADE    = 40,
    NUM_TYPES    = ALL_TRADE_TYPES.length;
  
  final byte
    tradeLevels[] = new byte[NUM_TYPES],
    tradeTypes [] = new byte[NUM_TYPES];
  
  
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
    s.loadByteArray(tradeLevels);
    s.loadByteArray(tradeTypes );
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveByteArray(tradeLevels);
    s.saveByteArray(tradeTypes );
  }
  
  
  
  /**  Upgrades, economic functions and behaviour implementation-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> ();
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES; }
  
  
  private void setTrading(Traded t, int type, int level) {
    final int index = Visit.indexOf(t, ALL_TRADE_TYPES);
    tradeLevels[index] = (byte) Visit.clamp(level, MIN_TRADE, MAX_TRADE);
    tradeTypes [index] = (byte) type ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates);
    if (! structure.intact()) return;
    
    final int interval = (int) scheduledInterval();
    
    for (int i = 0 ; i < NUM_TYPES; i++) {
      final Traded t = ALL_TRADE_TYPES[i];
      final int type = tradeTypes[i], level = tradeLevels[i];
      
      if      (type == TRADE_AUTO  ) {
        stocks.incDemand(t, 0, Stocks.TIER_TRADER, interval, this);
      }
      else if (type == TRADE_IMPORT) {
        stocks.forceDemand(t, level, Stocks.TIER_IMPORTER);
      }
      else if (type == TRADE_EXPORT) {
        stocks.forceDemand(t, level, Stocks.TIER_EXPORTER);
      }
    }
  }
  
  
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null;
    final Choice choice = new Choice(actor);
    
    final Plan r = Repairs.getNextRepairFor(actor, false);
    if (r != null) choice.add(r.setMotive(Plan.MOTIVE_DUTY, Plan.ROUTINE));
    
    final Delivery d = DeliveryUtils.bestBulkDeliveryFrom(
      this, services(), 2, 10, 5
    );
    if (d != null && personnel.assignedTo(d) < 1) choice.add(d);
    
    final Delivery c = DeliveryUtils.bestBulkCollectionFor(
      this, services(), 2, 10, 5
    );
    if (c != null && personnel.assignedTo(c) < 1) choice.add(c);
    
    if (choice.empty()) choice.add(new Supervision(actor, this));
    return choice.weightedPick();
  }
  
  
  public Background[] careers() {
    return new Background[] { Backgrounds.FAB_WORKER };
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v);
    if (v == Backgrounds.FAB_WORKER) return nO + 3;
    return 0;
  }
  
  
  public Traded[] services() {
    return ALL_TRADE_TYPES;
  }
  
  
  /**  Placement and construction previews-
    */
  public boolean setPosition(float x, float y, Stage world) {
    if (! super.setPosition(x, y, world)) {
      return false;
    }
    
    final Tile o = origin();
    final LaunchHangar hangar = new LaunchHangar(base);
    if (! hangar.setPosition(o.x, o.y + ydim(), world)) {
      return false;
    }
    
    hangar.structure.assignGroup(this, hangar);
    this.  structure.assignGroup(this, hangar);
    return true;
  }
  
  
  public LaunchHangar childHangar() {
    for (Installation i : structure.asGroup()) if (i instanceof LaunchHangar) {
      return (LaunchHangar) i;
    }
    return null;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  final static String CAT_ORDERS = "ORDERS";
  
  public SelectionInfoPane configPanel(SelectionInfoPane panel, BaseUI UI) {
    panel = VenueDescription.configPanelWith(
      this, panel, UI, CAT_ORDERS, CAT_STATUS, CAT_STOCK, CAT_STAFF
    );
    if (panel.category() == CAT_ORDERS) {
      final Description d = panel.detail();
      
      d.append("Orders:");
      
      for (int i = 0 ; i < NUM_TYPES; i++) {
        final Traded t = ALL_TRADE_TYPES[i];
        final int type = tradeTypes[i], level = tradeLevels[i];
        
        d.append("\n  ");
        
        if (type == TRADE_IMPORT) d.append(new Description.Link("IMPORT") {
          public void whenTextClicked() {
            setTrading(t, TRADE_EXPORT, 0);
          }
        }, Colour.GREEN);
        if (type == TRADE_AUTO) d.append(new Description.Link("FREE TRADE") {
          public void whenTextClicked() {
            setTrading(t, TRADE_IMPORT, 0);
          }
        }, Colour.BLUE);
        if (type == TRADE_EXPORT) d.append(new Description.Link("EXPORT") {
          public void whenTextClicked() {
            setTrading(t, TRADE_AUTO  , 0);
          }
        }, Colour.MAGENTA);
        if (type != TRADE_AUTO) {
          d.append(" ");
          d.append(new Description.Link(I.lengthen(level, 4)) {
            public void whenTextClicked() {
              setTrading(t, type, (level == MAX_TRADE) ? 0 : (level * 2));
            }
          });
          d.append(" ");
        }
        else d.append("  ");
        d.append(t);
      }
    }
    return panel;
  }
  
  
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
  
  
  /*
  public void renderSelection(Rendering rendering, boolean hovered) {
    BaseUI.current().selection.renderTileOverlay(
      rendering, world,
      hovered ? Colour.transparency(0.5f) : Colour.WHITE,
      Selection.SELECT_OVERLAY, true, this, this
    );
  }
  //*/
}




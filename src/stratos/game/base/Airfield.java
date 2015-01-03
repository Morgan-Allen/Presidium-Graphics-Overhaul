

package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
//import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.economic.Economy.*;
import static stratos.game.actors.Backgrounds.*;



//  TODO:  You need some staff and pilots here, to provide security escorts for
//  shipping, along with fueling-options.


public class Airfield extends Venue {
  
  /**  Constructors, data fields, setup and save/load methods-
    */
  final static String IMG_DIR = "media/Buildings/merchant/";
  
  final static ModelAsset
    MODEL_BASE = CutoutModel.fromSplatImage(
      Airfield.class, IMG_DIR+"airfield_base.png", 6.0f
    ),
    MODEL_ROOF = CutoutModel.fromImage(
      Airfield.class, IMG_DIR+"airfield_roof.png", 6.0f, 3.0f
    );
  final static ImageAsset ICON = ImageAsset.fromImage(
    Airfield.class, "media/GUI/Buttons/airfield_button.gif"
  );
  
  
  final static Traded
    ALL_TRADE_TYPES[] = ALL_MATERIALS,
    ALL_SERVICES[] = (Traded[]) Visit.compose(Traded.class,
      ALL_TRADE_TYPES, new Traded[] { SERVICE_COMMERCE }
    );
  final public static int
    TRADE_IMPORT = -1,
    TRADE_EXPORT =  1,
    TRADE_AUTO   =  0,
    MIN_TRADE    = 5 ,
    MAX_TRADE    = 40,
    NUM_TYPES    = ALL_TRADE_TYPES.length;
  
  final byte
    tradeLevels[] = new byte[NUM_TYPES],
    tradeTypes [] = new byte[NUM_TYPES];
  
  private float fuelLevels;
  private Dropship docking;
  
  
  public Airfield(Base base) {
    super(6, 3, ENTRANCE_WEST, base);
    structure.setupStats(
      250,  //integrity
      5,    //armour
      400,  //build cost
      Structure.NORMAL_MAX_UPGRADES,
      Structure.TYPE_VENUE
    );
    staff.setShiftType(SHIFTS_BY_HOURS);
    
    final GroupSprite sprite = new GroupSprite();
    sprite.attach(MODEL_BASE, 0   ,  0   ,  0    );
    sprite.attach(MODEL_ROOF, 0   ,  0   ,  0    );
    sprite.setSortMode(GroupSprite.SORT_BY_ADDITION);
    attachSprite(sprite);
  }
  

  public Airfield(Session s) throws Exception {
    super(s);
    s.loadByteArray(tradeLevels);
    s.loadByteArray(tradeTypes );
    fuelLevels = s.loadFloat();
    docking    = (Dropship) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveByteArray(tradeLevels);
    s.saveByteArray(tradeTypes );
    s.saveFloat (fuelLevels);
    s.saveObject(docking   );
  }
  
  
  
  /**  Docking functions-
    */
  public Boarding[] canBoard() {
    //  TODO:  Cache this.
    final Batch <Boarding> CB = new Batch <Boarding> ();
    if (mainEntrance() != null) CB.add(mainEntrance());
    
    if (docking != null) CB.include(docking);
    for (Mobile m : inside()) if (m instanceof Boarding) {
      CB.include((Boarding) m);
    }
    return CB.toArray(Boarding.class);
  }
  
  
  public Dropship docking() {
    return docking;
  }
  
  
  public void setToDock(Dropship ship) {
    docking = ship;
  }

  
  public Vec3D dockLocation(Dropship ship) {
    final Vec3D DL = this.origin().position(null);
    DL.x += 0.5f + 1.5f;
    DL.y += 2.0f + 1.5f;
    return DL;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    if (docking != null && ! docking.inWorld()) docking = null;
    
    //
    //  TODO:  As long as you have power and LCHC, you can manufacture fuel for
    //  dropships...
    stocks.forceDemand(POWER, 5, TIER_CONSUMER);
    
    final int interval = (int) scheduledInterval();
    for (int i = 0 ; i < NUM_TYPES; i++) {
      final Traded t = ALL_TRADE_TYPES[i];
      final int type = tradeTypes[i], level = tradeLevels[i];
      
      if      (type == TRADE_AUTO  ) {
        stocks.incDemand(t, 0, TIER_TRADER, interval, this);
      }
      else if (type == TRADE_IMPORT) {
        stocks.forceDemand(t, level, TIER_IMPORTER);
      }
      else if (type == TRADE_EXPORT) {
        stocks.forceDemand(t, level, TIER_EXPORTER);
      }
    }
  }
  
  
  public void setTrading(Traded t, int type, int level) {
    final int index = Visit.indexOf(t, ALL_TRADE_TYPES);
    tradeLevels[index] = (byte) Nums.clamp(level, MIN_TRADE, MAX_TRADE);
    tradeTypes [index] = (byte) type ;
  }
  
  
  public Traded[] services() {
    return ALL_SERVICES;
  }
  
  
  public Background[] careers() {
    return new Background[] { WINGMAN, DECK_HAND };
  }
  
  
  protected int numOpenings(Background b) {
    final int nO = super.numOpenings(b);
    if (b == WINGMAN  ) return nO + 1;
    if (b == DECK_HAND) return nO + 1;
    return 0;
  }
  
  
  protected Behaviour jobFor(Actor actor, boolean onShift) {
    if (! onShift) return null;
    final Choice choice = new Choice(actor);
    
    if (actor.vocation() == DECK_HAND) {
      final Traded goods[] = services();
      final Delivery d = DeliveryUtils.bestBulkDeliveryFrom(
        this, goods, 1, 5, 5
      );
      choice.add(d);
      final Delivery c = DeliveryUtils.bestBulkCollectionFor(
        this, goods, 1, 5, 5
      );
      choice.add(c);
    }
    
    return choice.weightedPick();
  }
  
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "airfield");
  }
  
  
  public String fullName() {
    return "Airfield";
  }
  
  
  public String helpInfo() {
    return
      "The Airfield provides smaller dropships with a convenient site to "+
      "land and refuel, so facilitating offworld trade and migration as "+
      "well as local air defence.";
  }
  
  
  public String objectCategory() {
    return UIConstants.TYPE_MERCHANT;
  }

  final static String CAT_ORDERS = "ORDERS";
  
  
  public SelectionInfoPane configPanel(SelectionInfoPane panel, BaseUI UI) {
    panel = VenueDescription.configStandardPanel(
      this, panel, UI, CAT_ORDERS
    );
    if (panel.category() == CAT_ORDERS) {
      final Description d = panel.listing();
      d.append("Orders:");
      
      for (int i = 0 ; i < NUM_TYPES; i++) {
        final Traded t = ALL_TRADE_TYPES[i];
        final int type = tradeTypes[i], level = tradeLevels[i];
        
        d.append("\n  ");
        
        if (type == TRADE_IMPORT) d.append(new Description.Link("IMPORT") {
          public void whenClicked() {
            setTrading(t, TRADE_EXPORT, 0);
          }
        }, Colour.GREEN);
        if (type == TRADE_AUTO) d.append(new Description.Link("FREE TRADE") {
          public void whenClicked() {
            setTrading(t, TRADE_IMPORT, 0);
          }
        }, Colour.BLUE);
        if (type == TRADE_EXPORT) d.append(new Description.Link("EXPORT") {
          public void whenClicked() {
            setTrading(t, TRADE_AUTO  , 0);
          }
        }, Colour.MAGENTA);
        if (type != TRADE_AUTO) {
          d.append(" ");
          d.append(new Description.Link(I.lengthen(level, 4)) {
            public void whenClicked() {
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
}

















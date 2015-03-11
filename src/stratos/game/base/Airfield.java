

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
  
  final static VenueProfile PROFILE = new VenueProfile(
    Airfield.class, "airfield", "Airfield",
    6, 3, false, new VenueProfile[] { StockExchange.PROFILE, TrooperLodge.PROFILE }
  );
  
  private float fuelLevels;
  private Dropship docking;
  
  
  public Airfield(Base base) {
    super(PROFILE, base);
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
    fuelLevels = s.loadFloat();
    docking    = (Dropship) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
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
    stocks.setDefaultTier(ALL_TRADE_TYPES, Tier.TRADER);
    stocks.forceDemand(POWER, 5, Tier.CONSUMER);
  }
  

  public int spaceFor(Traded t) {
    return 40;
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
  
  
  public String helpInfo() {
    return
      "The Airfield provides smaller dropships with a convenient site to "+
      "land and refuel, facilitating offworld trade and migration as "+
      "well as local air defence.";
  }
  
  
  public String objectCategory() {
    return UIConstants.TYPE_COMMERCE;
  }

  final static String CAT_ORDERS = "ORDERS";
  
  
  public SelectionInfoPane configPanel(SelectionInfoPane panel, BaseUI UI) {
    return VenuePane.configStandardPanel(this, panel, UI, true);
  }
}

















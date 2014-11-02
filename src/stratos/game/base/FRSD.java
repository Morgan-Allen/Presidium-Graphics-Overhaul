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

import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.building.Economy.*;



//  Okay.  I want individual control over import levels.



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
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> ();
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES; }
  
  final static Traded ALL_TRADE_TYPES[] = {
    CARBS, PROTEIN, GREENS, LCHC,
    ORES, TOPES, PARTS, PLASTICS
  };
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates);
    if (! structure.intact()) return;
    
    final int interval = (int) scheduledInterval();
    for (Traded type : ALL_TRADE_TYPES) {
      stocks.incDemand(type, 0, Stocks.TIER_TRADER, interval, this);
    }
  }
  
  
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null;
    final Choice choice = new Choice(actor);
    
    final Plan r = Repairs.getNextRepairFor(actor, false);
    choice.add(r);
    
    
    //  TODO:  There's a problem at the moment where deliveries directly to
    //  housing can occur.  They should not be happening.
    
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




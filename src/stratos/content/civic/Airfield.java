/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.civic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.plans.*;
import stratos.game.verse.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.util.*;

import static stratos.game.actors.Backgrounds.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.craft.Economy.*;



//  TODO:  You need some staff and pilots here, to provide security escorts for
//  shipping, along with fueling-options.


public class Airfield extends Venue implements EntryPoints.Docking {
  
  /**  Constructors, data fields, setup and save/load methods-
    */
  final static String IMG_DIR = "media/Buildings/merchant/";
  
  final static ModelAsset
    MODEL_BASE = CutoutModel.fromSplatImage(
      Airfield.class, "airfield_base_model", IMG_DIR+"airfield_base.png", 6
    ),
    MODEL_ROOF = CutoutModel.fromImage(
      Airfield.class, "airfield_roof_model", IMG_DIR+"airfield_roof.png", 6, 3
    );
  final static ImageAsset ICON = ImageAsset.fromImage(
    Airfield.class, "airfield_icon", "media/GUI/Buttons/airfield_button.gif"
  );
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    Airfield.class, "airfield",
    "Airfield", Target.TYPE_COMMERCE, ICON,
    "The Airfield provides smaller dropships with a convenient site to "+
    "land and refuel, facilitating offworld trade and migration.",
    6, 1, Structure.IS_NORMAL,
    Owner.TIER_FACILITY, 250, 5,
    SERVICE_SECURITY, SERVICE_DOCKING, DECK_HAND, SHIP_CAPTAIN
  );
  
  final public static Upgrade LEVELS[] = BLUEPRINT.createVenueLevels(
    Upgrade.SINGLE_LEVEL,
    new Upgrade[] { SupplyDepot.LEVELS[0], Bastion.LEVELS[0] },
    new Object[] { 5, PILOTING, 5, HARD_LABOUR },
    400
  );
  
  private float fuelLevels;
  private Vehicle docking;
  
  
  public Airfield(Base base) {
    super(BLUEPRINT, base);
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
    docking    = (Vehicle) s.loadObject();
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
  
  
  public boolean allowsDocking(Vehicle docks) {
    return docks instanceof Dropship & docking == null;
  }
  
  
  public boolean isDocked(Vehicle docks) {
    return docking == docks;
  }
  
  
  public Series <Vehicle> docked() {
    return new Batch(docking);
  }
  
  
  public Vec3D dockLocation(Vehicle docks) {
    //  TODO:  No magic numbers please!
    final Vec3D DL = this.origin().position(null);
    DL.x += 0.5f + 1.5f;
    DL.y += 2.0f + 1.5f;
    return DL;
  }
  
  
  public void setAsDocked(Vehicle docks, boolean is) {
    if (is) this.docking = docks;
    else this.docking = null;
  }
  
  
  public static boolean isGoodDockSite(Boarding dropPoint) {
    if (! (dropPoint instanceof Airfield)) return false;
    final Airfield airfield = (Airfield) dropPoint;
    
    //  TODO:  Implement refueling at the Airfield!
    //if (airfield.fuelLevels <= 0) return false;
    return true;
  }
  


  /**  Behaviour implementation-
    */
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    if (docking != null && ! docking.inWorld()) docking = null;
    
    //  TODO:  As long as you have power and fuel rods, you can manufacture
    //  fuel for dropships.
    stocks.forceDemand(POWER, 5, 0);
  }
  
  
  protected int numPositions(Background b) {
    final int level = structure.mainUpgradeLevel();
    if (b == DECK_HAND) return level + 1;
    return 0;
  }
  
  
  protected Behaviour jobFor(Actor actor) {
    final Choice choice = new Choice(actor);
    
    final boolean shouldHaul =
      actor.mind.vocation() == DECK_HAND &&
      docking               != null      &&
      docking.flightState() == Dropship.STATE_LANDED;
    if (shouldHaul) {
      final Traded goods[] = docking.services();
      final Bringing d = BringUtils.bestBulkDeliveryFrom(
        docking, goods, 1, 5, 5, true
      );
      choice.add(d);
      final Bringing c = BringUtils.bestBulkCollectionFor(
        docking, goods, 1, 5, 5, true
      );
      choice.add(c);
    }
    
    //  TODO:  ADD SUPERVISION TASKS
    
    return choice.weightedPick();
  }
}

















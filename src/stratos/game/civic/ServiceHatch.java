/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.civic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.game.wild.Vermin;
import static stratos.game.economic.Economy.*;



public class ServiceHatch extends Venue {
  
  
  /**  Data fields, constructors and save/load methods:
    */
  final public static ModelAsset MODEL = CutoutModel.fromSplatImage(
    ServiceHatch.class, "media/Buildings/civilian/access_hatch_2.png", 2
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    ServiceHatch.class, "media/GUI/Buttons/access_hatch_button.gif"
  );
  
  final static Blueprint BLUEPRINT = new Blueprint(
    ServiceHatch.class, "service_hatch",
    "Service Hatch", UIConstants.TYPE_HIDDEN,
    2, 1, IS_FIXTURE,
    Bastion.BLUEPRINT, Owner.TIER_PRIVATE
  );
  
  
  public ServiceHatch(Base base) {
    super(BLUEPRINT, base);
    structure.setupStats(
      40,  // integrity
      8 ,  // armour
      30,  // build cost
      0 ,  // max. upgrades
      Structure.TYPE_FIXTURE
    );
    attachModel(MODEL);
  }
  

  public ServiceHatch(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  public int pathType() {
    return Tile.PATH_ROAD;
  }
  
  
  
  /**  Updates and economic methods-
    */
  public Background[] careers() { return null; }
  public Traded[] services() { return null; }
  
  
  public float ratePlacing(Target point, boolean exact) {
    //  TODO:  These are far too damn ugly.  Work out a way to make them look
    //  better.
    if (true) return 0;
    
    Stage world = point.world();
    final Object key = ServiceHatch.class;
    final float range = Stage.ZONE_SIZE / 2, abutRange = 2 + 1;
    
    if (exact) {
      Target near = null;
      near = world.presences.nearestMatch(base, this, abutRange);
      if (near == null) return -1;
      near = world.presences.nearestMatch(key, this, range);
      if (near != null) return -1;
      return 1;
    }
    else {
      float supply = base.demands.supplyAround(point, key, range);
      return 1 * (1 - supply);
    }
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    structure.assignOutputs(
      Item.withAmount(ATMO , 2),
      Item.withAmount(POWER, 1)
    );
    structure.setAmbienceVal(-2);
  }
  
  
  protected void updatePaving(boolean inWorld) {
    base.transport.updatePerimeter(this, inWorld);
  }
  
  
  public boolean allowsEntry(Mobile m) {
    if (m instanceof Vermin) return true;
    else return false;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "service_hatch");
  }
  
  
  public String helpInfo() {
    return
      "Service Hatches are neccesary for maintenance of urban "+
      "infrastructure, but can allow entry to dangerous vermin.";
  }
}











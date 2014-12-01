


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.util.*;



public class ItemDisposal extends Plan {
  
  
  /**  Data fields, constructors, and save/load functionality-
    */
  private static boolean verbose = true;
  
  
  public ItemDisposal(Actor actor) {
    super(actor, actor, true);
  }
  
  
  public ItemDisposal(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected float getPriority() {
    return CASUAL;
  }
  
  
  protected Behaviour getNextStep() {
    final Batch <Traded> goodTypes = new Batch <Traded> ();
    for (Item i : actor.gear.allItems()) {
      goodTypes.add(i.type);
    }
    //  TODO:  This is a problem.  You need a list of associated demands in
    //  order for this to work.  Eh... I can just move that to the Inventory
    //  class, in that case.
    
    final Delivery d = DeliveryUtils.bestBulkDeliveryFrom(
      actor, goodTypes.toArray(Traded.class), 1, 5, 5
    );
    /*
    final Delivery d = Deliveries.nextDeliveryFor(
      actor, actor, goodTypes.toArray(Service.class), 1, actor.world()
    );
    //*/
    if (d != null) return d;
    
    //  If you, home, or work are carrying anything that you, home or work
    //  would value, transfer it.
    //  If you, home or work are carrying anything manifestly useless, get rid
    //  of it.
    //  If you can, try selling these things off at a local vendor.
    //  If you spot a nearby unattended object, acquire it.
    
    return null;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    super.needsSuffix(d, "");
  }
}





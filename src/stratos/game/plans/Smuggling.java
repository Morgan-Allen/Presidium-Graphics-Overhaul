

package stratos.game.plans;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.economic.*;
import stratos.util.*;



public class Smuggling extends Plan {// implements Offworld.Activity {
  
  
  private Dropship vessel;
  private Item[] moved;
  
  
  public Smuggling(Actor actor, Venue from) {
    super(actor, from, true, NO_HARM);
  }
  
  
  public Smuggling(Session s) throws Exception {
    super(s);
    vessel = (Dropship) s.loadObject();
    moved = Item.loadItemsFrom(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(vessel);
    Item.saveItemsTo(s, moved);
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected float getPriority() {
    return 0;
  }
  
  
  protected Behaviour getNextStep() {
    return null;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
  }
}











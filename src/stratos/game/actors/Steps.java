


package stratos.game.actors;
import stratos.game.common.*;
import stratos.game.common.Session.Saveable;
import stratos.user.*;
import stratos.util.*;


//
//  TODO:  Generalise this to arbitrary behaviours.

public class Steps extends Plan {
  
  
  final float priority;
  final Stack <Action> actions = new Stack <Action> ();
  
  final String title;
  
  
  public Steps(
    Actor actor, Target key,
    float priority, boolean persistent, float harmFactor,
    String title,
    Action... actions
  ) {
    super(actor, key, persistent, harmFactor);
    this.priority = priority;
    for (Action a : actions) this.actions.add(a);
    
    this.title = title;
  }
  
  
  public Steps(Session s) throws Exception {
    super(s);
    priority = s.loadFloat();
    s.loadObjects(actions);
    
    title = s.loadString();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveFloat(priority);
    s.saveObjects(actions);
    
    s.saveString(title);
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  
  /**  
    */
  protected float getPriority() {
    return priority;
  }
  
  
  protected Behaviour getNextStep() {
    final int index = actions.indexOf((Action) lastStep) + 1;
    if (index >= actions.size()) return null;
    return actions.atIndex(index);
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    super.needsSuffix(d, title);
  }
}









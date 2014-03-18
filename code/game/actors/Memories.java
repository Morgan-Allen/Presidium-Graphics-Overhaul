


package code.game.actors ;
import code.game.common.*;
import code.game.common.Session.Saveable;
import code.util.*;



public class Memories {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final static int
    DEFAULT_MAX_MEMORY = 10,
    DEFAULT_MAX_ASSOC  = 100 ;
  
  private static boolean verbose = false ;
  
  
  final Actor actor ;
  final List <Plan> remembered ;
  final Table <Plan, List <Saveable>> associations ;
  //
  //  TODO:  You might want to use classes here instead of plans, at least for
  //  the association table.
  
  
  Memories(Actor actor) {
    this.actor = actor ;
    remembered = new List <Plan> () ;
    associations = new Table <Plan, List <Saveable>> () ;
  }
  
  
  protected void loadState(Session s) throws Exception {
    s.loadObjects(remembered) ;
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Plan key = (Plan) s.loadObject() ;
      final List <Saveable> associated = new List <Saveable> () ;
      s.loadObjects(associated) ;
      associations.put(key, associated) ;
    }
  }
  
  
  protected void saveState(Session s) throws Exception {
    s.saveObjects(remembered) ;
    s.saveInt(associations.size()) ;
    for (Plan p : associations.keySet()) {
      s.saveObject(p) ;
      s.saveObjects(associations.get(p)) ;
    }
  }
  
  
  /**  Modification and updates-
    */
  //
  //  TODO:  Allow for varying degrees of association.
  public void associateWithCurrentBehaviour(Saveable s) {
    final Behaviour current = actor.mind.rootBehaviour() ;
    if (current == null) return ;
    final List <Saveable> assoc = associations.get(current) ;
    assoc.include(s) ;
  }
  
  
  /**  Queries and data access-
    */
  public Series <Plan> remembered() {
    return remembered ;
  }
  
  
  public Series <Saveable> associationsFor(Plan p) {
    return associations.get(p) ;
  }
}






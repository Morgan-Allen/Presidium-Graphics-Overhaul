/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.actors ;
import stratos.game.common.*;
import stratos.util.*;



public class Activities {
  
  
  
  /**  Fields, constructors, and save/load methods.
    */
  final World world ;
  
  final Table <Target, List <Behaviour>> activeTable = new Table(1000) ;
  final Table <Behaviour, Behaviour> behaviours = new Table(1000) ;
  
  
  public Activities(World world) {
    this.world = world ;
  }
  
  
  public void loadState(Session s) throws Exception {
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Target t = s.loadTarget() ;
      final List <Behaviour> l = new List <Behaviour> () ;
      s.loadObjects(l) ;
      activeTable.put(t, l) ;
      
      //  TODO:  RESTORE AND TEST
      /*
      //
      //  Safety checks for can't-happen events-
      if (! t.inWorld()) {
        I.say(t+" IS NOT IN WORLD ANY MORE!") ;
      }
      for (Action a : l) {
        if (! a.actor.inWorld()) {
          I.say("  "+a+" BELONGS TO DEAD ACTOR!") ;
          l.remove(a) ;
        }
      }
      //*/
    }
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Behaviour b = (Behaviour) s.loadObject() ;
      behaviours.put(b, b) ;
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(activeTable.size()) ;
    ///I.say("Saving "+actions.size()+" actions in activity table.") ;
    for (Target t : activeTable.keySet()) {
      s.saveTarget(t) ;
      s.saveObjects(activeTable.get(t)) ;
    }
    s.saveInt(behaviours.size()) ;
    ///I.say("Saving "+behaviours.size()+" behaviours in behaviour table.") ;
    for (Behaviour b : behaviours.keySet()) {
      s.saveObject(b) ;
    }
  }
  
  
  
  /**  Asserting and asking after action registrations-
    */
  public void toggleBehaviour(Behaviour b, boolean is) {
    if (is) behaviours.put(b, b) ;
    else behaviours.remove(b) ;

    if (b == null) return ;
    /*
    if (! b.actor.inWorld()) {
      I.complain(b+" TOGGLED BY DEFUNCT ACTOR: "+a.actor) ;
    }
    //*/
    
    final Target t = b.subject() ;
    List <Behaviour> forT = activeTable.get(t) ;
    if (is) {
      if (forT == null) activeTable.put(t, forT = new List <Behaviour> ()) ;
      forT.include(b) ;
    }
    else if (forT != null) {
      forT.remove(b) ;
      if (forT.size() == 0) activeTable.remove(t) ;
    }
  }
  
  
  public boolean includes(Behaviour b) {
    return behaviours.get(b) != null ;
  }
  
  
  public boolean includes(Target t, Class behaviourClass) {
    final List <Behaviour> onTarget = activeTable.get(t) ;
    if (onTarget == null) return false ;
    for (Behaviour b : onTarget) {
      if (b.getClass() == behaviourClass) return true ;
    }
    return false ;
  }
  
  
  public boolean includesAction(Target t, String methodName) {
    final List <Behaviour> onTarget = activeTable.get(t) ;
    if (onTarget == null) return false ;
    for (Behaviour b : onTarget) if (b instanceof Action) {
      final String name = ((Action) b).methodName();
      if (name.equals(methodName)) return true;
    }
    return false ;
  }
  
  
  public Batch <Behaviour> targeting(Target t) {
    final Batch <Behaviour> batch = new Batch <Behaviour> () ;
    final List <Behaviour> onTarget = activeTable.get(t) ;
    if (onTarget == null) return batch ;
    for (Behaviour b : onTarget) batch.add(b) ;
    return batch ;
  }
  
  
  public Batch <Behaviour> actionMatches(Target t, Class planClass) {
    final Batch <Behaviour> batch = new Batch <Behaviour> ();
    final List <Behaviour> onTarget = activeTable.get(t);
    if (onTarget == null) return batch;
    
    for (Behaviour b : onTarget) if (b instanceof Action) {
      final Plan match = ((Action) b).actor.matchFor(planClass);
      if (match != null) batch.add(match);
    }
    return batch;
  }
}











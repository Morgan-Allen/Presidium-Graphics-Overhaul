/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors;
import stratos.game.common.*;
import stratos.util.*;
import java.util.Map.Entry;



public class Activities {
  
  
  
  /**  Fields, constructors, and save/load methods.
    */
  private static boolean
    saveVerbose = false,
    verbose     = false;
  
  final Stage world;
  final Table <Target   , List <Behaviour>> activeTable = new Table(1000);
  final Table <Behaviour, Target          > activeFoci  = new Table(1000);
  final private Batch NONE = new Batch();
  
  
  public Activities(Stage world) {
    this.world = world;
  }
  
  
  public void loadState(Session s) throws Exception {
    //final boolean report = saveVerbose;
    
    for (int n = s.loadInt(); n-- > 0;) {
      final Target t = s.loadTarget();
      final List <Behaviour> l = new List <Behaviour> ();
      s.loadObjects(l);
      activeTable.put(t, l);
    }
    for (int n = s.loadInt(); n-- > 0;) {
      final Behaviour b = (Behaviour) s.loadObject();
      final Target f = s.loadTarget();
      activeFoci.put(b, f);
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    final boolean report = saveVerbose;
    
    if (report) {
      I.say("\nSaving "+activeFoci.size()+" plans in activity table.");
    }
    s.saveInt(activeTable.size());
    for (Target t : activeTable.keySet()) {
      s.saveTarget(t);
      s.saveObjects(activeTable.get(t));
    }
    s.saveInt(activeFoci.size());
    
    for (Entry <Behaviour, Target> entry : activeFoci.entrySet()) {
      if (report) {
        I.say("  Behaviour:  "+entry.getKey  ());
        I.say("  Focus: "+entry.getValue());
      }
      s.saveObject(entry.getKey  ());
      s.saveTarget(entry.getValue());
    }
  }
  
  
  
  /**  Asserting and asking after action registrations-
    */
  public void registerFocus(Behaviour b, boolean is) {
    if (b == null) return; 
    registerFocus(b, b.subject(), is);
  }
  
  
  public void registerFocus(Behaviour b, Target focus, boolean is) {
    if (b == null) return;
    final Target oldFocus = activeFoci.get(b);
    Actor doing = null;
    if (b instanceof Plan  ) doing = ((Plan  ) b).actor();
    if (b instanceof Action) doing = ((Action) b).actor();
    
    boolean report = verbose && (
      I.talkAbout == focus ||
      I.talkAbout == oldFocus
    );
    
    if (oldFocus != null) {
      if (report) {
        I.say("\n"+doing+" removing old focus for "+b);
        I.say("  Focus was: "+oldFocus);
      }
      activeFoci.remove(b);
      final List <Behaviour> active = activeTable.get(oldFocus);
      active.remove(b);
      if (active.size() == 0) activeTable.remove(oldFocus);
      if (report) I.say("  ALL ACTIVE: "+active);
    }
    
    if (is) {
      if (report) {
        I.say("\n"+doing+" registering new focus for "+b);
        I.say("  Focus is: "+focus);
      }
      activeFoci.put(b, focus);
      List <Behaviour> active = activeTable.get(focus);
      
      if (active == null) {
        active  = new List <Behaviour> ();
        activeTable.put(focus, active);
      }
      active.include(b);
      if (report) I.say("  ALL ACTIVE: "+active);
    }
  }
  
  
  public boolean includes(Behaviour b) {
    return activeFoci.get(b) != null;
  }
  
  
  public Series <Behaviour> allTargeting(Target t) {
    final List <Behaviour> onTarget = activeTable.get(t);
    if (onTarget == null) return NONE;
    return onTarget;
  }
  
  
  public boolean includesActivePlan(Target t, Class planClass) {
    return activePlanMatches(t, planClass).size() > 0;
  }
  
  
  public Batch <Plan> activePlanMatches(Target t, Class planClass) {
    final Batch <Plan> batch = new Batch <Plan> ();
    final List <Behaviour> onTarget = activeTable.get(t);
    if (onTarget == null) return batch;
    for (Behaviour b : onTarget) if (b instanceof Plan) {
      if (planClass != null && b.getClass() != planClass) continue;
      batch.add((Plan) b);
    }
    return batch;
  }
  
  
  public Batch <Action> actionMatches(Target t) {
    final Batch <Action> batch = new Batch <Action> ();
    final List <Behaviour> onTarget = activeTable.get(t);
    if (onTarget == null) return batch;
    for (Behaviour b : onTarget) if (b instanceof Action) {
      final Action a = (Action) b;
      if (! a.hasBegun()) continue;
      batch.add(a);
    }
    return batch;
  }
}











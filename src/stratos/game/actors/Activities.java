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
  final Table <Target, List <Plan>> activeTable = new Table(1000);
  final Table <Plan  , Target     > activeFoci  = new Table(1000);
  
  
  public Activities(Stage world) {
    this.world = world;
  }
  
  
  public void loadState(Session s) throws Exception {
    final boolean report = saveVerbose;
    
    for (int n = s.loadInt(); n-- > 0;) {
      final Target t = s.loadTarget();
      final List <Plan> l = new List <Plan> ();
      s.loadObjects(l);
      activeTable.put(t, l);
      //
      //  Safety checks for can't-happen events-
      if (report) {
        if (! t.inWorld()) for (Plan a : l) {
          if (! a.actor().inWorld()) {
            I.say("  "+a+" BELONGS TO DEAD ACTOR!");
            l.remove(a);
          }
        }
      }
    }
    for (int n = s.loadInt(); n-- > 0;) {
      final Plan b = (Plan) s.loadObject();
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
    
    for (Entry <Plan, Target> entry : activeFoci.entrySet()) {
      if (report) {
        I.say("  Plan:  "+entry.getKey  ());
        I.say("  Focus: "+entry.getValue());
      }
      s.saveObject(entry.getKey  ());
      s.saveTarget(entry.getValue());
    }
  }
  
  
  
  /**  Asserting and asking after action registrations-
    */
  public void registerFocus(Behaviour b, boolean is) {
    if (! (b instanceof Plan)) return;
    registerFocus((Plan) b, b.subject(), is);
  }
  
  
  public void registerFocus(Plan b, Target focus, boolean is) {
    if (b == null) return;
    final Target oldFocus = activeFoci.get(b);
    
    final boolean report = verbose && (
      I.talkAbout == b.actor() ||
      I.talkAbout == focus ||
      I.talkAbout == oldFocus
    );
    
    if (oldFocus != null) {
      if (report) {
        I.say("\nRemoving old focus for "+b);
        I.say("  Focus was: "+oldFocus+" for "+b.actor());
      }
      activeFoci.remove(b);
      final List <Plan> active = activeTable.get(oldFocus);
      active.remove(b);
      if (active.size() == 0) activeTable.remove(oldFocus);
    }
    
    if (is) {
      if (report) {
        I.say("\nRegistering new focus for "+b);
        I.say("  Focus is: "+focus+" for "+b.actor());
      }
      activeFoci.put(b, focus);
      List <Plan> active = activeTable.get(focus);
      if (active == null) activeTable.put(focus, active = new List <Plan> ());
      active.include(b);
    }
  }
  
  
  public boolean includes(Behaviour b) {
    return activeFoci.get(b) != null;
  }
  
  
  public Batch <Plan> allTargeting(Target t) {
    final Batch <Plan> batch = new Batch <Plan> ();
    final List <Plan> onTarget = activeTable.get(t);
    if (onTarget == null) return batch;
    for (Plan b : onTarget) batch.add(b);
    return batch;
  }
  
  
  public boolean includesActivePlan(Target t, Class planClass) {
    return activePlanMatches(t, planClass).size() > 0;
  }
  
  
  public Batch <Plan> activePlanMatches(Target t, Class planClass) {
    final Batch <Plan> batch = new Batch <Plan> ();
    final List <Plan> onTarget = activeTable.get(t);
    if (onTarget == null) return batch;
    for (Plan b : onTarget) {
      if (planClass != null && b.getClass() != planClass) continue;
      if (b.actor.actionFocus() == t) batch.add(b);
    }
    return batch;
  }
}











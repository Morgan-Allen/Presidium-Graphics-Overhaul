/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors;
import stratos.game.common.*;
import stratos.game.politic.*;
import stratos.util.*;
import stratos.game.wild.Species;
import stratos.game.economic.Property;



public abstract class ActorMind implements Qualities {
  
  
  /**  Field definitions, constructor, save/load methods-
    */
  private static boolean
    decisionVerbose = Choice.verbose,
    stepsVerbose    = Choice.verbose;
  
  
  final protected Actor actor;
  
  final List <Behaviour> agenda   = new List <Behaviour> ();
  final List <Behaviour> todoList = new List <Behaviour> ();
  
  protected Mission mission;
  protected Property home, work;
  protected Actor master;
  
  
  
  protected ActorMind(Actor actor) {
    this.actor = actor;
  }
  
  
  protected void loadState(Session s) throws Exception {
    s.loadObjects(agenda);
    s.loadObjects(todoList);
    
    mission = (Mission) s.loadObject();
    home = (Property) s.loadObject();
    work = (Property) s.loadObject();
    //application = (Application) s.loadObject();
    master = (Actor) s.loadObject();
  }
  
  
  protected void saveState(Session s) throws Exception {
    s.saveObjects(agenda);
    s.saveObjects(todoList);
    
    s.saveObject(mission);
    s.saveObject(home);
    s.saveObject(work);
    //s.saveObject(application);
    s.saveObject(master);
  }
  
  
  protected void onWorldExit() {
  }
  
  
  
  /**  Calling regular, periodic updates and triggering AI refreshments-
    */
  protected void updateAI(int numUpdates) {
    if (numUpdates % 10 != 0) return;
    final boolean report = decisionVerbose && I.talkAbout == actor;
    //
    //  Remove any expired behaviour-sources:
    if (home != null && home.destroyed()) {
      setHome(null);
    }
    if (work != null && work.destroyed()) {
      setWork(null);
    }
    if (mission != null && mission.finished()) {
      assignMission(null);
    }
    //
    //  Cull any expired items on the to-do list, and see if it's worth
    //  switching to a different behaviour-
    for (Behaviour b : todoList) if (b.finished()) todoList.remove(b);
    final Behaviour last = rootBehaviour();
    final Behaviour next = nextBehaviour();
    
    if (report) {
      I.say("\nPerformed periodic AI update.");
      final float
        lastP = last == null ? -1 : last.priorityFor(actor),
        nextP = next == null ? -1 : next.priorityFor(actor);
      I.say("  LAST PLAN: "+last+" "+lastP);
      I.say("  NEXT PLAN: "+next+" "+nextP);
    }
    if (Choice.wouldSwitch(actor, last, next, true, false)) {
      assignBehaviour(next);
      if (report) I.say("  Switching to next plan!");
    }
  }
  
  
  public Behaviour nextBehaviour() {
    final boolean report = decisionVerbose && I.talkAbout == actor;
    if (report) I.say("\n\nACTOR IS GETTING NEXT BEHAVIOUR...");
    
    if (report) I.say("\nGetting next from to-do list:");
    final Choice fromTodo = new Choice(actor, todoList);
    final Behaviour notDone = fromTodo.pickMostUrgent();
    if (report && fromTodo.empty()) I.say("  Nothing on todo list.");
    
    if (report) I.say("\nGetting newly created behaviour:");
    final Choice fromNew = createNewBehaviours(new Choice(actor));
    final Behaviour newChoice = fromNew.weightedPick();
    if (report && fromNew.empty()) I.say("  No new behaviour.");
    
    final Behaviour taken =
      Choice.wouldSwitch(actor, notDone, newChoice, false, false) ?
      newChoice : notDone;
    
    if (report) {
      I.say("\nNext plans acquired...");
      I.say("  Last plan: "+rootBehaviour());
      final float
        notP = notDone == null ? -1 : notDone.priorityFor(actor),
        newP = newChoice == null ? -1 : newChoice.priorityFor(actor);
      I.say("  From Todo:  "+notDone+  "  (priority "+notP+")");
      I.say("  New choice: "+newChoice+"  (priority "+newP+")");
      I.say("TAKEN: "+taken);
    }
    return taken;
  }
  
  
  public Series <Plan> getBehaviourRange() {
    final Choice fromNew = createNewBehaviours(new Choice(actor));
    final Batch <Plan> range = new Batch <Plan> ();
    for (Behaviour b : fromNew.plans) if (b instanceof Plan) {
      range.add((Plan) b);
    }
    return range;
  }
  
  
  protected abstract Choice createNewBehaviours(Choice choice);
  protected abstract void addReactions(Target m, Choice choice);
  protected abstract void putEmergencyResponse(Choice choice);
  
  
  protected Action getNextAction() {
    final boolean report = I.talkAbout == actor && stepsVerbose;
    final int MAX_LOOP = 20;  // Safety feature (see below.)
    
    for (int loop = MAX_LOOP; loop-- > 0;) {
      if (report) {
        I.say("\n"+actor+" in action-decision loop:");
        for (Behaviour b : agenda) I.say("  "+b);
      }
      //
      //  If all current behaviours are complete, generate a new one.
      if (agenda.size() == 0) {
        final Behaviour taken = nextBehaviour();
        if (report) {
          I.say("\nCurrent agenda was empty!");
          I.say("  Next behaviour: "+taken);
        }
        if (taken == null) return null;
        assignBehaviour(taken);
      }
      //
      //  Root behaviours which return null, but aren't complete, should be
      //  stored for later.  Otherwise, unfinished behaviours should return
      //  their next step.
      final Behaviour current = topBehaviour(), root = rootBehaviour();
      if (current == null) continue;
      final Behaviour next = current.nextStepFor(actor);
      final boolean
        isDone  = current.finished(),
        doLater = current == root && current.persistent() && ! isDone;
      
      if (report) {
        I.say("");
        I.say("  Current step:   "+current);
        I.say("  Class type:     "+current.getClass().getSimpleName());
        I.say("  Next step:      "+next);
        I.say("  Done:           "+isDone);
        I.say("  Will do later:  "+doLater);
      }
      if (isDone || next == null) {
        if (report) I.say("  Popping current step...");
        if (doLater) todoList.add(current);
        popBehaviour(current);
      }
      else if (next instanceof Action) {
        if (report) I.say("  Returning action!");
        return (Action) next;
      }
      else {
        if (report) I.say("  Pushing next step...");
        pushBehaviour(next);
      }
    }
    //
    //  If you exhaust the maximum number of iterations (which I assume *would*
    //  be enough for any reasonable use-case,) report the problem.
    I.say("\n"+actor+" COULD NOT DECIDE ON NEXT STEP.");
    final Behaviour root = rootBehaviour();
    final Behaviour next = root == null ? null : root.nextStepFor(actor);
    I.say("  Root behaviour: " + root);
    I.say("  Next step:      " + next);
    if (next != null) {
      I.say("  Valid/finished  " + next.valid() + "/" + next.finished());
    }
    I.reportStackTrace();
    return null;
  }
  
  
  protected boolean needsUpdate() {
    final Behaviour current = topBehaviour(), root = rootBehaviour();
    if (current instanceof Action && current == root) return false;
    if (root    == null || root   .finished()) return true;
    if (current == null || current.finished()) return true;
    return false;
  }
  
  
  
  /**  Setting home and work venues & applications, plus missions-
    */
  public void setWork(Property e) {
    if (work == e) return;
    if (work != null) work.staff().setWorker(actor, false);
    work = e;
    if (work != null) work.staff().setWorker(actor, true);
  }
  
  
  public Property work() {
    return work;
  }
  
  
  public void setHome(Property home) {
    final Property old = this.home;
    if (old == home) return;
    if (old != null) old.staff().setResident(actor, false);
    this.home = home;
    if (home != null) home.staff().setResident(actor, true);
  }
  
  
  public Property home() {
    return home;
  }
  
  
  public void assignMission(Mission mission) {
    //  TODO:  Add some safety checks here...
    
    final Mission oldMission = this.mission;
    if (mission == oldMission) return;
    this.mission = mission;
    
    if (oldMission != null) {
      cancelBehaviour(oldMission);
      oldMission.setApplicant(actor, false);
    }
    if (mission != null) {
      mission.setApplicant(actor, true);
    }
  }
  
  
  public Mission mission() {
    return mission;
  }
  
  
  public void assignMaster(Actor master) {
    this.master = master;
  }
  
  
  public Actor master() {
    return master;
  }
  
  
  
  /**  Methods related to maintaining the agenda stack-
    */
  private void pushBehaviour(Behaviour b) {
    if (todoList.includes(b)) todoList.remove(b);
    agenda.addFirst(b);
    if (stepsVerbose && I.talkAbout == actor) {
      I.say("\nPUSHING BEHAVIOUR: "+b);
    }
    actor.world().activities.registerFocus(b, true);
  }
  
  
  private Behaviour popBehaviour(Behaviour toPop) {
    if (toPop != null && agenda.first() != toPop) {
      return toPop;
    }
    final Behaviour b = agenda.removeFirst();
    if (stepsVerbose && I.talkAbout == actor) {
      I.say("\nPOPPING BEHAVIOUR: "+b);
      I.say("  Finished/valid: "+b.finished()+"/"+b.valid());
      I.say("  Priority "+b.priorityFor(actor));
    }
    actor.world().activities.registerFocus(b, false);
    return b;
  }
  
  
  public void assignToDo(Behaviour toDo) {
    if (wouldSwitchTo(toDo)) assignBehaviour(toDo);
    else todoList.include(toDo);
  }
  
  
  public void assignBehaviour(Behaviour behaviour) {
    if (behaviour == null) I.complain("CANNOT ASSIGN NULL BEHAVIOUR.");
    final boolean report = decisionVerbose && I.talkAbout == actor;
    
    if (report) I.say("Assigning behaviour "+behaviour);
    actor.assignAction(null);
    
    final Behaviour replaced = rootBehaviour();
    cancelBehaviour(replaced);
    
    if (replaced != null && ! replaced.finished() && replaced.persistent()) {
      if (report) I.say(" SAVING PLAN AS TODO: "+replaced);
      todoList.include(replaced);
    }
    
    pushBehaviour(behaviour);
  }
  
  
  public void pushFromParent(Behaviour b, Behaviour parent) {
    if (! agenda.includes(parent)) {
      //I.complain("Behaviour not active.");
      return;
    }
    cancelBehaviour(parent);
    pushBehaviour(parent);
    pushBehaviour(b);
    actor.assignAction(null);
  }
  
  
  public void cancelBehaviour(Behaviour b) {
    if (b == null) return;
    if (decisionVerbose && I.talkAbout == actor) {
      I.say("\nCANCELLING "+b);
    }
    if (agenda.includes(b)) while (agenda.size() > 0) {
      final Behaviour popped = popBehaviour(null);
      if (popped == b) break;
    }
    if (agenda.includes(b)) I.complain("Duplicate behaviour!");
    todoList.remove(b);
    actor.assignAction(null);
  }
  
  
  public boolean wouldSwitchTo(Behaviour next) {
    if ((! actor.health.conscious()) || (! actor.inWorld())) return false;
    final boolean report = decisionVerbose && I.talkAbout == actor;
    return Choice.wouldSwitch(actor, rootBehaviour(), next, true, report);
  }
  
  
  public boolean mustIgnore(Behaviour next) {
    if (! actor.health.conscious()) return true;
    final boolean report = decisionVerbose && I.talkAbout == actor;
    return Choice.wouldSwitch(actor, next, rootBehaviour(), false, report);
  }
  
  
  public void clearAgenda() {
    if (rootBehaviour() != null) cancelBehaviour(rootBehaviour());
    todoList.clear();
  }
  
  
  public Series <Behaviour> agenda() {
    return agenda;
  }
  
  
  public Behaviour topBehaviour() {
    return agenda.first();
  }
  
  
  public Behaviour rootBehaviour() {
    return agenda.last();
  }
  
  
  public boolean hasToDo(Class planClass) {
    for (Behaviour b : agenda  ) if (b.getClass() == planClass) return true;
    for (Behaviour b : todoList) if (b.getClass() == planClass) return true;
    return false;
  }
  
  
  public boolean hasToDo(Behaviour b) {
    if (agenda  .includes(b)) return true;
    if (todoList.includes(b)) return true;
    return false;
  }
}


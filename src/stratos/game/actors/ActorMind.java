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
    decisionVerbose = Choice.mindVerbose,
    stepsVerbose    = Choice.mindVerbose;
  
  
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
    master = (Actor) s.loadObject();
  }
  
  
  protected void saveState(Session s) throws Exception {
    s.saveObjects(agenda);
    s.saveObjects(todoList);
    
    s.saveObject(mission);
    s.saveObject(home);
    s.saveObject(work);
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
      Choice.wouldSwitch(actor, notDone, newChoice, true, report) ?
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
    //
    //  Firstly, check to ensure that our root behaviour is still valid- if
    //  not, you'll need to pick out a new one:
    Behaviour root = rootBehaviour();
    if (report) {
      I.say("\nGETTING NEXT ACTION FOR "+actor);
      I.say("  Current root behaviour: "+I.tagHash(root));
    }
    if (! Plan.canFollow(actor, root)) {
      if (Plan.canPersist(root)) todoList.add(root);
      root = nextBehaviour();
      if (report) {
        I.say("  Current agenda was empty!");
        I.say("  New root behaviour: "+I.tagHash(root));
      }
    }
    //
    //  Then, delete all existing entries from the agenda.
    for (Behaviour b : agenda) popBehaviour(b);
    if (! Plan.canFollow(actor, root)) {
      I.say("  CANNOT FOLLOW PLAN: "+root);
      return null;
    }
    //
    //  Then, keep pushing new behaviours until you squeeze an action out,
    //  which you can then return-
    final int MAX_LOOP = 20;
    Behaviour next = root;
    for (int loop = MAX_LOOP; loop-- > 0;) {
      pushBehaviour(next);
      next = next.nextStepFor(actor);
      final boolean valid = Plan.canFollow(actor, next);
      if (report) I.say("  Next step: "+next+", valid? "+valid);
      if (! Plan.canFollow(actor, next)) break;
      else if (next instanceof Action) return (Action) next;
    }
    
    cancelBehaviour(root);
    //
    //  If you exhaust the maximum number of iterations (which I assume *would*
    //  be enough for any reasonable use-case,) report the problem.
    I.say("\n"+actor+" COULD NOT DECIDE ON NEXT STEP.");
    I.say("  Root behaviour: "+root);
    I.say("  Next step:      "+next   );
    if (next != null) {
      I.say("  Valid/finished  "+next.valid()+"/"+ next.finished());
    }
    I.reportStackTrace();
    return null;
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
    final boolean report = I.talkAbout == actor && decisionVerbose;
    
    if (report) I.say("\nASSIGNING BEHAVIOUR "+behaviour);
    actor.assignAction(null);
    
    final Behaviour replaced = rootBehaviour();
    cancelBehaviour(replaced);
    
    if (Plan.canPersist(replaced)) {
      if (report) I.say("  SAVING PLAN AS TODO: "+replaced);
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
    final boolean report = I.talkAbout == actor && decisionVerbose;
    if (! agenda.includes(b)) return;
    if (report) I.say("\nCANCELLING "+b);
    while (agenda.size() > 0) {
      final Behaviour popped = popBehaviour(null);
      if (popped == b) break;
    }
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


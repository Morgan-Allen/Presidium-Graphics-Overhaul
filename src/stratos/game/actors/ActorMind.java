/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.util.*;
import stratos.game.economic.Property;



//  TODO:  Break down the report functions with some finer granularity.  You
//         may only want to inspect certain facets of the decision-process.

public abstract class ActorMind implements Qualities {
  
  
  /**  Field definitions, constructor, save/load methods-
    */
  private static boolean
    decisionVerbose = false,
    stepsVerbose    = false,
    warnVerbose     = true ;
  
  
  final protected Actor actor;
  protected Actor master;
  
  final List <Behaviour> agenda   = new List <Behaviour> ();
  final List <Behaviour> todoList = new List <Behaviour> ();
  
  protected Mission mission;
  protected Property home, work;
  protected Background vocation;
  
  
  
  public ActorMind(Actor actor) {
    this.actor = actor;
  }
  
  
  public void loadState(Session s) throws Exception {
    master = (Actor) s.loadObject();
    
    s.loadObjects(agenda  );
    s.loadObjects(todoList);
    
    mission  = (Mission   ) s.loadObject();
    home     = (Property  ) s.loadObject();
    work     = (Property  ) s.loadObject();
    vocation = (Background) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(master);
    
    s.saveObjects(agenda  );
    s.saveObjects(todoList);
    
    s.saveObject(mission );
    s.saveObject(home    );
    s.saveObject(work    );
    s.saveObject(vocation);
  }
  
  
  public void onWorldExit() {
    //  TODO:  DETACH ALL MEMORIES TO AVOID INDEFINITE REFERENCE-CHAINS FROM
    //         DEAD OBJECTS!
  }
  
  
  
  /**  Calling regular, periodic updates and triggering AI refreshments-
    */
  public void updateAI(int numUpdates) {
    
    if (numUpdates % 10 != 0) return;
    final boolean report = I.talkAbout == actor && decisionVerbose;
    
    if (report) {
      I.say("\nHome is: "+home);
      if (home != null) I.say("  Intact? "+(! home.destroyed()));
      I.say("\nWork is: "+work);
      if (work != null) I.say("  Intact? "+(! work.destroyed()));
    }
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
    if (next != last) {
      if (next != null) assignBehaviour(next);
      else cancelBehaviour(last, "NO NEXT BEHAVIOUR FOUND");
    }
  }
  
  
  public Behaviour nextBehaviour() {
    final boolean report = I.talkAbout == actor && decisionVerbose;
    if (report) I.say("\n\nACTOR IS GETTING NEXT BEHAVIOUR...");
    Behaviour taken = null, onMission = null;
    
    if (report) I.say("\nGetting newly created behaviour:");
    final Choice fromNew = createNewBehaviours(new Choice(actor));
    final Behaviour newChoice = fromNew.weightedPick();
    taken = Choice.switchFor(actor, newChoice, taken, true, report);
    if (report && fromNew.empty()) I.say("  No new behaviour.");
    
    if (report) I.say("\nGetting next from to-do list:");
    final Choice fromTodo = new Choice(actor, todoList);
    final Behaviour notDone = fromTodo.pickMostUrgent();
    taken = Choice.switchFor(actor, notDone, taken, true, report);
    if (report && fromTodo.empty()) I.say("  Nothing on todo list.");
    
    if (report) I.say("\nGetting current behaviour:");
    final Behaviour current = rootBehaviour();
    taken = Choice.switchFor(actor, current, taken, true, report);
    if (report && current == null) I.say("  No current behaviour.");
    
    if (report) I.say("\nGetting next step for mission: "+mission);
    if (mission != null && mission.hasBegun() && mission.isApproved(actor)) {
      onMission = mission.nextStepFor(actor, true);
    }
    taken = Choice.switchFor(actor, onMission, taken, true, report);
    if (report && onMission == null) I.say("  No mission behaviour.");
    
    if (report) {
      I.say("\nNext plans acquired...");
      I.say("  Last plan: "+rootBehaviour());
      final float
        notP = notDone   == null ? -1 : notDone  .priorityFor(actor),
        newP = newChoice == null ? -1 : newChoice.priorityFor(actor),
        curP = current   == null ? -1 : current  .priorityFor(actor),
        onMP = onMission == null ? -1 : onMission.priorityFor(actor);
      I.say("  New choice: "+newChoice+"  (priority "+newP+")");
      I.say("  From Todo:  "+notDone  +"  (priority "+notP+")");
      I.say("  Current:    "+current  +"  (priority "+curP+")");
      I.say("  On Mission: "+onMission+"  (priority "+onMP+")");
      I.say("  TAKEN: "+taken+"\n");
    }
    return taken;
  }
  
  
  public Action getNextAction() {
    final boolean report = I.talkAbout == actor && stepsVerbose;
    Behaviour root = null, next = null;
    final int MAX_LOOP = 20;
    final String cause = "Getting next action";
    
    for (int loop = MAX_LOOP; loop-- > 0;) {
      //
      //  Firstly, check to ensure that our root behaviour is still valid- if
      //  not, you'll need to pick out a new one:
      root = rootBehaviour();
      next = null;
      if (report) {
        I.say("\nGETTING NEXT ACTION FOR "+actor);
        I.say("  Current root behaviour: "+I.tagHash(root));
      }
      if (! Plan.canFollow(actor, root, true)) {
        if (Plan.canPersist(root)) todoList.add(root);
        root = nextBehaviour();
        if (report) {
          I.say("  Current agenda was empty!");
          I.say("  New root behaviour: "+I.tagHash(root));
        }
      }
      //
      //  Then, delete all existing entries from the agenda.
      for (Behaviour b : agenda) popBehaviour(b, cause);
      if (! Plan.canFollow(actor, root, true)) {
        if (warnVerbose) I.say(actor+"  CANNOT FOLLOW PLAN: "+root);
        return null;
      }
      //
      //  Then descend from the root node, adding each sub-step to the agenda,
      //  and return once you hit a valid action-step.  If that never happens,
      //  cancel from the root and start over.
      next = root;
      while (loop-- > 0) {
        pushBehaviour(next, cause);
        next = next.nextStepFor(actor);
        final boolean valid = Plan.canFollow(actor, next, true);
        if (report) {
          I.say("  Next step: "+next+", valid? "+valid);
          if (! valid) Plan.reportPlanDetails(next, actor);
        }
        if (! valid) break;
        else if (next instanceof Action) return (Action) next;
      }
      cancelBehaviour(root, cause);
    }
    if (warnVerbose) {
      //
      //  If you exhaust the maximum number of iterations (which I assume
      //  *would* be enough for any reasonable use-case,) report the problem.
      I.say("\n"+actor+" COULD NOT DECIDE ON NEXT STEP.");
      I.say("  Root behaviour: "+root);
      I.say("  Next step:      "+next);
      Plan.reportPlanDetails(next, actor);
      I.reportStackTrace();
    }
    return null;
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
  
  
  
  /**  Setting home and work venues & applications, plus missions-
    */
  public void assignMaster(Actor master) {
    this.master = master;
  }
  
  
  public Actor master() {
    return master;
  }
  
  
  public void assignMission(Mission mission) {
    final Mission oldMission = this.mission;
    if (mission == oldMission) return;
    this.mission = mission;
    
    if (oldMission != null) {
      final Behaviour oldStep = oldMission.nextStepFor(actor, false);
      cancelBehaviour(oldStep, "Assigned new mission");
      oldMission.setApplicant(actor, false);
    }
    if (mission != null) {
      mission.setApplicant(actor, true);
    }
  }
  
  
  public Mission mission() {
    return mission;
  }
  
  
  public void setWork(Property e) {
    if (work == e) return;
    if (work != null) work.staff().setWorker(actor, false);
    work = e;
    if (work != null) work.staff().setWorker(actor, true );
  }
  
  
  public void setHome(Property h) {
    if (home == h) return;
    if (home != null) home.staff().setLodger(actor, false);
    home = h;
    if (home != null) home.staff().setLodger(actor, true );
  }
  
  
  public Property work() {
    return work;
  }
  
  
  public Property home() {
    return home;
  }
  
  
  public void setVocation(Background vocation) {
    this.vocation = vocation;
  }
  
  
  public Background vocation() {
    return vocation;
  }
  
  
  
  /**  Methods related to maintaining the agenda stack-
    */
  private void pushBehaviour(Behaviour b, String cause) {
    final boolean report = I.talkAbout == actor && stepsVerbose;
    
    if (todoList.includes(b)) todoList.remove(b);
    agenda.addFirst(b);
    if (report) {
      I.say("\nPUSHING BEHAVIOUR: "+b);
      I.say("  Cause:          "+cause);
    }
    b.toggleActive(true);
  }
  
  
  private Behaviour popBehaviour(Behaviour toPop, String cause) {
    final boolean report = I.talkAbout == actor && stepsVerbose;
    
    if (toPop != null && agenda.first() != toPop) {
      return toPop;
    }
    final Behaviour b = agenda.removeFirst();
    if (report) {
      I.say("\nPOPPING BEHAVIOUR: "+b);
      I.say("  Cause:          "+cause);
      I.say("  Finished/valid: "+b.finished()+"/"+b.valid());
      I.say("  Priority        "+b.priorityFor(actor));
    }
    b.toggleActive(false);
    return b;
  }
  
  
  public void assignToDo(Behaviour toDo) {
    if (wouldSwitchTo(toDo)) assignBehaviour(toDo);
    else todoList.include(toDo);
  }
  
  
  public void assignBehaviour(Behaviour behaviour) {
    if (behaviour == null) I.complain("CANNOT ASSIGN NULL BEHAVIOUR.");
    final boolean report = I.talkAbout == actor && decisionVerbose;

    if (report) {
      I.say("\nASSIGNING BEHAVIOUR "+behaviour);
      if (warnVerbose) I.reportStackTrace();
    }
    actor.assignAction(null);
    
    final Behaviour replaced = rootBehaviour();
    cancelBehaviour(replaced, "Assigned new behaviour");
    
    if (Plan.canPersist(replaced)) {
      if (report) I.say("  SAVING PLAN AS TODO: "+replaced);
      todoList.include(replaced);
    }
    else if (report) {
      I.say("  Old plan discarded: "+replaced);
      Plan.reportPlanDetails(replaced, actor);
    }
    
    behaviour.priorityFor(actor);
    behaviour.nextStepFor(actor);
    pushBehaviour(behaviour, "Assigned new behaviour");
  }
  
  
  public void pushFromParent(Behaviour b, Behaviour parent) {
    if (! agenda.includes(parent)) {
      //I.complain("Behaviour not active.");
      return;
    }
    
    final String cause = "Pushing behaviour from parent";
    cancelBehaviour(parent, cause);
    pushBehaviour(parent, cause);
    pushBehaviour(b, cause);
    actor.assignAction(null);
  }
  
  
  public void cancelBehaviour(Behaviour b, String cause) {
    final boolean report = I.talkAbout == actor && stepsVerbose;
    if (! agenda.includes(b)) return;
    if (report) {
      I.say("\nCANCELLING "+b+", CAUSE: "+cause);
      if (warnVerbose) I.reportStackTrace();
    }
    while (agenda.size() > 0) {
      final Behaviour popped = popBehaviour(null, cause);
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
    if (rootBehaviour() != null) {
      cancelBehaviour(rootBehaviour(), "Clearing agenda");
    }
    todoList.clear();
  }
  
  
  public Series <Behaviour> agenda() {
    return agenda;
  }
  
  
  public Series <Behaviour> todoList() {
    return todoList;
  }
  
  
  public Behaviour topBehaviour() {
    return agenda.first();
  }
  
  
  public Behaviour rootBehaviour() {
    return agenda.last();
  }
  
  
  public float urgency() {
    Behaviour b = rootBehaviour();
    if (b == null) return 0;
    return b.priorityFor(actor) / Plan.PARAMOUNT;
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


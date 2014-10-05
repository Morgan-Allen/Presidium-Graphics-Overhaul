/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */



package stratos.game.actors;

import stratos.game.building.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.game.tactical.*;
import stratos.user.*;
import stratos.util.*;

import org.apache.commons.math3.util.FastMath;



public abstract class ActorMind implements Qualities {
  
  
  /**  Field definitions, constructor, save/load methods-
    */
  private static boolean
    decisionVerbose = Choice.verbose,
    stepsVerbose    = false;
  
  
  final protected Actor actor;
  
  final List <Behaviour> agenda = new List <Behaviour> ();
  final List <Behaviour> todoList = new List <Behaviour> ();
  
  protected Mission mission;
  protected Employer home, work;
  protected Application application;
  protected Actor master;
  
  
  
  protected ActorMind(Actor actor) {
    this.actor = actor;
  }
  
  
  protected void loadState(Session s) throws Exception {
    s.loadObjects(agenda);
    s.loadObjects(todoList);
    
    mission = (Mission) s.loadObject();
    home = (Employer) s.loadObject();
    work = (Employer) s.loadObject();
    application = (Application) s.loadObject();
    master = (Actor) s.loadObject();
  }
  
  
  protected void saveState(Session s) throws Exception {
    s.saveObjects(agenda);
    s.saveObjects(todoList);
    
    s.saveObject(mission);
    s.saveObject(home);
    s.saveObject(work);
    s.saveObject(application);
    s.saveObject(master);
  }
  
  
  protected void onWorldExit() {
  }
  
  
  
  /**  Calling regular, periodic updates and triggering AI refreshments-
    */
  protected void updateAI(int numUpdates) {
    if (numUpdates % 10 != 0) return;
    //
    //  Remove any expired behaviour-sources:
    if (home != null && home.destroyed()) {
      setHome(null);
    }
    if (work != null && work.destroyed()) {
      setWork(null);
    }
    if (application != null && ! application.valid()) {
      switchApplication(null);
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
    
    if (decisionVerbose && I.talkAbout == actor) {
      I.say("\nPerformed periodic AI update.");
      final float
        lastP = last == null ? -1 : last.priorityFor(actor),
        nextP = next == null ? -1 : next.priorityFor(actor);
      I.say("  LAST PLAN: "+last+" "+lastP);
      I.say("  NEXT PLAN: "+next+" "+nextP);
      I.say("\n");
    }
    if (Choice.wouldSwitch(actor, last, next, true)) assignBehaviour(next);
  }
  
  
  public Behaviour nextBehaviour() {
    final boolean report = decisionVerbose && I.talkAbout == actor;
    
    if (report) I.say("\nGetting next from to-do list:");
    final Choice fromTodo = new Choice(actor, todoList);
    final Behaviour notDone = fromTodo.pickMostUrgent();
    
    if (report) I.say("\nGetting newly created behaviour:");
    final Choice fromNew = createNewBehaviours(new Choice(actor));
    final Behaviour newChoice = fromNew.weightedPick();
    
    if (report) I.say("\nChecking for switch from undone to new choice:");
    final Behaviour taken =
      Choice.wouldSwitch(actor, notDone, newChoice, false) ?
      newChoice : notDone;
    
    if (report) {
      I.say("\nPLANS ACQUIRED:");
      I.say("  LAST PLAN: "+rootBehaviour());
      final float
        notP = notDone == null ? -1 : notDone.priorityFor(actor),
        newP = newChoice == null ? -1 : newChoice.priorityFor(actor);
      I.say("  NOT DONE: "+notDone+", PRIORITY: "+notP);
      I.say("  NEW CHOICE: "+newChoice+", PRIORITY: "+newP);
      I.say("  CURRENT FAVOURITE: "+taken);
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
  
  
  protected Action getNextAction() {
    final boolean
      reportStep = stepsVerbose && I.talkAbout == actor,
      reportNew = reportStep || (decisionVerbose && I.talkAbout == actor);
    
    
    final int MAX_LOOP = 20;  // Safety feature, see below...
    for (int loop = MAX_LOOP; loop-- > 0;) {
      if (reportStep) I.say("\n"+actor+" in action-decision loop:");
      //
      //  If all current behaviours are complete, generate a new one.
      if (agenda.size() == 0) {
        final Behaviour taken = nextBehaviour();
        if (reportNew) {
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
      final Behaviour current = topBehaviour();
      final Behaviour next = current.nextStepFor(actor);
      final boolean isDone = current.finished();
      if (reportStep) {
        I.say("  Current action "+current);
        I.say("  Class type: "+current.getClass().getSimpleName());
        I.say("  Next step "+next);
        I.say("  Done "+isDone);
      }
      if (isDone || next == null) {
        if (current == rootBehaviour() && ! isDone && current.persistent()) {
          todoList.add(current);
        }
        popBehaviour();
      }
      else if (current instanceof Action) {
        if (reportStep) {
          I.say("Next action: "+current);
          I.say("Agenda size: "+agenda.size());
        }
        return (Action) current;
      }
      else {
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
    I.say("  Next step: " + next);
    if (next != null) {
      I.say("  Valid/finished " + next.valid() + "/" + next.finished());
    }
    new Exception().printStackTrace();
    return null;
  }
  
  
  
  /**  Setting home and work venues & applications, plus missions-
    */
  public void switchApplication(Application a) {
    if (this.application == a) return;
    if (application != null) {
      application.employer.personnel().setApplicant(application, false);
    }
    application = a;
    if (application != null) {
      application.employer.personnel().setApplicant(application, true);
    }
  }
  
  
  public Application application() {
    return application;
  }
  
  
  public void setWork(Employer e) {
    if (work == e) return;
    if (work != null) work.personnel().setWorker(actor, false);
    work = e;
    if (work != null) work.personnel().setWorker(actor, true);
  }
  
  
  public Employer work() {
    return work;
  }
  
  
  public void setHome(Employer home) {
    final Employer old = this.home;
    if (old == home) return;
    if (old != null) old.personnel().setResident(actor, false);
    this.home = home;
    if (home != null) home.personnel().setResident(actor, true);
  }
  
  
  public Employer home() {
    return home;
  }
  
  
  public void assignMission(Mission mission) {
    if (this.mission == mission) {
      return;
    }
    if (this.mission != null) {
      cancelBehaviour(this.mission);
      this.mission.setApplicant(actor, false);
    }
    if (mission != null) {
      mission.setApplicant(actor, true);
    }
    this.mission = mission;
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
      I.say("PUSHING BEHAVIOUR: "+b);
    }
    actor.world().activities.toggleBehaviour(b, true);
  }
  
  
  private Behaviour popBehaviour() {
    final Behaviour b = agenda.removeFirst();
    if (stepsVerbose && I.talkAbout == actor) {
      I.say("POPPING BEHAVIOUR: "+b);
      I.say("  Finished/valid: "+b.finished()+"/"+b.valid());
      I.say("  Priority "+b.priorityFor(actor));
    }
    actor.world().activities.toggleBehaviour(b, false);
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
    pushBehaviour(behaviour);
    
    if (replaced != null && ! replaced.finished() && replaced.persistent()) {
      if (report) I.say(" SAVING PLAN AS TODO: "+replaced);
      todoList.include(replaced);
    }
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
      ///new Exception().printStackTrace();
    }
    
    if (agenda.includes(b)) while (agenda.size() > 0) {
      final Behaviour popped = popBehaviour();
      if (popped == b) break;
    }
    if (agenda.includes(b)) I.complain("Duplicate behaviour!");
    todoList.remove(b);
    actor.assignAction(null);
  }
  
  
  public boolean wouldSwitchTo(Behaviour next) {
    if (! actor.health.conscious()) return false;
    return Choice.wouldSwitch(actor, rootBehaviour(), next, true);
  }
  
  
  public boolean mustIgnore(Behaviour next) {
    if (! actor.health.conscious()) return true;
    return Choice.wouldSwitch(actor, next, rootBehaviour(), false);
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
    for (Behaviour b : agenda) if (b.getClass() == planClass) return true;
    for (Behaviour b : todoList) if (b.getClass() == planClass) return true;
    return false;
  }
  
  
  public boolean doing(Behaviour b) {
    //  I do a little bit of speed optimisation here, rather than using an
    //  iterator...
    for (ListEntry <Behaviour> bE = agenda; (bE = bE.nextEntry()) != agenda;) {
      if (bE.refers == b) return true;
    }
    return false;
  }
  
  
  /**  Supplementary methods for relationships and attitudes-
    */
  //  TODO:  MOVE TO THE RELATION CLASS
  
  public float attraction(Actor other) {
    if (this.actor.species() != Species.HUMAN) return 0;
    if (other.species() != Species.HUMAN) return 0;
    //
    //  TODO:  Create exceptions based on age and kinship modifiers.
    //
    //  First, we establish a few facts about each actor's sexual identity:
    float actorG = 0, otherG = 0;
    if (actor.traits.hasTrait(GENDER, "Male"  )) actorG = -1;
    if (actor.traits.hasTrait(GENDER, "Female")) actorG =  1;
    if (other.traits.hasTrait(GENDER, "Male"  )) otherG = -1;
    if (other.traits.hasTrait(GENDER, "Female")) otherG =  1;
    float attraction = other.traits.traitLevel(HANDSOME) * 3.33f;
    attraction += otherG * other.traits.traitLevel(FEMININE) * 3.33f;
    attraction *= (actor.traits.relativeLevel(INDULGENT) + 1f) / 2;
    //
    //  Then compute attraction based on orientation-
    final String descO = actor.traits.levelDesc(ORIENTATION);
    float matchO = 0;
    if (descO.equals("Heterosexual")) {
      matchO = (actorG * otherG < 0) ? 1 : 0.33f;
    }
    else if (descO.equals("Bisexual")) {
      matchO = 0.66f;
    }
    else if (descO.equals("Homosexual")) {
      matchO = (actorG * otherG > 0) ? 1 : 0.33f;
    }
    return attraction * matchO / 10f;
  }
  
  
  public String preferredGender() {
    final boolean male = actor.traits.male();
    if (actor.traits.hasTrait(ORIENTATION, "Heterosexual")) {
      return male ? "Female" : "Male";
    }
    if (actor.traits.hasTrait(ORIENTATION, "Homosexual")) {
      return male ? "Male" : "Female";
    }
    return Rand.yes() ? "Male" : "Female";
  }
}




/*
//
//  TODO:  CONSIDER GETTING RID OF THIS CLAUSE?  It's handy in certain
//  situations, (e.g, where completing the current plan would come at 'no
//  cost' to the next plan,) but may be more trouble than it's worth.
final Target NT = targetFor(next);
if (NT != null && targetFor(last) == NT && NT != actor.aboard()) {
  return false;
}
//*/

/*
private Target targetFor(Behaviour b) {
  final Behaviour n = b.nextStepFor(actor);
  if (n instanceof Action) return ((Action) n).subject();
  else if (n == null || n.finished()) return null;
  else return targetFor(n);
}
//*/





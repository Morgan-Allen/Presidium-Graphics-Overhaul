/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.user.*;
import stratos.util.*;



//  TODO:  Some of the reasoning here is a little opaque.  Rewrite this to
//  have a single 'update' method, then read off everything else passively.



public abstract class Plan implements Session.Saveable, Behaviour {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  private static boolean
    stepsVerbose    = false,
    priorityVerbose = false,
    extraVerbose    = false,
    beginsVerbose   = false;
  
  private static Class
    verboseClass = null;
  
  
  final public static int
    NO_PROPERTIES    = 0 ,
    MOTIVE_LEISURE   = 1 ,
    MOTIVE_JOB       = 2 ,
    MOTIVE_PERSONAL  = 4 ,
    MOTIVE_EMERGENCY = 8 ,
    MOTIVE_MISSION   = 16,
    
    IS_CANCELLED = 32 ,
    HAS_PRIORITY = 64 ,
    HAS_STEPS    = 128;
  final static String MOTIVE_NAMES[] = {
    "Leisure", "Job", "Personal", "Emergency", "Mission", "Cancelled"
  };
  
  final public static float
    
    NO_FAIL_RISK      = 0.0f,
    MILD_FAIL_RISK    = 0.5f,
    REAL_FAIL_RISK    = 1.0f,
    EXTREME_FAIL_RISK = 2.0f,
    
    NO_COMPETITION   =  0.0f,
    MILD_COMPETITION =  0.5f,
    FULL_COMPETITION =  1.0f,
    MILD_COOPERATION = -0.5f,
    FULL_COOPERATION = -1.0f,
    
    NO_HARM      =  0.0f,
    MILD_HARM    =  0.5f,
    REAL_HARM    =  1.0f,
    EXTREME_HARM =  1.5f,
    MILD_HELP    = -0.5f,
    REAL_HELP    = -1.0f,
    EXTREME_HELP = -1.5f,
    
    NO_DISTANCE_CHECK      = 0.0f,
    PARTIAL_DISTANCE_CHECK = 0.5f,
    NORMAL_DISTANCE_CHECK  = 1.0f,
    HEAVY_DISTANCE_CHECK   = 2.0f,
    
    NO_MODIFIER = 0,
    
    PRIORITY_NEVER = 0 - PARAMOUNT;
  final protected static Skill NO_SKILLS[] = null;
  final protected static Trait NO_TRAITS[] = null;
  
  final static float
    NULL_PRIORITY = -100;
  
  
  final public Target subject;
  protected Actor actor;
  
  protected float
    lastEvalTime = -1,
    priorityEval = NULL_PRIORITY;
  
  protected Plan parent;
  protected Behaviour nextStep = null;
  private int
    properties  = 0;
  private float
    motiveBonus = 0,
    harmFactor  = 0,
    competence  = 0;
  
  
  
  protected Plan(
    Actor actor, Target subject, int motiveType, float harmFactor
  ) {
    if (subject == null) I.complain("NULL PLAN SUBJECT");
    this.actor      = actor     ;
    this.subject    = subject   ;
    this.properties = motiveType;
    this.harmFactor = harmFactor;
  }
  
  
  public Plan(Session s) throws Exception {
    s.cacheInstance(this);
    this.actor   = (Actor ) s.loadObject();
    this.subject = (Target) s.loadObject();
    
    this.lastEvalTime = s.loadFloat();
    this.priorityEval = s.loadFloat();
    
    this.parent   = (Plan     ) s.loadObject();
    this.nextStep = (Behaviour) s.loadObject();
    
    this.properties  = s.loadInt  ();
    this.motiveBonus = s.loadFloat();
    this.harmFactor  = s.loadFloat();
    this.competence  = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(actor  );
    s.saveObject(subject);
    
    s.saveFloat (lastEvalTime);
    s.saveFloat (priorityEval);
    s.saveObject(parent  );
    s.saveObject(nextStep);
    
    s.saveInt  (properties );
    s.saveFloat(motiveBonus);
    s.saveFloat(harmFactor );
    s.saveFloat(competence );
  }
  
  
  public boolean matchesPlan(Behaviour p) {
    if (p == null || p.getClass() != this.getClass()) return false;
    return this.subject == ((Plan) p).subject;
  }
  
  
  public Target subject() {
    return subject;
  }
  
  
  
  /**  Default implementations of Behaviour methods-
    */
  public int motionType(Actor actor) {
    return MOTION_ANY;
  }
  
  
  public boolean valid() {
    if (! subject.inWorld()) return false;
    if (actor != null && ! actor.inWorld()) return false;
    return true;
  }
  
  
  public boolean persistent() {
    return hasMotives(MOTIVE_JOB) || hasMotives(MOTIVE_PERSONAL);
  }
  
  
  public boolean isJob() {
    return hasMotives(MOTIVE_JOB);
  }
  
  
  public boolean isEmergency() {
    return hasMotives(MOTIVE_EMERGENCY);
  }
  
  
  public void toggleActive(boolean is) {
    if (actor == null || ! actor.inWorld()) return;
    actor.world().activities.registerFocus(this, is);
  }
  
  
  public void interrupt(String cause) {
    if (! hasBegun()) return;
    final boolean report =
      I.talkAbout == actor && (stepsVerbose || priorityVerbose)
    ;
    if (report) {
      I.say("\n"+actor+" Aborting plan! "+I.tagHash(this));
      I.say("  Cause: "+cause);
      I.reportStackTrace();
    }
    nextStep = null;
    priorityEval = NULL_PRIORITY;
    actor.mind.cancelBehaviour(this, cause);
    addMotives(IS_CANCELLED, 0);
  }
  
  
  public void updatePlanFor(Actor actor) {
    final boolean report = I.talkAbout == actor && (
      verboseClass == null || verboseClass == this.getClass()
    ) && (beginsVerbose || hasBegun()) && (priorityVerbose || stepsVerbose);
    
    attemptToBind(actor);

    if (hasMotives(IS_CANCELLED)) {
      priorityEval = NULL_PRIORITY;
      nextStep     = null;
      return;
    }

    if (! Plan.canFollow(actor, nextStep, false)) nextStep = null;
    if (! checkRefreshDue(actor, report && extraVerbose)) return;
    final boolean asRoot = parentPlan() == null;
    final float time = actor.world().currentTime();
    
    if (report) {
      I.say("\nRefreshing plan: "+I.tagHash(this));
      I.say("  Old priority: "+priorityEval);
      I.say("  Last step:    "+nextStep    );
    }
    
    if (asRoot) {
      priorityEval = 0;  //  Note:  This avoids certain types of infinite loop.
      priorityEval = getPriority();
      if (report) I.say("\nNew priority: "+priorityEval);
      properties |= HAS_PRIORITY;
    }
    
    Behaviour lastStep = nextStep;
    Behaviour step = getNextStep();
    
    if (lastStep != null && lastStep.matchesPlan(step)) {
      if (report) I.say("    NEXT STEP THE SAME AS OLD STEP: "+step);
      nextStep = lastStep;
    }
    else if (step != null) {
      if (report) I.say("  New plan step: "+I.tagHash(nextStep));
      nextStep = step;
    }
    if (nextStep instanceof Plan) {
      ((Plan) nextStep).parent = this;
    }
    
    properties |= HAS_STEPS;
    lastEvalTime = time;
  }
  
  
  private boolean checkRefreshDue(Actor actor, boolean report) {
    if (report) {
      I.say("\nChecking if refreshment due for "+this);
      I.say("  Priority:  "+priorityEval);
      I.say("  Next step: "+nextStep);
    }
    //
    //  If the plan is no longer valid, we quit immediately.
    if (! valid()) {
      if (report) I.say("\nNEXT STEP IS NULL: NOT VALID");
      onceInvalid();
      this.interrupt(INTERRUPT_NOT_VALID);
      return false;
    }
    //
    //  If the last step has completed (or has exhausted it's set of steps), we
    //  need to generate another.
    boolean oldDone = nextStep != null && (
      nextStep.finished() ||
      nextStep.nextStep() == null
    );
    if (oldDone) {
      if (report) I.say("OLD STEP FINISHED: "+nextStep);
      return true;
    }
    //
    //  Only root (or unattached) behaviours need to evaluate their priority
    //  explicitly in order to be valid- sub-step behaviours only to need to
    //  refresh their own step-sequence.
    final boolean root = actor.mind.rootBehaviour() == this || ! hasBegun();
    if ((root && priorityEval == NULL_PRIORITY) || nextStep == null) {
      if (report) I.say("ALREADY FLAGGED FOR EVALUATION!");
      return true;
    }
    //
    //  Finally, we always force a fresh evaluation after enough time has
    //  elapsed...
    final float
      timeGone = actor.world().currentTime() - lastEvalTime,
      interval = evaluationInterval();
    if (report) {
      I.say("\nChecking for fresh evaluation: "+this);
      I.say("  Time gone: "+timeGone+"/"+interval);
    }
    if (timeGone >= interval) {
      if (report) I.say("WILL REFRESH!");
      return true;
    }
    else {
      if (report) I.say("Refresh not yet due.");
      return false;
    }
  }
  
  
  protected void attemptToBind(Actor actor) {
    final Actor oldActor = this.actor;
    if (oldActor != null && oldActor != actor) I.complain(
      "WRONG ACTOR INVOKING PLAN: "+this+", IS "+actor+", SHOULD BE "+oldActor
    );
    else this.actor = actor;
  }
  
  
  protected int evaluationInterval() {
    return actor.senses.isEmergency() ? 1 : 10;
  }
  
  
  protected Action action() {
    if (nextStep instanceof Action) return (Action) nextStep;
    else return null;
  }
  
  
  public Plan parentPlan() {
    return parent;
  }
  
  
  public float priority() {
    return priorityEval;
  }
  
  
  public Behaviour nextStep() {
    return nextStep;
  }
  
  
  public boolean finished() {
    final boolean report = I.talkAbout == actor &&
      (stepsVerbose || priorityVerbose) && hasBegun();
    
    if (hasMotives(IS_CANCELLED)) return true;
    if (actor == null) return false;
    
    updatePlanFor(actor);
    
    if (parentPlan() == null && priority() <= 0) {
      if (report) I.say("\nNO PRIORITY: "+I.tagHash(this));
      return true;
    }
    if (nextStep() == null) {
      if (report) I.say("\nNO NEXT STEP: "+I.tagHash(this));
      return true;
    }
    return false;
  }
  
  
  public boolean hasBegun() {
    return actor != null && ((properties & HAS_STEPS) == HAS_STEPS);
  }
  
  
  public boolean isActive() {
    return actor != null && actor.isDoing(this, true);
  }
  
  
  public Actor actor() {
    return actor;
  }
  
  
  
  /**  Assorted utility evaluation methods-
    */
  //
  //  NOTE:  Motive properties can be OR'd together to signify that you have
  //  more than one.  It is not recommended that you modify their bonus after
  //  a plan has started, however (or you might get indefinite-increments over
  //  time.)
  
  public Plan addMotives(int props, float bonus) {
    if (hasBegun() && bonus != 0) I.complain(
      "\nWARNING:  Should not alter motive bonus once a plan begins! "+this
    );
    this.properties   |= props;
    this.motiveBonus  += bonus;
    this.lastEvalTime = -1    ;
    return this;
  }
  
  
  public Plan toggleMotives(int props, boolean has) {
    if (has) properties |= props;
    else properties &= ~ props;
    this.lastEvalTime = -1    ;
    return this;
  }
  
  
  public Plan setMotivesFrom(Plan parent, float bonus) {
    this.properties  = parent.properties;
    this.motiveBonus = parent.motiveBonus + bonus;
    this.lastEvalTime = -1;
    return this;
  }
  
  
  public void clearMotives() {
    this.properties   = NO_PROPERTIES;
    this.motiveBonus  = 0;
    this.priorityEval = NULL_PRIORITY;
    this.lastEvalTime = -1;
    this.nextStep = null;
  }
  
  
  public boolean hasMotives(int props) {
    return (this.properties & props) == props;
  }
  
  
  public float motiveBonus() {
    return this.motiveBonus;
  }
  
  
  //  TODO:  This also needs to be automated.
  protected void setCompetence(float c) {
    this.competence = c;
  }
  
  
  protected void setHarmFactor(float harmFactor) {
    this.harmFactor = harmFactor;
  }
  
  
  public float harmFactor() { return harmFactor; }
  public float competence() { return competence; }
  
  public int motiveProperties() { return properties; }
  
  protected void onceInvalid() {}
  
  protected abstract Behaviour getNextStep();
  protected abstract float getPriority();
  public abstract Plan copyFor(Actor other);
  
  
  
  /**  Various utility methods, such as for application to generic Behaviours-
    */
  public static float harmIntended(Actor actor, Target toward) {
    if (toward == null) return 0;
    final Behaviour root = actor.mind.rootBehaviour();
    if (! (root instanceof Plan)) return 0;
    return ((Plan) root).harmIntended(toward);
  }
  
  
  public float harmIntended(Target t) {
    if (t == subject || t == actionFocus()) return harmFactor;
    return 0;
  }
  
  
  public static boolean canFollow(Actor a, Behaviour b, boolean checkPriority) {
    if (b == null || ! b.valid()) return false;
    b.updatePlanFor(a);
    
    if (b.finished() || b.nextStep() == null) {
      return false;
    }
    if (checkPriority && b.priority() <= 0) {
      return false;
    }
    return true;
  }
  
  
  public static boolean canPersist(Behaviour b) {
    if (b == null || ! b.valid()) return false;
    if (b.finished() || ! b.persistent()) return false;
    return true;
  }
  
  
  public static boolean hasMotive(Behaviour b, int motives) {
    if (! (b instanceof Plan)) return false;
    return ((Plan) b).hasMotives(motives);
  }
  
  
  public Target stepFocus() {
    return nextStep == null ? null : nextStep.subject();
  }
  
  
  public Target actionFocus() {
    for (Behaviour step = nextStep; step != null;) {
      if (step instanceof Action) return step.subject();
      else step = ((Plan) step).nextStep;
    }
    return null;
  }
  
  
  protected boolean lastStepIs(String methodName) {
    if (! (nextStep instanceof Action)) return false;
    return ((Action) nextStep).methodName().equals(methodName);
  }
  
  
  protected Target lastStepTarget() {
    if (nextStep == null) return null;
    if (nextStep instanceof Action) return ((Action) nextStep).subject();
    if (nextStep instanceof Plan  ) return ((Plan  ) nextStep).lastStepTarget();
    return null;
  }
  
  
  
  /**  Rendering, interface and debug methods-
    */
  public String toString() {
    final StringDescription desc = new StringDescription();
    describeBehaviour(desc);
    return desc.toString();
  }
  
  
  protected boolean needsSuffix(Description d, String defaultPrefix) {
    if (nextStep == null) {
      d.append(defaultPrefix);
      return true;
    }
    if (nextStep instanceof Action) {
      nextStep.describeBehaviour(d);
      return true;
    }
    else {
      nextStep.describeBehaviour(d);
      return false;
    }
  }
  
  
  public static String priorityDescription(float priority) {
    final int maxIndex = PRIORITY_DESCRIPTIONS.length;
    final float index = (priority / PARAMOUNT) * (maxIndex - 1);
    
    String desc = "";
    if (priority > 0) {
      desc+=I.shorten(priority, 1);
      desc+=": "+PRIORITY_DESCRIPTIONS[Nums.clamp((int) index, maxIndex)];
    }
    else desc+="No Priority";
    return desc;
  }
  
  
  public static void reportPlanDetails(Behaviour b, Actor a) {
    if (b == null) { I.say("  IS NULL"); return; }
    
    I.say("    Plan class:    "+b.getClass().getSimpleName());
    I.say("    Priority:      "+b.priority());
    I.say("    Valid:         "+b.valid());
    I.say("    Finished:      "+b.finished());
    I.say("    Next step:     "+b.nextStep());
    I.say("    Persists?      "+b.persistent());
    I.say("    Target is:     "+b.subject());
    I.say("    Target exists? "+b.subject().inWorld());
    I.say("    Target intact? "+(! b.subject().destroyed()));
    
    if (b instanceof Plan) {
      final Plan p = (Plan) b;
      I.say("    Plan properties: "+p.properties);
      for (int i = 0; i < MOTIVE_NAMES.length; i++) {
        final int prop = 1 << i;
        if (p.hasMotives(prop)) I.say("    "+MOTIVE_NAMES[i]+" ("+prop+")");
      }
    }
  }
}




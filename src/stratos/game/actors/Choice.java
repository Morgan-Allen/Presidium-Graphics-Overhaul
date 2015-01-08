/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors;
import stratos.util.*;



public class Choice implements Qualities {
  
  
  /**  Data fields, constructors and setup-
    */
  public static boolean
    verbose       = false,
    mindVerbose   = false,
    verboseReject = false,
    verboseSwitch = false;
  
  final Actor actor;
  final Batch <Behaviour> plans = new Batch <Behaviour> ();
  public boolean isVerbose = false;
  
  
  public Choice(Actor actor) {
    this.actor = actor;
  }
  
  
  public Choice(Actor actor, Series <Behaviour> plans) {
    this.actor = actor;
    for (Behaviour p : plans) add(p);
  }
  
  
  public boolean add(Behaviour plan) {
    if (plan == null) return false;
    if (! checkPlanValid(plan)) return false;
    plans.add(plan);
    return true;
  }
  
  
  protected boolean checkPlanValid(Behaviour plan) {
    final boolean report = isVerbose || (verboseReject && I.talkAbout == actor);
    
    final boolean valid = plan.valid();
    if (! valid) {
      if (report) I.say("\n  "+plan+" rejected- no longer valid.");
      return false;
    }
    final float priority = plan.priorityFor(actor);
    if (priority <= 0) {
      if (report) I.say("\n  "+plan+" rejected- priority "+priority);
      return false;
    }
    final Behaviour nextStep = plan.nextStepFor(actor);
    if (nextStep == null) {
      if (report) I.say("\n  "+plan+" rejected- no next step.");
      return false;
    }
    final boolean finished = plan.finished();
    if (finished) {
      if (report) {
        I.say("\n  "+plan+" rejected: is finished.");
        I.say("    Priority:  "+priority);
        I.say("    Next step: "+nextStep);
      }
      return false;
    }
    return true;
  }
  
  
  public int size() {
    return plans.size();
  }
  
  
  public boolean empty() {
    return plans.size() == 0;
  }
  
  
  
  /**  Picks a plan from those assigned earlier using priorities to weight the
    *  likelihood of their selection.
    */
  public Behaviour pickMostUrgent() {
    return weightedPick(false);
  }
  
  
  public Behaviour weightedPick() {
    return weightedPick(true);
  }
  
  
  private static float competeThreshold(
    Actor actor, float topPriority, boolean fromCurrent
  ) {
    final float stubborn = actor.traits.relativeLevel(STUBBORN) / 2f;
    
    float thresh = topPriority;
    if (fromCurrent) thresh -= 1 + stubborn;
    else thresh -= stubborn;
    
    if (topPriority > Plan.PARAMOUNT) {
      final float extra = (topPriority - Plan.PARAMOUNT) / Plan.PARAMOUNT;
      thresh -= Plan.DEFAULT_SWITCH_THRESHOLD * extra;
    }
    thresh -= Plan.DEFAULT_SWITCH_THRESHOLD;
    
    return thresh < 0 ? 0 : thresh;
  }
  
  
  private Behaviour weightedPick(boolean free) {
    final boolean report = (verbose && I.talkAbout == actor) || isVerbose;
    
    if (plans.size() == 0) {
      if (verboseReject && I.talkAbout == actor) I.say("  ...Empty choice!");
      return null;
    }
    if (report) {
      String label = actor.getClass().getSimpleName();
      if (actor.vocation() != null) label = actor.vocation().name;
      else if (actor.species() != null) label = actor.species().toString();
      I.say("\n"+actor+" ("+label+") is making a choice.");
      I.say("Range of choice is "+plans.size()+", free? "+free);
    }
    //
    //  Firstly, acquire the priorities for each plan.  If the permitted range
    //  of priorities is zero, simply return the most promising.
    float bestPriority = 0;
    Behaviour picked = null;
    final float weights[] = new float[plans.size()];
    int i = 0;
    for (Behaviour plan : plans) {
      final float priority = plan.priorityFor(actor);
      if (priority > bestPriority) { bestPriority = priority; picked = plan; }
      weights[i++] = priority;
      if (report) I.say("  "+plan+" has priority: "+priority);
    }
    if (! free) {
      if (report) I.say("Picked: "+picked);
      return picked;
    }
    //
    //  Eliminate all weights outside the permitted range, so that only plans
    //  of comparable attractiveness to the most important are considered-
    final float minPriority = competeThreshold(actor, bestPriority, false);
    if (report) {
      I.say("  Best priority: "+bestPriority);
      I.say("  Min. priority: "+minPriority);
    }
    float sumWeights = 0;
    for (i = weights.length; i-- > 0;) {
      weights[i] = Nums.max(0, weights[i] - minPriority);
      sumWeights += weights[i];
    }
    if (sumWeights == 0) {
      if (report) I.say("Picked: "+picked);
      return picked;
    }
    //
    //  Finally, select a candidate at random using weights based on priority-
    float randPick = Rand.num() * sumWeights;
    i = 0;
    for (Behaviour plan : plans) {
      final float chance = weights[i++];
      if (randPick < chance) { picked = plan; break; }
      else randPick -= chance;
    }
    if (report) I.say("Picked: "+picked);
    return picked;
  }
  
  
  public static boolean wouldSwitch(
    Actor actor, Behaviour last, Behaviour next, boolean stubborn,
    boolean report
  ) {
    if (report) I.say("\nConsidering switch from "+last+" to "+next);
    if (next == null) return false;
    if (last == null) return true ;
    
    final float
      lastPriority = last.priorityFor(actor),
      nextPriority = next.priorityFor(actor);
    if (report) {
      I.say("  Last priority: "+lastPriority);
      I.say("  Next priority: "+nextPriority);
    }
    
    if (nextPriority <= 0) return false;
    if (lastPriority <= 0) return true ;
    
    final float minPriority = stubborn ?
      competeThreshold(actor, nextPriority, true) :
      nextPriority;
    
    if (report) {
      I.say("  Min. priority for last is: "+minPriority);
      I.say("  Would switch from last to next? "+(lastPriority < minPriority));
    }
    return lastPriority < minPriority;
  }
}







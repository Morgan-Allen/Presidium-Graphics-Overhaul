/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.actors ;
import stratos.game.common.*;
import stratos.util.*;



public class Choice {
  
  
  /**  Data fields, constructors and setup-
    */
  final static float
    DEFAULT_PRIORITY_RANGE = 5.0f,
    DEFAULT_TRAIT_RANGE    = 2.0f ;
  
  public static boolean
    verbose       = false,
    verboseReject = verbose && false ;
  
  final Actor actor ;
  final Batch <Behaviour> plans = new Batch <Behaviour> () ;
  
  
  public Choice(Actor actor) {
    this.actor = actor ;
  }
  
  
  public Choice(Actor actor, Series <Behaviour> plans) {
    this.actor = actor ;
    for (Behaviour p : plans) add(p) ;
  }
  
  
  public boolean add(Behaviour plan) {
    if (plan == null) return false;
    if (! checkPlanValid(plan)) return false;
    plans.add(plan);
    return true;
  }
  
  
  protected boolean checkPlanValid(Behaviour plan) {
    final boolean finished = plan.finished();
    final float priority = plan.priorityFor(actor);
    final Behaviour nextStep = plan.nextStepFor(actor);

    if (finished || priority <= 0 || nextStep == null) {
      if (verboseReject && plan != null && I.talkAbout == actor) {
        I.say("  Rejected option: " + plan + " (" + plan.getClass() + ")");
        I.say("  Priority: " + priority);
        I.say("  Finished/valid: " + finished + "/" + plan.valid());
        I.say("  Next step: " + nextStep + "\n");
      }
      return false;
    }
    return true;
  }
  
  
  public int size() {
    return plans.size() ;
  }
  
  
  
  /**  Picks a plan from those assigned earlier using priorities to weight the
    *  likelihood of their selection.
    */
  public Behaviour pickMostUrgent() {
    return weightedPick(0) ;
  }
  
  
  public Behaviour weightedPick() {
    final float
      sub = actor.traits.relativeLevel(Abilities.STUBBORN),
      range = DEFAULT_PRIORITY_RANGE - (sub * DEFAULT_TRAIT_RANGE) ;
    return weightedPick(range) ;
  }
  
  
  private Behaviour weightedPick(float priorityRange) {
    if (plans.size() == 0) {
      if (verboseReject) I.sayAbout(actor, "  ...Empty choice!") ;
      return null ;
    }
    else if (verbose) I.sayAbout(actor, "Range of choice is "+plans.size()) ;
    if (verbose && I.talkAbout == actor) {
      String label = "Actor" ;
      if (actor.vocation() != null) label = actor.vocation().name ;
      else if (actor.species() != null) label = actor.species().toString() ;
      I.say(actor+" ("+label+") is making a choice, range: "+priorityRange) ;
      I.say("  Current time: "+actor.world().currentTime()) ;
    }
    //
    //  Firstly, acquire the priorities for each plan.  If the permitted range
    //  of priorities is zero, simply return the most promising.
    float bestPriority = 0 ;
    Behaviour picked = null ;
    final float weights[] = new float[plans.size()] ;
    int i = 0 ;
    for (Behaviour plan : plans) {
      final float priority = plan.priorityFor(actor) ;
      if (priority > bestPriority) { bestPriority = priority ; picked = plan ; }
      weights[i++] = priority ;
      if (verbose) I.sayAbout(actor, "  "+plan+" has priority: "+priority) ;
    }
    if (priorityRange == 0) {
      if (verbose) I.sayAbout(actor, "    Picked: "+picked) ;
      return picked ;
    }
    //
    //  Eliminate all weights outside the permitted range, so that only plans
    //  of comparable attractiveness to the most important are considered-
    final float minPriority = Math.max(0, bestPriority - priorityRange) ;
    float sumWeights = 0 ;
    for (i = weights.length ; i-- > 0 ;) {
      weights[i] = Math.max(0, weights[i] - minPriority) ;
      sumWeights += weights[i] ;
    }
    if (sumWeights == 0) {
      if (verbose) I.sayAbout(actor, "    Picked: "+picked) ;
      return picked ;
    }
    //
    //  Finally, select a candidate at random using weights based on priority-
    float randPick = Rand.num() * sumWeights ;
    picked = null ;
    i = 0 ;
    for (Behaviour plan : plans) {
      final float chance = weights[i++] ;
      if (randPick < chance) { picked = plan ; break ; }
      else randPick -= chance ;
    }
    if (verbose) I.sayAbout(actor, "    Picked: "+picked) ;
    return picked ;
  }
}




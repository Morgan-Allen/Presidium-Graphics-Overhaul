

package stratos.game.actors;
import static stratos.game.actors.Qualities.EVASION;

import stratos.game.common.*;
import stratos.game.craft.Placeable;
import stratos.game.plans.*;
import stratos.util.*;



public class SenseUtils {
  
  private static boolean
    sightVerbose = false;
  
  
  /**  Utility method for breaking awareness/pursuit when a hide attempt is
    *  successful.
    */
  public static boolean breaksPursuit(Actor actor, Action taken) {
    
    boolean allBroken = true;
    
    //  TODO:  This is more trouble than it's worth.  Removing for now.
    /*
    for (Plan p : actor.world().activities.activePlanMatches(actor, null)) {
      final Actor follows = p.actor();
      if (p.harmFactor() <= 0 && actor.base() == follows.base()) continue;
      final float
        sightRange   = follows.health.sightRange(),
        chaseUrgency = p.priority() / Plan.PARAMOUNT,
        chasePenalty = -10 * Nums.clamp(chaseUrgency, 0, 1);
      
      final float hideBonus = (actor.skills.test(
        Qualities.EVASION, follows,
        Qualities.SURVEILLANCE, chasePenalty, 1, 2, taken
      ) * ActorHealth.DEFAULT_SIGHT);
      
      final boolean report = sightVerbose && (
        I.talkAbout == actor || I.talkAbout == follows
      );
      if (report) {
        final float dist = Spacing.distance(actor, follows);
        I.say("\nChecking for breakoff of pursuit.");
        I.say("  Between "+follows+" and "+actor);
        I.say("  Sight range:   "+sightRange+" (distance "+dist+")");
        I.say("  Chase urgency: "+chaseUrgency+" (penalty "+chasePenalty+"");
        I.say("  Hide bonus:    "+hideBonus);
      }
      if (! follows.senses.notices(actor, sightRange, hideBonus)) {
        if (report) I.say("  Breakoff successful!");
        //  TODO:  This might need a dedicated method.
        follows.senses.clearAwareness(actor);
        p.interrupt(Plan.INTERRUPT_LOSE_SIGHT);
      }
      else {
        if (report) I.say("  Breakoff failed.");
        allBroken = false;
      }
    }
    
    //*/
    return allBroken;
  }
  
  
  public static boolean indoorsTo(Actor actor, Target e, boolean with) {
    if (! (e instanceof Mobile)) return false;
    final Mobile m = (Mobile) e;
    return m.indoors() && ((m.aboard() == actor.aboard()) == with);
  }
  
  
  
  /**  Used for flanking-checks (returns a result in degrees.)
    */
  public static float flankingAngle(Actor origin, Actor flanks) {
    final Vec2D disp = origin.pathing.displacement(flanks);
    final float angle = disp.normalise().toAngle();
    return Nums.abs(angle - origin.rotation());
  }
  
  
  //  In essence, this gives a bonus to spot an actor- or be spotted yourself-
  //  in cases where you're actively targeting something at range (e.g,
  //  gunshots or dialogue.)
  public static float focusBonus(Target e, Target with, float maxRange) {
    if (! (e instanceof Actor)) return 0;
    final Actor actor = (Actor) e;
    final Action action = actor.currentAction();
    if (action == null || action.subject() != with) return 0;
    
    final float distance = Spacing.distance(actor, action.subject());
    if (distance >= maxRange) return 0;
    else return maxRange - distance;
  }
  
  
  public static float stealthFactor(Target e, Actor looks) {
    if (e instanceof Actor) {
      final Actor other = (Actor) e;
      final Action action = other.currentAction();
      
      float stealth = other.traits.usedLevel(EVASION) / 20f;
      if (action != null && action.quick  ()) stealth /= 2;
      if (action != null && action.careful()) stealth *= 2;
      return Nums.clamp(stealth, 0, 2);
    }
    if (e instanceof Placeable) {
      return ((Placeable) e).structure().cloaking() / 10f;
    }
    return 0;
  }
  
  
  
  /**  Returns whether any blocked tiles lie between the two points given.
    */
  public static boolean hasLineOfSight(
    Actor origin, Target target, float maxRange
  ) {
    if (origin == null || target == null) return false;
    final float distance = Spacing.distance(origin, target);
    if (maxRange > 0 && distance > maxRange) {
      return false;
    }
    if (distance <= 0 || indoorsTo(origin, target, true)) {
      return true;
    }
    final boolean reports = I.talkAbout == origin && sightVerbose;
    
    //  Firstly, we determine the start and end points for the line segment,
    //  and the vector connecting the two-
    final Stage world = origin.world();
    final Vec2D
      orig = new Vec2D().setTo(origin.position(null)),
      dest = new Vec2D().setTo(target.position(null)),
      line = new Vec2D().setTo(dest).sub(orig);
    final float fullLength = line.length();
    final Batch <Tile> considered = new Batch <Tile> ();
    final Vec2D toCheck = new Vec2D();
    
    //  Next, we assemble a list of each tile that might plausibly intersect
    //  the line segment-
    for (int i = 0; i < fullLength; i++) {
      toCheck.setTo(line).normalise().scale(i).add(orig);
      final Tile t = world.tileAt(toCheck.x, toCheck.y);
      if (t == null || t.flaggedWith() != null) continue;
      considered.add(t);
      t.flagWith(considered);
      for (Tile n : t.edgeAdjacent(Spacing.tempT4)) {
        if (n == null || n.flaggedWith() != null) continue;
        considered.add(n);
        n.flagWith(considered);
      }
    }
    for (Tile t : considered) t.flagWith(null);
    
    //
    //  There's a special dispensation here for actors inside defensive turrets
    //  and the like- we allow them to ignore their current platform for line-
    //  of-sight purposes.
    //  TODO:  USE AN INTERFACE FOR THIS!  (Hook into blocksSight below?)
    final Target platform = Patrolling.turretIsAboard(origin);
    
    //  Then, we check to see if any such tiles are actually blocked, and
    //  perform a more exacting intersection test-
    if (reports) {
      I.say("\nCHECKING LINE OF SIGHT TO: "+target);
      I.say("  Mobile origin: "+orig);
      I.say("  Target position: "+dest);
    }
    boolean blocked = false;
    boolean onRight, onLeft;
    for (Tile t : considered) if (t.blocksSight() && t.above() != platform) {
      if (t == target || t.above() == target) continue;
      
      //  We first check whether the centre of the tile in question falls
      //  between the start and end points of the line segment-
      toCheck.set(t.x, t.y).sub(orig);
      final float dot = line.dot(toCheck);
      if (dot < 0 || dot > (fullLength * fullLength)) continue;
      onRight = onLeft = false;
      
      //  Then, we check to see if corners of this tile lie to both the left
      //  and right of the line segment-
      for (int d : Tile.T_DIAGONAL) {
        toCheck.set(
          t.x - (Tile.T_X[d] / 2f),
          t.y - (Tile.T_Y[d] / 2f)
        ).sub(orig);
        final float side = line.side(toCheck);
        if (side < 0) onLeft = true;
        if (side > 0) onRight  = true;
      }
      
      //  If both checks are positive, we consider the tile blocked, and return
      //  the result-
      if (reports) {
        I.say("  Might be blocked at: "+t);
        I.say("  On right/left? "+onRight+"/"+onLeft);
      }
      if (onRight && onLeft) { blocked = true; break; }
    }
    if (reports) I.say(blocked ? "L_O_S BLOCKED!" : "L_O_S OKAY...");
    return ! blocked;
  }
}

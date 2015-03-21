

package stratos.game.actors;
import stratos.game.common.*;
import stratos.util.*;



public class SenseUtils {
  
  private static boolean
    sightVerbose = false;
  
  
  
  /**  Utility method for breaking awareness/pursuit when a hide attempt is
    *  successful.
    */
  public static boolean breaksPursuit(Actor actor) {
    
    boolean allBroken = true;
    
    for (Plan p : actor.world().activities.activePlanMatches(actor, null)) {
      final Actor follows = p.actor();
      final float
        sightRange   = follows.health.sightRange(),
        chaseUrgency = p.priorityFor(follows) / Plan.PARAMOUNT,
        chasePenalty = -10 * Nums.clamp(chaseUrgency, 0, 1);

      final float hideBonus = (actor.skills.test(
        Qualities.STEALTH_AND_COVER, follows,
        Qualities.SURVEILLANCE, chasePenalty, 1, 2
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
        follows.senses.awares.remove(actor);
        follows.senses.awareOf.clear();
        p.interrupt(Plan.INTERRUPT_LOSE_SIGHT);
      }
      else {
        if (report) I.say("  Breakoff failed.");
        allBroken = false;
      }
    }
    
    return allBroken;
  }
  

  
  
  /**  Returns whether any blocked tiles lie between the two points given.
    */
  public static boolean hasLineOfSight(
    Target origin, Target target, float maxRange
  ) {
    if (origin == null || target == null) return false;
    if (maxRange > 0 && Spacing.distance(origin, target) > maxRange) {
      return false;
    }
    final boolean reports = sightVerbose && I.talkAbout == origin;
    
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
    
    //  Then, we check to see if any such tiles are actually blocked, and
    //  perform a more exacting intersection test-
    if (reports) {
      I.say("\nCHECKING LINE OF SIGHT TO: "+target);
      I.say("  Mobile origin: "+orig);
      I.say("  Target position: "+dest);
    }
    boolean blocked = false;
    boolean onRight, onLeft;
    for (Tile t : considered) if (t.blocked()) {
      if (t == target || t.onTop() == target) continue;
      
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

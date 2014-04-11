

package stratos.game.actors;

import stratos.game.common.*;
import stratos.game.tactical.*;
import stratos.game.building.*;
import stratos.util.*;
import stratos.game.common.Session.Saveable;
import java.util.Map.Entry;



//
//  ...You need to be able to lose track of distant objects, based on stealth
//  values.
//
//  ...This makes distant objects almost impossible to maintain consistent
//  tracking of, though.
//  Maybe... just for mobiles.  Or the stealthed.

//  But then... how do you lose track of the venues?

//  Wait.  If it's the current focus of your attention, reduce stealth value
//  by 0.5f, and boost noticeRange.  Aha!  Cool!



public class Senses implements Qualities {
  
  
  /**  
    */
  private static boolean
    reactVerbose  = false,
    noticeVerbose = false,
    sightVerbose  = false;
  
  final Actor actor;
  final Table <Target, Saveable> seen = new Table();
  final Batch <Target> seenBatch = new Batch();
  
  
  protected Senses(Actor actor) {
    this.actor = actor;
  }
  
  
  public void loadState(Session s) throws Exception {
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Target e = s.loadTarget() ;
      seen.put(e, s.loadObject()) ;
      seenBatch.add(e);
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(seen.size()) ;
    for (Target e : seen.keySet()) {
      s.saveTarget(e) ;
      s.saveObject(seen.get(e)) ;
    }
  }
  
  
  
  
  /**  Dealing with seen objects and reactions to them-
    */
  protected void updateSeen() {
    
    final boolean report = reactVerbose && I.talkAbout == actor;
    final World world = actor.world();
    final Batch <Target> unseen = new Batch(), noticed = new Batch();
    final float range = actor.health.sightRange();
    if (report) I.say("\nUpdating seen, sight range: "+range);
    
    //  Add anything newly within range-
    final int reactLimit = 1 + (int) (actor.traits.traitLevel(PERCEPT) / 5);
    world.presences.sampleFromKeys(
      actor, world, reactLimit, noticed,
      Mobile.class,
      Venue.class
    );
    
    //  Automatically include home, work, anyone actively targeting you, and
    //  anything you target.
    noticed.add(actor.mind.home);
    noticed.add(actor.mind.work);
    for (Behaviour b : world.activities.targeting(actor)) {
      if (b instanceof Action) noticed.add(((Action) b).actor);
    }
    noticed.add(actor.focusFor(null));
    
    //  Check for anything new, and react to it.
    final Choice reactions = new Choice(actor);
    for (Target e : noticed) pushAwareness(e, range, reactions);
    final Behaviour reaction = reactions.pickMostUrgent();
    if (actor.mind.wouldSwitchTo(reaction)) {
      actor.mind.assignBehaviour(reaction);
    }
    
    //  Remove anything old.
    for (Entry <Target, Saveable> entry : seen.entrySet()) {
      final Target e = entry.getKey();
      if (! notices(e, range)) unseen.add(e);
    }
    for (Target e : unseen) {
      if (report) I.say("  No longer visible: "+e);
      seen.remove(e);
    }
    
    //  Finally, compile a list of everything seen for easy iteration-
    seenBatch.clear();
    for (Target e : seen.keySet()) seenBatch.add(e);
  }
  
  
  public void pushAwareness(Target e, float sightRange, Choice reactions) {
    if (e == null) return;
    if (sightRange > 0 && ! notices(e, sightRange)) return;
    
    final boolean report = reactVerbose && I.talkAbout == actor;
    final Session.Saveable after = reactionKey(e), before = seen.get(e);
    if (before != after) {
      if (report && before == null) I.say("  Have just seen: "+e);
      if (report) I.say("  Reacting to: "+e);
      if (reactions != null) actor.mind.addReactions(e, reactions);
    }
    seen.put(e, after);
  }
  
  
  private boolean notices(Target e, float sightRange) {
    
    final float fog = actor.base().intelMap.fogAt(e);
    if (fog == 0) return false;
    
    //  Decide on the basic stats for evaluation-
    final boolean report = noticeVerbose && I.talkAbout == actor;
    final boolean
      seen = this.seen.get(e) != null,
      focus = actor.focusFor(null) == e;
    final float
      distance = Spacing.distance(e, actor),
      stealth = stealthFactor(e, actor);
    float
      senseFactor = sightRange * fog,
      hideFactor = distance;
    
    //  The target is easier to spot if they're your current focus, or if
    //  they've already been seen.
    if (seen) senseFactor += sightRange;
    if (focus) {
      //  NOTE:  Hide factor needs to go down to zero in order for long-range
      //  behaviours to work.  Hence, the pure multiply with stealth (if 0.)
      senseFactor += sightRange;
      hideFactor *= stealth;
    }
    else hideFactor *= 0.5f + stealth;
    
    //  Anything focused on you is easier to spot, and indoor targets aren't.
    if (e instanceof Mobile) {
      if (((Mobile) e).indoors()) senseFactor /= 2;
    }
    if (e instanceof Actor) {
      if (((Actor) e).focusFor(null) == actor) senseFactor *= 2;
    }
    
    //  Summarise and return-
    final float noticeChance = senseFactor / (hideFactor + senseFactor);
    final boolean success = Rand.num() < noticeChance;
    if (report) {
      I.say("  Checking to notice: "+e+", chance: "+noticeChance);
      I.say("    Distance/stealth: "+distance+"/"+stealth);
      I.say("    Seen/is focus: "+seen+"/"+focus);
      I.say("    Sense/hide factors: "+senseFactor+"/"+hideFactor);
      I.say("    Success: "+success);
    }
    return success;
  }
  
  
  public static float stealthFactor(Target e, Actor looks) {
    
    if (e instanceof Actor) {
      final Actor a = (Actor) e;
      if (a.base() == looks.base()) return 0;
      float stealth = a.traits.useLevel(STEALTH_AND_COVER) / 20f;
      stealth += a.health.baseSpeed() / Action.moveRate(a, false);
      return stealth;
    }
    
    if (e instanceof Installation) {
      final Installation i = (Installation) e;
      return i.structure().cloaking() / 10f;
    }
    
    return 0 ;
  }
  
  
  private Session.Saveable reactionKey(Target seen) {
    if (seen instanceof Actor) {
      final Actor a = (Actor) seen ;
      if (a.currentAction() == null) return a ;
      final Behaviour b = a.mind.rootBehaviour() ;
      if (b == null) return a ;
      else return b ;
    }
    if (seen instanceof Element) {
      return (Element) seen;
    }
    if (seen instanceof Tile) {
      return (Tile) seen;
    }
    return null;
  }
  
  
  public boolean awareOf(Target e) {
    return seen.get(e) != null ;
  }
  
  
  public boolean hasSeen(Target e) {
    return seen.get(e) != null ;
  }
  
  
  public Batch <Target> awareOf() {
    return seenBatch;
  }
  
  
  
  /**  Returns whether any blocked tiles lie between the two points given.
    */
  public static boolean hasLineOfSight(
    Target origin, Target target, float maxRange
  ) {
    if (origin == null || target == null) return false ;
    if (maxRange > 0 && Spacing.distance(origin, target) > maxRange) {
      return false ;
    }
    final boolean reports = sightVerbose && I.talkAbout == origin ;
    
    //  Firstly, we determine the start and end points for the line segment,
    //  and the vector connecting the two-
    final World world = origin.world() ;
    final Vec2D
      orig = new Vec2D().setTo(origin.position(null)),
      dest = new Vec2D().setTo(target.position(null)),
      line = new Vec2D().setTo(dest).sub(orig) ;
    final float fullLength = line.length() ;
    final Batch <Tile> considered = new Batch <Tile> () ;
    final Vec2D toCheck = new Vec2D() ;
    
    //  Next, we assemble a list of each tile that might plausibly intersect
    //  the line segment-
    for (int i = 0 ; i < fullLength ; i++) {
      toCheck.setTo(line).normalise().scale(i).add(orig) ;
      final Tile t = world.tileAt(toCheck.x, toCheck.y) ;
      if (t == null || t.flaggedWith() != null) continue ;
      considered.add(t) ;
      t.flagWith(considered) ;
      for (Tile n : t.edgeAdjacent(Spacing.tempT4)) {
        if (n == null || n.flaggedWith() != null) continue ;
        considered.add(n) ;
        n.flagWith(considered) ;
      }
    }
    for (Tile t : considered) t.flagWith(null) ;
    
    //  Then, we check to see if any such tiles are actually blocked, and
    //  perform a more exacting intersection test-
    if (reports) {
      I.say("\nCHECKING LINE OF SIGHT TO: "+target) ;
      I.say("  Mobile origin: "+orig) ;
      I.say("  Target position: "+dest) ;
    }
    boolean blocked = false ;
    boolean onRight, onLeft ;
    for (Tile t : considered) if (t.blocked()) {
      if (t == target || t.owner() == target) continue ;
      
      //  We first check whether the centre of the tile in question falls
      //  between the start and end points of the line segment-
      toCheck.set(t.x, t.y).sub(orig);
      final float dot = line.dot(toCheck);
      if (dot < 0 || dot > (fullLength * fullLength)) continue;
      onRight = onLeft = false;
      
      //  Then, we check to see if corners of this tile lie to both the left
      //  and right of the line segment-
      for (int d : Tile.N_DIAGONAL) {
        toCheck.set(
          t.x - (Tile.N_X[d] / 2f),
          t.y - (Tile.N_Y[d] / 2f)
        ).sub(orig) ;
        final float side = line.side(toCheck) ;
        if (side < 0) onLeft = true ;
        if (side > 0) onRight  = true ;
      }
      
      //  If both checks are positive, we consider the tile blocked, and return
      //  the result-
      if (reports) {
        I.say("  Might be blocked at: "+t) ;
        I.say("  On right/left? "+onRight+"/"+onLeft) ;
      }
      if (onRight && onLeft) { blocked = true ; break ; }
    }
    if (reports) I.say(blocked ? "L_O_S BLOCKED!" : "L_O_S OKAY...") ;
    return ! blocked ;
  }
}





package stratos.game.actors;

import stratos.game.common.*;
import stratos.game.plans.Combat;
import stratos.game.plans.CombatUtils;
import stratos.game.tactical.*;
import stratos.game.building.*;
import stratos.util.*;
import stratos.game.common.Session.Saveable;

import java.util.Map.Entry;



public class Senses implements Qualities {
  
  
  /**  Data fields, constants, constructors, and save/load methods-
    */
  private static boolean
    reactVerbose  = false,
    noticeVerbose = false,
    sightVerbose  = false,
    dangerVerbose = false;
  
  final Actor actor;
  final Table <Target, Saveable> awares = new Table <Target, Saveable> ();
  final Batch <Target> awareOf = new Batch <Target> ();

  private boolean emergency ;
  private float   powerLevel;
  private float   fearLevel ;
  
  
  protected Senses(Actor actor) {
    this.actor = actor;
  }
  
  
  public void loadState(Session s) throws Exception {
    for (int n = s.loadInt(); n-- > 0;) {
      final Target e = s.loadTarget();
      awares.put(e, s.loadObject());
      awareOf.add(e);
    }
    emergency  = s.loadBool ();
    powerLevel = s.loadFloat();
    fearLevel  = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(awares.size());
    for (Target e : awares.keySet()) {
      s.saveTarget(e);
      s.saveObject(awares.get(e));
    }
    s.saveBool (emergency );
    s.saveFloat(powerLevel);
    s.saveFloat(fearLevel );
  }
  
  
  
  
  /**  Dealing with seen objects and reactions to them-
    */
  protected void updateSenses() {
    final boolean report = reactVerbose && I.talkAbout == actor;
    final float range = actor.health.sightRange();
    if (report) I.say("\nUpdating senses, sight range: "+range);
    
    //  Get the set of all targets that the actor could observe, and update
    //  one's sense of personal danger on that basis-
    final Batch <Target> toNotice  = toNotice(range);
    final Batch <Target> justSeen  = new Batch <Target> ();
    final Batch <Target> lostSight = new Batch <Target> ();

    for (Target e : toNotice) if (notices(e, range)) {
      final Session.Saveable after = reactionKey(e), before = awares.get(e);
      if (before != after) justSeen.add(e);
      e.flagWith(this);
      awares.put(e, after);
    }
    
    for (Target e : awares.keySet()) {
      if (e.flaggedWith() == this) { e.flagWith(null); continue; }
      if (! notices(e, range)) lostSight.add(e);
    }

    awareOf.clear();
    for (Target e : lostSight) awares.remove(e);
    for (Target e : awares.keySet()) awareOf.add(e);
    updateDangerEval(awareOf);

    final Choice reactions = new Choice(actor);
    for (Target e : justSeen) {
      actor.mind.addReactions(e, reactions);
    }
    final Behaviour reaction = reactions.pickMostUrgent();
    if (actor.mind.wouldSwitchTo(reaction)) {
      actor.mind.assignBehaviour(reaction);
    }
  }
  
  
  protected Batch <Target> toNotice(float range) {
    final Batch <Target> noticed = new Batch <Target> ();
    final World world = actor.world();
    //
    //  Automatically include home, work, anyone actively targeting you, and
    //  anything you target.
    noticed.include(actor.mind.home);
    noticed.include(actor.mind.work);
    for (Behaviour b : world.activities.targeting(actor)) {
      if (b instanceof Action) noticed.include(((Action) b).actor);
    }
    noticed.include(actor.focusFor(null));
    //
    //  And add anything newly within range-
    final float percept = actor.traits.traitLevel(PERCEPT);
    final int reactLimit = (int) (2.5f + (percept / 5));
    int reactCount = 0;
    
    for (Object m : world.presences.matchesNear(Mobile.class, actor, range)) {
      noticed.add((Target) m);
      if (++reactCount > reactLimit) break;
    }
    
    world.presences.sampleFromMaps(
      actor, world, reactLimit, noticed,
      Mobile.class,
      Venue.class
    );
    return noticed;
  }
  
  
  private boolean notices(Target e, final float sightRange) {
    final boolean report = noticeVerbose && I.talkAbout == actor;
    if (e == null || e == actor) return false;
    
    final float distance = Spacing.distance(e, actor);
    final Base  base     = actor.base();
    final float fog      = base.intelMap.fogAt(e);
    if (fog <= 0) return false;
    
    float senseChance = sightRange * fog;
    if (awareOf(e)) senseChance *= 2;
    if (focusedOn(actor, e)) senseChance += sightRange;
    //senseChance *= Rand.saltFrom(actor.world().tileAt(e)) + 0.5f;
    
    float hideChance = distance * (1 + stealthFactor(e, actor));
    if (focusedOn(e, actor)) hideChance /= 2;
    if (indoors(e)) hideChance += sightRange;
    //hideChance *= Rand.saltFrom(actor.origin()) + 0.5f;

    if (report) {
      I.say("  Checking to notice: "+e);
      I.say("    Distance/fog: "+distance+"/"+fog);
      I.say("    Sense/hide chance: "+senseChance+"/"+hideChance);
    }
    
    return senseChance > hideChance;
  }
  
  /*
  protected Choice getReactions(Batch <Target> toNotice, float range) {
    final Choice reactions = new Choice(actor);
    
    for (Target e : toNotice) if (notices(e, range)) {
      final Session.Saveable after = reactionKey(e), before = awares.get(e);
      if (before != after) {
        actor.mind.addReactions(e, reactions);
      }
      e.flagWith(this);
      awares.put(e, after);
    }
    //
    //  Remove anything old.
    final Batch <Target> unseen = new Batch <Target> ();
    for (Target e : awares.keySet()) {
      if (e.flaggedWith() == this) {
        e.flagWith(null);
        continue;
      }
      if (! notices(e, range)) unseen.add(e);
    }
    for (Target e : unseen) awares.remove(e);
    
    return reactions;
  }
  //*/
  
  
  private Session.Saveable reactionKey(Target seen) {
    if (seen instanceof Actor) {
      final Actor a = (Actor) seen;
      if (a.currentAction() == null) return a;
      final Behaviour b = a.mind.rootBehaviour();
      return b == null ? a : b;
    }
    if (seen instanceof Element) {
      return (Element) seen;
    }
    if (seen instanceof Tile) {
      return (Tile) seen;
    }
    return null;
  }
  
  
  private boolean indoors(Target e) {
    if (! (e instanceof Mobile)) return false;
    final Mobile m = (Mobile) e;
    if ((! m.indoors()) || m.aboard() == actor.aboard()) return false;
    return true;
  }
  
  
  private boolean focusedOn(Target e, Target other) {
    if (! (e instanceof Actor)) return false;
    return ((Actor) e).focusFor(null) == other;
  }
  
  
  private float stealthFactor(Target e, Actor looks) {
    
    if (e instanceof Actor) {
      final Actor a = (Actor) e;
      if (a.base() == looks.base()) return 0;
      float stealth = a.traits.useLevel(STEALTH_AND_COVER) / 20f;
      stealth += a.health.baseSpeed() / Action.moveRate(a, false);
      return stealth;
    }
    
    if (e instanceof Installation) {
      return ((Installation) e).structure().cloaking() / 10f;
    }
    return 0;
  }
  
  
  public boolean awareOf(Target e) {
    return awares.get(e) != null;
  }
  
  
  public boolean hasSeen(Target e) {
    return awares.get(e) != null;
  }
  
  
  public Batch <Target> awareOf() {
    return awareOf;
  }
  
  
  
  /**  Threat Evaluation methods-
    */
  protected void updateDangerEval(Batch <Target> awareOf) {
    final boolean report = dangerVerbose && I.talkAbout == actor;
    
    emergency = false;
    powerLevel = CombatUtils.powerLevel(actor);
    float sumAllies = 1, sumFoes = 0;
    
    for (Target t : awareOf) if ((t instanceof Actor) && (t != actor)) {
      final Actor near = (Actor) t;
      
      if (CombatUtils.isHostileTo(actor, near)) {
        if (report) I.say("Enemy nearby: "+near);
        emergency = true;
        sumFoes += CombatUtils.powerLevelRelative(near, actor);
      }
      else if (CombatUtils.isAllyOf(actor, near)) {
        if (report) I.say("Ally nearby: "+near);
        sumAllies += near.senses.powerLevel() * 2 / (1 + powerLevel);
      }
    }
    
    fearLevel = sumFoes / (sumFoes + sumAllies);
    fearLevel += actor.base().dangerMap.sampleAt(actor);
    
    if (report) {
      I.say("Sum allies: "+sumAllies);
      I.say("Sum of foes: "+sumFoes);
      I.say("Fear level: "+fearLevel);
    }
  }
  
  
  public boolean isEndangered() {
    return emergency;
  }
  
  
  public float powerLevel() {
    return powerLevel;
  }
  
  
  public float fearLevel() {
    return fearLevel;
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
    final World world = origin.world();
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
      for (int d : Tile.N_DIAGONAL) {
        toCheck.set(
          t.x - (Tile.N_X[d] / 2f),
          t.y - (Tile.N_Y[d] / 2f)
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







package stratos.game.actors;

import stratos.game.common.*;
import stratos.game.plans.*;
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

  private boolean emergency  = false;
  private float   powerLevel = 0    ;
  private float   fearLevel  = 0    ;
  private Target  safePoint  = null ;
  
  
  protected Senses(Actor actor) {
    this.actor = actor;
  }
  
  
  public void loadState(Session s) throws Exception {
    for (int n = s.loadInt(); n-- > 0;) {
      final Target e = s.loadTarget();
      awares.put(e, s.loadObject());
      awareOf.add(e);
    }
    emergency  = s.loadBool  ();
    powerLevel = s.loadFloat ();
    fearLevel  = s.loadFloat ();
    safePoint  = s.loadTarget();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(awares.size());
    for (Target e : awares.keySet()) {
      s.saveTarget(e);
      s.saveObject(awares.get(e));
    }
    s.saveBool  (emergency );
    s.saveFloat (powerLevel);
    s.saveFloat (fearLevel );
    s.saveTarget(safePoint );
  }
  
  
  
  
  /**  Dealing with seen objects and reactions to them-
    */
  protected void updateSenses() {
    final boolean report = reactVerbose && I.talkAbout == actor;
    final float range = actor.health.sightRange();
    if (report) I.say("\nUpdating senses, sight range: "+range);
    
    //  First, get the set of all targets that the actor might observe.
    final Batch <Target> toNotice  = toNotice(range);
    final Batch <Target> justSeen  = new Batch <Target> ();
    final Batch <Target> lostSight = new Batch <Target> ();
    
    //  Get the set of all freshly-spotted targets-
    for (Target e : toNotice) if (notices(e, range)) {
      final Session.Saveable after = reactionKey(e), before = awares.get(e);
      if (before != after) justSeen.add(e);
      e.flagWith(this);
      awares.put(e, after);
    }
    
    //  Then iterate over anything you're currently aware of, and get the set
    //  of all targets we've lost sight of-
    for (Target e : awares.keySet()) {
      if (e.flaggedWith() == this) { e.flagWith(null); continue; }
      if (! notices(e, range)) lostSight.add(e);
    }
    
    //  Remove the latter from the list, and having added the former, update
    //  our sense of personal endangerment-
    awareOf.clear();
    for (Target e : lostSight) awares.remove(e);
    for (Target e : awares.keySet()) awareOf.add(e);
    updateDangerEval(awareOf);
    
    //  And finally, add any reactions to freshly-spotted targets-
    //  TODO:  Delegate all this to the ActorMind class..?
    final Choice reactions = new Choice(actor);
    if (isEmergency()) {
      actor.mind.putEmergencyResponse(reactions);
    }
    for (Target e : justSeen) {
      actor.mind.addReactions(e, reactions);
    }
    
    final Behaviour reaction = reactions.pickMostUrgent();
    if (report) {
      I.say("\nTOP REACTION IS: "+reaction);
      I.say("  Current behaviour: "+actor.mind.rootBehaviour());
    }
    if (actor.mind.wouldSwitchTo(reaction)) {
      if (report) I.say("  Switching over!");
      actor.mind.assignBehaviour(reaction);
    }
    else if (report) I.say("  Sticking with current plan.");
  }
  
  
  protected Batch <Target> toNotice(float range) {
    final Batch <Target> noticed = new Batch <Target> ();
    final Stage world = actor.world();
    //
    //  Automatically include home, work, anyone actively targeting you, and
    //  anything you target.
    noticed.include(actor.mind.home);
    noticed.include(actor.mind.work);
    final Target focus = actor.focusFor(null);
    if (focus instanceof Element) {
      noticed.include(focus);
    }
    for (Behaviour b : world.activities.activePlanMatches(actor, null)) {
      if (b instanceof Action) {
        noticed.include(((Action) b).actor);
      }
    }
    //
    //  And add anything newly within range-
    final float percept = actor.traits.usedLevel(PERCEPT);
    final int reactLimit = (int) (2.5f + (percept / 5));
    world.presences.sampleFromMaps(
      actor, world, reactLimit, noticed,
      Mobile.class,
      Venue.class
    );
    return noticed;
  }
  
  
  private boolean notices(Target e, final float sightRange) {
    if (e == null || e == actor) return false;
    final boolean report = noticeVerbose && I.talkAbout == actor;
    
    final float distance = Spacing.distance(e, actor);
    final Base  base     = actor.base();
    final float fog      = base.intelMap.fogAt(e);
    if (fog <= 0) return false;
    
    float senseChance = sightRange * fog;
    if (awareOf(e)) senseChance *= 2;
    senseChance += focusBonus(e    , null, sightRange    );
    senseChance += focusBonus(actor, e   , sightRange * 2);
    
    float hideChance = distance * (1 + stealthFactor(e, actor));
    if (indoors(e)) hideChance += sightRange;
    
    if (report && senseChance > hideChance) {
      I.say("\n  Have noticed:     "+e);
      I.say("    Stealth value:  "+stealthFactor(e, actor));
      if (e instanceof Actor) {
        final Actor o = (Actor) e;
        I.say("    Current motion: "+Action.moveRate(o, true));
        I.say("    Base speed:     "+o.health.baseSpeed());
        I.say("    Stealth skill:  "+o.traits.usedLevel(STEALTH_AND_COVER));
      }
      I.say("    Distance/fog:   "+distance+"/"+fog);
      I.say("    Sense vs. hide: "+senseChance+" vs. "+hideChance);
    }
    return senseChance > hideChance;
  }
  
  
  private Session.Saveable reactionKey(Target seen) {
    if (seen instanceof Actor) {
      final Actor a = (Actor) seen;
      if (a.currentAction() == null) return a;
      final Behaviour b = a.mind.rootBehaviour();
      return b == null ? a : b;
    }
    if (seen instanceof Session.Saveable) {
      return (Session.Saveable) seen;
    }
    return null;
  }
  
  
  private boolean indoors(Target e) {
    if (! (e instanceof Mobile)) return false;
    final Mobile m = (Mobile) e;
    if ((! m.indoors()) || m.aboard() == actor.aboard()) return false;
    return true;
  }
  
  
  //  In essence, this gives to spot an actor- or be spotted yourself- in cases
  //  where you're actively targetting something at range (e.g, gunshots or
  //  dialogue.)
  private float focusBonus(Target e, Target with, float maxRange) {
    if (! (e instanceof Actor)) return 0;
    final Actor other = (Actor) e;
    final Target focus = other.focusFor(null);
    if (with != null && with != focus) return 0;
    if (focus == null || Spacing.distance(actor, focus) > maxRange) return 0;
    return Spacing.distance(other, focus);
  }
  
  
  private float stealthFactor(Target e, Actor looks) {
    if (e instanceof Actor) {
      final Actor a = (Actor) e;
      if (a.base() == looks.base()) return 0;
      
      final float speed = a.health.baseSpeed();
      float stealth = a.traits.usedLevel(STEALTH_AND_COVER) / 20f;
      stealth += 0.5f - (Action.moveRate(a, false) / (speed * 1));
      
      return Visit.clamp(stealth, 0, 2);
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
    if (report) {
      I.say("\nUpdating danger assessment for "+actor);
      I.say("  Vocation: "+actor.vocation());
    }
    
    emergency = false;
    powerLevel = CombatUtils.powerLevel(actor);
    float sumAllies = 1, sumFoes = 0;
    
    for (Target t : awareOf) if ((t instanceof Actor) && (t != actor)) {
      final Actor near = (Actor) t;
      float hostility = CombatUtils.hostileRating(actor, near);
      ///emergency |= actor.relations.valueFor(near.base()) <= 0;
      
      if (hostility > 0) {
        if (report) I.say("  Enemy nearby: "+near+", hostility: "+hostility);
        
        float power = CombatUtils.powerLevelRelative(near, actor);
        hostility = Visit.clamp(hostility + 0.5f, 0, 1);
        
        if (CombatUtils.isActiveHostile(actor, near)) {
          emergency = true;
          hostility += 1;
          if (near.focusFor(Combat.class) == actor) power *= 2;
        }
        sumFoes += power * hostility;
      }
      else {
        float power = near.senses.powerLevel();
        if (report) I.say("  Ally nearby: "+near+", bond: "+(0 - hostility));
        sumAllies += power * (0.5f - hostility) * 2 / (1 + powerLevel);
      }
    }
    
    final float ambientDanger = actor.base().dangerMap.sampleAt(actor);
    if (ambientDanger > 0) sumFoes += ambientDanger / powerLevel;
    else sumAllies += 0 - ambientDanger / powerLevel;
    
    fearLevel = sumFoes / (sumFoes + sumAllies);
    safePoint = Retreat.nearestHaven(actor, null, emergency);
    
    if (report) {
      I.say("Sum allies:  "+sumAllies);
      I.say("Sum foes:    "+sumFoes  );
      I.say("Fear level:  "+fearLevel);
      I.say("Safe point:  "+safePoint);
    }
  }
  
  
  public boolean isEmergency() {
    return emergency;
  }
  
  
  public float powerLevel() {
    return powerLevel;
  }
  
  
  public float fearLevel() {
    return fearLevel;
  }
  
  
  public Boarding haven() {
    return (Boarding) safePoint;
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





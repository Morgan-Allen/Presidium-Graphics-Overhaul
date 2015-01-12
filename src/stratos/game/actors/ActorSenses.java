

package stratos.game.actors;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.util.*;
import stratos.game.economic.*;
import stratos.game.common.Session.Saveable;


//  TODO:  I need to distinguish between objects the actor can *see* and
//  objects they *remember*.


public class ActorSenses implements Qualities {
  
  
  /**  Data fields, constants, constructors, and save/load methods-
    */
  private static boolean
    reactVerbose  = false,
    noticeVerbose = false,
    sightVerbose  = false,
    dangerVerbose = false;
  
  final static int NUM_DIRS = TileConstants.T_INDEX.length / 2;
  
  final Actor actor;
  final Table <Target, Saveable> awares = new Table <Target, Saveable> ();
  final Batch <Target> awareOf = new Batch <Target> ();

  private boolean emergency  = false;
  private float   powerLevel = 0    ;
  private float   fearLevel  = 0    ;
  
  private float fearByDirection[] = new float[4];
  private Target  safePoint  = null ;
  
  
  public ActorSenses(Actor actor) {
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
  public void updateSenses() {
    final boolean report = reactVerbose && I.talkAbout == actor;
    final float range = actor.health.sightRange();
    if (report) I.say("\nUpdating senses, sight range: "+range);
    
    //  First, get the set of all targets that the actor might observe.
    final float percept = actor.traits.usedLevel(PERCEPT);
    final int reactLimit = (int) (2.5f + (percept / 5));
    final Batch <Target> toNotice  = toNotice(range, reactLimit);
    final Batch <Target> justSeen  = new Batch <Target> ();
    final Batch <Target> lostSight = new Batch <Target> ();
    
    //  Get the set of all freshly-spotted targets-
    for (Target e : toNotice) if (notices(e, range, 0)) {
      final Session.Saveable after = reactionKey(e), before = awares.get(e);
      if (before != after) justSeen.add(e);
      e.flagWith(this);
      awares.put(e, after);
    }
    
    //  Then iterate over anything you're currently aware of, and get the set
    //  of all targets we've lost sight of-
    for (Target e : awares.keySet()) {
      if (e.flaggedWith() == this) { e.flagWith(null); continue; }
      if (! notices(e, range, 0)) lostSight.add(e);
    }
    
    //  Remove the latter from the list, and having added the former, update
    //  our sense of personal endangerment-
    awareOf.clear();
    for (Target e : lostSight) awares.remove(e);
    for (Target e : awares.keySet()) awareOf.add(e);
    updateDangerEval(awareOf);
    
    if (report) {
      I.say("Currently aware of: (react limit "+reactLimit+")");
      for (Target e : awareOf) {
        final String dist = " (distance "+Spacing.distance(actor, e)+")";
        I.say("  "+e+(justSeen.includes(e) ? " (NEW)" : "")+dist);
      }
      for (Target e : lostSight) I.say("  Lost sight of "+e);
    }
    
    //  And finally, add any reactions to freshly-spotted targets-
    //  TODO:  Delegate all this to the ActorMind class..?
    final Choice reactions = new Choice(actor);
    reactions.isVerbose = report;
    if (isEmergency()) {
      actor.mind.putEmergencyResponse(reactions);
    }
    for (Target e : justSeen) {
      actor.mind.addReactions(e, reactions);
    }
    
    final Behaviour reaction = reactions.pickMostUrgent();
    if (reaction == null) return;
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
  
  
  protected Batch <Target> toNotice(float range, int reactLimit) {
    final Batch <Target> noticed = new Batch <Target> ();
    final Stage world = actor.world();
    //
    //  Automatically include home, work, anyone actively targeting you, and
    //  anything you target.
    noticed.include(actor.mind.home);
    noticed.include(actor.mind.work);
    final Target focus = actor.actionFocus();
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
    world.presences.sampleFromMaps(
      actor, world, reactLimit, noticed,
      Mobile.class,
      Venue.class,
      Item.Dropped.class
    );
    return noticed;
  }
  
  
  private boolean notices(Target e, final float sightRange, float hideBonus) {
    if (e == null || e == actor) return false;
    final boolean report = noticeVerbose && I.talkAbout == actor;
    
    final float distance = Spacing.distance(e, actor);
    final Base  base     = actor.base();
    final float fog      = base.intelMap.fogAt(e);
    
    float senseChance = sightRange * fog;
    if (awareOf(e)) senseChance *= 2;
    senseChance += focusBonus(e    , null, sightRange    );
    senseChance += focusBonus(actor, e   , sightRange * 2);
    
    float hideChance = distance * (1 + stealthFactor(e, actor));
    if (indoors(e)) hideChance += sightRange;
    hideChance += hideBonus;
    
    if (report && senseChance > hideChance) {
      I.say("\n  Have noticed:     "+e);
      I.say("    Stealth value:  "+stealthFactor(e, actor));
      if (e instanceof Actor) {
        final Actor o = (Actor) e;
        I.say("    Current motion: "+Action.speedMultiple(o, true));
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
    final Target focus = other.actionFocus();
    if (with != null && with != focus) return 0;
    if (focus == null || Spacing.distance(actor, focus) > maxRange) return 0;
    return Spacing.distance(other, focus);
  }
  
  
  private float stealthFactor(Target e, Actor looks) {
    if (e instanceof Actor) {
      final Actor other = (Actor) e;
      final Action action = other.currentAction();
      
      float stealth = other.traits.usedLevel(STEALTH_AND_COVER) / 20f;
      if (action != null && action.quick  ()) stealth /= 2;
      if (action != null && action.careful()) stealth *= 2;
      return Nums.clamp(stealth, 0, 2);
    }
    if (e instanceof Structure.Basis) {
      return ((Structure.Basis) e).structure().cloaking() / 10f;
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
    
    //  Firstly, we iterate over every visible target, determine their degree
    //  of hostility, and sum for all allies and enemies nearby.  (We also
    //  split the counts up by quadrant, to allow for directional decisions
    //  about where to retreat.)
    float sumAllies = 1, sumFoes = 0;
    for (int n = NUM_DIRS; n-- > 0;) fearByDirection[n] = 0;
    emergency = false;
    powerLevel = CombatUtils.powerLevel(actor);
    final Base attacked = CombatUtils.baseAttacked(actor);
    
    for (Target t : awareOf) if ((t instanceof Actor) && (t != actor)) {
      final Actor near = (Actor) t;
      float hostility = CombatUtils.hostileRating(actor, near), avoidance = 0;
      
      //  We set the emergency flag only if the other actor is actively doing
      //  something dangerous, and provide some bonuses to threat rating.
      if (hostility > 0) {
        if (report) I.say("  Enemy nearby: "+near+", hostility: "+hostility);
        
        float power = CombatUtils.powerLevelRelative(near, actor);
        hostility = Nums.clamp(hostility + 0.5f, 0, 1);
        
        if (CombatUtils.isActiveHostile(actor, near)) {
          if (report) I.say("  Is active hostile!");
          emergency = true;
          hostility += 1;
          power *= 2;
        }
        final float foeRating = power * hostility;
        sumFoes += foeRating;
        avoidance = foeRating;
      }
      else {
        float power = near.senses.powerLevel();
        if (report) I.say("  Ally nearby: "+near+", bond: "+(0 - hostility));
        sumAllies += power * (0.5f - hostility) * 2 / (1 + powerLevel);
      }

      //  If you're doing something harmful to a member of a given base, then
      //  anyone from that base is considered a potential fear-source (at least
      //  for pathing and cover-taking purposes.  Naturally, this also applies
      //  to any real enemies detected.)  So we record the quadrant this threat
      //  lies in, with a partial bonus to either side:
      if (near.base() == attacked) {
        if (report) I.say("  Belongs to base attacked...");
        avoidance = Nums.max(avoidance, 1);
      }
      if (avoidance > 0) {
        int quadrant = Spacing.compassDirection(actor.origin(), near.origin());
        int left  = ((quadrant /= 2) + 1      ) % NUM_DIRS;
        int right = (quadrant + (NUM_DIRS - 1)) % NUM_DIRS;
        fearByDirection[quadrant] += avoidance    ;
        fearByDirection[left    ] += avoidance / 2;
        fearByDirection[right   ] += avoidance / 2;
      }
    }
    
    //  Finally, we adjust our sense of danger/safety based on ambient danger
    //  levels for the region as a whole:
    final float ambientDanger = actor.base().dangerMap.sampleAround(
      actor, Stage.SECTOR_SIZE
    );
    if (ambientDanger > 0) sumFoes += ambientDanger / powerLevel;
    else sumAllies += 0 - ambientDanger / powerLevel;
    
    fearLevel = sumFoes / (sumFoes + sumAllies);
    safePoint = Retreat.nearestHaven(actor, null, emergency);
    
    if (report) {
      I.say("Sum allies:  "+sumAllies);
      I.say("Sum foes:    "+sumFoes  );
      I.say("Fear level:  "+fearLevel);
      I.say("Safe point:  "+safePoint);
      I.say("Emergency:   "+emergency);
      
      I.say("Danger by direction:");
      for (int n : TileConstants.T_ADJACENT) {
        I.say("  "+TileConstants.DIR_NAMES[n]+": "+dangerFromDirection(n));
      }
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
  
  
  public float dangerFromDirection(Target point) {
    final Tile at = actor.world().tileAt(point);
    final int quadrant = Spacing.compassDirection(actor.origin(), at) / 2;
    return fearByDirection[quadrant];
  }
  
  
  public float dangerFromDirection(int dirIndex) {
    return fearByDirection[dirIndex / 2];
  }
  
  
  
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
        STEALTH_AND_COVER, follows, SURVEILLANCE, chasePenalty, 1, 2
      ) * ActorHealth.DEFAULT_SIGHT);
      
      final boolean report = reactVerbose && (
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





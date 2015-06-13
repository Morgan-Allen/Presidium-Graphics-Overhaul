

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
    dangerVerbose = false;
  
  final static int NUM_DIRS = TileConstants.T_INDEX.length / 2;
  
  final Actor actor;
  final Table <Target, Saveable> awares = new Table <Target, Saveable> ();
  final Batch <Target> awareOf = new Batch <Target> ();
  
  private boolean emergency   = false;
  private boolean underAttack = false;
  private float   powerLevel  = -1   ;
  private float   fearLevel   =  0   ;
  
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
    emergency   = s.loadBool  ();
    underAttack = s.loadBool  ();
    powerLevel  = s.loadFloat ();
    fearLevel   = s.loadFloat ();
    safePoint   = s.loadTarget();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(awares.size());
    for (Target e : awares.keySet()) {
      s.saveTarget(e);
      s.saveObject(awares.get(e));
    }
    s.saveBool  (emergency  );
    s.saveBool  (underAttack);
    s.saveFloat (powerLevel );
    s.saveFloat (fearLevel  );
    s.saveTarget(safePoint  );
  }
  
  
  
  /**  Dealing with seen objects and reactions to them-
    */
  //  TODO:  Move the decision-related bits of this to the Mind class.
  
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
      for (Target e : justSeen ) I.say("  Just saw "+e);
      for (Target e : lostSight) I.say("  Lost sight of "+e);
    }
    
    //  And finally, add any reactions to freshly-spotted targets-
    //  TODO:  Delegate all this to the ActorMind class..?
    final Choice reactions = new Choice(actor);
    reactions.isVerbose = report;
    for (Target e : justSeen) {
      actor.mind.addReactions(e, reactions);
    }
    if (isEmergency()) {
      actor.mind.putEmergencyResponse(reactions);
    }
    
    final Behaviour reaction = reactions.pickMostUrgent();
    if (reaction == null) return;
    final Behaviour root = actor.mind.rootBehaviour();
    if (report) {
      I.say("\nTOP REACTION IS: "+reaction);
      I.say("  Current behaviour: "+root);
    }
    if (Choice.wouldSwitch(actor, root, reaction, true, report)) {
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
    for (Action a : world.activities.actionMatches(actor)) {
      noticed.include(a.actor());
    }
    //
    //  And add anything newly within range-
    
    //  TODO:  Just use the stuff picked up during fog-lifting!
    for (Object o : world.presences.matchesNear(Mobile.class, actor, range)) {
      if (o == actor) continue;
      noticed.include((Target) o);
      if (noticed.size() > (reactLimit * 2)) break;
    }
    world.presences.sampleFromMaps(
      actor, world, reactLimit, noticed,
      Mobile.class,
      Venue.class,
      Item.Dropped.class
    );
    return noticed;
  }
  
  
  protected boolean notices(Target e, final float sightRange, float hideBonus) {
    if (e == null || e == actor) return false;
    final boolean report = noticeVerbose && I.talkAbout == actor;
    if (report) I.say("\nChecking to notice: "+e);
    
    final float distance = Spacing.distance(e, actor);
    final Base  base     = actor.base();
    final float fog      = base.intelMap.fogAt(e);
    
    float senseChance = sightRange * fog;
    if (awareOf(e)) senseChance *= 2;
    senseChance += focusBonus(e    , null, sightRange    );
    senseChance += focusBonus(actor, e   , sightRange * 2);
    
    float hideChance = distance * (1 + stealthFactor(e, actor));
    if (SenseUtils.indoorsTo(actor, e, false)) hideChance += sightRange;
    hideChance += hideBonus;
    final boolean noticed = senseChance > hideChance;
    
    if (report && ! noticed) {
      I.say("\n  Have noticed      "+e+"? "+noticed);
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
    return noticed;
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
    if (e instanceof Placeable) {
      return ((Placeable) e).structure().cloaking() / 10f;
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
  //  TODO:  This needs to be merged with the combatWinChance and homeDistance
  //  methods in PlanUtils.
  
  protected void updateDangerEval(Batch <Target> awareOf) {
    final boolean report = I.talkAbout == actor && dangerVerbose;
    if (report) {
      I.say("\nUpdating danger assessment for "+actor);
      I.say("  Total aware of: "+awareOf.size());
      for (Target t : awareOf) I.say("    "+t);
    }
    
    float sumAllies = 1, sumFoes = 0, bravery;
    boolean stealthy = Action.isStealthy(actor);
    emergency   = false;
    underAttack = false;
    powerLevel  = CombatUtils.powerLevel(actor);
    bravery     = (2 + actor.traits.relativeLevel(FEARLESS)) / 2;
    
    for (Target t : awareOf) if (t instanceof Actor) {
      final Actor near = (Actor) t;
      if (near == actor || ! near.health.alive()) continue;
      if (stealthy && ! near.senses.awareOf(actor)) continue;
      
      float attackRisk = PlanUtils.combatPriority(actor, near, 0, 0, false, 1);
      if (attackRisk > 0) {
        attackRisk = Nums.clamp((attackRisk + 5) / 10, 0, 2);
        final float power = CombatUtils.powerLevelRelative(near, actor);
        sumFoes += power * attackRisk;
        if (report) {
          I.say("  Enemy nearby: "+near);
          I.say("    Attack chance: "+attackRisk+", power: "+power);
        }
      }
      else {
        final float power = near.senses.powerLevel() / (1 + powerLevel);
        final float helpChance = Nums.clamp(attackRisk / -10, 0, 1);
        sumAllies += power * helpChance;
        if (report) {
          I.say("  Ally nearby: "+near);
          I.say("    Help chance: "+helpChance+", power: "+power);
        }
      }
      
      final Target victim = near.planFocus(Combat.class, true);
      emergency   |= victim != null || sumFoes > bravery;
      underAttack |= victim == actor;
    }
    
    //
    //  Finally, we adjust our sense of danger/safety based on ambient danger
    //  levels for the region as a whole.
    final float ambientDanger = actor.base().dangerMap.sampleAround(
      actor, Stage.ZONE_SIZE
    );
    if (underAttack || ! stealthy) {
      if (sumFoes > 0 && ambientDanger > 0) {
        sumFoes += ambientDanger / powerLevel;
      }
      if (sumAllies > 0 && ambientDanger < 0) {
        sumAllies += 0 - ambientDanger / powerLevel;
      }
    }
    fearLevel = sumFoes / (sumFoes + sumAllies);
    safePoint = Retreat.nearestHaven(actor, null, emergency);

    if (report) {
      I.say("  Sum allies:      "+sumAllies    );
      I.say("  Sum foes:        "+sumFoes      );
      I.say("  Bravery:         "+bravery      );
      I.say("  Personal power:  "+powerLevel   );
      I.say("  Ambient danger:  "+ambientDanger);
      I.say("  Fear level:      "+fearLevel    );
      I.say("  Safe point:      "+safePoint    );
      I.say("  Emergency:       "+emergency    );
      I.say("  In stealth mode: "+stealthy     );
      I.say("  Under attack:    "+underAttack  );
      I.say("Danger by direction:");
      for (int n : TileConstants.T_ADJACENT) {
        I.say("  "+TileConstants.DIR_NAMES[n]+": "+dangerFromDirection(n));
      }
    }
  }
  
  
  public float powerLevel() {
    if (powerLevel == -1) powerLevel = CombatUtils.powerLevel(actor);
    return powerLevel;
  }
  
  
  public boolean isEmergency() {
    return emergency;
  }
  
  
  public boolean underAttack() {
    return underAttack;
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
}




/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



public class Retreat extends Plan implements Qualities {
  
  
  //  TODO:  Hiding evaluation needs to be much more reliable!  You'll have to
  //  check all visible tiles, I think.  Or maybe reserve strictly for agents?
  //  Yeah.  They get some special treatment.
  
  
  /**  Constants, field definitions, constructors and save/load methods-
    */
  final static float
    DANGER_MEMORY_FADE = 0.9f,  //TODO:  use a time-limit instead?
    MIN_RETREAT_DIST = Stage.SECTOR_SIZE / 2;
  
  private static boolean
    havenVerbose = false,
    stepsVerbose = true ;
  
  
  private float maxPriority = 0;
  private Boarding safePoint = null;
  
  
  public Retreat(Actor actor) {
    this(actor, actor.senses.haven());
  }
  
  
  public Retreat(Actor actor, Boarding safePoint) {
    super(actor, actor, MOTIVE_LEISURE, NO_HARM);
    this.safePoint = safePoint;
  }


  public Retreat(Session s) throws Exception {
    super(s);
    this.maxPriority = s.loadFloat();
    this.safePoint = (Boarding) s.loadTarget();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveFloat(maxPriority);
    s.saveTarget(safePoint);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Retreat(other, safePoint);
  }
  
  
  
  /**  Evaluation of havens and hide points-
    */
  public static Boarding nearestHaven(
    final Actor actor, Class prefClass, final boolean emergency
  ) {
    final Target oldHaven = actor.senses.haven();
    final Stage world = actor.world();
    final float
      runRange   = actor.health.sightRange() + Stage.SECTOR_SIZE,
      sightHaven = Stage.SECTOR_SIZE / 2;
    final boolean
      atHaven  = actor.aboard() == oldHaven,
      mustMove = atHaven && actor.senses.isEmergency() && ! actor.indoors();
    
    if (mustMove && I.talkAbout == actor) {
      I.say("\nObtaining retreat point for "+actor);
    }
    
    final Pick <Boarding> pick = new Pick <Boarding> () {
      
      public void compare(Boarding next, float rating) {
        if (next == null || ! next.allowsEntry(actor)) return;
        
        final float absDist = Spacing.distance(actor, next);
        if (mustMove && absDist <= sightHaven) return;
        
        final int direction = Spacing.compassDirection(
          actor.origin(), world.tileAt(next)
        );
        rating *= 1 - actor.senses.dangerFromDirection(direction);
        
        final float dist = 1 + (absDist / runRange);
        super.compare(next, rating / (dist * (emergency ? 0.5f : 0.2f)));
      }
    };
    
    if (emergency) {
      if (pick.empty()) {
        pick.compare(pickHidePoint(actor, runRange, actor, -2), 2);
      }
      if (pick.empty()) {
        pick.compare(pickHidePoint(actor, runRange, actor,  0), 1);
      }
      if (pick.empty()) {
        pick.compare(Spacing.pickRandomTile(actor, runRange, world), 0);
      }
    }
    
    pick.compare(actor.mind.home(), 10);
    pick.compare(actor.mind.work(), 5 );
    
    final Presences presences = actor.world().presences;
    final Target refuge = presences.nearestMatch(
      Economy.SERVICE_REFUGE, actor, -1
    );
    final Target pref   = presences.nearestMatch(
      prefClass             , actor, -1
    );
    final Target cover  = presences.nearestMatch(
      Venue.class           , actor, -1
    );
    pick.compare((Boarding) refuge, emergency ? 5 : 10);
    pick.compare((Boarding) pref  , 10                );
    pick.compare((Boarding) cover , emergency ? 1 : 2 );
    
    if (pick.result() != null) return pick.result();
    else return actor.aboard();
  }
  
  
  
  /**  Picks a nearby tile for the actor to temporarily withdraw to (as opposed
    *  to a full-blown long-distance retreat.)  Used to perform hit-and-run
    *  tactics, stealth while travelling, or an emergency hide.
    */
  public static Tile pickHidePoint(
    final Actor actor, float range, Target from, final int advanceFactor
  ) {
    final boolean report = havenVerbose && I.talkAbout == actor;
    if (report) I.say("\nPICKING POINT OF WITHDRAWAL FROM "+actor.origin());
    
    //  The idea here is to pick tiles at random at first, then as the actor
    //  gets closer to a given area, allow systematic scanning of nearby tiles
    //  to zero in on any strong cover available.
    final Stage world = actor.world();
    final Tile  at    = actor.origin();
    final Pick <Tile> pick = new Pick <Tile> () {
      public void compare(Tile next, float rating) {
        if (advanceFactor == 0) rating *= 1;
        else rating *= rateTileCover(actor, next, advanceFactor);
        if (report) I.say("  Rating for "+next+" was: "+rating);
        super.compare(next, rating);
      }
    };
    
    //  We provide a slight rating bonus for the actor's current location, then
    //  compare random tiles in each direction, and then compare any tiles
    //  within 2 units of the actor's origin.  Then return the most promising
    //  result.
    pick.compare(at, 1.1f);
    
    for (int n : TileConstants.T_ADJACENT) {
      Tile t = world.tileAt(
        Nums.clamp(at.x + (TileConstants.T_X[n] * range), 0, world.size - 1),
        Nums.clamp(at.y + (TileConstants.T_Y[n] * range), 0, world.size - 1)
      );
      t = Spacing.pickRandomTile(t, range, world);
      t = Spacing.nearestOpenTile(t, t);
      pick.compare(t, 1);
    }
    
    final Box2D around = actor.area(null).expandBy(2);
    for (Tile t : world.tilesIn(around, true)) pick.compare(t, 1);
    
    final Tile hides = pick.result();
    if (report) I.say("HIDING AT: "+hides);
    return hides;
  }
  
  
  private static float rateTileCover(Actor actor, Tile t, int advanceFactor) {
    //  TODO:  Check to make sure the tile is reachable!
    if (t == null || t.blocked()) return 0;

    final boolean report = havenVerbose && I.talkAbout == actor;
    
    //  We confer a bonus to the rating if the tile in question has cover in
    //  the same direction as the actor's perceived sources of danger, while
    //  allowing clear sight to one side or the other (for easy retaliation.)
    float rating = 0.5f;
    final Tile allNear[] = t.allAdjacent(null);
    
    if (report) {
      for (int n : TileConstants.T_INDEX) if (isCover(allNear[n])) {
        I.say("      Blocked: "+TileConstants.DIR_NAMES[n]);
      }
    }
    
    for (int n : TileConstants.T_ADJACENT) {
      final Tile
        tile  = allNear[n],
        left  = allNear[(n + 1) % 8],
        right = allNear[(n + 7) % 8];
      if (isCover(tile) && ! (isCover(left) && isCover(right))) {
        final float danger = actor.senses.dangerFromDirection(n);
        if (report && danger > 0) I.say(
          "    Cover from "+TileConstants.DIR_NAMES[n]+
          "    Danger: "+actor.senses.dangerFromDirection(n)
        );
        rating += danger;
      }
    }
    
    //  We also favour locations that are either towards or away from danger,
    //  depending on whether an advance is called for:
    final int direction = Spacing.compassDirection(actor.origin(), t);
    final float distance = Spacing.distance(actor.origin(), t);
    final float maxMove = ActorHealth.DEFAULT_SIGHT * actor.health.baseSpeed();
    
    float dirBonus = actor.senses.dangerFromDirection(direction);
    dirBonus *= distance * advanceFactor / maxMove;
    if (report && dirBonus > 0) I.say("    Direction bonus: "+dirBonus);
    
    return rating * Nums.clamp(1 + dirBonus, 0, 2);
  }
  
  
  private static boolean isCover(Tile t) {
    return t != null && t.blocked();
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected float getPriority() {
    float priority = PlanUtils.retreatPriority(actor);
    if (actor.senses.isEmergency()) addMotives(MOTIVE_EMERGENCY);
    else { clearMotives(); return priority; }
    maxPriority = Nums.max(maxPriority, priority);
    return maxPriority;
  }
  
  
  protected Behaviour getNextStep() {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    final boolean urgent = actor.senses.isEmergency();
    
    if (
      safePoint == null || actor.aboard() == safePoint ||
      safePoint.pathType() == Tile.PATH_BLOCKS
    ) {
      safePoint = actor.senses.haven();
    }
    if (safePoint == null) {
      interrupt(INTERRUPT_NO_PREREQ);
      return null;
    }
    
    final Target home = actor.mind.home();
    final boolean goHome = (! urgent) && home != null;
    
    final Action flees = new Action(
      actor, goHome ? home : safePoint,
      this, "actionFlee",
      urgent ? Action.MOVE_SNEAK : Action.FALL, "Fleeing to "
    );
    flees.setProperties(Action.NO_LOOP);
    
    if (report) {
      I.say("\nFleeing to "+safePoint+", urgent? "+urgent);
    }
    return flees;
  }
  
  
  public int motionType(Actor actor) {
    if (actor.senses.isEmergency()) return Action.MOTION_FAST;
    return super.motionType(actor);
  }
  
  
  public boolean actionFlee(Actor actor, Target safePoint) {
    final boolean emergency = actor.senses.isEmergency();
    //  TODO:  USE THE SIGHT-BREAKING CODE.
    
    if (! emergency) {
      final Resting rest = new Resting(actor, safePoint);
      rest.addMotives(Plan.MOTIVE_LEISURE, priorityFor(actor));
      maxPriority = 0;
      interrupt(INTERRUPT_CANCEL);
      actor.mind.assignBehaviour(rest);
      return true;
    }
    else {
      maxPriority *= DANGER_MEMORY_FADE;
      if (maxPriority < 0.5f) maxPriority = 0;
    }
    
    if (stepsVerbose && I.talkAbout == actor) {
      I.say("Max. danger: "+maxPriority);
      I.say("Still in danger? "+emergency);
    }
    return true;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (! actor.senses.isEmergency()) {
      d.append("Retiring to ");
      d.append(safePoint);
      return;
    }
    if (actor.aboard() == safePoint) d.append("Seeking refuge at ");
    else d.append("Retreating to ");
    d.append(safePoint);
  }
}




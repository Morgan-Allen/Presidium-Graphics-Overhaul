/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.maps.*;
import stratos.game.verse.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;



//  TODO:  Hiding evaluation needs to be much more reliable!  You'll have to
//  check all visible tiles, I think.  Or maybe reserve strictly for agents?
//  Yeah.  They get some special treatment.


public class Retreat extends Plan {
  
  
  /**  Constants, field definitions, constructors and save/load methods-
    */
  final static float
    DANGER_MEMORY_FADE = 0.9f,  //TODO:  use a time-limit instead?
    MIN_RETREAT_DIST = Stage.ZONE_SIZE / 2;
  
  private static boolean
    havenVerbose = false,
    stepsVerbose = false;
  
  private float    maxPriority   = 0   ;
  private Boarding safePoint     = null;
  private Target   lastHidePoint = null;
  
  
  public Retreat(Actor actor) {
    this(actor, actor.senses.haven());
  }
  
  
  public Retreat(Actor actor, Boarding safePoint) {
    super(actor, actor, MOTIVE_LEISURE, NO_HARM);
    this.safePoint = safePoint;
  }


  public Retreat(Session s) throws Exception {
    super(s);
    this.maxPriority   = s.loadFloat();
    this.safePoint     = (Boarding) s.loadObject();
    this.lastHidePoint = (Boarding) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveFloat (maxPriority  );
    s.saveObject(safePoint    );
    s.saveObject(lastHidePoint);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Retreat(other, safePoint);
  }
  
  
  
  /**  Evaluation of havens and hide points-
    */
  public static Boarding nearestHaven(
    final Actor actor, final Class prefClass, final boolean emergency
  ) {
    final boolean report = I.talkAbout == actor && havenVerbose;
    if (report) {
      I.say("\nPicking haven for "+actor+"...");
      I.say("  Currently aboard: "+actor.aboard());
      I.say("  Sight range:      "+actor.health.sightRange());
    }
    
    if (actor.mind.home() != null) return actor.mind.home();
    if (actor.mind.work() != null) return actor.mind.work();
    
    if (actor.species().sapient()) {
      final Pick <Boarding> pick = new Pick <Boarding> (0) {
        public void compare(Boarding next, float rating) {
          if (PathSearch.accessLocation(next, actor) == null) return;
          
          final float absDist = Spacing.zoneDistance(actor, next);
          float danger = actor.base().dangerMap.sampleAround(next, -1);
          rating /= 1 + Nums.max(danger, 0);
          rating /= 1 + absDist;
          super.compare(next, rating);
        }
      };
      
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
      pick.compare((Boarding) refuge, 1.5f);
      pick.compare((Boarding) pref  , 1);
      pick.compare((Boarding) cover , 1);
      if (! pick.empty()) return pick.result();
    }
    
    Tile hides = pickHidePoint(actor, actor.health.sightRange(), actor, -2);
    if (report) I.say("  hide point is: "+hides);
    
    if (hides != null) return hides;
    return actor.aboard();
  }
  
  
  
  /**  Picks a nearby tile for the actor to temporarily withdraw to (as opposed
    *  to a full-blown long-distance retreat.)  Used to perform hit-and-run
    *  tactics, stealth while travelling, or an emergency hide.
    */
  public static Tile pickHidePoint(
    final Actor actor, final float range, final Target from,
    final int advanceFactor
  ) {
    final boolean report = I.talkAbout == actor && havenVerbose;
    //
    //  The idea here is to pick tiles at random at first, then as the actor
    //  gets closer to a given area, allow systematic scanning of nearby tiles
    //  to zero in on any strong cover available.
    final Stage  world     = actor.world ();
    final Tile   at        = actor.origin();
    final Target seen   [] = actor.senses.awareOf();
    final float  threats[] = actor.senses.awareThreats();
    if (report) {
      I.say("\nPICKING POINT OF WITHDRAWAL FROM "+actor.origin());
      I.say("  Has seen: ");
      for (int i = seen.length; i-- > 0;) {
       I.say("    "+seen[i]+", threat: "+threats[i]);
      }
      I.say("  Tile ratings: ");
    }
    
    //  TODO:  You need to include the 'advance factor' for consideration here,
    //  and possibly restore tile-cover considerations.  Also, try to include
    //  fog-of-war/danger-map ratings here.
    
    final Pick <Tile> pick = new Pick <Tile> () {
      public void compare(Tile next, float rating) {
        if (next == null || rating <= 0) return;
        //if (advanceFactor == 0) rating *= 1;
        //else rating *= rateTileCover(actor, next);
        
        float threatSum = 0;
        for (int i = seen.length; i-- > 0;) {
          final float dist = Spacing.zoneDistance(next, seen[i]);
          threatSum += threats[i] / (0.5f + dist);
        }
        if (threatSum > 0) rating /= 1 + threatSum;
        else               rating *= 1 - threatSum;
        
        if (report) I.say("  Rating for "+next+" was: "+rating);
        super.compare(next, rating);
      }
    };
    //
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
    
    //final Box2D around = actor.area(null).expandBy(2);
    //for (Tile t : world.tilesIn(around, true)) pick.compare(t, 1);
    
    final Tile hides = pick.result();
    if (report) I.say("HIDING AT: "+hides);
    return hides;
  }
  
  
  private static boolean isCover(Tile t) {
    return t == null || t.blocked();
  }
  
  
  private static float rateTileCover(Actor actor, Tile at) {
    final boolean report = havenVerbose && I.talkAbout == actor;
    //
    //  Basic sanity-checks and variable-setup first:
    //  TODO:  Check to make sure the tile is reachable!
    if (at == null || at.blocked()) return 0;
    final IntelMap map = actor.base().intelMap;
    final float baseDanger = map.dangerAt(at);
    float rating = 0;
    
    final Tile allNear[] = at.allAdjacent(null);
    if (report) for (int n : TileConstants.T_INDEX) if (isCover(allNear[n])) {
      I.say("      Blocked: "+TileConstants.DIR_NAMES[n]);
    }
    //
    //  We confer a bonus to the rating if the tile in question has cover in
    //  the same direction as the actor's perceived sources of danger, while
    //  allowing clear sight to one side or the other (for easy retaliation.)
    for (int n : TileConstants.T_ADJACENT) {
      final Tile
        cover = allNear[n],
        left  = allNear[(n + 1) % 8],
        right = allNear[(n + 7) % 8];
      
      if (isCover(cover) && ! (isCover(left) && isCover(right))) {
        final float danger = map.dangerAt(cover);
        if (report && danger > 0) I.say(
          "    Cover from "+TileConstants.DIR_NAMES[n]+
          "    Danger: "+danger+" (base "+baseDanger+")"
        );
        if (danger > baseDanger) rating++;
        if (danger < baseDanger) rating--;
      }
    }
    return rating;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected float getPriority() {
    final boolean report = I.talkAbout == actor && hasBegun() && stepsVerbose;
    if (report) {
      I.say("\nGetting retreat priority for: "+actor);
    }
    
    final boolean urgent = actor.senses.isEmergency();
    final Target haven = actor.senses.haven();
    final boolean hasExit = Verse.isWorldExit(haven, actor);
    
    float priority = PlanUtils.retreatPriority(
      actor, actor.origin(), haven, true, urgent, hasExit
    );
    if (actor.senses.underAttack()) {
      toggleMotives(MOTIVE_EMERGENCY, true);
      priority = Nums.max(priority, ROUTINE);
    }
    else {
      toggleMotives(MOTIVE_EMERGENCY, false);
    }
    maxPriority = Nums.max(maxPriority, priority);
    return maxPriority;
  }
  
  
  protected Behaviour getNextStep() {
    final boolean report = I.talkAbout == actor && stepsVerbose;
    final boolean urgent = actor.senses.isEmergency();
    if (report) {
      I.say("\nFleeing to "+safePoint+", urgent? "+urgent);
    }
    
    if (
      safePoint == null           ||
      safePoint == actor.aboard() ||
      ! safePoint.allowsEntry(actor)
    ) {
      safePoint = actor.senses.haven();
      if (report) I.say("  Current haven: "+safePoint);
    }
    if (safePoint == null) {
      if (report) I.say("  No safe point found.");
      interrupt(INTERRUPT_NO_PREREQ);
      return null;
    }
    
    final Property home = actor.mind.home();
    final boolean goHome = (! urgent) && home != null;
    if (goHome) {
      safePoint = home;
      if (report) I.say("  Will go home: "+safePoint);
    }
    
    final Action flees = new Action(
      actor, safePoint,
      this, "actionFlee",
      urgent ? Action.MOVE_SNEAK : Action.FALL, "Fleeing to "
    );
    flees.setProperties(Action.NO_LOOP);
    if (report) I.say("  Will flee to "+safePoint);
    
    return flees;
  }
  
  
  public int motionType(Actor actor) {
    if (priority() >= ROUTINE) return Action.MOTION_FAST;
    return super.motionType(actor);
  }
  
  
  public boolean actionFlee(Actor actor, Target safePoint) {
    
    //  TODO:  If the actor has a mission, check that this was the boarding
    //         point!
    if (Verse.isWorldExit(safePoint, actor) && actor.senses.isEmergency()) {
      final Stage world = actor.world();
      final EntryPoints.Portal exit = (EntryPoints.Portal) safePoint;
      final Sector goes = exit.leadsTo();
      final Journey j = Journey.configAsEscape(exit, goes, world, actor);
      j.beginJourney(actor);
    }
    /*
    else if (actor.senses.fearLevel() <= 0) {
      final Resting rest = new Resting(actor, safePoint);
      rest.addMotives(Plan.MOTIVE_LEISURE, priorityFor(actor));
      maxPriority = 0;
      interrupt(INTERRUPT_CANCEL);
      actor.mind.assignBehaviour(rest);
      return true;
    }
    //*/
    else {
      if (lastHidePoint != safePoint) SenseUtils.breaksPursuit(actor, action());
      lastHidePoint = safePoint;
      maxPriority *= DANGER_MEMORY_FADE;
      if (maxPriority < 0.5f) maxPriority = 0;
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
    if (actor.aboard() == safePoint) d.append("Hiding");
    else d.appendAll("Retreating to ", safePoint);
  }
}




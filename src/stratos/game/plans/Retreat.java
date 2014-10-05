



package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.civilian.*;
import stratos.game.maps.*;
import stratos.util.*;

import static stratos.game.actors.Qualities.*;
import static stratos.game.building.Economy.*;
import org.apache.commons.math3.util.FastMath;



public class Retreat extends Plan implements Qualities {
  
  
  /**  Constants, field definitions, constructors and save/load methods-
    */
  final static float
    DANGER_MEMORY_FADE = 0.9f;
  
  private static boolean
    evalVerbose  = true ,
    havenVerbose = true ,
    stepsVerbose = true ;
  
  
  private float maxDanger = 0;
  private Boarding safePoint = null;
  
  
  public Retreat(Actor actor) {
    this(actor, nearestHaven(actor, null));
  }
  
  
  public Retreat(Actor actor, Boarding safePoint) {
    super(actor, safePoint);
    this.safePoint = safePoint;
  }


  public Retreat(Session s) throws Exception {
    super(s);
    this.maxDanger = s.loadFloat();
    this.safePoint = (Boarding) s.loadTarget();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveFloat(maxDanger);
    s.saveTarget(safePoint);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Retreat(other, safePoint);
  }
  
  
  
  /**  Evaluation of priority and targets--
    */
  //  TODO:  Possibly get rid of these?
  final Skill BASE_SKILLS[] = { ATHLETICS, STEALTH_AND_COVER };
  final Trait BASE_TRAITS[] = { NERVOUS };
  
  
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    
    final boolean emergency = actor.senses.isEndangered();
    float danger = actor.senses.fearLevel();
    if (emergency) danger += actor.health.injuryLevel();
    else maxDanger = 0;
    maxDanger = FastMath.max(danger, maxDanger);
    
    final float priority = priorityForActorWith(
      actor, safePoint, maxDanger * PARAMOUNT,
      NO_HARM, NO_COMPETITION,
      BASE_SKILLS, BASE_TRAITS,
      NO_MODIFIER, NO_DISTANCE_CHECK, NO_FAIL_RISK,
      report
    );
    
    if (report) {
      I.say("\n  PLAN ID IS: "+this.hashCode());
      
      I.say("  Max Danger: "+maxDanger);
      I.say("  Fear Level: "+actor.senses.fearLevel());
      I.say("  Base priority: "+priority);
      I.say("  Endangered? "+actor.senses.isEndangered());
    }
    if (emergency) return priority + PARAMOUNT;
    else return priority;
    
    //  TODO:  Consider re-introducing the code below-
    /*
    //  Make retreat more attractive the further you are from home, and the
    //  more dangerous the area is-
    float
      danger = CombatUtils.dangerAtSpot(actor.origin(), actor, null),
      distFactor = 0, nightVal = 1f - Planet.dayValue(actor.world());
    maxDanger = FastMath.max(danger, maxDanger);
    
    if (rateHaven(safePoint, actor, null) > 1) {
      distFactor = Plan.rangePenalty(actor, safePoint);
      distFactor *= (2 + actor.traits.relativeLevel(NERVOUS)) / 2f;
    }
    //*/
  }
  
  
  public boolean finished() {
    if (super.finished()) return true;
    if (hasBegun() && ! actor.isDoing(Retreat.class, null)) return true;
    return false;
  }
  
  
  public static Boarding nearestHaven(Actor actor, Class prefClass) {
    
    //  TODO:  Increase the range of options here.
    
    return (Tile) pickWithdrawPoint(
      actor, actor.health.sightRange() + World.SECTOR_SIZE, actor, false
    );
  }
  
  
  
  /**  Picks a nearby tile for the actor to temporarily withdraw to (as opposed
    *  to a full-blown long-distance retreat.)  Used to perform hit-and-run
    *  tactics, stealth while travelling, or an emergency hide.
    */
  public static Target pickWithdrawPoint(
    Actor actor, float range, Target from, boolean advance
  ) {
    final boolean report = havenVerbose && I.talkAbout == actor;
    
    final World world = actor.world();
    final Tile at = actor.origin();
    Target pick = Spacing.pickRandomTile(actor, range, world);
    float bestRating = 0;
    if (report) I.say("\nPICKING POINT OF WITHDRAWAL FROM "+at);
    
    for (int n : TileConstants.N_ADJACENT) {
      final Tile t = world.tileAt(
        at.x + (TileConstants.N_X[n] + Rand.num() - 0.5f) * range,
        at.y + (TileConstants.N_Y[n] + Rand.num() - 0.5f) * range
      );
      if (t == null) continue;
      
      float rating = 0;
      for (Target s : actor.senses.awareOf()) {
        if (! CombatUtils.isHostileTo(actor, s)) continue;
        final float distance = Spacing.distance(t, s);
        final float threat = CombatUtils.powerLevelRelative(actor, (Actor) s);
        rating += distance * threat;
        if (report) {
          I.say("  THREAT FROM "+s+" IS "+threat+", DISTANCE "+distance);
        }
      }
      
      rating /= range + Spacing.distance(t, actor);
      rating *= advance ? -1 : 1;
      if (report) I.say("  RATING FOR "+t+" IS "+rating);
      if (rating > bestRating) { pick = t; bestRating = rating; }
    }
    return pick;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    final boolean urgent = urgent();
    
    if (
      safePoint == null || actor.aboard() == safePoint ||
      safePoint.pathType() == Tile.PATH_BLOCKS
    ) {
      safePoint = nearestHaven(actor, null);
    }
    if (safePoint == null) {
      abortBehaviour();
      return null;
    }
    
    final Target home = actor.mind.home();
    final boolean goHome = (! urgent) && home != null;// (rateHaven(home, actor, null) > 0);
    
    final Action flees = new Action(
      actor, goHome ? home : safePoint,
      this, "actionFlee",
      Action.MOVE_SNEAK, "Fleeing to "
    );
    if (urgent) flees.setProperties(Action.QUICK);
    
    if (report) {
      I.say("\nFleeing to "+safePoint+", urgent? "+urgent);
    }
    return flees;
  }
  
  
  private boolean urgent() {
    return priorityFor(actor) >= ROUTINE;
  }
  
  
  public boolean actionFlee(Actor actor, Target safePoint) {
    
    if (actor.indoors() && ! urgent()) {
      final Resting rest = new Resting(actor, safePoint);
      rest.setMotive(Plan.MOTIVE_LEISURE, priorityFor(actor));
      maxDanger = 0;
      abortBehaviour();
      return true;
    }
    
    if (stepsVerbose && I.talkAbout == actor) {
      I.say("Max. danger: "+maxDanger);
      I.say("Still in danger? "+actor.senses.isEndangered());
    }
    
    if (maxDanger < 0.5f || ! actor.senses.isEndangered()) {
      maxDanger = 0;
    }
    else {
      maxDanger *= DANGER_MEMORY_FADE;
    }
    return true;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (! urgent()) {
      d.append("Retiring to safety");
      return;
    }
    if (actor.aboard() == safePoint) d.append("Seeking refuge at ");
    else d.append("Retreating to ");
    d.append(safePoint);
  }
}







/*
final boolean report = havenVerbose && I.talkAbout == actor;

final Presences presences = actor.world().presences;

final Batch <Target> considered = new Batch <Target> ();

considered.add(presences.nearestMatch(Venue.class, actor, -1));
considered.add(presences.nearestMatch(Economy.SERVICE_REFUGE, actor, -1));
considered.add(pickWithdrawPoint(
  actor, actor.health.sightRange() + World.SECTOR_SIZE, actor, false
));
considered.add(actor.aboard());
considered.add(actor.mind.home());
for (Target e : actor.senses.awareOf()) considered.add(e);

Object picked = null;
float bestRating = 0;
for (Target e : considered) {
  final float rating = rateHaven(e, actor, prefClass);
  if (report) I.say("  Rating for "+e+" is "+rating);
  if (rating > bestRating) { bestRating = rating; picked = e; }
}

if (report) I.say("Haven picked is: "+picked);
return (Boarding) picked;


/*
private static float rateHaven(Object t, Actor actor, Class prefClass) {
final boolean report = havenVerbose && I.talkAbout == actor;

if (! (t instanceof Boarding)) return -1;
if (! (t instanceof Venue)) return 1;

final Venue haven = (Venue) t;
if (haven.mainEntrance() == null) return -1;
if (! haven.structure.intact()) return -1;
if (! haven.allowsEntry(actor)) return -1;

float rating = 1;
if (prefClass != null && haven.getClass() == prefClass) rating *= 2;
if (haven.base() == actor.base()) rating *= 2;
if (haven == actor.mind.home()) rating *= 2;
if (haven == actor.aboard()) rating *= 2;

rating *= haven.structure.maxIntegrity() / 50f;
final int SS = World.SECTOR_SIZE;
rating *= SS / (SS + Spacing.distance(actor, haven));

final Tile o = actor.world().tileAt(haven);
rating /= 1 + actor.base().dangerMap.sampleAt(o.x, o.y);

return rating;
}
//*/




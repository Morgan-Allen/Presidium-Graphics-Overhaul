



package stratos.game.tactical ;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.civilian.*;
import stratos.game.maps.*;
import stratos.util.*;

import org.apache.commons.math3.util.FastMath;



public class Retreat extends Plan implements Qualities {
  
  
  /**  Constants, field definitions, constructors and save/load methods-
    */
  final static float
    DANGER_MEMORY_FADE = 0.9f;
  
  private static boolean
    evalVerbose  = false,
    havenVerbose = false,
    stepsVerbose = false;
  
  
  private float maxDanger = 0;
  private Boardable safePoint = null ;
  
  
  public Retreat(Actor actor) {
    this(actor, nearestHaven(actor, null)) ;
  }
  
  
  public Retreat(Actor actor, Boardable safePoint) {
    super(actor, safePoint) ;
    this.safePoint = safePoint ;
  }


  public Retreat(Session s) throws Exception {
    super(s) ;
    this.maxDanger = s.loadFloat();
    this.safePoint = (Boardable) s.loadTarget() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveFloat(maxDanger);
    s.saveTarget(safePoint) ;
  }
  
  
  
  /**  Evaluation of priority and targets--
    */
  //  TODO:  Possibly get rid of these?
  final Skill BASE_SKILLS[] = { };  //  TODO:  Include speed in calculation
  final Trait BASE_TRAITS[] = { NERVOUS };
  
  
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    
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
    
    final float priority = priorityForActorWith(
      actor, safePoint, distFactor + nightVal + (maxDanger * PARAMOUNT),
      NO_HARM, NO_COMPETITION,
      BASE_SKILLS, BASE_TRAITS,
      NO_MODIFIER, NO_DISTANCE_CHECK, NO_FAIL_RISK,
      report
    );
    
    if (report) {
      I.say("  Safe point for retreat is: "+safePoint);
      I.say("  Current/max danger: "+danger+"/"+maxDanger);
      I.say("  Distance factor: "+distFactor);
      I.say("  Retreat priority is: "+priority);
    }
    return priority;
  }
  
  
  public boolean finished() {
    if (super.finished()) return true;
    if (hasBegun() && ! actor.isDoing(Retreat.class, null)) return true;
    return false;
  }
  
  
  public static Boardable nearestHaven(Actor actor, Class prefClass) {
    final boolean report = havenVerbose && I.talkAbout == actor;
    
    final Batch <Target> considered = new Batch();
    considered.add(actor.world().presences.nearestMatch(
      Economy.SERVICE_REFUGE, actor, -1
    ));
    considered.add(pickWithdrawPoint(
      actor, actor.health.sightRange() + World.SECTOR_SIZE, actor, false
    ));
    considered.add(actor.aboard());
    considered.add(actor.mind.home());
    for (Target e : actor.senses.awareOf()) considered.add(e);
    
    Object picked = null;
    float bestRating = 0;
    for (Target e : considered) {
      final float rating = rateHaven(e, actor, prefClass) ;
      if (report) I.say("  Rating for "+e+" is "+rating);
      if (rating > bestRating) { bestRating = rating ; picked = e ; }
    }
    
    if (report) I.say("Haven picked is: "+picked);
    return (Boardable) picked ;
  }
  
  
  private static float rateHaven(Object t, Actor actor, Class prefClass) {
    final boolean report = havenVerbose && I.talkAbout == actor;
    
    if (! (t instanceof Boardable)) return -1 ;
    if (! (t instanceof Venue)) return 1 ;
    
    final Venue haven = (Venue) t ;
    if (! haven.structure.intact()) return -1 ;
    if (! haven.allowsEntry(actor)) return -1 ;
    
    float rating = 1 ;
    if (prefClass != null && haven.getClass() == prefClass) rating *= 2 ;
    if (haven.base() == actor.base()) rating *= 2 ;
    if (haven == actor.mind.home()) rating *= 2 ;
    if (haven == actor.aboard()) rating *= 2 ;
    
    rating *= haven.structure.maxIntegrity() / 50f;
    final int SS = World.SECTOR_SIZE;
    rating *= SS / (SS + Spacing.distance(actor, haven));
    
    final Tile o = actor.world().tileAt(haven);
    rating /= 1 + actor.base().dangerMap.sampleAt(o.x, o.y);
    
    return rating;
  }
  
  
  
  /**  Picks a nearby tile for the actor to temporarily withdraw to (as opposed
    *  to a full-blown long-distance retreat.)  Used to perform hit-and-run
    *  tactics, stealth while travelling, or an emergency hide.
    */
  public static Target pickWithdrawPoint(
    Actor actor, float range, Target from, boolean advance
  ) {
    final boolean report = havenVerbose && I.talkAbout == actor;
    
    final Tile o = from == null ? actor.origin() : actor.world().tileAt(from);
    final Vec2D off = new Vec2D();
    final float salt = Rand.num();
    
    final Series <Target> seen = actor.senses.awareOf();
    final float threats[] = new float[seen.size()];
    
    int i = 0; for (Target s : seen) {
      threats[i++] = CombatUtils.threatTo(actor, s, 0, report);
    }

    Target pick = null;
    float bestRating = Float.NEGATIVE_INFINITY;
    
    for (int n = 8; n-- > 0;) {
      off.setFromAngle(((n + salt) * 360 / 8f) % 360);
      off.scale(range * Rand.avgNums(2));
      
      Tile under = actor.world().tileAt(o.x + off.x, o.y + off.y);
      under = Spacing.nearestOpenTile(under, actor);
      if (under == null) continue;
      
      float rating = 0;
      i = 0; for (Target s : seen) {
        final float distance = Spacing.distance(from, s);
        rating -= threats[i++] / (1 + distance);
      }
      rating /= 1 + Spacing.distance(under, actor);
      rating *= advance ? -1 : 1;
      
      if (rating > bestRating) { pick = under; bestRating = rating; }
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
      safePoint = nearestHaven(actor, null) ;
    }
    if (safePoint == null) {
      abortBehaviour() ;
      return null ;
    }
    
    final Target home = actor.mind.home();
    final boolean goHome = (! urgent) && (rateHaven(home, actor, null) > 0);
    
    final Action flees = new Action(
      actor, goHome ? home : safePoint,
      this, "actionFlee",
      Action.MOVE_SNEAK, "Fleeing to "
    );
    if (urgent) flees.setProperties(Action.QUICK);
    if (report) I.say("Fleeing to... "+safePoint);
    return flees ;
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
    maxDanger *= DANGER_MEMORY_FADE;
    if (maxDanger < 0.5f) maxDanger = 0;
    return true ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (! urgent()) {
      d.append("Retiring to safety");
      return;
    }
    if (actor.aboard() == safePoint) d.append("Seeking refuge at ") ;
    else d.append("Retreating to ") ;
    d.append(safePoint) ;
  }
}










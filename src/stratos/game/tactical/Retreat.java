



package stratos.game.tactical ;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.civilian.*;
import stratos.game.planet.*;
import stratos.util.*;
import org.apache.commons.math3.util.FastMath;



public class Retreat extends Plan implements Qualities {
  
  
  /**  Constants, field definitions, constructors and save/load methods-
    */
  final static float
    DANGER_MEMORY_FADE = 0.9f;
  
  private static boolean
    evalVerbose  = true,
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
      distFactor = 1 + Plan.rangePenalty(actor, safePoint);
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
  
  
  public static Boardable nearestHaven(Actor actor, Class prefClass) {
    final boolean report = havenVerbose && I.talkAbout == actor;
    
    final Batch <Target> considered = new Batch();
    considered.add(actor.world().presences.nearestMatch(
      Economy.SERVICE_REFUGE, actor, -1
    ));
    considered.add(pickWithdrawPoint(
      actor, actor.health.sightRange() + World.SECTOR_SIZE, actor, 0.1f
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
    final int SS = World.SECTOR_SIZE ;
    rating *= SS / (SS + Spacing.distance(actor, haven)) ;
    
    final Tile o = actor.world().tileAt(haven);
    rating /= 1 + actor.base().dangerMap.sampleAt(o.x, o.y);
    
    return rating ;
  }
  
  
  public static Target pickWithdrawPoint(
    Actor actor, float range,
    Target target, float salt
  ) {
    final boolean report = havenVerbose && I.talkAbout == actor;
    final int numPicks = 3 ;  // TODO:  Make this an argument instead of range?
    Target pick = null ;
    float bestRating = salt > 0 ?
      Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY ;
    
    for (int i = numPicks ; i-- > 0 ;) {
      //  TODO:  Check by compass-point directions instead of purely at random?
      Tile tried = Spacing.pickRandomTile(actor, range, actor.world()) ;
      if (tried == null) continue ;
      tried = Spacing.nearestOpenTile(tried, target) ;
      if (tried == null || Spacing.distance(tried, target) > range) continue ;
      
      //  TODO:  Just use general danger-map readings, and sample more spots.
      float tryRating = CombatUtils.dangerAtSpot(tried, actor, null);
      tryRating += (Rand.num() - 0.5f) * salt ;
      if (salt < 0) tryRating *= -1 ;
      if (tryRating < bestRating) { bestRating = tryRating ; pick = tried ; }
    }
    return pick ;
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
    final Action flees = new Action(
      actor, (home != null && ! urgent) ? home : safePoint,
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
      actor.mind.assignBehaviour(rest);
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










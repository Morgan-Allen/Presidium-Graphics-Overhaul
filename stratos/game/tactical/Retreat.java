



package stratos.game.tactical ;
import org.apache.commons.math3.util.FastMath;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.util.*;



public class Retreat extends Plan implements Qualities {
  
  
  
  /**  Constants, field definitions, constructors and save/load methods-
    */
  final static float
    DANGER_MEMORY_FADE = 0.9f;
  
  static boolean verbose = true, havenVerbose = false;
  
  
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
  final Skill BASE_SKILLS[] = { ATHLETICS };
  final Trait BASE_TRAITS[] = { NERVOUS };
  
  
  public float priorityFor(Actor actor) {
    final boolean report = verbose && I.talkAbout == actor;
    float danger = CombatUtils.dangerAtSpot(actor.origin(), actor, null);
    danger *= 1 + actor.traits.relativeLevel(NERVOUS);
    maxDanger = FastMath.max(danger, maxDanger);
    
    if (maxDanger <= 0) {
      if (report) I.say("NO DANGER!");
      return 0;
    }
    float safety = (0.5f - maxDanger) * ROUTINE;
    //final float safety = (0.5f - maxDanger) * 2 * ROUTINE;
    
    final float priority = priorityForActorWith(
      actor, safePoint, PARAMOUNT,
      NO_HARM, NO_COMPETITION,
      BASE_SKILLS, BASE_TRAITS,
      0 - safety, NO_DISTANCE_CHECK, NO_DANGER,
      report
    );
    
    if (report) {
      I.say("  Current/max danger: "+danger+"/"+maxDanger);
      I.say("  Safety rating: "+safety);
      I.say("  Retreat priority is: "+priority);
    }
    return priority;
  }
  
  
  public static Boardable nearestHaven(Actor actor, Class prefClass) {
    final boolean report = havenVerbose && I.talkAbout == actor;
    Object picked = pickWithdrawPoint(
      actor, actor.health.sightRange() + World.SECTOR_SIZE, actor, 0.1f
    ) ;
    float bestRating = rateHaven(picked, actor, prefClass);
    
    for (Target e : actor.senses.awareOf()) {
      final float rating = rateHaven(e, actor, prefClass) ;
      if (report) I.say("  Rating for "+e+" is "+rating);
      if (rating > bestRating) { bestRating = rating ; picked = e ; }
    }
    if (report) I.say("Haven picked is: "+picked);
    return (Boardable) picked ;
  }
  
  
  private static float rateHaven(Object t, Actor actor, Class prefClass) {
    final boolean report = havenVerbose && I.talkAbout == actor;
    //
    //  TODO:  Don't pick anything too close by either.  That'll be in a
    //  dangerous area.
    if (! (t instanceof Boardable)) return -1 ;
    if (! (t instanceof Venue)) return 1 ;
    final Venue haven = (Venue) t ;
    if (! haven.structure.intact()) return -1 ;
    if (! haven.allowsEntry(actor)) return -1 ;
    float rating = 1 ;
    if (prefClass != null && haven.getClass() == prefClass) rating *= 2 ;
    if (haven.base() == actor.base()) rating *= 2 ;
    if (haven == actor.mind.home()) rating *= 2 ;
    final int SS = World.SECTOR_SIZE ;
    rating *= SS / (SS + Spacing.distance(actor, haven)) ;
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
    final boolean report = verbose && I.talkAbout == actor;
    if (
      safePoint == null || actor.aboard() == safePoint ||
      safePoint.pathType() == Tile.PATH_BLOCKS
    ) {
      safePoint = nearestHaven(actor, null) ;
      if (report) I.say("Safe point is: "+safePoint);
    }
    if (safePoint == null) {
      abortBehaviour() ;
      return null ;
    }
    final Action flees = new Action(
      actor, safePoint,
      this, "actionFlee",
      Action.MOVE_SNEAK, "Fleeing to "
    );
    flees.setProperties(Action.QUICK);
    if (report) I.say("Fleeing to... "+safePoint);
    return flees ;
  }
  
  
  public boolean actionFlee(Actor actor, Target safePoint) {
    maxDanger *= DANGER_MEMORY_FADE;
    if (maxDanger < 0.5f) maxDanger = 0;
    return true ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (actor.aboard() == safePoint) d.append("Seeking refuge at ") ;
    else d.append("Retreating to ") ;
    d.append(safePoint) ;
  }
}




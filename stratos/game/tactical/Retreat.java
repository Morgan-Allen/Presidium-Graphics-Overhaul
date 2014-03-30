



package stratos.game.tactical ;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.game.planet.*;
import stratos.user.*;
import stratos.util.*;



public class Retreat extends Plan implements Abilities {
  
  
  
  /**  Constants, field definitions, constructors and save/load methods-
    */
  static boolean verbose = false ;
  
  private Boardable safePoint = null ;
  
  
  public Retreat(Actor actor) {
    super(actor) ;
  }
  
  
  public Retreat(Actor actor, Boardable safePoint) {
    super(actor) ;
    this.safePoint = safePoint ;
  }


  public Retreat(Session s) throws Exception {
    super(s) ;
    this.safePoint = (Boardable) s.loadTarget() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveTarget(safePoint) ;
  }
  
  
  
  /**  Evaluation of priority and targets--
    */
  public float priorityFor(Actor actor) {
    float danger = dangerAtSpot(
      actor.origin(), actor, null, actor.mind.awareOf()
    ) ;
    danger += priorityMod / ROUTINE ;
    if (danger <= 0) return 0 ;
    danger *= actor.traits.scaleLevel(NERVOUS) ;
    
    if (verbose && I.talkAbout == actor) {
      I.say("Perceived danger: "+danger) ;
    }
    return danger * PARAMOUNT ;
  }
  
  
  protected static float dangerAtSpot(
    Target spot, Actor actor, Actor enemy
  ) {
    
    final float range = actor.health.sightRange() ;
    final World world = actor.world() ;
    //
    //  TODO:  Blend values from the danger map?
    if (Spacing.distance(actor, spot) < range * 2) {
      return dangerAtSpot(spot, actor, enemy, actor.mind.awareOf()) ;
    }
    
    final Batch <Element> seen = new Batch <Element> () ;
    for (Object o : world.presences.matchesNear(Mobile.class, spot, range)) {
      final Mobile m = (Mobile) o ;
      if (! m.visibleTo(actor.base())) continue ;
      seen.add(m) ;
    }
    return dangerAtSpot(spot, actor, enemy, seen) ;
  }
  
  
  private static float dangerAtSpot(
    Target spot, Actor actor, Actor enemy, Batch <Element> seen
  ) {
    if (spot == null) return 0 ;
    final boolean report = verbose && I.talkAbout == actor ;
    
    final float basePower = Combat.combatStrength(actor, enemy) ;
    float sumAllies = basePower, sumEnemies = 0, sumTargeting = 0 ;
    
    //
    //  TODO:  Blend values from the danger map?
    
    for (Element m : seen) {
      if (m == actor || ! (m instanceof Actor)) continue ;
      final Actor near = (Actor) m ;
      if (near.indoors() || ! near.health.conscious()) continue ;

      final Target victim = near.targetFor(Combat.class) ;
      final float relation = near.mind.relationValue(actor) ;
      final float power = Combat.combatStrength(near, enemy) ;
      if (relation > 0) {
        sumAllies += power ;
      }
      if (relation < 0) {
        sumEnemies += power ;
      }
      if (victim == actor) sumTargeting += power ;
    }
    
    final float
      injury = actor.health.injuryLevel(),
      stress = actor.health.stressPenalty() ;
    
    if (report) {
      I.say("  Sum allied/enemy strength: "+sumAllies+" / "+sumEnemies) ;
      I.say("  Sum targetting: "+sumTargeting) ;
      I.say("  Injury & stress: "+injury+" / "+stress) ;
    }
    
    float danger = (
      (1 - (basePower / (basePower + sumTargeting))) +
      (sumEnemies / (sumEnemies + sumAllies))
    ) / 2.0f ;
    danger += Visit.clamp(injury + stress - 0.5f, 0, 1) ;
    danger *= 1 + injury + stress ;
    
    if (report) I.say("  Danger estimate: "+danger) ;
    return danger ;
  }
  
  
  public static Boardable nearestHaven(Actor actor, Class prefClass) {
    Object picked = null ;
    float bestRating = 0 ;
    
    for (Element e : actor.mind.awareOf()) {
      final float rating = rateHaven(e, actor, prefClass) ;
      if (rating > bestRating) { bestRating = rating ; picked = e ; }
    }
    if (picked == null) picked = pickWithdrawPoint(
      actor, actor.health.sightRange(), actor, 0.1f
    ) ;
    if (verbose) I.sayAbout(actor, "Haven picked is: "+picked) ;
    return (Boardable) picked ;
  }
  
  
  private static float rateHaven(Object t, Actor actor, Class prefClass) {
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
      
      //  TODO:  Have danger-map sampling built into dangerAtSpot().
      float tryRating = actor.base().dangerMap.sampleAt(tried.x, tried.y) ;
      tryRating = dangerAtSpot(tried, actor, null, actor.mind.awareOf()) ;
      tryRating /= 2 ;
      tryRating += (Rand.num() - 0.5f) * salt ;
      if (salt < 0) tryRating *= -1 ;
      if (tryRating < bestRating) { bestRating = tryRating ; pick = tried ; }
    }
    return pick ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    if (
      safePoint == null || actor.aboard() == safePoint ||
      safePoint.pathType() == Tile.PATH_BLOCKS
    ) {
      safePoint = nearestHaven(actor, null) ;
      priorityMod *= 0.5f ;
      if (priorityMod < 0.25f) priorityMod = 0 ;
    }
    if (safePoint == null) {
      abortBehaviour() ;
      return null ;
    }
    final Action flees = new Action(
      actor, safePoint,
      this, "actionFlee",
      Action.LOOK, "Fleeing to "
    ) ;
    flees.setProperties(Action.QUICK) ;
    return flees ;
  }
  
  
  public boolean actionFlee(Actor actor, Target safePoint) {
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







/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.util.I;
import stratos.util.Nums;



//  Okay.  Here, I'm going to make an effort to replace the various plan-
//  priority evaluation methods with something simpler.

public class PlanUtils {
  
  
  private static boolean
    verbose = true ;
  
  private static boolean reportOn(Actor a) {
    return I.talkAbout == a && verbose;
  }
  
  
  /**  Combat-priority.  Should range from 0 to 30.
    */
  public static float combatPriority(
    Actor actor, Target subject, float rewardBonus
  ) {
    float incentive = 0, empathy = 0, winChance = 0, priority = 0;
    
    incentive += rewardBonus;
    incentive -= actor.relations.valueFor(subject) * 5;
    incentive += harmIntendedBy(subject, actor, false) * 5;
    if (incentive <= 0) return -1;
    
    empathy = actor.traits.relativeLevel(Qualities.EMPATHIC) * 10;
    if (incentive < empathy) return -1;
    
    if (! isArmed(actor)) incentive -= 10;
    else if (actor.senses.isEmergency()) incentive += 10;
    
    winChance = combatWinChance(actor, subject);
    priority = incentive * winChance;
    
    if (reportOn(actor) && priority > 0) I.reportVars(
      "\nCombat priority for "+actor, "  ",
      "subject  ", subject,
      "reward   ", rewardBonus,
      "incentive", incentive,
      "empathy  ", empathy,
      "winChance", winChance,
      "priority ", priority
    );
    return priority;
  }
  
  
  /**  Retreat priority.  Should range from 0 to 30.
    */
  public static float retreatPriority(
    Actor actor
  ) {
    float incentive = 0, loseChance = 0, priority = 0;
    float homeDistance = 0, escapeChance = 0;
    
    loseChance = 1f - combatWinChance(actor, actor.origin());
    incentive += loseChance * 10;
    
    homeDistance = homeDistanceFactor(actor, actor.origin());
    if (! isArmed(actor)) {
      homeDistance = (homeDistance + 2) / 2;
      if (actor.senses.isEmergency()) incentive += 10;
    }
    
    escapeChance = 1f - actor.health.fatigueLevel();
    //  TODO:  If under fire, reduce effective speed
    
    priority = incentive * homeDistance * Nums.clamp(escapeChance, 0, 1);
    
    if (reportOn(actor) && priority > 0) I.reportVars(
      "\nRetreat priority for "+actor, "  ",
      "incentive   ", incentive,
      "loseChance  ", loseChance,
      "homeDistance", homeDistance,
      "escapeChance", escapeChance,
      "priority    ", priority
    );
    return priority;
  }
  
  
  
  /**  Combat-related utility methods.
    */
  public static boolean isArmed(Actor actor) {
    final DeviceType type = actor.gear.deviceType();
    final float baseDamage = actor.gear.baseDamage();
    return baseDamage > 0 || ((type != null) && type.baseDamage > 0);
  }
  
  
  public static float combatWinChance(Actor actor, Target around) {
    float fearLevel = actor.senses.fearLevel ();
    float strength  = actor.senses.powerLevel();
    float health = 1f - actor.health.injuryLevel();
    
    Tile at = actor.world().tileAt(around);
    float danger = actor.base().dangerMap.sampleAround(
      at.x, at.y, Stage.SECTOR_SIZE
    );
    
    if (fearLevel == 0) danger = 0;
    else danger = Nums.max(fearLevel, danger / strength);
    
    
    float chance = Nums.clamp(health * (1 - danger), 0, 1);
    
    if (around instanceof Actor) {
      float power = CombatUtils.powerLevelRelative(actor, (Actor) around);
      chance -= Nums.clamp(1f - power, 0, 1);
    }
    
    chance *= 1 + actor.traits.relativeLevel(Qualities.FEARLESS);
    return Nums.clamp(chance, 0, 1);
  }
  
  
  public static float homeDistanceFactor(Actor actor, Target around) {
    final Stage world = actor.world();
    float baseMult = 1;
    
    final Tile at = world.tileAt(around);
    final Base owns = world.claims.baseClaiming(at);
    if (owns != null) baseMult -= owns.relations.relationWith(actor.base());
    
    return Nums.clamp(baseMult, 0, 2);
  }
  
  
  public static float harmIntendedBy(
    Target near, Actor by, boolean asCombat
  ) {
    if (near instanceof Actor) {
      final Actor other = (Actor) near;
      final Target victim = asCombat ?
        other.planFocus(Combat.class, true) :
        other.actionFocus()
      ;
      if (victim != null) {
        float protectUrge = by.relations.valueFor(victim);
        return other.harmIntended(victim) * protectUrge;
      }
    }
    if (near instanceof Venue) {
    }
    return 0;
  }
  
  
  
  
  
  //  TODO:  Have a general 'isAgent' decision-check?
  
  //  TODO:  Use a 'raise-alarm' system to ensure combat-specific agents can
  //         arrive on the scene faster/more reliably- or have Retreat morph
  //         into a 'run for help' behaviour.
  
  //  TODO:  Allow for hostile venues.

  //  TODO:  Switch between these two evaluation methods based on
  //  intelligence?  (Or maybe the battle-tactics skill?)
  /*
  final boolean report = evalVerbose && I.talkAbout == actor;
  
  if (other instanceof Actor) {
    final Actor struck = (Actor) other;
    float chance = CombatUtils.powerLevelRelative(actor, struck) / 2f;
    chance = (chance + 1 - actor.senses.fearLevel()) / 2f;
    return Visit.clamp(chance, 0, 1);
  }
  else return 1;
  //*/
  
  //  TODO:  You're going to need an ultra-fast pathability-check, including
  //  the level of ambient dangers en-route, in order to assign proper weight
  //  to long-distance plans.
  
}











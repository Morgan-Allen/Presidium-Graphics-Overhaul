/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.util.*;



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
    priority  = incentive * winChance;
    
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
  
  
  
  /**  Dialogue priority.  Should range from 0 to 30.
    */
  public static float dialoguePriority(
    Actor actor, Actor subject, float rewardBonus
  ) {
    float liking = 0, novelty = 0, solitude = 0, commChance = 0;
    float harmIntended = 0, baseNovelty = 0;
    float chatIncentive = 0, pleaIncentive = 0, priority = 0;
    
    final Relation r = actor.relations.relationWith(subject);
    liking  = r == null ? 0 : r.value  ();
    novelty = r == null ? 1 : r.novelty();
    solitude = actor.motives.solitude();
    harmIntended = Nums.clamp(harmIntendedBy(subject, actor, true), 0, 1);
    baseNovelty  = actor.relations.noveltyFor(subject.base());
    
    chatIncentive += (liking * 1) + ((novelty * 4) * (1 + liking) / 2);
    if (r == null) chatIncentive *= solitude;
    
    pleaIncentive = (harmIntended * 10) + (baseNovelty * 10);
    if (pleaIncentive > 0) pleaIncentive += 5;
    
    priority = (chatIncentive + pleaIncentive + rewardBonus) * commChance;

    if (reportOn(actor) && priority > 0) I.reportVars(
      "\nRetreat priority for "+actor, "  ",
      "subject"      , subject,
      "reward"       , rewardBonus,
      "liking"       , liking,
      "novelty"      , novelty,
      "solitude"     , solitude,
      "commChance"   , commChance,
      "harmIntended" , harmIntended,
      "baseNovelty"  , baseNovelty,
      "chatIncentive", chatIncentive,
      "pleaIncentive", pleaIncentive,
      "priority"     , priority
    );
    return priority;
  }
  
  
  
  /**  Assistance priority.  Should range from 0 to 30.
    */
  public static float supportPriority(
    Actor actor, Element subject, float rewardBonus,
    float supportChance, float subjectDanger
  ) {
    float incentive = 0, liking = 0, priority = 0;
    
    liking = actor.relations.valueFor(subject);
    incentive = (liking * 10) * subjectDanger * 2;
    incentive += rewardBonus;
    priority = incentive * supportChance;
    
    if (reportOn(actor) && priority > 0) I.reportVars(
      "\nSupport priority for "+actor, "  ",
      "subject"      , subject,
      "reward"       , rewardBonus,
      "supportChance", supportChance,
      "subjectDanger", subjectDanger,
      "liking"       , liking,
      "incentive"    , incentive,
      "priority"     , priority
    );
    return priority;
  }
  
  
  
  /**  Exploring priority- should range from 0 to 20.
    */
  public static float explorePriority(
    Actor actor, Target surveyed, float rewardBonus
  ) {
    float incentive = 0, novelty = 0, priority = 0, exploreChance = 0;
    
    novelty = Nums.clamp(Nums.max(
      (1 - actor.base().intelMap.fogAt(surveyed)),
      (actor.relations.noveltyFor(surveyed) - 1)
    ), 0, 1);
    
    exploreChance = actor.health.baseSpeed();
    exploreChance *= 2 / (1 + homeDistanceFactor(actor, surveyed));
    
    incentive = (novelty * 5) + rewardBonus;
    incentive *= 1 + actor.traits.relativeLevel(Qualities.CURIOUS);
    
    priority = incentive * exploreChance;
    return priority;
  }
  
  
  
  /**  Ambition priority- should range from 0 to 10.
    */
  public static float ambitionPriority(
    Actor actor, Background position, Venue at, float quality
  ) {
    final boolean report = reportOn(actor);
    float teamBonus = 0, crowding = 0, priority = 0;
    float locale = 0, ambience = 0, safety = 0, loyalty = 0;
    
    ambience = Nums.clamp(at.world().ecology().ambience.valueAt(at) / 10, 0, 1);
    safety   = Nums.clamp(at.base().dangerMap.sampleAround(at, -1) / -10, 0, 1);
    loyalty  = actor.relations.valueFor(at.base());
    locale   = ((1 + ambience) * (1 + safety) * (1 + loyalty)) / 4;
    
    crowding = at.crowdRating(actor, position);
    
    teamBonus = occupantRelations(actor, at) * (1 + crowding);
    
    priority = 5 * (quality + teamBonus + locale) / 3;
    if (! at.staff.doesBelong(actor)) priority *= (1 - crowding);
    
    if (report && priority > 0) I.reportVars(
      "\nSupport priority for "+actor, "  ",
      "position" , position,
      "at"       , at,
      "quality"  , quality,
      "ambience" , ambience,
      "safety"   , safety,
      "loyalty"  , loyalty,
      "locale"   , locale,
      "teamBonus", teamBonus,
      "crowding" , crowding,
      "priority" , priority
    );
    return priority;
  }
  
  
  public static float occupantRelations(Actor actor, Venue at) {
    float sumR = 0, sumW = 0;
    for (Actor a : at.staff.workers()) {
      sumR += actor.relations.valueFor(a);
      sumW++;
    }
    for (Actor a : at.staff.residents()) {
      sumR += actor.relations.valueFor(a);
      sumW++;
    }
    if (sumW == 0) return 0;
    return sumR / sumW;
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
  
  //  TODO:  Move ambience to the Stage and salary to the Staff class.
  
  //  TODO:  Move home/work-ambitions to the Motives class.
  
  //  TODO:  Modify various checks based on intelligence!
  
  //  TODO:  Use a 'raise-alarm' system to ensure combat-specific agents can
  //         arrive on the scene faster/more reliably- or have Retreat morph
  //         into a 'run for help' behaviour.
  
  //  TODO:  If under fire, modify effective escape-chance based on speed.
  
  //  TODO:  Allow for hostile venues.
  
  //  TODO:  Use this for ambitions-evaluation?
  /*
  if (Visit.arrayIncludes(at.careers(), position)) {
    //quality = at.base().profiles.profileFor(actor).salary() / position.defaultSalary;
    quality += Career.ratePromotion(position, actor, report) * 2;
  }
  else if (position == Backgrounds.AS_RESIDENT) {
    quality = at.structure.mainUpgradeLevel() * 2 / 5f;
  }
  else I.complain(at+" DOES NOT ALLOW FOR POSITION: "+position);
  //*/

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











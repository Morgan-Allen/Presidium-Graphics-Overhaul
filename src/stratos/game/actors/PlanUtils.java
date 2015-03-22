/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.game.wild.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;



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
    Actor actor, Target subject,
    float rewardBonus, int teamSize, boolean asRealTask
  ) {
    float incentive = 0, winChance, inhibition, priority;
    float harmDone, dislike, wierdness, conscience;
    final boolean report = reportOn(actor);
    
    incentive += rewardBonus;
    incentive += dislike   = actor.relations.valueFor(subject)     * -5;
    incentive += harmDone  = harmIntendedBy(subject, actor, false) *  5;
    incentive += wierdness = baseCuriosity(actor, subject, false)  *  5;
    if (! asRealTask) return incentive;
    
    conscience = 10 * baseConscience(actor, subject);
    if      (incentive <= conscience   ) return -1     ;
    else if (! isArmed(actor)          ) incentive -= 5;
    else if (actor.senses.isEmergency()) incentive += 5;
    
    winChance  = combatWinChance(actor, subject, teamSize);
    inhibition = Nums.max(0, conscience);
    priority   = incentive * winChance;
    
    if (report) I.reportVars(
      "\nCombat priority for "+actor, "  ",
      "subject  " , subject    ,
      "reward   " , rewardBonus,
      "harmDone"  , harmDone   ,
      "dislike"   , dislike    ,
      "wierdness" , wierdness  ,
      "conscience", conscience ,
      "inhibition", inhibition ,
      "incentive" , incentive  ,
      "winChance" , winChance  ,
      "priority " , priority   
    );
    if (priority < inhibition) return -1;
    return priority;
  }
  
  
  
  /**  Retreat priority.  Should range from 0 to 30.
    */
  public static float retreatPriority(
    Actor actor
  ) {
    float incentive = 0, loseChance, priority;
    float homeDistance, escapeChance;
    final boolean report = reportOn(actor);
    
    loseChance = 1f - combatWinChance(actor, actor.origin(), 1);
    if (actor.senses.fearLevel() == 0) loseChance = 0;
    incentive += loseChance * 10;
    
    homeDistance = homeDistanceFactor(actor, actor.origin());
    if (! isArmed(actor)) homeDistance = (homeDistance + 2) / 2;
    if (actor.senses.isEmergency()) homeDistance *= 1.5f;
    
    escapeChance = Nums.clamp(1f - actor.health.fatigueLevel(), 0, 1);
    priority = incentive * homeDistance * escapeChance;
    
    if (report) I.reportVars(
      "\nRetreat priority for "+actor, "  ",
      "incentive   ", incentive,
      "fear level  ", actor.senses.fearLevel(),
      "emergency   ", actor.senses.isEmergency(),
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
  
  
  
  /**  Social-related utility methods-
    */
  public static float baseConscience(Actor actor, Target toward) {
    if (! actor.species().sapient()) return 0;
    float conscience = (1 + actor.traits.relativeLevel(EMPATHIC)) / 2;
    
    if (toward instanceof Actor) {
      final Species s = ((Actor) toward).species();
      if      (s.sapient()) conscience /= 1;
      else if (s.living ()) conscience /= 2;
      else                  conscience /= 4;
    }
    else conscience /= 4;
    
    return conscience;
  }
  
  
  public static float baseCuriosity(
    Actor actor, Target toward, boolean positive
  ) {
    float strangeness = actor.relations.noveltyFor(toward.base());
    float curiosity = (1 + actor.traits.relativeLevel(CURIOUS)) / 2;
    if (positive) return curiosity * strangeness;
    else return Nums.clamp(strangeness - curiosity, 0, 1);
  }
  
  
  
  /**  Combat-related utility methods.
    */
  public static boolean isArmed(Actor actor) {
    final DeviceType type = actor.gear.deviceType();
    final float baseDamage = actor.gear.baseDamage();
    return baseDamage > 0 || ((type != null) && type.baseDamage > 0);
  }
  
  
  public static float homeDistanceFactor(Actor actor, Target around) {
    final Stage world = actor.world();
    float baseMult = 1;
    
    final Tile at   = world.tileAt(around);
    final Base owns = world.claims.baseClaiming(at);
    if (owns != null) baseMult -= owns.relations.relationWith(actor.base());
    
    return Nums.clamp(baseMult, 0, 2);
  }
  
  
  public static float combatWinChance(
    Actor actor, Target around, int teamSize
  ) {
    float fearLevel = actor.senses.fearLevel ();
    float strength  = actor.senses.powerLevel();
    float health    = 1f - actor.health.injuryLevel();
    float courage   = 1 + actor.traits.relativeLevel(FEARLESS);

    final Base otherBase = around.base(), ownBase = actor.base();
    float danger = ownBase.dangerMap.sampleAround(
      around, Stage.SECTOR_SIZE
    );
    if (otherBase != null) {
      final float foeSafe = 0 - otherBase.dangerMap.sampleAround(
        around, Stage.SECTOR_SIZE
      );
      danger = (danger + Nums.max(0, foeSafe)) / 2;
    }
    
    strength += (teamSize - 1) * CombatUtils.AVG_POWER;
    
    danger = (courage > 1) ? (danger / courage) : (danger * (2 - courage));
    danger = danger / (strength + Nums.max(0, danger));
    danger = Nums.max(fearLevel, danger);
    
    float chance = Nums.clamp(health * (1 - danger), 0, 1);
    if (around instanceof Actor) {
      float power = CombatUtils.powerLevelRelative(actor, (Actor) around);
      power *= courage;
      chance -= Nums.clamp(1f - power, 0, 1) / 2;
    }
    return Nums.clamp(chance, 0, 1);
  }
  
  
  public static float harmIntendedBy(
    Target acts, Actor witness, boolean asCombat
  ) {
    if (acts instanceof Actor) {
      final Actor other = (Actor) acts;
      final Target victim = asCombat ?
        other.planFocus(Combat.class, true) :
        other.actionFocus()
      ;
      if (victim != null) {
        float protectUrge = witness.relations.valueFor(victim);
        return other.harmIntended(victim) * protectUrge;
      }
    }
    if (acts instanceof Venue) {
    }
    return 0;
  }
  
  
  //  TODO:  Consider passing in parent-Missions directly for purposes of
  //         reward-evaluation and team-assessment.
  
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










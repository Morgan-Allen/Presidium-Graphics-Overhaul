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
import stratos.game.maps.Planet;
import static stratos.game.actors.Qualities.*;



//  Okay.  Here, I'm going to make an effort to replace the various plan-
//  priority evaluation methods with something simpler.

public class PlanUtils {
  
  
  private static boolean
    verbose     = false,
    failVerbose = true ;
  
  
  private static boolean reportOn(Actor a, float priority) {
    if (priority <= 0 && ! failVerbose) return false;
    return I.talkAbout == a && verbose;
  }
  
  
  /**  Combat-priority.  Should range from 0 to 20.
    */
  public static float combatPriority(
    Actor actor, Target subject,
    float rewardBonus, int teamSize, boolean asRealTask, float harm
  ) {
    float incentive = 0, winChance, inhibition, priority;
    float harmDone, dislike, wierdness, conscience;
    
    if (! asRealTask && CombatUtils.isDowned(subject, Combat.OBJECT_EITHER)) {
      return 0;
    }
    
    dislike = actor.relations.valueFor(subject) * -10 * harm;
    if (dislike > 0) {
      dislike *= PlanUtils.traitAverage(actor, DEFENSIVE, CRUEL) * 2;
    }
    
    harmDone = harmIntendedBy(subject, actor, false) * 10;
    if (harmDone > 0) {
      harmDone *= PlanUtils.traitAverage(actor, DEFENSIVE, IMPULSIVE);
    }
    
    incentive += dislike ;
    incentive += harmDone;
    incentive += wierdness = baseCuriosity(actor, subject, false) * 5;
    incentive += rewardBonus;
    incentive = Nums.clamp(incentive, -20, 20);
    if (! asRealTask) return incentive;
    
    conscience = 10 * baseConscience(actor, subject) * harm;
    if      (incentive <= conscience   ) return -1     ;
    else if (! isArmed(actor)          ) incentive -= 5;
    else if (actor.senses.isEmergency()) incentive += 5;
    
    winChance  = combatWinChance(actor, subject, teamSize);
    inhibition = Nums.max(10 * (1 - winChance), conscience);
    if (incentive < inhibition) return -1;
    priority = incentive * (1 + winChance) / 2;
    
    if (reportOn(actor, priority)) I.reportVars(
      "\nCombat priority for "+actor, "  ",
      "subject  " , subject    ,
      "reward   " , rewardBonus,
      "emergency" , actor.senses.isEmergency(),
      "harm level", harm       ,
      "harm done ", harmDone   ,
      "dislike"   , dislike    ,
      "wierdness" , wierdness  ,
      "conscience", conscience ,
      "inhibition", inhibition ,
      "incentive" , incentive  ,
      "winChance" , winChance  ,
      "priority " , priority   
    );
    return priority;
  }
  
  
  
  /**  Retreat priority.  Should range from 0 to 20.
    */
  public static float retreatPriority(
    Actor actor
  ) {
    float incentive = 0, loseChance, priority;
    float homeDistance, escapeChance, injury;
    
    loseChance = 1f - combatWinChance(actor, actor.origin(), 1);
    if (actor.senses.fearLevel() == 0) loseChance = 0;
    
    homeDistance = homeDistanceFactor(actor, actor.origin());
    if (Action.isStealthy(actor)) homeDistance /= 4;
    if (actor.senses.isEmergency()) homeDistance *= 1.5f;
    if (loseChance > 0 && ! isArmed(actor)) homeDistance += 0.5f;
    
    incentive    += Nums.max(loseChance, homeDistance / 3) * 20;
    incentive    += (injury = actor.health.injuryLevel()) * 10;
    escapeChance = Nums.clamp(1.5f - actor.health.fatigueLevel(), 0, 1);
    escapeChance = escapeChance * Nums.min(1, homeDistance);
    priority     = Nums.clamp(incentive * escapeChance, -10, 20);
    
    if (reportOn(actor, priority)) I.reportVars(
      "\nRetreat priority for "+actor, "  ",
      "haven is    ", actor.senses.haven()+" (at "+actor.origin()+")",
      "incentive   ", incentive,
      "injury level", injury,
      "fatigue     ", actor.health.fatigueLevel(),
      "fear level  ", actor.senses.fearLevel   (),
      "emergency   ", actor.senses.isEmergency (),
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
    Actor actor, Actor subject, float rewardBonus, float commChance
  ) {
    float liking = 0, novelty = 0, solitude = 0;
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

    if (reportOn(actor, priority)) I.reportVars(
      "\nDialogue priority for "+actor, "  ",
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
    float incentive = 0, liking = 0, priority = 0, conscience;
    
    conscience = baseConscience(actor, subject);
    liking     = actor.relations.valueFor(subject);
    incentive  = ((liking + conscience - 0.5f) * 10) * subjectDanger * 2;
    incentive  += rewardBonus;
    priority   = incentive * (supportChance + subjectDanger) / 2;
    
    if (reportOn(actor, priority)) I.reportVars(
      "\nSupport priority for "+actor, "  ",
      "subject"      , subject      ,
      "reward"       , rewardBonus  ,
      "supportChance", supportChance,
      "subjectDanger", subjectDanger,
      "liking"       , liking       ,
      "conscience"   , conscience   ,
      "incentive"    , incentive    ,
      "priority"     , priority     
    );
    return priority;
  }
  
  
  
  /**  Exploring priority- should range from 0 to 15.
    */
  public static float explorePriority(
    Actor actor, Target surveyed,
    float rewardBonus, boolean idle, float competence
  ) {
    float incentive = 0, novelty = 0, priority = 0, enjoys = 0;
    float daylight = 0, exploreChance = 0, homeDist = 0;
    
    if (idle) novelty = 0.2f;
    else novelty = Nums.clamp(Nums.max(
      (actor.base().intelMap.fogAt(surveyed) == 0 ? 0.5f : 0.25f),
      (actor.relations.noveltyFor(surveyed) - 1)
    ), 0, 1);
    
    incentive = novelty * 10;
    incentive *= enjoys = PlanUtils.traitAverage(actor, CURIOUS, ENERGETIC);
    incentive += rewardBonus;

    daylight = Planet.dayValue(actor.world());
    exploreChance = actor.health.baseSpeed() * competence;
    exploreChance *= (daylight + 1) / 2;
    
    homeDist = homeDistanceFactor(actor, surveyed);
    
    priority = incentive * Nums.clamp(exploreChance, 0, 1);
    priority -= (homeDist * 5) + ((1 - daylight) * 2.5f);
    
    if (reportOn(actor, priority)) I.reportVars(
      "\nExplore priority for "+actor, "  ",
      "surveyed"      , surveyed     ,
      "reward bonus"  , rewardBonus  ,
      "competence"    , competence   ,
      "enjoy rating"  , enjoys       ,
      "incentive"     , incentive    ,
      "novelty"       , novelty      ,
      "home distance" , homeDist     ,
      "explore chance", exploreChance,
      "daylight"      , daylight     ,
      "priority"      , priority
    );
    
    return priority;
  }
  
  
  
  /**  Ambition priority- should range from 0 to 10.
    */
  public static float ambitionPriority(
    Actor actor, Background position, Venue at, float quality
  ) {
    float teamBonus = 0, crowding = 0, priority = 0;
    float locale = 0, ambience = 0, safety = 0, loyalty = 0;
    
    ambience = Nums.clamp(at.world().ecology().ambience.valueAt(at) / 10, 0, 1);
    safety   = Nums.clamp(at.base().dangerMap.sampleAround(at, -1) / -10, 0, 1);
    loyalty  = actor.relations.valueFor(at.base());
    locale   = ((1 + ambience) * (1 + safety) * (1 + loyalty)) / 4;
    
    crowding = at.crowdRating(actor, position);
    
    teamBonus = occupantRelations(actor, at) * (1 + crowding);
    
    priority = 5 * (quality + teamBonus + locale) / 3;
    if (! Staff.doesBelong(actor, at)) priority *= (1 - crowding);
    
    if (reportOn(actor, priority)) I.reportVars(
      "\nAmbition priority for "+actor, "  ",
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
    for (Actor a : at.staff.lodgers()) {
      sumR += actor.relations.valueFor(a);
      sumW++;
    }
    if (sumW == 0) return 0;
    return sumR / sumW;
  }
  
  
  
  /**  Job-priority method- should return a result between 0 and 10.
    */
  public static float jobPlanPriority(
    Actor actor, Plan plan,
    float urgency, float competence,
    int helpLimit, float riskLevel, Trait... enjoyTraits
  ) {
    float incentive = 0, priority = 0, enjoyBonus = 0;
    float dutyBonus = 0, helpBonus = 0, shift = 0;
    float failPenalty = 0, help = 0;
    
    final Property work = actor.mind.work();
    float liking = actor.relations.valueFor(plan.subject);
    if (urgency > 0 && plan.hasBegun()) urgency = Nums.max(urgency, 0.5f);
    
    incentive += urgency * 10 * liking;
    incentive *= (enjoyBonus = traitAverage(actor, enjoyTraits)) * 2;
    incentive += plan.motiveBonus();
    
    if (helpLimit >= 0) {
      help = competition(plan, plan.subject, actor);
      if (help > helpLimit && ! plan.hasBegun()) return -1;
    }
    if (helpLimit > 0 && help > 0) {
      helpBonus = Nums.min(priority * 0.5f, help * 2.5f / helpLimit);
      incentive += helpBonus;
    }
    if (incentive <= 0 && urgency <= 0) {
      if (reportOn(actor, 0)) I.say("\nNo incentive for "+plan);
      return -1;
    }
    
    priority = incentive * competence;

    if (plan.isJob() && work != null) {
      shift = work.staff().shiftFor(actor);
      dutyBonus = actor.traits.relativeLevel(DUTIFUL) * 1.25f;
      if (shift == Venue.OFF_DUTY     ) dutyBonus -= 2.5f;
      if (shift == Venue.PRIMARY_SHIFT) dutyBonus += 2.5f;
      priority += dutyBonus;
    }
    
    priority -= failPenalty = (1 - competence) * 10 * riskLevel;
    
    if (reportOn(actor, priority)) I.reportVars(
      "\nJob priority for "+actor, "  ",
      "Job is"     , plan        ,
      "Is job?"    , plan.isJob(),
      "On shift"   , (int) shift ,
      "Liking"     , liking      ,
      "Urgency"    , urgency     ,
      "Competence" , competence  ,
      "enjoyBonus" , enjoyBonus  ,
      "dutyBonus"  , dutyBonus   ,
      "failRisk"   , riskLevel   ,
      "failPenalty", failPenalty ,
      "help gotten", help+"/"+helpLimit,
      "helpBonus"  , helpBonus   ,
      "Incentive"  , incentive   ,
      "Priority"   , priority    
    );
    return Nums.clamp(priority, 0, 10);
  }
  
  
  
  /**  Social-related utility methods-
    */
  public static float traitAverage(Actor actor, Trait... traits) {
    if (traits == null || traits.length == 0) return 0;
    float avg = 0;
    for (Trait t : traits) {
      avg += 1 + actor.traits.relativeLevel(t);
    }
    return avg / (traits.length * 2);
  }
  
  
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
  
  
  public static float competition(Class planClass, Target t, Actor actor) {
    float competition = 0;
    final Stage world = actor.world();
    for (Behaviour b : world.activities.allTargeting(t)) {
      if (b instanceof Plan) {
        final Plan plan = (Plan) b;
        if (plan.getClass() != planClass) continue;
        if (plan.actor() == null || plan.actor() == actor) continue;
        competition += plan.successChanceFor(plan.actor());
      }
    }
    return competition;
  }
  
  
  public static float competition(Plan match, Target t, Actor actor) {
    float competition = 0;
    final Stage world = actor.world();
    for (Behaviour b : world.activities.allTargeting(t)) {
      if (b instanceof Plan) {
        final Plan plan = (Plan) b;
        if (plan.actor() == actor) continue;
        if (! plan.matchesPlan(match)) continue;
        competition += plan.successChanceFor(plan.actor());
      }
    }
    return competition;
  }
  

  //  TODO:  Pass in a conversion instead (or allow for that.)
  public static float successForActorWith(
    Actor actor, Skill baseSkills[], float DC, boolean realTest
  ) {
    if (realTest) {
      float success = 0;
      for (Skill s : baseSkills) {
        success += actor.skills.test(s, DC, 1) ? 1 : 0;
      }
      return success / baseSkills.length;
    }
    else {
      float chance = 0;
      for (Skill s : baseSkills) {
        chance += actor.skills.chance(s, DC);
      }
      return chance / baseSkills.length;
    }
  }
  
  
  
  /**  Combat-related utility methods.
    */
  public static boolean isArmed(Actor actor) {
    final DeviceType type = actor.gear.deviceType();
    final float baseDamage = actor.gear.baseDamage();
    return baseDamage > 0 || ((type != null) && type.baseDamage > 0);
  }
  
  
  public static float homeDistanceFactor(Actor actor, Target around) {
    
    final Target haven = actor.senses.haven();
    if (haven == null) return 0;
    
    float baseMult = Spacing.distance(around, haven);
    baseMult = Nums.sqrt(baseMult / Stage.ZONE_SIZE);
    
    //baseMult = 1 - (1 / (1 + baseMult));
    
    return Nums.clamp(baseMult, 0, 2);
  }
  
  
  public static float combatWinChance(
    Actor actor, Target enemy, int teamSize
  ) {
    float fearLevel = actor.senses.fearLevel ();
    float strength  = actor.senses.powerLevel();
    float health    = 1f - actor.health.injuryLevel();
    float courage   = 1 + actor.traits.relativeLevel(FEARLESS);
    
    final Base otherBase = enemy.base(), ownBase = actor.base();
    float danger = ownBase.dangerMap.sampleAround(
      enemy, Stage.ZONE_SIZE
    );
    if (otherBase != null) {
      final float foeSafe = 0 - otherBase.dangerMap.sampleAround(
        enemy, Stage.ZONE_SIZE
      );
      danger = (danger + Nums.max(0, foeSafe)) / 2;
    }
    
    strength += (teamSize - 1) * CombatUtils.AVG_POWER;
    
    danger = (courage > 1) ? (danger / courage) : (danger * (2 - courage));
    danger = danger / (strength + Nums.max(0, danger));
    danger = Nums.max(fearLevel, danger);
    
    float chance = Nums.clamp(health * (1 - danger), 0, 1);
    if (enemy instanceof Actor) {
      float power = CombatUtils.powerLevelRelative(actor, (Actor) enemy);
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











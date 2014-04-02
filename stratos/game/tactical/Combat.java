/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.tactical ;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.user.*;
import stratos.util.*;


//  TODO:  This will have to be looked into again.


public class Combat extends Plan implements Qualities {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  private static boolean
    verbose         = false,
    eventsVerbose   = false,
    strengthVerbose = false ;
  
  final static int
    STYLE_RANGED = 0,  //TODO:  Replace with hit-and-run/skirmish
    STYLE_MELEE  = 1,  //TODO:  Replace with stand ground/direct assault
    STYLE_EITHER = 2,
    ALL_STYLES[] = { 0, 1, 2 },
    
    OBJECT_EITHER  = 0,
    OBJECT_SUBDUE  = 1,
    OBJECT_DESTROY = 2,
    ALL_OBJECTS[] = { 0, 1, 2 } ;

  final static String STYLE_NAMES[] = {
    "Ranged",
    "Melee",
    "Either"
  } ;
  final static String OBJECT_NAMES[] = {
    "Neutralise ",
    "Capture ",
    "Destroy ",
  } ;
  
  
  final Element target ;  //, concern. (in case of defensive action.)
  final int style, object ;
  
  
  public Combat(Actor actor, Element target) {
    this(actor, target, STYLE_EITHER, OBJECT_EITHER) ;
  }
  
  
  public Combat(
    Actor actor, Element target, int style, int object
  ) {
    super(actor, target) ;
    this.target = target ;
    this.style = style ;
    this.object = object ;
  }
  
  
  public Combat(Session s) throws Exception {
    super(s) ;
    this.target = (Element) s.loadObject() ;
    this.style = s.loadInt() ;
    this.object = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(target) ;
    s.saveInt(style) ;
    s.saveInt(object) ;
  }
  
  
  
  /**  Gauging the relative strength of combatants, odds of success, and how
    *  (un)appealing an engagement would be.
    */
  final static Trait BASE_TRAITS[] = { FEARLESS, DEFENSIVE };
  final static Skill
    MELEE_SKILLS[]  = { HAND_TO_HAND, SHIELD_AND_ARMOUR, FORMATION_COMBAT },
    RANGED_SKILLS[] = { MARKSMANSHIP, STEALTH_AND_COVER, FORMATION_COMBAT };
  
  
  public float priorityFor(Actor actor) {
    if (isDowned(target)) return 0 ;
    final boolean melee = actor.gear.meleeWeapon();
    
    float modifier = 0 - ROUTINE;
    if (target instanceof Actor) {
      //  TODO:  Just use the general harm-level of the other guy's current
      //  behaviour as the basis for evaluation?
      final Target victim = ((Actor) target).targetFor(Combat.class);
      if (victim != null) {
        modifier += ROUTINE * actor.mind.relationValue(victim);
      }
    }
    
    float harmLevel = REAL_HARM;
    if (object == OBJECT_SUBDUE ) harmLevel = MILD_HARM;
    if (object == OBJECT_DESTROY) harmLevel = EXTREME_HARM;
    
    return priorityForActorWith(
      actor, target, PARAMOUNT,
      harmLevel, MILD_COOPERATION,
      melee ? MELEE_SKILLS : RANGED_SKILLS, BASE_TRAITS,
      modifier, NORMAL_DISTANCE_CHECK, REAL_DANGER
    );
  }
  
  
  protected float successChance() {
    float danger;
    
    if (target instanceof Actor) {
      final Actor struck = (Actor) target ;
      danger = Retreat.dangerAtSpot(target, actor, struck) ;
    }
    else if (target instanceof Venue) {
      final Venue struck = (Venue) target ;
      danger = Retreat.dangerAtSpot(struck, actor, null) ;
    }
    else danger = Retreat.dangerAtSpot(target, actor, null) / 2;
    
    danger *= 1 + actor.traits.relativeLevel(NERVOUS);
    final float chance = Visit.clamp(1 - danger, 0.1f, 0.9f) ;
    return chance;
  }
  
  
  protected static boolean isDowned(Element subject) {
    //
    //  TODO:  Vary this based on objective type, along with the types of
    //  damage dealt.
    if (subject instanceof Actor)
      return ! ((Actor) subject).health.conscious() ;
    if (subject instanceof Venue)
      return ((Venue) subject).structure.destroyed() ;
    return false ;
  }
  
  
  public boolean valid() {
    if (target instanceof Mobile && ((Mobile) target).indoors()) return false ;
    return super.valid() ;
  }
  
  
  //
  //  TODO:  Actors may need to cache this value?  Maybe later.  Not urgent at
  //  the moment.
  //
  //  Note:  it's acceptable to pass null as the enemy argument, for a general
  //  estimate of combat prowess.  (TODO:  Put in a separate method for that?)
  public static float combatStrength(Actor actor, Actor enemy) {
    float strength = 0 ;
    strength += (actor.gear.armourRating() + actor.gear.attackDamage()) / 20f ;
    strength *= (1 + (actor.health.maxHealth() / 10)) / 2f ;
    strength *= (1 - actor.health.injuryLevel()) ;
    strength *= 1 - actor.health.stressPenalty() ;
    //
    //  Scale by general ranks in relevant skills-
    if (strengthVerbose) I.sayAbout(actor, "Native strength: "+strength) ;
    strength *= (2 +
      actor.traits.useLevel(HAND_TO_HAND) +
      actor.traits.useLevel(MARKSMANSHIP)
    ) / 20 ;
    strength *= (2 +
      actor.traits.useLevel(HAND_TO_HAND) +
      actor.traits.useLevel(STEALTH_AND_COVER)
    ) / 20 ;
    if (strengthVerbose) I.sayAbout(actor, "With skills: "+strength) ;
    //
    //  And if you're measuring combat against a specific adversary, scale by
    //  chance of victory-
    if (enemy != null) {
      final Skill attack, defend ;
      if (actor.gear.meleeWeapon()) {
        attack = defend = HAND_TO_HAND ;
      }
      else {
        attack = MARKSMANSHIP ;
        defend = STEALTH_AND_COVER ;
      }
      final float chance = actor.traits.chance(attack, enemy, defend, 0) ;
      if (strengthVerbose && I.talkAbout == enemy) {
        I.say("Chance to injure "+enemy+" is: "+chance) ;
        I.say("Native strength: "+strength) ;
      }
      strength *= 2 * chance ;
    }
    return strength ;
  }
  
  
  public static float stealthValue(Element e, Actor looks) {
    if (e instanceof Actor) {
      final Actor a = (Actor) e ;
      if (a.base() == looks.base()) return 0 ;
      float stealth = a.traits.traitLevel(STEALTH_AND_COVER) / 2f ;
      stealth *= Action.moveLuck(a) ;
      stealth /= (0.5f + a.health.baseSpeed()) ;
      return stealth ;
    }
    if (e instanceof Installation) {
      final Installation i = (Installation) e ;
      return i.structure().cloaking() ;
    }
    return 0 ;
  }
  
  
  
  
  /**  Actual behaviour implementation-
    */
  protected Behaviour getNextStep() {
    if (eventsVerbose && hasBegun()) {
      I.sayAbout(actor, "NEXT COMBAT STEP "+this.hashCode()) ;
    }
    //
    //  This might need to be tweaked in cases of self-defence, where you just
    //  want to see off an attacker.
    if (isDowned(target)) {  //  TODO:  This might need to be varied-
      if (eventsVerbose && hasBegun()) I.sayAbout(actor, "COMBAT COMPLETE") ;
      return null ;
    }
    Action strike = null ;
    final DeviceType DT = actor.gear.deviceType() ;
    final boolean melee = actor.gear.meleeWeapon() ;
    final boolean razes = target instanceof Venue ;
    final float danger = Retreat.dangerAtSpot(
      actor.origin(), actor, razes ? null : (Actor) target
    ) ;
    
    final String strikeAnim = DT == null ? Action.STRIKE : DT.animName ;
    if (razes) {
      strike = new Action(
        actor, target,
        this, "actionSiege",
        strikeAnim, "Razing"
      ) ;
    }
    else {
      strike = new Action(
        actor, target,
        this, "actionStrike",
        strikeAnim, "Striking at"
      ) ;
    }
    //
    //  Depending on the type of target, and how dangerous the area is, a bit
    //  of dancing around may be in order.
    if (melee) configMeleeAction(strike, razes, danger) ;
    else configRangedAction(strike, razes, danger) ;
    ///if (eventsVerbose) I.sayAbout(actor, "NEXT STRIKE "+strike) ;
    return strike ;
  }
  
  
  private void configMeleeAction(Action strike, boolean razes, float danger) {
    ///if (eventsVerbose) I.sayAbout(actor, "Configuring melee attack.\n") ;
    final World world = actor.world() ;
    strike.setProperties(Action.QUICK) ;
    if (razes) {
      if (! Spacing.adjacent(actor, target)) {
        strike.setMoveTarget(Spacing.nearestOpenTile(target, actor, world)) ;
      }
      else if (Rand.num() < 0.2f) {
        strike.setMoveTarget(Spacing.pickFreeTileAround(target, actor)) ;
      }
      else strike.setMoveTarget(actor.origin()) ;
    }
  }
  
  
  private void configRangedAction(Action strike, boolean razes, float danger) {
    ///if (eventsVerbose) I.sayAbout(actor, "Configuring ranged attack.\n") ;
    
    final float range = actor.health.sightRange() ;
    boolean underFire = actor.world().activities.includes(actor, Combat.class) ;
    if (razes && Rand.num() < 0.1f) underFire = true ;
    
    boolean dodges = false ;
    if (actor.mind.hasSeen(target)) {
      final float distance = Spacing.distance(actor, target) / range ;
      //
      //  If not under fire, consider advancing for a clearer shot-
      if (Rand.num() < distance && ! underFire) {
        final Target AP = Retreat.pickWithdrawPoint(
          actor, range, target, -0.1f
        ) ;
        if (AP != null) { dodges = true ; strike.setMoveTarget(AP) ; }
      }
      //
      //  Otherwise, consider falling back for cover-
      if (underFire && Rand.num() > distance) {
        final Target WP = Retreat.pickWithdrawPoint(
          actor, range, target, 0.1f
        ) ;
        if (WP != null) { dodges = true ; strike.setMoveTarget(WP) ; }
      }
    }
    
    if (dodges) strike.setProperties(Action.QUICK) ;
    else strike.setProperties(Action.RANGED | Action.QUICK | Action.TRACKS) ;
  }
  
  
  
  /**  Executing the action-
    */
  public boolean actionStrike(Actor actor, Actor target) {
    if (target.health.dying()) return false ;
    //
    //  You may want a separate category for animals.
    if (actor.gear.meleeWeapon()) {
      performStrike(actor, target, HAND_TO_HAND, HAND_TO_HAND) ;
    }
    else {
      performStrike(actor, target, MARKSMANSHIP, STEALTH_AND_COVER) ;
    }
    return true ;
  }
  
  
  public boolean actionSiege(Actor actor, Venue target) {
    if (target.structure.destroyed()) return false ;
    performSiege(actor, target) ;
    return true ;
  }
  
  
  static void performStrike(
    Actor actor, Actor target,
    Skill offence, Skill defence
  ) {
    //
    //  TODO:  Allow for wear and tear to weapons/armour over time...
    final boolean success = actor.health.conscious() ? actor.traits.test(
      offence, target, defence, 0 - rangePenalty(actor, target), 1
    ) : true ;
    if (success) {
      float damage = actor.gear.attackDamage() * Rand.avgNums(2) ;
      damage -= target.gear.armourRating() * Rand.avgNums(2) ;
      
      final float oldDamage = damage ;
      damage = target.gear.afterShields(damage, actor.gear.physicalWeapon()) ;
      final boolean hit = damage > 0 ;
      if (damage != oldDamage) {
        OutfitType.applyFX(target.gear.outfitType(), target, actor, hit) ;
      }
      if (hit && ! GameSettings.noBlood) target.health.takeInjury(damage) ;
    }
    DeviceType.applyFX(actor.gear.deviceType(), actor, target, success) ;
  }
  
  
  static void performSiege(
    Actor actor, Venue besieged
  ) {
    boolean accurate = false ;
    if (actor.gear.meleeWeapon()) {
      accurate = actor.traits.test(HAND_TO_HAND, 0, 1) ;
    }
    else {
      final float penalty = rangePenalty(actor, besieged) ;
      accurate = actor.traits.test(MARKSMANSHIP, penalty, 1) ;
    }
    
    float damage = actor.gear.attackDamage() * Rand.avgNums(2) * 1.5f ;
    if (accurate) damage *= 1.5f ;
    else damage *= 0.5f ;
    
    final float armour = besieged.structure.armouring() ;
    damage -= armour * (Rand.avgNums(2) + 0.25f) ;
    damage *= 5f / (5 + armour) ;
    
    ///I.say("Armour/Damage: "+armour+"/"+damage) ;
    
    if (damage > 0) besieged.structure.takeDamage(damage) ;
    DeviceType.applyFX(actor.gear.deviceType(), actor, besieged, true) ;
  }
  
  
  static float rangePenalty(Actor a, Target t) {
    final float range = Spacing.distance(a, t) / 2 ;
    return range * 5 / (a.health.sightRange() + 1f) ;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("In combat with ") ;
    d.append(target) ;
  }
}






/*
final boolean report = verbose && I.talkAbout == actor ;

if (target instanceof Actor) {
  final Actor struck = (Actor) target ;
  float lossCost = PARAMOUNT, winReward = priorityMod ;
  if (hasBegun()) lossCost = 0 ;
  
  float BP = combatPriority(actor, struck, winReward, lossCost, report) ;
  return BP <= 0 ? 0 : BP + ROUTINE ;
}
if (target instanceof Venue) {
  
  final Venue struck = (Venue) target ;
  float BP = priorityMod - (actor.mind.relationValue(struck) * ROUTINE) ;
  BP += ROUTINE ;
  
  //  TODO:  Factor this out.  Also repeated below.
  if (! hasBegun()) {
    float danger = Retreat.dangerAtSpot(struck, actor, null) ;
    danger += Plan.dangerPenalty(struck, actor) ;
    BP += 0 - (danger * ROUTINE) ;
  }
  //  TODO:  Eliminate loss cost in a similar fashion to the above, if
  //  begun or in range, etc.
  
  ///I.sayAbout(actor, "Priority mod is: "+priorityMod) ;
  ///I.sayAbout(actor, "Relation is: "+actor.mind.relation(struck)) ;
  return BP ;// BP <= 0 ? 0 : BP + ROUTINE ;
}
return -1 ;
//*/
//}


//  TODO:  This could probably be made more generalised.
/*
public static float hostility(Actor actor) {
Plan p = null ;
for (Behaviour b : actor.mind.agenda()) if (b instanceof Plan) {
  p = (Plan) b ;
  break ;
}
if (p instanceof Combat) {
  return 1.0f ;
}
if (p instanceof Dialogue) return -0.5f ;
if (p instanceof FirstAid) return -1.0f ;
return 0 ;
}


protected static float combatPriority(
Actor actor, Actor enemy, float winReward, float lossCost, boolean report
) {
if (actor == enemy) return 0 ;
if (report) I.say("  Basic combat reward/cost: "+winReward+"/"+lossCost) ;

float danger = Retreat.dangerAtSpot(enemy, actor, enemy) ;
final float chance = Visit.clamp(1 - danger, 0.1f, 0.9f) ;

final Target enemyTargets = enemy.targetFor(null) ;
float hostility = hostility(enemy) ;
hostility *= PARAMOUNT * actor.mind.relationValue(enemyTargets) ;
winReward += hostility ;
lossCost -= hostility / 2 ;
winReward -= actor.mind.relationValue(enemy) * PARAMOUNT ;

if (winReward < 0) return 0 ;
winReward *= actor.traits.scaleLevel(AGGRESSIVE) ;

if (report) {
  I.say(
    "  "+actor+" considering COMBAT with "+enemy+
    ", time: "+actor.world().currentTime()
  ) ;
  I.say(
    "  Danger level: "+danger+
    "\n  Appeal before chance: "+winReward+", chance: "+chance
  ) ;
}
if (chance <= 0) {
  if (report) I.say("  No chance of victory!\n") ;
  return 0 ;
}
float appeal = (winReward * chance) - ((1 - chance) * lossCost) ;
if (report) I.say("  Final appeal: "+appeal+"\n") ;
return appeal ;
}
//*/
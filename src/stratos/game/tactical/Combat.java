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

public class Combat extends Plan implements Qualities {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  private static boolean
    evalVerbose   = false,
    eventsVerbose = false;
  
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
  
  
  final Element target ;
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
  
  
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (isDowned(target, object)) return 0;
    final boolean melee = actor.gear.meleeWeapon();
    
    float modifier = 0 - ROUTINE;
    Target victim = null;
    if (target instanceof Actor) {
      //  TODO:  Just use the general harm-level of the other guy's current
      //  behaviour as the basis for evaluation?
      victim = ((Actor) target).focusFor(Combat.class);
      if (victim != null) {
        modifier += PARAMOUNT * actor.memories.relationValue(victim);
      }
    }
    
    float harmLevel = REAL_HARM;
    if (object == OBJECT_SUBDUE ) harmLevel = MILD_HARM;
    if (object == OBJECT_DESTROY) harmLevel = EXTREME_HARM;
    
    final float priority = priorityForActorWith(
      actor, target, PARAMOUNT,
      harmLevel, FULL_COOPERATION,
      melee ? MELEE_SKILLS : RANGED_SKILLS, BASE_TRAITS,
      modifier, NORMAL_DISTANCE_CHECK, REAL_DANGER,
      report
    );
    
    if (report) {
      I.say("  Victim was: "+victim);
      I.say("  Modifier was: "+modifier);
      I.say("  Final combat priority: "+priority);
    }
    return priority;
  }
  
  
  protected float successChance() {
    float danger;
    
    if (target instanceof Actor) {
      final Actor struck = (Actor) target;
      danger = CombatUtils.dangerAtSpot(target, actor, struck);
    }
    else if (target instanceof Venue) {
      final Venue struck = (Venue) target;
      danger = CombatUtils.dangerAtSpot(struck, actor, null);
    }
    else danger = CombatUtils.dangerAtSpot(target, actor, null) / 2;
    
    final float chance = Visit.clamp(1 - danger, 0.1f, 0.9f);
    return chance;
  }
  
  
  protected static boolean isDowned(Element subject, int object) {
    if (subject instanceof Actor) {
      final Actor struck = (Actor) subject;
      if (object == OBJECT_DESTROY) return ! struck.health.alive();
      return ! struck.health.conscious() ;
    }
    if (subject instanceof Venue)
      return ((Venue) subject).structure.destroyed() ;
    return false ;
  }
  
  
  public boolean valid() {
    if (target instanceof Mobile && ((Mobile) target).indoors()) return false ;
    return super.valid() ;
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
    if (isDowned(target, object)) {
      if (eventsVerbose && hasBegun()) I.sayAbout(actor, "COMBAT COMPLETE") ;
      return null ;
    }
    Action strike = null ;
    final DeviceType DT = actor.gear.deviceType() ;
    final boolean melee = actor.gear.meleeWeapon() ;
    final boolean razes = target instanceof Venue ;
    final float danger = CombatUtils.dangerAtSpot(
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
    if (actor.senses.hasSeen(target)) {
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
    
    if (dodges) strike.setProperties(Action.QUICK | Action.TRACKS) ;
    else strike.setProperties(Action.RANGED | Action.QUICK | Action.TRACKS) ;
  }
  
  
  
  /**  Executing the action-
    */
  public boolean actionStrike(Actor actor, Actor target) {
    if (target.health.dying()) return false ;
    final boolean subdue = object == OBJECT_SUBDUE;
    //
    //  TODO:  You may want a separate category for animals.
    if (actor.gear.meleeWeapon()) {
      performStrike(actor, target, HAND_TO_HAND, HAND_TO_HAND, subdue) ;
    }
    else {
      performStrike(actor, target, MARKSMANSHIP, STEALTH_AND_COVER, subdue) ;
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
    Skill offence, Skill defence,
    boolean subdue
  ) {
    //  TODO:  Move weapon/armour properties to dedicated subclasses.
    final boolean canStun = actor.gear.hasDeviceProperty(Economy.STUN);
    float penalty = 0, damage = 0;
    penalty -= rangePenalty(actor, target);
    if (subdue && ! canStun) penalty -= 5;
    
    final boolean success = target.health.conscious() ? actor.traits.test(
      offence, target, defence, penalty, 1
    ) : true ;
      
    if (success) {
      damage = actor.gear.attackDamage() * Rand.avgNums(2) ;
      damage -= target.gear.armourRating() * Rand.avgNums(2) ;
      
      final float oldDamage = damage ;
      damage = target.gear.afterShields(damage, actor.gear.physicalWeapon()) ;
      final boolean hit = damage > 0 ;
      if (damage != oldDamage) {
        OutfitType.applyFX(target.gear.outfitType(), target, actor, hit) ;
      }
    }
    
    if (damage > 0 && ! GameSettings.noBlood) {
      if (subdue && canStun) target.health.takeFatigue(damage);
      else if (subdue || canStun) {
        target.health.takeFatigue(damage / 2);
        target.health.takeInjury(damage / 2);
      }
      else target.health.takeInjury(damage);
      //  TODO:  Allow for wear and tear to weapons/armour over time...
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





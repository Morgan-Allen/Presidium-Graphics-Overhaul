/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.game.tactical.*;
import stratos.user.*;
import stratos.util.*;



//  ...I think that distance needs to be made a more significant factor here,
//  and 'active dodge' needs to be implemented.  Point-blank attacks should
//  never miss unless the enemy is visibly dancing around.



public class Combat extends Plan implements Qualities {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  private static boolean
    evalVerbose   = true ,
    eventsVerbose = false,
    damageVerbose = false;
  
  
  final public static int
    STYLE_RANGED = 0,
    STYLE_MELEE  = 1,
    STYLE_EITHER = 2,
    ALL_STYLES[] = { 0, 1, 2 },
    
    OBJECT_EITHER  = 0,
    OBJECT_SUBDUE  = 1,
    OBJECT_DESTROY = 2,
    ALL_OBJECTS[] = { 0, 1, 2 };

  final public static String STYLE_NAMES[] = {
    "Ranged",
    "Melee",
    "Either"
  };
  final public static String OBJECT_NAMES[] = {
    "Neutralise ",
    "Capture ",
    "Destroy ",
  };
  
  
  final int style, object;
  final boolean pursue;
  
  
  public Combat(Actor actor, Element target) {
    this(actor, target, STYLE_EITHER, OBJECT_EITHER, false);
  }
  
  
  public Combat(
    Actor actor, Element target, int style, int object, boolean pursue
  ) {
    super(actor, target, pursue);
    this.style  = style ;
    this.object = object;
    this.pursue = pursue;
  }
  
  
  public Combat(Session s) throws Exception {
    super(s);
    this.style  = s.loadInt();
    this.object = s.loadInt();
    this.pursue = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(style);
    s.saveInt(object);
    s.saveBool(pursue);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Combat(other, (Element) subject, style, object, pursue);
  }
  
  
  
  /**  Gauging the relative strength of combatants, odds of success, and how
    *  (un)appealing an engagement would be.
    */
  final static Trait BASE_TRAITS[] = { FEARLESS, DEFENSIVE, CRUEL };
  final static Skill
    MELEE_SKILLS[]  = { HAND_TO_HAND },
    RANGED_SKILLS[] = { MARKSMANSHIP, STEALTH_AND_COVER };
  
  
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (CombatUtils.isDowned(subject, object)) return 0;

    float harmLevel = REAL_HARM;
    if (object == OBJECT_SUBDUE ) harmLevel = MILD_HARM   ;
    if (object == OBJECT_DESTROY) harmLevel = EXTREME_HARM;
    
    final boolean melee  = actor.gear.meleeWeapon();
    final float   danger = actor.senses.fearLevel();
    
    final float hostility = CombatUtils.hostileRating(actor, subject);
    if (hostility <= 0) return 0;
    float bonus = (actor.senses.isEmergency() ? 1 : 0) + hostility - danger;
    
    final float priority = priorityForActorWith(
      actor, subject,
      ROUTINE, bonus * PARAMOUNT,
      harmLevel, FULL_COOPERATION,
      REAL_FAIL_RISK, melee ? MELEE_SKILLS : RANGED_SKILLS,
      BASE_TRAITS, NORMAL_DISTANCE_CHECK,
      report
    );
    
    //  NOTE:  This may seem like a bit of kluge, but on balance it seems to
    //  result in much more sensible behaviour (unless you're a psychopath,
    //  you don't 'casually' decide to kill something.)
    float threshold = (1 + actor.traits.relativeLevel(EMPATHIC)) / 2;
    threshold *= PARAMOUNT;
    if (report) {
      I.say("\n  Priority bonus:        "+bonus);
      I.say("  Danger level:          "+danger);
      I.say("  Hostility rating:      "+hostility);
      I.say("  Emergency?             "+actor.senses.isEmergency());
      I.say("  Basic combat priority: "+priority);
      I.say("  Empathy threshold:     "+threshold);
    }
    return priority >= threshold ? priority : 0;
  }
  
  
  protected float successChance() {
    //  TODO:  Switch between these two evaluation methods based on
    //  intelligence?  (Or maybe the battle-tactics skill?)
    return Visit.clamp(1 - actor.senses.fearLevel(), 0.1f, 0.9f);
    /*
    final boolean report = evalVerbose && I.talkAbout == actor;
    
    if (subject instanceof Actor) {
      final Actor struck = (Actor) subject;
      float chance = CombatUtils.powerLevelRelative(actor, struck) / 2f;
      chance = (chance + 1 - actor.senses.fearLevel()) / 2f;
      return Visit.clamp(chance, 0, 1);
    }
    else return 1;
    //*/
  }
  
  
  public boolean valid() {
    if (subject instanceof Mobile && ((Mobile) subject).indoors()) {
      return false;
    }
    return super.valid();
  }
  
  
  
  /**  Actual behaviour implementation-
    */
  protected Behaviour getNextStep() {
    final boolean report = eventsVerbose && I.talkAbout == actor && hasBegun();
    if (report) {
      I.say("\nNEXT COMBAT STEP "+this.hashCode());
    }
    //
    //  This might need to be tweaked in cases of self-defence, where you just
    //  want to see off an attacker.
    if (CombatUtils.isDowned(subject, object)) {
      if (report) I.say("  COMBAT COMPLETE");
      return null;
    }
    
    Target struck = CombatUtils.bestTarget(actor, subject, true);
    if (struck == null) struck = subject;
    
    if (report) {
      I.say("  Main target: "+this.subject);
      I.say("  Struck is: "+struck);
    }

    //  If we're not in pursuit, call off the activity when or if the enemy is
    //  in retreat.
    /*
    if ((! pursue) && CombatUtils.isFleeing(subject) && Rand.yes()) {
      return null;
    }
    //*/
    
    //  Consider using any special combat-based techniques.
    final Action technique = actor.skills.pickIndependantAction(
      struck, Technique.TRIGGER_ATTACK, this
    );
    if (technique != null) return technique;
    
    Action strike = null;
    final DeviceType DT  = actor.gear.deviceType();
    final boolean melee  = actor.gear.meleeWeapon();
    final boolean razes  = struck instanceof Venue;
    final float   danger = actor.senses.fearLevel();
    
    final String strikeAnim = DT == null ? Action.STRIKE : DT.animName;
    if (razes) {
      strike = new Action(
        actor, struck,
        this, "actionSiege",
        strikeAnim, "Razing"
      );
    }
    else {
      strike = new Action(
        actor, struck,
        this, "actionStrike",
        strikeAnim, "Striking at"
      );
    }
    
    //  Depending on the type of target, and how dangerous the area is, a bit
    //  of dancing around may be in order.
    if (melee) configMeleeAction (strike, razes, danger);
    else       configRangedAction(strike, razes, danger);
    return strike;
  }
  
  
  private void configMeleeAction(
    Action strike, boolean razes, float danger
  ) {
    ///if (eventsVerbose) I.sayAbout(actor, "Configuring melee attack.\n");
    
    final Element struck = (Element) strike.subject();
    final Stage world = actor.world();
    strike.setProperties(Action.QUICK);
    if (razes) {
      if (! Spacing.adjacent(actor, struck)) {
        strike.setMoveTarget(Spacing.nearestOpenTile(struck, actor, world));
      }
      //  TODO:  Properly incorporate dodge mechanics here.
      /*
      else if (Rand.num() < 0.2f) {
        strike.setMoveTarget(Spacing.pickFreeTileAround(struck, actor));
      }
      //*/
      else strike.setMoveTarget(actor.origin());
    }
  }
  
  
  private void configRangedAction(
    Action strike, boolean razes, float danger
  ) {
    final Activities activities = actor.world().activities;
    
    final Element struck = (Element) strike.subject();
    final float range = actor.health.sightRange();
    
    boolean underFire = activities.includesAction(actor, "actionStrike");
    boolean shouldDodge = actor.senses.hasSeen(struck);
    if (razes && Rand.num() > danger) shouldDodge = false;
    else if (Rand.yes() || Rand.num() > danger) shouldDodge = false;
    
    boolean dodged = false;
    if (shouldDodge) {
      final float distance = Spacing.distance(actor, struck) / range;
      //
      //  If not under fire, consider advancing for a clearer shot-
      if (Rand.num() < distance && ! underFire) {
        final Target AP = Retreat.pickWithdrawPoint(
          actor, range, struck, true
        );
        if (AP != null) { dodged = true; strike.setMoveTarget(AP); }
      }
      //
      //  Otherwise, consider falling back for cover-
      if (underFire && Rand.num() > distance) {
        final Target WP = Retreat.pickWithdrawPoint(
          actor, range, struck, false
        );
        if (WP != null) { dodged = true; strike.setMoveTarget(WP); }
      }
    }
    
    if (dodged) strike.setProperties(Action.QUICK | Action.TRACKS);
    else strike.setProperties(Action.RANGED | Action.QUICK | Action.TRACKS);
  }
  
  
  
  /**  Executing the action-
    */
  public boolean actionStrike(Actor actor, Actor target) {
    if (target.health.dying()) return false;
    final boolean subdue = object == OBJECT_SUBDUE;
    //
    //  TODO:  You may want a separate category for animals?  Or Psy?
    if (actor.gear.meleeWeapon()) {
      performStrike(actor, target, HAND_TO_HAND, HAND_TO_HAND, subdue);
    }
    else {
      performStrike(actor, target, MARKSMANSHIP, STEALTH_AND_COVER, subdue);
    }
    return true;
  }
  
  
  public boolean actionSiege(Actor actor, Venue target) {
    if (target.structure.destroyed()) return false;
    performSiege(actor, target);
    return true;
  }
  
  
  public static void performStrike(
    Actor actor, Actor target,
    Skill offence, Skill defence,
    boolean subdue
  ) {
    final boolean report = damageVerbose && I.talkAbout == actor;
    if (report) I.say("\n"+actor+" performing strike against "+target);
    
    //  TODO:  Move weapon/armour properties to dedicated subclasses.
    final boolean canStun = actor.gear.hasDeviceProperty(Economy.STUN);
    float penalty = 0, damage = 0;
    penalty = rangePenalty(actor, target);
    final float bypass = Visit.clamp(0 - penalty, 0, 5);
    if (subdue && ! canStun) penalty += 5;
    
    
    final boolean success = target.health.conscious() ? actor.skills.test(
      offence, target, defence, 0 - penalty, 1
    ) : true;
    
    if (report) {
      I.say("  Max. damage:    "+actor.gear.attackDamage()+", stun: "+canStun);
      I.say("  Vs. Armour:     "+target.gear.armourRating()+", pass "+bypass);
      I.say("  Range penalty:  "+penalty+", success? "+success);
    }
      
    if (success) {
      damage = actor.gear.attackDamage() * Rand.num();
      final float afterShields = target.gear.afterShields(
        damage, actor.gear.physicalWeapon()
      );
      final float
        armourSoak  = (target.gear.armourRating() * Rand.num()) - bypass,
        afterArmour = Visit.clamp(afterShields - armourSoak, 0, damage);
      
      if (report) {
        I.say("  Base damage:    "+damage      );
        I.say("  After shields:  "+afterShields);
        I.say("  Armour absorbs: "+armourSoak  );
        I.say("  Final total:    "+afterArmour );
      }
      if (damage != afterArmour) {
        final boolean hit = damage > 0;
        OutfitType.applyFX(target.gear.outfitType(), target, actor, hit);
      }
      damage = afterArmour;
    }
    
    if (damage > 0 && ! GameSettings.noBlood) {
      //  TODO:  Allow for wear and tear to weapons/armour over time.
      
      float fatDamage = 0, injDamage = 0;
      if (subdue && canStun) fatDamage = damage;
      else if (subdue || canStun) fatDamage = injDamage = damage / 2;
      else injDamage = damage;
      
      if (report) {
        I.say("  Fatigue damage: "+fatDamage);
        I.say("  Injury damage:  "+injDamage);
      }
      if (injDamage > 0) target.health.takeInjury (injDamage, false);
      if (fatDamage > 0) target.health.takeFatigue(fatDamage       );
    }
    
    CombatFX.applyFX(actor.gear.deviceType(), actor, target, success);
  }
  
  
  public static void performSiege(
    Actor actor, Venue besieged
  ) {
    final boolean report = damageVerbose && I.talkAbout == actor;
    
    boolean accurate = false;
    if (actor.gear.meleeWeapon()) {
      accurate = actor.skills.test(HAND_TO_HAND, 0, 1);
    }
    else {
      final float penalty = rangePenalty(actor, besieged);
      accurate = actor.skills.test(MARKSMANSHIP, penalty, 1);
    }
    
    float damage = actor.gear.attackDamage() * Rand.avgNums(2) * 1.5f;
    if (accurate) damage *= 1.5f;
    else damage *= 0.5f;
    
    final float armour = besieged.structure.armouring();
    damage -= armour * Rand.avgNums(2);
    damage *= 10f / (5 + armour);
    
    if (report) I.say("Armour/Damage: "+armour+"/"+damage);
    
    if (damage > 0) besieged.structure.takeDamage(damage);
    CombatFX.applyFX(actor.gear.deviceType(), actor, besieged, true);
  }
  
  
  static float rangePenalty(Actor a, Target t) {
    final boolean report = damageVerbose && I.talkAbout == a;
    
    final float range = Spacing.distance(a, t);
    if (report) {
      I.say("  Target range:   "+range);
      I.say("  Sight range:    "+a.health.sightRange());
    }
    
    return ((range / (a.health.sightRange() + 1f)) - 1) * 5;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("In combat with ");
    d.append(subject);
  }
}





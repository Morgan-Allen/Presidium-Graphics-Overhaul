/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.base.Mission;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.util.*;



public class Combat extends Plan implements Qualities {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  protected static boolean
    evalVerbose   = false,
    begunVerbose  = false,
    stepsVerbose  = false,
    damageVerbose = false;
  
  
  final public static int
    STYLE_RANGED = 0,
    STYLE_MELEE  = 1,
    STYLE_EITHER = 2,
    ALL_STYLES[] = { 0, 1, 2 },
    
    OBJECT_EITHER  = 0,
    OBJECT_SUBDUE  = 1,
    OBJECT_DESTROY = 2,
    ALL_OBJECTS[]  = { 0, 1, 2 };

  final public static String STYLE_NAMES[] = {
    "Ranged",
    "Melee" ,
    "Either"
  };
  final public static String OBJECT_NAMES[] = {
    "Neutralise ",
    "Capture "   ,
    "Destroy "   ,
  };
  
  
  final int style, object;
  final boolean pursue;
  
  
  public Combat(Actor actor, Element target) {
    this(actor, target, STYLE_EITHER, OBJECT_EITHER);
  }
  
  
  public Combat(
    Actor actor, Element target, int style, int object
  ) {
    super(actor, target, MOTIVE_EMERGENCY, REAL_HARM);
    this.style  = style ;
    this.object = object;
    this.pursue = false;
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
    return new Combat(other, (Element) subject, style, object);
  }
  
  
  
  /**  Gauging the relative strength of combatants, odds of success, and how
    *  (un)appealing an engagement would be.
    */
  final static Trait BASE_TRAITS[] = { FEARLESS, DEFENSIVE, CRUEL };
  final static Skill
    MELEE_SKILLS[]  = { HAND_TO_HAND },
    RANGED_SKILLS[] = { MARKSMANSHIP, STEALTH_AND_COVER };
  
  
  protected float getPriority() {
    int teamSize = hasMotives(MOTIVE_MISSION) ? Mission.AVG_PARTY_LIMIT : 1;
    
    if (! PlanUtils.isArmed(actor)) setCompetence(0);
    else setCompetence(successChanceFor(actor));
    
    return PlanUtils.combatPriority(
      actor, subject, motiveBonus(), teamSize, true
    );
  }
  
  
  public float successChanceFor(Actor actor) {
    int teamSize = hasMotives(MOTIVE_MISSION) ? Mission.AVG_PARTY_LIMIT : 1;
    return PlanUtils.combatWinChance(actor, subject, teamSize);
  }
  
  
  public int motionType(Actor actor) {
    if (CombatUtils.isDowned(subject, object)) return MOTION_ANY;
    final boolean
      canSee = actor.senses.awareOf(subject),
      urgent = actor.senses.isEmergency();
    
    if (urgent) {
      return MOTION_FAST;
    }
    else if (subject instanceof Venue && canSee) {
      return MOTION_FAST;
    }
    else if (subject instanceof Actor && canSee) {
      return MOTION_SNEAK;
    }
    else return super.motionType(actor);
  }
  
  

  /**  Actual behaviour implementation-
    */
  protected Behaviour getNextStep() {
    final boolean report = stepsVerbose && I.talkAbout == actor && (
      hasBegun() || ! begunVerbose
    );
    if (report) {
      I.say("\nGetting next combat step for: "+I.tagHash(this));
    }

    //
    //  Check to see if the main target is downed, and if not, see what your
    //  best current target would be:
    if (CombatUtils.isDowned(subject, object)) {
      if (report) I.say("  COMBAT COMPLETE!");
      return null;
    }
    Target struck = subject;
    if (hasBegun()) struck = CombatUtils.bestTarget(actor, subject, true);
    //
    //  If your prey has ducked indoors, consider tearing up the structure-
    //  TODO:  Look into potential 'siege' options for targets that you can't
    //  path to directly- i.e, when indoors, or going through walls.
    if (struck == subject && subject instanceof Actor) {
      final Boarding haven = ((Actor) struck).aboard();
      if (! haven.allowsEntry(actor)) {
        if (haven.base() == actor.base() || object != OBJECT_DESTROY) {
          interrupt("Prey has found sanctuary!");
          return null;
        }
        else struck = haven;
      }
    }
    
    if (report) {
      I.say("  Main target: "+this.subject);
      I.say("  Best target: "+struck);
    }
    
    //  Consider using any special combat-based techniques.
    final Action technique = actor.skills.pickIndependantAction(
      struck, Technique.TRIGGER_ATTACK, this
    );
    if (technique != null) return technique;
    
    Action strike = null;
    final String strikeAnim = strikeAnimFor(actor.gear.deviceType());
    final boolean melee     = actor.gear.meleeWeapon();
    final boolean razes     = struck instanceof Structure.Basis;
    final float   danger    = 1f - successChanceFor(actor);
    
    if (razes) {
      if (report) I.say("  Laying siege to: "+struck);
      strike = new Action(
        actor, struck,
        this, "actionSiege",
        strikeAnim, "Razing"
      );
    }
    else {
      if (report) I.say("  Striking at: "+struck);
      strike = new Action(
        actor, struck,
        this, "actionStrike",
        strikeAnim, "Striking at"
      );
    }
    
    //  Depending on the type of target, and how dangerous the area is, a bit
    //  of dancing around may be in order.
    if (melee) configMeleeAction (strike, razes, danger, report);
    else       configRangedAction(strike, razes, danger, report);
    return strike;
  }
  
  
  private String strikeAnimFor(DeviceType DT) {
    String anim = Action.STRIKE;
    if (DT != null) anim = DT.animName;
    if (anim == Action.STRIKE && Rand.yes()) anim = Action.STRIKE_BIG;
    return anim;
  }
  
  
  private void configMeleeAction(
    Action strike, boolean razes, float danger, boolean report
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
    Action strike, boolean razes, float danger, boolean report
  ) {
    final Activities activities = actor.world().activities;
    
    final Element struck   = (Element) strike.subject();
    final float   range    = actor.health.sightRange();
    final Tile    origin   = actor.origin();
    final float   distance = Spacing.distance(actor, struck) / range;
    
    boolean underFire = activities.includesActivePlan(actor, Combat.class);
    float dodgeChance = danger * 2 / (1 + distance);
    dodgeChance = Nums.clamp(dodgeChance, 0.25f, 0.75f);
    
    boolean dodged = false, shouldDodge = Rand.num() < dodgeChance;
    if (razes && Rand.yes()) shouldDodge = false;
    if (! hasBegun()) shouldDodge = false;
    
    if (shouldDodge) {
      //
      //  We try to find a good spot to hide if possible, and just pick a tile
      //  at random otherwise.  (Anything further away than max. firing range
      //  is probably pointless.)
      Target WP = Retreat.pickHidePoint(
        actor, range, struck, underFire ? -1 : 1
      );
      if (WP == null || WP == origin) {
        WP = Spacing.pickRandomTile(actor, range, actor.world());
        WP = Spacing.nearestOpenTile(WP, actor);
      }
      
      final float hideDist = Spacing.distance(WP, struck) / range;
      if (hideDist < 2) {
        //
        //  If not under fire, consider advancing for a clearer shot-
        //  Otherwise, consider falling back for cover.
        if (Rand.num() > dodgeChance || ! underFire) {
          if (hideDist > distance) WP = null;
        }
        if (WP != null && WP != origin) {
          dodged = true;
          strike.setMoveTarget(WP);
        }
      }
    }
    
    if (report) {
      I.say("\nConfiguring ranged action.");
      I.say("  Struck:       "+struck     );
      I.say("  Razing:       "+razes      );
      I.say("  Danger:       "+danger     );
      I.say("  Distance:     "+distance   );
      I.say("  Under fire?   "+underFire  );
      I.say("  Dodge chance: "+dodgeChance);
      I.say("  Should dodge? "+shouldDodge);
      I.say("  Did dodge?    "+dodged     );
      I.say("  Currently at: "+actor.origin());
      I.say("  Move target:  "+strike.movesTo());
    }
    
    if (dodged) strike.setProperties(Action.QUICK | Action.TRACKS);
    else strike.setProperties(Action.RANGED | Action.QUICK | Action.TRACKS);
  }
  
  
  
  /**  Executing the action-
    */
  public boolean actionStrike(Actor actor, Actor target) {
    if (target.health.dying()) return false;
    //
    //  TODO:  You may want a separate category for animals?  Or Psy?
    if (actor.gear.meleeWeapon()) {
      performStrike(actor, target, HAND_TO_HAND, HAND_TO_HAND, object);
    }
    else {
      performStrike(actor, target, MARKSMANSHIP, STEALTH_AND_COVER, object);
    }
    return true;
  }
  
  
  public boolean actionSiege(Actor actor, Structure.Basis target) {
    if (target.structure().destroyed()) return false;
    performSiege(actor, target);
    return true;
  }
  
  
  public static void performStrike(
    Actor actor, Actor target,
    Skill offence, Skill defence,
    int strikeType
  ) {
    final boolean report = damageVerbose && I.talkAbout == actor;
    if (report) I.say("\n"+actor+" performing strike against "+target);
    
    final boolean
      subdue = strikeType == OBJECT_SUBDUE ,
      lethal = strikeType == OBJECT_DESTROY,
      showFX = ! (actor.indoors() && target.aboard() == actor.aboard());
    
    //  TODO:  Move weapon/armour properties to dedicated subclasses.
    final boolean canStun = actor.gear.hasDeviceProperty(Economy.STUN);
    float penalty = 0, damage = 0;
    penalty = rangePenalty(actor, target);
    final float bypass = Nums.clamp(0 - penalty, 0, 5);
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
        afterArmour = Nums.clamp(afterShields - armourSoak, 0, damage);
      
      if (report) {
        I.say("  Base damage:    "+damage      );
        I.say("  After shields:  "+afterShields);
        I.say("  Armour absorbs: "+armourSoak  );
        I.say("  Final total:    "+afterArmour );
      }
      if (damage != afterArmour && showFX) {
        final boolean hit = damage > 0;
        CombatFX.applyShieldFX(target.gear.outfitType(), target, actor, hit);
      }
      damage = afterArmour;
    }
    
    if (damage > 0 && ! GameSettings.noBlood) {
      //  TODO:  Allow for wear and tear to weapons/armour over time...
      
      float fatDamage = 0, injDamage = 0;
      if (subdue && canStun) fatDamage = damage;
      else if (subdue || canStun) fatDamage = injDamage = damage / 2;
      else injDamage = damage;
      
      if (report) {
        I.say("  Fatigue damage: "+fatDamage);
        I.say("  Injury damage:  "+injDamage);
      }
      if (injDamage > 0) target.health.takeInjury (injDamage, lethal);
      if (fatDamage > 0) target.health.takeFatigue(fatDamage        );
    }
    
    if (! showFX) return;
    CombatFX.applyFX(actor.gear.deviceType(), actor, target, success);
  }
  
  
  public static void performSiege(
    Actor actor, Structure.Basis besieged
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

    if (report) {
      I.say("\nPerforming siege attack vs. "+besieged);
      I.say("  Accurate?    "+accurate);
      I.say("  Base damage: "+actor.gear.attackDamage());
    }
    
    float damage = actor.gear.attackDamage() * Rand.avgNums(2) * 2;
    final float armour = besieged.structure().armouring();
    if (accurate) damage *= 1.5f;
    else damage *= 0.5f;
    
    float afterArmour = damage;
    afterArmour -= armour * Rand.avgNums(2);
    if (Rand.num() < besieged.structure().repairLevel()) {
      afterArmour *= 5f / (5 + armour);
    }
    
    if (report) {
      I.say("  Base armour:  "+armour);
      I.say("  Damage roll:  "+damage);
      I.say("  After armour: "+afterArmour);
    }
    
    if (afterArmour > 0) besieged.structure().takeDamage(afterArmour);
    CombatFX.applyFX(actor.gear.deviceType(), actor, besieged, true);
  }
  
  
  static float rangePenalty(Actor a, Target t) {
    final boolean report = damageVerbose && I.talkAbout == a;
    if (report) I.say("\nObtaining range penalty to hit "+t);
    
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


    /*
    final boolean report = evalVerbose && I.talkAbout == actor && (
      hasBegun() || ! begunVerbose
    );
    if (CombatUtils.isDowned(subject, object)) return 0;
    float harmLevel = REAL_HARM;
    if (object == OBJECT_SUBDUE ) harmLevel = MILD_HARM   ;
    if (object == OBJECT_DESTROY) harmLevel = EXTREME_HARM;
    
    final float hostility = CombatUtils.hostileRating(actor, subject);
    final boolean melee = actor.gear.meleeWeapon();
    final boolean siege = (subject instanceof Venue);
    
    if (hostility <= 0 && motiveBonus() <= 0) return 0;
    
    float bonus = 0;
    if (siege || CombatUtils.isActiveHostile(actor, subject)) {
      bonus += PARAMOUNT;
      bonus += CombatUtils.homeDefenceBonus(actor, subject);
    }
    if (! CombatUtils.isArmed(actor)) bonus -= PARAMOUNT;
    
    final float priority = priorityForActorWith(
      actor, subject,
      ROUTINE, bonus,
      harmLevel, FULL_COOPERATION,
      REAL_FAIL_RISK, melee ? MELEE_SKILLS : RANGED_SKILLS,
      BASE_TRAITS, NORMAL_DISTANCE_CHECK,
      report
    );
    
    //  NOTE:  This may seem like a bit of kluge, but on balance it seems to
    //  result in much more sensible behaviour (unless you're a psychopath,
    //  you don't 'casually' decide to kill something.)
    float threshold = 0;
    threshold += (1 + actor.traits.relativeLevel(EMPATHIC)) / 2;
    threshold *= siege ? ROUTINE : PARAMOUNT;
    threshold *= 1 - hostility;
    if (report) {
      I.say("\n  Priority bonus:        "+bonus);
      I.say("  Hostility of subject:  "+hostility);
      I.say("  Emergency?             "+actor.senses.isEmergency());
      I.say("  Basic combat priority: "+priority);
      I.say("  Empathy threshold:     "+threshold);
      I.say("  Success chance:        "+successChanceFor(actor));
    }
    return priority >= threshold ? priority : 0;
    //*/



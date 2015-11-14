/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.PathSearch;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;



public class Combat extends Plan {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  protected static boolean
    evalVerbose   = false,
    begunVerbose  = false,
    stepsVerbose  = false,
    damageVerbose = false;
  
  
  final public static int
    STYLE_SKIRMISH = 0,
    STYLE_DEPLOYED = 1,
    STYLE_EITHER   = 2,
    ALL_STYLES[] = { 0, 1, 2 },
    
    OBJECT_EITHER  = 0,
    OBJECT_SUBDUE  = 1,
    OBJECT_DESTROY = 2,
    ALL_OBJECTS[]  = { 0, 1, 2 },
    
    STEP_INIT   = -1,
    STEP_RAZING =  0,
    STEP_FIGHT  =  1,
    STEP_COVER  =  2;

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
  private int stepType = STEP_INIT;
  
  
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
    this.style    = s.loadInt ();
    this.object   = s.loadInt ();
    this.pursue   = s.loadBool();
    this.stepType = s.loadInt ();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt (style   );
    s.saveInt (object  );
    s.saveBool(pursue  );
    s.saveInt (stepType);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Combat(other, (Element) subject, style, object);
  }
  
  
  
  /**  Gauging the relative strength of combatants, odds of success, and how
    *  (un)appealing an engagement would be.
    */
  protected float getPriority() {
    int teamSize = hasMotives(MOTIVE_MISSION) ? Mission.AVG_PARTY_LIMIT : 1;
    
    float harmLevel = REAL_HARM;
    if (object == OBJECT_SUBDUE ) harmLevel = MILD_HARM;
    if (object == OBJECT_DESTROY) harmLevel = EXTREME_HARM;
    final float priority = PlanUtils.combatPriority(
      actor, subject, motiveBonus(),
      teamSize, true, harmLevel
    );
    
    if (priority <= 0 || ! PlanUtils.isArmed(actor)) setCompetence(0);
    else setCompetence(successChanceFor(actor));
    return priority;
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
    final boolean report = I.talkAbout == actor && (
      hasBegun() || ! begunVerbose
    ) && stepsVerbose;
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
    if (hasBegun()) {
      struck = CombatUtils.bestTarget(actor, subject, true);
      if (struck == null) struck = subject;
    }
    //
    //  If your prey has ducked indoors, consider tearing up the structure...
    if (struck == subject && subject instanceof Actor) {
      final Boarding
        aboard = ((Actor) struck).aboard(),
        access = PathSearch.accessLocation(struck, actor);
      final boolean
        hasRefuge  = access == null,
        shouldRaze = object == OBJECT_DESTROY || hasMotives(MOTIVE_MISSION);
      
      if (hasRefuge) {
        if (shouldRaze && aboard != null && aboard.base() != actor.base()) {
          struck = aboard;
        }
        else {
          interrupt("Prey has found sanctuary!");
          return null;
        }
      }
    }
    
    if (report) {
      I.say("  Main target: "+this.subject);
      I.say("  Best target: "+struck);
    }
    
    Action strike = null;
    final String strikeAnim = strikeAnimFor(actor.gear.deviceType());
    final boolean melee     = actor.gear.meleeDeviceOnly();
    final boolean razes     = struck instanceof Placeable;
    final float   danger    = 1f - successChanceFor(actor);
    final Target  covers    = coverPoint(actor, struck, razes, melee, danger);
    
    if (razes) {
      if (report) I.say("  Laying siege to: "+struck);
      strike = new Action(
        actor, struck,
        this, "actionSiege",
        strikeAnim, "Razing"
      );
      stepType = STEP_RAZING;
    }
    else {
      if (report) I.say("  Striking at: "+struck);
      strike = new Action(
        actor, struck,
        this, "actionStrike",
        strikeAnim, "Striking at"
      );
      stepType = covers == null ? STEP_FIGHT : STEP_COVER;
    }
    
    //  Depending on the type of target, and how dangerous the area is, a bit
    //  of dancing around may be in order.
    if (melee) configMeleeAction (strike, razes, danger, covers, report);
    else       configRangedAction(strike, razes, danger, covers, report);
    return strike;
  }
  
  
  private String strikeAnimFor(DeviceType DT) {
    String anim = Action.STRIKE;
    if (DT != null) anim = DT.animName;
    if (anim == Action.STRIKE && Rand.yes()) anim = Action.STRIKE_BIG;
    return anim;
  }
  
  
  private Target coverPoint(
    Actor a, Target struck, boolean razes, boolean melee, float danger
  ) {
    final float range    = actor.health.sightRange();
    final float distance = Spacing.distance(actor, struck);
    
    if (melee || distance > range              ) return null;
    if (   razes  && Rand.num() < 0.9f         ) return null;
    if ((! razes) && (! a.senses.underAttack())) return null;
    
    float dodgeChance = danger * 2 / (1 + (distance / range));
    if (distance <= 1) dodgeChance = 1;
    dodgeChance = Nums.clamp(dodgeChance, 0.25f, 0.75f);
    if (Rand.num() > dodgeChance) return null;
    
    Target covers = Retreat.pickHidePoint(actor, range, struck, 1);
    if (covers != null) covers = Spacing.nearestOpenTile(covers, actor);
    if (covers == null) return null;
    
    final float hideDist = Spacing.distance(covers, struck) / range;
    if (hideDist > 1) return null;
    return covers;
  }
  
  
  private Tile formationPoint(Actor a) {
    
    //  TODO:  Test this out!
    //  Use tiles and fixed directions instead (have to rely on a grid.)
    
    Mission deployment = a.mind.mission();
    List <Actor> team = deployment.approved();
    
    Vec3D flagPoint = deployment.subjectAsTarget().position(null);
    
    Vec3D avgPos = new Vec3D(), tmp = new Vec3D();
    for (Actor t : team) avgPos.add(t.position(tmp));
    avgPos.scale(1f / team.size());
    
    Vec3D heading = tmp.setTo(flagPoint).sub(avgPos).normalise();
    int index     = team.indexOf(a);
    
    float
      rank   = (index / 4) + (team.size() / 8f),
      file   = (index % 4) - 1.5f,
      standX = flagPoint.x + (heading.x * file),
      standY = flagPoint.y + (heading.y * rank);
    
    Stage world = a.world();
    Tile stands = world.tileAt(
      Nums.clamp(standX, 0, world.size -1),
      Nums.clamp(standY, 0, world.size -1)
    );
    stands = Spacing.nearestOpenTile(stands, stands);
    
    return stands;
  }
  
  
  private void configMeleeAction(
    Action strike, boolean razes, float danger, Target cover, boolean report
  ) {
    strike.setProperties(Action.QUICK | Action.TRACKS);
    configPathPoint(strike);
  }
  
  
  private void configRangedAction(
    Action strike, boolean razes, float danger, Target cover, boolean report
  ) {
    if (cover != null) {
      strike.setMoveTarget(cover);
      strike.setProperties(Action.QUICK | Action.TRACKS);
    }
    else {
      strike.setProperties(Action.RANGED | Action.QUICK | Action.TRACKS);
    }
    configPathPoint(strike);
  }
  
  
  private void configPathPoint(Action strike) {
    final Target point = strike.movesTo();
    if (PathSearch.accessLocation(point, actor) == null) {
      final Tile free = Spacing.pickFreeTileAround(point, actor);
      if (free != null) strike.setMoveTarget(free);
      else interrupt(INTERRUPT_LOSE_PATH);
    }
  }
  
  
  
  /**  Executing the action-
    */
  public boolean actionStrike(Actor actor, Actor target) {
    return Combat.performGeneralStrike(actor, target, object, action());
  }
  
  
  public boolean actionSiege(Actor actor, Placeable target) {
    return performSiege(actor, target, action());
  }
  
  
  public static boolean performGeneralStrike(
    Actor actor, Target target, int object, Action a
  ) {
    if (target instanceof Actor) {
      if (actor.gear.meleeDeviceOnly()) return performStrike(
        actor, (Actor) target, HAND_TO_HAND, HAND_TO_HAND, object, a
      );
      else return performStrike(
        actor, (Actor) target, MARKSMANSHIP, STEALTH_AND_COVER, object, a
      );
    }
    else if (target instanceof Placeable) {
      return performSiege(actor, (Placeable) target, a);
    }
    else return false;
  }
  
  
  public static boolean performStrike(
    Actor actor, Actor target,
    Skill offence, Skill defence,
    int strikeType, Action action
  ) {
    final boolean report = damageVerbose && I.talkAbout == actor;
    if (report) I.say("\n"+actor+" performing strike against "+target);
    
    final boolean
      subdue = strikeType == OBJECT_SUBDUE ,
      lethal = strikeType == OBJECT_DESTROY,
      showFX = ! (actor.indoors() && target.aboard() == actor.aboard());
    
    final boolean phys    = actor.gear.hasDeviceProperty(Devices.KINETIC);
    final boolean canStun = actor.gear.hasDeviceProperty(Devices.STUN   );
    final boolean melee   = actor.gear.meleeDeviceOnly();
    float penalty = 0, damage = 0;
    penalty = rangePenalty(actor, target);
    
    final float bypass = Nums.clamp(0 - penalty, 0, 5);
    if (subdue && ! canStun) penalty += 5;
    
    final boolean success = target.health.conscious() ? actor.skills.test(
      offence, target, defence, 0 - penalty, 1, action
    ) : true;
    
    if (report) {
      I.say("  Max. damage:    "+actor.gear.totalDamage()+", stun: "+canStun);
      I.say("  Vs. Armour:     "+target.gear.totalArmour()+", pass "+bypass);
      I.say("  Range penalty:  "+penalty+", success? "+success);
    }
    
    if (success) {
      final float maxDamage = actor.gear.totalDamage();
      damage = maxDamage * Rand.num();
      final float
        afterShields = melee ? damage : target.gear.afterShields(damage, phys),
        shieldsTook  = damage - afterShields,
        maxArmour    = target.gear.totalArmour(),
        armourSoak   = (maxArmour * Rand.num()) - bypass,
        afterArmour  = Nums.clamp(afterShields - armourSoak, 0, damage),
        armourTook   = afterShields - afterArmour;
      
      if (report) {
        I.say("  Base damage:    "+damage      );
        I.say("  After shields:  "+afterShields);
        I.say("  Armour took:    "+armourTook  );
        I.say("  Final total:    "+afterArmour );
      }
      
      final Item used = actor .gear.deviceEquipped();
      final Item worn = target.gear.outfitEquipped();
      Item.checkForBreakdown(actor , used, armourTook / maxDamage, 10);
      Item.checkForBreakdown(target, worn, armourTook / maxArmour, 10);
      damage = afterArmour;
      
      if (shieldsTook > 0 && showFX) {
        final boolean hit = damage > 0;
        ActionFX.applyShieldFX(target.gear.outfitType(), target, actor, hit);
      }
    }
    
    if (damage > 0 && ! GameSettings.noBlood) {
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
    
    if (showFX) {
      ActionFX.applyFX(actor.gear.deviceType(), actor, target, success);
    }
    
    return success;
  }
  
  
  public static boolean performSiege(
    Actor actor, Placeable besieged, Action action
  ) {
    final boolean report = damageVerbose && I.talkAbout == actor;
    
    boolean accurate = false, success = false;;
    if (actor.gear.meleeDeviceOnly()) {
      accurate = actor.skills.test(HAND_TO_HAND, 0, 1, action);
    }
    else {
      final float penalty = rangePenalty(actor, besieged);
      accurate = actor.skills.test(MARKSMANSHIP, penalty, 1, action);
    }

    if (report) {
      I.say("\nPerforming siege attack vs. "+besieged);
      I.say("  Accurate?    "+accurate);
      I.say("  Base damage: "+actor.gear.totalDamage());
    }
    
    final float maxDamage = actor.gear.totalDamage();
    final Item implement = actor.gear.deviceEquipped();
    float damage = maxDamage * Rand.avgNums(2) * 2;
    Item.checkForBreakdown(actor, implement, damage / maxDamage, 10);
    
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
    
    if (afterArmour > 0) {
      besieged.structure().takeDamage(afterArmour);
      success = true;
    }
    ActionFX.applyFX(actor.gear.deviceType(), actor, besieged, true);
    
    return success;
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
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (stepType == STEP_INIT || stepType == STEP_FIGHT) {
      d.append("In combat with ");
    }
    if (stepType == STEP_RAZING) {
      d.append("Razing ");
    }
    if (stepType == STEP_COVER) {
      d.append("Taking cover from ");
    }
    d.append(subject);
  }
}
















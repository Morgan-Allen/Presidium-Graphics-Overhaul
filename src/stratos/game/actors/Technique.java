/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors;
import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.game.economic.*;
import stratos.graphics.common.*;
import stratos.start.*;
import stratos.user.Selectable;
import stratos.util.*;


//  TODO:  You need a more polished constructor interface for this.
/*
TECHNIQUES

Initial acquisition:
  Is natural ability.
  Is learnable.
  Is trained only.
  Is item-derived.
  
  Skills needed & min level?  Item-basis?  Other pre-reqs?

Triggers and constraints:
  Skill-passive.
  Gear-use-passive.
  Focus-targeting.
  Self-targeting.
  Free-targeting.

  Behaviour triggers- combat, retreat, dialogue, treating?
  Helpful or harmful?
  Raw materials needed?
  Fatigue/concentration costs?

Scope and duration:
  Is a condition/status.
  Is area-of-effect.
  Is sovereign power.
  
  Effects table (three components)
    channel (injury, skill, or trait?)
    amount  (3, 6-9 min-max, 50%?)
    type    (instant cost, temp bonus, regen?)
//*/


public abstract class Technique extends Constant {
  
  final public static int
    
    IS_LEARNED_NORMALLY   = 0   ,
    IS_TRAINED_ONLY       = 1   ,
    IS_GAINED_FROM_ITEM   = 2   ,
    IS_NATURAL_ONLY       = 4   ,
    
    IS_PASSIVE_SKILL_FX   = 8   ,
    IS_GEAR_PROFICIENCY   = 16  ,
    
    IS_SELF_TARGETING     = 32  ,  //  Will only affect self.
    IS_FOCUS_TARGETING    = 64  ,  //  Will affect focus of current activity.
    IS_ANY_TARGETING      = 128 ,  //  Can affect anyone or (thing) nearby.
    
    IS_SOVEREIGN_POWER    = 2048;
  
  final public static float
    MINOR_POWER          = 1.0f ,
    MEDIUM_POWER         = 3.0f ,
    MAJOR_POWER          = 5.0f ,
    NO_FATIGUE           = 0.0f ,
    MINOR_FATIGUE        = 2.0f ,
    MEDIUM_FATIGUE       = 5.0f ,
    MAJOR_FATIGUE        = 10.0f,
    NO_CONCENTRATION     = 0.0f ,
    MINOR_CONCENTRATION  = 2.0f ,
    MEDIUM_CONCENTRATION = 5.0f ,
    MAJOR_CONCENTRATION  = 8.0f ;
  
  final public static float
    NO_HARM      = Plan.NO_HARM     ,
    MILD_HARM    = Plan.MILD_HARM   ,
    REAL_HARM    = Plan.REAL_HARM   ,
    EXTREME_HARM = Plan.EXTREME_HARM,
    MILD_HELP    = Plan.MILD_HELP   ,
    REAL_HELP    = Plan.REAL_HELP   ,
    EXTREME_HELP = Plan.EXTREME_HELP;
  
  
  final static Index <Technique> INDEX = new Index <Technique> ();
  final static Table <Object, List <Technique>> BY_SOURCE = new Table();
  
  final public Class sourceClass;
  
  final public ImageAsset icon       ;
  final public String     description;
  final public String     animName   ;
  
  final private int   properties;
  final private Skill skillNeed ;
  final private int   minLevel  ;
  
  final public float
    powerLevel       ,
    harmFactor       ,
    fatigueCost      ,
    concentrationCost;
  final public Object focus;
  final public int    actionProperties;
  
  final public Condition asCondition;
  
  
  private Technique(
    String name, String iconFile,
    String description,
    Class sourceClass, String uniqueID,
    float power, float harm, float fatigue, float concentration,
    int properties, Skill skillUsed, int minLevel, Object focus,
    String animName, int actionProperties
  ) {
    super(INDEX, uniqueID, name);
    this.sourceClass = sourceClass;
    
    if (Assets.exists(iconFile)) {
      this.icon = ImageAsset.fromImage(sourceClass, iconFile);
    }
    else this.icon = null;
    this.description = description;
    this.animName    = animName   ;
    
    this.powerLevel        = power           ;
    this.harmFactor        = harm            ;
    this.fatigueCost       = fatigue         ;
    this.concentrationCost = concentration   ;
    this.focus             = focus           ;
    this.actionProperties  = actionProperties;
    
    this.properties = properties;
    this.skillNeed  = skillUsed ;
    this.minLevel   = minLevel  ;
    
    List <Technique> bySource = BY_SOURCE.get(skillUsed);
    if (bySource == null) BY_SOURCE.put(skillUsed, bySource = new List());
    bySource.add(this);
    
    this.asCondition = new Condition(
      sourceClass, name, description, iconFile, 0, 0, 0, new Table(), name
    ) {
      public void affect    (Actor a) { applyAsCondition(a); }
      public void onAddition(Actor a) { onConditionStart(a); }
      public void onRemoval (Actor a) { onConditionEnd  (a); }
    };
  }
  
  
  //  Used for active techniques.
  public Technique(
    String name, String iconFile,
    String description,
    Class sourceClass, String uniqueID,
    float power, float harm, float fatigue, float concentration,
    int properties, Skill skillUsed, int minLevel,
    String animName, int actionProperties
  ) { this(
    name, iconFile, description, sourceClass, uniqueID,
    power, harm, fatigue, concentration,
    properties, skillUsed, minLevel, null,
    animName, actionProperties
  ); }
  
  
  //  Used for passive techniques.
  public Technique(
    String name, String iconFile,
    String description,
    Class sourceClass, String uniqueID,
    float power, float harm, float fatigue, float concentration,
    int properties, Skill skillUsed, int minLevel,
    Object focus
  ) { this(
    name, iconFile, description, sourceClass, uniqueID,
    power, harm, fatigue, concentration,
    properties, skillUsed, minLevel, focus,
    Action.STAND, Action.NORMAL
  ); }
  
  
  public static Technique loadConstant(Session s) throws Exception {
    return INDEX.loadEntry(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    INDEX.saveEntry(this, s.output());
  }
  
  
  
  /**  Helper methods for determining skill-aquisition and triggering-
    */
  public static Series <Technique> learntFrom(Object source) {
    return BY_SOURCE.get(source);
  }
  
  
  public boolean canBeLearnt(Actor learns, boolean trained) {
    if (hasProperty(IS_NATURAL_ONLY)             ) return false;
    if (hasProperty(IS_TRAINED_ONLY) && ! trained) return false;
    if (skillNeed == null || minLevel <= 0       ) return true ;
    float level = learns.traits.traitLevel(skillNeed);
    level += learns.traits.bonusFrom(skillNeed.parent);
    return level >= minLevel;
  }
  
  
  public boolean hasProperty(int prop) {
    return (properties & prop) == prop;
  }
  
  
  public boolean triggersAction(
    Actor actor, Plan current, Target subject
  ) {
    return current.getClass() == focus && subject == current.subject();
  }
  
  
  public boolean triggersPassive(
    Actor actor, Plan current, Skill used, Target subject
  ) {
    return used == focus;
  }
  
  
  public float basePriority(
    Actor actor, Plan current, Target subject
  ) {
    //
    //  Techniques become less attractive based on the fraction of fatigue or
    //  concentration they would consume.
    final boolean report = I.talkAbout == actor && ActorSkills.techsVerbose;
    final float
      conCost = concentrationCost / actor.health.concentration(),
      fatCost = fatigueCost       / actor.health.fatigueLimit ();
    if (report) I.say("  Con/Fat costs: "+conCost+"/"+fatCost);
    if (conCost > 1 || fatCost > 1) return 0;
    //
    //  Don't use a harmful technique against a subject you want to help, and
    //  try to avoid extreme harm against subjects you only want to subdue, et
    //  cetera.
    final float hostility = Nums.clamp(PlanUtils.combatPriority(
      actor, subject, 0, 1, false, Plan.REAL_HARM
    ) / Plan.PARAMOUNT, EXTREME_HELP, EXTREME_HARM);
    if (harmFactor >  0 && hostility <= 0) return 0;
    if (harmFactor <= 0 && hostility >  0) return 0;
    float rating = 10;
    rating -= Nums.abs(harmFactor - hostility) * 10;
    rating *= ((1 - conCost) + (1 - fatCost)) / 2f;
    rating = powerLevel * rating / 10f;
    if (report) I.say("  Overall rating: "+rating);
    return rating;
  }
  
  
  
  /**  Basic interface and utility methods for use and evaluation of different
    *  techniques-
    */
  public void applyEffect(
    Actor using, boolean success, Target subject, boolean passive
  ) {
    using.health.takeFatigue      (fatigueCost      );
    using.health.takeConcentration(concentrationCost);
  }
  
  
  protected Action createActionFor(Plan parent, Actor actor, Target subject) {
    final Action action = new Action(
      actor, subject,
      this, "actionUseTechnique",
      animName, "Using "+name
    );
    action.setProperties(actionProperties);
    action.setPriority(parent.priorityFor(actor));
    return action;
  }
  
  
  public boolean actionUseTechnique(Actor actor, Target subject) {
    final boolean success = checkActionSuccess(actor, subject);
    final Plan plan = (Plan) I.cast(actor.mind.topBehaviour(), Plan.class);
    
    final float radius = effectRadius();
    final boolean desc = effectDescriminates();
    if (radius <= 0) {
      applyEffect(actor, success, subject, false);
    }
    else for (Target caught : PlanUtils.subjectsInRange(subject, radius)) {
      if (desc && basePriority(actor, plan, subject) <= 0) continue;
      applyEffect(actor, success, caught, false);
    }
    return success;
  }
  
  
  protected boolean checkActionSuccess(Actor actor, Target subject) {
    if (skillNeed == null) return true;
    else return actor.skills.test(skillNeed, minLevel, 1, null);
  }
  
  
  protected void onConditionStart(Actor affected) {
    //  TODO:  Fill this in?
  }
  
  
  protected void onConditionEnd(Actor affected) {
    //  TODO:  Fill this in?
  }
  
  
  protected void applyAsCondition(Actor affected) {
    if (affected.traits.traitLevel(asCondition) > 0) {
      affected.traits.incLevel(asCondition, -1f / conditionDuration());
      return;
    }
    else affected.traits.setLevel(asCondition, 0);
  }
  
  
  public float passiveBonus(Actor using, Skill skill, Target subject) {
    return 0;
  }
  
  
  protected float conditionDuration() {
    return Stage.STANDARD_HOUR_LENGTH;
  }
  
  
  protected float effectRadius() {
    return 0;
  }
  
  
  protected boolean effectDescriminates() {
    return false;
  }
  
  
  public boolean isPower() {
    return hasProperty(IS_SOVEREIGN_POWER) && (this instanceof Power);
  }
  
  
  public Traded allowsUse() {
    if (! hasProperty(IS_GEAR_PROFICIENCY)) return null;
    if (focus instanceof Traded) return (Traded) focus;
    else return null;
  }
  
  
  public Traded itemNeeded() {
    return null;
  }
  
  
  public boolean isPassive() {
    return hasProperty(IS_PASSIVE_SKILL_FX);
  }
  
  
  public boolean isItemDerived() {
    return hasProperty(IS_GAINED_FROM_ITEM) && itemNeeded() != null;
  }
  
  
  public boolean targetsSelf() {
    return hasProperty(IS_SELF_TARGETING);
  }
  
  
  public boolean targetsFocus() {
    return hasProperty(IS_FOCUS_TARGETING);
  }
  
  
  public boolean targetsAny() {
    return hasProperty(IS_ANY_TARGETING);
  }
  

  
  /**  Other static helper methods:
    */
  //  TODO:  Move these to other helper classes!
  
  public static boolean isDoingAction(Actor actor, Technique used) {
    final Action taken = actor.currentAction();
    return taken != null && taken.basis == used;
  }
  
  
  public static Action currentTechniqueBy(Actor actor) {
    final Action taken = actor.currentAction();
    if (taken == null || ! (taken.basis instanceof Technique)) return null;
    return taken;
  }
  
  
  protected static boolean hasUpgrade(Target v, Upgrade upgrade, int level) {
    if (! (v instanceof Venue)) return false;
    final Structure s = ((Venue) v).structure();
    return s.upgradeLevel(upgrade, Structure.STATE_INTACT) >= level;
  }
  
  
  protected static boolean hasGear(Actor actor, Traded gearType) {
    if (actor.gear.deviceType() == gearType) return true;
    if (actor.gear.outfitType() == gearType) return true;
    return false;
  }
  
  
  protected static float roll(float min, float max) {
    return min + (Rand.num() * (max - min));
  }
  
  
  
  /**  Rendering, interface and printout methods-
    */
  public String toString() {
    return "{"+name+"}";
  }
  
  
  public static void describeAction(
    Action techniqueUse, Actor actor, Description d
  ) {
    d.append("Using ");
    d.append(techniqueUse.basis);
    d.append(" on ");
    d.append(techniqueUse.subject());
  }
  
  
  public void describeHelp(Description d, Selectable prior) {
    substituteReferences(description, d);
    d.append("\n");
    
    if (hasProperty(IS_NATURAL_ONLY)) {
      d.append("\n  Instinctive");
    }
    else if (skillNeed != null && minLevel > 0) {
      d.append("\n  Minimum ");
      d.append(skillNeed);
      d.append(": "+minLevel);
    }
    if (hasProperty(IS_TRAINED_ONLY)) {
      d.append("\n  Trained Only");
    }
  }
}














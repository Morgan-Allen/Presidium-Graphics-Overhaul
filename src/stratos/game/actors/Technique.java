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
import stratos.util.*;



//  TODO:  You need to handle area-of-effect as well.


public abstract class Technique extends Index.Entry
  implements Session.Saveable
{
  
  final public static int
    TYPE_PASSIVE_SKILL_FX   = 0,
    TYPE_GEAR_PROFICIENCY   = 1,
    TYPE_INDEPENDANT_ACTION = 2,
    TYPE_SOVEREIGN_POWER    = 3;
  
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
  
  final public String     name    ;
  final public ImageAsset icon    ;
  final public String     animName;
  
  final public int    type     ;
  final public Skill  skillNeed;
  final public int    minLevel ;
  
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
    Class sourceClass, String uniqueID,
    float power, float harm, float fatigue, float concentration,
    int type, Skill skillUsed, int minLevel, Object focus,
    String animName, int actionProperties
  ) {
    super(INDEX, uniqueID);
    this.name     = name    ;
    this.animName = animName;
    
    if (Assets.exists(iconFile)) {
      this.icon = ImageAsset.fromImage(sourceClass, iconFile);
    }
    else this.icon = null;
    
    this.sourceClass = sourceClass;
    
    this.powerLevel        = power           ;
    this.harmFactor        = harm            ;
    this.fatigueCost       = fatigue         ;
    this.concentrationCost = concentration   ;
    this.focus             = focus           ;
    this.actionProperties  = actionProperties;
    
    this.type      = type     ;
    this.skillNeed = skillUsed;
    this.minLevel  = minLevel ;
    
    List <Technique> bySource = BY_SOURCE.get(skillUsed);
    if (bySource == null) BY_SOURCE.put(skillUsed, bySource = new List());
    bySource.add(this);
    
    this.asCondition = new Condition(name, false, name) {
      public void affect    (Actor a) { applyAsCondition(a); }
      public void onAddition(Actor a) { onConditionStart(a); }
      public void onRemoval (Actor a) { onConditionEnd  (a); }
    };
  }
  
  
  //  Used for active techniques.
  public Technique(
    String name, String iconFile, Class sourceClass, String uniqueID,
    float power, float harm, float fatigue, float concentration,
    int type, Skill skillUsed, int minLevel,
    String animName, int actionProperties
  ) { this(
    name, iconFile, sourceClass, uniqueID,
    power, harm, fatigue, concentration,
    type, skillUsed, minLevel, null,
    animName, actionProperties
  ); }
  
  
  //  Used for passive techniques.
  public Technique(
    String name, String iconFile, Class sourceClass,
    String uniqueID, float power,
    float harm, float fatigue,
    float concentration, int type,
    Skill skillUsed, int minLevel,
    Object focus
  ) { this(
    name, iconFile, sourceClass, uniqueID,
    power, harm, fatigue, concentration,
    type, skillUsed, minLevel, focus,
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
  
  
  public boolean canBeLearnt(Actor learns) {
    float level = learns.traits.traitLevel(skillNeed);
    level += learns.traits.bonusFrom(skillNeed.parent);
    return level >= minLevel;
  }
  
  
  public boolean triggeredBy(
    Actor actor, Plan current, Action action, Skill used, boolean passive
  ) {
    if (passive && type == TYPE_PASSIVE_SKILL_FX) {
      return used == focus;
    }
    else {
      return false;
    }
  }
  
  
  public float priorityFor(Actor actor, Target subject, float harmWanted) {
    //
    //  Techniques become less attractive based on the fraction of fatigue or
    //  concentration they would consume.
    final boolean report = ActorSkills.techsVerbose && I.talkAbout == actor;
    final float
      conCost = concentrationCost / actor.health.concentration(),
      fatCost = fatigueCost       / actor.health.fatigueLimit() ;
    if (report) I.say("  Con/Fat costs: "+conCost+"/"+fatCost);
    if (conCost > 1 || fatCost > 1) return 0;
    //
    //  Don't use a harmful technique against a subject you want to help, and
    //  try to avoid extreme harm against subjects you only want to subdue, et
    //  cetera.
    if (harmFactor >  0 && harmWanted <= 0) return 0;
    if (harmFactor <= 0 && harmWanted >  0) return 0;
    float rating = 10;
    rating -= Nums.abs(harmWanted - harmFactor);
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
    applyEffect(actor, success, subject, false);
    return success;
  }
  
  
  protected boolean checkActionSuccess(Actor actor, Target subject) {
    if (skillNeed == null) return true;
    else return actor.skills.test(skillNeed, minLevel, 1, null);
  }
  
  
  protected void applyAsCondition(Actor affected) {
    if (affected.traits.traitLevel(asCondition) > 0) {
      affected.traits.incLevel(asCondition, -1f / conditionDuration());
      return;
    }
    else affected.traits.setLevel(asCondition, 0);
  }
  
  
  protected void onConditionStart(Actor affected) {
  }
  
  
  protected void onConditionEnd(Actor affected) {
  }
  
  
  protected float conditionDuration() {
    return Stage.STANDARD_HOUR_LENGTH;
  }
  
  
  public float passiveBonus(Actor using, Skill skill, Target subject) {
    return 0;
  }
  
  
  public Traded allowsUse() {
    if (focus instanceof Traded) return (Traded) focus;
    else return null;
  }
  

  
  /**  Other static helper methods:
    */
  public static boolean isDoingAction(Actor actor, Technique used) {
    final Action taken = actor.currentAction();
    return taken != null && taken.basis == used;
  }
  
  
  public static Action currentTechniqueBy(Actor actor) {
    final Action taken = actor.currentAction();
    if (taken == null || ! (taken.basis instanceof Technique)) return null;
    return taken;
  }
  
  
  public static Series <Actor> subjectsInRange(Target point, float radius) {
    final Batch <Actor> subjects = new Batch();
    final Vec3D centre = point.position(null);
    final Box2D area = new Box2D(centre.x, centre.y, 0, 0);
    area.expandBy(Nums.round(radius + point.radius(), 1, true));
    
    for (Tile t : point.world().tilesIn(area, true)) {
      for (Mobile m : t.inside()) {
        if (Spacing.distance(m, point) > radius) continue;
        if (! (m instanceof Actor)) continue;
        subjects.add((Actor) m);
      }
    }
    return subjects;
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
  
  
  
  /**  Rendering, interface and printout methods-
    */
  public String toString() {
    return name;
  }
  
  
  public static void describeAction(
    Action techniqueUse, Actor actor, Description d
  ) {
    d.append("Using ");
    d.append(techniqueUse.basis);
    d.append(" on ");
    d.append(techniqueUse.subject());
  }
}










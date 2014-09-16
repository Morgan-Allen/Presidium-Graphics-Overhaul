

package stratos.game.actors;
import org.apache.commons.math3.util.FastMath;

import stratos.game.common.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.start.Assets;
import stratos.util.*;





public abstract class Technique implements Session.Saveable {
  
  
  final public static int
    TYPE_PASSIVE_EFFECT   = 0,
    TYPE_COMBINED_ACTION  = 1,
    TYPE_EXCLUSIVE_ACTION = 2;
  final public static int
    SOURCE_SKILL  = 2,
    SOURCE_ITEM   = 1,
    SOURCE_SECRET = 0;
  final public static Object
    TRIGGER_ATTACK = new Object(),
    TRIGGER_DEFEND = new Object(),
    TRIGGER_MOTION = new Object();
  
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
    MAJOR_CONCENTRATION  = 10.0f;
  
  final public static float
    NO_HARM      = Plan.NO_HARM     ,
    MILD_HARM    = Plan.MILD_HARM   ,
    REAL_HARM    = Plan.REAL_HARM   ,
    EXTREME_HARM = Plan.EXTREME_HARM,
    MILD_HELP    = Plan.MILD_HELP   ,
    REAL_HELP    = Plan.REAL_HELP   ,
    EXTREME_HELP = Plan.EXTREME_HELP;
  
  
  final public Class sourceClass;
  final public int   uniqueID   ;
  
  final public String     name    ;
  final public ImageAsset icon    ;
  final public String     animName;
  
  final public int    type      ;
  final public Object source    ;
  final public Object triggers[];
  final public int    minLevel  ;
  
  final public float
    powerLevel       ,
    harmFactor       ,
    fatigueCost      ,
    concentrationCost;
  
  final public Condition asCondition = new Condition() {
    public void affect(Actor a) { applyAsCondition(a); }
  };
  
  
  public Technique(
    String name, String iconFile, String animName,
    Class sourceClass, int uniqueID,
    float power, float harm, float fatigue, float concentration,
    int type, Object source, int minLevel, Object... triggers
  ) {
    this.name     = name    ;
    this.animName = animName;
    
    if (Assets.exists(iconFile)) {
      this.icon = ImageAsset.fromImage(iconFile, sourceClass);
    }
    else this.icon = null;
    
    this.sourceClass = sourceClass;
    this.uniqueID    = uniqueID   ;
    
    this.powerLevel        = power        ;
    this.harmFactor        = harm         ;
    this.fatigueCost       = fatigue      ;
    this.concentrationCost = concentration;
    
    this.type     = type    ;
    this.source   = source  ;
    this.minLevel = minLevel;
    this.triggers = triggers;
    
    if (ATT.get(uniqueID) != null) I.complain(
      "NON-UNIQUE TECHNIQUE ID: "+uniqueID
    );
    else {
      ATT.put(uniqueID, this);
      allTechniques.add(this);
      ATA = null;
    }
  }
  
  
  private static Batch <Technique> allTechniques = new Batch();
  private static Technique ATA[] = null;
  private static Table <Integer, Technique> ATT = new Table();
  
  
  public static Technique[] ALL_TECHNIQUES() {
    if (ATA != null) return ATA;
    ATA = allTechniques.toArray(Technique.class);
    return ATA;
  }
  
  
  public static Technique loadConstant(Session s) throws Exception {
    s.loadClass();
    return ATT.get(s.loadInt());
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveClass(sourceClass);
    s.saveInt(uniqueID);
  }
  
  
  
  /**  Basic interface and utility methods for use and evaluation of different
    *  techniques-
    */
  public abstract float applyEffect(Actor using, Behaviour b, Action a);
  
  
  public boolean actionUseTechnique(Actor actor, Target subject) {
    applyEffect(actor, actor.mind.rootBehaviour(), actor.currentAction());
    actor.health.takeFatigue      (fatigueCost      );
    actor.health.takeConcentration(concentrationCost);
    return true;
  }
  
  
  protected float appealFor(Actor actor, Target subject, float harmLevel) {
    //
    //  Techniques become less attractive based on the fraction of fatigue or
    //  concentration they would consume.
    final float
      conCost = concentrationCost / actor.health.concentration(),
      fatCost = fatigueCost       / actor.health.fatigueLimit() ;
    if (conCost > 1 || fatCost > 1) return 0;
    //
    //  Don't use a harmful technique against a subject you want to help, and
    //  try to avoid extreme harm against subjects you only want to subdue, et
    //  cetera.
    if (harmLevel * harmFactor <= 0) return 0;
    float rating = 10;
    rating -= FastMath.abs(harmLevel - harmFactor);
    rating *= ((1 - conCost) + (1 - fatCost)) / 2f;
    return powerLevel * rating / 10f;
  }
  
  
  protected boolean canApply(Actor using, Behaviour b) {
    return true;
  }
  
  
  protected void configAction(Action action) {
    action.setProperties(Action.RANGED | Action.QUICK);
  }
  
  
  protected void applyAsCondition(Actor affected) {
    if (affected.traits.traitLevel(asCondition) > 0) {
      affected.traits.incLevel(asCondition, -1f / conditionDuration());
      return;
    }
    else affected.traits.setLevel(asCondition, 1);
  }
  
  
  protected float conditionDuration() {
    return World.STANDARD_HOUR_LENGTH;
  }
  
  
  public static Action pickCombatAction(
    Actor actor, Target struck, Combat combat
  ) {
    Technique picked = null;
    float bestAppeal = 0;
    
    for (Technique t : actor.skills.known) {
      if (Visit.arrayIncludes(t.triggers, TRIGGER_ATTACK)) {
        if (! t.canApply(actor, combat)) continue;
        final float appeal = t.appealFor(actor, struck, combat.harmFactor());
        if (appeal > bestAppeal) { bestAppeal = appeal; picked = t; }
      }
    }
    if (picked == null) return null;
    
    final Action action = new Action(
      actor, struck,
      picked, "actionUseTechnique",
      picked.animName, "Using "+picked.name
    );
    picked.configAction(action);
    action.setPriority(bestAppeal);
    return action;
  }
  
  
  
  /**  Utility methods for storing values associated with a given source for
    *  the technique (e.g, ability cooldowns, previous targets, etc.)  Also
    *  tracks acquisition of the technique.
    */
  protected void storeVal(Actor actor, float val, String key) {
    actor.skills.storeDatumFor(this, key, null, val);
  }
  
  
  protected void storeRef(Actor actor, Object ref, String key) {
    actor.skills.storeDatumFor(this, key, (Session.Saveable) ref, -1);
  }
  
  
  protected float valStored(Actor actor, String key) {
    return actor.skills.datumValFor(this, key);
  }
  
  
  protected Object refStored(Actor actor, String key) {
    return actor.skills.datumRefFor(this, key);
  }
  
  
  protected float valBeforeStore(Actor actor, float val, String key) {
    final float before = valStored(actor, key);
    storeVal(actor, val, key);
    return before;
  }
  
  
  protected Object refBeforeStore(Actor actor, Object ref, String key) {
    final Object before = refStored(actor, key);
    storeRef(actor, (Session.Saveable) ref, key);
    return before;
  }
}






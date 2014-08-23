

package stratos.game.actors;
import stratos.game.common.*;
//import stratos.game.actors.*;

import stratos.graphics.common.*;
import stratos.start.Assets;
import stratos.util.*;


/*  Techniques are obtained either from skills or from certain equipped items,
 *  and either confer bonuses to certain action-attempts, or allow the actor to
 *  pull off special moves under those circumstances.  They also count as a
 *  form of behaviour.
 *  
 *  This can probably be unified with the Power class.  (And maybe Upgrades?)
 */

//  Input (trigger.)  Combat, travel, skill, behaviour.
//  Output (effects.)  Damage, bonus, side-effects.
//  Appeal (AI basis.)  Benefits, cost, attention needed.
//  Source (learning curve.)  Either a skill, an item, or secret.

//  Okay.  You specify certain triggers.  Give it an interface to modify.
//  Allow it to create side-effects if needed.

//  And if it's an active, attention-consuming technique, then you may have to
//  roll against the skill involved to see if there's any effect.  You may
//  also have to rate which of several possible techniques is the most
//  attractive.  However, some techniques are simply passive.



//  TODO:  Saving and loading techniques is causing corruption of save files.
//         Find out why.

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
  
  
  final public String     name;
  final public ImageAsset icon;
  
  final public Class sourceClass;
  final public int   uniqueID   ;
  
  final public int    type      ;
  final public Object source    ;
  final public int    minLevel  ;
  final public Object triggers[];
  
  
  public Technique(
    String name, String iconFile,
    Class sourceClass, int uniqueID,
    int type, Object source, int minLevel, Object... triggers
  ) {
    this.name = name;
    
    if (Assets.exists(iconFile)) {
      this.icon = ImageAsset.fromImage(iconFile, sourceClass);
    }
    else this.icon = null;
    
    this.sourceClass = sourceClass;
    this.uniqueID    = uniqueID   ;
    
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
  public abstract float applyBonus(Actor using, Behaviour b, Action a);
  //  TODO:  Also rate appeal, and model effects as a persistent condition.
  
  
  
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






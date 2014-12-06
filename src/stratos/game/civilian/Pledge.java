


package stratos.game.civilian;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.util.*;



/*
 *  Reward types should include (in practice this should be used in foreign
 *  negotiations as well)-
 *  
 *  Cash Payment or Goods
 *  Personnel or Captives
 *  Support for Legislation
 *  Joining a Mission (Strike/Recover/Secure, Recon/Intel, Diplomacy)
 *  
 *  Permission to Marry
 *  Promotion and Demotion
 *  Pardon or Arrest
 *  Truce, Allegiance or Fealty
 */
public class Pledge implements Session.Saveable {
  
  
  private static boolean
    evalVerbose = false;
  
  public static enum Type {
    
    PAYMENT,
    PROMOTION,
    ALLOW_MARRIAGE,
    SUPPORT_BILL,
    
    TRUCE,
    ALLIANCE,
    PATRONAGE,
    FEALTY
  }
  
  
  final Type type;
  final float amount;
  final Session.Saveable refers;
  final Accountable madeTo;

  

  public Pledge(Type type, Accountable madeTo) {
    this(type, -1, null, madeTo);
  }
  
  
  public Pledge(Type type, float amount, Accountable madeTo) {
    this(type, amount, null, madeTo);
  }
  

  public Pledge(Type type, Session.Saveable refers, Accountable madeTo) {
    this(type, -1, refers, madeTo);
  }
  
  
  protected Pledge(
    Type type, float amount, Session.Saveable refers, Accountable madeTo
  ) {
    this.type = type;
    this.amount = amount;
    this.refers = refers;
    this.madeTo = madeTo;
  }
  
  
  public Pledge(Session s) throws Exception {
    s.cacheInstance(this);
    this.type = Type.values()[s.loadInt()];
    this.amount = s.loadFloat();
    this.refers = s.loadObject();
    this.madeTo = (Accountable) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(type.ordinal());
    s.saveFloat(amount);
    s.saveObject(refers);
    s.saveObject((Session.Saveable) madeTo);
  }
  
  
  
  /**  Utility methods for calculating the appeal of different motivations.
    */
  public static float greedPriority(Actor actor, float creditsPerDay) {
    if (creditsPerDay <= 0) return 0;
    final boolean report = evalVerbose && I.talkAbout == actor;
    
    final float greed = 1 + actor.traits.relativeLevel(Qualities.ACQUISITIVE);
    final Profile p = actor.base().profiles.profileFor(actor);
    
    float baseUnit = actor.gear.credits();
    baseUnit += (100 + p.salary()) / 2;
    baseUnit /= Backgrounds.NUM_DAYS_PAY;
    
    float mag = 1f + (creditsPerDay / baseUnit);
    mag = Nums.log(2, mag) * greed;
    
    final float level;
    if (mag <= 1) level = mag * Plan.ROUTINE;
    else          level = mag + Plan.ROUTINE - 1;
    
    if (report) {
      I.say("\nEvaluating greed value of "+creditsPerDay+" credits.");
      I.say("  Salary: "+p.salary()+", credits: "+actor.gear.credits());
      I.say("  Pay interval: "+Backgrounds.NUM_DAYS_PAY+", greed: "+greed);
      I.say("  Base unit: "+baseUnit+", magnitude: "+mag);
      I.say("  Final level: "+level);
    }
    return level;
  }
  
  
  
  /**  UI and interface methods-
    */
}





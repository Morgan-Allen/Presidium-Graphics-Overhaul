/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.actors;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.politic.*;
import stratos.util.*;



public class Profile {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  //  TODO:  I'm only going to implement about half of these for now- the rest
  //  are intended for expanded content.
  public static enum Crime {
    CRIME_ASSAULT     ,
    CRIME_THEFT       ,
    CRIME_DESERTION   ,
    CRIME_CORRUPTION  ,
    //  TODO:  Include vice, false evidence, gene-crime and tek-crime.
  };
  public static enum Sentence {
    SENTENCE_CENSURE  ,
    SENTENCE_BEATING  ,
    SENTENCE_DEMOTION ,
    SENTENCE_CAPTIVITY,
    //  TODO:  Include rehab, arena combat, penal labour, and execution.
  }
  final static String OFFENCE_DESCRIPTIONS[] = {
    "Assault"            ,
    "Theft and Smuggling",
    "Desertion of Duty"  ,
    "Corruption"         ,
  };
  final static String SENTENCE_DESCRIPTIONS[] = {
    "Censure"            ,
    "Beating"            ,
    "Demotion"           ,
    "Captivity"          ,
  };
  
  
  final public Actor actor;
  float paymentDue    =  0;
  float lastWageEval  = -1;
  
  float lastPsychEval = -1;
  float offenderScore =  0;
  Sentence sentenced  = null;
  
  //TODO:  USE SENTENCINGS TO DISCOURAGE CRIMINAL ACTS!
  
  
  
  public Profile(Actor actor, BaseProfiles bP) {
    this.actor = actor;
    
    //  TODO:  Don't set this here.
    if (bP != null) lastWageEval = bP.base.world.currentTime();
  }
  
  
  public static Profile loadProfile(Session s) throws Exception {
    final Profile p = new Profile((Actor) s.loadObject(), null);
    p.paymentDue    = s.loadFloat();
    p.lastWageEval  = s.loadFloat();
    p.offenderScore = s.loadFloat();
    p.lastPsychEval = s.loadFloat();
    p.sentenced     = (Sentence) s.loadEnum(Sentence.values());
    return p;
  }
  
  
  public static void saveProfile(Profile p, Session s) throws Exception {
    s.saveObject(p.actor        );
    s.saveFloat (p.paymentDue   );
    s.saveFloat (p.lastWageEval );
    s.saveFloat (p.offenderScore);
    s.saveFloat (p.lastPsychEval);
    s.saveEnum  (p.sentenced    );
  }
  
  
  
  /**  Psych evaluation-
    */
  public float daysSincePsychEval(Stage world) {
    ///I.sayAbout(actor, "Last time: "+lastPsychEval);
    final float interval;
    if (lastPsychEval == -1) interval = Stage.STANDARD_YEAR_LENGTH;
    else interval = world.currentTime() - lastPsychEval;
    return interval / Stage.STANDARD_DAY_LENGTH;
  }
  
  
  public void setPsychEvalTime(float time) {
    lastPsychEval = time;
  }
  
  
  public void incOffenderScore(float scoreInc) {
    offenderScore += scoreInc;
    if (offenderScore < 0) offenderScore = 0;
  }
  
  
  
  /**  Criminal record-
    */
  public Sentence openSentence() {
    return sentenced;
  }
  
  
  
  /**  Wages and payments due-
    */
  public float salary() {
    //
    //  TODO:  This needs to be negotiated, or at least modified, based on
    //  reluctance to settle or personal dislike.
    if (actor.vocation() == null) return 0;
    final int standing = actor.vocation().standing;
    if (standing == Backgrounds.CLASS_NATIVE) return 0;
    
    //  TODO:  Reconsider this.
    /*
    //
    //  Rulers draw directly on the finances of the state if they have any
    //  particular shortage of funds.
    if (standing == Backgrounds.CLASS_STRATOI) {
      if (actor.gear.credits() < Audit.RULER_STIPEND) {
        return Audit.RULER_STIPEND - actor.gear.credits();
      }
      else return 0;
    }
    //*/
    return Backgrounds.HIRE_COSTS[standing] / 5f;
  }
  
  
  public float paymentDue() {
    return paymentDue;
  }
  
  
  public void incPaymentDue(float inc) {
    paymentDue += inc;
  }
  
  
  public float daysSincePayment(Stage world) {
    final float interval = world.currentTime() - lastWageEval;
    return interval / Stage.STANDARD_DAY_LENGTH;
  }
  
  
  public void clearWages(Stage world) {
    paymentDue = 0;
    lastWageEval = world.currentTime();
  }
}





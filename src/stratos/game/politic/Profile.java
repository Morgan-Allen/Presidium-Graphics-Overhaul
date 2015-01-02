/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.politic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.util.*;
import static stratos.game.politic.LawUtils.*;



public class Profile {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final public Actor actor;
  float paymentDue    =  0;
  float lastWageEval  = -1;
  
  float lastPsychEval = -1;
  Summons sentence;
  List <Crime> offences = new List <Crime> ();
  
  
  
  public Profile(Actor actor, BaseProfiles bP) {
    this.actor = actor;
    
    //  TODO:  Don't set this here.
    if (bP != null) lastWageEval = bP.base.world.currentTime();
  }
  
  
  public static Profile loadProfile(Session s) throws Exception {
    final Profile p = new Profile((Actor) s.loadObject(), null);
    p.paymentDue    = s.loadFloat();
    p.lastWageEval  = s.loadFloat();
    p.lastPsychEval = s.loadFloat();
    p.sentence      = (Summons) s.loadObject();
    for (int n = s.loadInt(); n-- > 0;) {
      p.offences.add((Crime) s.loadEnum(Crime.values()));
    }
    return p;
  }
  
  
  public static void saveProfile(Profile p, Session s) throws Exception {
    s.saveObject(p.actor        );
    s.saveFloat (p.paymentDue   );
    s.saveFloat (p.lastWageEval );
    s.saveFloat (p.lastPsychEval);
    s.saveObject(p.sentence     );
    s.saveInt(p.offences.size());
    for (Crime c : p.offences) s.saveEnum(c);
  }
  
  
  
  /**  Psych evaluation and criminal record-
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
  
  
  public void recordOffence(Crime crime) {
    offences.add(crime);
  }
  
  
  public void clearRecord() {
    offences.clear();
  }
  
  
  public Summons openSentence() {
    return sentence;
  }
  
  
  public void assignSentence(Summons sentence) {
    this.sentence = sentence;
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
    return Backgrounds.HIRE_COSTS[standing];
  }
  
  
  public float paymentDue() {
    return paymentDue;
  }
  
  
  public void incPaymentDue(float inc) {
    paymentDue += inc;
  }
  
  
  public float daysSincePayAssess(Stage world) {
    final float interval = world.currentTime() - lastWageEval;
    return interval / Stage.STANDARD_DAY_LENGTH;
  }
  
  
  public void clearWages(Stage world) {
    paymentDue = 0;
    lastWageEval = world.currentTime();
  }
}





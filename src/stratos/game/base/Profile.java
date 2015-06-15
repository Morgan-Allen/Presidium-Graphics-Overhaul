/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.util.*;
import static stratos.game.base.LawUtils.*;



public class Profile {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final BaseProfiles profiles;
  final public Actor actor;
  
  float paymentDue    =  0;
  float lastWageEval  = -1;
  float lastTaxEval   = -1;
  float lastPsychEval = -1;
  Summons sentence;
  List <Crime> offences = null;
  
  
  
  public Profile(Actor actor, BaseProfiles bP) {
    this.profiles = bP;
    this.actor = actor;
  }
  
  
  public static Profile loadProfile(
    Session s, BaseProfiles bP
  ) throws Exception {
    final Profile p = new Profile((Actor) s.loadObject(), bP);
    p.paymentDue    = s.loadFloat();
    p.lastWageEval  = s.loadFloat();
    p.lastTaxEval   = s.loadFloat();
    p.lastPsychEval = s.loadFloat();
    p.sentence      = (Summons) s.loadObject();
    
    if (p.offences == null) p.offences = new List();
    p.offences = (List) s.loadEnums(p.offences, Crime.values());
    return p;
  }
  
  
  public static void saveProfile(Profile p, Session s) throws Exception {
    s.saveObject(p.actor        );
    s.saveFloat (p.paymentDue   );
    s.saveFloat (p.lastWageEval );
    s.saveFloat (p.lastTaxEval  );
    s.saveFloat (p.lastPsychEval);
    s.saveObject(p.sentence     );
    s.saveEnums (p.offences     );
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
    if (offences == null) offences = new List();
    offences.add(crime);
  }
  
  
  public void clearRecord() {
    offences.clear();
    offences = null;
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
    //  reluctance to settle or personal dislike.  TODO:  Unify this with the
    //  FindWork / Career methods on the subject.
    final Background vocation = actor.mind.vocation();
    if (vocation == null || actor.mind.work() == null) return 0;
    //
    //  The ruler and his/her household have direct access to the funds of the
    //  state, so they get somewhat different treatment.
    final Actor ruler = actor.base().ruler();
    float mult = 1;
    if (actor == ruler) {
      mult = 0;
    }
    else if (ruler != null && actor.mind.home() == ruler.mind.home()) {
      mult = (1 - ruler.base().relations.communitySpirit());
    }
    return vocation.defaultSalary * mult;
  }
  
  
  private float worldTime() {
    return profiles.base.world.currentTime();
  }
  
  
  public float daysSinceWageAssessed() {
    if (salary() <= 0) return 0;
    if (lastWageEval == -1) lastWageEval = worldTime();
    final float interval = worldTime() - lastWageEval;
    return interval / Stage.STANDARD_DAY_LENGTH;
  }
  
  
  public float daysSinceSinceTaxAssessed() {
    if (salary() <= 0) return 0;
    if (lastTaxEval == -1) lastTaxEval = worldTime();
    final float interval = worldTime() - lastTaxEval;
    return interval / Stage.STANDARD_DAY_LENGTH;
  }
  
  
  public float wagesDue() {
    return paymentDue;
  }
  
  
  public void incWagesDue(float inc) {
    paymentDue += inc;
    lastWageEval = worldTime();
  }
  
  
  public void clearWages() {
    paymentDue = 0;
  }
  
  
  public void clearTax() {
    lastTaxEval = worldTime();
  }
}





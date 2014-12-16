

package stratos.game.plans;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.politic.*;
import stratos.game.economic.*;
import stratos.util.*;
import static stratos.game.politic.LawUtils.*;




public class Sentencing extends Plan {
  
  
  final Actor judge  ;
  final Actor accused;
  final Venue court  ;
  
  private float studyDone  = 0    ;
  private Summons verdict  = null ;
  private boolean complete = false;
  
  
  
  public Sentencing(Actor judge, Actor accused, Venue court) {
    super(judge, accused, true, NO_HARM);
    this.judge   = judge  ;
    this.accused = accused;
    this.court   = court  ;
  }
  
  
  public Sentencing(Session s) throws Exception {
    super(s);
    this.judge   = (Actor) s.loadObject();
    this.accused = (Actor) s.loadObject();
    this.court   = (Venue) s.loadObject();
    
    this.studyDone = s.loadFloat();
    this.verdict   = (Summons) s.loadObject();
    this.complete  = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(judge  );
    s.saveObject(accused);
    s.saveObject(court  );
    
    s.saveFloat (studyDone);
    s.saveObject(verdict  );
    s.saveBool  (complete );
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public static Sentencing nextTrialFor(Actor judge, Venue court) {
    final Base base = judge.base();
    for (Actor actor : court.personnel.visitors()) {
      if (base.profiles.crimesBy(actor).size() > 0) {
        return new Sentencing(judge, actor, court);
      }
    }
    return null;
  }
  
  
  protected float getPriority() {
    return ROUTINE;
  }
  
  
  //  TODO:  Allow for the possibility of bribery/corruption, or trial by jury
  //  (at the senate chamber.)
  
  protected Behaviour getNextStep() {
    if (complete) return null;
    
    if (studyDone < 1) {
      final Action study = new Action(
        judge, court,
        this, "actionStudy",
        Action.LOOK, "Studying case files"
      );
      return study;
    }
    else {
      final Action sentence = new Action(
        judge, court,
        this, "actionPassSentence",
        Action.TALK_LONG, "Passing sentence"
      );
      return sentence;
    }
  }
  
  
  public boolean actionStudy(Actor judge, Venue court) {
    studyDone += (Rand.num() + 0.5f) / 4;
    return true;
  }
  
  
  public boolean actionPassSentence(Actor judge, Venue court) {
    this.verdict = sentenceFor(accused);
    this.complete = true;
    
    if (verdict == null) return false;
    final Profile profile = judge.base().profiles.profileFor(accused);
    profile.clearRecord();
    profile.assignSentence(verdict);
    return true;
  }
  
  
  //  TODO:  Move this to the LawUtils class.  And give some weight to the
  //  opinions/competence of the judge...
  //  TODO:  Allow for differing kinds of punishment- possibly decreed by the
  //  player.
  Summons sentenceFor(Actor accused) {
    final Base base = judge.base();
    float offendScore = 0;
    
    for (Crime crime : base.profiles.crimesBy(accused)) {
      offendScore += LawUtils.severity(crime) * (0.5f + Rand.num());
    }
    if (offendScore <= 0) return null;
    final Summons s = new Summons(actor, null, court, Summons.TYPE_CAPTIVE);
    s.setStayDuration((int) Nums.ceil(offendScore * 2));
    return s;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    if (studyDone < 1) {
      final Base base = judge.base();
      d.append("Studying case files on ");
      d.append(accused);
      d.appendList("(Charged with ", base.profiles.crimesBy(accused));
      d.append("");
    }
    else {
      d.append("Passing sentence on ");
      d.append(accused);
    }
  }
}









package stratos.game.plans;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.actors.*;
import stratos.util.*;
import static stratos.game.base.LawUtils.*;




public class Sentencing extends Plan {
  
  
  final Actor judge  ;
  final Actor accused;
  final Venue court  ;
  
  private float studyDone  = 0    ;
  private Summons verdict  = null ;
  private boolean complete = false;
  
  
  
  public Sentencing(Actor judge, Actor accused, Venue court) {
    super(judge, accused, MOTIVE_JOB, NO_HARM);
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
    for (Actor actor : court.staff.visitors()) {
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

    final Profile profile = judge.base().profiles.profileFor(accused);
    this.verdict = sentenceFor(accused);
    profile.clearRecord();
    profile.assignSentence(verdict);
    this.complete = true;
    
    if (verdict == null) {
      Summons.cancelSummons(accused);
      return false;
    }
    else {
      accused.mind.assignBehaviour(verdict);
      return true;
    }
  }
  
  
  //  TODO:  Move this to the LawUtils class.  And give some weight to the
  //  opinions/competence of the judge...
  //  TODO:  Allow for differing kinds of punishment- possibly decreed by the
  //  player.
  Summons sentenceFor(Actor accused) {
    final Base base = judge.base();
    float offendScore = 0;
    
    for (Crime crime : base.profiles.crimesBy(accused)) {
      offendScore += crime.severity() * (0.5f + Rand.num());
    }
    if (offendScore <= 0) return null;
    final Summons s = new Summons(accused, actor, court, Summons.TYPE_CAPTIVE);
    s.setSentence((int) Nums.ceil(offendScore), Sentence.SENTENCE_CAPTIVITY);
    return s;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    if (studyDone < 1) {
      final Base base = judge.base();
      d.append("Studying case files on ");
      d.append(accused);
      d.append(" (charged with ");
      for (Crime crime : base.profiles.crimesBy(accused)) {
        d.append(" "+crime.description());
      }
      d.append("");
    }
    else {
      d.append("Passing sentence on ");
      d.append(accused);
    }
  }
}







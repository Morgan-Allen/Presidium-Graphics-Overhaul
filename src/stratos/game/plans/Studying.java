


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.base.Pledge;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



//  TODO:  Allow for learning of specific techniques.
//  TODO:  Allow upgrades at the archives to improve efficiency.
//  TODO:  Charge for the service?


public class Studying extends Plan {
  
  
  /**  Data fields, setup and save/load functions-
    */
  private static boolean
    evalVerbose  = false,
    studyVerbose = false,
    eventVerbose = false;
  
  final static float
    STAY_TIME   = Stage.STANDARD_HOUR_LENGTH,
    INSCRIBE_DC = TRIVIAL_DC,
    ACCOUNTS_DC = SIMPLE_DC ;
  
  final Venue venue;
  
  private float chargeCost;
  private float beginTime = -1;
  private Skill studied   = null;
  
  
  public Studying(Actor actor, Venue studyAt, float chargeCost) {
    super(actor, studyAt, MOTIVE_PERSONAL, NO_HARM);
    this.venue = studyAt;
    this.chargeCost = chargeCost;
  }
  
  
  public Studying(Session s) throws Exception {
    super(s);
    this.venue      = (Venue) s.loadObject();
    this.chargeCost = s.loadFloat();
    this.beginTime  = s.loadFloat();
    this.studied    = (Skill) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(venue     );
    s.saveFloat (chargeCost);
    s.saveFloat (beginTime );
    s.saveObject(studied   );
  }
  
  
  public Plan copyFor(Actor other) {
    return new Studying(other, venue, chargeCost);
  }
  
  
  
  /**  Evaluating targets and priority-
    */
  final static Skill BASE_SKILLS[] = { ACCOUNTING, INSCRIPTION };
  final static Trait BASE_TRAITS[] = { CURIOUS, AMBITIOUS, SOLITARY };

  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (! venue.isManned()) return 0;
    
    if (studied == null) studied = toStudy();
    if (studied == null) return 0;
    float modifier = 0 - actor.motives.greedPriority(chargeCost);
    
    return priorityForActorWith(
      actor, venue,
      CASUAL, modifier,
      MILD_HELP, NO_COMPETITION,
      MILD_FAIL_RISK, BASE_SKILLS,
      BASE_TRAITS, PARTIAL_DISTANCE_CHECK,
      report
    );
  }
  
  
  public float successChanceFor(Actor actor) {
    float chance = 1;
    chance += actor.skills.chance(INSCRIPTION, INSCRIBE_DC);
    chance += actor.skills.chance(ACCOUNTING , ACCOUNTS_DC);
    return chance / 3;
  }
  
  
  
  /**  Behaviour implementation-
    */
  private Skill toStudy() {
    final boolean report = studyVerbose && I.talkAbout == actor;
    final Pick <Skill> pick = new Pick <Skill> (null, 0);
    
    //  Include any skills that factor into the actor's long-term career plans-
    final Background ambition = FindWork.ambitionOf(actor);
    if (ambition != null) for (Skill s : ambition.skills()) {
      if (s.form != FORM_COGNITIVE) continue;
      final int knownLevel = (int) actor.traits.traitLevel(s);
      final float gap = (ambition.skillLevel(s) * 1.5f) - knownLevel;
      pick.compare(s, gap * Rand.num());
    }
    
    //  Inscription and accounting are always available-
    final float
      inscribeGap = INSCRIBE_DC - actor.traits.traitLevel(INSCRIPTION),
      accountsGap = ACCOUNTS_DC - actor.traits.traitLevel(ACCOUNTING );
    pick.compare(INSCRIPTION, inscribeGap * Rand.num());
    pick.compare(ACCOUNTING , accountsGap * Rand.num());
    
    //  File a report if called for, and return the end result-
    if (report) {
      I.say("\nGetting study regime for "+actor);
      I.say("  Ambition is: "+ambition+"  Key skills:");
      for (Skill s : ambition.skills()) {
        final int
          knownLevel = (int) actor.traits.traitLevel(s),
          needLevel  = ambition.skillLevel(s);
        I.say("    "+s+" "+knownLevel+"/"+needLevel);
      }
      I.say("  Skill to study: "+pick.result());
    }
    return pick.result();
  }
  
  
  protected Behaviour getNextStep() {
    
    final float time = actor.world().currentTime();
    if (beginTime == -1) beginTime = time;
    final float elapsed = time - beginTime;
    
    if (elapsed > STAY_TIME) {
      if (Rand.yes()) return null;
      else beginTime = time - (STAY_TIME * Rand.num());
    }
    final Action study = new Action(
      actor, venue,
      this, "actionStudy",
      Action.LOOK, "Studying"
    );
    return study;
  }
  
  
  public boolean actionStudy(Actor actor, Venue venue) {
    if (studied == null) return false;
    
    if (chargeCost > 0) {
      venue.stocks.incCredits(chargeCost);
      actor.gear.incCredits(0 - chargeCost);
      chargeCost = 0;
    }

    final int knownLevel = (int) actor.traits.traitLevel(studied);
    float practice = 1.0f;
    if (actor.skills.test(ACCOUNTING , ACCOUNTS_DC, 1)) practice++;
    if (actor.skills.test(INSCRIPTION, INSCRIBE_DC, 1)) practice++;
    
    actor.skills.practiceAgainst(knownLevel, practice, studied);
    return true;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("Studying ");
    d.append(studied);
    d.append(" at ");
    d.append(venue);
  }
}














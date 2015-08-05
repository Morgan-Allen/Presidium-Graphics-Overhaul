/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



//  TODO:  Allow for learning of specific techniques.
//  TODO:  Allow upgrades at the archives to improve efficiency.

public class Training extends Plan {
  
  
  /**  Data fields, setup and save/load functions-
    */
  private static boolean
    evalVerbose = false,
    stepVerbose = false;
  
  final static int
    TYPE_RESEARCH = 0,
    TYPE_DRILL    = 1,
    TYPE_TUTOR    = 2;
  
  final static float
    STAY_TIME   = Stage.STANDARD_HOUR_LENGTH,
    INSCRIBE_DC = TRIVIAL_DC,
    ACCOUNTS_DC = SIMPLE_DC ;
  
  final Venue venue;
  final int type;
  
  private float skillLimit = -1;
  private Skill available[] = null;
  
  private float chargeCost = 0;
  private float beginTime = -1;
  private Skill studied   = null;
  
  
  protected Training(Actor actor, Venue venue, int type) {
    super(actor, venue, MOTIVE_PERSONAL, NO_HARM);
    this.venue = venue;
    this.type  = type ;
  }
  
  
  public Training(Session s) throws Exception {
    super(s);
    this.venue      = (Venue) s.loadObject();
    this.type       = s.loadInt();
    
    this.skillLimit = s.loadFloat();
    this.available  = (Skill[]) s.loadObjectArray(Skill.class);
    
    this.chargeCost = s.loadFloat();
    this.beginTime  = s.loadFloat();
    this.studied    = (Skill) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(venue     );
    s.saveInt   (type      );
    
    s.saveFloat      (skillLimit);
    s.saveObjectArray(available );
    
    s.saveFloat (chargeCost);
    s.saveFloat (beginTime );
    s.saveObject(studied   );
  }
  
  
  public Plan copyFor(Actor other) {
    final Training s = new Training(other, venue, type);
    s.chargeCost = chargeCost;
    s.skillLimit = skillLimit;
    s.available  = available ;
    return s;
  }
  
  
  
  /**  Public factory methods-
    */
  public static Training asDrill(
    Actor actor, Venue venue, Skill available[], float skillLimit
  ) {
    final Training drill = new Training(actor, venue, TYPE_DRILL);
    drill.available  = available ;
    drill.skillLimit = skillLimit;
    return drill;
  }
  
  
  public static Training asResearch(
    Actor actor, Venue venue, float cost
  ) {
    final Training study = new Training(actor, venue, TYPE_RESEARCH);
    study.chargeCost = cost;
    return study;
  }
  
  
  
  /**  Evaluating targets and priority-
    */
  final static Trait
    DRILL_TRAITS   [] = { ENERGETIC, DUTIFUL, IGNORANT },
    RESEARCH_TRAITS[] = { CURIOUS, AMBITIOUS, SOLITARY };
  
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (report) {
      I.say("\nStudy priority for "+actor+" ("+actor.mind.vocation()+")");
      I.say("  "+venue+" open? "+venue.openFor(actor));
    }
    if (! venue.openFor(actor)) return -1;
    if (studied == null) studied = toStudy();
    if (studied == null) return -1;
    float modifier = IDLE - actor.motives.greedPriority(chargeCost);
    
    Trait baseTraits[] = NO_TRAITS;
    if (type == TYPE_DRILL   ) baseTraits = DRILL_TRAITS   ;
    if (type == TYPE_RESEARCH) baseTraits = RESEARCH_TRAITS;
    
    setCompetence(successChanceFor(actor));
    
    final float priority = PlanUtils.jobPlanPriority(
      actor, this,
      0.5f + (modifier / ROUTINE), competence(),
      -1, MILD_FAIL_RISK, baseTraits
    );
    return priority;
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
    final boolean report = evalVerbose && I.talkAbout == actor;
    //
    //  We select whatever available skill is furthest from it's desired level.
    final Pick <Skill> pick = new Pick <Skill> (null, 0) {
      public void compare(Skill s, float maxLevel) {
        final float rating = maxLevel - actor.traits.traitLevel(s);
        super.compare(s, rating);
      }
    };
    final Background ambition = FindWork.ambitionOf(actor);
    //
    //  If a customised skill-selection has been specified, we select from that
    //  range.
    if (available != null) for (Skill s : available) {
      final float bonus = ambition.skillLevel(s);
      pick.compare(s, (skillLimit + bonus) / 2);
    }
    //
    //  By default, training is otherwise limited to cognitive skills relevant
    //  to the actor's vocation:
    else for (Skill s : ambition.skills()) {
      if (s.form != FORM_COGNITIVE) continue;
      pick.compare(s, ambition.skillLevel(s) * 1.5f);
    }
    //
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
    if (type == TYPE_RESEARCH) {
      if (actor.skills.test(ACCOUNTING , ACCOUNTS_DC, 1)) practice++;
      if (actor.skills.test(INSCRIPTION, INSCRIBE_DC, 1)) practice++;
    }
    else practice += 0.5f;
    
    actor.skills.practiceAgainst(knownLevel, practice, studied);
    return true;
  }
  
  
  public void describeBehaviour(Description d) {
    if (type == TYPE_RESEARCH) d.append("Researching ");
    if (type == TYPE_DRILL   ) d.append("Drilling in ");
    d.append(studied);
    d.append(" at ");
    d.append(venue);
  }
}














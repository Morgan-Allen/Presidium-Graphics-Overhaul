/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.base.*;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.Planet;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



//  TODO:  Allow for learning of specific techniques.
//  TODO:  Allow upgrades at the archives to improve efficiency.

public class Studying extends Plan {
  
  
  /**  Data fields, setup and save/load functions-
    */
  private static boolean
    evalVerbose  = false,
    stepVerbose  = false,
    fastResearch = true ;
  private static float
    fastMultiple = 50f;
  
  final static int
    TYPE_STUDY    = 0,
    TYPE_DRILL    = 1,
    TYPE_RESEARCH = 3;
  
  final static float
    STAY_TIME   = Stage.STANDARD_HOUR_LENGTH,
    INSCRIBE_DC = TRIVIAL_DC,
    ACCOUNTS_DC = SIMPLE_DC ;
  
  final Venue venue;
  final int type;
  final Base base;
  
  private float skillLimit = -1;
  private Skill available[] = null;
  
  private float chargeCost = 0;
  private float beginTime = -1;
  private Constant studied = null;
  
  
  protected Studying(Actor actor, Venue venue, int type, Base base) {
    super(actor, venue, MOTIVE_PERSONAL, NO_HARM);
    this.venue = venue;
    this.type  = type ;
    this.base  = base ;
  }
  
  
  public Studying(Session s) throws Exception {
    super(s);
    this.venue      = (Venue) s.loadObject();
    this.type       = s.loadInt();
    this.base       = (Base) s.loadObject();
    
    this.skillLimit = s.loadFloat();
    this.available  = (Skill[]) s.loadObjectArray(Skill.class);
    
    this.chargeCost = s.loadFloat();
    this.beginTime  = s.loadFloat();
    this.studied    = (Constant) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(venue     );
    s.saveInt   (type      );
    s.saveObject(base      );
    
    s.saveFloat      (skillLimit);
    s.saveObjectArray(available );
    
    s.saveFloat (chargeCost);
    s.saveFloat (beginTime );
    s.saveObject(studied   );
  }
  
  
  public Plan copyFor(Actor other) {
    final Studying s = new Studying(other, venue, type, base);
    s.chargeCost = chargeCost;
    s.skillLimit = skillLimit;
    s.available  = available ;
    return s;
  }
  
  
  public boolean matchesPlan(Behaviour p) {
    if (! super.matchesPlan(p)) return false;
    final Studying s = (Studying) p;
    return s.studied == studied && s.type == type;
  }
  


  /**  Public factory methods-
    */
  public static Studying asStudy(
    Actor actor, Venue venue, float cost
  ) {
    final Studying study = new Studying(actor, venue, TYPE_STUDY, venue.base());
    study.chargeCost = cost;
    return study;
  }
  
  
  public static Studying asDrill(
    Actor actor, Venue venue, Skill available[], float skillLimit
  ) {
    final Studying drill = new Studying(actor, venue, TYPE_DRILL, venue.base());
    drill.available  = available ;
    drill.skillLimit = skillLimit;
    return drill;
  }
  
  
  public static Studying asResearch(
    Actor actor, Upgrade sought, Base base
  ) {
    //
    //  Firstly, we obtain a set of possible sites for research-
    final Pick <Venue> pick = new Pick();
    final Batch <Target> possible = new Batch();
    possible.include(actor.mind.home());
    possible.include(actor.mind.work());
    base.world.presences.sampleFromMap(
      actor, base.world, 5, possible, Economy.SERVICE_RESEARCH
    );
    //
    //  Then, we rate each on the basis of suitability for that field.
    for (Target t : possible) {
      if (! (t instanceof Venue)) continue;
      final Venue venue = (Venue) t;
      
      float rating = 1;
      if (sought.origin == venue.blueprint) rating *= 2;
      if (venue.blueprint.category == sought.origin.category) rating *= 2;
      pick.compare(venue, rating);
    }
    final Venue venue = pick.result();
    if (venue == null) return null;
    //
    //  And return the result.
    final Studying research = new Studying(actor, venue, TYPE_RESEARCH, base);
    research.studied = sought;
    return research;
  }
  
  
  
  /**  Evaluating targets and priority-
    */
  final static Trait
    DRILL_TRAITS   [] = { ENERGETIC, DUTIFUL, PATIENT },
    RESEARCH_TRAITS[] = { CURIOUS, AMBITIOUS, SOLITARY };
  
  
  
  protected float getPriority() {
    final boolean report = I.talkAbout == actor && evalVerbose;
    
    if (report) {
      I.say("\nStudy priority for "+actor+" ("+actor.mind.vocation()+")");
      I.say("  "+venue+" open for study? "+venue.openFor(actor));
    }
    
    if (! venue.openFor(actor)) return -1;
    if (studied == null) studied = toStudy();
    if (studied == null) return -1;
    
    final BaseResearch BR = venue.base().research;
    if (type == TYPE_RESEARCH && BR.hasTheory((Upgrade) studied)) return -1;
    
    float modifier = IDLE;
    Trait baseTraits[] = NO_TRAITS;
    if (type == TYPE_RESEARCH) {
      baseTraits = RESEARCH_TRAITS;
    }
    if (type == TYPE_DRILL) {
      baseTraits = DRILL_TRAITS;
    }
    if (type == TYPE_STUDY) {
      baseTraits = RESEARCH_TRAITS;
    }

    float dayValue = Planet.dayValue(actor.world());
    setCompetence(successChanceFor(actor));
    
    float priority = PlanUtils.traitAverage(actor, baseTraits) * CASUAL;
    modifier -= actor.motives.greedPriority(chargeCost);
    modifier += motiveBonus();
    priority = (priority + modifier) * competence() * dayValue;
    
    if (report) {
      I.say("  Motive bonus:   "+motiveBonus());
      I.say("  Modifier is:    "+modifier);
      I.say("  Competence is:  "+competence());
      I.say("  Day-value is:   "+dayValue);
      I.say("  Final priority: "+priority);
    }
    
    return priority;
  }
  
  
  public float successChanceFor(Actor actor) {
    if (type == TYPE_DRILL) {
      return 1;
    }
    else if (type == TYPE_RESEARCH) {
      final Conversion r = ((Upgrade) studied).researchProcess;
      return r.testChance(actor, 0);
    }
    else {
      float chance = 1;
      chance += actor.skills.chance(INSCRIPTION, INSCRIBE_DC);
      chance += actor.skills.chance(ACCOUNTING , ACCOUNTS_DC);
      return chance / 3;
    }
  }
  
  
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
  
  
  private Actor findColleague() {
    final Pick <Actor> pick = new Pick();
    
    for (Actor a : venue.staff.visitors()) {
      final Studying done = (Studying) a.matchFor(this);
      if (a != actor && done != null) {
        final float counselSkill = a.traits.usedLevel(COUNSEL) / 10;
        pick.compare(a, done.competence() * (counselSkill + 1));
      }
    }
    return pick.result();
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    final boolean report = I.talkAbout == actor && stepVerbose;
    if (report) {
      I.say("\nGetting next study-step for "+actor);
      I.say("  Study subject: "+studied);
    }
    
    final float time = actor.world().currentTime();
    if (beginTime == -1) beginTime = time;
    final float elapsed = time - beginTime;
    if (elapsed > STAY_TIME) return null;
    
    final int stepType = Rand.index(3);
    final Actor colleague = findColleague();
    //
    //  We split the chance somewhat-evenly between conferring, experiments and
    //  book-learnin'.
    if (stepType == 1 && colleague != null) {
      final Action confer = new Action(
        actor, colleague,
        this, "actionConfer",
        Action.TALK_LONG, "Conferring with "+colleague+" on"
      );
      if (report) I.say("  Returning confer-step.");
      return confer;
    }
    else if (stepType == 2 && type == TYPE_RESEARCH) {
      final Action experiment = new Action(
        actor, venue,
        this, "actionExperiment",
        Action.LOOK, "Experimenting with"
      );
      if (report) I.say("  Returning experiment-step.");
      return experiment;
    }
    else {
      final Action study = new Action(
        actor, venue,
        this, "actionStudy",
        Action.LOOK, "Studying"
      );
      if (report) I.say("  Returning normal study-step.");
      return study;
    }
  }
  
  
  public boolean actionConfer(Actor actor, Actor colleague) {
    final Studying done = (Studying) colleague.matchFor(this);
    if (done == null) return false;
    //
    //  Conferring grants a bonus based on the counsel skill and the competence
    //  of the instructor.
    float bonus = 1;
    bonus += DialogueUtils.talkResult(COUNSEL, ROUTINE_DC, colleague, actor);
    bonus *= done.competence();
    return performStudy(actor, venue, 1 + bonus);
  }
  
  
  public boolean actionExperiment(Actor actor, Venue venue) {
    final Upgrade u = (Upgrade) studied;
    if (u == null || u.origin == null) return false;
    //
    //  Experiment grants a bonus based on the presence of raw materials and
    //  adequate facilities.
    //  TODO:  Base this off any Conversions associated with the upgrade!
    float bonus = 0;
    if (venue.blueprint.category == u.origin.category) bonus += 0.5f;
    else bonus -= 0.5f;
    
    return performStudy(actor, venue, 1 + bonus);
  }
  
  
  public boolean actionStudy(Actor actor, Venue venue) {
    return performStudy(actor, venue, 1);
  }
  
  
  private boolean performStudy(Actor actor, Venue venue, float studyRate) {
    if (studied == null) return false;
    if (chargeCost > 0) {
      actor.gear.transferCredits(chargeCost, venue);
      chargeCost = 0;
    }
    final Action a = action();
    //
    //  Research progress can come as either gradual increments or sudden
    //  breakthroughs- or even setbacks.
    if (type == TYPE_RESEARCH) {
      final Upgrade sought = (Upgrade) studied;
      final BaseResearch BR = venue.base().research;
      
      if (BR.hasTheory(sought)) return true;
      final int   RES_LEVEL = BaseResearch.LEVEL_THEORY;
      final float RES_TIME  = BaseResearch.DEFAULT_RESEARCH_TIME;
      
      float skillTest = sought.researchProcess.performTest(actor, 0, 1, a);
      float progress = BR.researchProgress(sought, RES_LEVEL);
      final float breakthrough = (Rand.num() + 0.5f) / 2;
      
      float advance = fastResearch ? fastMultiple : 1;
      advance *= studyRate / RES_TIME;
      advance *= skillTest;
      
      float setback = fastResearch ? fastMultiple : 1;
      setback *= (1 - skillTest) / RES_TIME;
      setback *= (1 - progress) / 2;
      
      if (Rand.num() * breakthrough < advance) {
        sought.sendBreakthroughMessage(base);
        BR.incResearchFor(sought, breakthrough, BaseResearch.LEVEL_THEORY);
      }
      else if (Rand.num() < setback) {
        sought.sendSetbackMessage(base);
        final float setbackAmount = 0 - breakthrough * progress;
        BR.incResearchFor(sought, setbackAmount, BaseResearch.LEVEL_THEORY);
      }
      else {
        BR.incResearchFor(sought, advance, BaseResearch.LEVEL_THEORY);
      }
      return true;
    }
    //
    //  Personal study or drilling tends to be more straightforward.
    else {
      final Skill skill = (Skill) studied;
      final int knownLevel = (int) actor.traits.traitLevel(skill);
      float practice = 1.0f;
      
      if (type == TYPE_STUDY) {
        if (actor.skills.test(ACCOUNTING , ACCOUNTS_DC, 1, a)) practice++;
        if (actor.skills.test(INSCRIPTION, INSCRIBE_DC, 1, a)) practice++;
      }
      else {
        practice += 0.5f;
      }
      
      practice *= studyRate;
      actor.skills.practiceAgainst(knownLevel, practice, skill);
      return true;
    }
  }
  
  
  
  /**  Interface and reporting-
    */
  public void describeBehaviour(Description d) {
    if (type == TYPE_RESEARCH) {
      if (super.needsSuffix(d, "Researching"))
      d.append(" ");
      d.append(studied);
      return;
    }
    if (type == TYPE_STUDY   ) d.append("Studying ");
    if (type == TYPE_DRILL   ) d.append("Drilling in ");
    d.append(studied);
    d.append(" at ");
    d.append(venue);
  }
}














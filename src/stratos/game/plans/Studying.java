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
    fastResearch = false;
  
  final static int
    TYPE_STUDY    = 0,
    TYPE_DRILL    = 1,
    TYPE_TUTOR    = 2,
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
    
    setCompetence(successChanceFor(actor));
    
    float priority = PlanUtils.traitAverage(actor, baseTraits) * CASUAL;
    modifier -= actor.motives.greedPriority(chargeCost);
    modifier += motiveBonus();
    priority = (priority + modifier) * competence();
    
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
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    
    final float time = actor.world().currentTime();
    if (beginTime == -1) beginTime = time;
    final float elapsed = time - beginTime;
    if (elapsed > STAY_TIME) return null;
    
    //  TODO-  Add 'conferring with' and 'analysing data/experimenting' steps!
    
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
      actor.gear.transferCredits(chargeCost, venue);
      chargeCost = 0;
    }
    
    if (type == TYPE_RESEARCH) {
      final Upgrade sought = (Upgrade) studied;
      final BaseResearch BR = venue.base().research;
      if (BR.hasTheory(sought)) return true;
      
      float inc = fastResearch ? 50 : 1;
      inc *= sought.researchProcess.performTest(actor, 0, 1);
      inc /= BaseResearch.DEFAULT_RESEARCH_TIME;
      //
      //  Research progress can come as either gradual increments or sudden
      //  breakthroughs-
      final float breakthrough = (Rand.num() + 0.5f) / 2;
      if (Rand.num() * breakthrough < inc) {
        BR.incResearchFor(sought, breakthrough, BaseResearch.LEVEL_THEORY);
        sought.sendBreakThroughMessage(base);
      }
      else {
        BR.incResearchFor(sought, inc, BaseResearch.LEVEL_THEORY);
      }
      return true;
    }
    else if (type == TYPE_TUTOR) {
      return true;
    }
    else {
      final Skill skill = (Skill) studied;
      final int knownLevel = (int) actor.traits.traitLevel(skill);
      float practice = 1.0f;
      
      if (type == TYPE_STUDY) {
        if (actor.skills.test(ACCOUNTING , ACCOUNTS_DC, 1)) practice++;
        if (actor.skills.test(INSCRIPTION, INSCRIBE_DC, 1)) practice++;
      }
      else {
        practice += 0.5f;
      }
      actor.skills.practiceAgainst(knownLevel, practice, skill);
      return true;
    }
  }
  
  
  
  /**  Interface and reporting-
    */
  public void describeBehaviour(Description d) {
    if (type == TYPE_RESEARCH) d.append("Researching ");
    if (type == TYPE_STUDY   ) d.append("Studying ");
    if (type == TYPE_DRILL   ) d.append("Drilling in ");
    d.append(studied);
    d.append(" at ");
    d.append(venue);
  }
}
















package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.base.LawUtils.*;
import static stratos.game.economic.Economy.*;




//  TODO:  I'm disabling this for the moment, until I get it sorted (along with
//  the enforcer bloc) in expansions.


public class Arrest extends Plan {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  private static boolean
    evalVerbose  = false,
    stepsVerbose = false;
  
  final static int
    STAGE_INIT   = -1,
    STAGE_WARN   =  0,
    STAGE_CHASE  =  1,
    STAGE_ESCORT =  2,
    STAGE_REPORT =  3,
    STAGE_DONE   =  4,
    
    WARN_LIMIT = 2;
  
  
  private Venue   holding  = null;
  private int     stage    = STAGE_INIT;
  private Crime   observed = null;
  private Summons sentence = null;
  
  
  public Arrest(Actor actor, Target subject, Summons sentence) {
    this(actor, subject);
    this.sentence = sentence;
  }
  
  
  public Arrest(Actor actor, Target subject) {
    super(actor, subject, MOTIVE_EMERGENCY, MILD_HARM);
  }
  
  
  public Arrest(Session s) throws Exception {
    super(s);
    holding  = (Venue) s.loadObject();
    stage    = s.loadInt();
    observed = (Crime) s.loadEnum(Crime.values());
    sentence = (Summons) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(holding );
    s.saveInt   (stage   );
    s.saveEnum  (observed);
    s.saveObject(sentence);
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  public static Arrest nextOfficialArrest(Venue venue, Actor actor) {
    final Stage world = venue.world();
    final Pick <Arrest> pick = new Pick <Arrest> ();
    final Batch <Mobile> suspects = new Batch <Mobile> ();
    
    world.presences.sampleFromMaps(venue, world, 5, suspects, Mobile.class);
    
    for (Mobile m : suspects) if (m instanceof Actor) {
      final Actor suspect = (Actor) m;
      final Summons sentence = venue.base().profiles.sentenceFor(suspect);
      if (sentence != null) {
        final Arrest a = new Arrest(actor, suspect, sentence);
        pick.compare(a, a.priorityFor(actor));
      }
    }
    return pick.result();
  }
  
  
  
  /**  Various utility methods for decision-making:
    */
  /*
  private boolean canPursue() {
    if (stage == STAGE_DONE) return false;
    if (stage != STAGE_INIT) return true;
    if (actor.base() != subject.base()) return false;
    
    observed = LawUtils.crimeDoneBy((Actor) subject, actor);
    if (sentence == null && observed == null) {
      stage = STAGE_DONE; return false;
    }
    
    if (hasAuthority()) {
      holding = (Venue) actor.mind.work();
    }
    else {
      holding = Audit.nearestAdmin(actor);
      final Target home = ((Actor) subject).mind.work();
      if (holding == null && home instanceof Venue) holding = (Venue) home;
    }
    if (holding == null) { stage = STAGE_DONE; return false; }
    
    stage = STAGE_WARN;
    return true;
  }
  
  
  //  TODO:  Modify priority (and command-chance) based on difference in
  //  social standing.
  private boolean hasAuthority() {
    if (! PlanUtils.isArmed(actor)) return false;
    final Property work = actor.mind.work();
    if (work == null) return false;
    if (Visit.arrayIncludes(work.services(), SERVICE_SECURITY)) {
      return true;
    }
    return false;
  }
  
  
  public void interrupt(String cause) {
    if (cause == INTERRUPT_LOSE_SIGHT) {
      this.stage = STAGE_REPORT;
      super.clearEval(actor);
    }
    else super.interrupt(cause);
  }
  
  
  protected int evaluationInterval() {
    if (stage <= STAGE_ESCORT) return 1;
    else return super.evaluationInterval();
  }
  //*/
  

  private boolean hasAuthority() {
    if (! PlanUtils.isArmed(actor)) return false;
    final Property work = actor.mind.work();
    if (work == null) return false;
    return (Visit.arrayIncludes(work.services(), SERVICE_SECURITY));
  }
  
  
  
  /**  Behaviour implementation-
    */
  final static Skill BASE_SKILLS[] = { ATHLETICS, FORENSICS, COMMAND };
  final static Trait BASE_TRAITS[] = { DUTIFUL, ETHICAL, FEARLESS };
  
  
  protected float getPriority() {
    return -1;
    
    /*
    final boolean report = I.talkAbout == actor;// && evalVerbose;
    if (! canPursue()) return -1;
    
    
    final Actor other = (Actor) subject;
    final boolean official = hasAuthority();
    
    float urge = 0, bonus = 0;
    if (observed != null) urge = observed.severity() / 10f;
    if (sentence != null) {
      urge = Nums.max(0.5f, urge);
    }
    
    if (stage == STAGE_WARN) {
      if (urge <= 0) return 0;
      bonus = (ROUTINE * urge) + (official ? ROUTINE : 0);
    }
    else if (stage == STAGE_CHASE ) bonus = PARAMOUNT;
    else if (stage == STAGE_ESCORT) bonus = PARAMOUNT;
    else                            bonus = ROUTINE  ;
    
    //  TODO:  Include command/suasion as key skills?
    final float priority = PlanUtils.combatPriority(
      actor, other, (urge * PARAMOUNT) + bonus, 1, true, MILD_HARM
    );
    if (report) {
      I.say("Priority for arrest of "+other+" is: "+priority);
    }
    
    if (priority < ROUTINE && ! official) return 0;
    return priority;
    //*/
  }
  
  
  public float successChanceFor(Actor actor) {
    final Actor suspect = (Actor) subject;
    float chance = CombatUtils.powerLevelRelative(actor, suspect) / 2f;
    chance = (chance + 1 - actor.senses.fearLevel()) / 2f;
    return Nums.clamp(chance, 0, 1);
  }
  
  
  protected Behaviour getNextStep() {
    return null;
    /*
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (! canPursue()) return null;
    
    final Actor other = (Actor) subject;
    final boolean
      official = hasAuthority(),
      canWarn  = other.relations.noveltyFor(actor) > 0,
      downed   = CombatUtils.isDowned(other, Combat.OBJECT_SUBDUE),
      captive  = other.isDoing(Summons.class, actor),
      atLarge  = ! (downed || captive);
    
    //  TODO:  Base canWarn off relations with a concept (see below.)
    
    if (report) {
      I.say("\nGetting next arrest step for "+actor);
      I.say("  Arresting:     "+other    );
      I.say("  Has authority? "+official);
      I.say("  Other is:      "+other.mind.rootBehaviour());
    }
    
    if ((stage == STAGE_INIT || stage == STAGE_WARN) && atLarge) {
      if (canWarn) {
        if (report) I.say("  Ordering surrender.");
        final Action order = new Action(
          actor, other,
          this, "actionOrderSurrender",
          Action.TALK, "Ordering surrender of "
        );
        order.setProperties(Action.QUICK | Action.RANGED);
        return order;
      }
      else stage = official ? STAGE_CHASE : STAGE_REPORT;
    }
    
    if (official && atLarge) {
      if (report) I.say("  Giving chase!");
      stage = STAGE_CHASE;
      final Combat chase = new Combat(
        actor, other, Combat.STYLE_EITHER, Combat.OBJECT_SUBDUE
      );
      if (! chase.valid()) return null;
      return chase;
    }
    
    if (official && ! atLarge) {
      if (other.aboard() == holding) stage = STAGE_REPORT;
      else if (downed) {
        if (report) I.say("  Returning unconscious captive.");
        stage = STAGE_ESCORT;
        return new BringStretcher(actor, other, actor.mind.work());
      }
      else {
        if (report) I.say("  Escorting back to holding.");
        stage = STAGE_ESCORT;
        
        final boolean close = Spacing.distance(actor, other) < 1;
        final Action escort = new Action(
          actor, close ? holding : other,
          this, "actionEscort",
          Action.TALK_LONG, "Escorting "
        );
        return escort;
      }
    }
    
    if (stage == STAGE_REPORT) {
      if (observed == null) return null;
      final Action reports = new Action(
        actor, holding,
        this, "actionFileReport",
        Action.TALK_LONG, "Filing report on "
      );
      return reports;
    }
    
    return null;
    //*/
  }
  
  
  public boolean actionOrderSurrender(Actor actor, Actor other) {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) I.say("\nOrdering surrender of "+other);
    //
    //  Firstly, we determine just how 'persuasive' the actor in question is,
    //  and what degree of sentence they are entitled to impose.
    final boolean official = hasAuthority();
    if (sentence == null) sentence = new Summons(
      other, actor, holding,
      official ? Summons.TYPE_CAPTIVE : Summons.TYPE_SULKING
    );
    final float commandBonus = DialogueUtils.talkResult(
      COMMAND, MODERATE_DC, actor, other
    ) * ROUTINE;
    sentence.clearMotives();
    sentence.addMotives(Plan.MOTIVE_EMERGENCY, commandBonus);
    //
    //  Adjust the novelty of the other actor's relationship with you (to
    //  prevent repeats ad-infinitum.)
    other.relations.incRelation(actor, 0, 0, -2f * Rand.num() / WARN_LIMIT);
    if (report) {
      final Behaviour current = other.mind.rootBehaviour();
      I.say("  Sentence priority is:  "+sentence.priorityFor(other));
      I.say("  Current plan priority: "+current .priorityFor(other));
      I.say("  Destination:           "+holding);
      I.say("  Discussion novelty:    "+other.relations.noveltyFor(actor));
    }
    //
    //  The command priority needs to be at least paramount- nobody likes being
    //  locked up- and must outweigh whatever else the actor is doing.
    if (commandBonus < PARAMOUNT || other.mind.mustIgnore(sentence)) {
      if (report) I.say("  "+other+" has refused surrender!");
      stage = official ? STAGE_CHASE : STAGE_DONE;
      return false;
    }
    else {
      if (report) I.say("  "+other+" has surrendered!");
      other.mind.assignBehaviour(sentence);
      stage = official ? STAGE_ESCORT : STAGE_DONE;
      return true;
    }
  }
  
  
  public boolean actionEscort(Actor actor, Target point) {
    //  TODO:  Check to make sure the subject doesn't escape?
    return true;
  }
  
  
  public boolean actionFileReport(Actor actor, Venue office) {
    
    final Actor other = (Actor) subject;
    if (other.aboard() == holding) {
      final Summons capture = new Summons(
        other, actor, holding, Summons.TYPE_CAPTIVE
      );
      other.mind.assignBehaviour(capture);
    }
    
    final Profile profile = actor.base().profiles.profileFor(other);
    profile.recordOffence(observed);
    stage = STAGE_DONE;
    return true;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    if (stage == STAGE_WARN) {
      if (observed == null) {
        d.append("Ordering ");
        d.append(subject);
        d.append(" to surrender");
      }
      else {
        d.append("Warning ");
        d.append(subject);
        d.append(" against ");
        d.append(observed.description());
      }
    }
    if (stage == STAGE_CHASE || stage == STAGE_ESCORT) {
      d.append("Arresting ");
      d.append(subject);
    }
    if (stage >= STAGE_REPORT) {
      d.append("Filing a report");
      if (observed != null) {
        d.append(" of ");
        d.append(observed.description());
      }
      d.append(" against ");
      d.append(subject);
    }
  }
}












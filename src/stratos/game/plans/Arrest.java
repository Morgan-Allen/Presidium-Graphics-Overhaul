

package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.util.*;
import static stratos.game.actors.Profile.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



public class Arrest extends Plan {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  private static boolean
    evalVerbose  = true ,
    stepsVerbose = true ;
  
  final static int
    STAGE_INIT   = -1,
    STAGE_WARN   =  0,
    STAGE_CHASE  =  1,
    STAGE_ESCORT =  2,
    STAGE_REPORT =  3,
    STAGE_DONE   =  4,
    
    WARN_LIMIT = 2;
  
  
  private Venue holding;
  private int stage = STAGE_INIT;
  private Sentence sentence = null;  //  TODO:  Might not be needed here...
  
  
  public Arrest(Actor actor, Target subject, Sentence sentence) {
    this(actor, subject);
    this.sentence = sentence;
  }
  
  
  public Arrest(Actor actor, Target subject) {
    super(actor, subject, true, MILD_HARM);
    if (hasAuthority()) holding = (Venue) actor.mind.work();
    else {
      final Target home = ((Actor) subject).mind.work();
      holding = Audit.nearestAdmin(actor);
      if (holding == null && home instanceof Venue) holding = (Venue) home;
    }
  }
  
  
  public Arrest(Session s) throws Exception {
    super(s);
    holding  = (Venue) s.loadObject();
    stage    = s.loadInt();
    sentence = (Sentence) s.loadEnum(Sentence.values());
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(holding );
    s.saveInt   (stage   );
    s.saveEnum  (sentence);
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  
  /**  Behaviour implementation-
    */
  final static Trait BASE_TRAITS[] = { DUTIFUL, ETHICAL, FEARLESS };
  
  //  TODO:  Include modifiers based on penalties for the crime, specified by
  //  the sovereign.
  
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (stage == STAGE_DONE || holding == null || isExempt(subject)) {
      return 0;
    }
    
    final Actor other = (Actor) subject;
    final Target victim = other.planFocus(null);
    final boolean melee = actor.gear.meleeWeapon();
    final boolean official = hasAuthority();
    
    float urge = 0, bonus = 0;
    if (victim != null) {
      urge += actor.relations.valueFor(victim);
      urge += actor.relations.valueFor(victim.base());
      urge *= other.harmIntended(victim);
    }
    
    if (stage <= STAGE_WARN) {
      if (urge <= 0) return 0;
      bonus = (ROUTINE * urge) + (official ? ROUTINE : 0);
    }
    else if (stage == STAGE_CHASE ) bonus = PARAMOUNT;
    else if (stage == STAGE_ESCORT) bonus = PARAMOUNT;
    else                            bonus = ROUTINE  ;
    
    //  TODO:  Include command/suasion as key skills?
    final float priority = priorityForActorWith(
      actor, other,
      urge * PARAMOUNT, bonus,
      MILD_HARM, NO_COMPETITION, MILD_FAIL_RISK,
      melee ? Combat.MELEE_SKILLS : Combat.RANGED_SKILLS, BASE_TRAITS,
      NORMAL_DISTANCE_CHECK,
      report
    );
    return priority;
  }
  
  
  protected float successChance() {
    return CombatUtils.successChance(actor, subject);
  }
  
  
  protected int evaluationInterval() {
    return 1;
  }
  
  
  private boolean isExempt(Target other) {
    if (((Actor) other).isDoing(Arrest.class, null)) return true;
    return false;
  }
  
  //  TODO:  Modify priority (and command-chance) based on difference in
  //  social standing.
  private boolean hasAuthority() {
    if (! CombatUtils.isArmed(actor)) return false;
    final Property work = actor.mind.work();
    if (work == null) return false;
    if (Visit.arrayIncludes(work.services(), SERVICE_SECURITY)) {
      return true;
    }
    return false;
  }
  
  
  protected Behaviour getNextStep() {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (stage == STAGE_DONE || holding == null || isExempt(subject)) {
      return null;
    }
    
    final Actor   other = (Actor) subject;
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
        actor, other, Combat.STYLE_EITHER, Combat.OBJECT_SUBDUE, true
      );
      return chase;
    }
    
    if (official && ! atLarge) {
      if (other.aboard() == holding) stage = STAGE_REPORT;
      else if (downed) {
        if (report) I.say("  Returning unconscious captive.");
        stage = STAGE_ESCORT;
        return new StretcherDelivery(actor, other, actor.mind.work());
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
      final Action reports = new Action(
        actor, holding,
        this, "actionFileReport",
        Action.TALK_LONG, "Filing report on "
      );
      return reports;
    }
    
    return null;
  }
  
  
  public boolean actionOrderSurrender(Actor actor, Actor other) {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) I.say("\nOrdering surrender of "+other);
    
    final boolean official = hasAuthority();
    final Summons surrender = new Summons(
      other, actor, holding,
      official ? Summons.TYPE_CAPTIVE : Summons.TYPE_SULKING
    );
    final float commandBonus = DialogueUtils.talkResult(
      COMMAND, MODERATE_DC, actor, other
    ) * ROUTINE;
    surrender.setMotive(Plan.MOTIVE_EMERGENCY, commandBonus);
    
    //
    //  Adjust the novelty of the other actor's relationship with you (to
    //  prevent repeats ad-infinitum.)
    //  TODO:  Base this off relations with a concept, rather than the person.
    other.relations.incRelation(actor, 0, 0, -2f * Rand.num() / WARN_LIMIT);
    if (report) {
      final Behaviour current = other.mind.rootBehaviour();
      I.say("  Command priority is:   "+surrender.priorityFor(other));
      I.say("  Current plan priority: "+current  .priorityFor(other));
      I.say("  Destination:           "+holding);
      I.say("  Discussion novelty:    "+other.relations.noveltyFor(actor));
    }
    
    if (! other.mind.mustIgnore(surrender)) {
      if (report) I.say("  "+other+" has surrendered!");
      other.mind.assignBehaviour(surrender);
      stage = official ? STAGE_ESCORT : STAGE_DONE;
      return true;
    }
    else {
      if (report) I.say("  "+other+" has refused surrender!");
      if (official) stage = STAGE_CHASE;
      return false;
    }
  }
  
  
  public boolean actionEscort(Actor actor, Target point) {
    //  TODO:  Check to make sure the subject doesn't escape?
    return true;
  }
  
  
  public boolean actionFileReport(Actor actor, Venue office) {
    stage = STAGE_DONE;
    return true;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    if (! hasAuthority()) {
      d.append("Warning ");
      d.append(subject);
      
      //  TODO:  Stipulate the crime being warned against...
      /*
      d.append(" against ");
      d.append(((Actor) subject).mind.rootBehaviour());
      //*/
    }
    else {
      if (super.needsSuffix(d, "Arresting ")) {
        d.append(subject);
      }
    }
  }
}












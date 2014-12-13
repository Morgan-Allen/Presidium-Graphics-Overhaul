

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
    evalVerbose  = false,
    stepsVerbose = true ;
  
  final static int
    TYPE_CENSURE = 0,
    TYPE_ARREST  = 2,
    TYPE_BEATING = 1,
    
    WARN_LIMIT = 2;
  
  
  private Sentence sentence = null;
  private boolean doneWarning = false;
  
  
  public Arrest(Actor actor, Target subject, Sentence sentence) {
    this(actor, subject);
    this.sentence = sentence;
  }
  
  
  public Arrest(Actor actor, Target subject) {
    super(actor, subject, true, MILD_HARM);
  }
  
  
  public Arrest(Session s) throws Exception {
    super(s);
    sentence = (Sentence) s.loadEnum(Sentence.values());
    doneWarning = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveEnum(sentence);
    s.saveBool(doneWarning);
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  
  /**  Behaviour implementation-
    */
  final static Trait BASE_TRAITS[] = { DUTIFUL, ETHICAL, FEARLESS };
  //  TODO:  Include modifiers based on penalties for the crime, specified by
  //  the sovereign?
  
  
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;// && hasBegun();
    
    //  Don't arrest other arresters!  Other than that, priority is based on
    //  the harm-level and criminality of the act in question.
    final Actor other = (Actor) subject;
    if (other.isDoing(Arrest.class, null)) return 0;
    final Target victim = other.planFocus(null);
    final boolean melee = actor.gear.meleeWeapon();
    if (victim == null) return 0;
    
    //  TODO:  Modify priority (and command-chance) based on difference in
    //  social standing.
    //  TODO:  Use command/suasion as key skills when giving orders.
    
    float urge = 0, bonus = 0;
    urge += other.harmIntended(victim) * actor.relations.valueFor(victim);
    bonus += urge;
    
    final float priority = priorityForActorWith(
      actor, other,
      urge * PARAMOUNT, bonus * ROUTINE,
      MILD_HARM, NO_COMPETITION, MILD_FAIL_RISK,
      melee ? Combat.MELEE_SKILLS : Combat.RANGED_SKILLS, BASE_TRAITS,
      NORMAL_DISTANCE_CHECK,
      report
    );
    return priority;
  }
  
  
  private boolean hasAuthority() {
    if (! CombatUtils.isArmed(actor)) return false;
    final Property work = actor.mind.work();
    if (work == null) return false;
    if (Visit.arrayIncludes(work.services(), SERVICE_SECURITY)) {
      return true;
    }
    else return false;
  }
  
  
  protected Behaviour getNextStep() {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    final Actor other = (Actor) subject;
    final boolean authority = hasAuthority();
    final boolean canWarn = other.relations.noveltyFor(actor) > 0;
    //  TODO:  Base this off relations with a concept, rather than the person.
    
    if (report) {
      I.say("\nGetting next arrest step for "+actor);
      I.say("  Arresting:     "+other    );
      I.say("  Has authority? "+authority);
    }
    
    //  Give them a warning to stop first (chance to surrender/stand down.)
    //  Otherwise, give chase.
    if (canWarn && ! doneWarning) {
      if (report) I.say("  Ordering surrender.");
      final Action order = new Action(
        actor, other,
        this, "actionOrderSurrender",
        Action.TALK, "Ordering surrender of "
      );
      order.setProperties(Action.QUICK | Action.RANGED);
      return order;
    }
    
    //  If you don't have formal authority, give up at this point...
    else if (! authority) return null;
    
    //  If they've surrendered, escort them back to their cell:
    else if (other.isDoing(Summons.class, actor)) {
      
      //  If they're actually *in* the cell, call it quits:
      if (other.aboard() == actor.mind.work()) {
        if (report) I.say("  Arrest complete.");
        return null;
      }
      
      if (report) I.say("  Escorting back to holding.");
      final Action escort = new Action(
        actor, other,
        this, "actionEscort",
        Action.STAND, "Escorting "
      );
      escort.setMoveTarget(Spacing.nearestOpenTile(other, actor));
      return escort;
    }
    
    //  If they've been beaten, carry them back to their cell:
    else if (CombatUtils.isDowned(other, Combat.OBJECT_SUBDUE)) {
      if (report) I.say("  Returning unconscious captive.");
      return new StretcherDelivery(actor, other, actor.mind.work());
    }
    
    //  And if all else fails- give chase.
    else {
      if (report) I.say("  Giving chase!");
      final Combat chase = new Combat(
        actor, other, Combat.STYLE_EITHER, Combat.OBJECT_SUBDUE, true
      );
      return chase;
    }
    
  }
  
  
  public boolean actionOrderSurrender(Actor actor, Actor other) {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) I.say("\nOrdering surrender of "+other);
    
    final boolean authority = hasAuthority();
    final Boarding holding = authority ?
      actor.mind.work()   :
      other.senses.haven();
    
    if (! (holding instanceof Property)) {
      if (report) I.say("  No holding facility...");
      doneWarning = true;
      return false;
    }
    
    final Summons surrender = new Summons(
      other, actor, (Property) holding,
      authority ? Summons.TYPE_CAPTIVE : Summons.TYPE_SULKING
    );
    final float commandBonus = DialogueUtils.talkResult(
      COMMAND, MODERATE_DC, actor, other
    ) * ROUTINE;
    surrender.setMotive(Plan.MOTIVE_EMERGENCY, commandBonus);
    
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
      doneWarning = true;
      return true;
    }
    else {
      if (report) I.say("  "+other+" has refused surrender!");
      if (authority) doneWarning = true;
      return false;
    }
  }
  
  
  public boolean actionEscort(Actor actor, Actor other) {
    return true;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    if (! hasAuthority()) {
      d.append("Warning ");
      d.append(subject);
      
      //  TODO:  Stipulate the crime being warned against.
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












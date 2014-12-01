

package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.civilian.*;
import stratos.game.economic.*;
import stratos.start.PlayLoop;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



//  Ideally, Arrests need to be a reaction, rather than a job-assignment.  The
//  whole escort-back-to-holding-cells thing is just a specialised response
//  available to official enforcers.  The underlying purpose is to persuade
//  a 'comrade' *not* to do something objectionable- at least for a while.

//  TODO:  Extend Combat directly?


public class Arrest extends Plan {
  
  /**  Data fields, constructors and save/load methods-
    */
  private static boolean
    evalVerbose  = true ,
    stepsVerbose = false;
  
  
  private boolean gaveQuarter = false;
  
  
  public Arrest(Actor actor, Target subject) {
    super(actor, subject, true);
  }
  
  
  public Arrest(Session s) throws Exception {
    super(s);
    gaveQuarter = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveBool(gaveQuarter);
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
    final boolean report = evalVerbose && I.talkAbout == subject;
    
    //  Don't arrest other arresters!  Other than that, priority is based on
    //  the harm-level and criminality of the act in question.
    final Actor other = (Actor) subject;
    if (other.isDoing(Arrest.class, null)) return 0;
    final Target victim = other.focusFor(null);
    final boolean melee = actor.gear.meleeWeapon();
    if (victim == null) return 0;
    
    //  TODO:  Modify priority (and command-chance) based on difference in
    //  social standing.
    //  TODO:  Use command/suasion as key skills when giving orders.
    
    float urge = 0, bonus = 0;
    urge += other.harmDoneTo(victim) * actor.relations.valueFor(victim);
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
    final Employer work = actor.mind.work();
    if (work == null) return false;
    if (Visit.arrayIncludes(work.services(), SERVICE_SECURITY)) {
      return true;
    }
    else return false;
  }
  
  
  protected Behaviour getNextStep() {
    final boolean report = stepsVerbose && hasBegun();
    final Actor other = (Actor) subject;
    final boolean authority = hasAuthority();
    
    if (report) {
      I.say("\nGetting next arrest step for "+actor);
      I.say("  Arresting:     "+other    );
      I.say("  Has authority? "+authority);
    }
    PlayLoop.setPaused(true);
    
    //  TODO:  If you're *not* an official authority, just send them home
    //  without an escort.
    
    //  Give them a warning to stop first (chance to surrender/stand down.)
    //  Otherwise, give chase.
    if (! gaveQuarter) {
      if (report) I.say("  ...Ordering surrender.");
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
        if (report) I.say("  ...Arrest complete.");
        return null;
      }
      
      if (report) I.say("  ...Escorting back to holding.");
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
      if (report) I.say("  ...Returning unconscious captive.");
      return new StretcherDelivery(actor, other, actor.mind.work());
    }
    
    //  And if all else fails- give chase.
    else {
      if (report) I.say("  ...Giving chase!");
      final Combat chase = new Combat(
        actor, other, Combat.STYLE_EITHER, Combat.OBJECT_SUBDUE, true
      );
      return chase;
    }
    
  }
  
  
  public boolean actionOrderSurrender(Actor actor, Actor other) {
    
    final boolean authority = hasAuthority();
    final Venue holding = (Venue) (
      authority ? actor.mind.work() : other.mind.home()
    );
    if (holding == null) {
      gaveQuarter = true;
      return false;
    }
    
    final Summons surrender = new Summons(
      other, actor, holding,
      authority ? Summons.TYPE_CAPTIVE : Summons.TYPE_SULKING
    );
    final float commandBonus = DialogueUtils.talkResult(
      COMMAND, MODERATE_DC, actor, other
    ) * ROUTINE;
    surrender.setMotive(Plan.MOTIVE_EMERGENCY, commandBonus);
    
    if (! other.mind.mustIgnore(surrender)) {
      gaveQuarter = true;
      other.mind.assignBehaviour(surrender);
      return true;
    }
    else {
      gaveQuarter = true;
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
      d.append("Scolding ");
      d.append(subject);
    }
    else {
      if (super.needsSuffix(d, "Arresting ")) {
        d.append(subject);
      }
    }
  }
}












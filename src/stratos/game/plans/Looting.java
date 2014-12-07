

package stratos.game.plans;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.actors.*;
import stratos.game.base.*;
import stratos.util.*;
import stratos.game.economic.Inventory.Owner;
import static stratos.game.actors.Qualities.*;
import static stratos.game.economic.Economy.*;



//  You either sneak or use disguise to avoid attention.  If you've been seen,
//  then you run for it.

//  TODO:  Make this a general reaction/actor behaviour, and allow for multiple
//  items to be nicked in one sitting (as long as you can carry them!)


public class Looting extends Plan {
  
  
  /**  Data fields, construction and save/load methods-
    */
  private static boolean
    evalVerbose  = false,
    stepsVerbose = false;
  
  final static int
    TYPE_EXTORTION =  0,
    TYPE_BREAK_IN  =  1,
    TYPE_SCAVENGE  =  2,
    
    STAGE_INIT     = -1,
    STAGE_APPROACH =  0,
    STAGE_DROP     =  1,
    STAGE_DONE     =  2;
  
  final Owner mark;
  final Item taken;
  final Property dropOff;
  private int stage = STAGE_INIT;
  
  
  public Looting(Actor actor, Owner subject, Item taken, Property dropOff) {
    super(actor, subject, false, MILD_HARM);
    this.mark    = subject;
    this.taken   = taken  ;
    this.dropOff = dropOff;
  }
  
  
  public Plan copyFor(Actor other) {
    return new Looting(other, mark, taken, dropOff);
  }
  
  
  public Looting(Session s) throws Exception {
    super(s);
    mark    = (Owner) s.loadObject();
    taken   = Item.loadFrom(s);
    dropOff = (Property) s.loadObject();
    stage   = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(mark    );
    Item.saveTo (s, taken);
    s.saveObject(dropOff );
    s.saveInt   (stage   );
  }
  
  
  
  /**  Behaviour implementation-
    */
  final static Skill BASE_SKILLS[] = { STEALTH_AND_COVER, INSCRIPTION };
  final static Trait BASE_TRAITS[] = { DISHONEST, ACQUISITIVE };
  
  
  public static Looting nextLootingFor(Actor actor, Venue dropOff) {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (report) I.say("\nGetting next loot for "+actor);
    
    Item  bestTaken  = null;
    Owner bestMark   = null;
    float bestRating = 0   ;
    
    //  TODO:  Include dropped items as well...
    
    for (Target t : actor.senses.awareOf()) if (t instanceof Owner) {
      if (t == actor.mind.home() || t == actor.mind.work()) continue;
      
      //  TODO:  Just rate Lootings directly.
      
      final Owner mark = (Owner) t;
      for (Item taken : mark.inventory().allItems()) {
        taken = Item.withAmount(taken, Nums.min(1, taken.amount));
        final float rating = ActorMotives.rateDesire(taken, null, actor);
        
        if (rating * (1 + Rand.num()) > bestRating) {
          bestTaken  = taken ;
          bestMark   = mark  ;
          bestRating = rating;
          if (report) I.say("  Rating for "+taken+" at "+mark+" is "+rating);
        }
      }
    }
    
    if (bestTaken == null) return null;
    else return new Looting(actor, bestMark, bestTaken, dropOff);
  }
  
  
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    
    float urge = ActorMotives.rateDesire(taken, null, actor) / Plan.ROUTINE;
    
    if (mark.base() != null) {
      urge *= (1.5f - Planet.dayValue(actor.world()));  //  TODO:  USE SUCCESS-CHANCE INSTEAD
      if (mark.privateProperty()) urge -= 0.5f;
    }
    
    final float priority = priorityForActorWith(
      actor, mark,
      CASUAL * urge, CASUAL * (urge - 0.5f),
      MILD_HARM, FULL_COMPETITION, REAL_FAIL_RISK,
      BASE_SKILLS, BASE_TRAITS, NORMAL_DISTANCE_CHECK, report
    );
    if (report) {
      I.say("\n  Got theft priority for "+actor);
      I.say("  Source/taken:      "+mark+"/"+taken);
      I.say("  Private property?  "+mark.privateProperty());
      I.say("  Basic urge level:  "+urge);
    }
    return priority;
  }
  
  
  protected float successChance() {
    //  TODO:  Modify this to reflect ambient dangers (such as visibility,
    //  skill-set, day/night values, etc.)
    
    return super.successChance();
  }
  
  
  public int motionType(Actor actor) {
    if (CombatUtils.baseAttacked(actor) == null) {
      return super.motionType(actor);
    }
    if (stage == STAGE_APPROACH || stage == STAGE_DROP) {
      return Action.MOTION_SNEAK;
    }
    return super.motionType(actor);
  }
  
  
  protected int evaluationInterval() {
    return 1;
  }
  
  
  protected Behaviour getNextStep() {
    final boolean report = stepsVerbose && I.talkAbout == actor && hasBegun();
    if (report) I.say("\nGetting next step for looting.");
    
    if (stage == STAGE_DONE) {
      if (report) I.say("  Looting complete!");
      return null;
    }
    
    //  TODO:  Don't bother sneaking/hiding if you're, say, looting a body and
    //  no enemies are around.
    
    //  If you've been spotted, try to shake the pursuit!
    if (shouldHide()) {
      if (report) I.say("  You've been marked!  Scram!");
      
      final float range = actor.health.sightRange();
      final Target point = Retreat.pickHidePoint(actor, range, actor, false);
      final Action hide = new Action(
        actor, actor,
        this, "actionHide",
        Action.MOVE_SNEAK, "Hiding from "
      );
      hide.setProperties(Action.QUICK | Action.NO_LOOP);
      hide.setMoveTarget(point);
      return hide;
    }
    
    if (stage == STAGE_DROP) {
      if (dropOff == null) return null;
      if (report) I.say("  Dropping off goods at "+dropOff);
      
      final Action drop = new Action(
        actor, dropOff,
        this, "actionDropGoods",
        Action.REACH_DOWN, "Dropping off at "
      );
      return drop;
    }
    
    if (report) I.say("  Looting it is.");
    final Action loot = new Action(
      actor, mark,
      this, "actionLoot",
      Action.BUILD, "Looting "
    );
    if (mark instanceof Boarding);
    else loot.setMoveTarget(Spacing.nearestOpenTile(mark, actor));
    stage = STAGE_APPROACH;
    return loot;
  }
  
  
  private boolean shouldHide() {
    if (! hasBegun()) return false;
    final boolean report = stepsVerbose && I.talkAbout == actor;
    
    //  Technically, stealing counts as a form of 'attack'...
    final Base attacked = CombatUtils.baseAttacked(actor);
    if (attacked == null) return false;
    //  TODO:  Ignore other guild members?
    
    //  In essence, you flee if you're too close to a member of the base you're
    //  stealing from (and isn't the mark), or someone else has already made
    //  you a target:
    for (Plan p : actor.world().activities.activePlanMatches(actor, null)){
      if (report) I.say("  Somebody is targeting me: "+p.actor()+", "+p);
      if (p.actor().base() == attacked) return true;
    }
    if (actor.indoors()) return false;
    
    final float minRange = actor.health.sightRange() / 2;
    for (Target t : actor.senses.awareOf()) {
      if (! ((t instanceof Actor) && t.base() == attacked)) continue;
      if (t == mark || t == actor || ((Actor) t).indoors()) continue;
      if (Spacing.distance(t, actor) > minRange) continue;
      if (report) I.say("  Too close to "+t);
      return true;
    }
    return false;
  }
  
  
  public boolean actionHide(Actor actor, Actor self) {
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) I.say("\nHiding at "+self.origin());
    return Senses.breaksPursuit(actor);
  }
  
  
  public boolean actionLoot(Actor actor, Owner mark) {
    mark.inventory().transfer(taken, actor);
    if (
      actor.gear.amountOf(taken) >= 5 ||
      mark.inventory().amountOf(taken) == 0
    ) {
      if (dropOff != null) stage = STAGE_DROP;
      else stage = STAGE_DONE;
    }
    return true;
  }
  
  
  public boolean actionTakeCredits(Actor actor, Owner mark) {
    mark.inventory().incCredits(-10);
    actor.gear.incCredits(10);
    return true;
  }
  
  
  public boolean actionDropGoods(Actor actor, Property drop) {
    actor.gear.transfer(taken.type, drop);
    stage = STAGE_DONE;
    return true;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (actor.isDoingAction("actionHide", null)) {
      d.append("Hiding!");
      return;
    }
    if (stage == STAGE_DROP) {
      d.append("Dropping off ");
      d.append(taken.type);
      d.append(" at ");
      d.append(dropOff);
    }
    if (stage <= STAGE_APPROACH) {
      d.append("Looting ");
      d.append(taken.type);
      d.append(" from ");
      d.append(mark);
    }
  }
}









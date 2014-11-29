

package stratos.game.plans;
import org.apache.commons.math3.util.FastMath;

import stratos.game.common.*;
import stratos.game.maps.Planet;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.base.*;
import stratos.util.*;
import stratos.game.building.Inventory.Owner;
import static stratos.game.actors.Qualities.*;
import static stratos.game.building.Economy.*;



public class Looting extends Plan {
  
  
  /**  Data fields, construction and save/load methods-
    */
  private static boolean
    evalVerbose  = true ,
    stepsVerbose = true ;
  
  final Owner mark;
  final Item taken;
  
  
  public Looting(Actor actor, Owner subject, Item taken) {
    super(actor, subject, false);
    this.mark  = subject;
    this.taken = taken;
  }
  
  
  public Plan copyFor(Actor other) {
    return new Looting(other, mark, taken);
  }
  
  
  public Looting(Session s) throws Exception {
    super(s);
    mark  = (Owner) s.loadObject();
    taken = Item.loadFrom(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(mark);
    Item.saveTo(s, taken);
  }
  
  
  
  /**  Behaviour implementation-
    */
  final static Skill BASE_SKILLS[] = { STEALTH_AND_COVER, INSCRIPTION };
  final static Trait BASE_TRAITS[] = { DISHONEST, ACQUISITIVE };
  
  
  public static Looting nextLootingFor(Actor actor) {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (report) I.say("\nGetting next loot for "+actor);
    
    Item  bestTaken  = null;
    Owner bestMark   = null;
    float bestRating = 0   ;
    
    //  TODO:  Specify where to drop off the loot!
    //  TODO:  Include dropped items as well...
    
    for (Target t : actor.senses.awareOf()) if (t instanceof Owner) {
      if (t == actor.mind.home() || t == actor.mind.work()) continue;
      
      //  TODO:  Just rate Lootings directly.
      
      final Owner mark = (Owner) t;
      for (Item taken : mark.inventory().allItems()) {
        final float rating = ActorDesires.rateDesire(taken, null, actor);
        if (rating * (1 + Rand.num()) > bestRating) {
          bestTaken  = Item.withAmount(taken, FastMath.min(1, taken.amount));
          bestMark   = mark  ;
          bestRating = rating;
          if (report) I.say("  Rating for "+taken+" at "+mark+" is "+rating);
        }
      }
    }
    
    if (bestTaken == null) return null;
    else return new Looting(actor, bestMark, bestTaken);
  }
  
  
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    
    float urge = ActorDesires.rateDesire(taken, null, actor) / Plan.ROUTINE;
    
    urge *= (1.5f - Planet.dayValue(actor.world()));  //  TODO:  USE SUCCESS-CHANCE INSTEAD
    if (mark.privateProperty()) urge -= 0.5f;
    
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
    return Action.MOTION_SNEAK;
  }
  
  
  protected Behaviour getNextStep() {
    //  In essence, you just need to sneak up on the target without being seen
    //  and nick their stuff.
    
    //  TODO:  Return the goods to a drop, if specified
    if (mark.inventory().amountOf(taken) <= 0) return null;
    if (actor.gear.hasItem(taken)) return null;
    
    final Action loot = new Action(
      actor, mark,
      this, "actionLoot",
      Action.REACH_DOWN, "Looting"
    );
    return loot;
  }
  
  
  public boolean actionLoot(Actor actor, Owner mark) {
    mark.inventory().transfer(taken, actor);
    return true;
  }
  
  
  public boolean actionTakeCredits(Actor actor, Owner mark) {
    mark.inventory().incCredits(-10);
    actor.gear.incCredits(10);
    return true;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    d.append("Looting ");
    d.append(taken);
    d.append(" from ");
    d.append(mark);
  }
}










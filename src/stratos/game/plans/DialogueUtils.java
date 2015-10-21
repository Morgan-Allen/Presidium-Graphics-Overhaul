/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;



public class DialogueUtils {
  
  
  /**  Helper methods for elaborating on chat options-
    */
  private static Skill languageFor(Actor other) {
    if (other.health.animal  ()) return XENOZOOLOGY;
    if (other.health.artilect()) return INSCRIPTION;
    if (other.health.human   ()) {
      final int standing = other.mind.vocation().standing;
      if (standing == Backgrounds.CLASS_STRATOI) return NOBLE_ETIQUETTE;
      if (standing == Backgrounds.CLASS_NATIVE ) return NATIVE_TABOO   ;
      return COMMON_CUSTOM;
    }
    return null;
  }
  
  
  public static float communicationChance(Actor actor, Actor other) {
    final Skill language = languageFor(other);
    if (language == null) return 0;
    return actor.skills.chance(language, ROUTINE_DC);
  }
  
  
  public static void reinforceRelations(
    Actor actor  , Actor other,
    float toLevel, float noveltyInc,
    boolean symmetric
  ) {
    if (noveltyInc < 0) noveltyInc = -1f / Dialogue.BORED_DURATION;
    final float w = 0.1f;  //  Weight- use a constant?
    other.relations.incRelation(actor, toLevel, w, noveltyInc);
    if (symmetric) {
      actor.relations.incRelation(other, toLevel, w, noveltyInc);
    }
  }
  
  
  public static float talkResult(
    Skill plea, float opposeDC, Actor actor, Actor other
  ) {
    final Skill language = languageFor(other);
    final float attBonus = other.relations.valueFor(actor) * ROUTINE_DC;
    int result = 0;
    final Action a = actor.currentAction();
    result += actor.skills.test(language, ROUTINE_DC - attBonus, 1, a) ? 1 : 0;
    result += actor.skills.test(plea    , opposeDC   - attBonus, 1, a) ? 1 : 0;
    return result / 2f;
  }
  
  
  
  
  final static float
    CHAT_RELATION_BOOST = 0.5f;
  
  //  TODO:  Okay.  I want to make sure that relationships can actually go
  //  pretty far north or south, depending on how well traits and interests
  //  match up.
  public static float tryChat(Actor actor, Actor other) {
    float boost = talkResult(SUASION, TRIVIAL_DC, actor, other) / 10f;
    boost *= CHAT_RELATION_BOOST;
    reinforceRelations(other, actor, boost, -1, false);
    return boost;
  }
  
  
  protected static void discussPerson(Actor actor, Actor other, Actor about) {
    final float
      attA = actor.relations.valueFor(about),
      attO = other.relations.valueFor(about);
    
    final boolean agrees = Nums.abs(attA - attO) < 0.5f;
    final float effect = 0.2f * (agrees ? 1 : -1) * CHAT_RELATION_BOOST;
    
    reinforceRelations(actor, other, effect / 2   , -1, true);
    reinforceRelations(other, about, effect * attO,  0, false);
  }
  
  
  protected static void discussSkills(Actor actor, Actor other, Skill about) {
    final float level = actor.traits.usedLevel(about);
    //  TODO:  Use the Counsel skill here.
    
    final Action a = other.currentAction();
    float effect = 0;
    if (other.skills.test(about, level / 2, 0.5f, a)) effect += 5;
    else effect -= 5;
    if (other.skills.test(about, level * Rand.num(), 0.5f, a)) effect += 5;
    else effect -= 5;
    
    effect *= CHAT_RELATION_BOOST / 25f;
    reinforceRelations(actor, other, effect, -1, true);
  }
  
  
  protected static void discussPlan(Actor actor, Actor other, Plan about) {
    final Plan copy = about.copyFor(other);
    if (copy == null) return;
    
    final float urge = DialogueUtils.talkResult(
      SUASION, ROUTINE_DC, actor, other
    ) * Plan.CASUAL;
    copy.addMotives(Plan.MOTIVE_LEISURE, urge + copy.motiveBonus());
    
    //  TODO:  Compare with whatever the actor has on their todo-list, or from
    //  a mission.
    if (! other.mind.wouldSwitchTo(copy)) return;
    
    actor.mind.assignBehaviour(new Joining(actor, about, other));
    other.mind.assignBehaviour(new Joining(other, copy , actor));
  }
  
  
  protected static void discussEvent(Actor actor, Actor other, Memory about) {
    
  }
  
  
  
  protected static Session.Saveable pickChatTopic(
    Dialogue starts, Actor other
  ) {
    final Actor actor = starts.actor();
    final Pick <Session.Saveable> pick = new Pick <Session.Saveable> ();
    
    if (actor instanceof Human) {
      //  TODO:  Make these especially compelling during introductions?
      final Career c = ((Human) actor).career();
      //pick.compare(c.birth()    , Rand.num());
      pick.compare(c.homeworld(), Rand.num());
      //pick.compare(c.vocation() , Rand.num());
    }
    
    for (Relation r : actor.relations.relations()) {
      if (r.subject == other || r.subject == actor) continue;
      if (r.subject == other.base()) continue;
      final float
        otherR = other.relations.valueFor(r.subject),
        rating = (Nums.abs(otherR * r.value()) + 0.5f) * Rand.num();
      pick.compare((Session.Saveable) r.subject, rating);
    }
    
    for (Skill s : other.traits.skillSet()) {
      final float
        levelA = actor.traits.usedLevel(s),
        levelO = other.traits.usedLevel(s),
        rating = Nums.min(levelA, levelO) * Rand.num() / 10f;
      if (levelA < 0 || levelO < 0) continue;
      pick.compare(s, rating);
    }
    
    for (Behaviour b : actor.mind.todoList()) if (b instanceof Plan) {
      final Plan about = (Plan) b;
      final float rating = about.priorityFor(actor) * Rand.num() / 5;
      pick.compare(about, rating);
    }
    //  ...There has to be other stuff that an actor could suggest, such as at
    //  the close of conversation?  Try for that.
    
    
    return pick.result();
  }
  
  
  //  TODO:  Present these options as an array for dialogue by the sovereign-
  //         then they can pick and choose which to use.
  
  
  //  Memory- "<X> happened.  it was great/terrible!"
  
  //  TODO:  I'll need to have the memory system in place first, in order to
  //  present a more logical system.  (And you might consider reserving this
  //  *just* for conversations with the sovereign.)
  
  
  
  //  Skill- "are you interested in <X>?"
  
  //  Base this off the activity (if it was good.)
  
  
  
  //  Person- "do you know <X>?  what are they like?"
  
  //  Base this off the person responsible for the memory.
  
  
  
  //  Plan- "would you like to do <X>?"
  
  //  ...Reserve this for the end of the conversation, and only if relations
  //  are pretty good and novelty is high.
}








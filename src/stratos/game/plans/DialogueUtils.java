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
    if (other.health.artilect()) return LOGIC;
    if (other.health.human   ()) {
      final int standing = other.mind.vocation().standing;
      if (standing == Backgrounds.CLASS_STRATOI) return ETIQUETTE;
      if (standing == Backgrounds.CLASS_NATIVE ) return NATIVE_TABOO   ;
      return COMMON_CUSTOM;
    }
    return null;
  }
  
  
  public static float communicationChance(Actor actor, Actor other) {
    final Skill language = languageFor(other);
    if (language == null) return 0;
    float chance = 1;
    if (! actor.traits.hasTrait(language)) {
      if (actor.species() != other.species()) return 0;
      else chance = 0.5f;
    }
    return chance * actor.skills.chance(language, ROUTINE_DC);
  }
  
  
  public static void reinforceRelations(
    Actor actor, Actor other,
    float toLevel, float noveltyInc, boolean symmetric
  ) {
    if (noveltyInc < 0) noveltyInc = -1f / Dialogue.BORED_DURATION;
    final float w = 1f / Dialogue.BORED_DURATION;
    other.relations.incRelation(actor, toLevel, w, noveltyInc);
    if (symmetric) {
      actor.relations.incRelation(other, toLevel, w, noveltyInc);
    }
  }
  
  
  public static float talkChance(
    Skill plea, float opposeDC, Actor actor, Actor other
  ) {
    return talkTest(plea, opposeDC, actor, other, true);
  }
  
  
  public static float talkResult(
    Skill plea, float opposeDC, Actor actor, Actor other
  ) {
    return talkTest(plea, opposeDC, actor, other, false);
  }
  
  
  private static float talkTest(
    Skill plea, float opposeDC, Actor actor, Actor other, boolean chanceOnly
  ) {
    final Skill language = languageFor(other);
    final float attBonus = other.relations.valueFor(actor) * ROUTINE_DC;
    
    if (chanceOnly) {
      float chanceL = actor.skills.chance(language, ROUTINE_DC - attBonus);
      float chanceP = actor.skills.chance(plea    , opposeDC   - attBonus);
      return chanceL * chanceP;
    }
    else {
      final Action a = actor.currentAction();
      int result = 1;
      if (! actor.skills.test(language, ROUTINE_DC - attBonus, 1, a)) {
        result = 0;
      }
      if (! actor.skills.test(plea    , opposeDC   - attBonus, 1, a)) {
        result = 0;
      }
      if (I.talkAbout == other && Dialogue.stepsVerbose) {
        I.say("\nGetting talk result between "+actor+" and "+other);
        I.say("  Language is: "+language+", plea with: "+plea);
        I.say("  Oppose DC: "+opposeDC+", result is:   "+result  );
      }
      return result;
    }
  }
  
  
  
  
  /**  Common topics-
    */
  final static float
    CHAT_RELATION_BOOST = 0.5f;
  
  final static Constant.Storage
    CHAT_LINES[] = Constant.storeConstants(
      DialogueUtils.class, "chat_lines",
      "the weather",
      "who's a good boy"
    ),
    LINE_WEATHER = CHAT_LINES[0],
    LINE_ANIMAL  = CHAT_LINES[1];
  
  
  public static float tryChat(Actor actor, Actor other, int bonus) {
    float boost = talkResult(SUASION, TRIVIAL_DC - bonus, actor, other);
    boost *= CHAT_RELATION_BOOST;
    reinforceRelations(other, actor, boost, -1, false);
    return boost;
  }
  
  
  //  TODO:  Show speech bubbles!  If one of the participants is selected.
  
  //  TODO:  Discuss memories using information from the Career class.
  
  
  public static Session.Saveable pickChatTopic(
    Actor actor, Actor other, final Batch <Session.Saveable> record
  ) {
    
    if (! (actor.species().sapient() && other.species().sapient())) {
      if (record != null) record.add(LINE_ANIMAL);
      return LINE_ANIMAL;
    }
    
    final Pick <Session.Saveable> pick = new Pick() {
      public void compare(Object next, float rating) {
        if (record != null) record.add((Session.Saveable) next);
        super.compare(next, rating);
      }
    };
    
    if (actor instanceof Human) {
      //  TODO:  Make these especially compelling during introductions?
      final Career c = ((Human) actor).career();
      pick.compare(c.homeworld(), Rand.num());
      pick.compare(LINE_WEATHER, Rand.num());
    }
    
    for (Relation r : actor.relations.allRelations()) {
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
      final float rating = about.priority() * Rand.num() / 5;
      pick.compare(about, rating);
    }
    //  ...There has to be other stuff that an actor could suggest, such as at
    //  the close of conversation?  Try for that.
    
    return pick.result();
  }
  
  
  public static void discussTopic(
    Session.Saveable topic, boolean close, int checkBonus,
    Actor actor, Actor other
  ) {
    DialogueUtils.tryChat(actor, other, checkBonus);
    
    if (topic instanceof Actor ) {
      DialogueUtils.discussPerson(actor, other, (Actor ) topic);
      return;
    }
    if (topic instanceof Skill ) {
      DialogueUtils.discussSkills(actor, other, (Skill ) topic);
      return;
    }
    if (topic instanceof Plan  ) {
      DialogueUtils.discussPlan  (actor, other, (Plan  ) topic);
      return;
    }
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
  
}








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
  final public static Index <ChatLine> CHAT_TOPICS = new Index();
  
  public static class ChatLine extends Index.Entry implements Session.Saveable {
    
    final String line;
    
    ChatLine(String ID, String line) {
      super(CHAT_TOPICS, ID);
      this.line = line;
    }
    
    public static ChatLine loadConstant(Session s) throws Exception {
      return CHAT_TOPICS.loadEntry(s.input());
    }
    
    public void saveState(Session s) throws Exception {
      CHAT_TOPICS.saveEntry(this, s.output());
    }
    
    public String toString() {
      return line;
    }
  }
  
  final static ChatLine
    LINE_WEATHER = new ChatLine("line_weather", "the weather"),
    LINE_ANIMAL  = new ChatLine("line_animal", "'Who's a good boy then?'");
  
  
  final static float
    CHAT_RELATION_BOOST = 0.5f;
  
  
  protected static Session.Saveable pickChatTopic(
    Dialogue starts, Actor other
  ) {
    final Actor actor = starts.actor();
    final Pick <Session.Saveable> pick = new Pick <Session.Saveable> ();
    
    if (! (actor.species().sapient() && other.species().sapient())) {
      return LINE_ANIMAL;
    }
    
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
  
  
  //  TODO:  Okay.  I want to make sure that relationships can actually go
  //  pretty far north or south, depending on how well traits and interests
  //  match up.
  public static float tryChat(Actor actor, Actor other, int bonus) {
    float boost = talkResult(SUASION, TRIVIAL_DC - bonus, actor, other);
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








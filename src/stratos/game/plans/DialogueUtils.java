


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.politic.Transcript;
import stratos.graphics.sfx.TalkFX;
import stratos.util.*;



public class DialogueUtils implements Qualities {
  
  
  /**  Helper methods for elaborating on chat options-
    */
  private static Skill languageFor(Actor other) {
    if (other.health.animal  ()) return XENOZOOLOGY;
    if (other.health.artilect()) return INSCRIPTION;
    if (other.health.human   ()) {
      final int standing = other.vocation().standing;
      if (standing == Backgrounds.CLASS_STRATOI) return NOBLE_ETIQUETTE;
      if (standing == Backgrounds.CLASS_NATIVE ) return NATIVE_TABOO   ;
      return COMMON_CUSTOM;
    }
    return null;
  }
  
  
  public static void reinforceRelations(
    Actor actor  , Actor other,
    float toLevel, float noveltyInc,
    boolean symmetric
  ) {
    if (noveltyInc < 0) noveltyInc = -1f / Dialogue.BORED_DURATION;
    final float w = 0.1f;  //  Weight- use a constant?
    final float g = 4.0f;  //  Generalisation ratio.  (Should be constant?)
    other.relations.incRelation(actor       , toLevel, w    , noveltyInc    );
    other.relations.incRelation(actor.base(), toLevel, w / g, noveltyInc / g);
    if (symmetric) {
      actor.relations.incRelation(other       , toLevel, w    , noveltyInc    );
      actor.relations.incRelation(other.base(), toLevel, w / g, noveltyInc / g);
    }
  }
  
  
  public static float talkResult(
    Skill plea, float opposeDC, Actor actor, Actor other
  ) {
    final Skill language = languageFor(other);
    final float attBonus = other.relations.valueFor(actor) * ROUTINE_DC;
    int result = 0;
    result += actor.skills.test(language, ROUTINE_DC - attBonus, 1) ? 1 : 0;
    result += actor.skills.test(plea, opposeDC - attBonus, 1) ? 1 : 0;
    return result / 2f;
  }
  
  
  public static float tryChat(Actor actor, Actor other) {
    
    final float DC = other.traits.usedLevel(SUASION) / 2;
    float success = talkResult(SUASION, DC, actor, other);
    success *= Dialogue.RELATION_BOOST;
    reinforceRelations(other, actor, success, -1, false);
    
    switch (Rand.index(3)) {
      case (0) : anecdote(actor, other); break;
      case (1) : gossip  (actor, other); break;
      case (2) : advise  (actor, other); break;
    }
    return success;
  }
  
  
  private static void smalltalk(Actor actor, Actor other) {
    utters(actor, "Nice weather, huh?", 0);
    utters(other, "Uh-huh.", 0);
  }
  
  
  private static void anecdote(Actor actor, Actor other) {
    //
    //  Pick a random recent activity and see if the other also indulged in it.
    //  If the activity is similar, or was undertaken for similar reasons,
    //  improve relations.
    //  TODO:  At the moment, we just compare traits.  Fix later.
    
    utters(other, "What is best in life?", 0);
    
    Trait comp = null;
    float bestRating = 0;
    for (Trait t : actor.traits.personality()) {
      final float
        levelA = actor.traits.relativeLevel(t),
        levelO = actor.traits.relativeLevel(t),
        rating = (2f - Nums.abs(levelA - levelO)) * Rand.num();
      if (rating > bestRating) { bestRating = rating; comp = t; }
    }
    if (comp == null) {
      utters(actor, "Uh... I don't know.", 0);
      return;
    }
    final float
      levelA = actor.traits.relativeLevel(comp),
      levelO = other.traits.relativeLevel(comp),
      similarity = (1 - Nums.abs(levelA - levelO));
    final String desc = actor.traits.description(comp);
    
    final float effect = similarity * Dialogue.RELATION_BOOST;
    DialogueUtils.reinforceRelations(actor, other, effect, -1, true);
    
    utters(actor, "It's important to be "+desc+".", 0);
    if (similarity > 0.5f) utters(other, "Absolutely.", effect);
    else if (similarity < -0.5f) utters(other, "No way!", effect);
    else utters(other, "Yeah, I guess...", effect);
  }
  
  
  private static void gossip(Actor actor, Actor other) {
    //
    //  Pick an acquaintance, see if it's mutual, and if so compare attitudes
    //  on the subject.  TODO:  Include memories of recent activities?
    Relation pick = null;
    float bestRating = 0;
    for (Relation r : actor.relations.relations()) {
      if (r.subject == other || ! (r.subject instanceof Actor)) continue;
      final float
        otherR = other.relations.valueFor(r.subject),
        rating = (Nums.abs(otherR * r.value()) + 0.5f) * Rand.num();
      if (rating > bestRating) { pick = r; bestRating = rating; }
    }
    
    if (pick == null) {
      smalltalk(actor, other);
      return;
    }
    final Actor about = (Actor) pick.subject;
    final float
      attA = actor.relations.valueFor(about),
      attO = other.relations.valueFor(about);
    
    final boolean agrees = Nums.abs(attA - attO) < 0.5f;
    final float effect = 0.2f * (agrees ? 1 : -1) * Dialogue.RELATION_BOOST;
    reinforceRelations(actor, other, effect / 2, -1, true);
    reinforceRelations(other, about, effect * pick.value(), 0, false);
    
    utters(other, "What do you think of "+about+"?", 0);
    if (attA > 0.33f) {
      utters(actor, "We get along pretty well!", effect);
    }
    else if (attA < 0.33f) {
      utters(actor, "We don't get along.", effect);
    }
    else {
      utters(actor, "We get along okay...", effect);
    }
    if (agrees) utters(other, "Same here.", effect);
    else utters(other, "Really?", effect);
  }
  
  
  private static void advise(Actor actor, Actor other) {
    utters(other, "So, what do you do?", 0);
    
    Skill tested = null;
    float bestRating = 0;
    for (Skill s : other.traits.skillSet()) {
      final float
        levelA = actor.traits.usedLevel(s),
        levelO = other.traits.usedLevel(s),
        rating = levelA * levelO * Rand.num();
      if (levelA < 0 || levelO < 0) continue;
      if (rating > bestRating) { tested = s; bestRating = rating; }
    }
    
    if (tested == null) {
      utters(actor, "Oh, nothing you'd find interesting.", 0);
      return;
    }
    final float level = actor.traits.usedLevel(tested);
    utters(actor, "Well, I'm interested in "+tested+".", 0);
    utters(actor, "Let me show you a few tricks...", 0);
    
    //  TODO:  Use the Counsel skill here.
    float effect = 0;
    if (other.skills.test(tested, level / 2, 0.5f)) effect += 5;
    else effect -= 5;
    if (other.skills.test(tested, level * Rand.num(), 0.5f)) effect += 5;
    else effect -= 5;
    effect *= Dialogue.RELATION_BOOST / 25f;
    reinforceRelations(actor, other, effect, -1, true);
    
    if (effect > 0) {
      utters(other, "You mean like this?", effect);
      utters(actor, "Yes, exactly!", effect);
    }
    if (effect == 0) {
      utters(other, "You mean like this?", effect);
      utters(actor, "Close. Try again.", effect);
    }
    if (effect < 0) {
      utters(other, "You mean like this?", effect);
      utters(actor, "No, that's not it.", effect);
    }
  }
  
  
  
  //
  //  TODO:  It might be an idea to try restricting this purely to anecdotes
  //  (stuff you've done) and proposals/objections (stuff you'd like to do.)
  //  Polish that up and make as engaging as possible.
  
  //  Put advice/study/instruction under a separate plan type?  Or have lines
  //  to speak for every skill, so they sound less generic?
  
  
  
  
  /**  Rendering and interface utility methods-
    */
  final static Vec3D forwardVec = new Vec3D(1, 1, 0);
  private static boolean onRight(Actor a, Target b) {
    final Vec3D disp = a.position(null).sub(b.position(null));
    return disp.dot(forwardVec) > 0;
  }
  
  
  public static void utters(Actor a, String s, float effect) {
    if (a.indoors()) return;
    
    final String sign;
    if (effect == 0) sign = "";
    else if (effect > 0) sign = " (+)";
    else sign = " (-)";
    
    final Target opposite = a.planFocus(Dialogue.class, true);
    final int side = (opposite == null) ? TalkFX.FROM_RIGHT : (
      onRight(a, opposite) ? TalkFX.FROM_RIGHT : TalkFX.FROM_LEFT
    );
    
    final TalkFX chat = DialogueFX.transcriptFor(a);
    if (chat != null) chat.addPhrase(s+sign, side);
  }
  
  
  public Transcript transcript() {
    return null;
  }
}










package stratos.game.civilian ;
import org.apache.commons.math3.util.FastMath;

import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.graphics.sfx.TalkFX;
import stratos.user.*;
import stratos.util.*;


/*
TODO:  Incorporate effects of community spirit for novel acquaintances.
Basic dialogue effects can also happen spontaneously.
//*/

//  ...Actors also need to use dialogue to 'object' when they see someone
//  doing something they don't approve of *to* someone else.

//  TODO:  You need to restore the use of a communal ChatFX for a given instance
//  of dialogue.  (And have everything fade once complete.)


public class Dialogue extends Plan implements Qualities {
  
  
  /**  Constants, data fields, constructors and save/load methods-
    */
  final public static int
    TYPE_CONTACT   = 0,
    TYPE_CASUAL    = 1,
    TYPE_OBJECTION = 2;
  
  final static int
    STAGE_INIT  = -1,
    STAGE_GREET =  0,
    STAGE_CHAT  =  1,
    STAGE_BYE   =  2,
    STAGE_DONE  =  3;
  
  private static boolean
    evalVerbose   = false,
    eventsVerbose = true ;
  
  
  final Actor starts, other;
  final int type;
  private int stage = STAGE_INIT;
  private Boardable location = null, stands = null;
  
  
  
  public Dialogue(Actor actor, Actor other, int type) {
    this(actor, other, actor, type) ;
  }
  
  
  private Dialogue(Actor actor, Actor other, Actor starts, int type) {
    super(actor, other) ;
    if (actor == other) I.complain("CANNOT TALK TO SELF!") ;
    this.other = other ;
    this.starts = starts ;
    this.type = type ;
  }
  
  
  public Dialogue(Session s) throws Exception {
    super(s) ;
    other  = (Actor) s.loadObject() ;
    starts = (Actor) s.loadObject() ;
    type = s.loadInt() ;
    stage = s.loadInt() ;
    location = (Boardable) s.loadTarget() ;
    stands = (Boardable) s.loadTarget() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(other ) ;
    s.saveObject(starts) ;
    s.saveInt(type) ;
    s.saveInt(stage) ;
    s.saveTarget(location) ;
    s.saveTarget(stands) ;
  }
  
  
  
  /**  Target selection and priority evaluation-
    */
  final static Trait BASE_TRAITS[] = { OUTGOING, POSITIVE, EMPATHIC };
  final static Skill BASE_SKILLS[] = { SUASION, TRUTH_SENSE };
  
  
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;
    final Relation r = actor.memories.relationWith(other);
    final float curiosity = (1 + actor.traits.relativeLevel(CURIOUS)) / 2;
    
    float urgency = 0;
    if (r == null) {
      urgency += curiosity;
      urgency -= 5f / (10 + actor.memories.relations().size());
    }
    else {
      urgency += r.novelty() * curiosity;
      urgency += r.value() / 2f;
    }
    final boolean casual = type == TYPE_CASUAL;
    urgency = Visit.clamp(urgency, -1, 1);
    float distCheck = NORMAL_DISTANCE_CHECK;
    
    if (casual) {
      if (urgency <= 0) return 0;
      if (stage >= STAGE_DONE || ! canTalk(other)) return 0;
      distCheck = HEAVY_DISTANCE_CHECK;
    }
    
    final float priority = priorityForActorWith(
      actor, other, casual ? (ROUTINE * urgency) : URGENT,
      MILD_HELP, NO_COMPETITION,
      BASE_SKILLS, BASE_TRAITS,
      NO_MODIFIER, distCheck, NO_FAIL_RISK,
      report
    );
    return priority;
  }
  
  
  private boolean canTalk(Actor other) {
    if (! other.health.conscious()) return false;
    final Target talksWith = other.focusFor(Dialogue.class);
    if (talksWith == actor) return true;
    //if (talksWith != null) return false;
    if (starts != actor) return false;
    final Dialogue d = new Dialogue(other, actor, actor, type);
    return ! other.mind.mustIgnore(d);
  }
  
  
  private Batch <Dialogue> sides() {
    final World world = actor.world() ;
    final Batch <Dialogue> batch = new Batch <Dialogue> () ;
    
    batch.include(this) ;
    Dialogue oD = (Dialogue) other.matchFor(Dialogue.class) ;
    if (oD != null) batch.include(oD) ;
    
    for (Behaviour b : world.activities.targeting(other)) {
      if (b instanceof Dialogue) {
        final Dialogue d = (Dialogue) b ;
        batch.include(d) ;
      }
    }
    return batch ;
  }
  
  
  private void setLocationFor(Action talkAction) {
    final boolean report = I.talkAbout == actor;
    final Batch <Dialogue> sides = sides() ;
    if (location == null) {
      
      //  If a location has not already been assigned, look for one either used
      //  by existing conversants, or find a new spot nearby.
      for (Dialogue d : sides) if (d.location != null) {
        this.location = d.location ; break ;
      }
      if (location == null) {
        if (other.indoors() && starts.indoors()) location = other.aboard();
        else location = Spacing.bestMidpoint(starts, other);
        if (report) I.say("Initialising talk location: "+location);
      }
    }
    
    if (location instanceof Tile) {
      
      //  In the case of an open-air discussion, you need to find appropriate
      //  spots to stand around in.  Mark any spot claimed by other conversants
      //  and try to find one unused-
      for (Dialogue s : sides) if (s != this && s.stands != null) {
        s.stands.flagWith(s) ;
      }
      float minDist = Float.POSITIVE_INFINITY ;
      this.stands = null ;
      for (Tile t : ((Tile) location).allAdjacent(null)) {
        if (t == null) continue ;
        if (t.blocked() || t.flaggedWith() != null) continue ;
        final float dist = Spacing.distance(t, actor) ;
        if (dist < minDist) { stands = t ; minDist = dist ; }
      }
      for (Dialogue s : sides) if (s != this && s.stands != null) {
        s.stands.flagWith(null) ;
      }
      
      //  If none is available, quit and return.  Otherwise, walk over there.
      if (stands == null) { abortBehaviour() ; return ; }
      talkAction.setMoveTarget(stands) ;
    }
    else if (location instanceof Boardable) {
      
      //  In the case of an indoor discussion, just set the right venue.  If
      //  nothing else was available, quit and return.
      talkAction.setMoveTarget(location) ;
    }
    else abortBehaviour() ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    if (stage >= STAGE_DONE) return null ;
    if (type != TYPE_CONTACT && ! canTalk(other)) {
      abortBehaviour() ;
      return null ;
    }
    
    if (starts == actor && stage == STAGE_INIT) {
      final Action greeting = new Action(
        actor, other.aboard(),
        this, "actionGreet",
        Action.TALK, "Greeting "
      ) ;
      greeting.setProperties(Action.RANGED) ;
      return greeting ;
    }
    
    if (stage == STAGE_CHAT) {
      Actor chatsWith = other;
      if (type == TYPE_CASUAL) {
        chatsWith = ((Dialogue) Rand.pickFrom(sides())).actor;
        if (chatsWith == actor) chatsWith = other;
      }
      final Action chats = new Action(
        actor, chatsWith,
        this, "actionChats",
        Action.TALK_LONG, "Chatting with "
      ) ;
      setLocationFor(chats) ;
      return chats ;
    }
    
    if (stage == STAGE_BYE) {
      final Action farewell = new Action(
        actor, other,
        this, "actionFarewell",
        Action.TALK, "Saying farewell to "
      ) ;
      farewell.setProperties(Action.RANGED) ;
      return farewell ;
    }
    
    return null ;
  }
  
  
  public boolean actionGreet(Actor actor, Boardable aboard) {
    if (! other.isDoing(Dialogue.class, null)) {
      if (! canTalk(other)) {
        if (type == TYPE_CASUAL) abortBehaviour() ;
        return false ;
      }
      final Dialogue d = new Dialogue(other, actor, type) ;
      d.stage = STAGE_CHAT ;
      other.mind.assignBehaviour(d) ;
    }
    this.stage = STAGE_CHAT ;
    return true ;
  }
  
  
  public boolean actionChats(Actor actor, Actor other) {
    tryChat(actor, other) ;
    final float novelty = actor.memories.relationNovelty(other) ;
    if (novelty <= 0) {
      //I.say("  NO REMAINING NOVELTY!  SAYING BYE!");
      stage = STAGE_BYE ;
    }
    return true ;
  }
  
  
  public boolean actionFarewell(Actor actor, Actor other) {
    //  Used to close a dialogue.
    stage = STAGE_DONE ;
    return true ;
  }
  
  
  public boolean actionGift(Actor actor, Actor other) {
    //  (Base on novelty.)
    return true ;
  }
  
  
  public boolean actionAskFavour(Actor actor, Actor other) {
    //  (Base on relationship strength.)
    
    //  TODO:  Create a copy of your upcoming behaviour (something new, or on
    //  the todo-list,) pass it to the other for evaluation, and if they agree,
    //  you can both head off for that.
    return true ;
  }
  
  
  
  /**  Helper methods for elaborating on chat options-
    */
  private static Skill languageFor(Actor other) {
    if (other.health.animal()) return XENOZOOLOGY;
    if (other.health.artilect()) return INSCRIPTION;
    if (other.health.human()) {
      final int standing = other.vocation().standing;
      if (standing == Backgrounds.CLASS_STRATOI) return NOBLE_ETIQUETTE;
      if (standing == Backgrounds.CLASS_NATIVE ) return NATIVE_TABOO;
      return COMMON_CUSTOM;
    }
    return null;
  }
  
  
  public static float talkResult(
    Skill plea, float opposeDC, Actor actor, Actor other
  ) {
    final Skill language = languageFor(other);
    final float attBonus = other.memories.relationValue(actor) * ROUTINE_DC;
    int result = 0;
    result += actor.traits.test(language, ROUTINE_DC - attBonus, 1) ? 1 : 0;
    result += actor.traits.test(plea, opposeDC - attBonus, 1) ? 1 : 0;
    return result / 2f;
  }
  
  
  public static float tryChat(Actor actor, Actor other) {
    //  Base on comparison of recent activities and associated traits, skills
    //  or actors involved.
    
    final float DC = other.traits.useLevel(SUASION) / 2;
    float success = talkResult(SUASION, DC, actor, other) ;
    other.memories.incRelation(actor, success * Relation.MAG_CHATTING, 0.1f) ;
    
    switch (Rand.index(3)) {
      case (0) : anecdote(actor, other) ; break ;
      case (1) : gossip  (actor, other) ; break ;
      case (2) : advise  (actor, other) ; break ;
    }
    return success ;
  }
  
  
  //  Basic introductions.  (Name, origin, job, health, weather, events.)
  //  Recent activities.
  //  Other acquaintances.
  //  Shared skills.
  //  Ask for favour.
  //  Give a gift.
  
  //  List associations with each, and discuss those.
  //  Make statements, and agree or disagree.
  
  /*
  private Batch associationsFor(Object topic) {
    final Batch assoc = new Batch();
    if (topic instanceof Plan) {
      assoc.addAll(topic.keyTraits());
      assoc.addAll(topic.keySkills());
    }
    if (topic instanceof Actor) {
      assoc.add(topic.vocation());
      for (Plan p : actor.memories.recent()) {
        if (p.subject() == topic) assoc.add(p);
      }
    }
    if (topic instanceof Background) {
      
    }
    if (topic instanceof Skill) {
      
    }
    return assoc;
  }
  //*/
  
  
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
        rating = (2f - FastMath.abs(levelA - levelO)) * Rand.num();
      if (rating > bestRating) { bestRating = rating; comp = t; }
    }
    if (comp == null) {
      utters(actor, "Uh... I don't know.", 0);
      return ;
    }
    final float
      levelA = actor.traits.relativeLevel(comp),
      levelO = other.traits.relativeLevel(comp),
      similarity = (1 - Math.abs(levelA - levelO));
    final String desc = actor.traits.levelDesc(comp);
    
    final float effect = similarity * Relation.MAG_CHATTING;
    other.memories.incRelation(actor, effect, 0.1f);
    actor.memories.incRelation(other, effect, 0.1f);
    
    utters(actor, "It's important to be "+desc+".", 0) ;
    if (similarity > 0.5f) utters(other, "Absolutely.", effect) ;
    else if (similarity < -0.5f) utters(other, "No way!", effect) ;
    else utters(other, "Yeah, I guess...", effect) ;
  }
  
  
  private static void gossip(Actor actor, Actor other) {
    //
    //  Pick an acquaintance, see if it's mutual, and if so compare attitudes
    //  on the subject.  TODO:  Include memories of recent activities?
    Relation pick = null;
    float bestRating = 0;
    for (Relation r : actor.memories.relations()) {
      if (r.subject == other || ! (r.subject instanceof Actor)) continue;
      final float
        otherR = other.memories.relationValue(r.subject),
        rating = (FastMath.abs(otherR * r.value()) + 0.5f) * Rand.num();
      if (rating > bestRating) { pick = r; bestRating = rating; }
    }
    
    if (pick == null) {
      smalltalk(actor, other);
      return;
    }
    final Actor about = (Actor) pick.subject;
    final float
      attA = actor.memories.relationValue(about),
      attO = other.memories.relationValue(about);
    

    final boolean agrees = FastMath.abs(attA - attO) < 0.5f;
    final float effect = 0.2f * (agrees ? 1 : -1) * Relation.MAG_CHATTING;
    other.memories.incRelation(actor, effect / 2, 0.1f);
    actor.memories.incRelation(other, effect / 2, 0.1f);
    other.memories.incRelation(about, effect * pick.value(), 0.1f);
    
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
    utters(other, "So, what do you do?", 0) ;
    
    Skill tested = null;
    float bestRating = 0;
    for (Skill s : other.traits.skillSet()) {
      final float
        levelA = actor.traits.useLevel(s),
        levelO = other.traits.useLevel(s),
        rating = levelA * levelO * Rand.num();
      if (levelA < 0 || levelO < 0) continue;
      if (rating > bestRating) { tested = s; bestRating = rating; }
    }
    
    if (tested == null) {
      utters(actor, "Oh, nothing you'd find interesting.", 0);
      return;
    }
    final float level = actor.traits.useLevel(tested);
    utters(actor, "Well, I'm interested in "+tested+".", 0);
    utters(actor, "Let me show you a few tricks...", 0);
    
    //  TODO:  Use the Counsel skill here.
    float effect = 0 ;
    if (other.traits.test(tested, level / 2, 0.5f)) effect += 5 ;
    else effect -= 5 ;
    if (other.traits.test(tested, level * Rand.num(), 0.5f)) effect += 5 ;
    else effect -= 5 ;
    effect *= Relation.MAG_CHATTING / 25f ;
    other.memories.incRelation(actor, effect, 0.1f) ;
    actor.memories.incRelation(other, effect, 0.1f) ;
    
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
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (! super.describedByStep(d)) d.append("Talking to ") ;
    d.append(other) ;
  }
  
  
  final static Vec3D forwardVec = new Vec3D(1, 1, 0) ;
  private static boolean onRight(Actor a, Actor b) {
    final Vec3D disp = a.position(null).sub(b.position(null)) ;
    return disp.dot(forwardVec) > 0 ;
  }
  
  
  private static void utters(Actor a, String s, float effect) {
    final String sign;
    if (effect == 0) sign = "";
    else if (effect > 0) sign = " (+)";
    else sign = " (-)";
    
    final Dialogue says = (Dialogue) a.matchFor(Dialogue.class) ;
    if (says == null) return ;
    boolean picked = false ;
    for (Dialogue d : says.sides()) {
      if (BaseUI.isPicked(d.actor())) picked = true ;
    }
    if (! picked) return ;
    final Actor opposite = a == says.actor ? says.other : says.actor ;
    final boolean onRight = onRight(a, opposite) ;
    final int side = onRight ? TalkFX.FROM_RIGHT : TalkFX.FROM_LEFT ;
    a.chat.addPhrase(s+sign, side) ;
  }
  
  
  public Transcript transcript() {
    return null ;
  }
}



/*

//  TODO:  You're adding way too much XP to Truth Sense this way!  It should
//  only count if the opponent is lying.  Otherwise, half value.

//  ...You test against half opponent's skill.  They test against half
//  yours.  Simple enough.  Whoever is better skilled gets more influence
//  on the conversation.

float success = 0;

//  Suasion vs. Suasion.  Half DC.  Bonus for truth sense, esp. vs deceit.
//  Plus a routine check for language.

//  Another modifier for attitude.

//  Counsel or command for instruction.  Level of skill taught for DC.
//  ...Maybe that should be a different activity, though?

//  Suasion for pleas.  Level of reluctance for DC.  Command for inferiors.



return success;
/*
final float attitude = other.memories.relationValue(actor) ;
final int DC = ROUTINE_DC - (int) (attitude * MODERATE_DC) ;
float success = -1 ;

success += actor.traits.test(language, null, null, ROUTINE_DC, 10, 2) ;
success += actor.traits.test(plea, other, opposeSkill, 0 - DC, 10, 2) ;
success /= 3 ;
return success ;
//*/

/*
float
  value    = actor.memories.relationValue(other),
  novelty  = actor.memories.relationNovelty(other),
  solitude = actor.mind.solitude() ;

novelty *= (actor.traits.relativeLevel(CURIOUS) + 1) / 2f ;
if (! actor.mind.hasRelation(other)) novelty *= solitude ;
else novelty *= (1 + solitude) / 2f ;

float impetus = (CASUAL * value * 2) + (novelty * ROUTINE) ;
if (other.base() == actor.base()) {
  impetus += actor.base().communitySpirit() ;
}
impetus *= 1 + actor.traits.relativeLevel(OUTGOING) ;

if (verbose && I.talkAbout == actor) {
  I.say("\n  Priority for talking to "+other+" is: "+impetus) ;
  I.say("  Value/novelty: "+value+"/"+novelty) ;
}
return Visit.clamp(impetus, 0, URGENT) ;
//*/


/**  Helper methods for gifts and favours-
  */
/*
private Delivery gettingGiftFor(Actor other) {
  
  //  TODO:  Find the list of items needed (but not produced?) by the actor's
  //  home or work venues and see if those can be acquired locally.  Default
  //  to money, food or bling otherwise.
  return null ;
}


private Action assistanceFrom(Actor other) {
  
  //  TODO:  Clone actions from the actor's own behavioural repertoire and
  //  see if any of those suit the other actor.
  
  //Plan favour = actor.mind.createBehaviour() ;
  //favour.assignActor(other) ;
  //favour.setMotive(
    //Plan.MOTIVE_LEISURE, ROUTINE * other.memories.relationValue(actor)
  //) ;
  //return favour ;
  return null ;
}
//*/


/*
//  TODO:  Base gifts on the item's demanded at the subject's home or work
//         venue, while defaulting to food, money, or bling.
//  TODO:  Create a new plan for gift-giving.




/*
Behaviour assembleGift(Actor actor) {
  float giftValue = this.rewardAmount(actor) / 2f ;
  //
  //  Check a few nearby storage depots- vault,
  final World world = base.world ;
  
  final Batch <Venue> depots = new Batch <Venue> () ;
  world.presences.sampleFromKeys(
    actor, world, 2, depots,
    SupplyDepot.class,
    StockExchange.class,
    VaultSystem.class
  ) ;
  
  Venue pickDepot = null ;
  Item pickItem = null ;
  float bestRating = 0, amount ;
  
  for (Venue depot : depots) for (Service type : GIFT_TYPES) {
    amount = depot.stocks.amountOf(type) ;
    amount = Math.min(amount, giftValue / type.basePrice) ;
    amount = Math.min(amount, 5) ;
    final Item item = Item.withAmount(type, amount) ;
    final float rating = amount * type.basePrice ;
    //
    //  Get the most valuable good of bulk less than 5 and under gift value.
    if (rating > bestRating) {
      bestRating = rating ;
      pickItem = item ;
      pickDepot = depot ;
    }
  }
  if (pickItem == null) return null ;
  
  final Delivery delivery = new Delivery(pickItem, pickDepot, actor) ;
  return delivery ;
}
//*/





/*
public boolean actionIntro(Actor actor, Actor other) {
  //  Used when making a first impression.
  float success = talkResult(SUASION, SUASION, TRUTH_SENSE, other) ;
  if (other.mind.hasRelation(actor)) {
    other.mind.initRelation(actor, success / 2) ;
  }
  else other.mind.incRelation(actor, success) ;
  stage = STAGE_CHAT ;
  return true ;
}
//*/




package stratos.game.civilian ;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.graphics.sfx.TalkFX;
import stratos.user.*;
import stratos.util.*;


/*
Etiquette (based on subject) + Comm Skill (based on dialogue event).
  Humans:  Tribal, Commoner or Noble speech, plus Counsel, Command or Suasion
  Animals:  appropriate Ecology plus Zoologist
  Artilects:  Ancient Lore plus Inscription

TODO:  Incorporate effects of community spirit for novel acquaintances.
Basic dialogue effects can also happen spontaneously.
//*/

//  ...Actors also need to use dialogue to 'object' when they see someone
//  doing something they don't approve of *to* someone else.


public class Dialogue extends Plan implements Qualities {
  
  
  /**  Constants, data fields, constructors and save/load methods-
    */
  final public static int
    TYPE_CONTACT   = 0,
    TYPE_CASUAL    = 1,
    TYPE_OBJECTION = 2 ;
  
  final static int
    STAGE_INIT  = -1,
    STAGE_GREET =  0,
    STAGE_CHAT  =  1,
    STAGE_BYE   =  2,
    STAGE_DONE  =  3 ;
  
  private static boolean verbose = false ;
  
  
  final Actor starts, other ;
  final int type ;
  private int stage = STAGE_INIT ;
  private Boardable location = null, stands = null ;
  
  
  
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
  
  public float priorityFor(Actor actor) {
    final boolean report = verbose && I.talkAbout == actor;
    
    final float novelty = actor.mind.relationNovelty(other);
    if (novelty <= 0) return 0;
    if (stage >= STAGE_DONE || ! canTalk(other)) return 0;
    
    final float priority = priorityForActorWith(
      actor, other, CASUAL,
      MILD_HELP, MILD_COMPETITION,
      BASE_SKILLS, BASE_TRAITS,
      NO_MODIFIER, HEAVY_DISTANCE_CHECK, NO_DANGER,
      report
    );
    return priority;
  }
  
  
  private boolean canTalk(Actor other) {
    //if (! other.health.conscious()) return false;
    final Target talksWith = other.focusFor(Dialogue.class);
    if (talksWith == actor) return true;
    if (talksWith != null) return false;
    if (starts != actor) return false ;
    final Dialogue d = new Dialogue(other, actor, actor, type) ;
    return ! other.mind.mustIgnore(d) ;
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
    
    //  If a location has not already been assigned, look for one either used
    //  by existing conversants, or find a new spot nearby.
    final Batch <Dialogue> sides = sides() ;
    if (location == null) {
      for (Dialogue d : sides) if (d.location != null) {
        this.location = d.location ; break ;
      }
      if (location == null) location = Spacing.nearestOpenTile(actor, actor) ;
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
        actor, other,
        this, "actionGreet",
        Action.TALK, "Greeting "
      ) ;
      greeting.setProperties(Action.RANGED) ;
      return greeting ;
    }
    
    if (stage == STAGE_CHAT) {
      Actor chatsWith = ((Dialogue) Rand.pickFrom(sides())).actor ;
      if (chatsWith == actor) chatsWith = other ;
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
  
  
  public boolean actionGreet(Actor actor, Actor other) {
    if (! other.isDoing(Dialogue.class, null)) {
      if (type != TYPE_CONTACT && ! canTalk(other)) {
        abortBehaviour() ;
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
    final float novelty = actor.mind.relationNovelty(other) ;
    if (novelty <= 0) {
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
    return true ;
  }
  
  
  
  /**  Helper methods for elaborating on chat options-
    */
  private static float talkResult(
    Skill language, Skill plea, Skill opposeSkill, Actor actor, Actor other
  ) {
    final float attitude = other.mind.relationValue(actor) ;
    final int DC = ROUTINE_DC - (int) (attitude * MODERATE_DC) ;
    float success = -1 ;
    success += actor.traits.test(language, null, null, ROUTINE_DC, 10, 2) ;
    success += actor.traits.test(plea, other, opposeSkill, 0 - DC, 10, 2) ;
    success /= 3 ;
    return success ;
  }
  
  
  public static float tryChat(Actor actor, Actor other) {
    //  Base on comparison of recent activities and associated traits, skills
    //  or actors involved.
    
    float success = talkResult(SUASION, SUASION, TRUTH_SENSE, actor, other) ;
    other.mind.incRelation(actor, success / 10) ;
    
    switch (Rand.index(3)) {
      case (0) : anecdote(actor, other) ; break ;
      case (1) : gossip  (actor, other) ; break ;
      case (2) : advise  (actor, other) ; break ;
    }
    
    return success ;
  }
  
  
  private static void anecdote(Actor actor, Actor other) {
    //
    //  Pick a random recent activity and see if the other also indulged in it.
    //  If the activity is similar, or was undertaken for similar reasons,
    //  improve relations.
    //  TODO:  At the moment, we just compare traits.  Fix later.
    final Trait comp = (Trait) Rand.pickFrom(actor.traits.personality()) ;
    if (comp == null) {
      return ;
    }
    final float
      levelA = 1 + actor.traits.relativeLevel(comp),
      levelO = 1 + actor.traits.relativeLevel(comp),
      effect = 0.5f - (Math.abs(levelA - levelO) / 1.5f) ;
    final String desc = actor.traits.levelDesc(comp) ;
    
    other.mind.incRelation(actor, effect) ;
    actor.mind.incRelation(other, effect) ;
    
    utters(actor, "It's important to be "+desc+".") ;
    if (effect > 0) utters(other, "Absolutely.") ;
    if (effect == 0) utters(other, "Yeah, I guess...") ;
    if (effect < 0) utters(other, "No way!") ;
  }
  
  
  private static void gossip(Actor actor, Actor other) {
    //
    //  Pick an acquaintance, see if it's mutual, and if so compare attitudes
    //  on the subject.  TODO:  Include memories of recent activities?
    final Relation r = (Relation) Rand.pickFrom(actor.mind.relations()) ;
    if (r == null || r.subject == other) {
      utters(actor, "Nice weather, huh?") ;
      utters(other, "Uh-huh.") ;
      return ;
    }
    final float attA = r.value(), attO = other.mind.relationValue(actor) ;
    
    if (attA > 0) utters(actor, "I get on well with "+r.subject+".") ;
    else utters(actor, "I don't get on with "+r.subject+".") ;
    final boolean agrees = attO * attA > 0 ;
    if (agrees) utters(other, "I can see that.") ;
    else utters(other, "Really?") ;
    
    final float effect = 0.2f * (agrees ? 1 : -1) ;
    other.mind.incRelation(actor, effect / 2) ;
    actor.mind.incRelation(other, effect / 2) ;
    other.mind.incRelation(r.subject, effect * r.value()) ;
  }
  
  
  private static void advise(Actor actor, Actor other) {
    final Skill tested = (Skill) Rand.pickFrom(other.traits.skillSet()) ;
    if (tested == null) return ;
    final float level = actor.traits.useLevel(tested) ;
    
    utters(other, "I'm interested in "+tested.name+".") ;
    if (level < other.traits.useLevel(tested)) {
      utters(actor, "Don't know much about that.") ;
      return ;
    }
    utters(actor, "Well, here's what you do...") ;
    utters(other, "You mean like this?") ;
    
    //  Use the Counsel skill here.
    
    float effect = 0 ;
    if (other.traits.test(tested, level / 2, 0.5f)) effect += 5 ;
    else effect -= 5 ;
    if (other.traits.test(tested, level * Rand.num(), 0.5f)) effect += 5 ;
    else effect -= 5 ;
    effect /= 25 ;
    other.mind.incRelation(actor, effect) ;
    actor.mind.incRelation(other, effect) ;
    
    if (effect > 0) utters(actor, "Yes, exactly!") ;
    if (effect == 0) utters(actor, "Close. Try again.") ;
    if (effect < 0) utters(actor, "No, that's not it...") ;
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
  
  
  private static void utters(Actor a, String s) {
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
    a.chat.addPhrase(s, side) ;
  }
  
  
  public Transcript transcript() {
    return null ;
  }
}



/*
float
  value    = actor.mind.relationValue(other),
  novelty  = actor.mind.relationNovelty(other),
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
    //Plan.MOTIVE_LEISURE, ROUTINE * other.mind.relationValue(actor)
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

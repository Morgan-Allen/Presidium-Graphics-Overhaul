


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.util.*;



//  TODO:  Actors also need to use dialogue to 'object' when they see someone
//  doing something they don't approve of *to* someone else.

//  TODO:  You need to restore the use of a communal ChatFX for a given
//  instance of dialogue.  (And have everything fade once complete.)

//  TODO:  You need to use separate actions for invites/gifting.  Get rid of
//  urgency-based quits for now, and just allow for 3 exchanges at a time, say.



//  TODO:  Try and improve on the mechanics here in general.  Restore sensible
//  location-setting, and allow for multiple participants.


public class Dialogue extends Plan implements Qualities {
  
  
  /**  Constants, data fields, constructors and save/load methods-
    */
  private static boolean
    evalVerbose   = false,
    eventsVerbose = false;
  
  final public static int
    TYPE_CONTACT = 0,
    TYPE_CASUAL  = 1,
    TYPE_PLEA    = 2;
  
  final public static float
    RELATION_BOOST = 0.5f,
    BORED_DURATION = Stage.STANDARD_HOUR_LENGTH * 1;
  
  final static int
    STAGE_INIT   = -1,
    STAGE_PLEAD  =  0,
    STAGE_INTRO  =  1,
    STAGE_GREET  =  2,
    STAGE_OFFER  =  3,
    STAGE_CHAT   =  4,
    STAGE_INVITE =  5,
    STAGE_BYE    =  6,
    STAGE_DONE   =  7;
  
  
  final Actor starts, other;
  final int type;
  
  private int stage = STAGE_INIT;
  private Boarding location = null, stands = null;
  
  private Item gift;
  private Behaviour invitation;
  
  
  
  public Dialogue(Actor actor, Actor other) {
    this(actor, other, actor, TYPE_CASUAL);
  }
  
  
  public Dialogue(Actor actor, Actor other, int type) {
    this(actor, other, actor, type);
  }
  
  
  private Dialogue(Actor actor, Actor other, Actor starts, int type) {
    super(actor, other, false, MILD_HELP);
    if (actor == other) I.complain("CANNOT TALK TO SELF!");
    this.other = other;
    this.starts = starts;
    this.type = type;
  }
  
  
  public Dialogue(Session s) throws Exception {
    super(s);
    other  = (Actor) s.loadObject();
    starts = (Actor) s.loadObject();
    type = s.loadInt();
    stage = s.loadInt();
    
    location = (Boarding) s.loadTarget();
    stands = (Boarding) s.loadTarget();
    gift = Item.loadFrom(s);
    invitation = (Plan) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(other );
    s.saveObject(starts);
    s.saveInt(type);
    s.saveInt(stage);
    
    s.saveTarget(location);
    s.saveTarget(stands);
    Item.saveTo(s, gift);
    s.saveObject(invitation);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Dialogue(other, this.other, other, type);
  }
  
  
  public void attachGift(Item gift) {
    if (! actor.gear.hasItem(gift)) I.complain("Actor does not have "+gift);
    this.gift = gift;
  }
  
  
  public void attachInvitation(Plan invite) {
    if (invite.actor() != actor) I.complain("Favour must apply to actor!");
    this.invitation = invite;
  }
  
  
  
  /**  Target selection and priority evaluation-
    */
  final static Trait BASE_TRAITS[] = { OUTGOING, POSITIVE, EMPATHIC };
  final static Skill BASE_SKILLS[] = { SUASION, TRUTH_SENSE };
  
  
  protected float getPriority() {
    if (GameSettings.noChat) return -1;
    
    final boolean report = evalVerbose && (
      I.talkAbout == actor || I.talkAbout == other
    );

    if (stage == STAGE_BYE ) return CASUAL;
    if (stage == STAGE_DONE || shouldQuit()) {
      if (report) I.say("\nDialogue should quit!");
      return 0;
    }
    
    float maxRange = actor.health.sightRange() * 2;
    final float
      curiosity = (1 + actor.traits.relativeLevel(CURIOUS)) / 2f,
      solitude  = solitude(actor),
      novelty   = actor.relations.noveltyFor(other);
    float bonus = 0;
    if (type == TYPE_CASUAL) {
      bonus = solitude + (curiosity * novelty);
    }
    if (type == TYPE_PLEA) {
      bonus = novelty;
      bonus += CombatUtils.isActiveHostile(actor, other) ? 0 : 1;
    }
    if (type == TYPE_CONTACT) {
      bonus += 1;
    }
    else if (Spacing.distance(actor, other) > maxRange) {
      return 0;
    }
    if (report) {
      I.say("\nChecking for dialogue between "+actor+" and "+other);
      I.say("  Type is:           "+type);
      I.say("  Solitude:          "+solitude);
      I.say("  Curiosity/novelty: "+curiosity+"/"+novelty);
      I.say("  Bonus:             "+bonus);
    }
    
    final float priority = priorityForActorWith(
      actor, other,
      CASUAL, bonus * ROUTINE,
      MILD_HELP, NO_COMPETITION, NO_FAIL_RISK,
      BASE_SKILLS, BASE_TRAITS, HEAVY_DISTANCE_CHECK,
      report
    );
    return priority;
  }
  
  
  private float solitude(Actor actor) {
    //  TODO:  Only count positive relations!
    final float
      trait = (1 + actor.traits.relativeLevel(OUTGOING)) / 2f,
      baseF = ActorRelations.BASE_NUM_FRIENDS * (trait + 0.5f),
      numF  = actor.relations.relations().size();
    return (baseF - numF) / baseF;
  }
  
  
  private boolean shouldQuit() {
    if (invitation != null) return false;
    return
      (actor.relations.noveltyFor(other) <= 0) ||
      (type != TYPE_CONTACT && ! canTalk(other));
  }
  
  
  private boolean canTalk(Actor other) {
    if (! other.health.conscious()) return false;
    if (! other.health.human()) return false;
    
    if (other == starts && ! hasBegun()) return true;
    
    final Target talksWith = other.planFocus(Dialogue.class);
    if (talksWith == actor) return true;
    if (talksWith != null) return false;
    
    final Dialogue d = new Dialogue(other, actor, actor, type);
    final boolean can = ! other.mind.mustIgnore(d);
    
    final boolean report = evalVerbose && (
      I.talkAbout == actor || I.talkAbout == other
    );
    if (report) {
      I.say("\n  "+actor+" checking if "+other+" can talk? "+can);
      I.say("  Chat priority: "+d.priorityFor(other));
    }
    return can;
  }
  
  
  private void setLocationFor(Action talkAction) {
    final boolean report = eventsVerbose && I.talkAbout == actor;
    talkAction.setMoveTarget(other);
    location = other.origin();
    /*
    if (location == null) {
      location = starts.aboard();
    }
    talkAction.setMoveTarget(location);
    //*/
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    if (stage >= STAGE_DONE) return null;
    final boolean report = eventsVerbose && I.talkAbout == actor;
    
    if (starts == actor && stage == STAGE_INIT) {
      final Action greeting = new Action(
        actor, other.aboard(),
        this, "actionGreet",
        Action.TALK, "Greeting "
      );
      greeting.setProperties(Action.RANGED);
      return greeting;
    }
    
    if (stage == STAGE_CHAT) {
      final Action waits = new Action(
        actor, other,
        this, "actionWait",
        Action.TALK_LONG, "Waiting for "
      );
      setLocationFor(waits);
      if (location == null || Spacing.distance(other, location) > 1) {
        waits.setProperties(Action.NO_LOOP);
        return waits;
      }
      
      if (gift != null) {
        final Action offer = new Action(
          actor, other,
          this, "actionOfferGift",
          Action.TALK_LONG, "Offering "+gift.type+" to "
        );
        return offer;
      }
      
      if (invitation != null) {
        final Action invite = new Action(
          actor, other,
          this, "actionInvite",
          Action.TALK_LONG, "Inviting"
        );
        return invite;
      }
      
      final Action chats = new Action(
        actor, other,
        this, "actionChats",
        Action.TALK_LONG, "Chatting with "
      );
      setLocationFor(chats);
      return chats;
    }
    
    if (stage == STAGE_BYE) {
      final Action farewell = new Action(
        actor, other,
        this, "actionFarewell",
        Action.TALK, "Saying farewell to "
      );
      farewell.setProperties(Action.RANGED);
      return farewell;
    }
    
    return null;
  }
  
  
  public boolean actionGreet(Actor actor, Boarding aboard) {
    if (! other.isDoing(Dialogue.class, null)) {
      if (canTalk(other)) {
        final Dialogue d = new Dialogue(other, actor, actor, type);
        d.stage = STAGE_CHAT;
        other.mind.assignBehaviour(d);
      }
      else if (shouldQuit()) {
        abortBehaviour();
        return false;
      }
    }
    this.stage = STAGE_CHAT;
    return true;
  }
  
  
  public boolean actionChats(Actor actor, Actor other) {
    DialogueUtils.tryChat(actor, other);
    //final boolean canTalk = canTalk(other);
    final float relation = actor.relations.valueFor(other);
    
    if (shouldQuit()) {
      if (invitation == null && Rand.num() < relation) {
        invitation = actor.mind.nextBehaviour();
      }
      else stage = STAGE_BYE;
    }
    
    return true;
  }
  
  
  public boolean actionWait(Actor actor, Actor other) {
    return true;
  }
  
  
  public boolean actionFarewell(Actor actor, Actor other) {
    //  Used to close a dialogue.
    stage = STAGE_DONE;
    return true;
  }
  
  
  
  /**  Gift-giving behaviours-
    */
  public boolean actionOfferGift(Actor actor, Actor receives) {
    final boolean report = eventsVerbose && I.talkAbout == actor;

    //  Regardless of the outcome, this won't be offered twice.
    final Item gift = this.gift;
    this.gift = null;
    
    //  TODO:  Modify DC by the greed and honour of the subject.
    DialogueUtils.utters(actor, "I have a gift for you...", 0);
    final float value = ActorMotives.rateDesire(gift, null, receives) / 10f;
    float acceptDC = (0 - value) * ROUTINE_DC;
    float success = DialogueUtils.talkResult(
      SUASION, acceptDC, actor, receives
    );

    if (report) {
      I.say("\nOffering "+gift+" to "+receives+", DC: "+acceptDC);
      I.say("  Value: "+value+", success: "+success);
    }
    if (success == 0) {
      if (report) I.say("  Offer rejected!");
      stage = STAGE_BYE;
      return false;
    }
    if (report) {
      I.say("  Offer accepted!");
      I.say("  Relation before: "+receives.relations.valueFor(actor));
    }

    actor.gear.transfer(gift, receives);
    receives.relations.incRelation(actor, 1, value / 2, 0);
    actor.relations.incRelation(receives, 1, value / 4, 0);
    DialogueUtils.utters(receives, "Thank you for the "+gift.type+"!", value);
    
    if (report) {
      I.say("  Relation after: "+receives.relations.valueFor(actor));
    }
    return true;
  }
  
  
  public boolean actionInvite(Actor actor, Actor asked) {
    final boolean report = eventsVerbose && I.talkAbout == actor;
    this.stage = STAGE_BYE;
    
    if (! Joining.checkInvitation(actor, asked, this, invitation)) {
      if (report) I.say("Invitation rejected!- "+invitation);
      return false;
    }
    
    final Plan basis = (Plan) invitation;
    final Plan copy = basis.copyFor(asked);
    actor.mind.assignBehaviour(new Joining(actor, basis, asked));
    asked.mind.assignBehaviour(new Joining(asked, copy, actor));
    
    if (report) {
      I.say("  Assigning behaviour: "+basis+" to "+actor);
      I.say("  Assigning behaviour: "+copy+" to "+asked);
    }
    this.stage = STAGE_DONE;
    return true;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (super.lastStepIs("actionInvite")) {
      d.append("Inviting ");
      d.append(other);
      d.append(" to go ");
      d.append(invitation);
      return;
    }
    if (super.needsSuffix(d, "Talking to ")) {
      d.append(other);
    }
  }
}




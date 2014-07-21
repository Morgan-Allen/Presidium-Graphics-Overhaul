


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.util.*;



//  TODO:  Actors also need to use dialogue to 'object' when they see someone
//  doing something they don't approve of *to* someone else.

//  TODO:  You need to restore the use of a communal ChatFX for a given
//  instance of dialogue.  (And have everything fade once complete.)

//  TODO:  You need to use separate actions for invites/gifting.  Get rid of
//  urgency-based quits for now, and just allow for 3 exchanges at a time, say.


public class Dialogue extends Plan implements Qualities {
  
  
  /**  Constants, data fields, constructors and save/load methods-
    */
  final public static int
    TYPE_CONTACT = 0,
    TYPE_CASUAL  = 1,
    TYPE_PLEA    = 2;
  
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
  
  private static boolean
    evalVerbose   = false,
    eventsVerbose = true;
  
  
  final Actor starts, other;
  final int type;
  
  private int stage = STAGE_INIT;
  private Boarding location = null, stands = null;
  
  private Item gift;
  private Behaviour invitation;
  
  
  
  public Dialogue(Actor actor, Actor other, int type) {
    this(actor, other, actor, type);
  }
  
  
  private Dialogue(Actor actor, Actor other, Actor starts, int type) {
    super(actor, other);
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
    return new Dialogue(other, this.other, type);
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
    final boolean report = evalVerbose && I.talkAbout == actor;
    
    if (stage == STAGE_DONE) return 0;
    if (stage == STAGE_BYE) return CASUAL;  //Move down?
    
    float urgency = 0;
    float distCheck = NORMAL_DISTANCE_CHECK;
    
    final boolean casual = type == TYPE_CASUAL;
    if (casual) {
      if (! canTalk(other)) return 0;
      urgency = urgency();
      if (urgency <= 0) return 0;
      urgency = Visit.clamp(urgency, 0.5f, 1);
      distCheck = HEAVY_DISTANCE_CHECK;
    }
    
    final float priority = priorityForActorWith(
      actor, other, casual ? (ROUTINE * urgency) : URGENT,
      MILD_HELP, NO_COMPETITION,
      BASE_SKILLS, BASE_TRAITS,
      NO_MODIFIER, distCheck, NO_FAIL_RISK,
      report
    );
    if (report) {
      I.say("  Urgency of dialogue was: "+urgency);
      ///I.say("  Prior relationship? "+(r != null)+", curiosity: "+curiosity);
    }
    return priority;
  }
  
  
  private float urgency() {
    final float curiosity = (1 + actor.traits.relativeLevel(CURIOUS)) / 2f;
    final Relation r = actor.relations.relationWith(other);
    final float solitude = solitude(actor);
    
    float urgency = 0;
    if (r == null) {
      urgency += (solitude + actor.relations.relationValue(other)) / 2f;
      urgency += (1 + curiosity) * solitude;
    }
    else {
      urgency += (solitude + r.value()) / 2f;
      urgency += (1 + curiosity) * r.novelty();
    }
    return Visit.clamp(urgency, -1, 1);
  }
  
  
  private float solitude(Actor actor) {
    //  TODO:  Only count positive relations!
    final float
      trait = (1 + actor.traits.relativeLevel(OUTGOING)) / 2f,
      baseF = Relation.BASE_NUM_FRIENDS * (trait + 0.5f),
      numF  = actor.relations.relations().size();
    return (baseF - numF) / baseF;
  }
  
  
  private boolean canTalk(Actor other) {
    if (! other.health.conscious()) return false;
    if (! other.health.human()) return false;
    if (other == starts && ! hasBegun()) return true;
    final Target talksWith = other.focusFor(Dialogue.class);
    if (talksWith == actor) return true;
    if (talksWith != null) return false;
    
    final Dialogue d = new Dialogue(other, actor, actor, type);
    final boolean can = ! other.mind.mustIgnore(d);
    
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (report) {
      I.say("  "+other+" can talk? "+can);
      I.say("  Talk priority: "+d.priorityFor(other));
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
    
    if (type != TYPE_CONTACT && ! canTalk(other)) {
      abortBehaviour();
      return null;
    }
    
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
      if (! canTalk(other)) {
        if (I.talkAbout == actor) I.say("CAN'T TALK!");
        abortBehaviour();
        return false;
      }
      final Dialogue d = new Dialogue(other, actor, type);
      d.stage = STAGE_CHAT;
      other.mind.assignBehaviour(d);
    }
    this.stage = STAGE_CHAT;
    return true;
  }
  
  
  public boolean actionWait(Actor actor, Actor other) {
    return true;
  }
  
  
  public boolean actionChats(Actor actor, Actor other) {
    DialogueUtils.tryChat(actor, other);
    final boolean canTalk = canTalk(other);

    final float relation = actor.relations.relationValue(other);
    //I.say("Urgency: "+urgency()+", relation: "+relation);
    //I.say("Novelty: "+actor.memories.relationNovelty(other));
    if (urgency() <= 0 || ! canTalk) {
      if (invitation == null && Rand.num() < relation) {
        invitation = actor.mind.nextBehaviour();
      }
      else stage = STAGE_BYE;
    }
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
    final float value = Gifting.rateGift(gift, null, receives) / 10f;
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
      I.say("  Relation before: "+receives.relations.relationValue(actor));
    }

    actor.gear.transfer(gift, receives);
    receives.relations.incRelation(actor, 1, value / 2);
    actor.relations.incRelation(receives, 1, value / 4);
    DialogueUtils.utters(receives, "Thank you for the "+gift.type+"!", value);
    
    if (report) {
      I.say("  Relation after: "+receives.relations.relationValue(actor));
    }
    return true;
  }
  
  
  public boolean actionInvite(Actor actor, Actor asked) {
    final boolean report = eventsVerbose && I.talkAbout == actor;
    this.stage = STAGE_BYE;
    
    if (! (invitation instanceof Plan)) return false;
    if (actor.mind.hasToDo(Joining.class)) return false;
    final Plan basis = (Plan) invitation;
    if (basis.hasMotiveType(Plan.MOTIVE_DUTY)) return false;
    
    final Plan copy = basis.copyFor(asked);
    if (copy == null) {
      I.say("Warning: no copy of "+basis+" for "+asked);
      return false;
    }
    final float motiveBonus = DialogueUtils.talkResult(
      SUASION, ROUTINE_DC, actor, asked
    ) * CASUAL;
    basis.setMotiveFrom(this, 0);
    copy.setMotive(Plan.MOTIVE_LEISURE, motiveBonus);
    if (report) I.say("\nExtending invitation: "+basis+" to "+asked);
    
    if (! Choice.couldJoinActivity(asked, actor, basis, copy)) {
      if (report) I.say("  ...Invitation rejected.");
      return false;
    }
    
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




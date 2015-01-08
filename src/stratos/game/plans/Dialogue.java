


package stratos.game.plans;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.util.*;


//  TODO:  You need to use separate actions for invites/gifting.  Get rid of
//  urgency-based quits for now, and just allow for 3 exchanges at a time, say.

//  TODO:  Try and improve on the mechanics here in general.  Restore sensible
//  location-setting, and allow for multiple participants.


public class Dialogue extends Plan implements Qualities {
  
  
  /**  Constants, data fields, constructors and save/load methods-
    */
  private static boolean
    evalVerbose  = false,
    stepsVerbose = false;
  
  final public static int
    TYPE_CONTACT = 0,
    TYPE_CASUAL  = 1,
    TYPE_PLEA    = 2;
  
  //  TODO:  SEE IF YOU CAN USE THESE?..
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
  
  
  final Actor other;
  final int type;
  final Dialogue starts;
  final int depth;  //  Purely a safety measure against infinite-recursion.
  
  private int stage = STAGE_INIT;
  private Boarding location = null, stands = null;
  
  private Item gift;
  private Behaviour invitation;
  
  
  
  public Dialogue(Actor actor, Actor other) {
    this(actor, other, null, TYPE_CASUAL);
  }
  
  
  public Dialogue(Actor actor, Actor other, int type) {
    this(actor, other, null, type);
  }
  
  
  private Dialogue(Actor actor, Actor other, Dialogue starts, int type) {
    super(actor, other, false, MILD_HELP);
    this.other  = other;
    this.starts = starts == null ? this : starts;
    this.depth  = starts == null ? 0 : (1 + starts.depth);
    this.type   = type;
  }
  
  
  public Dialogue(Session s) throws Exception {
    super(s);
    other  = (Actor) s.loadObject();
    starts = (Dialogue) s.loadObject();
    depth  = s.loadInt();
    type   = s.loadInt();
    stage  = s.loadInt();
    
    location   = (Boarding) s.loadTarget();
    stands     = (Boarding) s.loadTarget();
    gift       = Item.loadFrom(s);
    invitation = (Plan) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(other );
    s.saveObject(starts);
    s.saveInt   (depth );
    s.saveInt   (type  );
    s.saveInt   (stage );
    
    s.saveTarget(location);
    s.saveTarget(stands  );
    Item.saveTo(s, gift);
    s.saveObject(invitation);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Dialogue(other, this.other, null, type);
  }
  
  
  public Boarding location() {
    if (location == null) {
      if (starts == this) location = other.origin();
      else location = starts.location();
    }
    return location;
  }
  
  
  public void attachGift(Item gift) {
    if (! actor.gear.hasItem(gift)) I.complain("Actor does not have "+gift);
    this.gift = gift;
  }
  
  
  public void attachInvitation(Plan invite) {
    if (invite.actor() != actor) I.complain("Favour must apply to actor!");
    this.invitation = invite;
  }
  
  
  /**  Utility methods for assessing possibility-
    */
  private boolean canTalk(Actor other) {
    final boolean report = evalVerbose && (
      I.talkAbout == actor || I.talkAbout == other
    );
    if (depth > 3) {
      I.say("\nWARNING: DIALOGUE COULD ENTER INFINITE LOOP:"+this);
      I.reportStackTrace();
      return false;
    }
    if (starts == null || starts.actor() == null) {
      I.complain("No conversation starter!");
      return false;
    }
    
    final Target chatsWith = other.planFocus(Dialogue.class, true);
    if (chatsWith != null && chatsWith != actor) return false;
    if (stage > STAGE_GREET) return chatsWith == actor;
    
    if (report) {
      I.say("\nChecking if "+other+" will talk to "+actor);
      I.say("  Starts:  "+I.tagHash(starts));
      I.say("  This is: "+I.tagHash(this  ));
    }
    
    if (this != starts && other == starts.actor()) return true;
    final Dialogue sample = starts.childDialogue(actor);
    if (! other.mind.mustIgnore(sample)) return true;
    return false;
  }
  
  
  private Dialogue childDialogue(Actor other) {
    final boolean report = evalVerbose && (
      I.talkAbout == actor || I.talkAbout == other
    );
    final Dialogue d = new Dialogue(other, actor, starts, type);
    
    if (report) {
      I.say("\nHAVE CREATED CHILD DIALOGUE: "+d);
      I.say("  Starts: "+d.starts);
    }
    
    d.stage = STAGE_CHAT;
    d.setMotiveFrom(this, 0 - motiveBonus() / 2f);
    return d;
  }
  
  
  
  
  /**  Target selection and priority evaluation-
    */
  final static Trait BASE_TRAITS[] = { OUTGOING, POSITIVE, EMPATHIC };
  final static Skill BASE_SKILLS[] = { SUASION, TRUTH_SENSE };
  
  
  protected float getPriority() {
    final boolean report = evalVerbose && I.talkAbout == actor;// && hasBegun();
    if (GameSettings.noChat) return -1;
    if (! other.health.conscious()) return -1;
    if (! other.health.human    ()) return -1;
    if (stage == STAGE_DONE) return -1;
    
    if (stage == STAGE_BYE) return CASUAL;
    if (type == TYPE_CASUAL && ! canTalk(other)) {
      if (report) I.say("\n"+other+" can't talk now- skipping!");
      return -1;
    }
    
    final float
      maxRange    = actor.health.sightRange() * 2,
      solitude    = actor.motives.solitude(),
      curiosity   = (1 + actor.traits.relativeLevel(CURIOUS)) / 2f,
      novelty     = actor.relations.noveltyFor(other),
      talkThresh  = hasBegun() ? 0 : (1 - solitude) / 2;
    final boolean
      freshFace   = ! actor.relations.hasRelation(other);
    
    if (report) {
      I.say("\nChecking for dialogue between "+actor+" and "+other);
      I.say("  Type is:      "+type       );
      I.say("  Stage:        "+stage      );
      I.say("  Solitude:     "+solitude   );
      I.say("  Curiosity:    "+curiosity  );
      I.say("  Novelty:      "+novelty    );
      I.say("  Base novelty: "+actor.relations.noveltyFor(other.base()));
      I.say("  Threshold:    "+talkThresh );
      I.say("  Stage/begun:  "+stage+"/"+hasBegun());
    }
    
    //  Strangers from entirely different bases might have novelty greater than
    //  1, which makes them worth investigating.  Otherwise, solitude is the
    //  dominant variable:
    float bonus = solitude + Nums.clamp(curiosity * novelty, 0, 1);
    if (freshFace) bonus *= solitude;
    if (novelty >= 1) bonus += (novelty - 1) * curiosity * 2;
    
    if (type == TYPE_CONTACT) bonus += 0.5f;
    else if (Spacing.distance(actor, other) > maxRange) return -1;
    
    if (report) {
      I.say("  Final bonus:  "+bonus      );
    }
    if (bonus <= talkThresh || novelty <= talkThresh) {
      if (report) I.say("\n  Nothing to talk about- skipping!");
      return -1;
    }
    final float priority = priorityForActorWith(
      actor, other,
      CASUAL, bonus * ROUTINE,
      MILD_HELP, NO_COMPETITION, NO_FAIL_RISK,
      BASE_SKILLS, BASE_TRAITS, HEAVY_DISTANCE_CHECK,
      report
    );
    return Nums.clamp(priority, 0, URGENT);
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    if (stage >= STAGE_DONE) return null;
    final boolean report = stepsVerbose && I.talkAbout == actor;
    if (report) {
      I.say("\nGetting next dialogue step for "+actor);
      I.say("  Plan is: "+I.tagHash(this));
    }
    
    if (starts == this && stage == STAGE_INIT) {
      if (report) I.say("  Greeting "+other);
      
      final Action greeting = new Action(
        actor, other.aboard(),
        this, "actionGreet",
        Action.TALK, "Greeting "
      );
      greeting.setProperties(Action.RANGED);
      return greeting;
    }
    
    if (stage == STAGE_CHAT) {
      if (Spacing.distance(other, actor) > 1) {
        if (report) I.say("  Waiting for "+other);
        final Action waits = new Action(
          actor, other,
          this, "actionWait",
          Action.TALK_LONG, "Waiting for "
        );
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

      if (report) I.say("  Chatting with "+other);
      final Action chats = new Action(
        actor, other,
        this, "actionChats",
        Action.TALK_LONG, "Chatting with "
      );
      return chats;
    }
    
    if (stage == STAGE_BYE) {
      if (report) I.say("  Bidding farewell to "+other);
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
    final boolean report = stepsVerbose && I.talkAbout == actor;
    final Target otherChats = other.planFocus(Dialogue.class, true);
    if (report) I.say("\nOther is chatting with: "+otherChats);
    
    if (otherChats != actor) {
      if (canTalk(other)) {
        if (report) I.say("  Assigning fresh dialogue to "+other);
        other.mind.assignBehaviour(childDialogue(other));
      }
      else return false;
    }
    
    if (report) I.say("  Okay to start chatting...");
    this.stage = STAGE_CHAT;
    return true;
  }
  
  
  public boolean actionWait(Actor actor, Actor other) {
    return true;
  }
  
  
  public boolean actionChats(Actor actor, Actor other) {
    DialogueUtils.tryChat(actor, other);
    if (! canTalk(other)) stage = STAGE_BYE;
    return true;
  }
  
  
  public boolean actionFarewell(Actor actor, Actor other) {
    stage = STAGE_DONE;
    return true;
  }
  
  
  
  /**  Gift-giving behaviours-
    */
  public boolean actionOfferGift(Actor actor, Actor receives) {
    final boolean report = stepsVerbose && I.talkAbout == actor;

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
    final boolean report = stepsVerbose && I.talkAbout == actor;
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




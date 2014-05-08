


package stratos.game.civilian ;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.common.*;
import stratos.util.*;



//  ...Actors also need to use dialogue to 'object' when they see someone
//  doing something they don't approve of *to* someone else.  (Or just build
//  that into basic decision-making?)

//  TODO:  You need to restore the use of a communal ChatFX for a given
//  instance of dialogue.  (And have everything fade once complete.)


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
  
  private Item gift;
  private Behaviour favour;
  
  
  
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
    gift = Item.loadFrom(s);
    favour = (Plan) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(other ) ;
    s.saveObject(starts) ;
    s.saveInt(type) ;
    s.saveInt(stage) ;
    
    s.saveTarget(location) ;
    s.saveTarget(stands) ;
    Item.saveTo(s, gift);
    s.saveObject(favour);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Dialogue(other, this.other, type);
  }
  
  
  public void attachGift(Item gift) {
    if (! actor.gear.hasItem(gift)) I.complain("Actor does not have "+gift);
    this.gift = gift;
  }
  
  
  public void attachfavour(Plan favour) {
    if (favour.actor() != other) I.complain("Favour must apply to partner!");
    this.favour = favour;
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
  
  
  //  TODO:  Try moving some of these methods out to the DialogueUtils class.
  
  
  private float urgency() {
    final float curiosity = (1 + actor.traits.relativeLevel(CURIOUS)) / 2;
    final Relation r = actor.memories.relationWith(other);
    final float
      value   = r == null ? actor.memories.relationValue(other) : r.value(),
      novelty = r == null ? 1 : r.novelty();
    
    float urgency = 0;
    urgency += novelty * curiosity;
    urgency += (solitude(actor) + value) / 2f;
    urgency = Visit.clamp(urgency, -1, 1);
    return urgency;
  }
  
  
  private float solitude(Actor actor) {
    //  TODO:  Only count positive relations?
    final float
      baseF = Relation.BASE_NUM_FRIENDS,
      numF  = actor.memories.relations().size();
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
    final boolean report = eventsVerbose && I.talkAbout == actor;
    if (location == null) {
      location = starts.aboard();
    }
    talkAction.setMoveTarget(location);
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    if (stage >= STAGE_DONE) return null ;
    final boolean report = eventsVerbose && I.talkAbout == actor;
    
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
      /*
      if (type == TYPE_CASUAL) {
        chatsWith = ((Dialogue) Rand.pickFrom(sides())).actor;
        if (chatsWith == actor) chatsWith = other;
      }
      //*/
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
  
  
  public boolean actionWait(Actor actor, Actor other) {
    return true;
  }
  
  
  public boolean actionChats(Actor actor, Actor other) {
    DialogueUtils.tryChat(actor, other);
    
    if (gift != null) {
      actionOfferGift(actor, other);
      gift = null;
      return true;
    }
    
    if (urgency() <= 0) {
      if (actionAskFavour(actor, other)) stage = STAGE_DONE;
      stage = STAGE_BYE;
    }
    return true;
  }
  
  
  public boolean actionFarewell(Actor actor, Actor other) {
    //  Used to close a dialogue.
    stage = STAGE_DONE ;
    return true ;
  }
  
  
  
  /**  Gift-giving behaviours-
    */
  public boolean actionOfferGift(Actor actor, Actor receives) {
    final boolean report = eventsVerbose && I.talkAbout == actor;
    
    final Action doing = receives.currentAction();
    if (doing == null || doing.methodName().equals("actionReceives")) {
      if (report) I.say("  Other is distracted.");
      return false;
    }
    
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
      I.say("  Relation before: "+receives.memories.relationValue(actor));
    }

    actor.gear.transfer(gift, receives);
    receives.memories.incRelation(actor, 1, value / 2);
    actor.memories.incRelation(receives, 1, value / 4);
    DialogueUtils.utters(receives, "Thank you for the "+gift.type+"!", value);
    
    if (report) {
      I.say("  Relation after: "+receives.memories.relationValue(actor));
    }
    return true;
  }
  
  
  public boolean actionAskFavour(Actor actor, Actor asked) {
    final boolean report = eventsVerbose && I.talkAbout == actor;
    if (report) I.say("\nSuggesting joint activity with "+asked);
    //  TODO:  Elaborate on this a little with some dialogue.
    
    final float joinMotive = DialogueUtils.talkResult(
      SUASION, ROUTINE_DC, actor, other
    ) * ROUTINE;
    if (Choice.assignedJointActivity(this, actor, other, joinMotive)) {
      if (report) I.say("  Accepted!");
      return true;
    }
    if (report) I.say("  Rejected.");
    return false;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (super.needsSuffix(d, "Talking to ")) {
      d.append(other);
    }
  }
}






/*
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
//*/




/*
float success = 0;
//  Suasion vs. Suasion.  Half DC.  Bonus for truth sense, esp. vs deceit.
//  Plus a routine check for language.

//  Another modifier for attitude.

//  Counsel or command for instruction.  Level of skill taught for DC.
//  ...Maybe that should be a different activity, though?

//  Suasion for pleas.  Level of reluctance for DC.  Command for inferiors.

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

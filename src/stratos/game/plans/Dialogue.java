/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
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
    stepsVerbose = false,
    onlyBegun    = true ;
  
  private boolean shouldReportEval() {
    return evalVerbose  && I.talkAbout == actor && (hasBegun() || ! onlyBegun);
  }
  
  private boolean shouldReportSteps() {
    return stepsVerbose && I.talkAbout == actor && (hasBegun() || ! onlyBegun);
  }
  
  final public static int
    TYPE_CONTACT = 0,
    TYPE_CASUAL  = 1,
    TYPE_PLEA    = 2;
  
  //  TODO:  SEE IF YOU CAN USE THESE
  final public static float
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
  
  final private int depth;  //  (Safety measure against infinite-recursion.)
  private int stage = STAGE_INIT;
  protected Session.Saveable topic;
  
  
  
  public Dialogue(Actor actor, Actor other) {
    this(actor, other, null, TYPE_CASUAL);
  }
  
  
  public Dialogue(Actor actor, Actor other, int type) {
    this(actor, other, null, type);
  }
  
  
  private Dialogue(Actor actor, Actor other, Dialogue starts, int type) {
    super(actor, other, MOTIVE_LEISURE, MILD_HELP);
    this.other  = other;
    this.starts = starts == null ? this : starts;
    this.depth  = starts == null ? 0 : (1 + starts.depth);
    this.type   = type;
    if (type == TYPE_PLEA) setMotive(MOTIVE_EMERGENCY, 0);
  }
  
  
  public Dialogue(Session s) throws Exception {
    super(s);
    other  = (Actor) s.loadObject();
    starts = (Dialogue) s.loadObject();
    depth  = s.loadInt();
    type   = s.loadInt();
    stage  = s.loadInt();
    topic  = s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(other );
    s.saveObject(starts);
    s.saveInt   (depth );
    s.saveInt   (type  );
    s.saveInt   (stage );
    s.saveObject(topic );
  }
  
  
  public Plan copyFor(Actor other) {
    return new Dialogue(other, this.other, null, type);
  }
  
  
  
  /**  Utility methods for assessing possibility-
    */
  private boolean canTalk(Actor other) {
    final boolean report = shouldReportEval();
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
    if (report) {
      I.say("\nChecking if "+other+" will talk to "+actor);
      I.say("  Starts:     "+I.tagHash(starts));
      I.say("  This is:    "+I.tagHash(this  ));
      I.say("  Stage is:   "+stage            );
      I.say("  Chats with: "+chatsWith        );
      
      if (chatsWith == null) {
        I.say("  Agenda is:");
        for (Behaviour b : other.mind.agenda()) {
          I.say("    "+b);
        }
      }
    }
    
    if (chatsWith != null && chatsWith != actor) {
      if (report) I.say("  Other actor busy talking.");
      return false;
    }
    if (chatsWith == actor) {
      if (report) I.say("  Conversation ongoing- okay.");
      return true;
    }
    if (starts.stage > STAGE_GREET && ! starts.isActive()) {
      if (report) I.say("  Conversation starter done.");
      return false;
    }
    if (this != starts && other == starts.actor()) {
      if (report) I.say("  Other actor started conversation.");
      return true;
    }
    
    final Dialogue sample = starts.childDialogue(actor);
    if (other.mind.mustIgnore(sample)) {
      if (report) I.say("  Other actor is too busy!");
      return false;
    }
    if (report) I.say("  Talking okay!");
    return true;
  }
  
  
  private Dialogue childDialogue(Actor other) {
    final boolean report = shouldReportEval();
    final Dialogue d = new Dialogue(other, actor, starts, type);
    
    if (report) {
      I.say("\nHAVE CREATED CHILD DIALOGUE: "+d);
      I.say("  Starts: "+d.starts);
    }
    d.setMotiveFrom(this, 0 - motiveBonus() / 2f);
    return d;
  }
  
  
  
  
  /**  Target selection and priority evaluation-
    */
  final static Trait BASE_TRAITS[] = { OUTGOING, POSITIVE, EMPATHIC };
  final static Skill BASE_SKILLS[] = { SUASION, TRUTH_SENSE };
  
  
  protected float getPriority() {
    final boolean report = shouldReportEval();
    
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
      //maxRange    = actor.health.sightRange() * 2,
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
    //else if (Spacing.distance(actor, other) > maxRange) return -1;
    
    if (report) {
      I.say("  Final bonus:  "+bonus      );
    }
    if (bonus <= talkThresh || novelty <= talkThresh) {
      if (report) I.say("\n  Nothing to talk about- skipping!");
      return -1;
    }
    final float priority = priorityForActorWith(
      actor, other,
      IDLE, bonus * CASUAL,
      MILD_HELP, NO_COMPETITION, NO_FAIL_RISK,
      BASE_SKILLS, BASE_TRAITS, HEAVY_DISTANCE_CHECK,
      report
    );
    return Nums.clamp(priority, 0, URGENT);
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    final boolean report = shouldReportSteps();
    if (report) {
      I.say("\nGetting next dialogue step for "+actor);
      I.say("  Plan is:  "+I.tagHash(this));
      I.say("  Stage is: "+stage);
    }
    
    if (stage >= STAGE_DONE) {
      if (report) I.say("  DIALOGUE COMPLETE");
      return null;
    }
    
    if (stage == STAGE_INIT || stage == STAGE_GREET) {
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
    
    if (report) I.say("  NO NEXT STEP FOUND");
    return null;
  }
  
  
  public boolean actionGreet(Actor actor, Boarding aboard) {
    final boolean report = shouldReportSteps();
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
    topic = (this == starts) ? selectTopic() : starts.topic;
    discussTopic(topic);
    if (! canTalk(other)) stage = STAGE_BYE;
    return true;
  }
  
  
  public boolean actionFarewell(Actor actor, Actor other) {
    stage = STAGE_DONE;
    return true;
  }
  

  
  protected Session.Saveable selectTopic() {
    //  TODO:  If this is a fresh acquaintance, consider general introductions.
    
    return DialogueUtils.pickChatTopic(this, other);
  }
  
  
  protected void discussTopic(Session.Saveable topic) {
    DialogueUtils.tryChat(other, other);
    
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
    if (topic instanceof Memory) {
      DialogueUtils.discussEvent (actor, other, (Memory) topic);
      return;
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (starts.topic != null) {
      d.append("Discussing ");
      d.append(starts.topic);
      d.append(" with ");
      d.append(other);
    }
    else if (super.needsSuffix(d, "Talking to ")) {
      d.append(other);
    }
  }
}





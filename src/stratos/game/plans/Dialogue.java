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
import static stratos.game.actors.Qualities.*;



//  TODO:  You need to use separate actions for invites/gifting.  Get rid of
//  urgency-based quits for now, and just allow for 3 exchanges at a time, say.


//  TODO:  Allow for 'discussion' with the dead- labelled as 'paying respects'?
//         (But only at grave markers.)



public class Dialogue extends Plan {
  
  
  /**  Constants, data fields, constructors and save/load methods-
    */
  private static boolean
    evalVerbose  = false,
    stepsVerbose = false,
    onlyBegun    = false;
  
  private boolean shouldReportEval() {
    //return I.talkAbout == actor;
    return evalVerbose  && I.talkAbout == actor && (hasBegun() || ! onlyBegun);
  }
  
  private boolean shouldReportSteps() {
    //return I.talkAbout == actor;
    return stepsVerbose && I.talkAbout == actor && (hasBegun() || ! onlyBegun);
  }
  
  final public static int
    TYPE_CONTACT = 0,
    TYPE_CASUAL  = 1,
    TYPE_PLEA    = 2;
  
  final public static float
    BORED_DURATION = Stage.STANDARD_HOUR_LENGTH * 1,
    PLEA_DURATION  = 3;
  
  final static int
    STAGE_INIT   = -1,
    STAGE_GREET  =  0,
    STAGE_CHAT   =  1,
    STAGE_BYE    =  2,
    STAGE_DONE   =  3;
  
  
  final Actor other;
  final int type;
  
  private Dialogue starts = this;
  private int stage = STAGE_INIT;
  private Session.Saveable topic;
  private float tryCounter;
  private int checkBonus;
  
  
  public static Dialogue dialogueFor(Actor actor, Actor other) {
    if (PlanUtils.harmIntendedBy(other, actor, false) > 0) {
      return new Dialogue(actor, other, TYPE_PLEA);
    }
    else {
      return new Dialogue(actor, other, TYPE_CASUAL);
    }
  }
  
  
  public static Dialogue responseFor(
    Actor other, Actor starts, Dialogue intro, float motiveBonus
  ) {
    final Dialogue current = (Dialogue) other.matchFor(Dialogue.class, true);
    if (current != null && current.subject == starts) return current;
    
    if (intro == null) intro = (Dialogue) starts.matchFor(Dialogue.class, true);
    if (intro != null && intro.other != other) intro = null;
    if (intro == null) intro = dialogueFor(starts, other);
    
    final Dialogue response = new Dialogue(other, starts, intro.type);
    response.starts = intro;
    
    motiveBonus += intro.motiveBonus() / 2;
    response.addMotives(intro.motiveProperties(), motiveBonus);
    return response;
  }
  
  
  protected Dialogue(Actor actor, Actor other, int type) {
    super(actor, other, MOTIVE_LEISURE, MILD_HELP);
    this.other  = other;
    this.starts = this ;
    this.type   = type ;
    if (type == TYPE_PLEA) toggleMotives(MOTIVE_EMERGENCY, true);
  }
  
  
  public Dialogue(Session s) throws Exception {
    super(s);
    other      = (Actor) s.loadObject();
    starts     = (Dialogue) s.loadObject();
    type       = s.loadInt();
    stage      = s.loadInt();
    topic      = s.loadObject();
    tryCounter = s.loadFloat();
    checkBonus = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(other     );
    s.saveObject(starts    );
    s.saveInt   (type      );
    s.saveInt   (stage     );
    s.saveObject(topic     );
    s.saveFloat (tryCounter);
    s.saveInt   (checkBonus);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Dialogue(other, this.other, type);
  }
  
  
  public Session.Saveable topic() {
    return topic;
  }
  
  
  public Dialogue setCheckBonus(float bonus) {
    this.checkBonus = (int) bonus;
    return this;
  }
  
  
  
  /**  Utility methods for assessing possibility-
    */
  private boolean canTalk(Actor other) {
    final boolean report = I.talkAbout == other && shouldReportEval();
    
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
      I.say("  Agenda is:");
      for (Behaviour b : other.mind.agenda()) {
        I.say("    "+b);
      }
    }
    
    if (! other.health.conscious()) {
      if (report) I.say("  Other actor is unconscious.");
      return false;
    }
    if (chatsWith == actor) {
      if (report) I.say("  Conversation ongoing- okay.");
      return true;
    }
    if (type == TYPE_CASUAL && chatsWith != null && chatsWith != actor) {
      return false;
    }
    if (stage == STAGE_INIT && other == starts.actor()) {
      if (report) I.say("  Other actor started conversation.");
      return true;
    }
    
    final Dialogue response = responseFor(other, actor, this, 0);
    if (other.mind.mustIgnore(response)) {
      if (report) {
        I.say("  Other actor is too busy!");
        I.say("    Chat priority: "+response.priorityFor(other));
      }
      return false;
    }
    if (report) I.say("  Talking okay!");
    return true;
  }
  
  
  private boolean isAnimal() {
    return actor.species().animal();
  }
  
  
  public boolean isStarter() {
    return starts == this;
  }
  
  
  public boolean isResponse() {
    return starts != this;
  }
  
  
  
  /**  Target selection and priority evaluation-
    */
  final static Trait BASE_TRAITS[] = { OUTGOING, POSITIVE, EMPATHIC };
  final static Skill BASE_SKILLS[] = { SUASION, TRUTH_SENSE };
  
  
  protected float getPriority() {
    final boolean report = shouldReportEval();
    if (report) I.say("\nChecking for dialogue between "+actor+" and "+other);
    
    final boolean casual = type == TYPE_CASUAL;
    setCompetence(1); //  Will modify below
    
    if (GameSettings.noChat || stage == STAGE_DONE) {
      if (report) I.say("\n  Dialogue complete.");
      return -1;
    }
    
    if (stage == STAGE_BYE) {
      if (report) I.say("\n  Saying goodbye.");
      return CASUAL;
    }
    
    if (casual && ! canTalk(other)) {
      if (report) I.say("\n  "+other+" can't talk now.");
      return -1;
    }
    
    setCompetence(successChanceFor(actor));
    if (starts == this && competence() <= 0) {
      if (report) I.say("\n  Cannot communicate with "+other+".");
      return -1;
    }
    
    final float priority = PlanUtils.dialoguePriority(
      actor, other, casual, motiveBonus(), competence()
    );
    
    if (report) I.say("  Priority is: "+priority);
    return priority;
  }
  
  
  public float successChanceFor(Actor actor) {
    if (isAnimal() && ! isStarter()) return 0.5f;
    return DialogueUtils.communicationChance(actor, other);
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
    if (stage == STAGE_INIT) {
      tryCounter = 0;
      stage = STAGE_GREET;
    }
    
    if (stage == STAGE_GREET) {
      if (report) I.say("  Greeting "+other);
      
      final Action greeting = new Action(
        actor, other,
        this, "actionGreet",
        Action.TALK, "Greeting "
      );
      if (! other.indoors()) greeting.setProperties(Action.RANGED);
      return greeting;
    }
    
    if (stage == STAGE_CHAT) {
      final String anim = Rand.yes() ? Action.TALK : Action.TALK_LONG;
      
      if (Spacing.distance(other, actor) > 1) {
        if (report) I.say("  Waiting for "+other);
        final Action waits = new Action(
          actor, other,
          this, "actionWait",
          anim, "Waiting for "
        );
        waits.setProperties(Action.NO_LOOP);
        return waits;
      }

      if (report) I.say("  Chatting with "+other);
      final Action chats = new Action(
        actor, other,
        this, "actionChats",
        anim, "Chatting with "
      );
      if (isAnimal()) {
        chats.setMoveTarget(Spacing.pickFreeTileAround(other, actor));
      }
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
  
  
  public boolean actionGreet(Actor actor, Actor other) {
    final boolean report = shouldReportSteps();
    final Target otherChats = other.planFocus(Dialogue.class, true);
    if (report) {
      I.say("\nGeeting "+other+"!");
      I.say("  Other is chatting with: "+otherChats);
    }
    
    if (otherChats != actor) {
      if (canTalk(other)) {
        if (report) I.say("  Assigning fresh dialogue to "+other);
        final Dialogue child = responseFor(other, actor, this, 0);
        other.mind.assignBehaviour(child);
      }
      else {
        tryCounter++;
        return false;
      }
    }
    
    if (report) I.say("  Okay to start chatting...");
    this.stage = STAGE_CHAT;
    return true;
  }
  
  
  public boolean actionWait(Actor actor, Actor other) {
    return true;
  }
  
  
  public boolean actionChats(Actor actor, Actor other) {
    final boolean close =
      shouldClose() && responseFor(other, actor, this, 0).shouldClose()
    ;
    topic = (this == starts) ? selectTopic(close) : starts.topic;
    discussTopic(topic, close);
    tryCounter++;
    
    final boolean report = shouldReportSteps();
    if (report) {
      I.say("\nChatting with "+other);
      I.say("  Should close? "+close);
      I.say("  Starts is:    "+starts.getClass().getSimpleName());
      I.say("  Starts ID:    "+starts.hashCode());
      I.say("  This is:      "+this.getClass().getSimpleName());
      I.say("  This ID:      "+this.hashCode());
    }
    
    if (close || ! canTalk(other)) stage = STAGE_BYE;
    return true;
  }
  
  
  public boolean actionFarewell(Actor actor, Actor other) {
    stage = STAGE_DONE;
    other.relations.incRelation(actor, 0, 0, -1);
    actor.relations.incRelation(other, 0, 0, -1);
    return true;
  }
  
  
  
  /**  Helper methods for determining closure and topic-selection:
    */
  protected boolean shouldClose() {
    if (type == TYPE_PLEA && tryCounter > PLEA_DURATION) return true;
    else return tryCounter >= BORED_DURATION;
  }
  
  
  protected Session.Saveable selectTopic(boolean close) {
    if (isAnimal()) return DialogueUtils.LINE_ANIMAL;
    
    //  TODO:  If this is a fresh acquaintance, consider general introductions?
    return DialogueUtils.pickChatTopic(this, other);
  }
  
  
  protected void discussTopic(Session.Saveable topic, boolean close) {
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
    if (topic instanceof Memory) {
      DialogueUtils.discussEvent (actor, other, (Memory) topic);
      return;
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (isAnimal()) {
      d.appendAll("Playing with ", other);
    }
    else if (! other.species().sapient()) {
      d.appendAll("Communicating with ", other);
    }
    else if (starts.topic != null) {
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





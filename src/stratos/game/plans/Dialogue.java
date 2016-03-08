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



//  TODO:  Allow for 'discussion' with the dead- labelled as 'paying respects'?
//         (But only at grave markers.)


public class Dialogue extends Plan {
  
  
  /**  Constants, data fields, constructors and save/load methods-
    */
  public static boolean
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
    STAGE_HAIL   =  0,
    STAGE_CHAT   =  1,
    STAGE_BYE    =  2,
    STAGE_DONE   =  3;
  
  
  final Actor other;
  final int type;
  
  private Dialogue starts = this;
  private int stage = STAGE_INIT;
  private Session.Saveable topic;
  private int checkBonus;
  
  
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
    checkBonus = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(other     );
    s.saveObject(starts    );
    s.saveInt   (type      );
    s.saveInt   (stage     );
    s.saveObject(topic     );
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
  
  
  
  /**  External factory methods and utility methods for assessing possibility-
    */
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
  
  
  private boolean canTalkWith(Actor with) {
    final boolean report = (
      I.talkAbout == other || I.talkAbout == actor
    ) && shouldReportEval();
    
    if (! with.health.conscious()) return false;
    if (actor.relations.noveltyFor(with) <= 0) return false;
    
    if (stage == STAGE_INIT && other == starts.actor()) return true;
    final Target chatsWith = with.planFocus(Dialogue.class, true);
    if (chatsWith == actor) return true;
    if (with.origin().inside().size() > 1 && ! with.indoors()) return false;
    
    final Dialogue response = responseFor(with, actor, this, 0);
    if (isCasual() && other.mind.mustIgnore(response)) {
      if (report) {
        I.say("  Other actor is too busy!");
        I.say("    Chat priority: "+response.priority());
      }
      return false;
    }
    if (report) I.say("  Talking okay!");
    return true;
  }
  
  
  protected boolean checkExpiry(float maxTries) {
    actor.relations.incRelation(other, 0, 0, -1f / maxTries);
    if (actor.relations.noveltyFor(other) == 0) return true;
    return false;
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
  
  
  public boolean isPlea() {
    return type == TYPE_PLEA;
  }
  
  
  public boolean isCasual() {
    return type == TYPE_CASUAL;
  }
  
  
  public boolean isContact() {
    return type == TYPE_CONTACT;
  }
  
  
  
  /**  Target selection and priority evaluation-
    */
  final static Trait BASE_TRAITS[] = { OUTGOING, EMPATHIC };
  final static Skill BASE_SKILLS[] = { SUASION, TRUTH_SENSE };
  
  
  protected float getPriority() {
    
    setCompetence(1);
    final boolean report = shouldReportEval();
    
    if (GameSettings.noChat || stage == STAGE_DONE) {
      if (report) I.say("\n  Dialogue complete.");
      return -1;
    }
    if (stage == STAGE_BYE) {
      if (report) I.say("\n  Saying goodbye.");
      return CASUAL;
    }
    if (! canTalkWith(other)) {
      if (report) I.say("\n  "+other+" can't talk now.");
      return -1;
    }

    if (isAnimal() && ! isStarter()) setCompetence(0.5f);
    else setCompetence(DialogueUtils.communicationChance(actor, other));
    
    if (starts == this && competence() <= 0) {
      if (report) I.say("\n  Cannot communicate with "+other+".");
      return -1;
    }
    
    final float priority = PlanUtils.dialoguePriority(
      actor, other, isCasual(), motiveBonus(), competence()
    );
    if (report) I.say("  Priority is: "+priority);
    return priority;
  }
  
  
  public boolean isEmergency() {
    final Behaviour b = other.mind.rootBehaviour();
    return isPlea() && b != null && b.isEmergency();
  }
  
  
  public int motionType(Actor actor) {
    if (isEmergency()) return MOTION_FAST;
    else return MOTION_NORMAL;
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
    if (stage == STAGE_INIT) stage = STAGE_HAIL;
    
    if (stage == STAGE_HAIL) {
      
      if (isPlea()) {
        if (report) I.say("  Pleading with "+other);
        final Action plead = new Action(
          actor, other,
          this, "actionPlead",
          Action.TALK, "Pleading with "
        );
        if (! other.indoors()) plead.setProperties(Action.RANGED);
        return plead;
      }
      else {
        if (report) I.say("  Hailing "+other);
        final Action hailing = new Action(
          actor, other,
          this, "actionHail",
          Action.TALK, "Hailing "
        );
        if (! other.indoors()) hailing.setProperties(Action.RANGED);
        return hailing;
      }
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
  
  
  public boolean actionHail(Actor actor, Actor other) {
    final boolean report = shouldReportSteps();
    if (report) I.say("Attempting to hail "+other);
    
    boolean canTalk = canTalkWith(other);
    final Dialogue child = responseFor(other, actor, this, 0);
    final Target otherChats = other.planFocus(Dialogue.class, true);
    
    if ((! isCasual()) && checkExpiry(PLEA_DURATION)) {
      canTalk = false;
    }
    if (canTalk) {
      if (otherChats != actor) other.mind.assignBehaviour(child);
      stage = STAGE_CHAT;
      if (report) I.say("  Hail successful, will begin chat.");
    }
    else {
      stage = STAGE_DONE;
      if (report) I.say("  Hail failed, will abort...");
    }
    return true;
  }
  
  
  public boolean actionPlead(Actor actor, Actor other) {
    final boolean report = shouldReportSteps();
    if (report) I.say("Attempting to plea with "+other);
    
    boolean canTalk = canTalkWith(other) && ! checkExpiry(PLEA_DURATION);
    if (PlanUtils.harmIntendedBy(other, actor, false) <= 0 || ! canTalk) {
      if (report) I.say("  Can no longer talk...");
      stage = STAGE_DONE;
      return false;
    }
    
    final Behaviour b = other.mind.rootBehaviour();
    final Target victim = b.subject();
    float motive = b == null ? 0 : b.priority() / Plan.PARAMOUNT;
    final int DC = (int) (motive * DIFFICULT_DC);
    
    if (DialogueUtils.talkResult(COMMAND, DC, actor, other) > 0.5f) {
      other.mind.cancelBehaviour(b, "Persuaded by plea");
      float value = actor.relations.valueFor(b.subject());
      other.relations.incRelation(victim, value, 0.33f, 0);
      stage = STAGE_DONE;
      if (report) I.say("  Persuaded to stop: "+b);
      return true;
    }
    else {
      if (report) I.say("  Plea failed to persuade...");
      return false;
    }
  }
  
  
  public boolean actionWait(Actor actor, Actor other) {
    return true;
  }
  
  
  public boolean actionChats(Actor actor, Actor other) {
    
    final Dialogue response = Dialogue.responseFor(other, actor, this, 0);
    boolean done = checkExpiry(BORED_DURATION) || ! canTalkWith(other);
    if (response != null) done &= response.checkExpiry(BORED_DURATION);
    
    topic = (this == starts) ? selectTopic(done) : starts.topic;
    discussTopic(topic, done);
    
    final boolean report = shouldReportSteps();
    if (report) {
      I.say("\nChatting with "+other);
      I.say("  Should close? "+done );
      I.say("  Starts is:    "+starts.getClass().getSimpleName());
      I.say("  Starts ID:    "+starts.hashCode());
      I.say("  This is:      "+this.getClass().getSimpleName());
      I.say("  This ID:      "+this.hashCode());
    }
    
    if (done) stage = STAGE_BYE;
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
    if (isPlea()) {
      d.appendAll("Pleading with ", other);
      final Behaviour b = other.mind.rootBehaviour();
      if (b instanceof Dialogue) {
        d.appendAll(" against talking with ", b.subject());
      }
      else if (b != null) {
        d.append(" against ");
        b.describeBehaviour(d);
      }
    }
    else if (isAnimal()) {
      d.appendAll("Playing with ", other);
    }
    else if (! other.species().sapient()) {
      d.appendAll("Communicating with ", other);
    }
    else if (starts.topic != null && ! (starts.topic instanceof Plan)) {
      d.appendAll("Discussing ", starts.topic, " with ", other);
    }
    else if (stage <= STAGE_HAIL) {
      d.appendAll("Greeting ", other);
    }
    else {
      d.appendAll("Talking to ", other);
    }
  }
}










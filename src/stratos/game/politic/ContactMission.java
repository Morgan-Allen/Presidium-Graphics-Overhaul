

package stratos.game.politic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;



//  TODO:  Allow spontaneous turn-coat behaviours for both actors and venues...


public class ContactMission extends Mission {
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  final static float
    MAX_DURATION = Stage.STANDARD_DAY_LENGTH * 2;
  
  final public static int
    OBJECT_FRIENDSHIP = 0,
    OBJECT_AUDIENCE   = 1,
    OBJECT_SUBMISSION = 2;
  final static String SETTING_DESC[] = {
    "Offer friendship to ",
    "Secure audience with ",
    "Demand submission from "
  };
  
  private static boolean 
    evalVerbose  = false,
    eventVerbose = true ;
  
  
  private Actor[] talksTo = null;  //Refreshed on request, at most once/second
  private List <Actor> agreed = new List <Actor> ();
  
  private boolean isSummons   = false;
  private boolean doneContact = false;
  
  
  public ContactMission(Base base, Target subject) {
    super(
      base, subject, CONTACT_MODEL,
      "Making Contact with "+subject
    );
  }
  
  
  public ContactMission(Session s) throws Exception {
    super(s);
    s.loadObjects(agreed);
    isSummons   = s.loadBool();
    doneContact = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObjects(agreed);
    s.saveBool(isSummons  );
    s.saveBool(doneContact);
  }
  
  
  public void setupAsSummons() {
    this.isSummons = true;
    setMissionType(TYPE_SCREENED);
    setObjective(OBJECT_AUDIENCE);
    
    final Actor summon = (Actor) subject;
    summon.mind.assignMission(this);
    setApprovalFor(summon, true);
    
    final Behaviour step = nextStepFor(summon, true);
    if (! summon.mind.mustIgnore(step)) summon.mind.assignBehaviour(step);
    beginMission();
  }
  
  
  public boolean isSummons() {
    return isSummons;
  }
  
  
  
  /**  Importance assessment-
    */
  public float rateImportance(Base base) {
    //  TODO:  FILL THIS IN LATER
    return -1;
  }
  
  
  
  /**  Behaviour implementation-
    */
  private Actor[] talksTo() {
    if (talksTo != null) return talksTo;
    final Batch <Actor> batch = new Batch <Actor> ();
    if (subject instanceof Actor) {
      final Actor a = (Actor) subject;
      batch.add(a);
    }
    else if (subject instanceof Venue) {
      final Venue v = (Venue) subject;
      for (Actor a : v.staff.residents()) batch.include(a);
      for (Actor a : v.staff.workers()  ) batch.include(a);
    }
    return talksTo = batch.toArray(Actor.class);
  }
  
  
  private boolean doneTalking(Actor a) {
    if (agreed.includes(a)) return true;
    return a.relations.noveltyFor(base) < 0;
  }
  
  
  public void updateMission() {
    super.updateMission();
    talksTo = null;
    
    final boolean report = eventVerbose && (
      ((List) approved()).includes(I.talkAbout) ||
      Visit.arrayIncludes(talksTo(), I.talkAbout) ||
      I.talkAbout == this
    );
    
    boolean allDone = roles.size() > 0;
    for (Actor a : talksTo()) {
      if (! doneTalking(a)) allDone = false;
    }
    
    if (allDone) {
      applyContactEffects(report);
      doneContact = true;
    }
  }
  
  
  protected Behaviour createStepFor(Actor actor) {
    if (finished()) return null;
    final Behaviour cached = nextStepFor(actor, false);
    if (cached != null) return cached;
    //
    //  In the case of direct summonings of base-citizens, we can skip directly
    //  to the Summons.
    if (isSummons) {
      return cacheStepFor(actor, Summons.officialSummons(actor));
    }
    //
    //  Otherwise, things get a little more complex...
    final Choice choice = new Choice(actor);
    final float basePriority = basePriority(actor);
    //
    //  First of all, try to complete normal dialogue efforts with everyone
    //  involved.
    for (Actor a : talksTo()) {
      final float novelty = a.relations.noveltyFor(actor);
      if (novelty <= 0) continue;
      final Dialogue talk = new Dialogue(actor, a, Dialogue.TYPE_CONTACT);
      talk.setMotive(Plan.MOTIVE_MISSION, basePriority);
      final Gifting gifts = Gifting.nextGifting(talk, actor, a);
      
      if (gifts != null) choice.add(gifts);
      else choice.add(talk);
    }
    
    //  If that is complete, try closing the talks.
    if (choice.size() == 0) for (Actor a : talksTo) {
      if (doneTalking(a)) continue;
      final float relation = a.relations.valueFor(actor);
      final Action closeTalks = new Action(
        actor, a,
        this, "actionCloseTalks",
        Action.TALK_LONG, "Closing talks"
      );
      closeTalks.setPriority(Plan.ROUTINE * (2 + relation) / 2f);
      choice.add(closeTalks);
    }
    
    return cacheStepFor(actor, choice.pickMostUrgent());
  }
  
  
  protected boolean shouldEnd() {
    return doneContact;
  }
  
  
  public boolean actionCloseTalks(Actor actor, Actor other) {
    final boolean report = eventVerbose && I.talkAbout == actor;
    
    float DC = other.relations.valueFor(actor) * -10;
    if (objective() == OBJECT_FRIENDSHIP) DC += 0 ;
    if (objective() == OBJECT_AUDIENCE  ) DC += 10;
    if (objective() == OBJECT_SUBMISSION) DC += 20;
    
    final float danger = other.senses.fearLevel();
    DC -= danger * 5;
    
    final float novelty = other.relations.noveltyFor(actor.base());
    if (novelty < 0) DC += novelty * 10;
    float success = DialogueUtils.talkResult(SUASION, DC, actor, other);
    
    if (report) I.say("Success rating was: "+success+" with "+other);
    
    //  TODO:  Reconsider?
    //  Failed efforts annoy the subject.
    
    final float noveltyInc = -1f / Dialogue.BORED_DURATION;
    other.relations.incRelation(base, 0, 0, noveltyInc);
    if (success < 1) {
      other.relations.incRelation(
        actor, 0 - Dialogue.RELATION_BOOST, 0.1f, noveltyInc
      );
      return false;
    }
    else {
      agreed.add(other);
      return true;
    }
  }
  
  
  private void applyContactEffects(boolean report) {
    final Actor ruler = base.ruler();
    final boolean majority = agreed.size() > (talksTo().length / 2);
    
    if (report) {
      I.say("\nCONTACT MISSION COMPLETE "+this);
      I.say("  Following have agreed to terms: ");
      for (Actor a : agreed) I.say("    "+a);
    }
    
    //  TODO:  Partial success might net you an informant.
    final int object = objective();
    for (Actor other : agreed) {
      if (object == OBJECT_FRIENDSHIP) {
        //  TODO:  Actually modify relations between the bases, depending on
        //  how successful you were.  (This has the added benefit of making
        //  spontaneous combat less likely.)
        other.relations.incRelation(base, Dialogue.RELATION_BOOST, 0.5f, 0);
      }
      if (object == OBJECT_AUDIENCE && ruler != null) {
        I.say("Issuing summons to: "+other);
        other.mind.assignToDo(Summons.officialSummons(other));
      }
      if (object == OBJECT_SUBMISSION) {
        other.relations.incRelation(base, -1, 0.5f, 0);
        other.assignBase(base);
      }
    }
    
    if (subject instanceof Venue && majority) {
      if (objective() == OBJECT_SUBMISSION) {
        ((Venue) subject).assignBase(base);
      }
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public SelectionInfoPane configPanel(SelectionInfoPane panel, BaseUI UI) {
    if (panel == null) panel = new NegotiationPane(UI, this);
    
    final int type = missionType();
    final NegotiationPane NP = (NegotiationPane) panel;
    
    if (UI.played() == base) {
      return NP.configOwningPanel();
    }
    else if (allVisible || type == TYPE_PUBLIC) {
      return NP.configPublicPanel();
    }
    else if (type == TYPE_SCREENED) {
      return NP.configScreenedPanel();
    }
    else return panel;
  }
  
  public String[] objectiveDescriptions() {
    return SETTING_DESC;
  }
  
  
  public void describeMission(Description d) {
    d.append("On ");
    d.append("Contact Mission ", this);
    d.append(" with ");
    //d.append(SETTING_DESC[objectIndex()]);
    d.append(subject);
  }
}










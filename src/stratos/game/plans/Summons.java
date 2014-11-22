

package stratos.game.plans;
import stratos.game.campaign.BaseFinance;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.game.tactical.*;
import stratos.user.*;
import stratos.util.Description.Link;
import stratos.util.*;



//  TODO:  You need some kind of visual indication of being summoned to the
//  sovereign's presence.  (Or being under arrest.)


public class Summons extends Plan {
  
  
  final static float
    MAX_STAY_DURATION = Stage.STANDARD_DAY_LENGTH;
  
  private static boolean verbose = true;
  final Actor ruler;
  private int timeStayed = 0;
  
  
  
  public Summons(Actor actor, Actor ruler) {
    super(actor, ruler, true);
    this.ruler = ruler;
  }
  
  
  public Summons(Session s) throws Exception {
    super(s);
    this.ruler = (Actor) s.loadObject();
    this.timeStayed = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(ruler);
    s.saveInt(timeStayed);
  }
  
  
  public Plan copyFor(Actor other) {
    return new Summons(other, ruler);
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected float getPriority() {
    final boolean report = verbose && I.talkAbout == actor;
    if (report) I.say("\nGetting priority for summons from "+ruler);
    
    //  TODO:  Include enforcement bonus, once crime/punishment are done...
    final float
      waited = timeStayed / MAX_STAY_DURATION,
      relation = actor.relations.valueFor(ruler);
    
    final float priority = URGENT + (relation * CASUAL) - (ROUTINE * waited);
    if (report) {
      I.say("  Relation and wait-time: "+relation+"/"+waited);
      I.say("  Final priority: "+priority);
    }
    return priority;
  }
  
  
  protected Behaviour getNextStep() {
    final boolean report = verbose && I.talkAbout == actor;
    if (timeStayed >= MAX_STAY_DURATION) return null;
    
    Boarding venue = ruler.mind.home();
    if (venue == null) venue = ruler.mind.work();
    if (venue == null) venue = ruler.aboard();
    if (venue == null) return null;
    if (venue instanceof Installation) {
      if (! ((Installation) venue).structure().intact()) return null;
    }
    
    final Action stay = new Action(
      actor, venue,
      this, "actionStay",
      Action.STAND, "Staying at "+venue
    );
    return stay;
  }
  
  
  public boolean actionStay(Actor actor, Boarding venue) {
    timeStayed += 1;
    
    if (venue instanceof Venue && actor.health.hungerLevel() > 0) {
      Resting.dineFrom(actor, (Venue) venue);
    }
    
    if (BaseUI.isSelected(actor)) {
      final BaseUI UI = BaseUI.current();
      UI.selection.pushSelection(null, false);
      configDialogueFor(UI, actor, true);
    }
    return true;
  }
  
  
  public static void beginSummons(Actor subject) {
    final Actor ruler = subject.base().ruler();
    if (ruler == null) I.complain("NO RULER TO VISIT");
    final Summons summons = new Summons(subject, ruler);
    subject.mind.assignToDo(summons);
  }
  
  
  public static void cancelSummons(Actor subject) {
    final Summons summons = (Summons) subject.matchFor(Summons.class);
    if (summons == null) return;
    summons.timeStayed = 1 + (int) MAX_STAY_DURATION;
    summons.abortBehaviour();
    
    final BaseUI UI = BaseUI.current();
    final Target aboard = subject.aboard();
    if (UI != null && aboard instanceof Selectable) {
      UI.selection.pushSelection((Selectable) subject.aboard(), false);
    }
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    if (actor.aboard() == ruler.mind.home()) {
      d.append("Staying at ");
      d.append(ruler.mind.home());
    }
    else {
      d.append("Granting audience to ");
      d.append(ruler);
    }
  }
  
  
  public static boolean canSummon(Target t, Base base) {
    final Actor ruler = base.ruler();
    
    if (ruler == null || ! ruler.health.conscious()) return false;
    if (ruler.mind.work() == null) return false;
    if (ruler == t || ! (t instanceof Actor)) return false;
    
    final Actor a = (Actor) t;
    if (a.base() != base || a.mind.hasToDo(Summons.class)) return false;
    return true;
  }
  
  
  public static DialoguePanel configDialogueFor(
    final BaseUI UI, final Actor with, boolean pushNow
  ) {
    final Stack <Link> responses = new Stack();
    
    //  Ask to join a declared mission.
    if (UI.played().allMissions().size() > 0) {
      responses.add(new Link("I want you to join a mission.") {
        public void whenTextClicked() {
          pushMissionDialogue(UI, with, "What would you ask of me?");
        }
      });
    }
    
    //  Ask for instruction on a given subject.
    
    //  Ask for advice or opinion of self or others.
    
    //  Make a philosophical declaration.
    
    //  Offer a gift.
    
    responses.add(new Link("I'd like to offer you a gift.") {
      public void whenTextClicked() {
        pushGiftDialogue(UI, with, "What do you have in mind?");
      }
    });
    
    //  Order banishment, execution, or arrest.
    
    //  TODO:  Say farewell or dismiss from summons.
    
    //  TODO:  IN OTHER WORDS, THESE ARE ALL STANDARDS ASPECTS OF DIALOGUE
    //  FUNCTIONS.  IMPLEMENT AS SUCH.
    
    responses.add(new Link("You are dismissed.") {
      public void whenTextClicked() {
        cancelSummons(with);
      }
    });
    
    final DialoguePanel panel = new DialoguePanel(
      UI, with.portrait(UI), "Audience with "+with,
      "Yes, my liege?",
      responses
    );
    if (pushNow) UI.setInfoPanels(panel, null);
    return panel;
  }
  
  static void pushGiftDialogue(
    final BaseUI UI, final Actor with, String lead
  ) {
    final Stack <Link> responses = new Stack <Link> ();
    //  TODO:  Include possibility of rejection here, along with relation
    //  effects!
    
    responses.add(new Link("How about 50 credits?") {
        public void whenTextClicked() {
          UI.played().finance.incCredits(-50, BaseFinance.SOURCE_REWARDS);
          with.gear.incCredits(50);
          pushGiftResponse(UI, with, "Thank you, your grace!");
        }
    });
    
    final DialoguePanel panel = new DialoguePanel(
      UI, with.portrait(UI), "Audience with "+with,
      lead, responses
    );
    UI.setInfoPanels(panel, null);
  }
  
  
  static void pushGiftResponse(
    final BaseUI UI, final Actor with, String lead
  ) {
    final DialoguePanel panel = new DialoguePanel(
      UI, with.portrait(UI), "Audience with "+with,
      lead,
      new Link("Very well, then...") {
        public void whenTextClicked() {
          configDialogueFor(UI, with, true);
        }
      }
    );
    UI.setInfoPanels(panel, null);
  }
  
  
  static void pushMissionDialogue(
    final BaseUI UI, final Actor with, String lead
  ) {
    final Stack <Link> responses = new Stack <Link> ();
    
    for (final Mission m : UI.played().allMissions()) {
      responses.add(new Link(""+m.toString()) {
        public void whenTextClicked() {
          final boolean wouldAccept = m.priorityFor(with) > Plan.ROUTINE;
          if (wouldAccept) pushMissionResponse(UI, with, m);
          else pushMissionDialogue(UI, with, "I... must decline, my lord.");
        }
      });
    }
    
    responses.add(new Link("Speaking of other business...") {
      public void whenTextClicked() {
        configDialogueFor(UI, with, true);
      }
    });
    
    final DialoguePanel panel = new DialoguePanel(
      UI, with.portrait(UI), "Audience with "+with,
      lead, responses
    );
    UI.setInfoPanels(panel, null);
  }
  
  
  static void pushMissionResponse(
    final BaseUI UI, final Actor with, final Mission taken
  ) {
    //final Actor ruler = UI.played().ruler();
    final DialoguePanel panel = new DialoguePanel(
      UI, with.portrait(UI), "Audience with "+with,
      "My pleasure, your grace.",
      new Link("Very well, then...") {
        public void whenTextClicked() {
          UI.selection.pushSelection(taken, true);
          taken.setApprovalFor(with, true);
          with.mind.assignMission(taken);
        }
      }
    );
    UI.setInfoPanels(panel, null);
  }
}












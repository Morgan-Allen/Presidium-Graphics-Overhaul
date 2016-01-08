

package stratos.game.plans;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.economic.*;
import stratos.game.base.LawUtils.*;
import stratos.user.*;
import stratos.user.notify.*;
import stratos.util.*;
import stratos.util.Description.Link;



public class Summons extends Plan implements Messaging {
  
  
  private static boolean
    verbose = false;
  
  final public static int
    TYPE_GUEST   = 0,
    TYPE_SULKING = 1,
    TYPE_CAPTIVE = 2,
    
    DEFAULT_STAY_DURATIONS[] = {
      Stage.STANDARD_DAY_LENGTH,
      Stage.STANDARD_DAY_LENGTH,
      Stage.STANDARD_DAY_LENGTH
    };
  

  final public Actor invites;
  final public Property stays;
  final public int type;
  
  private int timeStayed;
  private int stayUntil = -1;
  private Sentence sentence;
  
  
  
  public Summons(Actor actor, Actor invites, Property stays, int type) {
    super(
      actor, invites,
      (type == TYPE_GUEST ? MOTIVE_JOB : MOTIVE_EMERGENCY), NO_HARM
    );
    this.invites = invites;
    this.stays   = stays  ;
    this.type    = type   ;
    setSentence(DEFAULT_STAY_DURATIONS[type], null);
  }
  
  
  public Summons(Session s) throws Exception {
    super(s);
    this.invites    = (Actor   ) s.loadObject();
    this.stays      = (Property) s.loadObject();
    this.type       = s.loadInt();
    this.timeStayed = s.loadInt();
    this.stayUntil  = s.loadInt();
    this.sentence   = (Sentence) s.loadEnum(Sentence.values());
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(invites   );
    s.saveObject(stays     );
    s.saveInt   (type      );
    s.saveInt   (timeStayed);
    s.saveInt   (stayUntil );
    s.saveEnum  (sentence  );
  }
  
  
  public Plan copyFor(Actor other) {
    return null;
  }
  
  
  public void setSentence(int time, Sentence sentence) {
    this.timeStayed = time;
    this.stayUntil  = -1;
    this.sentence   = sentence;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected float getPriority() {
    final boolean report = verbose && I.talkAbout == actor;
    if (report) I.say("\nGetting priority for summons from "+invites);
    
    //  TODO:  Have this expire once charges are cleared!
    if (type == TYPE_CAPTIVE && actor.aboard() == stays) return 100;
    
    final float priority = motiveBonus();
    if (report) I.say("  Priority: "+priority);
    return priority;
  }
  
  
  protected Behaviour getNextStep() {
    if (! stays.structure().intact()) return null;
    final float time = stays.world().currentTime();
    
    final boolean report = verbose && I.talkAbout == actor;
    if (report) {
      I.say("\nGetting next summons step for "+actor);
      I.say("  Staying until "+stayUntil+", time: "+time);
    }
    
    if (stayUntil != -1 && time > stayUntil) return null;
    
    //  TODO:  Allow the captor to specify your destination.
    
    if (type == TYPE_CAPTIVE && actor.aboard() != stays) {
      if (report) I.say("  Following "+invites);
      
      final Action follow = new Action(
        actor, invites,
        this, "actionFollow",
        Action.STAND, "Following "
      );
      return follow;
    }
    
    if (report) I.say("  Staying at "+stays);
    final Action stay = new Action(
      actor, stays,
      this, "actionStay",
      Action.STAND, "Staying at "
    );
    if (type == TYPE_CAPTIVE) stay.setProperties(Action.PHYS_FX);
    return stay;
  }
  
  
  public boolean actionFollow(Actor actor, Actor captor) {
    return true;
  }
  
  
  public boolean actionStay(Actor actor, Property stays) {
    
    if (stayUntil == -1) {
      stayUntil = (int) (stays.world().currentTime() + timeStayed);
    }
    
    if (actor.health.hungerLevel() > 0) {
      Resting.dineFrom(actor, (Property) stays);
    }
    if (actor.health.fatigueLevel() > 0.5f) {
      actor.health.setState(ActorHealth.STATE_RESTING);
    }
    
    checkForDialogueEntry(actor, stays);
    return true;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    if (type == TYPE_GUEST) {
      d.append("Staying as guest at ");
      d.append(stays);
    }
    else if (type == TYPE_SULKING) {
      d.append("Brooding on misdeeds at ");
      d.append(stays);
    }
    else if (type == TYPE_CAPTIVE) {
      if (actor.aboard() != stays) {
        d.append("Being taken captive to ");
        d.append(stays);
      }
      else if (sentence != null) {
        d.append("Serving sentence ("+sentence.description()+") at ");
        d.append(stays);
        //  TODO:  Give information on release date/term spent!
      }
      else {
        d.append("In custody at ");
        d.append(stays);
      }
    }
  }
  
  
  
  /**  Helper methods for the player's UI-
    */
  //  TODO:  MOVE THESE OUT OF HERE
  
  //  TODO:  Replace this with a variety of contact mission?
  
  public static Summons officialSummons(Actor subject, Actor host) {
    if (host == null) I.complain("NO RULER TO VISIT");
    
    Boarding venue = host.mind.home();
    if (venue == null) venue = host.mind.work();
    if (venue == null) venue = host.aboard();
    if (! (venue instanceof Venue)) return null;
    
    final float relation = subject.relations.valueFor(host);
    final float priority = URGENT + (relation * CASUAL);
    
    final Summons summons = new Summons(
      subject, host, (Venue) venue, TYPE_GUEST
    );
    summons.addMotives(Plan.MOTIVE_JOB, priority);
    return summons;
  }
  
  
  private boolean checkForDialogueEntry(Actor actor, Boarding stays) {
    final Base base = stays.base();
    final Mission match = base.matchingMission(actor, MissionContact.class);
    if (
      (BaseUI.isSelected(actor) || BaseUI.isSelected(match)) &&
      stays == base.HQ() && ! BaseUI.hasMessageFocus(actor)
    ) {
      final BaseUI UI = BaseUI.current();
      configDialogueFor(UI, actor, true);
      return true;
    }
    return false;
  }
  
  
  public static void cancelSummons(Actor subject) {
    final Summons summons = (Summons) subject.matchFor(Summons.class, false);
    if (summons == null) return;
    if (I.logEvents()) I.say("CANCELLING SUMMONS FOR "+subject);
    summons.interrupt(INTERRUPT_CANCEL);
    final BaseUI UI = BaseUI.current();
    if (BaseUI.hasMessageFocus(subject)) UI.clearMessagePane();
  }
  
  
  public static int numSummoned(Property p) {
    int count = 0;
    for (Actor a : p.staff().visitors()) {
      if (a.isDoing(Summons.class, p)) count++;
    }
    return count;
  }
  
  
  public static boolean canSummon(Object t, Base base) {
    final Actor ruler = base.ruler();
    if (ruler == null || ruler.mind.work() == null) return false;
    if (ruler == t || ! (t instanceof Actor)) return false;
    
    final Actor a = (Actor) t;
    if (a.mind.hasToDo(Summons.class)) return false;
    return true;
  }
  
  
  public static Property summonedTo(Accountable m) {
    if (! (m instanceof Actor)) return null;
    final Summons s = (Summons) ((Actor) m).matchFor(Summons.class, false);
    return s == null ? null : s.stays;
  }
  
  
  
  /**  Helper methods for dialogue-construction
    */
  public MessagePane loadMessage(Session s, BaseUI UI) throws Exception {
    return configDialogueFor(UI, invites, false);
  }
  
  
  public void saveMessage(MessagePane message, Session s) throws Exception {
    //  No action required...
  }
  
  
  public void messageWasOpened(MessagePane message, BaseUI UI) {
    //  No action required...
  }
  
  
  private MessagePane messageFor(
    BaseUI UI, Actor with, String lead, Series <Link> responses
  ) {
    final MessagePane panel = new MessagePane(
      UI, with.portrait(UI), "Audience with "+with, with, this
    );
    panel.assignContent(lead, responses);
    panel.assignParent(Selection.currentSelectionPane());
    return panel;
  }
  
  
  private MessagePane configDialogueFor(
    final BaseUI UI, final Actor with, boolean pushNow
  ) {
    final Stack <Link> responses = new Stack();
    
    //  Ask to join a declared mission.
    if (UI.played().tactics.allMissions().size() > 0) {
      responses.add(new Link("I want you to join a mission.") {
        public void whenClicked(Object context) {
          pushMissionDialogue(UI, with, "What would you ask of me?");
        }
      });
    }
    
    //  Ask for instruction on a given subject.
    
    //  Ask for advice or opinion of self or others.
    
    //  Make a philosophical declaration.
    
    //  Offer a gift.
    
    responses.add(new Link("I'd like to offer you a gift.") {
      public void whenClicked(Object context) {
        pushGiftDialogue(UI, with, "What do you have in mind?");
      }
    });
    
    responses.add(new Link("I no longer require your services.") {
      public void whenClicked(Object context) {
        pushDismissalDialogue(UI, with, "But sir!  I have always been loyal!");
      }
    });
    
    //  Order banishment, execution, or arrest.
    
    //  TODO:  Say farewell or dismiss from summons.
    
    //  TODO:  IN OTHER WORDS, THESE ARE ALL STANDARDS ASPECTS OF DIALOGUE
    //  FUNCTIONS.  IMPLEMENT AS SUCH.
    
    responses.add(new Link("You are dismissed.") {
      public void whenClicked(Object context) {
        cancelSummons(with);
      }
    });
    
    final MessagePane pane = messageFor(UI, with, "Yes my liege?", responses);
    if (pushNow) UI.setMessagePane(pane);
    return pane;
  }
  
  
  private void pushGiftDialogue(
    final BaseUI UI, final Actor with, String lead
  ) {
    final Stack <Link> responses = new Stack <Link> ();
    //  TODO:  Include possibility of rejection here, along with relation
    //  effects!
    
    responses.add(new Link("How about 50 credits?") {
        public void whenClicked(Object context) {
          UI.played().finance.incCredits(-50, BaseFinance.SOURCE_REWARDS);
          with.gear.incCredits(50);
          pushGiftResponse(UI, with, "Thank you, your grace!");
        }
    });
    
    final MessagePane pane = messageFor(UI, with, lead, responses);
    UI.setMessagePane(pane);
  }
  
  
  private void pushDismissalDialogue(
    final BaseUI UI, final Actor with, String lead
  ) {
    final Stack <Link> responses = new Stack <Link> ();
    
    responses.add(new Link("Nonetheless.  Clear out your station.") {
      public void whenClicked(Object context) {
        with.mind.setWork(null);
        cancelSummons(with);
      }
    });
    responses.add(new Link("Hmm.  Perhaps I shall reconsider.") {
      public void whenClicked(Object context) {
        configDialogueFor(UI, with, true);
      }
    });
    
    final MessagePane pane = messageFor(UI, with, lead, responses);
    UI.setMessagePane(pane);
  }
  
  
  private void pushGiftResponse(
    final BaseUI UI, final Actor with, String lead
  ) {
    final MessagePane panel = new MessagePane(
      UI, with.portrait(UI), "Audience with "+with, with, this
    ).assignContent(lead, new Link("Very well, then...") {
      public void whenClicked(Object context) {
        configDialogueFor(UI, with, true);
      }
    });
    UI.setMessagePane(panel);
  }
  
  
  private void pushMissionDialogue(
    final BaseUI UI, final Actor with, String lead
  ) {
    final Stack <Link> responses = new Stack <Link> ();
    final Actor ruler = UI.played().ruler();
    if (! Pledge.TYPE_JOIN_MISSION.canMakePledge(with, ruler)) return;
    
    //  TODO:  If the offer is rejected, elaborate on why, and possibly suggest
    //  a counter-offer.  Also, consider an option to 'insist' on joining?
    
    for (Pledge p : Pledge.TYPE_JOIN_MISSION.variantsFor(with, ruler)) {
      final Mission m = (Mission) p.refers();
      final JoinMission joining = JoinMission.resume(with, m);
      
      responses.add(new Link(""+m.toString()) {
        public void whenClicked(Object context) {
          final boolean wouldAccept = joining.priorityFor(with) > 0;
          
          if (wouldAccept) {
            pushMissionResponse(UI, with, m);
          }
          else if (m.rateCompetence(with) < 1) {
            pushMissionDialogue(UI, with,
              "I fear I lack the skills required, my lord."
            );
          }
          else {
            pushMissionDialogue(UI, with, "I... must decline, my lord.");
          }
        }
      });
    }
    
    responses.add(new Link("Speaking of other business...") {
      public void whenClicked(Object context) {
        configDialogueFor(UI, with, true);
      }
    });

    final MessagePane pane = messageFor(UI, with, lead, responses);
    UI.setMessagePane(pane);
  }
  
  
  private void pushMissionResponse(
    final BaseUI UI, final Actor with, final Mission taken
  ) {
    final Batch <Link> responses = new Batch();
    responses.add(new Link("Very well, then...") {
      public void whenClicked(Object context) {
        Selection.pushSelection(taken, null);
        with.mind.assignMission(taken);
        taken.setApprovalFor(with, true);
      }
    });
    final MessagePane pane = messageFor(
      UI, with, "My pleasure, your grace.", responses
    );
    UI.setMessagePane(pane);
  }
}












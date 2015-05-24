

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



public class Summons extends Plan implements MessagePane.MessageSource {
  
  
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
  
  
  public MessagePane configMessage(String title, BaseUI UI) {
    //  TODO:  IMPLEMENT THIS!
    return null;
  }
  
  
  public void messageWasOpened(String titleKey, BaseUI UI) {
    //  TODO:  ALSO THIS!
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
      stays == base.HQ() && ! MessagePane.hasFocus(actor)
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
    final Target aboard = subject.aboard();
    if (UI != null && aboard instanceof Selectable) {
      UI.selection.pushSelection((Selectable) subject.aboard());
    }
  }
  
  
  public static int numSummoned(Property p) {
    int count = 0;
    for (Actor a : p.staff().visitors()) {
      if (a.isDoing(Summons.class, p)) count++;
    }
    return count;
  }
  
  
  public static boolean canSummon(Target t, Base base) {
    final Actor ruler = base.ruler();
    if (ruler == null || ! ruler.health.conscious()) return false;
    if (ruler.mind.work() == null) return false;
    if (ruler == t || ! (t instanceof Actor)) return false;
    
    final Actor a = (Actor) t;
    if (a.mind.hasToDo(Summons.class)) return false;
    return true;
  }
  
  
  public static Property summonedTo(Mobile m) {
    if (! (m instanceof Actor)) return null;
    final Summons s = (Summons) ((Actor) m).matchFor(Summons.class, false);
    return s == null ? null : s.stays;
  }
  
  
  
  /**  Helper methods for dialogue-construction
    */
  private MessagePane configDialogueFor(
    final BaseUI UI, final Actor with, boolean pushNow
  ) {
    final Stack <Link> responses = new Stack();
    
    //  Ask to join a declared mission.
    if (UI.played().tactics.allMissions().size() > 0) {
      responses.add(new Link("I want you to join a mission.") {
        public void whenClicked() {
          pushMissionDialogue(UI, with, "What would you ask of me?");
        }
      });
    }
    
    //  Ask for instruction on a given subject.
    
    //  Ask for advice or opinion of self or others.
    
    //  Make a philosophical declaration.
    
    //  Offer a gift.
    
    responses.add(new Link("I'd like to offer you a gift.") {
      public void whenClicked() {
        pushGiftDialogue(UI, with, "What do you have in mind?");
      }
    });
    
    //  Order banishment, execution, or arrest.
    
    //  TODO:  Say farewell or dismiss from summons.
    
    //  TODO:  IN OTHER WORDS, THESE ARE ALL STANDARDS ASPECTS OF DIALOGUE
    //  FUNCTIONS.  IMPLEMENT AS SUCH.
    
    responses.add(new Link("You are dismissed.") {
      public void whenClicked() {
        cancelSummons(with);
      }
    });
    
    final MessagePane panel = new MessagePane(
      UI, with.portrait(UI), "Audience with "+with, with, this
    ).assignContent("Yes, my liege?", responses);
    if (pushNow) UI.setInfoPanels(panel, null);
    return panel;
  }
  
  
  private void pushGiftDialogue(
    final BaseUI UI, final Actor with, String lead
  ) {
    final Stack <Link> responses = new Stack <Link> ();
    //  TODO:  Include possibility of rejection here, along with relation
    //  effects!
    
    responses.add(new Link("How about 50 credits?") {
        public void whenClicked() {
          UI.played().finance.incCredits(-50, BaseFinance.SOURCE_REWARDS);
          with.gear.incCredits(50);
          pushGiftResponse(UI, with, "Thank you, your grace!");
        }
    });
    
    final MessagePane panel = new MessagePane(
      UI, with.portrait(UI), "Audience with "+with, with, this
    ).assignContent(lead, responses);
    UI.setInfoPanels(panel, null);
  }
  
  
  private void pushGiftResponse(
    final BaseUI UI, final Actor with, String lead
  ) {
    final MessagePane panel = new MessagePane(
      UI, with.portrait(UI), "Audience with "+with, with, this
    ).assignContent(lead, new Link("Very well, then...") {
      public void whenClicked() {
        configDialogueFor(UI, with, true);
      }
    });
    UI.setInfoPanels(panel, null);
  }
  
  
  private void pushMissionDialogue(
    final BaseUI UI, final Actor with, String lead
  ) {
    final Stack <Link> responses = new Stack <Link> ();
    
    //  TODO:  If the offer is rejected, elaborate on why, and possibly suggest
    //  a counter-offer.  Also, consider an option to 'insist' on joining?
    //
    
    final Actor ruler = UI.played().ruler();
    for (Pledge p : Pledge.TYPE_JOIN_MISSION.variantsFor(with, ruler)) {
      final Mission m = (Mission) p.refers();
      
      responses.add(new Link(""+m.toString()) {
        public void whenClicked() {
          final boolean wouldAccept = m.priorityFor(with) > 0;
          
          if (wouldAccept) {
            pushMissionResponse(UI, with, m);
          }
          else if (JoinMission.competence(with, m) < 1) {
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
      public void whenClicked() {
        configDialogueFor(UI, with, true);
      }
    });
    
    final MessagePane panel = new MessagePane(
      UI, with.portrait(UI), "Audience with "+with, with, this
    ).assignContent(lead, responses);
    UI.setInfoPanels(panel, null);
  }
  
  
  private void pushMissionResponse(
    final BaseUI UI, final Actor with, final Mission taken
  ) {
    final MessagePane panel = new MessagePane(
      UI, with.portrait(UI), "Audience with "+with,
      with, this
    ).assignContent(
      "My pleasure, your grace.",
      new Link("Very well, then...") {
        public void whenClicked() {
          UI.selection.pushSelection(taken);
          with.mind.assignMission(taken);
          taken.setApprovalFor(with, true);
        }
      }
    );
    UI.setInfoPanels(panel, null);
  }
}












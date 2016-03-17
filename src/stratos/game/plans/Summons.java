/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.plans;
import stratos.game.base.*;
import stratos.game.common.*;
import stratos.game.craft.*;
import stratos.game.actors.*;
import stratos.game.base.LawUtils.*;
import stratos.user.*;
import stratos.util.*;



public class Summons extends Plan {
  
  
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
    
    float priority = motiveBonus();
    if (type == TYPE_GUEST) priority += 20;
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
      match != null && stays == base.HQ() &&
      Selection.paneSelection() != match
    ) {
      Selection.pushSelection(match, null);
      return true;
    }
    else return false;
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
    if (a.isDoing(Summons.class, null)) return false;
    return true;
  }
  
  
  public static Property summonedTo(Actor m) {
    final Summons s = (Summons) m.matchFor(Summons.class, false);
    return s == null ? null : s.stays;
  }
  
  
  public static boolean isSummoned(Actor m) {
    final Summons s = (Summons) ((Actor) m).matchFor(Summons.class, false);
    return s != null && m.aboard() == s.stays;
  }
  
}












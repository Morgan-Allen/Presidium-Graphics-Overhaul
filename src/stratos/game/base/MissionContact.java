/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
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



//  TODO:  Allow spontaneous turn-coat/spying behaviours for actors, if their
//         affection/fear of you grows high enough.
//  TODO:  Allow defection of structures if enough of their inhabitants feel
//         inclined to join you (base off influence-levels on the map.)


public class MissionContact extends Mission {
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  private static boolean 
    stepsVerbose = true;
  
  private Pledge offers;
  private Pledge sought;
  
  private Summons asSummons;
  private Batch <Actor> allTried = new Batch <Actor> ();
  
  
  public MissionContact(Base base, Element subject) {
    super(
      base, subject, CONTACT_MODEL,
      "Making Contact with "+subject
    );
    offers = Pledge.goodWillPledge(base.ruler(), (Actor) subject);
    sought = Pledge.goodWillPledge((Actor) subject, base.ruler());
  }
  
  
  public MissionContact(Session s) throws Exception {
    super(s);
    offers    = (Pledge ) s.loadObject();
    sought    = (Pledge ) s.loadObject();
    asSummons = (Summons) s.loadObject();
    s.loadObjects(allTried);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(offers     );
    s.saveObject(sought     );
    s.saveObject(asSummons  );
    s.saveObjects(allTried);
  }
  
  
  public void setTerms(Pledge offers, Pledge sought) {
    this.offers = offers;
    this.sought = sought;
  }
  
  
  public Pledge pledgeOffers() {
    return offers;
  }
  
  
  public Pledge pledgeSought() {
    return sought;
  }
  
  
  public void setupAsSummons() {
    
    final Actor actor = (Actor) subject;
    setMissionType(TYPE_SCREENED);
    asSummons = Summons.officialSummons(actor, base.ruler());
    actor.mind.assignMission(this);
    setApprovalFor(actor, true);
    beginMission();
    
    final Behaviour step = nextStepFor(actor, true);
    if (! actor.mind.mustIgnore(step)) actor.mind.assignBehaviour(step);
  }
  
  
  public boolean isSummons() {
    return asSummons != null;
  }
  
  
  
  /**  Importance/suitability assessment-
    */
  public static MissionContact contactFor(Object target, Base base) {
    if (Summons.canSummon(target, base)) {
      final Actor summoned = (Actor) target;
      final MissionContact mission = new MissionContact(base, summoned);
      if (summoned.base() == base) mission.setupAsSummons();
      return mission;
    }
    return null;
  }
  
  
  public float targetValue(Base base) {
    //  TODO:  FILL THIS IN LATER
    return -1;
  }
  
  
  public float harmLevel() {
    return Plan.NO_HARM;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour createStepFor(Actor actor) {
    final boolean report = I.talkAbout == actor;
    
    if (finished()) return null;
    final Behaviour cached = nextStepFor(actor, false);
    if (cached != null) return cached;
    //
    //  In the case of direct contact with base-citizens, we can skip directly
    //  to the summons-
    if (asSummons != null) return cacheStepFor(actor, asSummons);
    //
    //  Otherwise, we begin talks with the subject:
    final Actor with = (Actor) subject;
    final Proposal talks = new Proposal(actor, with);
    final float novelty = with.relations.noveltyFor(actor);
    talks.setTerms(offers, sought);
    talks.addMotives(Plan.MOTIVE_MISSION, basePriority(actor));
    
    if (report) {
      I.say("Motive bonus is:    "+talks.motiveBonus());
      I.say("Discussion novelty: "+novelty);
    }
    
    //  TODO:  Check that this correlates correctly with quitting!
    if (novelty <= 0) allTried.include(actor);
    return cacheStepFor(actor, talks);
  }
  
  
  protected boolean shouldEnd() {
    if (! hasBegun()) return false;
    
    if (asSummons != null) {
      return asSummons.finished();
    }
    else {
      if (sought.accepted()) return true;
      return allTried.size() == rolesApproved();
    }
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public SelectionPane configSelectPane(SelectionPane panel, HUD UI) {
    if (panel == null) panel = new NegotiationPane(UI, this);
    
    final int type = missionType();
    final NegotiationPane NP = (NegotiationPane) panel;
    
    if (BaseUI.currentPlayed() == base) {
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
    return null;
  }
  
  
  public void describeMission(Description d) {
    d.append("Contact Mission ", this);
    d.append(" with ");
    d.append(subject);
  }
}








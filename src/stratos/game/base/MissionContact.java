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
import stratos.game.verse.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.user.notify.*;
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
  
  
  
  /**  Helper methods for personal summons-
    */
  private void setupAsSummons() {
    final Actor actor = (Actor) subject;
    setMissionType(TYPE_SCREENED);
    asSummons = Summons.officialSummons(actor, base.ruler());
  }
  
  
  public boolean isSummons() {
    return asSummons != null;
  }
  

  public void updateMission() {
    if (isSummons() && ! hasBegun()) {
      final Actor actor = (Actor) subject;
      actor.mind.assignMission(this);
      setApprovalFor(actor, true);
      beginMission();
      final Behaviour step = nextStepFor(actor, true);
      if (! actor.mind.mustIgnore(step)) actor.mind.assignBehaviour(step);
    }
    super.updateMission();
  }
  
  
  
  /**  Importance/suitability assessment-
    */
  public static MissionContact contactFor(Object target, Base base) {
    if (target instanceof Sector) {
      final Sector sector = (Sector) target;
      final SectorBase SB = base.world.offworld.baseForSector(sector);
      final Actor ruler = SB.ruler();
      if (ruler == null) return null;
      
      final MissionContact mission = new MissionContact(base, ruler);
      mission.setJourney(Journey.configForMission(mission, true));
      return mission.journey() == null ? null : mission;
    }
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
  
  
  public boolean resolveMissionOffworld() {
    
    final Series <Actor> approved = approved();
    final Pick <Actor> pick = new Pick(0);
    float chance = 0;
    for (Actor a : approved) {
      pick.compare(a, chance += MissionUtils.competence(a, this));
    }
    final Actor talks = pick.result();
    chance /= approved.size();
    
    if (talks == null) {
      TOPIC_CONTACT_FAIL.dispatchMessage("Contact failed", subject, this);
      return true;
    }
    
    final Actor with = (Actor) subject;
    final Proposal props = new Proposal(talks, with);
    props.setTerms(offers, sought);
    
    //  TODO:  Add some experience to diplomatic skills for envoys!
    
    if (Rand.num() < chance) {
      props.setOfferAccepted(true);
      TOPIC_CONTACT_OKAY.dispatchMessage("Contact successful", subject, this);
    }
    else {
      props.setOfferAccepted(false);
      TOPIC_CONTACT_FAIL.dispatchMessage("Contact failed", subject, this);
    }
    return true;
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
  
  
  
  /**  Rendering, debug and interface methods-
    */
  final static MessageTopic TOPIC_CONTACT_OKAY = new MessageTopic(
    "topic_contact_okay", true, Mobile.class, MissionContact.class
  ) {
    protected void configMessage(BaseUI UI, Text d, Object... args) {
      d.appendAll("Negotiations with ", args[0], " were successful!");
      final MissionContact m = (MissionContact) args[1];
      d.appendAll(" They have agreed to ", m.sought);
      d.appendAll(", in exchange for "   , m.offers, ".");
    }
  };

  final static MessageTopic TOPIC_CONTACT_FAIL = new MessageTopic(
    "topic_contact_fail", true, Mobile.class, MissionContact.class
  ) {
    protected void configMessage(BaseUI UI, Text d, Object... args) {
      d.appendAll("Negotiations with ", args[0], " have failed.");
      final MissionContact m = (MissionContact) args[1];
      d.appendAll(" They have refused to provide ", m.sought);
      d.appendAll(" in exchange for ", m.offers, ".");
    }
  };
  
  
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
  
  
  public void describeMission(Description d) {
    d.append("Contact Mission ", this);
    d.append(" with ");
    d.append(subject);
  }
}








/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.game.verse.*;
import stratos.graphics.widgets.Text;
import stratos.user.BaseUI;
import stratos.user.notify.MessageTopic;
import stratos.util.*;



public class MissionSecurity extends Mission {
  
  
  /**  Field definitions, constants and save/load methods-
    */
  final static int DURATION_LENGTHS[] = {
    Stage.STANDARD_DAY_LENGTH * 2,
    Stage.STANDARD_DAY_LENGTH * 3,
    Stage.STANDARD_DAY_LENGTH * 4,
  };
  final static String DURATION_NAMES[] = {
    "48 hours security for ",
    "72 hours security for ",
    "96 hours security for ",
  };
  private static boolean verbose = false;
  
  
  float inceptTime = -1;
  
  
  private MissionSecurity(Base base, Element subject) {
    super(
      base, subject, SECURITY_MODEL,
      "Securing "+subject
    );
  }
  
  
  private MissionSecurity(Base base, Sector subject) {
    super(
      base, subject, SECURITY_MODEL,
      "Securing "+subject
    );
  }
  
  
  public MissionSecurity(Session s) throws Exception {
    super(s);
    inceptTime = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveFloat(inceptTime);
  }
  
  
  public float duration() {
    return DURATION_LENGTHS[objective()];
  }
  
  
  
  /**  Importance/suitability assessment-
    */
  public static MissionSecurity securityFor(Object target, Base base) {
    if (target instanceof Sector) {
      
      //  We only permit strikes against occupied friendly territories-
      final Sector sector = (Sector) target;
      final SectorBase b = base.world.offworld.baseForSector(sector);
      final Faction owns = b.faction();
      final Verse verse = base.world.offworld;
      
      if (owns == null || sector == base.world.localSector()) return null;
      if (Faction.relationValue(owns, base.faction(), verse) < 0) return null;
      
      final MissionSecurity m = new MissionSecurity(base, sector);
      m.setJourney(Journey.configForMission(m, true));
      return m.journey() == null ? null : m;
    }
    if ((
      target instanceof Actor ||
      target instanceof Venue ||
      target instanceof Item.Dropped
    ) && ! Faction.isFactionEnemy(base, (Target) target)) {
      return new MissionSecurity(base, (Element) target);
    }
    return null;
  }
  
  
  public float targetValue(Base base) {
    //  TODO:  FILL THIS IN LATER
    return -1;
  }
  
  
  public float harmLevel() {
    return Plan.REAL_HELP * duration() / DURATION_LENGTHS[1];
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected boolean shouldEnd() {
    if (subjectAsTarget().destroyed()) return true;
    if (inceptTime == -1) return false;
    return (base.world.currentTime() - inceptTime) > duration();
  }
  
  
  public void beginMission() {
    if (hasBegun()) return;
    super.beginMission();
    inceptTime = base.world.currentTime();
  }
  
  
  
  protected Behaviour createStepFor(Actor actor) {
    if (finished()) return null;
    final Behaviour cached = nextStepFor(actor, false);
    if (cached != null) return cached;
    //  TODO:  Implement item salvage?
    
    final Patrolling patrol = Patrolling.protectionFor(
      actor, (Element) subject, 0
    );
    final float basePriority = basePriority(actor);
    patrol.addMotives(Plan.MOTIVE_MISSION, basePriority);
    return cacheStepFor(actor, patrol);
  }
  
  
  public boolean resolveMissionOffworld() {
    final Sector s = (Sector) subject;
    final float initTime = journey().arriveTime();
    
    if (base.world.currentTime() - initTime > duration()) {
      TOPIC_SECURITY_DONE.dispatchMessage("Security detail finished: "+s, s);
      return true;
    }
    else return false;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  final static MessageTopic TOPIC_SECURITY_DONE = new MessageTopic(
    "topic_security_done", true, Sector.class
  ) {
    protected void configMessage(BaseUI UI, Text d, Object... args) {
      d.appendAll("Our Security detail for ", args[0], " is due back.");
    }
  };
  
  
  public String progressDescriptor() {
    if (inceptTime == -1 || ! hasBegun()) return super.progressDescriptor();
    
    final int hours = (DURATION_LENGTHS[objective()] - (int) (
      base.world.currentTime() - inceptTime
    )) / Stage.STANDARD_HOUR_LENGTH;
    
    return super.progressDescriptor()+" ("+hours+" hours remain)";
  }
  
  
  public void describeMission(Description d) {
    d.append("Security Mission", this);
    d.append(" for ");
    d.append(subject);
  }
}






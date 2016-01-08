/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.game.plans.*;
import stratos.game.verse.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.user.notify.MessageTopic;
import stratos.util.*;



public class MissionRecon extends Mission {
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  final static float SETTING_AREAS[] = {
    Stage.ZONE_SIZE * (float) Nums.sqrt(0.25f),
    Stage.ZONE_SIZE * (float) Nums.sqrt(0.50f),
    Stage.ZONE_SIZE * (float) Nums.sqrt(0.75f),
  };
  
  
  private boolean doneRecon = false;
  
  
  
  private MissionRecon(Base base, Tile subject) {
    super(
      base, subject, RECON_MODEL,
      "Exploring "+subject.habitat().name+" at "+subject.x+" "+subject.y
    );
  }
  
  
  private MissionRecon(Base base, Sector subject) {
    super(base, subject, RECON_MODEL, "Exploring "+subject);
  }
  
  
  public MissionRecon(Session s) throws Exception {
    super(s);
    doneRecon = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveBool(doneRecon);
  }
  
  
  
  /**  Importance/suitability assessment-
    */
  public static MissionRecon reconFor(Object target, Base base) {
    if (target instanceof Sector && target != base.location) {
      final MissionRecon m = new MissionRecon(base, (Sector) target);
      m.setJourney(Journey.configForMission(m, true));
      return m.journey() == null ? null : m;
    }
    if (target instanceof Tile && Exploring.canExplore(base, (Tile) target)) {
      return new MissionRecon(base, (Tile) target);
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
  
  
  public float rateCompetence(Actor actor) {
    return Exploring.rateCompetence(actor);
  }
  
  
  public boolean allowsMissionType(int type) {
    if (isOffworld() && type == TYPE_PUBLIC) return false;
    else return super.allowsMissionType(type);
  }
  
  
  public boolean resolveMissionOffworld() {
    final Sector s = (Sector) subject;
    
    float liftChance = 0;
    for (Actor a : approved()) liftChance += rateCompetence(a);
    liftChance /= Mission.MAX_PARTY_LIMIT;
    
    float liftAmount = 0;
    if (Rand.num() < liftChance) liftAmount += 0.5f;
    if (Rand.num() < liftChance) liftAmount += 1.0f;
    
    //  TODO:  You also have to incorporate the risk of injury, retreat or
    //  casualties if the sector in question is dangerous!
    
    if (liftAmount > 0) {
      base.intelMap.liftFogAt(s, liftAmount);
      TOPIC_RECON_OKAY.dispatchMessage("Recon successful: "+s.name, s);
    }
    else {
      TOPIC_RECON_FAIL.dispatchMessage("Recon failed: "+s.name, s);
    }
    return true;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour createStepFor(Actor actor) {
    if (finished()) return null;
    final Behaviour cached = nextStepFor(actor, false);
    if (cached != null) return cached;
    
    if (subject instanceof Sector) return null;
    
    final float range = exploreRadius();
    final Exploring explore = Exploring.nextSurvey(
      base, actor, (Tile) subject, range
    );
    if (explore == null) {
      endMission(true);
      doneRecon = true;
    }
    else explore.addMotives(Plan.MOTIVE_MISSION, basePriority(actor));
    return cacheStepFor(actor, explore);
  }
  
  
  protected boolean shouldEnd() {
    return doneRecon;
  }
  
  
  public float exploreRadius() {
    return SETTING_AREAS[objective()];
  }
  
  
  
  /**  Rendering, debug and interface methods-
    */
  final static MessageTopic TOPIC_RECON_OKAY = new MessageTopic(
    "topic_recon_okay", true, Sector.class
  ) {
    protected void configMessage(BaseUI UI, Text d, Object... args) {
      d.appendAll("Reconaissance of ", args[0], " was successful. ");
      d.append("We should now have up-to-date information on this sector.");
    }
  };
  
  final static MessageTopic TOPIC_RECON_FAIL = new MessageTopic(
    "topic_recon_fail", true, Sector.class
  ) {
    protected void configMessage(BaseUI UI, Text d, Object... args) {
      d.appendAll("Exploring ", args[0], " proved to be problematic- ");
      d.append("our surveyors had to retreat to avoid harsh weather.");
    }
  };
  
  
  public void describeMission(Description d) {
    d.append("Recon Mission", this);
    if (subject instanceof Sector) d.appendAll(" to ", subject);
    else d.appendAll(" around ", subject);
  }
  
  
  public String helpInfo() {
    if (Planet.dayValue(base.world) < 0.33f) return
      "Colonists are usually reluctant to explore at night.  You may have to "+
      "wait until morning for applicants.";
    return super.helpInfo();
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    BaseUI.current().selection.renderPlane(
      rendering, base.world,
      subjectAsTarget().position(null), exploreRadius(),
      hovered ? Colour.transparency(0.25f) : Colour.transparency(0.5f),
      Selection.SELECT_CIRCLE,
      true, I.tagHash(this)+"_explore_area_radius"
    );
  }
}




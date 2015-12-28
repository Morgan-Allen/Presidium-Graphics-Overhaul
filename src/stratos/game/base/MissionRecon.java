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
import stratos.user.*;
import stratos.util.*;



//  TODO:  Give three/four more interesting options.
//  Large Area.  Stake Claim.  Soil Sampling.  Stealth Recon?


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
    if (target instanceof Sector && target != base.world.localSector()) {
      final MissionRecon m = new MissionRecon(base, (Sector) target);
      m.setJourney(Journey.configForMission(m));
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
  
  
  public void resolveMissionOffworld() {
    return;
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




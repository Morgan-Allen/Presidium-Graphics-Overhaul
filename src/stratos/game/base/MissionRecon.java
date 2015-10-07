


package stratos.game.base;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.game.plans.Exploring;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



public class MissionRecon extends Mission {
  
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  final static float SETTING_AREAS[] = {
    Stage.ZONE_SIZE * (float) Nums.sqrt(0.25f),
    Stage.ZONE_SIZE * (float) Nums.sqrt(0.50f),
    Stage.ZONE_SIZE * (float) Nums.sqrt(0.75f),
  };
  
  //  TODO:  Give three/four more interesting options.
  //  Large Area.  Stake Claim.  Soil Sampling.  Stealth Recon.
  
  
  final static String SETTING_DESC[] = {
    "Small range survey of ",
    "Medium range survey of ",
    "Large range survey of "
  };
  
  private static boolean verbose = false;
  
  
  //private Tile inRange[] = new Tile[0];
  private boolean doneRecon = false;
  
  
  
  public MissionRecon(Base base, Tile subject) {
    super(
      base, subject, RECON_MODEL,
      "Exploring "+subject.habitat().name+" at "+subject.x+" "+subject.y
    );
  }
  
  
  public MissionRecon(Session s) throws Exception {
    super(s);
    //inRange = (Tile[]) s.loadTargetArray(Tile.class);
    doneRecon = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    //s.saveTargetArray(inRange);
    s.saveBool(doneRecon);
  }
  
  
  public float exploreRadius() {
    return SETTING_AREAS[objective()];
  }
  
  
  
  /**  Importance assessment-
    */
  public float rateImportance(Base base) {
    //  TODO:  FILL THIS IN LATER
    return -1;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour createStepFor(Actor actor) {
    if (finished()) return null;
    final Behaviour cached = nextStepFor(actor, false);
    if (cached != null) return cached;
    
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
  
  
  
  /**  Rendering and interface methods-
    */
  public String[] objectiveDescriptions() {
    return SETTING_DESC;
  }
  
  
  public void describeMission(Description d) {
    d.append("On ");
    d.append("Recon Mission", this);
    final Tile tile = (Tile) subject;
    d.append(" around ");
    d.append(tile);
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




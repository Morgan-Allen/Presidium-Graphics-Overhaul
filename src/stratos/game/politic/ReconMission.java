


package stratos.game.politic;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.maps.*;
import stratos.game.plans.Exploring;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;



public class ReconMission extends Mission {
  
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  final static float SETTING_AREAS[] = {
    Stage.SECTOR_SIZE * (float) Nums.sqrt(0.25f),
    Stage.SECTOR_SIZE * (float) Nums.sqrt(0.50f),
    Stage.SECTOR_SIZE * (float) Nums.sqrt(0.75f),
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
  
  
  
  public ReconMission(Base base, Tile subject) {
    super(
      base, subject,
      MissionsTab.RECON_MODEL,
      "Exploring "+subject.habitat().name+" at "+subject.x+" "+subject.y
    );
  }
  
  
  public ReconMission(Session s) throws Exception {
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
    return SETTING_AREAS[objectIndex()];
  }
  
  
  
  /**  Behaviour implementation-
    */
  public Behaviour nextStepFor(Actor actor) {
    if (finished()) return null;
    final Behaviour cached = cachedStepFor(actor, false);
    if (cached != null) return cached;
    
    final float range = exploreRadius();
    final Exploring explore = Exploring.nextSurvey(base, actor, subject, range);
    
    if (explore == null) {
      endMission();
      doneRecon = true;
    }
    else explore.setMotive(Plan.MOTIVE_MISSION, basePriority(actor));
    return cacheStepFor(actor, explore);
  }
  
  
  protected boolean shouldEnd() {
    return doneRecon;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected String[] objectiveDescriptions() {
    return SETTING_DESC;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("On ");
    d.append("Recon Mission", this);
    final Tile tile = (Tile) subject;
    d.append(" around ");
    d.append(tile);
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {

    BaseUI.current().selection.renderPlane(
      rendering, base.world,
      subject.position(null), exploreRadius(),
      hovered ? Colour.transparency(0.25f) : Colour.transparency(0.5f),
      Selection.SELECT_CIRCLE,
      true, this
    );
  }
}



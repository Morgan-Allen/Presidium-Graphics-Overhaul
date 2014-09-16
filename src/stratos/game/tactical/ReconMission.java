


package stratos.game.tactical;
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
    World.SECTOR_SIZE * (float) Math.sqrt(0.25f),
    World.SECTOR_SIZE * (float) Math.sqrt(0.50f),
    World.SECTOR_SIZE * (float) Math.sqrt(0.75f),
  };
  final static String SETTING_DESC[] = {
    "Small range survey of ",
    "Medium range survey of ",
    "Large range survey of "
  };
  
  private static boolean verbose = false;
  
  
  private Tile inRange[] = new Tile[0];
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
    inRange = (Tile[]) s.loadTargetArray(Tile.class);
    doneRecon = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveTargetArray(inRange);
    s.saveBool(doneRecon);
  }
  
  
  public float exploreRadius() {
    return SETTING_AREAS[objectIndex()];
  }
  
  
  
  /**  Behaviour implementation-
    */
  public float priorityFor(Actor actor) {
    final boolean report = verbose && I.talkAbout == actor;
    final Exploring exploring = new Exploring(actor, base, (Tile) subject, 0);
    
    float priority = exploring.priorityFor(actor) + basePriority(actor);
    priority *= SETTING_AREAS[1] / exploreRadius();
    if (report) I.say(actor+" priority is: "+priority);
    
    return priority;
  }
  
  
  public void beginMission() {
    super.beginMission();
    inRange = Exploring.grabExploreArea(
      base.intelMap, (Tile) subject, exploreRadius()
    );
  }
  
  
  public Behaviour nextStepFor(Actor actor) {
    if (! isActive()) return null;
    
    final IntelMap map = base.intelMap;
    Tile lookedAt = null;
    float bestRating = 0;
    
    //
    //  TODO:  Try just picking an unexplored tile at random?
    
    for (Tile t : inRange) if (! t.blocked()) {
      final float fog = map.fogAt(t);
      float rating = fog < 1 ? 1 : 0;
      
      for (Role role : roles) if (role.applicant != actor) {
        Target looks = role.applicant.focusFor(Exploring.class);
        if (looks == null) looks = role.applicant;
        rating *= (10 + Spacing.distance(actor, looks)) / 10f;
      }
      if (rating > bestRating) {
        lookedAt = t;
        bestRating = rating;
      }
    }
    if (lookedAt == null) {
      doneRecon = true;
      return null;
    }
    
    final Exploring e = new Exploring(actor, base, lookedAt, 0);
    return e;
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
    Selection.renderPlane(
      rendering, subject.position(null), exploreRadius(),
      hovered ? Colour.transparency(0.25f) : Colour.transparency(0.5f),
      Selection.SELECT_SQUARE
    );
  }
}





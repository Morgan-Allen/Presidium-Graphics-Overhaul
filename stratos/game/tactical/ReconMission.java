


package stratos.game.tactical ;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.planet.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.HUD;
import stratos.user.*;
import stratos.util.*;



public class ReconMission extends Mission {
  
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  final static float SETTING_AREAS[] = {
    World.SECTOR_SIZE * (float) Math.sqrt(0.25f),
    World.SECTOR_SIZE * (float) Math.sqrt(0.50f),
    World.SECTOR_SIZE * (float) Math.sqrt(0.75f),
  } ;
  final static String SETTING_DESC[] = {
    "Small range survey of ",
    "Medium range survey of ",
    "Large range survey of "
  } ;
  
  private static boolean verbose = false ;
  Tile inRange[] = new Tile[0] ;
  boolean done = false ;
  
  
  
  public ReconMission(Base base, Tile subject) {
    super(
      base, subject,
      MissionsTab.RECON_MODEL,
      "Exploring "+subject.habitat().name+" at "+subject.x+" "+subject.y
    ) ;
  }
  
  
  public ReconMission(Session s) throws Exception {
    super(s) ;
    inRange = (Tile[]) s.loadTargetArray(Tile.class) ;
    done = s.loadBool() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveTargetArray(inRange) ;
    s.saveBool(done) ;
  }
  
  
  public float exploreRadius() {
    return SETTING_AREAS[objectIndex()] ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public float priorityFor(Actor actor) {
    final Tile centre = (Tile) subject ;
    final float basePriority = super.basePriority(actor);
    float priority = Exploring.rateExplorePoint(actor, centre, basePriority) ;
    priority *= SETTING_AREAS[1] / exploreRadius() ;
    if (verbose) I.sayAbout(actor,
      actor+" priority is: "+priority+", base priority: "+basePriority
    ) ;
    return priority ;
  }
  
  
  public void beginMission() {
    super.beginMission() ;
    inRange = Exploring.grabExploreArea(
      base.intelMap, (Tile) subject, exploreRadius()
    ) ;
  }
  
  
  public Behaviour nextStepFor(Actor actor) {
    if (! isActive()) return null;
    
    final IntelMap map = base.intelMap ;
    Tile lookedAt = null ;
    float bestRating = 0 ;
    
    //
    //  TODO:  Try just picking an unexplored tile at random?
    
    for (Tile t : inRange) if (! t.blocked()) {
      final float fog = map.fogAt(t) ;
      float rating = fog < 1 ? 1 : 0 ;
      
      for (Role role : roles) if (role.applicant != actor) {
        Target looks = role.applicant.targetFor(Exploring.class) ;
        if (looks == null) looks = role.applicant ;
        rating *= (10 + Spacing.distance(actor, looks)) / 10f ;
      }
      if (rating > bestRating) {
        lookedAt = t ;
        bestRating = rating ;
      }
    }
    if (lookedAt == null) {
      done = true ;
      return null ;
    }
    
    final Exploring e = new Exploring(actor, base, lookedAt) ;
    //e.priorityMod = actor.mind.greedFor(rewardAmount(actor)) * ROUTINE ;
    return e ;
  }
  
  
  public boolean finished() {
    return done ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected String[] objectiveDescriptions() {
    return SETTING_DESC;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("On ") ;
    d.append("Recon Mission", this) ;
    final Tile tile = (Tile) subject ;
    d.append(" around ") ;
    d.append(tile) ;
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    Selection.renderPlane(
      rendering, subject.position(null), exploreRadius(),
      hovered ? Colour.transparency(0.25f) : Colour.transparency(0.5f),
      Selection.SELECT_SQUARE
    ) ;
  }
}








package src.game.tactical ;
import src.game.common.* ;
import src.game.planet.* ;
import src.game.actors.* ;
import src.graphics.common.* ;
import src.graphics.widgets.HUD;
import src.user.* ;
import src.util.* ;



public class ReconMission extends Mission {
  
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  final static float SETTING_AREAS[] = {
    World.SECTOR_SIZE * (float) Math.sqrt(0.25f),
    World.SECTOR_SIZE * (float) Math.sqrt(0.50f),
    World.SECTOR_SIZE * (float) Math.sqrt(0.75f),
  } ;
  final static String AREA_NAMES[] = {
    "Small",
    "Medium",
    "Large"
  } ;
  
  private static boolean verbose = false ;
  
  int areaSetting = 0 ;
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
    areaSetting = s.loadInt() ;
    inRange = (Tile[]) s.loadTargetArray(Tile.class) ;
    done = s.loadBool() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(areaSetting) ;
    s.saveTargetArray(inRange) ;
    s.saveBool(done) ;
  }
  
  
  public float exploreRadius() {
    return SETTING_AREAS[areaSetting] ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public float priorityFor(Actor actor) {
    final Tile centre = (Tile) subject ;
    float reward = actor.mind.greedFor(rewardAmount(actor)) * ROUTINE ;
    float priority = Exploring.rateExplorePoint(actor, centre, reward) ;
    priority *= SETTING_AREAS[1] / exploreRadius() ;
    if (verbose) I.sayAbout(actor,
      actor+" priority is: "+priority+", base reward: "+rewardAmount(actor)+
      "\nperceived reward: "+reward
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
    
    final IntelMap map = base.intelMap ;
    Tile lookedAt = null ;
    float bestRating = 0 ;
    
    //
    //  TODO:  Try just picking an unexplored tile at random?
    
    for (Tile t : inRange) if (! t.blocked()) {
      final float fog = map.fogAt(t) ;
      float rating = fog < 1 ? 1 : 0 ;
      
      for (Role role : this.roles) if (role.applicant != actor) {
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
  public void writeInformation(Description d, int categoryID, HUD UI) {
    super.writeInformation(d, categoryID, UI) ;
    d.append("\n\nArea: ") ;
    if (begun()) d.append(AREA_NAMES[areaSetting]) ;
    else d.append(new Description.Link(AREA_NAMES[areaSetting]) {
      public void whenClicked() {
        areaSetting = (areaSetting + 1) % SETTING_AREAS.length ;
      }
    }) ;
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





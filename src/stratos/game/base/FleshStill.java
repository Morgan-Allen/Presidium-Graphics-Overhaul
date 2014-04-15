

package stratos.game.base ;
import stratos.game.actors.* ;
import stratos.game.building.* ;
import stratos.game.common.* ;
import stratos.game.planet.* ;
import stratos.game.tactical.* ;
import stratos.graphics.common.* ;
import stratos.graphics.cutout.* ;
import stratos.graphics.widgets.* ;
import stratos.user.* ;
import stratos.util.* ;



public class FleshStill extends Venue implements Economy {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    FleshStill.class, "media/Buildings/ecologist/flesh_still.png", 3, 1
  ) ;
  
  
  final SurveyStation parent ;
  GroupSprite camouflaged ;
  
  
  public FleshStill(SurveyStation parent) {
    super(3, 1, Venue.ENTRANCE_EAST, parent.base()) ;
    structure.setupStats(
      100, 4, 150, 0, Structure.TYPE_FIXTURE
    ) ;
    this.parent = parent ;
    attachSprite(MODEL.makeSprite()) ;
    camouflaged = new GroupSprite() ;
  }
  
  
  public FleshStill(Session s) throws Exception {
    super(s) ;
    parent = (SurveyStation) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(parent) ;
  }
  
  
  
  /**  Upgrades, economic functions and behaviour implementations-
    */
  public Behaviour jobFor(Actor actor) {
    return null ;
  }
  
  public Background[] careers() { return null ; }
  
  public Service[] services() {
    return new Service[] { WATER, PROTEIN, TRUE_SPICE } ;
  }
  
  
  protected void updatePaving(boolean inWorld) {
  }
  
  
  public boolean canPlace() {
    if (! super.canPlace()) return false ;
    for (Tile t : Spacing.perimeter(area(), origin().world)) if (t != null) {
      if (t.owningType() >= this.owningType()) return false ;
    }
    return true ;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void renderFor(Rendering rendering, Base base) {
    if (base == this.base()) super.renderFor(rendering, base) ;
    else {
      //
      //  Render a bunch of rocks instead.  Also, make this non-selectable.
      this.position(camouflaged.position) ;
      camouflaged.fog = this.fogFor(base) ;
      camouflaged.readyFor(rendering);
    }
  }
  
  
  public String fullName() {
    return "Flesh Still" ;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(SurveyStation.ICON, "flesh_still");
  }
  
  
  public String helpInfo() {
    return
      "Flesh Stills help to extract protein and spice from culled specimens "+
      "of native wildlife." ;
  }
  
  
  public String buildCategory() {
    return UIConstants.TYPE_ECOLOGIST ;
  }
}









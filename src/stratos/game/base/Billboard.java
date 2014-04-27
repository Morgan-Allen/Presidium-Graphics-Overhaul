/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.base ;
import stratos.game.civilian.*;
import stratos.game.common.* ;
import stratos.game.actors.* ;
import stratos.game.building.* ;
import stratos.graphics.common.* ;
import stratos.graphics.cutout.* ;
import stratos.graphics.widgets.* ;
import stratos.user.* ;
import stratos.util.* ;




public class Billboard extends Venue implements Economy {
  

  final static ModelAsset MODEL = CutoutModel.fromImage(
    Artificer.class, "media/Buildings/aesthete/billboard.png", 1, 2
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    "media/GUI/Buttons/bulletin_board_button.gif", Billboard.class
  );
  
  
  //final AuditOffice parent ;
  
  
  public Billboard(Base base) {
    super(1, 2, ENTRANCE_WEST, base) ;
    //this.parent = parent ;
    structure.setupStats(
      20,  //integrity
      2,   //armour
      50,  //build cost
      0,   //max upgrades
      Structure.TYPE_FIXTURE
    ) ;
    personnel.setShiftType(SHIFTS_ALWAYS) ;
    this.attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public Billboard(Session s) throws Exception {
    super(s) ;
    //parent = (AuditOffice) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    //s.saveObject(parent) ;
  }
  
  
  
  /**  Economic functions, upgrades and behaviour implementation-
    */
  //
  //  TODO:  Get attention from advertisers.  ...Include the hypnotic suggestion
  //  techs here?  ...Yes.
  
  //  TODO:  Have multiple modes.
  //  Boost loyalty+morale.  Boost ambience+calm.  Boost caution+health.
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates);
    
    structure.setAmbienceVal(5);
  }
  
  
  public Behaviour jobFor(Actor actor) {
    return null ;
  }
  
  
  public Background[] careers() {
    return null ;
  }
  
  
  public Service[] services() {
    return null ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Billboard" ;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "billboard");
  }
  
  
  public String helpInfo() {
    return
      "Billboards provide a staging post for news and propaganda, helping to "+
      "draw citizens' attention and boost morale.";
  }
  

  public String buildCategory() {
    return UIConstants.TYPE_AESTHETE ;
  }
}






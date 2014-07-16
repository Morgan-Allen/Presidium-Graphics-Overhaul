/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.base;
import stratos.game.civilian.*;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;




public class Edifice extends Venue implements Economy {
  

  final static ModelAsset MODEL = CutoutModel.fromImage(
    Artificer.class, "media/Buildings/aesthete/edifice.png", 4, 2
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    "media/GUI/Buttons/edifice_button.gif", Edifice.class
  );
  
  
  //
  //  Events include:
  //    First landing.  1000 citizens.
  //    Full exploration of map.
  //    Full conquest of an enemy base.
  //    Birth of an heir.  Marriage alliance.
  //    Acquiring a relic or artifact.
  //    etc.
  //  (Effects become less powerful as you repeat an event to commemmorate.)
  //
  //  Styles include:  Representative, Geometric and Surreal
  //    Representative encourages/appeals to tradition
  //    Geometric encourages/appeals to logic
  //    Surreal encourages/appeals to creativity
  
  final public static int
    
    EVENT_POPULATION =  0,
    EVENT_FAMILY     =  1,
    EVENT_FIND_RELIC =  3,
    EVENT_CONQUEST   =  2,
    EVENT_ALLIANCE   =  3,
    EVENT_EXPLORE    =  4,
    
    STYLE_REPRESENTATIVE = 0,
    STYLE_GEOMETRIC      = 1,
    STYLE_SURREALISTIC   = 2;
  
  
  int eventCode = -1, styleCode = -1;
  
  
  public Edifice(Base base) {
    super(4, 2, ENTRANCE_NONE, base);
    structure.setupStats(
      500, 50, 800, 0, Structure.TYPE_FIXTURE
    );
    this.attachSprite(MODEL.makeSprite());
  }
  
  
  public Edifice(Session s) throws Exception {
    super(s);
    eventCode = s.loadInt();
    styleCode = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(eventCode);
    s.saveInt(styleCode);
  }
  
  
  
  /**  Economic functions, upgrades and behaviour implementation-
    */
  public Behaviour jobFor(Actor actor) {
    return null;
  }
  
  
  public Background[] careers() {
    return null;
  }
  
  
  public Service[] services() {
    return null;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates);
    if (! structure.intact()) return;
    ///I.sayAbout(this, "Ambience value: "+structure.ambienceVal());
    structure.setAmbienceVal(25);
  }
  


  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Edifice";
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "edifice");
  }
  
  
  public String helpInfo() {
    return
      "The Edifice commemorates significant events in the history of your "+
      "settlement beneath a frictionless tensile facade.";
  }
  

  public String buildCategory() {
    return UIConstants.TYPE_AESTHETE;
  }
}






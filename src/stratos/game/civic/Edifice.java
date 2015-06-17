/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.civic;
import stratos.game.common.*;
import stratos.game.economic.*;
import stratos.game.actors.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.economic.Economy.*;




//  TODO:  Merge with Shrines to a particular Founder for each School?
//  ...maybe rename to Effigy?


public class Edifice extends Venue {
  
  
  final static ModelAsset MODEL = CutoutModel.fromImage(
    EngineerStation.class, "media/Buildings/aesthete/edifice.png", 3, 2
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    Edifice.class, "media/GUI/Buttons/edifice_button.gif"
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
  
  final static Blueprint BLUEPRINT = new Blueprint(
    Edifice.class, "edifice",
    "Edifice", UIConstants.TYPE_AESTHETIC, ICON,
    "The Edifice commemorates significant events in the history of your "+
    "settlement beneath a frictionless composite facade.",
    4, 2, Structure.IS_FIXTURE,
    Fabricator.BLUEPRINT, Owner.TIER_FACILITY,
    500, 50, 800, Structure.NO_UPGRADES
  );
  
  int eventCode = -1, styleCode = -1;
  
  
  public Edifice(Base base) {
    super(BLUEPRINT, base);
    attachModel(MODEL);
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
  
  
  public Traded[] services() {
    return null;
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    if (! structure.intact()) return;
    ///I.sayAbout(this, "Ambience value: "+structure.ambienceVal());
    structure.setAmbienceVal(25);
  }
  


  /**  Rendering and interface methods-
    */
  public SelectionPane configSelectPane(SelectionPane panel, BaseUI UI) {
    return VenuePane.configSimplePanel(this, panel, UI, null);
  }
}






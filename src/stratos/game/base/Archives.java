


package stratos.game.base;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.building.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;

import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.building.Economy.*;



//  Agents go here to research history, look up info on enemies, hone their
//  cognitive skills (medicine, artifice, neuroscience, etc.) or to look at
//  interactive media (simstims, e-books, and so on.)

public class Archives extends Venue {
  
  

  final static ImageAsset ICON = ImageAsset.fromImage(
    Archives.class, "media/GUI/Buttons/archives_button.gif"
  );
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    Archives.class, "media/Buildings/physician/archives.png", 3, 2
  );
  /*
  final public static FacilityProfile PROFILE = new FacilityProfile(
    Archives.class, Structure.TYPE_VENUE,
    3, 200, 3, 7,
    new TradeType[] {},
    new Background[] { ARCHIVE_SAVANT },
    Economy.CIRCUITRY_TO_DATALINKS
  );
  //*/
  
  
  public Archives(Base base) {
    super(3, 2, ENTRANCE_SOUTH, base);
  }
  
  
  public Archives(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Upgrade and economy methods-
    */
  //  TODO:  Get rid of most of these now!
  public Background[] careers() {
    return null;
  }
  
  
  public Behaviour jobFor(Actor actor) {
    return null;
  }
  
  
  public TradeType[] services() {
    return null;
  }
  
  
  public void addServices(Choice choice, Actor forActor) {
    
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Archives";
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "archives");
  }
  
  
  public String helpInfo() {
    return
      "The Archives facilitate research, administration and self-training by "+
      "your base personnel.";
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_PHYSICIAN;
  }
}









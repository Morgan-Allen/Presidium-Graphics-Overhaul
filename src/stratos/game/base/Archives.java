/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.game.base;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.civilian.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.economic.Economy.*;



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
  
  final static float
    STUDY_FEE = Backgrounds.MIN_DAILY_EXPENSE;
  
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
    structure.setupStats(
      250, 3, 350,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    );
    personnel.setShiftType(SHIFTS_BY_DAY);
    
    attachSprite(MODEL.makeSprite());
  }
  
  
  public Archives(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Upgrade and economy methods-
    */
  //  TODO:  Decide on an appropriate set of upgrades here.
  
  
  
  public Background[] careers() {
    return new Background[] { SAVANT };
  }
  
  
  public int numOpenings(Background b) {
    final int nO = super.numOpenings(b);
    if (b == SAVANT) return nO + 2;
    return 0;
  }
  
  
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null;
    final Choice choice = new Choice(actor);
    
    choice.add(stocks.nextManufacture(actor, PARTS_TO_DATALINKS));
    
    if (choice.empty()) choice.add(new Supervision(actor, this));
    return choice.weightedPick();
  }
  
  
  public Traded[] services() {
    return new Traded[] { DATALINKS, SERVICE_ADMIN };
  }
  
  
  public void addServices(Choice choice, Actor actor) {
    choice.add(new Studying(actor, this, STUDY_FEE));
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates);
    stocks.translateDemands(1, PARTS_TO_DATALINKS, this);
    
    structure.setAmbienceVal(5);
    structure.assignOutputs(Item.withAmount(POWER, 3));
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
  
  
  public String objectCategory() {
    return InstallTab.TYPE_PHYSICIAN;
  }
}









/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.civic;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.I;
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
  
  //  TODO:  Decide on an appropriate set of upgrades here.
  
  
  final public static Conversion
    CIRCUITRY_TO_DATALINKS = new Conversion(
      Archives.class, "circuitry_to_datalinks",
      1, CIRCUITRY, TO, 2, TERMINALS,
      MODERATE_DC, INSCRIPTION, SIMPLE_DC, ASSEMBLY, ACCOUNTING
    )
  ;
  
  final static VenueProfile PROFILE = new VenueProfile(
    Archives.class, "archives", "Archives",
    4, 2, IS_NORMAL,
    PhysicianStation.PROFILE, Owner.TIER_FACILITY, CIRCUITRY_TO_DATALINKS
  );
  
  
  public Archives(Base base) {
    super(PROFILE, base);
    structure.setupStats(
      250, 3, 350,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    );
    staff.setShiftType(SHIFTS_BY_DAY);
    
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
  public Background[] careers() {
    return new Background[] { SAVANT };
  }
  
  
  protected int numOpenings(Background b) {
    final int nO = super.numOpenings(b);
    if (b == SAVANT) return nO + 2;
    return 0;
  }
  
  
  protected Behaviour jobFor(Actor actor, boolean onShift) {
    if (! onShift) return null;
    final Choice choice = new Choice(actor);
    
    final Delivery d = DeliveryUtils.bestBulkCollectionFor(
      this, new Traded[] { CIRCUITRY }, 1, 5, 5
    );
    if (d != null) return d;
    
    final Manufacture m = stocks.nextManufacture(actor, CIRCUITRY_TO_DATALINKS);
    if (m != null) choice.add(m.setBonusFrom(this, false));
    
    if (choice.empty()) choice.add(Supervision.oversight(this, actor));
    return choice.weightedPick();
  }
  
  
  public Traded[] services() {
    return new Traded[] { TERMINALS, SERVICE_ADMIN };
  }
  
  
  public void addServices(Choice choice, Actor actor) {
    
    //  TODO:  Allow upgrades in different skill areas?
    choice.add(Training.asResearch(actor, this, STUDY_FEE));
    
    final Owner home = actor.mind.home();
    if (home != null) {
      final Delivery shops = DeliveryUtils.fillBulkOrder(
        this, home, new Traded[] { TERMINALS }, 1, 1
      );
      if (shops != null) choice.add(shops.setWithPayment(actor, true));
    }
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    stocks.translateDemands(CIRCUITRY_TO_DATALINKS, 1);
    
    structure.setAmbienceVal(6);
    stocks.forceDemand(POWER, 3, false);
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "archives");
  }
  
  
  public String helpInfo() {
    return Manufacture.statusMessageFor(
      "The Archives facilitate research, administration and self-training by "+
      "your base personnel.",
      this, CIRCUITRY_TO_DATALINKS
    );
  }
  
  
  public String objectCategory() {
    return InstallationPane.TYPE_PHYSICIAN;
  }
}









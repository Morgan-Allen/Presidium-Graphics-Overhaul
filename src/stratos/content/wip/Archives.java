/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.wip;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.economic.*;
import stratos.game.plans.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
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
    Archives.class, "media/Buildings/physician/archives.png", 4, 2
  );
  
  final static float
    STUDY_FEE = Backgrounds.MIN_DAILY_EXPENSE;
  
  //  TODO:  Decide on an appropriate set of upgrades here.
  
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    Archives.class, "archives",
    "Archives", Target.TYPE_WIP, ICON,
    "The Archives provide "+DATALINKS+" and facilitate research and "+
    "administration by base personnel.",
    4, 2, Structure.IS_NORMAL, Owner.TIER_FACILITY, 250, 3,
    SAVANT, DATALINKS, SERVICE_RESEARCH
  );
  
  final public static Conversion
    CIRCUITRY_TO_DATALINKS = new Conversion(
      BLUEPRINT, "circuitry_to_datalinks",
      1, CIRCUITRY, TO, 2, DATALINKS,
      MODERATE_DC, INSCRIPTION, SIMPLE_DC, ASSEMBLY, ACCOUNTING
    );
  
  
  public Archives(Base base) {
    super(BLUEPRINT, base);
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
  protected int numPositions(Background b) {
    final int nO = super.numPositions(b);
    if (b == SAVANT) return nO + 2;
    return 0;
  }
  
  
  protected Behaviour jobFor(Actor actor) {
    if (staff.offDuty(actor)) return null;
    
    final float needCirc = stocks.relativeShortage(CIRCUITRY);
    final Bringing d = BringUtils.bestBulkCollectionFor(
      this, new Traded[] { CIRCUITRY }, 1, 5, 5, true
    );
    if (d != null) {
      d.addMotives(Plan.MOTIVE_JOB, needCirc * Plan.ROUTINE);
      return d;
    }
    
    final Choice choice = new Choice(actor);
    final Manufacture m = stocks.nextManufacture(actor, CIRCUITRY_TO_DATALINKS);
    if (m != null) choice.add(m.setBonusFrom(this, false));
    
    if (choice.empty()) choice.add(Supervision.oversight(this, actor));
    return choice.weightedPick();
  }
  
  
  public void addServices(Choice choice, Actor client) {
    //
    //  TODO:  Allow upgrades for different skill areas!
    choice.add(Studying.asSkillStudy(client, this, STUDY_FEE));
    choice.add(BringUtils.nextPersonalPurchase(client, this, DATALINKS));
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    stocks.translateRawDemands(CIRCUITRY_TO_DATALINKS, 1);
    
    structure.setAmbienceVal(6);
    stocks.forceDemand(POWER, 3, false);
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String helpInfo() {
    return Manufacture.statusMessageFor(
      super.helpInfo(), this, CIRCUITRY_TO_DATALINKS
    );
  }
  
  
  protected Traded[] goodsToShow() {
    return null;
  }
}









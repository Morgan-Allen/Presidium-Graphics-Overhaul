/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.base;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.plans.*;
import stratos.game.economic.*;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.graphics.widgets.*;
import stratos.user.*;
import stratos.util.*;
import static stratos.game.actors.Qualities.*;
import static stratos.game.actors.Backgrounds.*;
import static stratos.game.economic.Economy.*;



public class EnforcerBloc extends Venue {
  
  
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    EnforcerBloc.class, "media/Buildings/physician/enforcer_bloc.png", 3, 3
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    EnforcerBloc.class, "media/GUI/Buttons/audit_office_button.gif"
  );
  
  final static VenueProfile PROFILE = new VenueProfile(
    EnforcerBloc.class, "enforcer_bloc", "Enforcer Bloc",
    3, 3, false, Bastion.PROFILE
  );
  
  
  public EnforcerBloc(Base base) {
    super(PROFILE, base);
    structure.setupStats(
      450, 2, 450,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    );
    staff.setShiftType(SHIFTS_BY_DAY);
    this.attachSprite(MODEL.makeSprite());
  }
  
  
  public EnforcerBloc(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  
  /**  Economic functions, upgrades and behaviour implementation-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
  );
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES; }
  //  TODO:  Include others.
  //  Rehab Program
  //  Holding Cells
  //  Forensics Lab
  //  Mentat & Psy Corps
  
  final public static Conversion
    PLASTICS_TO_PRESSFEED = new Conversion(
      EnforcerBloc.class, "plastics_to_pressfeed",
      1, PLASTICS, TO, 10, PRESSFEED,
      SIMPLE_DC, ACCOUNTING, DIFFICULT_DC, GRAPHIC_DESIGN
    )
  ;
  
  
  public Behaviour jobFor(Actor actor, boolean onShift) {
    if (! onShift) return null;
    final Choice choice = new Choice(actor);
    
    if (actor.vocation() == Backgrounds.AUDITOR) {
      choice.add(Audit.nextOfficialAudit(actor));
      choice.add(Sentencing.nextTrialFor(actor, this));
      choice.add(stocks.nextManufacture(actor, PLASTICS_TO_PRESSFEED));
    }
    if (actor.vocation() == Backgrounds.ENFORCER) {
      choice.add(Arrest.nextOfficialArrest(this, actor));
      choice.add(Patrolling.nextGuardPatrol(actor, this, Plan.ROUTINE));
      //  TODO:  DISTRIBUTE PRESSFEED!
    }
    return choice.weightedPick();
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v);
    if (v == Backgrounds.AUDITOR) return nO + 1;
    if (v == Backgrounds.ENFORCER) return nO + 2;
    return 0;
  }
  
  
  public void updateAsScheduled(int numUpdates, boolean instant) {
    super.updateAsScheduled(numUpdates, instant);
    
    final int prisoners = Summons.numSummoned(this);
    stocks.forceDemand(CARBS  , prisoners, Tier.CONSUMER);
    stocks.forceDemand(PROTEIN, prisoners, Tier.CONSUMER);
  }
  
  
  public Background[] careers() {
    //  TODO:  INCLUDE PSY CORPS!
    return new Background[] { AUDITOR, ENFORCER };
  }
  
  
  public Traded[] services() {
    return new Traded[] { SERVICE_SECURITY };
  }
  
  
  public boolean allowsEntry(Mobile m) {
    if (super.allowsEntry(m)) return true;
    if (Summons.summonedTo(m) == this) return true;
    return false;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "enforcer_bloc");
  }
  
  
  public String helpInfo() {
    return
      "The Enforcer Bloc provides a dedicated police force for your base, "+
      "specialising in interrogation and forensics as well as self defence "+
      "and nonlethal force.";
  }
  
  
  public String objectCategory() {
    return UIConstants.TYPE_SECURITY;
  }
}





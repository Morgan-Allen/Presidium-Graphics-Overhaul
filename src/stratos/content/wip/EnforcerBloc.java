/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.content.wip;
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
    EnforcerBloc.class, "media/Buildings/military/enforcer_bloc.png", 4, 2
  );
  final static ImageAsset ICON = ImageAsset.fromImage(
    EnforcerBloc.class, "media/GUI/Buttons/enforcer_bloc_button.gif"
  );
  
  final public static Blueprint BLUEPRINT = new Blueprint(
    EnforcerBloc.class, "enforcer_bloc",
    "Enforcer Bloc", UIConstants.TYPE_WIP, ICON,
    "The Enforcer Bloc provides a civilian police force for your base, "+
    "assisting administration and espionage as well as providing nonlethal "+
    "force.",
    4, 2, Structure.IS_NORMAL,
    Owner.TIER_FACILITY, 450,
    6,  //Integrity
    450  ,  //Armour
    Structure.NORMAL_MAX_UPGRADES
  );
  
  
  public EnforcerBloc(Base base) {
    super(BLUEPRINT, base);
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
  
  
  public Behaviour jobFor(Actor actor) {
    if (staff.offDuty(actor)) return null;
    final Choice choice = new Choice(actor);
    
    //  TODO:  Unify these with similar duties at the Bastion!
    
    if (actor.mind.vocation() == Backgrounds.AUDITOR) {
      choice.add(Audit.nextOfficialAudit(actor));
      if (staff.onShift(actor)) {
        choice.add(Sentencing.nextTrialFor(actor, this));
      }
      //  TODO:  Customise pressfeed, rather than manufacture it.
    }
    if (actor.mind.vocation() == Backgrounds.ENFORCER) {
      if (staff.onShift(actor)) {
        choice.add(Patrolling.nextGuardPatrol(actor, this, Plan.ROUTINE));
      }
      choice.add(Arrest.nextOfficialArrest(this, actor));
      //  TODO:  DISTRIBUTE PRESSFEED
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
    stocks.forceDemand(CARBS  , prisoners, false);
    stocks.forceDemand(PROTEIN, prisoners, false);
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
}





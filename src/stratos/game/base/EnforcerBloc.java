/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package stratos.game.base ;
import stratos.game.civilian.*;
import stratos.game.common.* ;
import stratos.game.tactical.Patrolling;
import stratos.game.actors.* ;
import stratos.game.building.* ;
import stratos.graphics.common.* ;
import stratos.graphics.cutout.* ;
import stratos.graphics.widgets.* ;
import stratos.user.* ;
import stratos.util.* ;





public class EnforcerBloc extends Venue implements Economy {
  
  
  final public static ModelAsset MODEL = CutoutModel.fromImage(
    EnforcerBloc.class, "media/Buildings/physician/enforcer_bloc.png", 3, 3
  ) ;
  final static ImageAsset ICON = ImageAsset.fromImage(
    "media/GUI/Buttons/audit_office_button.gif", EnforcerBloc.class
  ) ;
  
  
  public EnforcerBloc(Base base) {
    super(3, 3, ENTRANCE_EAST, base) ;
    structure.setupStats(
      450, 2, 450,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    ) ;
    personnel.setShiftType(SHIFTS_BY_DAY) ;
    this.attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public EnforcerBloc(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  
  /**  Economic functions, upgrades and behaviour implementation-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    EnforcerBloc.class, "audit_office_upgrades"
  ) ;
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
  final public static Upgrade
    PRESS_OFFICE = new Upgrade(
      "Press Office",
      "Assists in the production of pressfeed and brings Advertisers into "+
      "your employ, helping to gather information and fortify base morale.",
      150, null, 1, null, ALL_UPGRADES
    )
  ;
  //  TODO:  Include others
  
  
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null;
    final Choice choice = new Choice(actor);
    
    //final int linkMod = (int) (5 * (1 - stocks.shortagePenalty(DATALINKS))) ;
    
    if (actor.vocation() == Backgrounds.AUDITOR) {
      final Venue toAudit = Audit.nextToAuditFor(actor);
      if (toAudit != null) {
        final Audit a = new Audit(actor, toAudit);
        //a.checkBonus = ((structure.upgradeLevel(FILE_SYSTEM) - 1) * 5) / 2 ;
        //a.checkBonus += linkMod ;
        choice.add(a);
      }
    }
    
    if (actor.vocation() == Backgrounds.ENFORCER) {
      choice.add(Patrolling.nextGuardPatrol(actor, this, Plan.ROUTINE));
    }
    
    return choice.weightedPick();
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v);
    if (v == Backgrounds.AUDITOR) return nO + 1;
    if (v == Backgrounds.ENFORCER) return nO + 2;
    return 0;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
  }
  
  
  public Background[] careers() {
    return new Background[] { Backgrounds.AUDITOR, Backgrounds.ENFORCER } ;
  }
  
  
  public Service[] services() {
    return new Service[] { SERVICE_ADMIN };
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Enforcer Bloc" ;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return Composite.withImage(ICON, "enforcer_bloc");
  }
  
  
  public String helpInfo() {
    return
      "The Enforcer Bloc provides a dedicated police force for your base, "+
      "specialising in interrogation and forensics as well as self defence "+
      "and nonlethal force.";
  }
  
  
  public String buildCategory() {
    return UIConstants.TYPE_PHYSICIAN;
  }
}



/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.game.base;
import stratos.game.common.*;
import stratos.game.actors.*;
import stratos.game.verse.*;
import stratos.start.*;
import stratos.user.*;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;



public class MissionClaiming extends Mission {
  
  
  /**  Data fields, constructors, and save/load methods-
    */
  final Sector claimed;
  final Expedition expedition;
  
  
  private MissionClaiming(Base base, Sector claimed) {
    super(base, claimed, null, "Claiming "+claimed);
    this.claimed    = claimed;
    this.expedition = new Expedition();
    
    setMissionType(Mission.TYPE_SCREENED  );
    assignPriority(Mission.PRIORITY_URGENT);
    expedition.setOrigin(base.world.localSector(), base.faction());
  }
  
  
  public MissionClaiming(Session s) throws Exception {
    super(s);
    this.claimed    = (Sector) subject;
    this.expedition = (Expedition) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(expedition);
  }
  
  
  
  /**  Strategic evaluation-
    */
  public static MissionClaiming claimFor(Object target, Base base) {
    if (target instanceof Sector) {
      
      //  We only permit claiming of currently uncolonised sectors:
      final Sector sector = (Sector) target;
      final SectorBase b = base.world.offworld.baseForSector(sector);
      final Faction owns = b == null ? null : b.faction();
      if (sector == base.world.localSector()) return null;
      if (owns != null && ! owns.primal()   ) return null;
      
      //  If the sector is 'virgin territory', we set the recruitment type and
      //  arrange a one-way trip:
      final MissionClaiming m = new MissionClaiming(base, sector);
      m.setJourney(Journey.configForMission(m, false));
      return m.journey() == null ? null : m;
    }
    return null;
  }
  
  
  public float targetValue(Base base) {
    return 0;
  }
  
  
  public float harmLevel() {
    return Plan.NO_HARM;
  }
  
  
  public boolean resolveMissionOffworld() {
    final Sector s = (Sector) subject;
    final SectorBase b = base.world.offworld.baseForSector(s);
    
    //  TODO:  Allow for the possibility of failure here, and ensure that
    //  proper tribute arrangements are set up!
    
    b.assignFaction(base.faction());
    b.assignRuler(expedition.leader());
    
    b.setPopulation(b.population() + 0.5f);
    return true;
  }
  
  
  private void beginNewMap(
    Stage oldWorld, Verse verse, Series <Actor> approved
  ) {
    
    //  Remove all references to the old world by the expedition members (so
    //  that it's not retained in memory.)
    for (Actor a : approved) {
      if (a.inWorld()) a.exitWorld();
      a.removeWorldReferences(oldWorld);
    }
    endMission(true);
    
    //  Then, we essentially 'reboot the world' in a different sector while
    //  retaining the old universe and expedition data.
    final String prefix = ((Scenario) PlayLoop.played()).savesPrefix();
    verse.setStartingDate((int) (journey().arriveTime() + 1));
    StartupScenario newGame = new StartupScenario(expedition, verse, prefix);
    newGame.skipLoading();
    PlayLoop.setupAndLoop(newGame);
  }
  
  
  
  /**  Regular life-cycle and updates:
    */
  public void beginMission() {
    super.beginMission();
    base.finance.incCredits(expedition.funding(), BaseFinance.SOURCE_LENDING);
  }
  
  
  public void updateMission() {
    final Actor leader = base.ruler();
    final List <Actor> approved = approved();
    final Stage world = base.world;
    final Verse verse = base.world.offworld;
    
    //  If the base's current leader isn't included as an applicant, add them,
    //  and refresh the list of applicants either way.
    if (leader != null && ! applicants().includes(leader)) {
      leader.mind.assignMission(this);
      setApprovalFor(leader, true);
      expedition.assignLeader(leader);
    }
    expedition.setApplicants(approved);
    
    //  In the event that the base's current governor is accepted as part of
    //  the expedition, then you need to start a fresh game on a new map...
    if (approved.includes(leader) && ! leader.inWorld()) {
      beginNewMap(world, verse, approved);
    }
    else super.updateMission();
  }
  
  
  
  /**  Assigning steps to actors-
    */
  protected boolean shouldEnd() {
    return false;
  }
  
  
  protected Behaviour createStepFor(Actor actor) {
    return null;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected ImageAsset iconForMission(HUD UI) {
    return Mission.CLAIMING_ICON;
  }
  
  
  protected Composite compositeForSubject(HUD UI) {
    return null;
    //return Composite.withImage(location.icon, "founding_"+location);
  }
  
  
  public void describeMission(Description d) {
    d.appendAll("Claiming ", subject);
  }
  
  
  public SelectionPane configSelectPane(SelectionPane panel, HUD UI) {
    if (panel == null) panel = new ExpeditionPane(UI, expedition, this);
    return super.configSelectPane(panel, UI);
  }
}














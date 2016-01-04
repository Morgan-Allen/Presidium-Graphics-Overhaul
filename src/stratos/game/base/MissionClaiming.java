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
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;




//  TODO:  Claims-missions, by default, are always screened.  (They cannot be
//  covert, and they cannot be purely military.)

public class MissionClaiming extends Mission {
  
  
  /**  Data fields, constructors, and save/load methods-
    */
  final Sector claimed;
  final Expedition expedition;
  
  
  private MissionClaiming(Base base, Sector claimed) {
    super(base, claimed, null, "Claiming "+claimed);
    this.claimed = claimed;
    this.expedition = new Expedition();
  }
  
  
  public MissionClaiming(Session s) throws Exception {
    super(s);
    this.claimed = (Sector) subject;
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
      final Sector sector = (Sector) target;
      final SectorBase b = base.world.offworld.baseForSector(sector);
      final Faction owns = b == null ? null : b.faction();
      if (sector == base.world.localSector()) return null;
      if (owns != null && ! owns.primal()   ) return null;
      
      final MissionClaiming m = new MissionClaiming(base, sector);
      m.setMissionType(Mission.TYPE_SCREENED);
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
  
  
  public void resolveMissionOffworld() {
    return;
  }
  
  
  public void updateMission() {
    final Actor leader = base.ruler();
    if (leader != null && ! applicants().includes(leader)) {
      leader.mind.assignMission(this);
      setApprovalFor(leader, true);
    }
    super.updateMission();
  }
  
  
  public void beginMission() {
    super.beginMission();
    //
    //  We include a special provision here in the case where the Base's ruler
    //  (i.e, the player-character) is part of the expedition.
    final Actor leader = base.ruler();
    final List <Actor> approved = approved();
    final Stage world = base.world;
    final Verse verse = base.world.offworld;
    
    if (approved.includes(leader)) {
      //
      //  Remove all references to the old world by the expedition members (so
      //  that it's not retained in memory.)
      for (Actor a : approved) a.removeWorldReferences(world);
      endMission(true);
      //
      //  Then, we essentially 'reboot the world' in a different sector while
      //  retaining the old universe and expedition data.
      final String prefix = ((Scenario) PlayLoop.played()).savesPrefix();
      verse.setStartingDate((int) (journey().arriveTime() + 1));
      StartupScenario newGame = new StartupScenario(expedition, verse, prefix);
      PlayLoop.setupAndLoop(newGame);
    }
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
}










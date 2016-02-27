/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.hooks;
import stratos.game.common.*;
import stratos.game.verse.*;
import stratos.start.DebugPlacing;
import stratos.start.PlayLoop;
import stratos.game.craft.*;
import stratos.game.maps.SiteUtils;
import stratos.game.maps.SitingPass;
import stratos.util.*;
import stratos.content.civic.*;
import static stratos.game.craft.Economy.*;
import stratos.user.*;
import stratos.user.notify.*;



public class ScenarioElysium extends SectorScenario {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final MessageScript script;
  
  private Base settlerBase;
  private Batch <Venue> settlerBuilt = new Batch();
  
  
  
  public ScenarioElysium() {
    super();
    this.script = new MessageScript(
      this, "src/stratos/content/hooks/ScriptElysium.xml"
    );
  }
  
  
  public ScenarioElysium(Session s) throws Exception {
    super(s);
    script = (MessageScript) s.loadObject();
    settlerBase = (Base) s.loadObject();
    s.loadObjects(settlerBuilt);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(script);
    s.saveObject(settlerBase);
    s.saveObjects(settlerBuilt);
  }
  
  
  
  /**  Script and setup methods-
    */
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    super.configureScenario(world, base, UI);
    
    Venue bastion = base.listInstalled(Bastion.BLUEPRINT, true).first();
    final Pick <StagePatch> pick = new Pick();
    for (StagePatch patch : world.patches.allGridPatches()) {
      Tile under = world.tileAt(patch);
      float rating = world.terrain().fertilitySample(under);
      rating *= 1 + (Spacing.zoneDistance(under, bastion) / 2);
      pick.compare(patch, rating);
    }
    Target settlePoint = pick.result();

    GameSettings.paveFree = true;
    
    Base settlerBase = Base.settlement(
      world, "Seilig's Landing", Faction.FACTION_CIVILISED
    );
    final Venue toPlace[] = {
      new BotanicalStation(settlerBase),
      new EcologistRedoubt(settlerBase),
      new Bastion         (settlerBase),
    };
    Batch <Venue> holdings = new Batch();
    float residents = 0;
    for (Venue v : toPlace) {
      SiteUtils.establishVenue(v, settlePoint, -1, true, world);
      if (! v.inWorld()) continue;
      settlerBase.setup.fillVacancies(v, true);
      residents += v.staff.workforce();
    }
    settlePoint = toPlace[2];
    final Venue morePlaced[] = {
      new EngineerStation (settlerBase),
      new RunnerMarket    (settlerBase),
      new SupplyDepot     (settlerBase),
    };
    for (Venue v : morePlaced) {
      SiteUtils.establishVenue(v, settlePoint, -1, true, world);
      if (! v.inWorld()) continue;
      settlerBase.setup.fillVacancies(v, true);
      residents += v.staff.workforce();
    }
    
    settlerBase.demands.impingeDemand(
      Economy.SERVICE_HOUSING, residents, -1, settlePoint
    );
    for (float n = residents / HoldingUpgrades.OCCUPANCIES[0]; n-- > 0;) {
      Holding h = new Holding(settlerBase);
      SiteUtils.establishVenue(h, settlePoint, -1, true, world);
      if (! h.inWorld()) continue;
      holdings.add(h);
    }
    for (Venue v : holdings) {
      v.stocks.bumpItem(PARTS   , 5);
      v.stocks.bumpItem(PLASTICS, 5);
      v.stocks.bumpItem(CARBS   , 5);
      v.stocks.bumpItem(PROTEIN , 5);
      v.structure.addUpgrade(Holding.FREEBORN_LEVEL);
    }
    
    GameSettings.paveFree = false;
    
    Visit.appendTo(settlerBuilt, toPlace   );
    Visit.appendTo(settlerBuilt, morePlaced);
    Visit.appendTo(settlerBuilt, holdings  );
  }
  
  
  
  protected boolean checkShowIntro() {
    return true;
  }


  protected boolean checkScenarioSuccess() {
    if (settlerBase == null              ) return false;
    if (settlerBase.allUnits().size() > 0) return false;
    if (world().presences.mapFor(settlerBase).population() > 0) return false;
    return true;
  }
  
  
  
  
  /**  Update methods for when off-stage:
    */
  public void updateOffstage() {
  }
  
  
  
  /**  Rendering, debug and interface methods-
    */
  public void describeHook(Description d) {
    final String summary = script.contentForTopic("Summary");
    d.append(summary);
  }
  
}









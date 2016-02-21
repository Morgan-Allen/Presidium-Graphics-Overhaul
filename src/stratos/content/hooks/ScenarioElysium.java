/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.hooks;
import stratos.game.common.*;
import stratos.game.verse.*;
import stratos.game.craft.*;
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
  
  
  
  public ScenarioElysium(Verse verse) {
    super(StratosSetting.SECTOR_ELYSIUM, verse);
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
    
    settlerBase = Base.settlement(
      world, "Seilig's Landing", Faction.FACTION_CIVILISED
    );
    //
    //  Include some housing, a runner market, an ecologist redoubt, an
    //  engineer station, a botanical station and a supply depot with basic
    //  import/export options.  Some basic supplies and enough holdings to
    //  accomodate the population.
    
    //  TODO:  You'll want to select a good location for this... consider
    //  placing a Bastion first?
    
    final Venue toPlace[] = {
      new EngineerStation (settlerBase),
      new RunnerMarket    (settlerBase),
      new EcologistRedoubt(settlerBase),
      new BotanicalStation(settlerBase),
      new SupplyDepot     (settlerBase),
      new Condensor       (settlerBase)
    };
    settlerBase.setup.doPlacementsFor(toPlace);
    
    Batch <Venue> holdings;
    holdings = settlerBase.setup.doFullPlacements(Holding.BLUEPRINT);
    for (Venue v : holdings) {
      v.stocks.bumpItem(PARTS   , 5);
      v.stocks.bumpItem(PLASTICS, 5);
      v.stocks.bumpItem(CARBS   , 5);
      v.stocks.bumpItem(PROTEIN , 5);
      v.structure.addUpgrade(Holding.FREEBORN_LEVEL);
    }

    Visit.appendTo(settlerBuilt, toPlace );
    Visit.appendTo(settlerBuilt, holdings);
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









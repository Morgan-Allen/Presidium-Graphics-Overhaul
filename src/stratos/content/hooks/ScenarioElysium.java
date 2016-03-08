/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.content.hooks;
import stratos.game.common.*;
import stratos.game.verse.*;
import stratos.game.wild.Hareen;
import stratos.game.wild.NativeHut;
import stratos.game.wild.NestUtils;
import stratos.game.wild.Qudu;
import stratos.game.craft.*;
import stratos.game.maps.*;
import stratos.util.*;
import stratos.content.civic.*;
import static stratos.game.craft.Economy.*;
import stratos.user.*;
import stratos.user.notify.*;



public class ScenarioElysium extends SectorScenario {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  private Base settlerBase;
  private Batch <Venue> settlerBuilt = new Batch();
  
  
  
  public ScenarioElysium() {
    super("src/stratos/content/hooks/ScriptElysium.xml");
  }
  
  
  public ScenarioElysium(Session s) throws Exception {
    super(s);
    settlerBase = (Base) s.loadObject();
    s.loadObjects(settlerBuilt);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(settlerBase);
    s.saveObjects(settlerBuilt);
  }
  
  
  
  /**  Script and setup methods-
    */
  protected void configureScenario(Stage world, Base base, BaseUI UI) {
    super.configureScenario(world, base, UI);
  }
  
  
  protected void establishLocals(Stage world) {
    
    final int tribeID = NativeHut.TRIBE_FOREST;
    final Base natives = Base.natives(world, tribeID);
    final Batch <Venue> nativeHuts = new Batch();
    
    for (int n = 3; n-- > 0;) {
      nativeHuts.add(NativeHut.newHall(tribeID, natives));
    }
    for (int n = 9; n-- > 0;) {
      nativeHuts.add(NativeHut.newHut (tribeID, natives));
    }
    natives.setup.doPlacementsFor(nativeHuts.toArray(Venue.class));
    natives.setup.fillVacancies(nativeHuts, true);
    
    NestUtils.populateFauna(world, 0.5f, Qudu.SPECIES, Hareen.SPECIES);
  }
  
  
  protected boolean checkShowIntro() {
    return true;
  }
  
  
  protected boolean checkSettlersDestroyed() {
    return false;
  }
  
  
  protected boolean checkSettlersConverted() {
    return false;
  }
  
  
  protected boolean checkProsperity() {
    
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
    final String summary = script().contentForTopic("Summary");
    d.append(summary);
  }
  
}









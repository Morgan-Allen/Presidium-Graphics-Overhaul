

package stratos.start;
import stratos.content.hooks.*;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.verse.*;
import stratos.util.*;



public class DebugScenario extends ScenarioElysium {
  
  
  final static String PREFIX = "debug_scenario";
  
  
  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new DebugScenario());
  }
  
  
  public DebugScenario() {
    super();
    setSavesPrefix(PREFIX);
  }
  
  
  public DebugScenario(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  protected void initScenario(String prefix) {
    if (prefix == null || expedition() == null || verse() == null) {
      Verse verse = new StratosSetting();
      
      final Faction backing = StratosSetting.PLANET_HALIBAN.startingOwner;
      final Expedition e = new Expedition().configFrom(
        StratosSetting.PLANET_HALIBAN,
        StratosSetting.SECTOR_ELYSIUM,
        backing, Expedition.TITLE_COUNT, 5000, 0, new Batch()
      );
      e.assignLeader(Backgrounds.KNIGHTED.sampleFor(backing));
      setupScenario(e, verse, PREFIX);
    }
    super.initScenario(savesPrefix());
  }
}






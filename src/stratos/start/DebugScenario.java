

package stratos.start;
import stratos.content.hooks.*;
import stratos.game.actors.*;
import stratos.game.common.*;
import stratos.game.verse.*;
import stratos.util.*;



public class DebugScenario extends ScenarioPavonis {
  
  
  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new DebugScenario());
  }
  
  
  public DebugScenario() {
    super();
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
      
      final Faction backing = StratosSetting.PLANET_PAREM_V.startingOwner;
      final Expedition e = new Expedition().configFrom(
        StratosSetting.PLANET_PAREM_V,
        StratosSetting.SECTOR_PAVONIS,
        backing, Expedition.TITLE_COUNT, 2000, 0, new Batch()
      );
      e.assignLeader(Backgrounds.KNIGHTED.sampleFor(backing));
      
      setupScenario(e, verse, "debug_scenario");
    }
    super.initScenario(savesPrefix());
  }
  
}




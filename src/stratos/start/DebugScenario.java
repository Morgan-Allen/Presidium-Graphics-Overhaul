

package stratos.start;
import static stratos.start.SaveUtils.latestSave;
import static stratos.start.SaveUtils.loadGame;

import stratos.content.hooks.*;
import stratos.game.actors.Backgrounds;
import stratos.game.verse.*;
import stratos.graphics.common.Rendering;
import stratos.graphics.widgets.HUD;
import stratos.util.Batch;
import stratos.util.I;



public class DebugScenario implements Playable {
  
  
  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new DebugScenario());
  }
  
  
  public void beginGameSetup() {
    final String prefix = "debug_scenario";
    final String savePath = prefix == null ? null : latestSave(prefix);
    
    if (SaveUtils.saveExists(savePath)) {
      I.say("\n\nLoading scenario from save file...");
      loadGame(savePath, false);
      return;
    }
    
    final Verse          verse = new StratosSetting();
    final SectorScenario scene = new ScenarioElysium(verse);
    
    final Faction backing = StratosSetting.PLANET_PAREM_V.startingOwner;
    final Expedition e = new Expedition().configFrom(
      StratosSetting.PLANET_PAREM_V, StratosSetting.SECTOR_TERRA, backing,
      Expedition.TITLE_COUNT, 2000, 0, new Batch()
    );
    e.assignLeader(Backgrounds.KNIGHTED.sampleFor(backing));
    
    scene.beginScenario(e, prefix);
  }
  
  
  public void updateGameState() {
  }
  
  
  public void renderVisuals(Rendering rendering) {
  }
  
  
  public HUD UI() {
    return null;
  }
  
  
  public boolean isLoading() {
    return false;
  }
  
  
  public float loadProgress() {
    return 0;
  }
  
  
  public boolean shouldExitLoop() {
    return false;
  }
  
  
  public boolean wipeAssetsOnExit() {
    return false;
  }
}

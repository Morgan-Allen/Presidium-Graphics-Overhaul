


package src.start;
import src.graphics.common.*;
import src.graphics.widgets.*;



public interface Playable {
  
  void beginGameSetup();
  void updateGameState();
  void renderVisuals(Rendering rendering);
  HUD UI();
  
  boolean isLoading();
  float loadProgress();
  boolean shouldExitLoop();
}

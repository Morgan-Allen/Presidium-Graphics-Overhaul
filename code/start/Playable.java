


package code.start;
import code.graphics.common.*;
import code.graphics.widgets.*;



public interface Playable {
  
  void beginGameSetup();
  void updateGameState();
  void renderVisuals(Rendering rendering);
  HUD UI();
  
  boolean isLoading();
  float loadProgress();
  boolean shouldExitLoop();
}

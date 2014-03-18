


package stratos.start;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;



public interface Playable {
  
  void beginGameSetup();
  void updateGameState();
  void renderVisuals(Rendering rendering);
  HUD UI();
  
  boolean isLoading();
  float loadProgress();
  boolean shouldExitLoop();
}

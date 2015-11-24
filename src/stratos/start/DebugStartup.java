/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.start;
import stratos.graphics.common.Rendering;
import stratos.graphics.widgets.HUD;
import stratos.user.*;
import stratos.user.mainmenu.*;



public class DebugStartup {
  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new Playable() {
      
      private HUD UI;
      private boolean loading = false, loaded = false;
      
      public void beginGameSetup() {
        loading = true;
        final HUD UI = new HUD(PlayLoop.rendering());
        final MainMenuNew mainMenu = new MainMenuNew(UI);
        mainMenu.alignToFill();
        mainMenu.attachTo(UI);
        this.UI = UI;
        loaded = true;
        loading = false;
      }
      
      public HUD UI() {
        return UI;
      }
      
      public boolean isLoading() {
        return loading;
      }
      
      public float loadProgress() {
        return loaded ? 1 : 0;
      }
      
      public boolean shouldExitLoop() {
        return false;
      }

      
      public void updateGameState() {}
      public void renderVisuals(Rendering rendering) {}
    });
  }
}







package code.start;
import code.graphics.common.Rendering;
import code.graphics.widgets.HUD;
import code.user.*;
//import src.game.campaign.*;



public class DebugStartup {
  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new Playable() {
      
      private HUD UI;
      //MainMenu menu;
      private boolean loading = false, loaded = false;
      
      
      public void beginGameSetup() {
        loading = true;
        final HUD UI = new HUD();
        final MainMenu mainMenu = new MainMenu(UI);
        mainMenu.relBound.set(0.5f, 0, 0, 1);
        mainMenu.absBound.set(-200, 0, 400, 0);
        mainMenu.attachTo(UI);
        //this.menu = mainMenu;
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




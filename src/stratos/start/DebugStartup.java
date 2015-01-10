


package stratos.start;
import stratos.graphics.common.Rendering;
import stratos.graphics.widgets.HUD;
import stratos.user.*;




public class DebugStartup {
  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new Playable() {
      
      private HUD UI;
      private boolean loading = false, loaded = false;
      
      public void beginGameSetup() {
        loading = true;
        final HUD UI = new HUD(PlayLoop.rendering());
        final MainMenu mainMenu = new MainMenu(UI);
        mainMenu.alignHorizontal(0.5f, 400, 0);
        mainMenu.alignVertical  (50  , 50    );
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




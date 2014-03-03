

package src.start;
import src.graphics.common.*;
import src.graphics.cutout.*;
import src.graphics.solids.*;
import src.graphics.widgets.*;
import src.util.*;



public class DebugGraphics {
  
  
  //  TODO:  This will have to be thought out again.
  /*
  final static CutoutModel
    CM = CutoutModel.fromImage(
      "buildings/bastion_L1.png",
      DebugGraphics.class, 7, 5
    );
  final static MS3DModel
    SM = MS3DModel.loadFrom(
      "models/", "Micovore.ms3d",
      DebugGraphics.class, "FaunaModels.xml", "Micovore"
    );
  
  
  public static void main(String args[]) {
    //  Okay.  You'll want to create a piece of terrain, a mobile .ms3d sprite,
    //  a smattering of trees, and so on.  Then test out the rendering thereof.
    PlayLoop.setupAndLoop(new Playable() {
      
      private boolean loaded = false;
      List <Sprite> sprites = new List <Sprite> ();
      
      public void beginGameSetup() {
        sprites.add(CM.makeSprite());
        sprites.add(SM.makeSprite());
        loaded = true;
      }
      
      
      public HUD UI() {
        return null;
      }
      
      
      public boolean isLoading() {
        return loaded;
      }
      
      
      public float loadProgress() {
        return loaded ? 1 : 0;
      }
      
      
      public boolean shouldExitLoop() {
        return false;
      }
      
      
      public void updateGameState() {
      }
      
      
      public void renderVisuals(Rendering rendering) {
        for (Sprite sprite : sprites) {
          sprite.registerFor(rendering);
        }
      }
    });
  }
  //*/
}






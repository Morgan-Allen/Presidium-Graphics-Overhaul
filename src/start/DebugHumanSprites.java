

package src.start;
import src.graphics.common.*;
import src.graphics.solids.*;



public class DebugHumanSprites {
  
  final static MS3DModel
    HM = MS3DModel.loadFrom(
      "media/Actors/human/", "female_final.ms3d",
      DebugHumanSprites.class, "HumanModels.xml", "FemalePrime"
    );
  
  
  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new VisualDebug() {
      protected void loadVisuals() {
        sprites.add(HM.makeSprite());
        PlayLoop.rendering().view.zoomLevel = 5.0f;
      }
      protected void onRendering(Sprite sprite) {
        sprite.setAnimation(AnimNames.MOVE, Rendering.activeTime() % 1);
      }
    });
  }
}




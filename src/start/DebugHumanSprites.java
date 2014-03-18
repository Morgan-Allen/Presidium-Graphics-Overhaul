

package src.start;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

import src.graphics.common.*;
import src.graphics.solids.*;
import src.util.Rand;
import src.util.Visit;



public class DebugHumanSprites {
  
  final static String DIR = "media/Actors/human/";
  final static Class C = DebugHumanSprites.class;
  final static MS3DModel
    HM = MS3DModel.loadFrom(
      DIR, "female_final.ms3d", C, "HumanModels.xml", "FemalePrime"
    );
  final static ImageAsset
    SKIN_A = ImageAsset.fromImage(DIR+"desert_blood.gif"  , C),
    SKIN_B = ImageAsset.fromImage(DIR+"ecologist_skin.gif", C);
  
  
  
  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new VisualDebug() {
      
      private String animName = AnimNames.MOVE;
      private boolean press = false;
      private Sprite first = null;
      
      
      protected void loadVisuals() {
        
        final SolidSprite spriteA = (SolidSprite) HM.makeSprite();
        spriteA.showOnly(AnimNames.MAIN_BODY);
        spriteA.setOverlaySkins(
          AnimNames.MAIN_BODY,
          SKIN_A.asTexture(),
          SKIN_B.asTexture()
        );
        sprites.add(spriteA);
        first = spriteA;

        final SolidSprite spriteB = (SolidSprite) HM.makeSprite();
        spriteB.showOnly(AnimNames.MAIN_BODY);
        spriteB.position.set(0, 1, 0);
        sprites.add(spriteB);
        
        PlayLoop.rendering().view.zoomLevel = 5.0f;
      }
      
      
      protected void onRendering(Sprite sprite) {
        if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
          if (press) return;
          press = true;
          
          final String old = animName;
          final String names[] = new String[] {
            AnimNames.MOVE,
            AnimNames.FIRE,
            AnimNames.TALK_LONG,
            AnimNames.LOOK,
            AnimNames.STRIKE,
            AnimNames.MOVE_FAST,
            AnimNames.TALK,
            AnimNames.EVADE
          };
          animName = (String) Rand.pickFrom(names);
          if (animName == old) {
            final int index = Visit.indexOf(animName, names);
            animName = names[(index + 1) % names.length];
          }
        }
        else press = false;
        
        final float time = Rendering.activeTime();
        if (sprite == first) {
          sprite.setAnimation(animName, time % 1);
        }
        else {
          sprite.setAnimation(AnimNames.MOVE, (time + 0.5f) % 1);
        }
      }
    });
  }
}








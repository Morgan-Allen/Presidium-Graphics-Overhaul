

package src.start;
import org.apache.commons.math3.util.FastMath;

import src.game.building.DeviceType;
import src.graphics.common.*;
import src.graphics.cutout.*;
import src.graphics.solids.*;
import src.graphics.sfx.*;
import src.graphics.widgets.*;
import src.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;



public class DebugHumanSprites {
  
  final static MS3DModel
    HM = MS3DModel.loadFrom(
      "media/Actors/human/", "female_final.ms3d",
      DebugHumanSprites.class, "HumanModels.xml", "FemalePrime"
    );
  
  
  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new Playable() {
      
      private boolean loaded = false;
      List <Sprite> sprites = new List <Sprite> ();
      
      private boolean moused = false ;
      private float origX, origY, origR, origE ;
      
      
      public void beginGameSetup() {
        final Sprite HS = HM.makeSprite();
        sprites.add(HS);
        loaded = true;
        
        PlayLoop.rendering().view.zoomLevel = 3.5f;
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
        
        final Viewport port = rendering.view ;
        if (Gdx.input.isKeyPressed(Keys.UP)) {
          port.lookedAt.x-- ;
          port.lookedAt.y++ ;
        }
        if (Gdx.input.isKeyPressed(Keys.DOWN)) {
          port.lookedAt.x++ ;
          port.lookedAt.y-- ;
        }
        if (Gdx.input.isKeyPressed(Keys.RIGHT)) {
          port.lookedAt.x++ ;
          port.lookedAt.y++ ;
        }
        if (Gdx.input.isKeyPressed(Keys.LEFT)) {
          port.lookedAt.x-- ;
          port.lookedAt.y-- ;
        }
        
        final int MX = Gdx.input.getX(), MY = Gdx.input.getY();
        if (Gdx.input.isButtonPressed(Buttons.LEFT)) {
          if (! moused) {
            moused = true ;
            origX = MX ;
            origY = MY ;
            origR = port.rotation  ;
            origE = port.elevation ;
          }
          else {
            port.rotation  = origR + ((origX - MX) / 2);
            port.elevation = origE + ((MY - origY) / 2);
          }
        }
        else moused = false ;
        
        for (Sprite sprite : sprites) {
          sprite.update();
          sprite.registerFor(rendering);
          sprite.setAnimation(AnimNames.MOVE, Rendering.activeTime() % 1);
        }
      }
    });
  }
}









package src.start;
import src.graphics.common.*;
import src.graphics.cutout.*;
import src.graphics.solids.*;
import src.graphics.widgets.*;
import src.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;



public class DebugGraphics {
  
  
  final static CutoutModel
    CM = CutoutModel.fromImage(
      "media/Buildings/military/bastion.png",
      DebugGraphics.class, 7, 5
    );
  final static MS3DModel
    SM = MS3DModel.loadFrom(
      "media/Actors/fauna/", "Micovore.ms3d",
      DebugGraphics.class, "FaunaModels.xml", "Micovore"
    );
  
  
  public static void main(String args[]) {
    PlayLoop.setupAndLoop(new Playable() {
      
      private boolean loaded = false;
      List <Sprite> sprites = new List <Sprite> ();
      
      private boolean moused = false ;
      private float origX, origY, origR, origE ;
      
      
      public void beginGameSetup() {
        final Sprite SS = SM.makeSprite();
        sprites.add(SS);
        
        for (int i = 10 ; i-- > 0;) {
          final Sprite CS = CM.makeSprite();
          CS.position.set(i, -i, 0);
          CS.fog = (i + 1) / 10f;
          CS.colour = Colour.transparency(CS.fog);
          CS.scale = 0.5f;
          sprites.add(CS);
        }
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
          sprite.registerFor(rendering);
          final float f = sprite.fog, a = f * (1 - f) * 4;
          sprite.colour = Colour.transparency(a);
          sprite.fog = (f + 0.01f) % 1;
          sprite.rotation += 90 / 60f;
          
          sprite.setAnimation(AnimNames.MOVE, sprite.fog);
        }
      }
    });
  }
}







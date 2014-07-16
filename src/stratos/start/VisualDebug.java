

package stratos.start;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.*;



public abstract class VisualDebug implements Playable {

  
  private boolean loaded = false;
  List <Sprite> sprites = new List <Sprite> ();
  
  private boolean moused = false;
  private float origX, origY, origR, origE;

  
  
  public void beginGameSetup() {
    loadVisuals();
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
    
    final Viewport port = rendering.view;
    final float i = 0.1f;
    if (Gdx.input.isKeyPressed(Keys.UP   )) {
      port.lookedAt.x -= i;
      port.lookedAt.y += i;
    }
    if (Gdx.input.isKeyPressed(Keys.DOWN )) {
      port.lookedAt.x += i;
      port.lookedAt.y -= i;
    }
    if (Gdx.input.isKeyPressed(Keys.RIGHT)) {
      port.lookedAt.x -= i;
      port.lookedAt.y -= i;
    }
    if (Gdx.input.isKeyPressed(Keys.LEFT )) {
      port.lookedAt.x += i;
      port.lookedAt.y += i;
    }
    
    final int MX = Gdx.input.getX(), MY = Gdx.input.getY();
    if (Gdx.input.isButtonPressed(Buttons.LEFT)) {
      if (! moused) {
        moused = true;
        origX = MX;
        origY = MY;
        origR = port.rotation ;
        origE = port.elevation;
      }
      else {
        port.rotation  = origR + ((origX - MX) / 2);
        port.elevation = origE + ((MY - origY) / 2);
      }
    }
    else moused = false;
    
    for (Sprite sprite : sprites) {
      sprite.readyFor(rendering);
      onRendering(sprite);
    }
  }
  
  
  protected abstract void loadVisuals();
  protected abstract void onRendering(Sprite sprite);
}







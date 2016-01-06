

package stratos.start;
import stratos.graphics.common.*;
import stratos.graphics.widgets.*;
import stratos.util.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.*;



public abstract class VisualDebug implements Playable {

  
  private boolean loaded = false;
  List <Sprite> sprites = new List <Sprite> ();
  
  private boolean moused   = false;
  private boolean moveMode = false;
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
  
  
  public boolean wipeAssetsOnExit() {
    return true;
  }
  
  
  public void updateGameState() {
  }
  
  
  protected boolean inMoveMode() {
    return moveMode;
  }
  
  
  protected void toggleMoveMode() {
    moveMode = ! moveMode;
  }
  
  
  public void renderVisuals(Rendering rendering) {
    
    final Viewport port = rendering.view;
    final float i = 0.1f;
    final Mat3D r = new Mat3D();
    final Vec3D l = rendering.lighting.direction;
    
    if (KeyInput.wasTyped(Keys.ENTER)) {
      toggleMoveMode();
    }
    
    if (inMoveMode()) {
      if (Gdx.input.isKeyPressed(Keys.S)) {
        port.lookedAt.z += i;
      }
      if (Gdx.input.isKeyPressed(Keys.W)) {
        port.lookedAt.z -= i;
      }
      
      Vec3D across = rendering.view.screenHorizontal().normalise();
      
      if (Gdx.input.isKeyPressed(Keys.A)) {
        port.lookedAt.add(across.scale( i));
      }
      if (Gdx.input.isKeyPressed(Keys.D)) {
        port.lookedAt.add(across.scale(-i));
      }
    }
    
    if (Gdx.input.isKeyPressed(Keys.UP   )) {
      port.zoomLevel *= 1 + i;
    }
    if (Gdx.input.isKeyPressed(Keys.DOWN )) {
      port.zoomLevel /= 1 + i;
    }
    if (Gdx.input.isKeyPressed(Keys.RIGHT)) {
      r.setIdentity().rotateY(i *  1);
      r.trans(l);
      l.normalise();
    }
    if (Gdx.input.isKeyPressed(Keys.LEFT )) {
      r.setIdentity().rotateY(i * -1);
      r.trans(l);
      l.normalise();
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







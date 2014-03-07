

package src.graphics.terrain;
import src.graphics.common.Rendering;
import src.util.*;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Pixmap.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

import static com.badlogic.gdx.graphics.Texture.TextureFilter.*;



//
//  TODO:  FOG OVERLAYS AND TERRAIN SETS MUST BE DISPOSED OF MANUALLY

public class FogOverlay {
  
  
  final int size;
  
  private float oldVals[][], newVals[][];
  private Pixmap drawnTo;
  protected Texture oldTex, newTex;
  private float oldTime = 0;
  
  
  public FogOverlay(int size) {
    this.size = size;
    
    drawnTo = new Pixmap(size, size, Format.RGBA4444);
    Pixmap.setBlending(Blending.None);
    drawnTo.setColor(Color.BLACK);
    drawnTo.fill();
    oldVals = newVals = new float[size][size];

    oldTex = new Texture(drawnTo);
    oldTex.setFilter(Linear, Linear);
    newTex = new Texture(drawnTo);
    newTex.setFilter(Linear, Linear);
  }
  
  
  void dispose() {
    drawnTo.dispose();
    oldTex.dispose();
    newTex.dispose();
  }
  
  
  protected void applyToShader(ShaderProgram shader) {
    oldTex.bind(1);
    newTex.bind(2);
    shader.setUniformi("u_fog_old", 1);
    shader.setUniformi("u_fog_new", 2);
    shader.setUniformf("u_fogSize", size, size);
    shader.setUniformf("u_fogTime", oldTime % 1);
  }
  
  
  protected void checkBufferSwap(float newTime) {
    if (((int) oldTime) != ((int) newTime)) {
      final Texture temp = newTex;
      newTex = oldTex;
      oldTex = temp;
      newTex.draw(drawnTo, 0, 0);
    }
    oldTime = newTime;
  }
  
  
  public void registerFor(Rendering rendering) {
    rendering.terrainPass.applyFog(this);
  }
  
  
  //  TODO:  Hopefully, this should be reasonably fast.  ...ish.  See if it
  //  can't be improved on, though.
  public void assignNewVals(float newVals[][]) {
    oldVals = this.newVals;
    this.newVals = newVals;
    
    Pixmap.setBlending(Blending.None);
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      final float fog = newVals[c.x][c.y];
      drawnTo.setColor(fog, fog, fog, 1);
      drawnTo.drawPixel(c.x, c.y);
    }
  }
  
  
  public float sampleAt(int x, int y) {
    final float
      oldVal = oldVals[x][y],
      newVal = newVals[x][y];
    final float time = oldTime % 1;
    return (oldVal * (1 - time)) + (time * newVal);
  }
}





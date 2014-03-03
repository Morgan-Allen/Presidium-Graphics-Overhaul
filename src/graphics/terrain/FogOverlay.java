

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
  private Pixmap drawnTo;
  protected Texture oldTex, newTex;
  private float oldTime = 0;
  
  
  public FogOverlay(int size) {
    this.size = size;
    
    drawnTo = new Pixmap(size, size, Format.RGBA4444);
    Pixmap.setBlending(Blending.None);
    drawnTo.setColor(Color.BLACK);
    drawnTo.fill();

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
      //drawnTo.setColor(0, 0, 0, 0.25f);
      Pixmap.setBlending(Blending.SourceOver);
      //drawnTo.fillRectangle(0, 0, terrain.size, terrain.size);
    }
    oldTime = newTime;
  }
  
  
  //  Hopefully, this should be reasonably fast.  ...ish.
  
  
  public void registerFor(Rendering rendering) {
    rendering.terrainPass.applyFog(this);
  }
  
  
  public void assignNewVals(float newVals[][]) {
    Pixmap.setBlending(Blending.SourceOver);
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      drawnTo.setColor(1, 1, 1, newVals[c.x][c.y]);
      drawnTo.drawPixel(c.x, c.y);
    }
  }
  
  
  public float sampleAt(float x, float y) {
    return (colorValue(x, y) & 0xff) / 255f;
  }


  public int colorValue(float x, float y) {
    return drawnTo.getPixel((int) x, (int) y);
  }
}



/*
public void liftAround(int x, int y, int radius) {
  Pixmap.setBlending(Blending.SourceOver);
  for (Coord c : Visit
      .grid(x - radius, y - radius, radius * 2, radius * 2, 1)) {
    final float xd = c.x - x, yd = c.y - y, dist = (float) Math
        .sqrt((xd * xd) + (yd * yd));
    if (dist > radius)
      continue;
    drawnTo.setColor(1, 1, 1, 1 - (dist / radius));
    drawnTo.drawPixel(c.x, c.y);
  }
}


public float sampleAt(float x, float y) {
  return (colorValue(x, y) & 0xff) / 255f;
}


public int colorValue(float x, float y) {
  return drawnTo.getPixel((int) x, (int) y);
}
//*/





package src.graphics.terrain;
import src.graphics.common.*;
import src.util.*;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Pixmap.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

import static com.badlogic.gdx.graphics.Texture.TextureFilter.*;



//
//  TODO:  FOG OVERLAYS AND TERRAIN SETS MUST BE DISPOSED OF MANUALLY
//  TODO:  ...THAT!!!

public class FogOverlay {
  
  
  private static boolean verbose = false;
  
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
    oldVals = new float[size][size];
    newVals = new float[size][size];
    
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
  
  
  public void registerFor(Rendering rendering) {
    rendering.terrainPass.applyFog(this);
  }
  
  
  public void updateVals(float newTime, float updateVals[][]) {
    if (((int) oldTime) == ((int) newTime)) {
      if (verbose) I.say("OLD/NEW TIME: "+oldTime+"/"+newTime);
      oldTime = newTime;
      return;
    }
    final float tempV[][] = oldVals;
    oldVals = newVals;
    newVals = tempV;
    
    //  TODO:  You can use Pixmap.getPixels() to access the byte buffer
    //         directly!  Employ that!
    Pixmap.setBlending(Blending.None);
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      final float fog = updateVals[c.x][c.y];
      newVals[c.x][c.y] = fog;
      drawnTo.setColor(fog, fog, fog, 1);
      drawnTo.drawPixel(c.x, c.y);
    }
    
    final Texture tempT = newTex;
    newTex = oldTex;
    oldTex = tempT;
    newTex.draw(drawnTo, 0, 0);
    
    if (verbose) I.say("PERFORMED FOG REFRESH, TIMES: "+oldTime+"/"+newTime);
    oldTime = newTime;
  }
  
  
  public float sampleAt(int x, int y, Object client) {
    final float
      oldVal = oldVals[x][y],
      newVal = newVals[x][y];
    final float time = oldTime % 1;
    final float sample = (oldVal * (1 - time)) + (time * newVal);
    if (verbose && client != null && I.talkAbout == client) {
      I.say("  Client time is: "+time+", sample: "+sample);
    }
    return sample;
  }
}









package src.graphics.terrain;
import src.graphics.common.*;
import src.util.*;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Pixmap.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import static com.badlogic.gdx.graphics.Texture.TextureFilter.*;
import java.nio.*;



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
    
    drawnTo = new Pixmap(size, size, Format.RGBA8888);
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
    
    Pixmap.setBlending(Blending.None);
    final ByteBuffer pixels = drawnTo.getPixels();
    final byte rawData[] = new byte[pixels.capacity()];
    final int lineLen = drawnTo.getWidth() * 4;
    
    for (int i = 0 ; i < rawData.length ;) {
      final int
        x = (i % lineLen) / 4,
        y = i / lineLen;
      final float fog = updateVals[x][y];
      newVals[x][y] = fog;
      
      final byte fogByte = (byte) (((int) (fog * 255)) & 0xff);
      rawData[i++] = fogByte;
      rawData[i++] = fogByte;
      rawData[i++] = fogByte;
      rawData[i++] = (byte) 0xff;
    }
    pixels.rewind();
    pixels.put(rawData);
    pixels.rewind();
    
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







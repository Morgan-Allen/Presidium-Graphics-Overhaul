/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.terrain;
import stratos.graphics.common.*;
import stratos.util.*;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Pixmap.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import static com.badlogic.gdx.graphics.Texture.TextureFilter.*;
import java.nio.*;




//  TODO:  FOG OVERLAYS AND TERRAIN SETS MUST BE DISPOSED OF MANUALLY

public class FogOverlay {
  
  
  private static boolean verbose = false;
  
  final int sizeX, sizeY;
  
  private float oldVals[][], newVals[][];
  private Pixmap drawnTo;
  
  private Texture oldTex, newTex;
  private boolean doSwap = false;
  private float oldTime = 0;
  
  
  public FogOverlay(int sizeX, int sizeY) {
    this.sizeX = sizeX;
    this.sizeY = sizeY;
    
    drawnTo = new Pixmap(sizeX, sizeY, Format.RGBA8888);
    Pixmap.setBlending(Blending.None);
    drawnTo.setColor(Color.BLACK);
    drawnTo.fill();
    oldVals = new float[sizeX][sizeY];
    newVals = new float[sizeX][sizeY];
  }
  
  
  void dispose() {
    drawnTo.dispose();
    oldTex.dispose();
    newTex.dispose();
  }
  
  
  private void updateTex() {
    if (oldTex == null || newTex == null) {
      oldTex = new Texture(drawnTo);
      oldTex.setFilter(Linear, Linear);
      newTex = new Texture(drawnTo);
      newTex.setFilter(Linear, Linear);
    }
    
    if (doSwap) {
      final Texture tempT = newTex;
      newTex = oldTex;
      oldTex = tempT;
      newTex.draw(drawnTo, 0, 0);
      doSwap = false;
    }
  }
  
  
  protected void applyToTerrain(ShaderProgram shader) {
    updateTex();
    oldTex.bind(1);
    newTex.bind(2);
    shader.setUniformi("u_fog_old", 1);
    shader.setUniformi("u_fog_new", 2);
    shader.setUniformf("u_fogSize", sizeX, sizeY);
    shader.setUniformf("u_fogTime", oldTime % 1);
  }
  
  
  protected void applyToMinimap(ShaderProgram shader) {
    updateTex();
    oldTex.bind(1);
    newTex.bind(2);
    shader.setUniformi("u_fog_old", 1);
    shader.setUniformi("u_fog_new", 2);
    shader.setUniformf("u_fogTime", oldTime % 1);
  }
  
  
  public void registerFor(Rendering rendering) {
    updateTex();
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
    
    for (int i = 0; i < rawData.length;) {
      final int
        x = (i % lineLen) / 4,
        y = i / lineLen;
      final float fog = Nums.clamp(updateVals[x][y], 0, 1);
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
    
    if (verbose) I.say("PERFORMED FOG REFRESH, TIMES: "+oldTime+"/"+newTime);
    oldTime = newTime;
    doSwap = true;
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
  
  
  public float sampleAt(float x, float y, Object client) {
    final float
      oldVal = Nums.sampleMap(sizeX, sizeY, oldVals, x, y),
      newVal = Nums.sampleMap(sizeX, sizeY, newVals, x, y);
    final float time = oldTime % 1;
    final float sample = (oldVal * (1 - time)) + (time * newVal);
    if (verbose && client != null && I.talkAbout == client) {
      I.say("  Client time is: "+time+", sample: "+sample);
    }
    return sample;
  }
}







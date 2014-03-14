


package src.graphics.widgets;
import src.graphics.common.*;
import src.util.*;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
//import java.nio.*;



public class Fading {
  
  
  final static float
    FADE_TIME  = 0.50f,
    ALPHA_FADE = 1f / (Rendering.FRAMES_PER_SECOND * FADE_TIME);
  
  static class Fade {
    Box2D area = new Box2D();
    float alpha = 1.0f;
    Texture captured = null;
    String key;
  }
  
  
  final Rendering rendering;
  final Table <String, Fade> allFades = new Table();
  
  
  public Fading(Rendering r) {
    this.rendering = r;
  }
  
  
  //  TODO:  Have this called externally?
  public void finalize() {
    for (Fade fade : allFades.values()) fade.captured.dispose();
  }
  
  
  public void applyFadeWithin(Box2D area, String key) {
    
    final Fade prior = allFades.get(key);
    final Fade fade = (prior == null) ? new Fade() : prior;
    fade.area.setTo(area);
    fade.key = key;
    fade.alpha = 1;
    
    final int
      x = (int) area.xpos(), y = (int) area.ypos(),
      w = (int) area.xdim(), h = (int) area.ydim();
    final Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
    Gdx.gl.glReadPixels(
      x, y, w, h, GL10.GL_RGBA,
      GL10.GL_UNSIGNED_BYTE,
      pixmap.getPixels()
    );
    if (prior == null) fade.captured = new Texture(pixmap);
    else fade.captured.draw(pixmap, 0, 0);
    
    pixmap.dispose();
    allFades.put(key, fade);
  }
  
  
  public void applyTo(SpriteBatch batch2D) {
    final Batch <Fade> expired = new Batch <Fade> ();
    
    for (Fade fade : allFades.values()) {
      
      fade.alpha -= ALPHA_FADE;
      if (fade.alpha <= 0) {
        expired.add(fade);
        continue;
      }
      
      final Box2D b = fade.area;
      batch2D.setColor(1, 1, 1, fade.alpha);
      batch2D.draw(
        fade.captured,
        b.xpos(), b.ypos(), b.xdim(), b.ydim(),
        0, 0, 1, 1
      );
    }
    
    for (Fade fade : expired) {
      allFades.remove(fade.key);
      fade.captured.dispose();
    }
    
    final Colour c = rendering.foreColour;
    if (c != null) {
      batch2D.setColor(c.r, c.g, c.b, c.a);
      batch2D.draw(
        ImageAsset.WHITE_TEX,
        0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(),
        0, 0, 1, 1
      );
    }
  }
}










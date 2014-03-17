/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.graphics.sfx;
import src.graphics.common.*;
import src.util.*;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import java.io.*;



public class PlaneFX extends SFX {
  
  
  private static boolean verbose = false;
  
  
  /**  Model definitions, fields, constructors, and save/load methods-
    */
  //  TODO:  Move to a separate file?
  public static class Model extends src.graphics.common.ModelAsset {
    
    final String imageName;
    final float initSize, spin, growth ;
    final boolean tilted, vivid ;
    
    final Box2D animUV[];
    final Box2D bounds = new Box2D();
    final float duration;
    
    private Texture texture;
    private boolean loaded = false;
    
    
    public Model(
      String modelName, Class modelClass,
      String image, float initSize, float spin, float growth,
      boolean tilted, boolean vivid
    ) {
      super(modelName, modelClass);
      this.imageName = image;
      this.initSize = initSize;
      this.spin = spin;
      this.growth = growth;
      this.tilted = tilted;
      this.vivid = vivid;
      
      this.animUV = new Box2D[] {
        new Box2D().set(0, 0, 1, 1)
      };
      this.duration = -1;
    }
    
    
    public Model(
      String modelName, Class modelClass,
      String image, int gridX, int gridY, int numFrames, float duration
    ) {
      super(modelName, modelClass);
      this.imageName = image;
      this.initSize = 1;
      this.spin = 0;
      this.growth = 0;
      this.tilted = true;
      this.vivid = false;
      this.animUV = new Box2D[numFrames];
      
      final float gW = 1f / gridX, gH = 1f / gridY;
      int frame = 0;
      for (int y = 0 ; y < gridX ; y++) {
        for (int x = 0 ; x < gridY; x++) {
          if (frame >= numFrames) break;
          final Box2D b = animUV[frame++] = new Box2D();
          b.set(x * gW, y * gH, gW, gH);
        }
      }
      this.duration = duration;
    }
    
    
    protected void loadAsset() {
      texture = ImageAsset.getTexture(imageName);
      loaded = true;
      
      float
        w = texture.getWidth(),
        h = texture.getHeight(),
        m = Math.max(w, h);
      bounds.set(0, 0, w / m, h / m);
      
      if (animUV.length > 1) {
        w = bounds.xdim() * animUV[0].xdim();
        h = bounds.xdim() * animUV[0].ydim();
        m = Math.max(w, h);
        bounds.set(0, 0, w / m, h / m);
      }
    }
    
    
    protected void disposeAsset() {
      texture.dispose();
    }
    
    
    public boolean isLoaded() { return loaded ; }
    public Sprite makeSprite() { return new PlaneFX(this) ; }
  }
  
  
  final Model model ;
  private float inceptTime = -1;
  
  
  protected PlaneFX(Model model) {
    this.model = model ;
  }
  
  
  public Model model() {
    return model ;
  }
  
  
  public void update() {
    //super.update() ;
    if (inceptTime == -1) reset();
  }
  
  
  
  /**  Actual rendering-
    */
  private static Mat3D trans = new Mat3D();
  
  
  public float animProgress() {
    final float gap = Rendering.activeTime() - inceptTime;
    if (model.duration <= 0) return gap;
    else return gap / model.duration;
  }
  
  
  public void reset() {
    inceptTime = Rendering.activeTime();
  }
  
  
  protected void renderInPass(SFXPass pass) {
    
    //  Determine basic measurements-
    float progress = animProgress();
    if (model.duration > 0 && progress >= 1) return;
    final float radius = model.initSize + (model.growth * progress);
    final float r = radius * scale;
    final float newRot = (rotation + (model.spin * progress)) % 360;
    
    //  Determine the correct animation frame-
    final Box2D f;
    if (model.duration > 0) {
      f = model.animUV[(int) (progress * model.animUV.length)];
      if (verbose) I.say("  Animation frame: "+progress);
    }
    else f = model.animUV[0];
    if (f == null) return;
    
    //  Setup and translate vertex positions-
    final Viewport view = pass.rendering.view;
    trans.setIdentity();
    trans.rotateZ((float) (newRot * Math.PI / 180));
    final Vec3D screenPos = view.translateToScreen(new Vec3D().setTo(position));
    final float screenScale = view.screenScale();
    if (verbose) I.say("Vertices are: ");
    
    final float QV[] = SFXPass.QUAD_VERTS;
    int i = 0 ; for (Vec3D v : verts) {
      v.set(QV[i++], QV[i++], QV[i++]);
      v.x = (v.x - 0.5f) * r * 2 * model.bounds.xdim();
      v.y = (v.y - 0.5f) * r * 2 * model.bounds.ydim();
      v.z = 0;
      trans.trans(v);
      
      if (model.tilted) {
        v.x = (v.x * screenScale) + screenPos.x;
        v.y = screenPos.y - (v.y * screenScale);
        v.z = screenPos.z;
        view.translateFromScreen(v);
      }
      else v.add(position);
      if (verbose) I.say("  "+v);
    }
    
    //  Compile geometry-
    pass.compileQuad(
      model.texture, colour, verts,
      f.xpos(), f.ypos(), f.xmax(), f.ymax(),
      model.vivid
    );
  }
}






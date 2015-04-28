


package stratos.graphics.sfx;
import com.badlogic.gdx.graphics.*;

import java.io.*;

import stratos.graphics.common.*;
import stratos.util.*;



/**  Renders a particle beam between two chosen points-
  */
public class ShotFX extends SFX {
  
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  public static class Model extends stratos.graphics.common.ModelAsset {
    
    final String texName;
    final float arc, period, width, length;
    final boolean repeats, vivid;
    
    private Texture tex;
    private boolean loaded = false, disposed = false;
    
    public Model(
      String modelName, Class modelClass,
      String texName,
      float period   , float arc    ,
      float width    , float length ,
      boolean repeats, boolean vivid
    ) {
      super(modelName, modelClass);
      this.texName = texName;
      this.period  = period ;
      this.arc     = arc    ;
      this.width   = width  ;
      this.length  = length ;
      this.repeats = repeats;
      this.vivid   = vivid  ;
    }
    
    public boolean isLoaded() {
      return loaded;
    }
    
    
    protected void loadAsset() {
      tex = ImageAsset.getTexture(texName);
      loaded = true;
    }
    
    
    protected void disposeAsset() {
      tex.dispose();
      disposed = true;
    }
    
    
    public boolean isDisposed() {
      return disposed;
    }
    
    
    public Sprite makeSprite() {
      return new ShotFX(this);
    }
  }
  
  
  final Model model;
  final public Vec3D
    origin = new Vec3D(),
    target = new Vec3D();
  private float inceptTime = -1;
  
  
  public ShotFX(Model model) {
    super(PRIORITY_MIDDLE);
    this.model = model;
  }
  
  
  public void saveTo(DataOutputStream out) throws Exception {
    super.saveTo(out);
    origin.saveTo(out);
    target.saveTo(out);
    out.writeFloat(inceptTime);
  }
  
  
  public void loadFrom(DataInputStream in) throws Exception {
    super.loadFrom(in);
    origin.loadFrom(in);
    target.loadFrom(in);
    inceptTime = in.readFloat();
  }
  
  
  public Model model() {
    return model;
  }
  
  
  
  /**  Updates and modifications-
    */
  public void readyFor(Rendering rendering) {
    final Vec2D line = new Vec2D();
    line.x = target.x - origin.x;
    line.y = target.y - origin.y;
    this.position.setTo(origin).add(target).scale(0.5f);
    super.readyFor(rendering);
  }
  
  
  public void refreshShot() {
    inceptTime = Rendering.activeTime();
  }
  
  
  public void refreshBurst(Vec3D targPos, ShieldFX shield) {
    if (shield == null) target.setTo(targPos);
    else target.setTo(shield.interceptPoint(origin));
  }
  
  

  private static Vec3D
    tempA = new Vec3D(), tempB = new Vec3D(),
    perp  = new Vec3D(),
    line  = new Vec3D(),
    start = new Vec3D(),
    end   = new Vec3D();
  
  
  protected void renderInPass(SFXPass pass) {
    final boolean report = false;
    
    //  First, we need to determine what the 'perp' angle should be (as in,
    //  perpendicular to the line of the beam, as perceived by the viewer.)
    line.setTo(target).sub(origin);
    line.normalise();
    
    //  We translate the start and end points to screen, and get the line
    //  perpendicular to their direction on-screen.
    pass.rendering.view.translateToScreen(tempA.setTo(origin));
    pass.rendering.view.translateToScreen(tempB.setTo(target));
    perp.setTo(tempB).sub(tempA);
    perp.set(perp.y, -perp.x, 0);
    
    //  We then translate that displacement from screen to world coordinates.
    perp.add(tempA);
    pass.rendering.view.translateFromScreen(perp);
    perp.sub(origin);
    perp.normalise().scale(model.width);
    
    //  Alright.  Based on time elapsed, divided by period, you should have a
    //  certain number of missiles in flight.
    final float distance = origin.distance(target), numParts, partLen;
    final Colour c;
    if (model.period <= 0) {
      numParts = 1;
      partLen = distance;
      c = Colour.WHITE;
    }
    else {
      if (inceptTime == -1) inceptTime = Rendering.activeTime();
      numParts = (Rendering.activeTime() - inceptTime) / model.period;
      partLen = model.length;
      c = Colour.transparency(1f / (1 + numParts));
    }
    
    if (report) {
      I.say("\nRendering shot FX...");
      I.say("  Model ID:  "+model.texName);
      I.say("  Num parts: "+numParts);
      I.say("  Period:    "+model.period);
    }

    // Now render the beam itself-
    for (float n = numParts; n-- > 0;) {
      final float progress = partLen * n;
      final float lift = progress / distance;

      start.setTo(line).scale(progress).add(origin);
      start.z += lift * (1 - lift) * (distance * model.arc);
      end.setTo(line).scale(partLen).add(start);

      if (end.distance(origin) > distance) {
        if (progress > distance)
          break;
        else
          end.setTo(target);
      }
      if (progress < 0)
        start.setTo(origin);

      final float QV[] = SFXPass.QUAD_VERTS;
      int i = 0;
      for (Vec3D v : verts) {
        final boolean x = QV[i++] > 0, y = QV[i++] > 0;
        v.setTo(y ? end : start);
        v.z += QV[i++];
        
        final float initDepth = pass.rendering.view.screenDepth(v);
        if (x)
          v.add(perp);
        else
          v.sub(perp);
        final float afterDepth = pass.rendering.view.screenDepth(v);
        
        ///I.say("  Difference: "+(initDepth - afterDepth));
      }

      pass.compileQuad(model.tex, c, model.vivid, verts, 0, 0, 1, 1);
      if (! model.repeats)
        break;
    }
  }
}


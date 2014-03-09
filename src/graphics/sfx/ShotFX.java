


package src.graphics.sfx ;
import src.graphics.common.* ;

import java.io.* ;

import src.util.* ;

import com.badlogic.gdx.graphics.*;



/**  Renders a particle beam between two chosen points-
  */
public class ShotFX extends SFX {
  
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  public static class Model extends src.graphics.common.ModelAsset {
    
    //final Texture tex ;
    final String texName;
    final float arc, period, width, length ;
    final boolean repeats, vivid ;
    
    Texture tex;
    private boolean loaded = false;
    
    public Model(
      String modelName, Class modelClass,
      String texName,
      float period, float arc, float width, float length,
      boolean repeats, boolean vivid
    ) {
      super(modelName, modelClass) ;
      this.texName = texName ;
      //this.tex = Texture.loadTexture(texName) ;
      this.period = period ;
      this.arc = arc ;
      this.width = width ;
      this.length = length ;
      this.repeats = repeats ;
      this.vivid = vivid;
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
    }
    
    
    public Sprite makeSprite() {
      return new ShotFX(this) ;
    }
  }
  
  
  final Model model;
  final public Vec3D
    origin = new Vec3D(),
    target = new Vec3D();
  private float inceptTime = -1;
  
  
  public ShotFX(Model model) {
    this.model = model ;
  }
  
  
  public void saveTo(DataOutputStream out) throws Exception {
    super.saveTo(out) ;
    origin.saveTo(out) ;
    target.saveTo(out) ;
    out.writeFloat(inceptTime) ;
  }
  
  
  public void loadFrom(DataInputStream in) throws Exception {
    super.loadFrom(in) ;
    origin.loadFrom(in) ;
    target.loadFrom(in) ;
    inceptTime = in.readFloat() ;
  }
  
  
  public Model model() {
    return model ;
  }
  
  
  
  /**  Updates and modifications-
    */
  public void update() {
    super.update() ;
    final Vec2D line = new Vec2D() ;
    line.x = target.x - origin.x ;
    line.y = target.y - origin.y ;
    this.position.setTo(origin).add(target).scale(0.5f) ;
    //time += 1f / 25 ;
  }
  
  
  public void refreshShot() {
    inceptTime = Rendering.activeTime();
  }
  
  
  public void refreshBurst(Vec3D targPos, ShieldFX shield) {
    if (shield == null) target.setTo(targPos);
    else target.setTo(shield.interceptPoint(origin));
  }
  
  

  private static Vec3D
    perp  = new Vec3D(),
    line  = new Vec3D(),
    start = new Vec3D(),
    end   = new Vec3D() ;
  
  
  protected void renderInPass(SFXPass pass) {
    
    //  First, we need to determine what the 'perp' angle should be (as in,
    //  perpendicular to the line of the beam, as perceived by the viewer.)
    perp.setTo(line.setTo(target).sub(origin));
    line.normalise();
    pass.rendering.view.translateToScreen(perp);
    perp.set(perp.y, -perp.x, 0);
    pass.rendering.view.translateFromScreen(perp);
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
    
    //  Now render the beam itself-
    for (float n = numParts ; n-- > 0 ;) {
      final float progress = partLen * n ;
      final float lift = progress / distance ;
      
      start.setTo(line).scale(progress).add(origin) ;
      start.z += lift * (1 - lift) * (distance * model.arc) ;
      end.setTo(line).scale(partLen).add(start) ;
      
      if (end.distance(origin) > distance) {
        if (progress > distance) break ;
        else end.setTo(target) ;
      }
      if (progress < 0) start.setTo(origin) ;
      
      final float QV[] = SFXPass.QUAD_VERTS;
      int i = 0; for (Vec3D v : verts) {
        final boolean x = QV[i++] > 0, y = QV[i++] > 0, z = QV[i++] > 0;
        v.setTo(y ? end : start);
        if (x) v.add(perp);
        else v.sub(perp);
      }
      
      pass.compileQuad(model.tex, c, verts, 0, 0, 1, 1, model.vivid);
      if (! model.repeats) break ;
    }
  }
}






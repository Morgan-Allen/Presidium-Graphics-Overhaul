


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
    final boolean repeats ;
    
    Texture tex;
    private boolean loaded = false;
    
    public Model(
      String modelName, Class modelClass,
      String texName,
      float period, float arc, float width, float length, boolean repeats
    ) {
      super(modelName, modelClass) ;
      this.texName = texName ;
      //this.tex = Texture.loadTexture(texName) ;
      this.period = period ;
      this.arc = arc ;
      this.width = width ;
      this.length = length ;
      this.repeats = repeats ;
    }
    
    public boolean isLoaded() {
      return loaded;
    }
    
    
    protected void loadAsset() {
      tex = Assets.getTexture(texName);
      loaded = true;
    }
    
    
    protected void disposeAsset() {
      tex.dispose();
    }
    
    
    public Sprite makeSprite() {
      return new ShotFX(this) ;
    }
  }
  
  
  final Model model ;
  final public Vec3D
    origin = new Vec3D(),
    target = new Vec3D() ;
  public float time = 0 ;
  
  
  private ShotFX(Model model) {
    this.model = model ;
  }
  
  
  public void saveTo(DataOutputStream out) throws Exception {
    super.saveTo(out) ;
    origin.saveTo(out) ;
    target.saveTo(out) ;
    out.writeFloat(time) ;
  }
  
  
  public void loadFrom(DataInputStream in) throws Exception {
    super.loadFrom(in) ;
    origin.loadFrom(in) ;
    target.loadFrom(in) ;
    time = in.readFloat() ;
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
    time += 1f / 25 ;
  }
  
  
  
  public void refreshBurst(Vec3D targPos, ShieldFX shield) {
    if (shield == null) target.setTo(targPos) ;
    else target.setTo(shield.interceptPoint(origin)) ;
    //update() ;
  }
  
  

  private static Vec3D
    perp  = new Vec3D(),
    line  = new Vec3D(),
    start = new Vec3D(),
    end   = new Vec3D() ;
  
  
  public void registerFor(Rendering rendering) {
    rendering.sfxPass.register(this);
  }
  
  
  protected void renderInPass(SFXPass pass) {
    /*
    //  First, we need to determine what the 'perp' angle should be (as in,
    //  perpendicular to the line of the beam, as perceived by the viewer.)
    perp.setTo(line.setTo(target).sub(origin)) ;
    line.normalise() ;
    rendering.view.viewMatrix(perp) ;
    perp.set(perp.y, -perp.x, 0) ;
    rendering.view.viewInvert(perp) ;
    perp.normalise().scale(model.width) ;
    
    //  Alright.  Based on time elapsed, divided by period, you should have a
    //  certain number of missiles in flight.
    final float distance = origin.distance(target), numParts, partLen ;
    if (model.period <= 0) {
      numParts = 1 ;
      partLen = distance ;
    }
    else {
      numParts = time / model.period ;
      partLen = model.length ;
    }

    //  Now render the beam itself-
    final Colour c = this.colour ;
    final float f = this.fog ;
    GL11.glColor4f(c.r * f, c.g * f, c.b * f, c.a) ;
    
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
      
      verts[0].setTo(start).add(perp) ;
      verts[1].setTo(end  ).add(perp) ;
      verts[2].setTo(end  ).sub(perp) ;
      verts[3].setTo(start).sub(perp) ;
      renderTex(verts, model.tex) ;
      
      if (! model.repeats) break ;
    }
    GL11.glColor4f(1, 1, 1, 1) ;
    //*/
  }
}






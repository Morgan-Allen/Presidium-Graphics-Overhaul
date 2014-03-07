/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.graphics.sfx ;
import src.graphics.common.* ;
import src.util.* ;

import java.io.* ;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
//import org.lwjgl.opengl.GL11 ;



public class PlaneFX extends SFX {
  
  
  /**  Model definitions, fields, constructors, and save/load methods-
    */
  public static class Model extends src.graphics.common.ModelAsset {
    
    //final Texture image ;
    final String imageName;
    final float initSize, spin, growth ;
    final boolean tilted ;
    
    private Texture texture;
    private boolean loaded = false;
    
    public Model(
      String modelName, Class modelClass,
      String image, float initSize, float spin, float growth, boolean tilted
    ) {
      super(modelName, modelClass) ;
      this.imageName = image;
      //this.image = Assets.loadTexture(image) ;
      this.initSize = initSize ;
      this.spin = spin ;
      this.growth = growth ;
      this.tilted = tilted ;
    }

    
    protected void loadAsset() {
      texture = Assets.getTexture(imageName);
      loaded = true;
    }
    
    protected void disposeAsset() {
      texture.dispose();
    }
    
    public boolean isLoaded() { return loaded ; }
    public Sprite makeSprite() { return new PlaneFX(this) ; }
  }
  
  
  final Model model ;
  private float radius ;
  
  
  protected PlaneFX(Model model) {
    this.model = model ;
    this.radius = model.initSize ;
  }
  
  
  public Model model() {
    return model ;
  }
  
  
  public void update() {
    super.update() ;
    rotation = (rotation + model.spin) % 360 ;
    radius += model.growth ;
  }
  
  
  /**  Actual rendering-
    */
  private static Mat3D trans = new Mat3D();
  
  
  public void registerFor(Rendering rendering) {
    ///I.complain("Who?");
    rendering.sfxPass.register(this);
  }
  
  
  protected void renderInPass(SFXPass pass) {
    
    ///I.say("Rendering plane FX...");
    final Vec3D p = this.position;
    final float r = this.radius * scale;
    
    trans.setIdentity();
    trans.rotateZ((float) (rotation * Math.PI / 180));
    
    final float QV[] = SFXPass.QUAD_VERTS;
    int i = 0 ; for (Vec3D v : verts) {
      v.set(QV[i++], QV[i++], QV[i++]);
      v.x = (v.x - 0.5f) * r * 2;
      v.y = (v.y - 0.5f) * r * 2;
      v.z = 0;
      trans.trans(v);
      if (model.tilted) {
        final Viewport view = pass.rendering.view;
        view.translateFromScreen(v);
        v.scale(1f / view.screenScale());
      }
      v.add(p);
    }
    
    pass.compileQuad(model.texture, colour, verts);
  }
}






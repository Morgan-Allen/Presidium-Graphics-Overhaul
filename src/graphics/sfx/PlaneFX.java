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
  private static Mat3D trans = new Mat3D() ;
  
  public void registerFor(Rendering rendering) {
    /*
    final Vec3D p = this.position ;
    final float r = this.radius * scale ;
    
    trans.setIdentity() ;
    trans.rotateZ((float) (rotation * Math.PI / 180)) ;
    
    
    verts[0].set(0 - r, 0 - r, 0) ;
    verts[1].set(0 - r, 0 + r, 0) ;
    verts[2].set(0 + r, 0 + r, 0) ;
    verts[3].set(0 + r, 0 - r, 0) ;
    for (Vec3D v : verts) {
      trans.trans(v) ;
      if (model.tilted) rendering.view.viewInvert(v) ;
      v.add(p) ;
    }
    
    if (colour != null) colour.bindColour() ;
    renderTex(verts, model.image) ;
    //*/
  }
}











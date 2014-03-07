/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.graphics.sfx ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.util.* ;
//import org.lwjgl.opengl.* ;


//
//  This class should be a way of representing drifting smoke, 'spell'
//  indicators, and other similarly flat-on and/or randomly-drifting visual FX.


public class MoteFX extends SFX {
  
  
  final static ModelAsset MOTE_MODEL = new ModelAsset("mote_model", MoteFX.class) {
    public boolean isLoaded() { return true ; }
    protected void loadAsset() {}
    protected void disposeAsset() {}
    public Sprite makeSprite() { return new MoteFX() ; }
  } ;
  
  public Sprite mote ;
  public float progress = 0, animTime = -1 ;
  
  
  
  public ModelAsset model() { return MOTE_MODEL ; }
  MoteFX() {}
  
  
  public MoteFX(Sprite mote) {
    this.mote = mote ;
  }
  
  
  public void update() {
    super.update() ;
    if (animTime != -1) {
      progress += 1f / (25 * animTime) ;
      if (progress > 1) progress = 1 ;
    }
  }
  
  
  protected void renderInPass(SFXPass pass) {
    /*
    mote.matchTo(this) ;
    mote.position.z -= 0.5f ;
    mote.setAnimation("animation", progress) ;
    mote.update() ;
    GL11.glDepthMask(false) ;
    GL11.glDisable(GL11.GL_DEPTH_TEST) ;
    mote.renderTo(rendering) ;
    GL11.glEnable(GL11.GL_DEPTH_TEST) ;
    GL11.glDepthMask(true) ;
    //*/
  }
}



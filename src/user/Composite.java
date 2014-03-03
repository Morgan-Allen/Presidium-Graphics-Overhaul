/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.user ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;
import src.util.* ;


//
//  TODO:  Restore the use of this.

public class Composite {//extends Image {
  /*
  
  final Stack <Layer> layers = new Stack <Layer> () ;
  
  
  public Composite(HUD myHUD) {
    super(myHUD, Texture.BLACK_TEX) ;
    addLayer(this.texture, 0, 0, 1, 1) ;
  }
  

  public Composite(HUD myHUD, Texture tex) {
    super(myHUD, tex) ;
    addLayer(this.texture, 0, 0, 1, 1) ;
  }
  
  
  public Composite(HUD myHUD, String backName) {
    super(myHUD, backName) ;
    addLayer(this.texture, 0, 0, 1, 1) ;
  }
  
  
  class Layer {
    Box2D UV ;
    ImageAsset image;
    //Texture tex ;
  }
  
  
  public void addLayer(Texture tex, int offX, int offY, int gridW, int gridH) {
    if (tex == null) return ;
    final Layer layer = new Layer() ;
    layer.tex = tex ;
    final float sX = tex.maxU() * 1f / gridW, sY = tex.maxV() * 1f / gridH ;
    layer.UV = new Box2D().set(offX * sX, offY * sY, sX, sY) ;
    layers.add(layer) ;
  }
  
  
  protected void renderTo(SpriteBatch batch2D) {
    //
    //  TODO:  You'll have to use render-to-texture functions here?  You'd need
    //  OpenGL for that, though.  Might want to get some formal technical
    //  advice on that point.
    for (Layer layer : layers) {
      super.renderTex(layer.image.asTexture(), 1, batch2D);
      renderIn(bounds, layer.tex, layer.UV) ;
    }
  }
  //*/
}






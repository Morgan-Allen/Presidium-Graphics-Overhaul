/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package graphics.widgets ;
import util.* ;
import graphics.common.* ;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.*;




public class Button extends Image {
  
  
  /**  Data fields and basic constructors-
    */
  public float
    hoverLit = 0.5f,
    pressLit = 0.75f ;
  protected Texture highlit ;
  protected String info ;
  
  
  public Button(HUD myHUD, String norm, String infoS) {
    this(
      myHUD,
      loadTexture(norm),
      loadTexture("UI/lit_button.png"),
      infoS
    ) ;
  }
  
  public Button(HUD myHUD, String path, String norm, String lit, String infoS) {
    this(
      myHUD,
      loadTexture(path+norm),
      loadTexture(path+lit),
      infoS
    ) ;
  }
  
  public Button(HUD myHUD, Texture norm, Texture lit, String infoS) {
    super(myHUD, norm) ;
    info = infoS ;
    highlit = lit ;
  }
  
  
  public void setHighlight(Texture h) {
    highlit = h ;
  }
  
  
  protected String info() {
    return info ;
  }
  
  
  
  /**  UI method overrides/implementations-
    */
  protected UINode selectionAt(Vector2 mousePos) {
    if (super.selectionAt(mousePos) == null) return null ;
    return this;
  }
  

  protected void render(SpriteBatch batch2D) {
    final float FADE_TIME = 0.25f ;
    super.renderTex(texture, 1, batch2D);
    if (amPressed() || amDragged() || amClicked()) {
      super.renderTex(highlit, pressLit, batch2D);
    }
    else if (amHovered()) {
      float alpha = absAlpha * hoverLit;
      alpha *= Visit.clamp(myHUD.timeHovered() / FADE_TIME, 0, 1);
      super.renderTex(highlit, alpha, batch2D);
    }
  }
}



/*
//  selection modes.
final public static byte
  MODE_RADIUS = 0,
  MODE_BOUNDS = 1,
  MODE_ALPHA = 2 ;
//*/

/*
if (selectMode == MODE_BOUNDS) {
  return this ;
}
if (selectMode == MODE_RADIUS) {
  final float radius = Math.max(bounds.xdim(), bounds.ydim()) / 2 ;
  return (bounds.centre().dst(mousePos) < radius) ? this : null ;
}
if (selectMode == MODE_ALPHA) {
  final float
    tU = ((mousePos.x - bounds.xpos()) / bounds.xdim()),
    tV = ((mousePos.y - bounds.ypos()) / bounds.ydim()) ;
  
  texture.getTextureData().consumePixmap().getPixel(tU, tV);
  final Colour texSample = texture.getColour(tU, tV) ;
  return (texSample.a > 0.5f) ? this : null ;
}
return null ;
//*/


/*
final float FADE_TIME = 0.25f ;
super.render() ;
final Texture realTex = texture ;
final float realAlpha = absAlpha ;
texture = highlit ;
if (amPressed() || amDragged() || amClicked()) {
  absAlpha *= pressLit ;
  super.render() ;
}
if (amHovered()) {
  absAlpha *= hoverLit ;
  absAlpha *= Visit.clamp(myHUD.timeHovered() / FADE_TIME, 0, 1) ;
  super.render() ;
}
absAlpha = realAlpha ;
texture = realTex ;
//*/



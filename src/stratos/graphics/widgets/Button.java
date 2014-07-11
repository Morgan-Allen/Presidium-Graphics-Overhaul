/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.graphics.widgets ;
import stratos.graphics.common.*;
import stratos.util.*;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.*;




public class Button extends Image {
  
  
  /**  Data fields and basic constructors-
    */
  final public static ImageAsset
    DEFAULT_LIT = ImageAsset.fromImage(
      "media/GUI/iconLit.gif", Button.class
    ),
    CIRCLE_LIT = ImageAsset.fromImage(
      "media/GUI/icon_lit_circle.png", Button.class
    );
  
  public float
    hoverLit = DEFAULT_HOVER_ALPHA,
    pressLit = DEFAULT_PRESS_ALPHA;
  protected Texture highlit;
  protected String info;
  
  
  public Button(HUD myHUD, ImageAsset norm, String infoS) {
    this(
      myHUD, norm.asTexture(),
      DEFAULT_LIT.asTexture(),
      infoS
    ) ;
  }
  

  public Button(HUD myHUD, Texture norm, String infoS) {
    this(myHUD, norm, DEFAULT_LIT.asTexture(), infoS);
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
    return (bounds.contains(mousePos.x, mousePos.y)) ? this : null ;
  }
  

  protected void render(WidgetsPass pass) {
    super.renderTex(texture, absAlpha, pass);
    if (amPressed() || amDragged() || amClicked()) {
      super.renderTex(highlit, pressLit * absAlpha, pass);
    }
    else if (amHovered()) {
      float alpha = absAlpha * hoverLit;
      alpha *= Visit.clamp(UI.timeHovered() / DEFAULT_FADE_TIME, 0, 1);
      super.renderTex(highlit, alpha, pass);
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


  public Button(HUD myHUD, String path, String norm, String lit, String infoS) {
    this(
      myHUD,
      ImageAsset.getTexture(path+norm),
      ImageAsset.getTexture(path+lit),
      infoS
    );
  }
  //*/
//*/




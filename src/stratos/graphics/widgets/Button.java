/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.widgets;
import stratos.graphics.common.*;
import stratos.util.*;
import stratos.graphics.widgets.Text.Clickable;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.*;



public class Button extends Image {
  
  
  /**  Data fields and basic constructors-
    */
  final public static ImageAsset
    DEFAULT_LIT = ImageAsset.fromImage(
      Button.class, "media/GUI/iconLit.gif"
    ),
    CIRCLE_LIT  = ImageAsset.fromImage(
      Button.class, "media/GUI/icon_lit_circle.png"
    );
  
  
  protected Texture   highlit;
  protected String    info   ;
  protected Clickable links  ;
  
  public float
    hoverLit = DEFAULT_HOVER_ALPHA,
    pressLit = DEFAULT_PRESS_ALPHA;
  public boolean
    toggled = false;
  
  
  
  public Button(
    HUD UI, String widgetID, ImageAsset norm, String infoS
  ) {
    this(UI, widgetID, norm.asTexture(), DEFAULT_LIT.asTexture(), infoS);
  }
  

  public Button(
    HUD UI, String widgetID, ImageAsset norm, ImageAsset lit, String infoS
  ) {
    this(UI, widgetID, norm.asTexture(), lit.asTexture(), infoS);
  }
  
  
  public Button(
    HUD UI, String widgetID, Texture norm, String infoS
  ) {
    this(UI, widgetID, norm, DEFAULT_LIT.asTexture(), infoS);
  }
  
  
  public Button(
    HUD UI, String widgetID, Texture norm, Texture lit, String infoS
  ) {
    super(UI, norm);
    setWidgetID(widgetID);
    this.info     = infoS;
    this.highlit  = lit;
  }
  
  
  public void setLinks(Clickable links) {
    this.links = links;
    this.info  = links.fullName();
  }
  
  
  public void setHighlight(Texture h) {
    this.highlit = h;
  }
  
  
  protected String info() {
    if (! enabled) return info+"\n"+disableInfo();
    return info;
  }
  
  
  protected String disableInfo() {
    return "(Unavailable)";
  }
  
  
  public boolean equals(Object other) {
    if (! (other instanceof Button)) return false;
    final Button b = (Button) other;
    return this.toString().equals(b.toString());
  }
  
  
  public int hashCode() {
    if (links != null || info != null) return toString().hashCode();
    else return super.hashCode();
  }
  
  
  public String toString() {
    if      (links != null) return links.fullName()+" (button link)";
    else if (info  != null) return "Button "+I.shorten(info, 8);
    else                    return "Button "+hashCode();
  }
  
  
  
  /**  UI method overrides/implementations-
    */
  protected UINode selectionAt(Vector2 mousePos) {
    return (trueBounds().contains(mousePos.x, mousePos.y)) ? this : null;
    //  TODO:  Consider restoring multiple selection modes.
  }
  
  
  public void performAction() {
    this.whenClicked();
  }
  
  
  protected void whenClicked() {
    super.whenClicked();
    if (enabled && links != null) links.whenClicked();
  }
  
  
  protected void render(WidgetsPass pass) {
    super.render(pass);
    if (! enabled) return;
    if (toggled) {
      super.renderTex(highlit, 1, pass);
    }
    else if (amPressed() || amDragged() || amClicked()) {
      super.renderTex(highlit, pressLit * absAlpha, pass);
    }
    else if (amHovered()) {
      float alpha = absAlpha * hoverLit;
      alpha *= Nums.clamp(UI.timeHovered() / DEFAULT_FADE_TIME, 0, 1);
      super.renderTex(highlit, alpha, pass);
    }
  }
}





/*
//  selection modes.
final public static byte
  MODE_RADIUS = 0,
  MODE_BOUNDS = 1,
  MODE_ALPHA = 2;
//*/

/*
if (selectMode == MODE_BOUNDS) {
  return this;
}
if (selectMode == MODE_RADIUS) {
  final float radius = Nums.max(bounds.xdim(), bounds.ydim()) / 2;
  return (bounds.centre().dst(mousePos) < radius) ? this : null;
}
if (selectMode == MODE_ALPHA) {
  final float
    tU = ((mousePos.x - bounds.xpos()) / bounds.xdim()),
    tV = ((mousePos.y - bounds.ypos()) / bounds.ydim());
  
  texture.getTextureData().consumePixmap().getPixel(tU, tV);
  final Colour texSample = texture.getColour(tU, tV);
  return (texSample.a > 0.5f) ? this : null;
}
return null;


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




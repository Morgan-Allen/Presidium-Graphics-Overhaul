/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.graphics.widgets;
import stratos.graphics.common.*;
import stratos.util.*;
import com.badlogic.gdx.graphics.*;



public class Bordering extends UIGroup {
  
  
  final Texture borderTex;
  final public UIGroup inside;
  public int
    left   = 10, right = 10,
    bottom = 10, top   = 10;
  public float
    leftU   = 0.33f, rightU = 0.33f,
    bottomV = 0.33f, topV   = 0.33f;
  
  
  public Bordering(HUD UI, ImageAsset tex) {
    super(UI);
    this.borderTex = tex.asTexture();
    this.inside = new UIGroup(UI);
    inside.attachTo(this);
  }
  
  
  public void setInsets(int l, int r, int b, int t) {
    left   = l;
    right  = r;
    bottom = b;
    top    = t;
  }
  
  
  public void setUV(float l, float r, float b, float t) {
    leftU   = l;
    rightU  = r;
    bottomV = b;
    topV    = t;
  }
  
  
  public void surround(UINode other) {
    if (other.parent() != this.parent()) {
      I.complain("MUST HAVE COMMON REFERENCE FRAME!");
      return;
    }
    this.absBound.setTo(other.absBound);
    this.relBound.setTo(other.relBound);
    
    absBound.incX(0 - left);
    absBound.incWide(left + right);
    absBound.incY(0 - bottom);
    absBound.incHigh(bottom + top);
  }
  
  
  protected void updateState() {
    inside.relBound.set(0, 0, 1, 1);
    inside.absBound.set(left, bottom, 0 - (left + right), 0 - (top + bottom));
    super.updateState();
  }
  
  
  protected void render(WidgetsPass pass) {
    renderBorder(
      pass, bounds,
      left, right, top, bottom,
      leftU, rightU, bottomV, topV,
      borderTex, Colour.transparency(relAlpha)
    );
    super.render(pass);
  }
  
  
  
  /**  Public implementation for the convenience of other widgets-
    */
  final static float
    coordX[] = new float[4],
    coordY[] = new float[4],
    coordU[] = new float[4],
    coordV[] = new float[4];
  
  
  public static void renderBorder(
    WidgetsPass pass, Box2D area,
    int left, int right, int bottom, int top,
    float LU, float RU, float BV, float TV,
    Texture borderTex, Colour c
  ) {
    coordX[0] = 0;
    coordX[1] = left;
    coordX[2] = area.xdim() - right;
    coordX[3] = area.xdim();
    
    coordY[0] = 0;
    coordY[1] = bottom;
    coordY[2] = area.ydim() - top;
    coordY[3] = area.ydim();
    
    coordU[0] = 0;
    coordU[1] = LU;
    coordU[2] = 1 - RU;
    coordU[3] = 1;
    
    coordV[0] = 0;
    coordV[1] = BV;
    coordV[2] = 1 - TV;
    coordV[3] = 1;
    
    for (int i = 4; i-- > 0;) {
      coordX[i] += area.xpos();
      coordY[i] = area.ymax() - coordY[i];
    }
    
    pass.setColor(c.r, c.g, c.b, c.a);
    for (int x = 3; x-- > 0;) for (int y = 3; y-- > 0;) {
      pass.draw(
        borderTex,
        coordX[x],
        coordY[y],
        coordX[x + 1] - coordX[x],
        coordY[y + 1] - coordY[y],
        coordU[x],
        coordV[y],
        coordU[x + 1],
        coordV[y + 1]
      );
    }
  }
}









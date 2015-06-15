/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.graphics.cutout;
import stratos.graphics.common.*;
import stratos.util.*;
import java.io.*;



public class CutoutSprite extends Sprite {
  
  protected CutoutModel model;
  protected int faceIndex = 0;
  
  
  public CutoutSprite(CutoutModel model, int faceIndex) {
    this.model = model;
    this.passType = model.splat ? PASS_SPLAT : PASS_NORMAL;
    this.faceIndex = faceIndex;
  }
  
  
  protected void saveTo(DataOutputStream out) throws Exception {
    super.saveTo(out);
    out.write(faceIndex);
  }
  
  
  protected void loadFrom(DataInputStream in) throws Exception {
    super.loadFrom(in);
    faceIndex = in.read();
  }
  
  
  public CutoutModel model() {
    return model;
  }
  
  
  public void setModel(CutoutModel model) {
    this.model = model;
  }
  
  
  public void setAnimation(String animName, float progress, boolean loop) {}
  
  
  public void readyFor(Rendering rendering) {
    if (passType == PASS_NORMAL && model.splat) passType = PASS_SPLAT;
    rendering.cutoutsPass.register(this);
  }
  
  
  
  /**  Some additional utility methods for convenience (used for rendering
    *  status FX, good amounts when hovering over industries, etc...)
    */
  public static void renderAbove(
    Vec3D point, float offX, float offY, float offZ, Rendering rendering,
    float spacing, float scale, Series <CutoutModel> models
  ) {
    final Batch <CutoutSprite> sprites = new Batch();
    for (CutoutModel m : models) {
      CutoutSprite s = m.makeSprite();
      s.scale = scale;
      sprites.add(s);
    }
    layoutAbove(point, offX, offY, offZ, rendering.view, spacing, sprites);
    for (CutoutSprite s : sprites) s.readyFor(rendering);
  }
  
  
  public static void layoutAbove(
    Vec3D point, float offX, float offY, float offZ, Viewport view,
    float spacing, Series <? extends Sprite> sprites
  ) {
    float
      offAcross = (sprites.size() - 1) / 2f,
      index     = 0;
    final Vec3D
      horiz = view.screenHorizontal().normalise(),
      vert  = view.screenVertical  ().normalise(),
      deep  = view.direction       ().normalise(),
      adds  = new Vec3D();
    
    for (Sprite s : sprites) {
      s.position.setTo(point);
      adds.setTo(horiz).scale(offX + ((index - offAcross) * spacing));
      s.position.add(adds);
      adds.setTo(vert).scale(offY);
      s.position.add(adds);
      adds.setTo(deep).scale(offZ);
      s.position.add(adds);
      index++;
      
      s.colour = Colour.glow(1);
    }
  }
}

















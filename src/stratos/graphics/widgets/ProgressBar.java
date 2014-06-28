

package stratos.graphics.widgets;
import stratos.start.*;
import stratos.graphics.common.*;
import stratos.util.*;

import com.badlogic.gdx.graphics.*;



public class ProgressBar extends UINode {
  
  
  final Texture fillTex, backTex;
  public float repeatWidth = -1;
  
  public Colour fillColour = Colour.WHITE;
  public float progress = 0;
  
  
  public ProgressBar(HUD UI, String fillImage, String backImage) {
    super(UI);
    if (! PlayLoop.onRenderThread()) I.complain("ONLY DURING RENDER THREAD!");
    fillTex = ImageAsset.getTexture(fillImage);
    backTex = ImageAsset.getTexture(backImage);
    repeatWidth = fillTex.getWidth();
  }
  
  
  protected void render(WidgetsPass pass) {
    renderBar(backTex, pass, true);
    renderBar(fillTex, pass, false);
    //  TODO:  Add some bordering around the edge?
  }
  
  
  private void renderBar(Texture tex, WidgetsPass pass, boolean back) {
    float numUnits = (back ? 1 : progress) * bounds.xdim() / repeatWidth;
    float across = bounds.xpos();
    final Colour c = (fillColour == null || back) ? Colour.WHITE : fillColour;
    
    while (numUnits > 0) {
      final float wide = numUnits > 1 ? 1 : numUnits;
      pass.setColor(c.r, c.g, c.b, c.a);
      pass.draw(
        tex,
        across, bounds.ypos(), wide * repeatWidth, bounds.ydim(),
        0, 0, wide, 1
      );
      across += repeatWidth;
      numUnits--;
    }
  }
}




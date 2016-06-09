/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package stratos.graphics.widgets;
import stratos.graphics.common.*;

import com.badlogic.gdx.graphics.*;




//  TODO:  Consider merging this with the Healthbar class?  Or having it extend
//         this class?

public class ProgressBar extends UINode {
  
  
  final Texture fillTex, backTex;
  public float repeatWidth = -1;
  
  public Colour fillColour = Colour.WHITE;
  public float progress = 0;
  
  
  public ProgressBar(HUD UI, String fillImage, String backImage) {
    super(UI);
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
      pass.draw(
        tex, c,
        across, bounds.ypos(), wide * repeatWidth, bounds.ydim(),
        0, 0, wide, 1
      );
      across += repeatWidth;
      numUnits--;
    }
  }
}




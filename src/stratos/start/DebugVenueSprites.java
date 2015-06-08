/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.start;
import stratos.graphics.common.*;
import stratos.graphics.cutout.*;
import stratos.util.*;




//  TODO:  Expand on this in a similar manner to SkinsPreview, so you don't
//         have to re-load the entire game to see a change in a cutout.

public class DebugVenueSprites extends VisualDebug {
  
  
  public static void main(String a[]) {
    PlayLoop.setupAndLoop(new DebugVenueSprites(), "stratos.graphics");
  }
  
  
  protected void loadVisuals() {
    final int trim = 1, high = 1, size = 4;
    
    final CutoutModel basis = CutoutModel.fromImage(
      this.getClass(), "media/Buildings/artificer/artificer.png", size, high
    );
    Assets.loadNow(basis);
    final CutoutModel frame = CutoutModel.fromImage(
      this.getClass(), "media/Buildings/civilian/scaffold.png", 1.1f, 1
    );
    Assets.loadNow(frame);
    
    PlayLoop.rendering().backColour = Colour.RED;
    
    for (Coord c : Visit.grid(trim, 0, size - trim, size, 1)) {
      final CutoutSprite face = basis.topSprite(c.x, c.y);
      sprites.add(face);
      for (int h = high; h-- > 0;) {
        if (c.x == 0       ) sprites.add(basis.southSprite(c.y, h));
        if (c.y == size - 1) sprites.add(basis.eastSprite (c.x, h));
      }
    }
    
    for (int n = 4; n-- > 0;) {
      final CutoutSprite front = frame.makeSprite();
      front.position.set(trim - 2.5f, n - 1.5f, 0);
      sprites.add(front);
    }
  }
  
  
  protected void onRendering(Sprite sprite) {
  }
}












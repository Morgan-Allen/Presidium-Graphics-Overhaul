/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package stratos.start;
import stratos.game.civic.ShieldWall;
import stratos.graphics.common.*;
import stratos.graphics.cutout.CutoutModel;
import stratos.util.*;




//  TODO:  Expand on this in a similar manner to SkinsPreview, so you don't
//         have to re-load the entire game to see a change in a cutout.

public class DebugVenueSprites extends VisualDebug {
  
  
  public static void main(String a[]) {
    PlayLoop.setupAndLoop(new DebugVenueSprites(), "stratos.graphics");
  }
  
  
  protected void loadVisuals() {
    //if (true ) loadShieldWalls();
  }
  
  
  protected void onRendering(Sprite sprite) {
  }
}












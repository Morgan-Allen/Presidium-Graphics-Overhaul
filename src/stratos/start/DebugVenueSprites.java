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




public class DebugVenueSprites extends VisualDebug {
  
  
  public static void main(String a[]) {
    PlayLoop.setupAndLoop(new DebugVenueSprites(), "stratos.graphics");
  }
  
  
  protected void loadVisuals() {
    if (true ) loadShieldWalls();
  }
  
  
  final static ModelAsset WALL_MODELS[] = CutoutModel.fromImages(
    ShieldWall.class, "media/Buildings/military/", 2, 4, false,
    "wall_corner.png",
    "wall_tower_left.png",
    "wall_tower_right.png",
    "wall_segment_left.png",
    "wall_segment_right.png"
  );
  
  private void loadShieldWalls() {
    Sprite s = null;
    
    s = WALL_MODELS[0].makeSprite();
    s.position.set(0, 0, 0);
    sprites.add(s);
    
    s = WALL_MODELS[3].makeSprite();
    s.position.set(0, 2, 0);
    sprites.add(s);
    
    s = WALL_MODELS[1].makeSprite();
    s.position.set(0, 4, 0);
    sprites.add(s);
    
    s = WALL_MODELS[3].makeSprite();
    s.position.set(0, 6, 0);
    sprites.add(s);
    
    s = WALL_MODELS[3].makeSprite();
    s.position.set(0, 8, 0);
    sprites.add(s);
    
    s = WALL_MODELS[3].makeSprite();
    s.position.set(0, 10, 0);
    sprites.add(s);
  }
  
  
  protected void onRendering(Sprite sprite) {
  }
}











